package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.humanoidRobotics.communication.controllerAPI.command.ArmDesiredAccelerationsCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.ArmTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.NeckTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.OneDoFJointTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmDesiredAccelerationsMessage.ArmControlMode;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.trajectories.waypoints.SimpleTrajectoryPoint1DList;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class ControllerCommandValidationTools
{
   public static boolean checkArmTrajectoryCommand(OneDoFJoint[] joints, ArmTrajectoryCommand command)
   {
      return checkOneDoFJointTrajectoryCommandList(joints, command.getTrajectoryPointLists());
   }

   public static boolean checkNeckTrajectoryCommand(OneDoFJoint[] joints, NeckTrajectoryCommand command)
   {
      return checkOneDoFJointTrajectoryCommandList(joints, command.getTrajectoryPointLists());
   }

   public static boolean checkArmDesiredAccelerationsCommand(OneDoFJoint[] joints, ArmDesiredAccelerationsCommand command)
   {
      return command.getArmControlMode() != ArmControlMode.USER_CONTROL_MODE || command.getNumberOfJoints() == joints.length;
   }

   public static boolean checkOneDoFJointTrajectoryCommandList(OneDoFJoint[] joints, RecyclingArrayList<OneDoFJointTrajectoryCommand> trajectoryPointLists)
   {
      if (trajectoryPointLists.size() != joints.length)
         return false;

      for (int jointIndex = 0; jointIndex < trajectoryPointLists.size(); jointIndex++)
      {
         if (!ControllerCommandValidationTools.checkJointspaceTrajectoryPointList(joints[jointIndex], trajectoryPointLists.get(jointIndex)))
            return false;
      }

      return true;
   }

   public static boolean checkJointspaceTrajectoryPointList(OneDoFJoint joint, SimpleTrajectoryPoint1DList trajectoryPointList)
   {
      for (int i = 0; i < trajectoryPointList.getNumberOfTrajectoryPoints(); i++)
      {
         double waypointPosition = trajectoryPointList.getTrajectoryPoint(i).getPosition();
         double jointLimitLower = joint.getJointLimitLower();
         double jointLimitUpper = joint.getJointLimitUpper();
         if (!MathTools.isInsideBoundsInclusive(waypointPosition, jointLimitLower, jointLimitUpper))
            return false;
      }
      return true;
   }
}
