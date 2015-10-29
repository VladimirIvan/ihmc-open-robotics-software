package us.ihmc.communication.streamingData;

import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.ControllerCrashNotificationPacket;
import us.ihmc.communication.packets.ControllerCrashNotificationPacket.CrashLocation;
import us.ihmc.communication.packets.InvalidPacketNotificationPacket;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.streamingData.AtomicLastPacketHolder.LastPacket;
import us.ihmc.tools.thread.ThreadTools;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalDataProducer
{
   protected final PacketCommunicator communicator;
   private final ConcurrentLinkedQueue<Packet<?>> queuedData = new ConcurrentLinkedQueue<Packet<?>>();
   private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ThreadTools.getNamedThreadFactory("HumanoidGlobalDataProducer"));
   private final AtomicLastPacketHolder lastPacketHolder = new AtomicLastPacketHolder();

   public GlobalDataProducer(PacketCommunicator communicator)
   {
      this.communicator = communicator;
      executor.scheduleAtFixedRate(new DataProducerImpl(), 0, 1, TimeUnit.MILLISECONDS);
   }

   @SuppressWarnings("unchecked")
   public <T extends Packet<?>> void attachListener(Class<T> clazz, PacketConsumer<T> listener)
   {
      communicator.attachListener(clazz, listener);
      communicator.attachListener(clazz, lastPacketHolder);
   }

   public void queueDataToSend(Packet<?> packet)
   {
      queuedData.offer(packet);
   }

   public void notifyInvalidPacketReceived(Class<? extends Packet<?>> packetClass, String error)
   {
      queueDataToSend(new InvalidPacketNotificationPacket(packetClass, error));
   }

   public void notifyControllerCrash(CrashLocation location, String stackTrace)
   {
      queueDataToSend(new ControllerCrashNotificationPacket(location, stackTrace));
   }

   public void stop()
   {
      executor.shutdown();
   }

   public LastPacket getLastPacket()
   {
      return lastPacketHolder.getLastPacket();
   }

   public void setRobotTime(long time)
   {
      lastPacketHolder.setRobotTime(time);
   }

   private class DataProducerImpl implements Runnable
   {

      @Override
      public void run()
      {
         Packet<?> dataObject;
         while ((dataObject = queuedData.poll()) != null)
         {
            communicator.send(dataObject);
         }
      }

   }
}