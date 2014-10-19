package com.sri.bolt.service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UWServiceClient extends SocketServiceClient implements SessionDataServiceClient {

   public UWServiceClient(Properties config, Language lang) {
      super(config);
      name = "UWServiceClient-" + lang.getAbbreviation();
      endpointAddress = config.getProperty("UWEndpointAddr");
      endpointPath = config.getProperty("UWPath");
      endpointExe = config.getProperty("UWExe");
      //endpointInputDir = config.getProperty("UWInputDir");
      //endpointOutputDir = config.getProperty("UWOutputDir");
      //endpointConfigFile = config.getProperty("UWConfigFile");
      //endpointConfigName = config.getProperty("UWConfigName");
      //endpointInitConfig = config.getProperty("UWInitConfig");
      endpointInitTimeoutSeconds = Integer.parseInt(config.getProperty("UWInitTimeoutSeconds"));
      String args = config.getProperty(lang == Language.ENGLISH ? "UWArgsEnglish" : "UWArgsArabic").trim();
      endpointArgs = com.sri.bolt.Util.splitAndResolve(config, args);
      //javaEndpointPort = config.getProperty("UWJavaPort");
      port = config.getProperty(lang == Language.ENGLISH ? "UWEnglishEndpointPort" : "UWArabicEndpointPort");
      this.language = lang;
      //convert to absolute path
      endpointPath = new File(endpointPath).toURI().normalize().getPath();
      init();
   }

   @Override
   public void init() {
      killChildProcesses();
      buildEndpoint();
      remoteAddr = new InetSocketAddress(endpointAddress, Integer.parseInt(port));
      //socket = SocketServiceConnector.getConnected(name, endpointInitTimeoutSeconds, remoteAddr, endpoint);
   }

   @Override
   public SessionData checkInput(SessionData data) {
      //TODO add error checking
      return data;
   }

   @Override
   public SessionData process(SessionData data) {
      SessionData checkedData = checkInput(data);
      try {
         byte[] returnData = makeServiceCall("processData", data.toByteArray(), true);
         socket.close();
         return SessionData.parseFrom(returnData);
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
         return null;
      }
   }

   @Override
   public void cleanup() {
      //TODO Make call for cleanup that doesn't retry
      boolean forceKilledUW = false;
      {
         // Socket should already be closed so would have to first
         // connect then call "endProcessing".
         /* We skip the section below since leaves 12345 in TIME_WAIT if we call "endProcessing"
         // Here, our timeout is only 2 seconds so we don't waste time.
         socket = SocketServiceConnector.getConnected("UWServiceClient", 2, remoteAddr, endpoint);
         if ((socket != null) && (socket.isConnected())) {
            App.getLog4jLogger().info("Calling UW endProcessing");
            // If this is done then 12345 in TIME_WAIT but 12346 freed.
            // If just kill endpoint then 12345 free but 12346 still listening.
            makeServiceCall("endProcessing", false);
            try {
                socket.close();
            } catch (IOException e) {
                // Do nothing
            }
            // Wait for 1 second before killing
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               // Ignore
            }
         }
         */
         /* Below method would kill both UW processes forcefully.
          * Reverted to old method on 9/25/2012
         // Kill 12346
         App.getLog4jLogger().info("Killing secondary UW by port " + javaEndpointPort);
         super.killEndpoint(javaEndpointPort);
         try {
            int exitVal = endpoint.exitValue();
            App.getLog4jLogger().info("UW ended on own, exitValue: " + exitVal);
         } catch (IllegalThreadStateException e) {
            App.getLog4jLogger().info("Destroying primary process UW");
            // Hasn't exited on own; destroy
            endpoint.destroy();
         }
         forceKilledUW = true;
         */
      }

      try {
         // This was the orignal call - see notes above and note that
         // now we've killed the processes so no need to even think of trying this.
         if (!forceKilledUW) {
             makeServiceCall("endProcessing", false);
            socket.close();
         }
         endpoint.waitFor();
         killChildProcesses();
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   private void killChildProcesses() {
      List<String> args = new ArrayList<String>();
      args.add("pkill");
      args.add("-f");
      if (language == Language.IRAQI_ARABIC) {
         args.add("java.*boltbc-uw.*TranstacProcessing.*88");
      } else {
         args.add("java.*boltbc-uw.*TranstacProcessing.*89");
      }
      ProcessBuilder pb = new ProcessBuilder(args);
      Process process = null;
      try {
         process = pb.start();
         process.waitFor();
      } catch (IOException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      if (language == Language.ENGLISH) {
         args.clear();
         args.add("pkill");
         args.add("-f");
         args.add("java.*boltbc-uw.*TranstacProcessing.*parser.properties");
         pb = new ProcessBuilder(args);
         try {
            process = pb.start();
            process.waitFor();
         } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }
      }
   }

   @Override
   protected void doPreCallWork(int numAttempts) {
      //TODO If we try to connect too soon after initing, we put the connection in a state
      //where we are connected but get a -1 on the read for the return value.  Need to figure
      //out why.  For now, just wait if we are reconnecting after a reinit
      if (numAttempts > 0) {
         try {
            Thread.sleep(5000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      //UW Client will close the socket on each message
      tryConnect();
   }

   @Override
   protected boolean killEndpoint(String port) {
      //need to kill the java process at 12346 as well in earlier versions
      //return super.killEndpoint(port) && super.killEndpoint(javaEndpointPort);
      killChildProcesses();
      return super.killEndpoint(port);
   }

   public void buildEndpoint() {
       /* Old - now have "UWArgs" that specifies args.
      ProcessBuilder pb = new ProcessBuilder("perl", endpointPath + endpointExe,
            "--input", endpointPath + endpointInputDir,
            "--output", endpointPath + endpointOutputDir,
            "--configFile", endpointPath + endpointConfigFile,
            "--configName", endpointConfigName,
            "--initConfig", endpointInitConfig);
            */
      List<String> args = new ArrayList<String>();
      args.add("perl");
      args.add(endpointPath + endpointExe);

      for (int i = 0; i < endpointArgs.length; i++) {
         args.add(endpointArgs[i]);
      }

      ProcessBuilder pb = new ProcessBuilder(args);
      try {
          endpoint = startAndHandleLogging(pb, name);
       } catch (IOException e) {
          logger.error(e.getMessage(), e);
       }
   }

   private void tryConnect() {
      socket = SocketServiceConnector.getConnected(name, endpointInitTimeoutSeconds, remoteAddr, endpoint);
      // Old version would set clientSocket if not connected
      if (socket == null) {
         socket = new Socket();
      }
   }

   @Override
   protected int getTimeout() {
      return 90;
   }

   private static final Logger logger = LoggerFactory.getLogger(UWServiceClient.class);
   //private String javaEndpointPort;
   private String endpointAddress;
   private String endpointPath;
   private String endpointExe;
   //private String endpointInputDir;
   //private String endpointOutputDir;
   //private String endpointConfigFile;
   //private String endpointConfigName;
   private int endpointInitTimeoutSeconds;
   private String[] endpointArgs;
   //private String endpointInitConfig;

   private InetSocketAddress remoteAddr;

   // Set to "UWServiceClient" variant after knowing lang
   private final String name;
   private Language language;
}
