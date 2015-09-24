package us.ihmc.simulationconstructionset;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.tools.io.printing.PrintTools;

public enum SupportedGraphics3DAdapter
{
   JAVA_MONKEY_ENGINE, JAVA3D;

   public String getClassWithPackageName()
   {
      switch (this)
      {
         case JAVA_MONKEY_ENGINE :
         {
            return "us.ihmc.graphics3DAdapter.jme.JMEGraphics3DAdapter";
         }

         case JAVA3D :
         {
            return "us.ihmc.graphics3DAdapter.java3D.Java3DGraphicsAdapter";
         }

         default :
         {
            return null;
         }

      }
   }

   public Graphics3DAdapter instantiateGraphics3DAdapter()
   {
      String graphicsAdapter = this.getClassWithPackageName();
      try
      {
         Graphics3DAdapter graphics3dAdapter = (Graphics3DAdapter) Class.forName(graphicsAdapter).newInstance();
         System.out.println(PrintTools.INFO + "Found graphics adapter: " + graphicsAdapter);

         return graphics3dAdapter;

      }
      catch (Exception e)
      {
         System.err.println(PrintTools.WARN + "Cannot find graphics adapter" + graphicsAdapter);

         return null;
      }
   }

   public static Graphics3DAdapter instantiateDefaultGraphicsAdapter(boolean showGUI)
   {
      if (!showGUI)
      {
         return null;
      }

      for (SupportedGraphics3DAdapter supportedGraphics3DAdapter : SupportedGraphics3DAdapter.values())
      {
         Graphics3DAdapter graphics3DAdapter = supportedGraphics3DAdapter.instantiateGraphics3DAdapter();
         if (graphics3DAdapter != null)
         {
            return graphics3DAdapter;
         }

      }

      throw new RuntimeException(PrintTools.ERROR + "No supported Graphics3DAdapter found");
   }

}