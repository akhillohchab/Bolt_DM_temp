package com.sri.bolt.service;


import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class CPPSocketServiceClient extends SocketServiceClient implements SessionDataServiceClient {
   public CPPSocketServiceClient(Properties config) {
      super(config);
      name = "";
   }

   @Override
   public void init() {
      buildEndpoint();

      InetSocketAddress addr = new InetSocketAddress(mEndpointAddress, Integer.parseInt(port));

      // Helper in parent class SocketServiceConnector
      // Try for up to 100 seconds
      socket = SocketServiceConnector.getConnected(name, mEndpointInitTimeoutSeconds, addr, endpoint);

      // mClientSocket could be null if connection failed

      // XXX Until this point, better not have other threads trying to use socket
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
         byte[] returnData = makeServiceCall(getFunctionCallName(), checkedData.toByteArray(), true);
         mFirstInput = false;
         return SessionData.parseFrom(returnData);
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
         return null;
      }
   }

   @Override
   public void cleanup() {
      try {
         if (socket != null) {
            socket.close();
         }
         // This would wait for process to finish
         /*
         if (mEndpoint != null) {
            mEndpoint.waitFor();
         }
         */
      } catch (IOException e) {
         logger.error(name + ": " + e.getMessage(), e);
         /* only needed for waitFor()
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         */
      }
      // This destroys the process
      if (endpoint != null) {
         endpoint.destroy();
         endpoint = null;
      }
   }

   public void buildEndpoint() {
      // Next input will be first input for this endpoint
      mFirstInput = true;

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

   @Override
   protected int getTimeout() {
      // Default to 15 second timeout, except for first input
      if (mFirstInput) {
         return 60;
      } else {
         return 15;
      }
   }

   protected abstract String getFunctionCallName();

   private static final Logger logger = LoggerFactory.getLogger(CPPSocketServiceClient.class);

   protected String mEndpointAddress;
   protected String mEndpointPath;
   protected String mEndpointExe;
   protected String mEndpointConfig;
   protected int mEndpointInitTimeoutSeconds = 60;
   protected String mArgs;
   protected String[] mResolvedArgs;

   // Name for logging
   protected String name;

   // Track if this is first input (or first after restart)
   protected boolean mFirstInput = true;
}
