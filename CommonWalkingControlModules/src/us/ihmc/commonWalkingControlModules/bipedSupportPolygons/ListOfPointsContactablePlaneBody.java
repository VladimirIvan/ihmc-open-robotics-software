package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Tuple2d;

import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class ListOfPointsContactablePlaneBody implements ContactablePlaneBody
{
   private final RigidBody rigidBody;
   private final ReferenceFrame soleFrame;
   private final List<Point2d> contactPoints = new ArrayList<Point2d>();
   private final int totalNumberOfContactPoints;

   public ListOfPointsContactablePlaneBody(RigidBody rigidBody, ReferenceFrame soleFrame, List<Point2d> contactPointsInSoleFrame)
   {
      this.rigidBody = rigidBody;
      this.soleFrame = soleFrame;

      for (Point2d contactPoint : contactPointsInSoleFrame)
      {
         this.contactPoints.add(new Point2d(contactPoint));
      }

      totalNumberOfContactPoints = contactPoints.size();
   }

   @Override
   public RigidBody getRigidBody()
   {
      return rigidBody;
   }

   @Override
   public String getName()
   {
      return rigidBody.getName();
   }

   @Override
   public List<FramePoint> getContactPointsCopy()
   {
      List<FramePoint> ret = new ArrayList<FramePoint>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2d point = contactPoints.get(i);
         ret.add(new FramePoint(soleFrame, point.getX(), point.getY(), 0.0));
      }

      return ret;
   }

   @Override
   public ReferenceFrame getFrameAfterParentJoint()
   {
      return rigidBody.getParentJoint().getFrameAfterJoint();
   }

   public FrameConvexPolygon2d getContactPolygonCopy()
   {
      return new FrameConvexPolygon2d(soleFrame, contactPoints);
   }

   @Override
   public ReferenceFrame getSoleFrame()
   {
      return soleFrame;
   }

   @Override
   public List<FramePoint2d> getContactPoints2d()
   {
      List<FramePoint2d> ret = new ArrayList<FramePoint2d>(contactPoints.size());
      for (int i = 0; i < contactPoints.size(); i++)
      {
         Tuple2d point = contactPoints.get(i);
         ret.add(new FramePoint2d(soleFrame, point));
      }

      return ret;
   }

   @Override
   public int getTotalNumberOfContactPoints()
   {
      return totalNumberOfContactPoints;
   }

}