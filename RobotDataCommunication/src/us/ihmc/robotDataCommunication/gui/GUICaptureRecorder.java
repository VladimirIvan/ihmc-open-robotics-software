package us.ihmc.robotDataCommunication.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import us.ihmc.communication.configuration.NetworkParameterKeys;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.multicastLogDataProtocol.LogDataProtocolSettings;
import us.ihmc.robotDataCommunication.logger.LogProperties;
import us.ihmc.robotDataCommunication.logger.LogSettings;
import us.ihmc.robotDataCommunication.logger.NetworkStreamVideoDataLogger;

public class GUICaptureRecorder extends JFrame
{
   private static final long serialVersionUID = -5900895574333786847L;
   public final JButton start = new JButton("start");
   public final JButton stop = new JButton("stop");
   private final JComboBox<LogSettings> gui = new JComboBox<>();
   public NetworkStreamVideoDataLogger currentLogger = null;
   
   private File target;
   

   public GUICaptureRecorder()
   {
      super("GUI Capture recorder");
      BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.X_AXIS);
      getContentPane().setLayout(layout);

      for (LogSettings logSetting : LogSettings.values())
      {
         if (logSetting.getVideoStream() != null)
         {
            gui.addItem(logSetting);
         }
      }
      
      start.addActionListener(new ActionListener()
      {
         
         @Override
         public void actionPerformed(ActionEvent e)
         {
            try
            {
               startLog();
            }
            catch (UnknownHostException | SocketException e1)
            {
               e1.printStackTrace();
            }
         }
      });
      
      stop.addActionListener(new ActionListener()
      {
         
         @Override
         public void actionPerformed(ActionEvent e)
         {
            stopLog(false);
         }
      });
      
      getContentPane().add(gui);
      getContentPane().add(start);
      getContentPane().add(stop);
      
      setLocationByPlatform(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      Runtime.getRuntime().addShutdownHook(new Thread()
      {
         @Override
         public void run()
         {
            stopLog(true);
         }
      });
      
      pack();
      setVisible(true);
   }

   public void startLog() throws UnknownHostException, SocketException
   {
      if(currentLogger == null)
      {
         DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
         Calendar calendar = Calendar.getInstance();
         String timestamp = dateFormat.format(calendar.getTime());
         
         LogSettings settings = (LogSettings) gui.getSelectedItem();
         target = new File(System.getProperty("user.home") + "/robotLogs/", timestamp + "_" + settings.toString() + "_UILog");
         target.mkdirs();

         LogProperties logProperties = new LogProperties()
         {
            private static final long serialVersionUID = 1L; 
         };
         
         InetAddress listen = InetAddress.getByName(NetworkParameters.getHost(NetworkParameterKeys.logger)); 
         InetSocketAddress address = new InetSocketAddress(settings.getVideoStream(), LogDataProtocolSettings.UI_DATA_PORT);
         currentLogger = new NetworkStreamVideoDataLogger(listen.getAddress(), target, logProperties, address);
         start.setEnabled(false);
         stop.setEnabled(true);
      }
      
   }

   public void stopLog(boolean exiting)
   {
      if(currentLogger != null)
      {
         currentLogger.close();
         if(!exiting)
         {
            JOptionPane.showMessageDialog(this, "Video saved to " + target.getAbsolutePath());
         }
         else
         {
            System.out.println("Video saved to " + target.getAbsolutePath());
         }
         currentLogger = null;
      }
      start.setEnabled(true);

   }

   public static void main(String args[])
   {
      new GUICaptureRecorder();
   }
}
