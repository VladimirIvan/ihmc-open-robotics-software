package object_manipulation_msgs;

public interface PlaceGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "object_manipulation_msgs/PlaceGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n# An action for placing an object\n\n# which arm to be used for grasping\nstring arm_name\n\n# a list of possible transformations for placing the object (place_trans)\n# the final pose of the wrist for placement (place_wrist_pose) is as follows:\n# place_wrist_pose = place_trans * grasp_pose\n# the frame_id for wrist_trans is defined here, and \n# should be the same for all place_locations\ngeometry_msgs/PoseStamped[] place_locations\n\n# the grasp that has been executed on this object\n# (contains the grasp_pose referred to above)\nGrasp grasp\n\n# how far the retreat should ideally be away from the place location\nfloat32 desired_retreat_distance\n\n# the min distance between the retreat and the place location that must actually be feasible \n# for the place not to be rejected\nfloat32 min_retreat_distance\n\n# how the place location should be approached\n# the frame_id that this lift is specified in MUST be either the robot_frame \n# or the gripper_frame specified in your hand description file\nGripperTranslation approach\n\n# the name that the target object has in the collision map\n# can be left empty if no name is available\nstring collision_object_name\n\n# the name that the support surface (e.g. table) has in the collision map\n# can be left empty if no name is available\nstring collision_support_surface_name\n\n# whether collisions between the gripper and the support surface should be acceptable\n# during move from pre-place to place and during retreat. Collisions when moving to the\n# pre-place location are still not allowed even if this is set to true.\nbool allow_gripper_support_collision\n\n# whether reactive placing based on tactile sensors should be used\nbool use_reactive_place\n\n# how much the object should be padded by when deciding if the grasp\n# location is freasible or not\nfloat64 place_padding\n\n# set this to true if you only want to query the manipulation pipeline as to what \n# place locations it thinks are feasible, without actually executing them. If this is set to \n# true, the atempted_location_results field of the result will be populated, but no arm \n# movement will be attempted\nbool only_perform_feasibility_test\n\n# OPTIONAL (These will not have to be filled out most of the time)\n# constraints to be imposed on every point in the motion of the arm\narm_navigation_msgs/Constraints path_constraints\n\n# OPTIONAL (These will not have to be filled out most of the time)\n# additional collision operations to be used for every arm movement performed\n# during placing. Note that these will be added on top of (and thus overide) other \n# collision operations that the grasping pipeline deems necessary. Should be used\n# with care and only if special behaviors are desired.\narm_navigation_msgs/OrderedCollisionOperations additional_collision_operations\n\n# OPTIONAL (These will not have to be filled out most of the time)\n# additional link paddings to be used for every arm movement performed\n# during placing. Note that these will be added on top of (and thus overide) other \n# link paddings that the grasping pipeline deems necessary. Should be used\n# with care and only if special behaviors are desired.\narm_navigation_msgs/LinkPadding[] additional_link_padding\n\n";
  java.lang.String getArmName();
  void setArmName(java.lang.String value);
  java.util.List<geometry_msgs.PoseStamped> getPlaceLocations();
  void setPlaceLocations(java.util.List<geometry_msgs.PoseStamped> value);
  object_manipulation_msgs.Grasp getGrasp();
  void setGrasp(object_manipulation_msgs.Grasp value);
  float getDesiredRetreatDistance();
  void setDesiredRetreatDistance(float value);
  float getMinRetreatDistance();
  void setMinRetreatDistance(float value);
  object_manipulation_msgs.GripperTranslation getApproach();
  void setApproach(object_manipulation_msgs.GripperTranslation value);
  java.lang.String getCollisionObjectName();
  void setCollisionObjectName(java.lang.String value);
  java.lang.String getCollisionSupportSurfaceName();
  void setCollisionSupportSurfaceName(java.lang.String value);
  boolean getAllowGripperSupportCollision();
  void setAllowGripperSupportCollision(boolean value);
  boolean getUseReactivePlace();
  void setUseReactivePlace(boolean value);
  double getPlacePadding();
  void setPlacePadding(double value);
  boolean getOnlyPerformFeasibilityTest();
  void setOnlyPerformFeasibilityTest(boolean value);
  arm_navigation_msgs.Constraints getPathConstraints();
  void setPathConstraints(arm_navigation_msgs.Constraints value);
  arm_navigation_msgs.OrderedCollisionOperations getAdditionalCollisionOperations();
  void setAdditionalCollisionOperations(arm_navigation_msgs.OrderedCollisionOperations value);
  java.util.List<arm_navigation_msgs.LinkPadding> getAdditionalLinkPadding();
  void setAdditionalLinkPadding(java.util.List<arm_navigation_msgs.LinkPadding> value);
}