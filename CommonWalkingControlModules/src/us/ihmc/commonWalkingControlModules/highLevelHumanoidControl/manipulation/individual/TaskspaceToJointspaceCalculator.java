package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

import us.ihmc.utilities.kinematics.InverseJacobianSolver;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SpatialMotionVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;
import us.ihmc.yoUtilities.math.YoSolvePseudoInverseSVDWithDampedLeastSquaresNearSingularities;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFramePoint;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFrameQuaternion;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoFrameVector;
import us.ihmc.yoUtilities.math.filters.AlphaFilteredYoVariable;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameQuaternion;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;

public class TaskspaceToJointspaceCalculator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry;

   public enum SecondaryObjective {TOWARD_RESTING_CONFIGURATION, AWAY_FROM_JOINT_LIMITS};

   private final RigidBodyTransform desiredControlFrameTransform = new RigidBodyTransform();
   private final FramePose desiredControlFramePose = new FramePose();
   private final Twist desiredControlFrameTwist = new Twist();

   private final OneDoFJoint[] originalJoints;
   private final OneDoFJoint[] localJoints;
   private final ReferenceFrame originalBaseFrame;
   private final ReferenceFrame localBaseFrame;
   private final ReferenceFrame originalBaseParentJointFrame;
   private final PoseReferenceFrame localBaseParentJointFrame;
   private final ReferenceFrame localEndEffectorFrame;
   private final ReferenceFrame originalEndEffectorFrame;
   private final ReferenceFrame localControlFrame;
   private ReferenceFrame originalControlFrame;

   private final Map<ReferenceFrame, ReferenceFrame> originalToLocalFramesMap = new LinkedHashMap<>();

   private final GeometricJacobian jacobian;
   private final YoSolvePseudoInverseSVDWithDampedLeastSquaresNearSingularities solver;
   private final InverseJacobianSolver inverseJacobianSolver;
   private final int numberOfDoF;
   private final int maxNumberOfConstraints = SpatialMotionVector.SIZE;

   private final DoubleYoVariable jointAngleRegularizationWeight;
   private final IntegerYoVariable exponentForPNorm;
   private final EnumYoVariable<SecondaryObjective> currentSecondaryObjective;

   private final DoubleYoVariable maximumJointVelocity;
   private final DoubleYoVariable maximumJointAcceleration;

   private final DoubleYoVariable maximumTaskspaceAngularVelocityMagnitude;
   private final DoubleYoVariable maximumTaskspaceLinearVelocityMagnitude;

   private final YoFrameVector yoErrorRotation;
   private final YoFrameVector yoErrorTranslation;

   private final YoFrameVector yoAngularVelocityFromError;
   private final YoFrameVector yoLinearVelocityFromError;

   private final DoubleYoVariable alphaSpatialVelocityFromError;
   private final AlphaFilteredYoFrameVector filteredAngularVelocityFromError;
   private final AlphaFilteredYoFrameVector filteredLinearVelocityFromError;

   private final YoFramePoint yoBaseParentJointFramePosition;
   private final YoFrameQuaternion yoBaseParentJointFrameOrientation;

   private final DoubleYoVariable alphaBaseParentJointPose;
   private final AlphaFilteredYoFramePoint yoBaseParentJointFramePositionFiltered;
   private final AlphaFilteredYoFrameQuaternion yoBaseParentJointFrameOrientationFiltered;

   private final BooleanYoVariable enableFeedbackControl;
   private final DoubleYoVariable kpTaskspaceAngularError;
   private final DoubleYoVariable kpTaskspaceLinearError;

   private final FramePoint baseParentJointFramePosition = new FramePoint();
   private final FrameOrientation baseParentJointFrameOrientation = new FrameOrientation();

   private final AxisAngle4d errorAxisAngle = new AxisAngle4d();
   private final Vector3d errorRotationVector = new Vector3d();
   private final Vector3d errorTranslationVector = new Vector3d();
   private final DenseMatrix64F spatialVelocityFromError = new DenseMatrix64F(maxNumberOfConstraints, 1);
   private final DenseMatrix64F subspaceSpatialError = new DenseMatrix64F(maxNumberOfConstraints, 1);
   private final DenseMatrix64F spatialDesiredVelocity = new DenseMatrix64F(maxNumberOfConstraints, 1);

   private final DoubleYoVariable[] yoPrivilegedJointPositions;

   private final DenseMatrix64F privilegedJointVelocities;
   private final DenseMatrix64F jointSquaredRangeOfMotions;
   private final DenseMatrix64F jointAnlgesAtMidRangeOfMotion;
   private final DenseMatrix64F desiredJointAngles;
   private final DenseMatrix64F desiredJointVelocities;
   private final DenseMatrix64F desiredJointAccelerations;

   private final FramePose originalBasePose = new FramePose();

   private final double controlDT;

   public TaskspaceToJointspaceCalculator(String namePrefix, RigidBody base, RigidBody endEffector, double controlDT, YoVariableRegistry parentRegistry)
   {
      registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.controlDT = controlDT;

      originalBaseParentJointFrame = base.getParentJoint().getFrameAfterJoint();
      originalBaseFrame = base.getBodyFixedFrame();
      localBaseParentJointFrame = new PoseReferenceFrame(base.getName() + "Local", worldFrame);
      String localBaseFrameName = originalBaseFrame.getName() + "Local";
      RigidBodyTransform transformToParent = originalBaseFrame.getTransformToDesiredFrame(originalBaseParentJointFrame);
      localBaseFrame = ReferenceFrame.constructFrameWithUnchangingTransformToParent(localBaseFrameName, localBaseParentJointFrame, transformToParent);

      originalJoints = ScrewTools.createOneDoFJointPath(base, endEffector);
      localJoints = ScrewTools.cloneJointPathDisconnectedFromOriginalRobot(originalJoints, OneDoFJoint.class, "Local", localBaseParentJointFrame);
      numberOfDoF = localJoints.length;

      originalEndEffectorFrame = endEffector.getBodyFixedFrame();
      originalControlFrame = endEffector.getBodyFixedFrame();

      localEndEffectorFrame = localJoints[numberOfDoF - 1].getSuccessor().getBodyFixedFrame();
      localControlFrame = createLocalControlFrame(localEndEffectorFrame, originalEndEffectorFrame);

      populateRefrenceFrameMap();

      jacobian = new GeometricJacobian(localJoints, localEndEffectorFrame);
      solver = new YoSolvePseudoInverseSVDWithDampedLeastSquaresNearSingularities(namePrefix, maxNumberOfConstraints, maxNumberOfConstraints, registry);

      inverseJacobianSolver = new InverseJacobianSolver(maxNumberOfConstraints, numberOfDoF, solver);

      jointAngleRegularizationWeight = new DoubleYoVariable(namePrefix + "JointAngleRegularizationWeight", registry);

      exponentForPNorm = new IntegerYoVariable(namePrefix + "ExponentForPNorm", registry);
      exponentForPNorm.set(6);

      currentSecondaryObjective = new EnumYoVariable<>(namePrefix + "SecondaryObjective", registry, SecondaryObjective.class);
      currentSecondaryObjective.set(SecondaryObjective.TOWARD_RESTING_CONFIGURATION);

      yoPrivilegedJointPositions = new DoubleYoVariable[numberOfDoF];
      privilegedJointVelocities = new DenseMatrix64F(numberOfDoF, 1);

      desiredJointAngles = new DenseMatrix64F(numberOfDoF, 1);
      desiredJointVelocities = new DenseMatrix64F(numberOfDoF, 1);
      desiredJointAccelerations = new DenseMatrix64F(numberOfDoF, 1);

      jointSquaredRangeOfMotions = new DenseMatrix64F(numberOfDoF, 1);
      jointAnlgesAtMidRangeOfMotion = new DenseMatrix64F(numberOfDoF, 1);

      maximumJointVelocity = new DoubleYoVariable(namePrefix + "MaximumJointVelocity", registry);
      maximumJointVelocity.set(Double.POSITIVE_INFINITY);
      maximumJointAcceleration = new DoubleYoVariable(namePrefix + "MaximumJointAcceleration", registry);
      maximumJointAcceleration.set(Double.POSITIVE_INFINITY);

      maximumTaskspaceAngularVelocityMagnitude = new DoubleYoVariable(namePrefix + "MaximumTaskspaceAngularVelocityMagnitude", registry);
      maximumTaskspaceLinearVelocityMagnitude = new DoubleYoVariable(namePrefix + "MaximumTaskspaceLinearVelocityMagnitude", registry);

      maximumTaskspaceAngularVelocityMagnitude.set(Double.POSITIVE_INFINITY);
      maximumTaskspaceLinearVelocityMagnitude.set(Double.POSITIVE_INFINITY);

      for (int i = 0; i < numberOfDoF; i++)
      {
         String jointName = originalJoints[i].getName();
         yoPrivilegedJointPositions[i] = new DoubleYoVariable("q_privileged_" + jointName, registry);
         jointSquaredRangeOfMotions.set(i, 0, MathTools.square(localJoints[i].getJointLimitUpper() - localJoints[i].getJointLimitLower()));
         jointAnlgesAtMidRangeOfMotion.set(i, 0, 0.5 * (localJoints[i].getJointLimitUpper() + localJoints[i].getJointLimitLower()));
      }

      yoErrorRotation = new YoFrameVector(namePrefix + "ErrorRotation", localControlFrame, registry);
      yoErrorTranslation = new YoFrameVector(namePrefix + "ErrorTranslation", localControlFrame, registry);

      yoAngularVelocityFromError = new YoFrameVector(namePrefix + "AngularVelocityFromError", localControlFrame, registry);
      yoLinearVelocityFromError = new YoFrameVector(namePrefix + "LinearVelocityFromError", localControlFrame, registry);

      alphaSpatialVelocityFromError = new DoubleYoVariable(namePrefix + "AlphaSpatialVelocityFromError", registry);
      filteredAngularVelocityFromError = AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector(namePrefix + "FilteredAngularVelocityFromError", "", registry, alphaSpatialVelocityFromError, yoAngularVelocityFromError);
      filteredLinearVelocityFromError = AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector(namePrefix + "FilteredLinearVelocityFromError", "", registry, alphaSpatialVelocityFromError, yoLinearVelocityFromError);

      yoBaseParentJointFramePosition = new YoFramePoint(namePrefix + "BaseParentJointFrame", worldFrame, registry);
      yoBaseParentJointFrameOrientation = new YoFrameQuaternion(namePrefix + "BaseParentJointFrame", worldFrame, registry);

      alphaBaseParentJointPose = new DoubleYoVariable(namePrefix + "AlphaBaseParentJointPose", registry);
      yoBaseParentJointFramePositionFiltered = AlphaFilteredYoFramePoint.createAlphaFilteredYoFramePoint(namePrefix + "BaseParentJointFrameFiltered", "", registry, alphaBaseParentJointPose, yoBaseParentJointFramePosition);
      yoBaseParentJointFrameOrientationFiltered = new AlphaFilteredYoFrameQuaternion(namePrefix + "BaseParentJointFrameFiltered", "", yoBaseParentJointFrameOrientation, alphaBaseParentJointPose, registry);

      enableFeedbackControl = new BooleanYoVariable(namePrefix + "EnableFeedBackControl", registry);
      kpTaskspaceAngularError = new DoubleYoVariable(namePrefix + "KpTaskspaceAngularError", registry);
      kpTaskspaceLinearError = new DoubleYoVariable(namePrefix + "KpTaskspaceLinearError", registry);

      parentRegistry.addChild(registry);
   }

   private void populateRefrenceFrameMap()
   {
      originalToLocalFramesMap.put(originalBaseParentJointFrame, localBaseParentJointFrame);
      originalToLocalFramesMap.put(originalControlFrame, localControlFrame);
      originalToLocalFramesMap.put(originalBaseFrame, localBaseFrame);

      for (int i = 0; i < numberOfDoF; i++)
      {
         OneDoFJoint originalJoint = originalJoints[i];
         RigidBody originalBody = originalJoint.getSuccessor();
         OneDoFJoint localJoint = localJoints[i];
         RigidBody localBody = localJoint.getSuccessor();
         
         originalToLocalFramesMap.put(originalJoint.getFrameAfterJoint(), localJoint.getFrameAfterJoint());
         originalToLocalFramesMap.put(originalJoint.getFrameBeforeJoint(), localJoint.getFrameBeforeJoint());
         originalToLocalFramesMap.put(originalBody.getBodyFixedFrame(), localBody.getBodyFixedFrame());
      }
   }

   private ReferenceFrame createLocalControlFrame(final ReferenceFrame localEndEffectorFrame, final ReferenceFrame originalEndEffectorFrame)
   {
      ReferenceFrame localControlFrame = new ReferenceFrame("controlFrame", localEndEffectorFrame)
      {
         private static final long serialVersionUID = -7254192469546194133L;

         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
            originalControlFrame.getTransformToDesiredFrame(transformToParent, originalEndEffectorFrame);
         }
      };
      return localControlFrame;
   }

   // TODO Need to be extracted
   public void setupWithDefaultParameters()
   {
      setFullyConstrained();
      setPrivilegedJointPositionsToMidRange();
      setJointAngleRegularizationWeight(5.0);
      setMaximumJointVelocity(5.0);
      setMaximumJointAcceleration(50.0);
      setMaximumTaskspaceVelocity(1.5, 0.5);
      setFilterBreakFrequencyForBaseFrameUpdater(1.0);
      setEnableFeedbackControl(false);
      setTaskspaceProportionalGainsForFeedbackControl(0.3, 0.3);
      setTaskspaceVelocityFromErrorFilterBreakFrequency(Double.POSITIVE_INFINITY);
   }

   public void setSelectionMatrix(DenseMatrix64F selectionMatrix)
   {
      inverseJacobianSolver.setSelectionMatrix(selectionMatrix);
   }

   public void setFullyConstrained()
   {
      inverseJacobianSolver.setSelectionMatrixForFullConstraint();
   }

   public void setControlFrameFixedInEndEffector(ReferenceFrame controlFrame)
   {
      originalControlFrame = controlFrame;
      localControlFrame.update();
      jacobian.changeFrame(localControlFrame);
   }

   public void initialize(DenseMatrix64F jointAngles)
   {
      setLocalBaseFrameToActualAndResetFilters();
      ScrewTools.setJointPositions(localJoints, jointAngles);
      localJoints[0].updateFramesRecursively();
   }

   public void initializeFromDesiredJointAngles()
   {
      setLocalBaseFrameToActualAndResetFilters();
      setLocalJointAnglesToDesiredJointAngles();
   }

   public void initializeFromCurrentJointAngles()
   {
      setLocalBaseFrameToActualAndResetFilters();
      setLocalJointAnglesToCurrentJointAngles();
   }

   private void setLocalJointAnglesToDesiredJointAngles()
   {
      for (int i = 0; i < numberOfDoF; i++)
      {
         localJoints[i].setQ(originalJoints[i].getqDesired());
         localJoints[i].getFrameAfterJoint().update();
      }
   }

   private void setLocalJointAnglesToCurrentJointAngles()
   {
      for (int i = 0; i < numberOfDoF; i++)
      {
         localJoints[i].setQ(originalJoints[i].getQ());
         localJoints[i].getFrameAfterJoint().update();
      }
   }

   public void setSecondaryObjective(SecondaryObjective secondaryObjective)
   {
      this.currentSecondaryObjective.set(secondaryObjective);
   }

   public void setJointAngleRegularizationWeight(double weight)
   {
      jointAngleRegularizationWeight.set(weight);
   }

   public void setPrivilegedJointPositionsToMidRange()
   {
      for (int i = 0; i < numberOfDoF; i++)
         yoPrivilegedJointPositions[i].set(jointAnlgesAtMidRangeOfMotion.get(i, 0));
   }

   public void setMaximumJointVelocity(double maximumJointVelocity)
   {
      this.maximumJointVelocity.set(maximumJointVelocity);
   }

   public void setMaximumJointAngleCorrection(double maximumJointAngleCorrection)
   {
      this.maximumJointVelocity.set(maximumJointAngleCorrection / controlDT);
   }

   public void setMaximumJointAcceleration(double maximumJointAcceleration)
   {
      this.maximumJointAcceleration.set(maximumJointAcceleration);
   }

   public void setMaximumTaskspaceVelocity(double maximumAngularVelocity, double maximumLinearVelocity)
   {
      this.maximumTaskspaceAngularVelocityMagnitude.set(maximumAngularVelocity);
      this.maximumTaskspaceLinearVelocityMagnitude.set(maximumLinearVelocity);
   }

   public void setTaskspaceVelocityFromErrorFilterBreakFrequency(double breakFrequency)
   {
      alphaSpatialVelocityFromError.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(breakFrequency, controlDT));
   }

   public void setFilterBreakFrequencyForBaseFrameUpdater(double breakFrequency)
   {
      alphaBaseParentJointPose.set(AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(breakFrequency, controlDT));
   }

   public void setEnableFeedbackControl(boolean enable)
   {
      enableFeedbackControl.set(enable);
   }

   public void setTaskspaceProportionalGainsForFeedbackControl(double kPAngularError, double kPLinearError)
   {
      kpTaskspaceAngularError.set(kPAngularError);
      kpTaskspaceLinearError.set(kPLinearError);
   }

   public void compute(FramePoint desiredPosition, FrameOrientation desiredOrientation, FrameVector desiredLinearVelocity, FrameVector desiredAngularVelocity)
   {
      desiredControlFramePose.setPoseIncludingFrame(desiredPosition, desiredOrientation);
      desiredControlFrameTwist.set(originalEndEffectorFrame, originalBaseFrame, originalControlFrame, desiredLinearVelocity, desiredAngularVelocity);

      compute(desiredControlFramePose, desiredControlFrameTwist);
   }

   public void compute(FramePose desiredPose, Twist desiredTwist)
   {
      jacobian.compute();

      desiredControlFramePose.setPoseIncludingFrame(desiredPose);
      desiredControlFrameTwist.set(desiredTwist);
      computeJointAnglesAndVelocities(desiredControlFramePose, desiredControlFrameTwist);
   }

   private void computeJointAnglesAndVelocities(FramePose desiredControlFramePose, Twist desiredControlFrameTwist)
   {
      if (enableFeedbackControl.getBooleanValue())
         setLocalJointAnglesToCurrentJointAngles();

      updateLocalBaseFrame();

      desiredControlFrameTwist.checkReferenceFramesMatch(originalEndEffectorFrame, originalBaseFrame, originalControlFrame);

      ReferenceFrame originalControlledWithRespectToFrame = desiredControlFramePose.getReferenceFrame();
      ReferenceFrame localControlledWithRespectToFrame = originalToLocalFramesMap.get(originalControlledWithRespectToFrame);
      if (localControlledWithRespectToFrame != null)
      {
         desiredControlFramePose.getPose(desiredControlFrameTransform);
         desiredControlFramePose.setPoseIncludingFrame(localControlledWithRespectToFrame, desiredControlFrameTransform);
      }

      desiredControlFramePose.changeFrame(localControlFrame);

      desiredControlFramePose.getOrientation(errorAxisAngle);
      errorRotationVector.set(errorAxisAngle.getX(), errorAxisAngle.getY(), errorAxisAngle.getZ());
      errorRotationVector.scale(errorAxisAngle.getAngle());

      desiredControlFramePose.getPosition(errorTranslationVector);

      yoErrorRotation.set(errorRotationVector);
      yoErrorTranslation.set(errorTranslationVector);

      if (enableFeedbackControl.getBooleanValue())
      {
         errorRotationVector.scale(kpTaskspaceAngularError.getDoubleValue());
         errorTranslationVector.scale(kpTaskspaceLinearError.getDoubleValue());
      }

      MatrixTools.setDenseMatrixFromTuple3d(spatialVelocityFromError, errorRotationVector, 0, 0);
      MatrixTools.setDenseMatrixFromTuple3d(spatialVelocityFromError, errorTranslationVector, 3, 0);

      CommonOps.scale(1.0 / controlDT, spatialVelocityFromError);

      computeDesiredSpatialVelocityToSolveFor(spatialDesiredVelocity, spatialVelocityFromError, desiredControlFrameTwist);

      if (currentSecondaryObjective.getEnumValue() == SecondaryObjective.TOWARD_RESTING_CONFIGURATION)
         computePrivilegedJointVelocitiesForPriviligedJointAngles(privilegedJointVelocities, jointAngleRegularizationWeight.getDoubleValue(), yoPrivilegedJointPositions);
      else
         computePrivilegedVelocitiesForStayingAwayFromJointLimits(privilegedJointVelocities, jointAngleRegularizationWeight.getDoubleValue());

      inverseJacobianSolver.solveUsingNullspaceMethod(spatialDesiredVelocity, jacobian.getJacobianMatrix(), privilegedJointVelocities);
      desiredJointVelocities.set(inverseJacobianSolver.getJointspaceVelocity());

      if ( Double.isNaN(desiredJointVelocities.get(0)) )
         throw new RuntimeException("Invalid computed desired joint velocities: " + desiredJointVelocities.toString());
      
      subspaceSpatialError.reshape(inverseJacobianSolver.getNumberOfConstraints(), 1);
      subspaceSpatialError.set(inverseJacobianSolver.getSubspaceSpatialVelocity());
      CommonOps.scale(controlDT, subspaceSpatialError);

      for (int i = 0; i < numberOfDoF; i++)
      {
         OneDoFJoint joint = localJoints[i];
         double qDotDesired = MathTools.clipToMinMax(desiredJointVelocities.get(i, 0), maximumJointVelocity.getDoubleValue());
         double qDotDotDesired = (qDotDesired - joint.getQd()) / controlDT;
         qDotDotDesired = MathTools.clipToMinMax(qDotDotDesired, maximumJointAcceleration.getDoubleValue());
         qDotDesired = joint.getQd() + qDotDotDesired * controlDT;

         double qDesired = joint.getQ() + qDotDesired * controlDT;
         qDesired = MathTools.clipToMinMax(qDesired, joint.getJointLimitLower(), joint.getJointLimitUpper());
         qDotDesired = (qDesired - joint.getQ()) / controlDT;
         qDotDotDesired = (qDotDesired - joint.getQd()) / controlDT;

         joint.setQ(qDesired);
         joint.setQd(qDotDesired);
         joint.setQdd(qDotDotDesired);

         desiredJointAngles.set(i, qDesired);
         desiredJointVelocities.set(i, 0, qDotDesired);
         desiredJointAccelerations.set(i, 0, qDotDotDesired);

         joint.getFrameAfterJoint().update();
      }
   }

   private void updateLocalBaseFrame()
   {
      originalBasePose.setToZero(originalBaseParentJointFrame);
      originalBasePose.changeFrame(worldFrame);
      
      originalBasePose.getPoseIncludingFrame(baseParentJointFramePosition, baseParentJointFrameOrientation);
      yoBaseParentJointFrameOrientation.set(baseParentJointFrameOrientation);
      yoBaseParentJointFramePosition.set(baseParentJointFramePosition);
      
      yoBaseParentJointFrameOrientationFiltered.update();
      yoBaseParentJointFramePositionFiltered.update();

      yoBaseParentJointFrameOrientationFiltered.getFrameOrientationIncludingFrame(baseParentJointFrameOrientation);
      yoBaseParentJointFramePositionFiltered.getFrameTupleIncludingFrame(baseParentJointFramePosition);

      localBaseParentJointFrame.setPoseAndUpdate(baseParentJointFramePosition, baseParentJointFrameOrientation);
      updateFrames();
   }

   private void setLocalBaseFrameToActualAndResetFilters()
   {
      yoBaseParentJointFrameOrientationFiltered.reset();
      yoBaseParentJointFramePositionFiltered.reset();
      updateLocalBaseFrame();
   }

   private void computeDesiredSpatialVelocityToSolveFor(DenseMatrix64F spatialDesiredVelocityToPack, DenseMatrix64F spatialVelocityFromError, Twist desiredControlFrameTwist)
   {
      // Clip to maximum velocity
      clipSpatialVector(spatialVelocityFromError, maximumTaskspaceAngularVelocityMagnitude.getDoubleValue(), maximumTaskspaceLinearVelocityMagnitude.getDoubleValue());

      // Update YoVariables for the velocity
      getAngularAndLinearPartsFromSpatialVector(yoAngularVelocityFromError, yoLinearVelocityFromError, spatialVelocityFromError);

      filteredAngularVelocityFromError.update();
      filteredLinearVelocityFromError.update();

      setSpatialVectorFromAngularAndLinearParts(spatialVelocityFromError, filteredAngularVelocityFromError, filteredLinearVelocityFromError);

      desiredControlFrameTwist.packMatrix(spatialDesiredVelocityToPack, 0);
      CommonOps.add(spatialVelocityFromError, spatialDesiredVelocityToPack, spatialDesiredVelocityToPack);
   }

   private final FrameVector angularPart = new FrameVector();
   private final FrameVector linearPart = new FrameVector();

   private void clipSpatialVector(DenseMatrix64F spatialVectorToClip, double maximumAngularMagnitude, double maximumLinearMagnitude)
   {
      getAngularAndLinearPartsFromSpatialVector(angularPart, linearPart, spatialVectorToClip);

      clipToVectorMagnitude(maximumAngularMagnitude, angularPart);
      clipToVectorMagnitude(maximumLinearMagnitude, linearPart);

      setSpatialVectorFromAngularAndLinearParts(spatialVectorToClip, angularPart, linearPart);
   }

   @SuppressWarnings("unused")
   private void timeDerivative(DenseMatrix64F spatialVectorRateToPack, DenseMatrix64F spatialVector, DenseMatrix64F previousSpatialVector)
   {
      CommonOps.subtract(spatialVector, previousSpatialVector, spatialVectorRateToPack);
      CommonOps.scale(1.0 / controlDT, spatialVectorRateToPack);
   }

   @SuppressWarnings("unused")
   private void timeIntegration(DenseMatrix64F spatialVectorToPack, DenseMatrix64F previousSpatialVector, DenseMatrix64F spatialVectorRate)
   {
      CommonOps.add(previousSpatialVector, controlDT, spatialVectorRate, spatialVectorToPack);
   }

   private void getAngularAndLinearPartsFromSpatialVector(YoFrameVector angularPartToPack, YoFrameVector linearPartToPack, DenseMatrix64F spatialVector)
   {
      getAngularAndLinearPartsFromSpatialVector(angularPart, linearPart, spatialVector);
      angularPartToPack.setAndMatchFrame(angularPart);
      linearPartToPack.setAndMatchFrame(linearPart);
   }

   private void getAngularAndLinearPartsFromSpatialVector(FrameVector angularPartToPack, FrameVector linearPartToPack, DenseMatrix64F spatialVector)
   {
      MatrixTools.extractFrameTupleFromEJMLVector(angularPartToPack, spatialVector, localControlFrame, 0);
      MatrixTools.extractFrameTupleFromEJMLVector(linearPartToPack, spatialVector, localControlFrame, 3);
   }

   private void setSpatialVectorFromAngularAndLinearParts(DenseMatrix64F spatialVectorToPack, YoFrameVector yoAngularPart, YoFrameVector yoLinearPart)
   {
      yoAngularPart.getFrameTupleIncludingFrame(angularPart);
      yoLinearPart.getFrameTupleIncludingFrame(linearPart);
      MatrixTools.insertFrameTupleIntoEJMLVector(angularPart, spatialVectorToPack, 0);
      MatrixTools.insertFrameTupleIntoEJMLVector(linearPart, spatialVectorToPack, 3);
   }

   private void setSpatialVectorFromAngularAndLinearParts(DenseMatrix64F spatialVectorToPack, FrameVector angularPart, FrameVector linearPart)
   {
      MatrixTools.insertFrameTupleIntoEJMLVector(angularPart, spatialVectorToPack, 0);
      MatrixTools.insertFrameTupleIntoEJMLVector(linearPart, spatialVectorToPack, 3);
   }

   private void clipToVectorMagnitude(double maximumMagnitude, FrameVector frameVectorToClip)
   {
      double magnitude = frameVectorToClip.length();
      if (magnitude > maximumMagnitude)
      {
         frameVectorToClip.scale(maximumMagnitude / magnitude);
      }
   }

   private void computePrivilegedJointVelocitiesForPriviligedJointAngles(DenseMatrix64F privilegedJointVelocitiesToPack, double weight, DoubleYoVariable[] yoPrivilegedJointPositions)
   {
      for (int i = 0; i < numberOfDoF; i++)
      {
         privilegedJointVelocitiesToPack.set(i, 0, - 2.0 * weight * (localJoints[i].getQ() - yoPrivilegedJointPositions[i].getDoubleValue()) / jointSquaredRangeOfMotions.get(i, 0));
      }
   }

   /**
    * Compute the gradient of q times the p-norm |q - q_midRange|_p
    * @param privilegedJointVelocitiesToPack
    * @param weight
    */
   private void computePrivilegedVelocitiesForStayingAwayFromJointLimits(DenseMatrix64F privilegedJointVelocitiesToPack, double weight)
   {
      int p = exponentForPNorm.getIntegerValue();

      double sumOfPows = 0.0;
      double pThRootOfSumOfPows = 0.0;
      
      for (int i = 0; i < numberOfDoF; i++)
      {
         sumOfPows += MathTools.powWithInteger(Math.abs(localJoints[i].getQ() - jointAnlgesAtMidRangeOfMotion.get(i, 0)), p);
      }

      pThRootOfSumOfPows = Math.pow(sumOfPows, 1.0 / ((double) p));

      for (int i = 0; i < numberOfDoF; i++)
      {
         double numerator = MathTools.powWithInteger(Math.abs(localJoints[i].getQ() - jointAnlgesAtMidRangeOfMotion.get(i, 0)), p - 1) * pThRootOfSumOfPows;
         double qDotPrivileged = - weight * numerator / sumOfPows;
         privilegedJointVelocitiesToPack.set(i, 0, qDotPrivileged);
      }
   }

   private void updateFrames()
   {
      localJoints[0].getPredecessor().updateFramesRecursively();
   }

   public ReferenceFrame getControlFrame()
   {
      return originalControlFrame;
   }

   public DenseMatrix64F getSubspaceSpatialError()
   {
      return subspaceSpatialError;
   }

   public double getSpatialErrorScalar()
   {
      return NormOps.normP2(subspaceSpatialError);
   }

   public DenseMatrix64F getDesiredJointAngles()
   {
      return desiredJointAngles;
   }

   public DenseMatrix64F getDesiredJointVelocities()
   {
      return desiredJointVelocities;
   }

   public DenseMatrix64F getDesiredJointAccelerations()
   {
      return desiredJointAccelerations;
   }

   public void packDesiredJointAnglesIntoOneDoFJoints(OneDoFJoint[] joints)
   {
      ScrewTools.setDesiredJointPositions(joints, desiredJointAngles);
   }

   public void packDesiredJointVelocitiesIntoOneDoFJoints(OneDoFJoint[] joints)
   {
      ScrewTools.setDesiredJointVelocities(joints, desiredJointVelocities);
   }

   public void packDesiredJointAccelerationsIntoOneDoFJoints(OneDoFJoint[] joints)
   {
      ScrewTools.setDesiredAccelerations(joints, desiredJointAccelerations);
   }

   public double computeDeterminant()
   {
      jacobian.compute();
      return inverseJacobianSolver.computeDeterminant(jacobian.getJacobianMatrix());
   }

   public double getLastComputedDeterminant()
   {
      return inverseJacobianSolver.getLastComputedDeterminant();
   }

   public void getDesiredEndEffectorPoseFromQDesireds(FramePose desiredPose, ReferenceFrame desiredFrame)
   {
      desiredPose.setToZero(localControlFrame);
      desiredPose.changeFrame(desiredFrame);
   }
   
   public FrameVector getInitialHandPoseVelocity(ReferenceFrame referenceFrame)
   {
      return  new FrameVector(referenceFrame);
   }
   
   public FrameVector getInitialHandPoseAngularVelocity(ReferenceFrame referenceFrame)
   {
      return  new FrameVector(referenceFrame);
   }
}