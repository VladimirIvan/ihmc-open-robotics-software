package us.ihmc.commonWalkingControlModules.controlModules.head;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.HeadOrientationControllerParameters;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableHeadTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.solver.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.packetConsumers.HeadTrajectoryMessageSubscriber;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadTrajectoryMessage;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.trajectories.waypoints.MultipleWaypointsOrientationTrajectoryGenerator;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class HeadOrientationManager
{
   private static final double defaultTrajectoryTime = 1.0;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry;
   private final HeadTrajectoryMessageSubscriber headTrajectoryMessageSubscriber;
   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable receivedNewHeadOrientationTime;
   private final MultipleWaypointsOrientationTrajectoryGenerator waypointOrientationTrajectoryGenerator;

   private final BooleanYoVariable isTrackingOrientation;

   private final ReferenceFrame headFrame;
   private final ReferenceFrame chestFrame;

   private final BooleanYoVariable hasBeenInitialized;

   private final FrameOrientation homeOrientation = new FrameOrientation();
   private final FrameOrientation desiredOrientation = new FrameOrientation();
   private final FrameVector desiredAngularVelocity = new FrameVector();
   private final FrameVector feedForwardAngularAcceleration = new FrameVector();

   private final OrientationFeedbackControlCommand orientationFeedbackControlCommand = new OrientationFeedbackControlCommand();

   public HeadOrientationManager(MomentumBasedController momentumBasedController, HeadOrientationControllerParameters headOrientationControllerParameters,
         YoOrientationPIDGainsInterface gains, HeadTrajectoryMessageSubscriber headTrajectoryMessageSubscriber, double[] initialDesiredHeadYawPitchRoll,
         YoVariableRegistry parentRegistry)
   {
      registry = new YoVariableRegistry(getClass().getSimpleName());

      this.yoTime = momentumBasedController.getYoTime();
      FullHumanoidRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();

      RigidBody head = fullRobotModel.getHead();
      RigidBody chest = fullRobotModel.getChest();
      RigidBody elevator = fullRobotModel.getElevator();
      orientationFeedbackControlCommand.set(elevator, head);
      orientationFeedbackControlCommand.setGains(gains);
      chestFrame = chest.getBodyFixedFrame();
      headFrame = head.getBodyFixedFrame();

      this.headTrajectoryMessageSubscriber = headTrajectoryMessageSubscriber;
      parentRegistry.addChild(registry);

      receivedNewHeadOrientationTime = new DoubleYoVariable("receivedNewHeadOrientationTime", registry);
      isTrackingOrientation = new BooleanYoVariable("isTrackingOrientation", registry);

      hasBeenInitialized = new BooleanYoVariable("hasHeadOrientationManagerBeenInitialized", registry);
      hasBeenInitialized.set(false);

      homeOrientation.setIncludingFrame(chestFrame, initialDesiredHeadYawPitchRoll);
      waypointOrientationTrajectoryGenerator = new MultipleWaypointsOrientationTrajectoryGenerator("head", 15, true, chestFrame, registry);
      waypointOrientationTrajectoryGenerator.registerNewTrajectoryFrame(worldFrame);
   }

   public void initialize()
   {
      if (hasBeenInitialized.getBooleanValue())
         return;

      hasBeenInitialized.set(true);

      desiredOrientation.setToZero(headFrame);
      goToHome(defaultTrajectoryTime, desiredOrientation);
   }

   public void goToHomeFromCurrentDesired(double trajectoryTime)
   {
      waypointOrientationTrajectoryGenerator.getOrientation(desiredOrientation);
      goToHome(trajectoryTime, desiredOrientation);
   }

   public void goToHome(double trajectoryTime, FrameOrientation initialOrientation)
   {
      initialOrientation.changeFrame(chestFrame);
      desiredAngularVelocity.setToZero(chestFrame);

      waypointOrientationTrajectoryGenerator.switchTrajectoryFrame(chestFrame);
      waypointOrientationTrajectoryGenerator.appendWaypoint(0.0, initialOrientation, desiredAngularVelocity);
      waypointOrientationTrajectoryGenerator.appendWaypoint(trajectoryTime, homeOrientation, desiredAngularVelocity);
      waypointOrientationTrajectoryGenerator.initialize();

      receivedNewHeadOrientationTime.set(yoTime.getDoubleValue());
      isTrackingOrientation.set(true);
   }

   public void compute()
   {
      handleHeadTrajectoryMessages();

      if (isTrackingOrientation.getBooleanValue())
      {
         double deltaTime = yoTime.getDoubleValue() - receivedNewHeadOrientationTime.getDoubleValue();
         waypointOrientationTrajectoryGenerator.compute(deltaTime);
         isTrackingOrientation.set(!waypointOrientationTrajectoryGenerator.isDone());
      }
      waypointOrientationTrajectoryGenerator.getOrientation(desiredOrientation);
      desiredAngularVelocity.setToZero(worldFrame);
      feedForwardAngularAcceleration.setToZero(worldFrame);
      orientationFeedbackControlCommand.changeFrameAndSet(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);
   }

   private void handleHeadTrajectoryMessages()
   {
      if (headTrajectoryMessageSubscriber == null || !headTrajectoryMessageSubscriber.isNewTrajectoryMessageAvailable())
         return;

      HeadTrajectoryMessage message = headTrajectoryMessageSubscriber.pollMessage();
      handleHeadTrajectoryMessage(message);
   }

   public void handleHeadTrajectoryMessage(HeadTrajectoryMessage message)
   {
      receivedNewHeadOrientationTime.set(yoTime.getDoubleValue());

      if (message.getTrajectoryPoint(0).getTime() > 1.0e-5)
      {
         waypointOrientationTrajectoryGenerator.getOrientation(desiredOrientation);
         desiredOrientation.changeFrame(worldFrame);
         desiredAngularVelocity.setToZero(worldFrame);

         waypointOrientationTrajectoryGenerator.switchTrajectoryFrame(worldFrame);
         waypointOrientationTrajectoryGenerator.clear();
         waypointOrientationTrajectoryGenerator.appendWaypoint(0.0, desiredOrientation, desiredAngularVelocity);
      }
      else
      {
         waypointOrientationTrajectoryGenerator.switchTrajectoryFrame(worldFrame);
         waypointOrientationTrajectoryGenerator.clear();
      }

      waypointOrientationTrajectoryGenerator.appendWaypoints(message);
      waypointOrientationTrajectoryGenerator.changeFrame(chestFrame);
      waypointOrientationTrajectoryGenerator.initialize();
      isTrackingOrientation.set(true);
   }
   
   public void handleHeadTrajectoryMessage(ModifiableHeadTrajectoryMessage message)
   {
      receivedNewHeadOrientationTime.set(yoTime.getDoubleValue());
      
      if (message.getTrajectoryPoint(0).getTime() > 1.0e-5)
      {
         waypointOrientationTrajectoryGenerator.getOrientation(desiredOrientation);
         desiredOrientation.changeFrame(worldFrame);
         desiredAngularVelocity.setToZero(worldFrame);
         
         waypointOrientationTrajectoryGenerator.switchTrajectoryFrame(worldFrame);
         waypointOrientationTrajectoryGenerator.clear();
         waypointOrientationTrajectoryGenerator.appendWaypoint(0.0, desiredOrientation, desiredAngularVelocity);
      }
      else
      {
         waypointOrientationTrajectoryGenerator.switchTrajectoryFrame(worldFrame);
         waypointOrientationTrajectoryGenerator.clear();
      }
      
      waypointOrientationTrajectoryGenerator.appendWaypoints(message);
      waypointOrientationTrajectoryGenerator.changeFrame(chestFrame);
      waypointOrientationTrajectoryGenerator.initialize();
      isTrackingOrientation.set(true);
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return null;
   }

   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return orientationFeedbackControlCommand;
   }
}
