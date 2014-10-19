package com.sri.bolt.service;

import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MTErrorDetectorSocketServiceClient extends SocketServiceClient implements SessionDataServiceClient {
   public MTErrorDetectorSocketServiceClient(Properties config, Language lang) {
      super(config);
      language = lang;
      String langPrefix = lang == Language.ENGLISH ? "en." : "ia.";
      name = "mtErrorDetector" + langPrefix;
      mEndpointAddress = config.getProperty("mtErrorEndpointAddr");
      mEndpointPath = config.getProperty(langPrefix + "mtErrorEndpointPath");
      mEndpointExe = config.getProperty("mtErrorServerExe");
      port = config.getProperty(langPrefix + "mtErrorEndpointPort");
      mArgs = config.getProperty(langPrefix  + "mtErrorArgs").trim();
      mResolvedArgs = com.sri.bolt.Util.splitAndResolve(config, mArgs);
      mEndpointPath = config.getProperty(langPrefix + "mtErrorPath");
      mEndpointInitTimeoutSeconds = Integer.parseInt(config.getProperty("mtErrorInitTimeoutSeconds"));
      init();
   }

   @Override
   public void init() {
      killEndpoint();
      buildEndpoint();
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
         byte[] returnData = makeServiceCall("markErrors", checkedData.toByteArray(), true);
         socket = null;
         return SessionData.parseFrom(returnData);
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
         return null;
      }
   }

   public SessionData detectSenses(SessionData data) {
      SessionData checkedData = checkInput(data);
      try {
         byte[] returnData = makeServiceCall("markSenses", checkedData.toByteArray(), true);
         socket = null;
         return SessionData.parseFrom(returnData);
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
         return null;
      }
   }

   public void reset() {
      makeServiceCall("reset", true);
      socket = null;
   }

   @Override
   protected void doPreCallWork(int numAttempts) {
      //need to wait for process to be up before we can reconnect
      if (numAttempts > 0) {
         try {
            Thread.sleep(10000);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      //MTError detection will close connection after call
      InetSocketAddress addr = new InetSocketAddress(mEndpointAddress, Integer.parseInt(port));
      socket = SocketServiceConnector.getConnected(name, mEndpointInitTimeoutSeconds, addr, endpoint);
   }

   @Override
   public void cleanup() {
      try {
         if (socket != null) {
            socket.close();
         }
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
      }
      socket = null;
      // This destroys the process
      if (endpoint != null) {
         endpoint.destroy();
         endpoint = null;
      }
      killEndpoint();
   }

   public void buildEndpoint() {
      List<String> args = new ArrayList<String>();

      args.add("./" + mEndpointExe);

      for (int i = 0; i < mResolvedArgs.length; i++) {
         args.add(mResolvedArgs[i]);
      }

      ProcessBuilder pb = new ProcessBuilder(args);
      // Set working directory
      pb.directory(new File(mEndpointPath));
      try {
         endpoint = startAndHandleLogging(pb, name);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(MTErrorDetectorSocketServiceClient.class);
   private String mEndpointAddress;
   private String mEndpointPath;
   private String mEndpointExe;
   private String mArgs;
   private String name;
   private String[] mResolvedArgs;
   private Language language;
   private int mEndpointInitTimeoutSeconds;
}
