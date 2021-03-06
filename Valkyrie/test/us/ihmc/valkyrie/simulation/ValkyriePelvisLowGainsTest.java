package us.ihmc.valkyrie.simulation;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCPelvisLowGainsTest;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.screwTheory.InverseDynamicsCalculatorListener;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.tools.testing.TestPlanTarget;


@DeployableTestClass(targets = {TestPlanTarget.Exclude})
public class ValkyriePelvisLowGainsTest extends DRCPelvisLowGainsTest
{
   private final DRCRobotModel robotModel = new ValkyrieRobotModel(DRCRobotModel.RobotTarget.SCS, false);

   @Override
   public DRCRobotModel getRobotModel()
   {
      robotModel.setEnableJointDamping(false);

      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
   
   @Override
   protected DoubleYoVariable getPelvisOrientationErrorVariableName(SimulationConstructionSet scs)
   {
      return (DoubleYoVariable) scs.getVariable("MomentumBasedControllerFactory.PelvisOrientationManager.RootJointAngularAccelerationControlModule.v1PelvisAxisAngleOrientationController",
                                                "v1PelvisOrientationErrorMagnitude");
   }

   @Override
   public String getKpPelvisOrientationName()
   {
      return "kpXYAngularPelvisOrientation";
   }

   @Override
   public String getZetaPelvisOrientationName()
   {
      return "zetaXYAngularPelvisOrientation";
   }

   @Override
   public InverseDynamicsCalculatorListener getInverseDynamicsCalculatorListener(SDFFullRobotModel controllersFullRobotModel, SDFRobot robot)
   {
      return null;
   }
}
