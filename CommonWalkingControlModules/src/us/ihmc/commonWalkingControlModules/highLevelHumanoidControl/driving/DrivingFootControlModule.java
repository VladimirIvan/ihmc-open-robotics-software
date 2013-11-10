package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.driving;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.taskExecutor.Task;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.taskExecutor.TaskExecutor;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.commonWalkingControlModules.trajectories.StraightLinePositionTrajectoryGenerator;
import us.ihmc.packets.LowLevelDrivingAction;
import us.ihmc.packets.LowLevelDrivingStatus;
import us.ihmc.utilities.io.streamingData.QueueBasedStreamingDataProducer;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.SpatialMotionVector;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.utilities.screwTheory.Wrench;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.AxisAngleOrientationController;
import com.yobotics.simulationconstructionset.util.EuclideanPositionController;
import com.yobotics.simulationconstructionset.util.GainCalculator;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsList;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicReferenceFrame;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.trajectory.AverageVelocityTrajectoryTimeProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.PositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.YoPositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoVariableDoubleProvider;

/**
 * @author twan
 *         Date: 6/6/13
 */
public class DrivingFootControlModule
{
   private final YoVariableRegistry registry;
   private final GeometricJacobian footJacobian;
   private final FramePoint toePoint;
   private final EuclideanPositionController toePointPositionController;
   private final MomentumBasedController momentumBasedController;

   private final QueueBasedStreamingDataProducer<LowLevelDrivingStatus> statusProducer;

   private final FramePoint desiredPosition = new FramePoint();
   private final FrameVector desiredVelocity = new FrameVector();
   private final FrameVector feedForward = new FrameVector();

   private final YoFramePoint desiredPositionYoFramePoint;
   private final YoFrameVector desiredVelocityYoFrameVector;

   private final FrameVector currentVelocity = new FrameVector();
   private final FrameVector currentAngularVelocity = new FrameVector();

   private final PositionTrajectoryGenerator positionTrajectoryGenerator;
   private final YoFramePoint initialToePointPosition;
   private final YoFramePoint finalToePointPosition;
   private final DoubleYoVariable footPitch;
   private final DoubleYoVariable footRoll;

   private final DoubleYoVariable trajectoryInitializationTime;
   private final DoubleYoVariable time;

   private final AxisAngleOrientationController orientationController;
   private final DenseMatrix64F footOrientationSelectionMatrix;
   private final DenseMatrix64F footOrientationNullspaceMultipliers = new DenseMatrix64F(0, 1);

   private final SpatialAccelerationVector footOrientationControlSpatialAcceleration;
   private final TaskspaceConstraintData footOrientationTaskspaceConstraintData = new TaskspaceConstraintData();

   private final FrameOrientation desiredOrientation;
   private final FrameVector desiredAngularVelocity;
   private final FrameVector feedForwardAngularAcceleration;

   private final TwistCalculator twistCalculator;
   private final Twist currentTwist = new Twist();
   private final FramePoint toePointInBase = new FramePoint();
   private final ReferenceFrame toePointFrame;

   private final DrivingReferenceFrames drivingReferenceFrames;

   private final RigidBody foot;

   private final TaskExecutor taskExecutor = new TaskExecutor();
   private final double pedalY = 0.0;

   private final FrameVector pedalForceToCompensateFor = new FrameVector();
   private final IntegerYoVariable nFootTasksRemaining;
   private final YoVariableDoubleProvider averageVelocityProvider;
   private final List<DynamicGraphicReferenceFrame> dynamicGraphicReferenceFrames = new ArrayList<DynamicGraphicReferenceFrame>();


   public DrivingFootControlModule(FullRobotModel fullRobotModel, ContactablePlaneBody contactablePlaneFoot, MomentumBasedController momentumBasedController,
                                   DrivingReferenceFrames drivingReferenceFrames, double dt, DoubleYoVariable yoTime, TwistCalculator twistCalculator,
                                   YoVariableRegistry parentRegistry, QueueBasedStreamingDataProducer<LowLevelDrivingStatus> statusProducer,
                                   DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.statusProducer = statusProducer;
      this.foot = contactablePlaneFoot.getRigidBody();
      registry = new YoVariableRegistry(foot.getName() + getClass().getSimpleName());
      footJacobian = new GeometricJacobian(fullRobotModel.getElevator(), foot, fullRobotModel.getElevator().getBodyFixedFrame());
      toePoint = getCenterToePoint(contactablePlaneFoot);
//      toePoint = getLeftFrontToePoint(contactablePlaneFoot);
      String toePointName = foot.getName() + "ToePoint";
      Transform3D transform = new Transform3D();
      transform.set(toePoint.getVectorCopy());
      toePointFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(toePointName, toePoint.getReferenceFrame(), transform);
      this.drivingReferenceFrames = drivingReferenceFrames;
      toePointPositionController = new EuclideanPositionController(toePointName, toePointFrame, dt, registry);
      this.momentumBasedController = momentumBasedController;
      this.time = yoTime;
      trajectoryInitializationTime = new DoubleYoVariable(toePointName + "InitializationTime", registry);
      footPitch = new DoubleYoVariable("footPitch", registry);
      footRoll = new DoubleYoVariable("footRoll", registry);

      desiredPositionYoFramePoint = new YoFramePoint("desiredFootPointPosition", ReferenceFrame.getWorldFrame(), registry);
      desiredVelocityYoFrameVector = new YoFrameVector("desiredFootPointVelocity", ReferenceFrame.getWorldFrame(), registry);

      ReferenceFrame vehicleFrame = drivingReferenceFrames.getVehicleFrame();

//      desiredOrientation = new FrameOrientation(drivingReferenceFrames.getObjectFrame(VehicleObject.GAS_PEDAL));    // should be the same for brake pedal

      desiredOrientation = new FrameOrientation(vehicleFrame);

      desiredAngularVelocity = new FrameVector(vehicleFrame);
      feedForwardAngularAcceleration = new FrameVector(vehicleFrame);

      initialToePointPosition = new YoFramePoint(toePointName + "Initial", vehicleFrame, registry);
      finalToePointPosition = new YoFramePoint(toePointName + "Final", vehicleFrame, registry);

      PositionProvider initialPositionProvider = new YoPositionProvider(initialToePointPosition);
      PositionProvider finalPositionProvider = new YoPositionProvider(finalToePointPosition);
      averageVelocityProvider = new YoVariableDoubleProvider("drivingFootAverageVelocity", registry);
      DoubleProvider trajectoryTimeProvider = new AverageVelocityTrajectoryTimeProvider(initialPositionProvider, finalPositionProvider,
                                                 averageVelocityProvider, 0.1);
      this.positionTrajectoryGenerator = new StraightLinePositionTrajectoryGenerator(toePointName + "Trajectory", vehicleFrame, trajectoryTimeProvider,
              initialPositionProvider, finalPositionProvider, registry);

      double kP = 300.0;
      double dampingRatio = 1.0;
      double kD = GainCalculator.computeDerivativeGain(kP, dampingRatio);
      toePointPositionController.setProportionalGains(kP, kP, kP);
      toePointPositionController.setDerivativeGains(kD, kD, kD);


      orientationController = new AxisAngleOrientationController(foot.getName() + "PD", foot.getBodyFixedFrame(), dt, registry);
      double kPOrientationYZ = 50.0;
      double kDOrientationYZ = GainCalculator.computeDerivativeGain(kPOrientationYZ, dampingRatio);

      double kPOrientationX = 50.0;
      double kDOrientationX = GainCalculator.computeDerivativeGain(kPOrientationX, dampingRatio);

      orientationController.setProportionalGains(kPOrientationX, kPOrientationYZ, kPOrientationYZ);
      orientationController.setDerivativeGains(kDOrientationX, kDOrientationYZ, kDOrientationYZ);

      nFootTasksRemaining = new IntegerYoVariable("nFootTasksRemaining", registry);

      footOrientationSelectionMatrix = new DenseMatrix64F(3, SpatialMotionVector.SIZE);
      footOrientationSelectionMatrix.zero();
      footOrientationSelectionMatrix.set(0, 0, 1.0);
      footOrientationSelectionMatrix.set(1, 1, 1.0);
      footOrientationSelectionMatrix.set(2, 2, 1.0);

      footPitch.set(0.3);
      footRoll.set(0.0);

      footOrientationControlSpatialAcceleration = new SpatialAccelerationVector();

      this.twistCalculator = twistCalculator;

//      taskExecutor.setPrintDebugStatements(true);

      parentRegistry.addChild(registry);

      if (dynamicGraphicObjectsListRegistry != null)
      {
         DynamicGraphicReferenceFrame toePointFrameViz = new DynamicGraphicReferenceFrame(toePointFrame, registry, 0.1);
         DynamicGraphicObjectsList list = new DynamicGraphicObjectsList("drivingFootControlModule");
         dynamicGraphicReferenceFrames.add(toePointFrameViz);
         list.add(toePointFrameViz);
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjectsList(list);
      }

   }

// public void addTaskCompletedNotifier(LowLevelDrivingAction action)
// {
//    if(statusProducer != null)
//    {
//       taskExecutor.submit(new NotifyStatusListenerTask<LowLevelDrivingStatus>(statusProducer, new LowLevelDrivingStatus(action, true)));
//    }
// }

   public void reset()
   {
      orientationController.reset();
      toePointPositionController.reset();
   }
   public void holdPosition()
   {
      FramePoint target = new FramePoint(toePoint);
      pedalForceToCompensateFor.setToZero(toePointFrame);
      double averageVelocity = 1.0;    // arbitrary positive number
      moveToPosition(target, pedalForceToCompensateFor, averageVelocity, false, null);
   }

   public void moveToPositionInGasPedalFrame(double z, double averageVelocity, boolean notifyIfDone)
   {
      FramePoint target = new FramePoint(drivingReferenceFrames.getObjectFrame(VehicleObject.GAS_PEDAL), 0.0, pedalY, z);

      pedalForceToCompensateFor.setToZero(drivingReferenceFrames.getObjectFrame(VehicleObject.GAS_PEDAL));
      pedalForceToCompensateFor.setZ(computePedalForceToCompensateFor(z));

      moveToPosition(target, pedalForceToCompensateFor, averageVelocity, notifyIfDone, LowLevelDrivingAction.GASPEDAL);
   }

   public void moveToPositionInBrakePedalFrame(double z, double averageVelocity, boolean notifyIfDone)
   {
      FramePoint target = new FramePoint(drivingReferenceFrames.getObjectFrame(VehicleObject.BRAKE_PEDAL), 0.0, pedalY, z);

      pedalForceToCompensateFor.setToZero(drivingReferenceFrames.getObjectFrame(VehicleObject.BRAKE_PEDAL));
      pedalForceToCompensateFor.setZ(computePedalForceToCompensateFor(z));

      moveToPosition(target, pedalForceToCompensateFor, averageVelocity, notifyIfDone, LowLevelDrivingAction.FOOTBRAKE);
   }

   private double computePedalForceToCompensateFor(double z)
   {
      return MathTools.clipToMinMax(-800.0 * z, 0.0, 10.0);    // from DRCVehiclePlugin.cc
   }

   public void initialize()
   {
      initialToePointPosition.set(toePoint.changeFrameCopy(initialToePointPosition.getReferenceFrame()));
   }

   public void doControl()
   {
      updateVisualizers();
      taskExecutor.doControl();
      footJacobian.compute();
      updateCurrentVelocity();
      doToePositionControl();
      doFootOrientationControl();
   }

   private void updateVisualizers()
   {
      for (DynamicGraphicReferenceFrame dynamicGraphicReferenceFrame : dynamicGraphicReferenceFrames)
      {
         dynamicGraphicReferenceFrame.update();
      }
   }

   private void moveToPosition(FramePoint target, FrameVector forceToCompensateFor, double averageVelocity, boolean notifyIfDone, LowLevelDrivingAction action)
   {
      Task task = new FootControlTask(target, forceToCompensateFor, averageVelocity, notifyIfDone, action);
      nFootTasksRemaining.increment();
      taskExecutor.submit(task);
   }

   private void doToePositionControl()
   {
      positionTrajectoryGenerator.compute(time.getDoubleValue() - trajectoryInitializationTime.getDoubleValue());
      positionTrajectoryGenerator.packLinearData(desiredPosition, desiredVelocity, feedForward);

      FrameVector output = new FrameVector(toePointFrame);
      toePointPositionController.compute(output, desiredPosition, desiredVelocity, currentVelocity, feedForward);
      momentumBasedController.setDesiredPointAcceleration(footJacobian, toePoint, output);

      desiredPosition.changeFrame(desiredPositionYoFramePoint.getReferenceFrame());
      desiredPositionYoFramePoint.set(desiredPosition);

      desiredVelocity.changeFrame(desiredVelocityYoFrameVector.getReferenceFrame());
      desiredVelocityYoFrameVector.set(desiredVelocity);
   }

   private void doFootOrientationControl()
   {
//      Matrix3d rotationMatrix = new Matrix3d();
//      rotationMatrix.setColumn(0, 0.0, 0.0, -1.0);
//      rotationMatrix.setColumn(1, -1.0, 0.0, 0.0);
//      rotationMatrix.setColumn(2, 0.0, 1.0, 0.0);
//
//      Matrix3d postRotation = new Matrix3d();
//      RotationFunctions.setYawPitchRoll(postRotation, 0.0, 0.0, 0.0);
//      rotationMatrix.mul(postRotation);
//
//      desiredOrientation.set(drivingReferenceFrames.getObjectFrame(VehicleObject.GAS_PEDAL), rotationMatrix);

      desiredOrientation.set(drivingReferenceFrames.getVehicleFrame());
      desiredOrientation.setYawPitchRoll(0.0, footPitch.getDoubleValue(), footRoll.getDoubleValue());
      desiredAngularVelocity.setToZero(toePointFrame);
      feedForwardAngularAcceleration.setToZero(toePointFrame);

      FrameVector output = new FrameVector(toePointFrame);
      orientationController.compute(output, desiredOrientation, desiredAngularVelocity, currentAngularVelocity, feedForwardAngularAcceleration);

      footOrientationControlSpatialAcceleration.setToZero(foot.getBodyFixedFrame(), footJacobian.getBaseFrame(), foot.getBodyFixedFrame());
      footOrientationControlSpatialAcceleration.setAngularPart(output.getVector());
      footOrientationTaskspaceConstraintData.set(footOrientationControlSpatialAcceleration, footOrientationNullspaceMultipliers,
              footOrientationSelectionMatrix);
      momentumBasedController.setDesiredSpatialAcceleration(footJacobian, footOrientationTaskspaceConstraintData);
   }

   private void updateCurrentVelocity()
   {
      twistCalculator.packRelativeTwist(currentTwist, footJacobian.getBase(), footJacobian.getEndEffector());
      currentTwist.packAngularPart(currentAngularVelocity);
      currentTwist.changeFrame(footJacobian.getBaseFrame());
      toePointInBase.setAndChangeFrame(toePoint);
      toePointInBase.changeFrame(footJacobian.getBaseFrame());
      currentTwist.packVelocityOfPointFixedInBodyFrame(currentVelocity, toePointInBase);
   }

   private static FramePoint getCenterToePoint(ContactablePlaneBody foot)
   {
      FrameVector forward = new FrameVector(foot.getPlaneFrame(), 1.0, 0.0, 0.0);
      int nToePoints = 2;
      List<FramePoint> toePoints = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(foot.getContactPoints(), forward, nToePoints);
      FramePoint centerToePoint = FramePoint.average(toePoints);

      return centerToePoint;
   }

   private static FramePoint getLeftFrontToePoint(ContactablePlaneBody foot)
   {
      FrameVector forward = new FrameVector(foot.getPlaneFrame(), 1.0, 1.0, 0.0);
      int nToePoints = 1;
      List<FramePoint> toePoints = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(foot.getContactPoints(), forward, nToePoints);

      return toePoints.get(0);
   }

   private class FootControlTask implements Task
   {
      private final FramePoint targetPosition;
      private final FrameVector forceToCompensate;
      private final FrameVector tempVector = new FrameVector();
      private final Wrench wrench = new Wrench();
      private final LowLevelDrivingAction action;
      private final double averageVelocity;
      private boolean notifyIfDone;

      public FootControlTask(FramePoint targetPosition, FrameVector forceToCompensate, double averageVelocity, boolean notifyIfDone,
                             LowLevelDrivingAction action)
      {
         this.targetPosition = targetPosition;
         this.forceToCompensate = new FrameVector(forceToCompensate);
         this.averageVelocity = averageVelocity;
         this.action = action;
         this.notifyIfDone = notifyIfDone;
      }

      public void doTransitionIntoAction()
      {
         averageVelocityProvider.set(averageVelocity);
         DrivingFootControlModule.this.finalToePointPosition.set(
             targetPosition.changeFrameCopy(DrivingFootControlModule.this.finalToePointPosition.getReferenceFrame()));
         positionTrajectoryGenerator.initialize();
         trajectoryInitializationTime.set(time.getDoubleValue());
         nFootTasksRemaining.decrement();
      }

      public void doAction()
      {
         wrench.setToZero(foot.getBodyFixedFrame(), toePointFrame);
         tempVector.setAndChangeFrame(forceToCompensate);
         tempVector.changeFrame(toePointFrame);
         wrench.setLinearPart(tempVector.getVector());
         wrench.changeFrame(foot.getBodyFixedFrame());
         momentumBasedController.setExternalWrenchToCompensateFor(foot, wrench);
      }

      public void doTransitionOutOfAction()
      {
         FramePoint currentDesired = new FramePoint();
         positionTrajectoryGenerator.get(currentDesired);
         currentDesired.changeFrame(initialToePointPosition.getReferenceFrame());
         initialToePointPosition.set(currentDesired);
      }

      public boolean isDone()
      {
         // this is so that the TaskExecutor keeps doing our doAction and doesn't transition into a NullTask, where it doesn't set the external wrench to compensate for.
         boolean newTasksAreAvailable = nFootTasksRemaining.getIntegerValue() > 0;

         if (positionTrajectoryGenerator.isDone() && notifyIfDone)
         {
            statusProducer.queueDataToSend(new LowLevelDrivingStatus(action, true));
            notifyIfDone = false;
         }

         return positionTrajectoryGenerator.isDone() && newTasksAreAvailable;
      }

      @Override
      public String toString()
      {
         return getClass().getSimpleName() + ": targetPosition: " + targetPosition + ", forceToCompensate: " + forceToCompensate;
      }
   }
}
