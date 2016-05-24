package us.ihmc.humanoidBehaviors.behaviors;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;

public class KickBallBehaviorCoactiveElementBehaviorSide extends KickBallBehaviorCoactiveElement
{
  
   private KickBallBehavior kickBallBehavior;

   public void setKickBallBehavior(KickBallBehavior kickBallBehavior)
   {
      this.kickBallBehavior = kickBallBehavior;
   }

   @Override public void initializeUserInterfaceSide()
   {
   }

   @Override public void updateUserInterfaceSide()
   {
   }

   @Override
   public void initializeMachineSide()
   {
      machineSideCount.set(100);
   }

   @Override
   public void updateMachineSide()
   {
      if (abortAcknowledged.getBooleanValue() && (!abortClicked.getBooleanValue()))
      {
         abortAcknowledged.set(false);
      }

      if ((abortClicked.getBooleanValue()) && (!abortAcknowledged.getBooleanValue()))
      {
         if (kickBallBehavior != null)
         {
            kickBallBehavior.abort();
         }
         abortCount.increment();
         abortAcknowledged.set(true);
      }

      machineSideCount.increment();

      numBlobsDetected.set(kickBallBehavior.getNumBlobsDetected());
      if (numBlobsDetected.getIntegerValue() > 0)
      {
         blobX.set(kickBallBehavior.getBlobLocation().x);
         blobY.set(kickBallBehavior.getBlobLocation().y);
      }
   }
}