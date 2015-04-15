package us.ihmc.steppr.hardware.state;

import java.nio.ByteBuffer;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;

public class StepprXSensState
{
   private final YoVariableRegistry registry;
   private final DoubleYoVariable accelX, accelY, accelZ;
   private final DoubleYoVariable gyroX, gyroY, gyroZ;
   private final DoubleYoVariable magX, magY, magZ;
   
   private final DoubleYoVariable qs, qx, qy, qz;
   
   private final IntegerYoVariable sample;
   
   public StepprXSensState(String name, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.accelX = new DoubleYoVariable("accelX", registry);
      this.accelY = new DoubleYoVariable("accelY", registry);
      this.accelZ = new DoubleYoVariable("accelZ", registry);

      this.gyroX = new DoubleYoVariable("gyroX", registry);
      this.gyroY = new DoubleYoVariable("gyroY", registry);
      this.gyroZ = new DoubleYoVariable("gyroZ", registry);

      this.magX = new DoubleYoVariable("magX", registry);
      this.magY = new DoubleYoVariable("magY", registry);
      this.magZ = new DoubleYoVariable("magZ", registry);

      this.qs = new DoubleYoVariable("qs", registry);
      this.qx = new DoubleYoVariable("qx", registry);
      this.qy = new DoubleYoVariable("qy", registry);
      this.qz = new DoubleYoVariable("qz", registry);
      
      this.sample = new IntegerYoVariable("sample", registry);
      
      parentRegistry.addChild(registry);
   }
   
   public void getAccel(Vector3d accelToPack)
   {
      accelToPack.setX(accelX.getDoubleValue());
      accelToPack.setY(accelY.getDoubleValue());
      accelToPack.setZ(accelZ.getDoubleValue());
   }
   
   public void getGyro(Vector3d gyroToPack)
   {
      gyroToPack.setX(gyroX.getDoubleValue());
      gyroToPack.setY(gyroY.getDoubleValue());
      gyroToPack.setZ(gyroZ.getDoubleValue());
   }
   
   public void getMagnetometer(Vector3d magToPack)
   {
      magToPack.setX(magX.getDoubleValue());
      magToPack.setY(magY.getDoubleValue());
      magToPack.setZ(magZ.getDoubleValue());
   }
   
   public void getQuaternion(Quat4d quatToPack)
   {
      quatToPack.set(qx.getDoubleValue(), qy.getDoubleValue(), qz.getDoubleValue(), qs.getDoubleValue());
   }
   
   public int getSample()
   {
      return sample.getIntegerValue();
   }
   
   public void update(ByteBuffer buffer)
   {
      //Commented because IMU coordinates are not equivalent to Pelvis Coordinates
//      accelX.set(buffer.getFloat());
//      accelY.set(buffer.getFloat());
//      accelZ.set(buffer.getFloat());
//      
//      gyroX.set(buffer.getFloat());
//      gyroY.set(buffer.getFloat());
//      gyroZ.set(buffer.getFloat());
//      
//      magX.set(buffer.getFloat());
//      magY.set(buffer.getFloat());
//      magZ.set(buffer.getFloat());
//
//      qs.set(buffer.getFloat());
//      qx.set(buffer.getFloat());
//      qy.set(buffer.getFloat());
//      qz.set(buffer.getFloat());
//      
//      sample.set(buffer.getShort() & 0xFFFF);
      
      
      //simple re-mappping of IMU Coordinates to Pelvis Coordinates  (Steve Spencer)
      //Robot Z is IMU -X
      //Robot Y is IMU Y
      //Robot X is IMU Z
      accelZ.set(-buffer.getFloat());
      accelY.set(buffer.getFloat());
      accelX.set(buffer.getFloat());

      gyroZ.set(-buffer.getFloat());
      gyroY.set(buffer.getFloat());
      gyroX.set(buffer.getFloat());
      
      magZ.set(-buffer.getFloat());
      magY.set(buffer.getFloat());
      magX.set(buffer.getFloat());

      //Quaternions aren't as simple to re-map   (Steve Spencer)
      //IMU output maps IMU body coords to Spatial coords (Qsi)
      //We want to map Pelvis coords to Spatial coords (Qsp)
      //This can be done using Qsp = Qsi*Qip where Qip is a -90deg rotation about Y
      double qstemp = buffer.getFloat();
      double qxtemp = buffer.getFloat();
      double qytemp = buffer.getFloat();
      double qztemp = buffer.getFloat();
      Quat4d Qsi = new Quat4d();
      Quat4d Qip = new Quat4d();
      Qsi.set(qxtemp,qytemp,qztemp,qstemp);
      Qip.set(0,.707106781186548,0,-.707106781186548);
      Qsi.mul(Qip);
      qs.set(Qsi.w);
      qx.set(Qsi.x);
      qy.set(Qsi.y);
      qz.set(Qsi.z);
      
      sample.set(buffer.getShort() & 0xFFFF);
      
      @SuppressWarnings("unused")
      int checksum = buffer.getShort() & 0xFFFF;
      
   }
}