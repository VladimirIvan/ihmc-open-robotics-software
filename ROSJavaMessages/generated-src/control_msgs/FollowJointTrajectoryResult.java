package control_msgs;

public interface FollowJointTrajectoryResult extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "control_msgs/FollowJointTrajectoryResult";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nint32 error_code\nint32 SUCCESSFUL = 0\nint32 INVALID_GOAL = -1\nint32 INVALID_JOINTS = -2\nint32 OLD_HEADER_TIMESTAMP = -3\nint32 PATH_TOLERANCE_VIOLATED = -4\nint32 GOAL_TOLERANCE_VIOLATED = -5\n\n";
  static final int SUCCESSFUL = 0;
  static final int INVALID_GOAL = -1;
  static final int INVALID_JOINTS = -2;
  static final int OLD_HEADER_TIMESTAMP = -3;
  static final int PATH_TOLERANCE_VIOLATED = -4;
  static final int GOAL_TOLERANCE_VIOLATED = -5;
  int getErrorCode();
  void setErrorCode(int value);
}