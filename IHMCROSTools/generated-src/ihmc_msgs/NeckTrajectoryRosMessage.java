package ihmc_msgs;

public interface NeckTrajectoryRosMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/NeckTrajectoryRosMessage";
  static final java.lang.String _DEFINITION = "## NeckTrajectoryRosMessage\n# This message commands the controller to move the neck in jointspace to the desired joint angles\n# while going through the specified trajectory points. A third order polynomial function is used to\n# interpolate between trajectory points. A message with a unique id equals to 0 will be interpreted as\n# invalid and will not be processed by the controller. This rule does not apply to the fields of this\n# message.\n\n# List of points in the trajectory. The expected joint ordering is from the closest joint to the chest\n# to the closest joint to the head.\nihmc_msgs/OneDoFJointTrajectoryRosMessage[] joint_trajectory_messages\n\n# A unique id for the current message. This can be a timestamp or sequence number. Only the unique id\n# in the top level message is used, the unique id in nested messages is ignored. Use\n# /output/last_received_message for feedback about when the last message was received. A message with\n# a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller.\nint64 unique_id\n\n\n";
  java.util.List<ihmc_msgs.OneDoFJointTrajectoryRosMessage> getJointTrajectoryMessages();
  void setJointTrajectoryMessages(java.util.List<ihmc_msgs.OneDoFJointTrajectoryRosMessage> value);
  long getUniqueId();
  void setUniqueId(long value);
}
