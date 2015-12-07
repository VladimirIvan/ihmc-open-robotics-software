package us.ihmc.quadrupedRobotics.stateEstimator.kinematicsBased;

import java.util.ArrayList;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.quadrupedRobotics.sensorProcessing.sensorProcessors.FootSwitchUpdater;
import us.ihmc.quadrupedRobotics.stateEstimator.QuadrupedStateEstimator;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.time.TimeTools;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicReferenceFrame;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation.JointStateUpdater;

public class QuadrupedKinematicsBasedStateEstimator implements QuadrupedStateEstimator
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final YoGraphicsListRegistry yoGraphicsListRegistry;

   private final SDFFullRobotModel sdfFullRobotModelForViz;

   private final JointStateUpdater jointStateUpdater;
   private final FootSwitchUpdater footSwitchUpdater;
   private final CenterOfMassLinearStateUpdater comLinearStateUpdater;

   private final SensorOutputMapReadOnly sensorOutputMapReadOnly;

   private final ArrayList<YoGraphicReferenceFrame> graphicReferenceFrames = new ArrayList<>();

   public QuadrupedKinematicsBasedStateEstimator(FullInverseDynamicsStructure inverseDynamicsStructure, SensorOutputMapReadOnly sensorOutputMapReadOnly,
         FootSwitchUpdater footSwitchUpdater, JointStateUpdater jointStateUpdater, CenterOfMassLinearStateUpdater comLinearStateUpdater,
         SDFFullRobotModel sdfFullRobotModelForViz, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.yoGraphicsListRegistry = yoGraphicsListRegistry;

      this.jointStateUpdater = jointStateUpdater;

      this.footSwitchUpdater = footSwitchUpdater;
      this.sensorOutputMapReadOnly = sensorOutputMapReadOnly;

      this.comLinearStateUpdater = comLinearStateUpdater;

      this.sdfFullRobotModelForViz = sdfFullRobotModelForViz;

      if (this.sdfFullRobotModelForViz != null)
         initializeVisualization();

      parentRegistry.addChild(registry);
   }

   private void initializeVisualization()
   {
      OneDoFJoint[] oneDoFJoints = sdfFullRobotModelForViz.getOneDoFJoints();
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         String prefix = "StateEstimator" + oneDoFJoints[i].getName();
         ReferenceFrame referenceFrame = oneDoFJoints[i].getFrameBeforeJoint();
         YoGraphicReferenceFrame vizReferenceFrame = new YoGraphicReferenceFrame(prefix, referenceFrame, registry, 0.4, YoAppearance.AliceBlue());
         graphicReferenceFrames.add(vizReferenceFrame);
         yoGraphicsListRegistry.registerYoGraphic("KinematicsBasedStateEstimator", vizReferenceFrame);
      }
   }

   @Override
   public boolean isFootInContact(RobotQuadrant quadrant)
   {
      return footSwitchUpdater.isFootInContactWithGround(quadrant);
   }

   public void initialize()
   {
      jointStateUpdater.initialize();
      comLinearStateUpdater.initialize();
   }

   @Override
   public void doControl()
   {
      jointStateUpdater.updateJointState();
      comLinearStateUpdater.updateCenterOfMassLinearState();

      sdfFullRobotModelForViz.updateFrames();
      updateViz();
   }

   private void updateViz()
   {
      for (int i = 0; i < graphicReferenceFrames.size(); i++)
         graphicReferenceFrames.get(i).update();
   }

   @Override
   public double getCurrentTime()
   {
      return TimeTools.nanoSecondstoSeconds(sensorOutputMapReadOnly.getTimestamp());
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }
}
