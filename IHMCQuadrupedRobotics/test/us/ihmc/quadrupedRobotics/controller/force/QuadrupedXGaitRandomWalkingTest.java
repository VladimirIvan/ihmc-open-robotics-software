package us.ihmc.quadrupedRobotics.controller.force;

import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.quadrupedRobotics.QuadrupedForceTestYoVariables;
import us.ihmc.quadrupedRobotics.QuadrupedMultiRobotTestInterface;
import us.ihmc.quadrupedRobotics.QuadrupedTestBehaviors;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.robotics.testing.YoVariableTestGoal;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureException;
import us.ihmc.simulationconstructionset.util.simulationRunner.GoalOrientedTestConductor;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

public abstract class QuadrupedXGaitRandomWalkingTest implements QuadrupedMultiRobotTestInterface
{
   private GoalOrientedTestConductor conductor;
   private QuadrupedForceTestYoVariables variables;

   @Before
   public void setup()
   {
      try
      {
         MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

         QuadrupedTestFactory quadrupedTestFactory = createQuadrupedTestFactory();
         quadrupedTestFactory.setControlMode(QuadrupedControlMode.FORCE);
         quadrupedTestFactory.setGroundContactModelType(QuadrupedGroundContactModelType.FLAT);
         conductor = quadrupedTestFactory.createTestConductor();
         variables = new QuadrupedForceTestYoVariables(conductor.getScs());
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error loading simulation: " + e.getMessage());
      }
   }

   @After
   public void tearDown()
   {
      conductor = null;
      variables = null;

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   private double randomValidVelocity(Random random)
   {
      return random.nextDouble() * 2.0 - 1.0;
   }

   private double randomValidYawRate(Random random)
   {
      return random.nextDouble() * 1.0 - 0.5;
   }

   private double randomSimulationDuration(Random random)
   {
      return random.nextDouble() * 2.0 + 0.25;
   }

   @DeployableTestMethod(estimatedDuration = 100.0, targets = {TestPlanTarget.InDevelopment, TestPlanTarget.Video})
   @Test(timeout = 500000)
   public void testExtremeRandomWalking() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);

      variables.getUserTrigger().set(QuadrupedForceControllerRequestedEvent.REQUEST_XGAIT);

      Random random = new Random(1447L);
      double runningDuration = variables.getYoTime().getDoubleValue();

      for (int i = 0; i < 10; i++)
      {
         runningDuration += randomSimulationDuration(random);
         variables.getYoPlanarVelocityInputX().set(randomValidVelocity(random) * 2.0);
         variables.getYoPlanarVelocityInputZ().set(randomValidYawRate(random) * 2.0);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();
      }

      conductor.concludeTesting();
   }

   @DeployableTestMethod(estimatedDuration = 100.0)
   @Test(timeout = 500000)
   public void testWalkingRandomly() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);

      variables.getUserTrigger().set(QuadrupedForceControllerRequestedEvent.REQUEST_XGAIT);

      Random random = new Random(1547L);
      double runningDuration = variables.getYoTime().getDoubleValue();

      for (int i = 0; i < 10; i++)
      {
         runningDuration += randomSimulationDuration(random);
         variables.getYoPlanarVelocityInputX().set(randomValidVelocity(random));
         variables.getYoPlanarVelocityInputZ().set(randomValidYawRate(random));
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();

         runningDuration += 1.0;
         variables.getYoPlanarVelocityInputX().set(0.0);
         variables.getYoPlanarVelocityInputZ().set(0.0);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();
      }

      conductor.concludeTesting();
   }

   @DeployableTestMethod(estimatedDuration = 75.0)
   @Test(timeout = 600000)
   public void testWalkingAtRandomSpeedsWithStops() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);

      variables.getUserTrigger().set(QuadrupedForceControllerRequestedEvent.REQUEST_XGAIT);

      Random random = new Random(2456L);
      double runningDuration = variables.getYoTime().getDoubleValue();

      for (int i = 0; i < 6; i++)
      {
         double randomSimulationDuration = randomSimulationDuration(random);
         double randomValidVelocity = randomValidVelocity(random);
         runningDuration += randomSimulationDuration;
         variables.getYoPlanarVelocityInputX().set(randomValidVelocity);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();

         runningDuration += 1.0;
         variables.getYoPlanarVelocityInputX().set(0.0);
         variables.getYoPlanarVelocityInputZ().set(0.0);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();
      }

      conductor.concludeTesting();
   }

   @DeployableTestMethod(estimatedDuration = 100.0)
   @Test(timeout = 600000)
   public void testWalkingRandomVelocitiesStoppingAndTurning() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);

      variables.getUserTrigger().set(QuadrupedForceControllerRequestedEvent.REQUEST_XGAIT);

      Random random = new Random(1557L);
      double runningDuration = variables.getYoTime().getDoubleValue();

      for (int i = 0; i < 6; i++)
      {
         double randomSimulationDuration = randomSimulationDuration(random);
         double randomValidVelocity = randomValidVelocity(random);
         runningDuration += randomSimulationDuration;
         variables.getYoPlanarVelocityInputX().set(randomValidVelocity);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();

         runningDuration += 1.0;
         variables.getYoPlanarVelocityInputX().set(0.0);
         variables.getYoPlanarVelocityInputZ().set(0.0);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();

         randomSimulationDuration = randomSimulationDuration(random);
         double randomValidYawRate = randomValidYawRate(random);
         runningDuration += randomSimulationDuration;
         variables.getYoPlanarVelocityInputZ().set(randomValidYawRate);
         conductor.addSustainGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyZ(), 0.3));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getYoTime(), runningDuration));
         conductor.simulate();
      }

      conductor.concludeTesting();
   }
}