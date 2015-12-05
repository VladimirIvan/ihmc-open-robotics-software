package us.ihmc.valkyrieRosControl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.yaml.snakeyaml.Yaml;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.time.TimeTools;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.AuxiliaryRobotData;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorProcessing;
import us.ihmc.sensorProcessing.sensorProcessors.SensorRawOutputMapReadOnly;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.StateEstimatorSensorDefinitions;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.tools.TimestampProvider;
import us.ihmc.valkyrieRosControl.dataHolders.YoForceTorqueSensorHandle;
import us.ihmc.valkyrieRosControl.dataHolders.YoIMUHandleHolder;
import us.ihmc.valkyrieRosControl.dataHolders.YoJointHandleHolder;

public class ValkyrieRosControlSensorReader implements SensorReader
{

   private final SensorProcessing sensorProcessing;

   private final TimestampProvider timestampProvider;

   private final List<YoJointHandleHolder> yoJointHandleHolders;
   private final List<YoIMUHandleHolder> yoIMUHandleHolders;
   private final List<YoForceTorqueSensorHandle> yoForceTorqueSensorHandles;

   private final Vector3d linearAcceleration = new Vector3d();
   private final Vector3d angularVelocity = new Vector3d();
   private final Quat4d orientation = new Quat4d();

   private final DenseMatrix64F torqueForce = new DenseMatrix64F(6, 1);

   private final ArrayList<ValkyrieRosControlJointControlCommandCalculator> controlCommandCalculators = new ArrayList<>();

   private final DoubleYoVariable doIHMCControlRatio;
   private final DoubleYoVariable timeInStandprep;
   private final DoubleYoVariable masterGain;

   private final BooleanYoVariable startStandPrep;
   private long standPrepStartTime = -1;

   public ValkyrieRosControlSensorReader(StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions, StateEstimatorParameters stateEstimatorParameters,
         TimestampProvider timestampProvider, List<YoJointHandleHolder> yoJointHandleHolders, List<YoIMUHandleHolder> yoIMUHandleHolders,
         List<YoForceTorqueSensorHandle> yoForceTorqueSensorHandles, YoVariableRegistry registry)
   {

      this.sensorProcessing = new SensorProcessing(stateEstimatorSensorDefinitions, stateEstimatorParameters, registry);
      this.timestampProvider = timestampProvider;
      this.yoJointHandleHolders = yoJointHandleHolders;
      this.yoIMUHandleHolders = yoIMUHandleHolders;
      this.yoForceTorqueSensorHandles = yoForceTorqueSensorHandles;

      doIHMCControlRatio = new DoubleYoVariable("doIHMCControlRatio", registry);
      masterGain = new DoubleYoVariable("StandPrepMasterGain", registry);
      timeInStandprep = new DoubleYoVariable("timeInStandprep", registry);
      startStandPrep = new BooleanYoVariable("startStandPrep", registry);
      masterGain.set(0.3);

      Yaml yaml = new Yaml();
      
      InputStream gainStream = getClass().getClassLoader().getResourceAsStream("standPrep/gains.yaml");
      InputStream offsetsStream = getClass().getClassLoader().getResourceAsStream("standPrep/offsets.yaml");
      InputStream setpointsStream = getClass().getClassLoader().getResourceAsStream("standPrep/setpoints.yaml");
      
      Map<String, Map<String, Double>> gainMap = (Map<String, Map<String, Double>>) yaml.load(gainStream);
      Map<String, Map<String, Double>> offsetMap = (Map<String, Map<String, Double>>) yaml.load(offsetsStream);
      Map<String, Double> setPointMap = (Map<String, Double>) yaml.load(setpointsStream);
      
      try
      {
         gainStream.close();
         offsetsStream.close();
         setpointsStream.close();
      }
      catch (IOException e)
      {
      }

      for (YoJointHandleHolder oneDoFJoint : yoJointHandleHolders)
      {
         Map<String, Double> standPrepGains = gainMap.get(oneDoFJoint.getName());
         Map<String, Double> offsets = offsetMap.get(oneDoFJoint.getName());

         double standPrepAngle = 0.0;
         if (setPointMap.containsKey(oneDoFJoint.getName()))
         {
            standPrepAngle = setPointMap.get(oneDoFJoint.getName());
         }
         ValkyrieRosControlJointControlCommandCalculator controlCommandCalculator = new ValkyrieRosControlJointControlCommandCalculator(oneDoFJoint,
               standPrepGains, offsets, standPrepAngle, stateEstimatorParameters.getEstimatorDT(), registry);
         controlCommandCalculators.add(controlCommandCalculator);

      }

   }

   @Override
   public void read()
   {
      for (YoJointHandleHolder yoJointHandleHolder : yoJointHandleHolders)
      {
         yoJointHandleHolder.update();

         sensorProcessing.setJointPositionSensorValue(yoJointHandleHolder.getOneDoFJoint(), yoJointHandleHolder.getQ());
         sensorProcessing.setJointVelocitySensorValue(yoJointHandleHolder.getOneDoFJoint(), yoJointHandleHolder.getQd());
         sensorProcessing.setJointTauSensorValue(yoJointHandleHolder.getOneDoFJoint(), yoJointHandleHolder.getTauMeasured());
      }

      for (YoIMUHandleHolder yoIMUHandleHolder : yoIMUHandleHolders)
      {
         yoIMUHandleHolder.update();

         yoIMUHandleHolder.packLinearAcceleration(linearAcceleration);
         yoIMUHandleHolder.packAngularVelocity(angularVelocity);
         yoIMUHandleHolder.packOrientation(orientation);

         sensorProcessing.setLinearAccelerationSensorValue(yoIMUHandleHolder.getImuDefinition(), linearAcceleration);
         sensorProcessing.setAngularVelocitySensorValue(yoIMUHandleHolder.getImuDefinition(), angularVelocity);
         sensorProcessing.setOrientationSensorValue(yoIMUHandleHolder.getImuDefinition(), orientation);
      }

      for (YoForceTorqueSensorHandle yoForceTorqueSensorHandle : yoForceTorqueSensorHandles)
      {
         yoForceTorqueSensorHandle.update();

         yoForceTorqueSensorHandle.packWrench(torqueForce);
         sensorProcessing.setForceSensorValue(yoForceTorqueSensorHandle.getForceSensorDefinition(), torqueForce);
      }

      long timestamp = timestampProvider.getTimestamp();
      sensorProcessing.startComputation(timestamp, timestamp, -1);

      write(timestamp);
   }

   private void write(long timestamp)
   {
      if (standPrepStartTime > 0)
      {
         timeInStandprep.set(TimeTools.nanoSecondstoSeconds(timestamp - standPrepStartTime));

         for (ValkyrieRosControlJointControlCommandCalculator commandCalculator : controlCommandCalculators)
         {
            commandCalculator.computeAndUpdateJointTorque(timeInStandprep.getDoubleValue(), doIHMCControlRatio.getDoubleValue(), masterGain.getDoubleValue());
         }

      }
      else if (startStandPrep.getBooleanValue())
      {
         standPrepStartTime = timestamp;
         for (ValkyrieRosControlJointControlCommandCalculator commandCalculator : controlCommandCalculators)
         {
            commandCalculator.initialize();
         }
      }

   }

   @Override
   public SensorOutputMapReadOnly getSensorOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override
   public SensorRawOutputMapReadOnly getSensorRawOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override
   public AuxiliaryRobotData newAuxiliaryRobotDataInstance()
   {
      return null;
   }

}