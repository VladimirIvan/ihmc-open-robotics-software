package us.ihmc.simulationconstructionset.util.environments;

import java.util.ArrayList;

import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.Link;
import us.ihmc.simulationconstructionset.PinJoint;
import us.ihmc.utilities.math.RotationalInertiaCalculator;
import us.ihmc.utilities.math.geometry.FrameCylinder3d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameTorus3d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

public class ContactableSteeringWheelRobot extends ContactablePinJointRobot
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final double DEFAULT_DAMPING = 3;

   private String name;

   protected double steeringWheelRadius;
   private double steeringColunmLength;
   private double steeringWheelThickness;

   protected double spokesThickness;

   private FramePose steeringWheelPoseInWorld = new FramePose();

   private double totalNumberOfPossibleTurns;

   private final DoubleYoVariable steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion;

   private double mass;
   private Matrix3d inertiaMatrix;

   private FrameTorus3d steeringWheelTorus;
   protected ArrayList<FrameCylinder3d> spokesCylinders = new ArrayList<FrameCylinder3d>();

   protected Link steeringWheelLink;
   private PinJoint steeringWheelPinJoint;
   protected Graphics3DObject steeringWheelLinkGraphics = new Graphics3DObject();
   private final DoubleYoVariable steeringDamping;

   protected PoseReferenceFrame steeringWheelFrame;

   private final RigidBodyTransform originalSteeringWheelPose = new RigidBodyTransform();

   public static ContactableSteeringWheelRobot createPolarisSteeringWheel(double x, double y, double z, double yaw, double pitch)
   {
      FramePose steeringWheelPoseInWorld = new FramePose(worldFrame);
      steeringWheelPoseInWorld.setPosition(x, y, z);
      steeringWheelPoseInWorld.setOrientation(yaw, pitch, 0.0);
      double steeringWheelRadius = 0.175;
      double steerigColunmLength = 0.20;
      double steeringWheelThickness = 0.03;
      double spokeThickness = 0.03;
      double totalNumberOfPossibleTurns = 3.25;
      double mass = 5.0;
      return new ContactableSteeringWheelRobot("PolarisSteeringWheel", steeringWheelRadius, steerigColunmLength, steeringWheelThickness, spokeThickness, steeringWheelPoseInWorld, totalNumberOfPossibleTurns, mass);
   }

   public ContactableSteeringWheelRobot(String name, double steeringWheelRadius, double steerigColunmLength, double steeringWheelThickness, double spokesThickness,
         FramePose steeringWheelPoseInWorld, double totalNumberOfPossibleTurns, double mass)
   {
      super(name);
      this.name = name;
      setProperties(steeringWheelRadius, steerigColunmLength, steeringWheelThickness, spokesThickness, totalNumberOfPossibleTurns, mass);
      setPoseInWorld(steeringWheelPoseInWorld);
      setMass(mass);
      steeringDamping = new DoubleYoVariable(getName() + "SteeringDamping", yoVariableRegistry);
      steeringDamping.set(DEFAULT_DAMPING);
      steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion = new DoubleYoVariable("steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion", yoVariableRegistry);
      steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion.set(0.0);
   }

   public void setProperties(double steeringWheelRadius, double steerinWheelColunmLength, double steeringWheelThickness, double spokesThickness,
         double totalNumberOfPossibleTurns, double mass)
   {
      this.steeringWheelRadius = steeringWheelRadius;
      this.steeringColunmLength = steerinWheelColunmLength;
      this.steeringWheelThickness = steeringWheelThickness;

      this.spokesThickness = spokesThickness;

      this.totalNumberOfPossibleTurns = totalNumberOfPossibleTurns;

      this.mass = mass;
   }

   private FramePoint spinnerHandleCenter = null;

   public void addSpinnerHandle()
   {
      addSpinnerHandle(0.0, 1.0, 0.15, spokesThickness / 2.0);
   }

   public void addSpinnerHandle(double angleOnSteeringWheelingInDegrees, double percentOfSteeringWheelRadius, double handleLength, double handleRadius)
   {
      RigidBodyTransform transform = new RigidBodyTransform();

      double angleOnSteeringWheel = Math.toRadians(angleOnSteeringWheelingInDegrees);
      double distanceFromCenter = percentOfSteeringWheelRadius * steeringWheelRadius;
      double xHandle = distanceFromCenter * Math.cos(angleOnSteeringWheel);
      double yHandle = distanceFromCenter * Math.sin(angleOnSteeringWheel);

      transform.setTranslation(xHandle, yHandle, 0.0);

      FrameCylinder3d spinnerHandleCylinder = new FrameCylinder3d(steeringWheelFrame, transform, handleLength, handleRadius);
      spokesCylinders.add(spinnerHandleCylinder);

      steeringWheelLinkGraphics.translate(xHandle, yHandle, 0.0);
      steeringWheelLinkGraphics.addCylinder(handleLength, handleRadius, YoAppearance.IndianRed());
      steeringWheelLinkGraphics.translate(-xHandle, -yHandle, 0.0);

      steeringWheelLink.setLinkGraphics(steeringWheelLinkGraphics);

      spinnerHandleCenter = new FramePoint(steeringWheelFrame, xHandle, yHandle, handleLength / 2.0);
   }

   public void createSteeringWheelRobot()
   {
      YoGraphicsListRegistry graphListRegistry = new YoGraphicsListRegistry();

      steeringWheelFrame = new PoseReferenceFrame("steeringWheelFrame", steeringWheelPoseInWorld);
      steeringWheelFrame.getPose(originalSteeringWheelPose);

      //creating the pinJoint 
      Vector3d jointAxisVector = new Vector3d(0.0, 0.0, 1.0);
      RigidBodyTransform steeringWheelTransformToWorld = new RigidBodyTransform();
      steeringWheelFrame.getTransformToDesiredFrame(steeringWheelTransformToWorld, worldFrame);
      steeringWheelTransformToWorld.transform(jointAxisVector);

      Vector3d steeringWheelPositionInWorld = new Vector3d();
      steeringWheelPoseInWorld.getPosition(steeringWheelPositionInWorld);
      steeringWheelPinJoint = new PinJoint("steeringWheelPinJoint", steeringWheelPositionInWorld, this, jointAxisVector);
      steeringWheelPinJoint.setLimitStops(-totalNumberOfPossibleTurns * Math.PI, totalNumberOfPossibleTurns * Math.PI, 1000, 100);
      steeringWheelPinJoint.setDamping(steeringDamping.getDoubleValue());

      //put the graphics frame in the proper orientation
      Matrix3d rotationMatrix = new Matrix3d();
      steeringWheelPoseInWorld.getOrientation(rotationMatrix);
      steeringWheelLinkGraphics.rotate(rotationMatrix);
      RigidBodyTransform rotationTransform = new RigidBodyTransform();
      rotationTransform.setRotation(rotationMatrix);

      //Creating the physical link for the simulation
      steeringWheelLink = new Link("steerinWheelLink");
      steeringWheelLink.setMass(mass);
      steeringWheelLink.setComOffset(new Vector3d(0.0, 0.0, 0.0));

      inertiaMatrix = RotationalInertiaCalculator.getRotationalInertiaMatrixOfTorus(mass, steeringWheelRadius, steeringWheelThickness);
      steeringWheelLink.setMomentOfInertia(inertiaMatrix);
      steeringWheelPinJoint.setLink(steeringWheelLink);
      this.addRootJoint(steeringWheelPinJoint);

      //torus and offsetCylinder
      RigidBodyTransform transform = new RigidBodyTransform();
      RigidBodyTransform invertTransform = new RigidBodyTransform();

      invertTransform.set(transform);
      invertTransform.invert();

      steeringWheelTorus = new FrameTorus3d(steeringWheelFrame, transform, steeringWheelRadius, steeringWheelThickness / 2.0);
      steeringWheelLinkGraphics.transform(transform);
      steeringWheelLinkGraphics.addArcTorus(0.0, 2 * Math.PI, steeringWheelRadius, steeringWheelThickness / 2.0, YoAppearance.DarkRed());
      steeringWheelLinkGraphics.translate(0.0, 0.0, -steeringColunmLength);
      steeringWheelLinkGraphics.addCylinder(steeringColunmLength, spokesThickness / 2.0, YoAppearance.DarkRed());
      steeringWheelLinkGraphics.translate(0.0, 0.0, steeringColunmLength);
      steeringWheelLinkGraphics.transform(invertTransform);

      Quat4d quat = new Quat4d();
      //spokes
      for (int i = 0; i < 3; i++)
      {
         RotationFunctions.setQuaternionBasedOnYawPitchRoll(quat, 0.0, Math.PI / 2.0, i * 2.0 * Math.PI / 4.0 + Math.PI / 2.0);
         transform.setRotation(quat);
         invertTransform.set(transform);
         invertTransform.invert();

         RigidBodyTransform yoGraphicTransform = new RigidBodyTransform();
         yoGraphicTransform.multiply(rotationTransform, transform);

         FrameCylinder3d spokeCylinder = new FrameCylinder3d(steeringWheelFrame, transform, steeringWheelRadius, spokesThickness / 2.0);
         spokesCylinders.add(spokeCylinder);

         steeringWheelLinkGraphics.transform(transform);
         steeringWheelLinkGraphics.addCylinder(steeringWheelRadius, spokesThickness / 2.0, YoAppearance.DarkRed());
         steeringWheelLinkGraphics.transform(invertTransform);
      }

      //setting the graphics for the link
      steeringWheelLink.setLinkGraphics(steeringWheelLinkGraphics);

      yoGraphicsListRegistries.add(graphListRegistry);

      steeringWheelPinJoint.getQ().addVariableChangedListener(new VariableChangedListener()
      {

         @Override
         public void variableChanged(YoVariable<?> v)
         {
            double rangeOfMotion = 2 * Math.PI * totalNumberOfPossibleTurns;
            steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion.set((Math.abs(steeringWheelPinJoint.getQ().getDoubleValue()) / (0.5 * rangeOfMotion)) * 100);
         }
      });
   }

   @Override
   public void updateAllGroundContactPointVelocities()
   {
      RigidBodyTransform pinJointTransform = new RigidBodyTransform();
      RigidBodyTransform newPose = new RigidBodyTransform();
      pinJointTransform.rotZ(steeringWheelPinJoint.getQ().getDoubleValue());
      newPose.multiply(originalSteeringWheelPose, pinJointTransform);
      steeringWheelFrame.setPoseAndUpdate(newPose);

      super.updateAllGroundContactPointVelocities();
   }

   private final FramePoint pointToCheck = new FramePoint();

   @Override
   public boolean isPointOnOrInside(Point3d pointInWorldToCheck)
   {
      pointToCheck.setIncludingFrame(worldFrame, pointInWorldToCheck);
      pointToCheck.changeFrame(steeringWheelFrame);

      if (steeringWheelTorus.isInsideOrOnSurface(pointToCheck))
         return true;
      for (int i = 0; i < spokesCylinders.size(); i++)
      {
         if (spokesCylinders.get(i).isInsideOrOnSurface(pointToCheck))
            return true;
      }
      return false;
   }

   @Override
   public boolean isClose(Point3d pointInWorldToCheck)
   {
      return isPointOnOrInside(pointInWorldToCheck);
   }

   @Override
   public void closestIntersectionAndNormalAt(Point3d intersectionToPack, Vector3d normalToPack, Point3d pointInWorldToCheck)
   {
      FramePoint pointToCheck = new FramePoint(worldFrame, pointInWorldToCheck);
      pointToCheck.changeFrame(steeringWheelFrame);

      if (steeringWheelTorus.checkIfInside(pointToCheck, intersectionToPack, normalToPack))
         return;
      for (int i = 0; i < spokesCylinders.size(); i++)
      {
         if (spokesCylinders.get(i).checkIfInside(pointToCheck, intersectionToPack, normalToPack))
            return;
      }
   }

   @Override
   public PinJoint getPinJoint()
   {
      return steeringWheelPinJoint;
   }

   @Override
   public void getBodyTransformToWorld(RigidBodyTransform transformToWorld)
   {
      transformToWorld.set(originalSteeringWheelPose);
   }

   public double getSteeringWheelRadius()
   {
      return steeringWheelRadius;
   }

   public double getSteeringWheelAngleAsAbsolutePercentageOfRangeOfMotion()
   {
      return steeringWheelAngleAsAbsolutePercentageOfRangeOfMotion.getDoubleValue();
   }

   public PoseReferenceFrame getSteeringWheelFrame()
   {
      return steeringWheelFrame;
   }

   public FramePoint getSpinnerHandleCenter()
   {
      return new FramePoint(spinnerHandleCenter);
   }

   public FrameVector getSteeringWheelAxis()
   {
      return new FrameVector(steeringWheelFrame, 0.0, 0.0, 1.0);
   }

   public FramePoint getSteeringWheelCenter()
   {
      return new FramePoint(steeringWheelFrame, 0.0, 0.0, 0.0);
   }

   public double getNumberOfPossibleTurns()
   {
      return totalNumberOfPossibleTurns;
   }

   @Override
   public void setMass(double mass)
   {
      this.mass = mass;
   }

   @Override
   public void setMomentOfInertia(double Ixx, double Iyy, double Izz)
   {
      inertiaMatrix.setM00(Ixx);
      inertiaMatrix.setM01(0.0);
      inertiaMatrix.setM02(0.0);
      inertiaMatrix.setM10(0.0);
      inertiaMatrix.setM11(Iyy);
      inertiaMatrix.setM12(0.0);
      inertiaMatrix.setM20(0.0);
      inertiaMatrix.setM21(0.0);
      inertiaMatrix.setM22(Izz);
   }

   public void setPoseInWorld(FramePose poseInWorld)
   {
      this.steeringWheelPoseInWorld.setPose(poseInWorld);
   }

   public void setDamping(double dampingValue)
   {
      steeringDamping.set(dampingValue);
   }

   public void setSteeringAngleInDegrees(double steeringAngle)
   {
      steeringWheelPinJoint.setQ(steeringAngle);
   }
}