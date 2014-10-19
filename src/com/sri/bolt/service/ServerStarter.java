package com.sri.bolt.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStarter {
   public ServerStarter(Properties config, String prefix) {
      mPrefix = prefix;
      mPath = config.getProperty(prefix + ".Path");
      mExe = config.getProperty(prefix + ".Exe");
      mArgs = config.getProperty(prefix + ".Args");
      mPort = Integer.parseInt(config.getProperty(prefix + ".Port"));
      // Break arguments into array and resolve %var%
      mResolvedArgs = com.sri.bolt.Util.splitAndResolve(config, mArgs);

      mKillByPortPath = config.getProperty("KillEndpointPath");
      mKillByPortExe = config.getProperty("KillEndpointExe");

      launch(true);
   }

   private void launch(final boolean wantRelaunch) {
      // Kill if necessary
      kill();

      // Set after kill() since kill() sets to false.
      mWantRunning = true;

      // In case process dies (unexpectedly), restart it.
      Runnable relaunchRunnable = new Runnable() {
         @Override
         public void run() {
            if (mWantRunning) {
               try {
                  // Prevent case of failed relaunches from happening too quickly
                  Thread.sleep(1000);
               } catch (InterruptedException e) {
                  // Ignore
               }
               ServerStarter.this.launch(wantRelaunch);
            }
         }
      };

      List<String> l = new ArrayList<String>();
      l.add(mExe);
      for (int i = 0; i < mResolvedArgs.length; i++) {
         l.add(mResolvedArgs[i]);
      }
      ProcessBuilder pb = new ProcessBuilder(l);
      pb.directory(new File(mPath));
      try {
         mProcess = SocketServiceClient.startAndHandleLogging(pb, mPrefix, true, wantRelaunch?relaunchRunnable:null);
      } catch (IOException e) {
         mProcess = null;
         logger.error("Error starting " + mPrefix + ": " + e);
      }
   }

   public void kill() {
      mWantRunning = false;

      if (mProcess == null) {
         logger.info("Kill any existing process at port " + mPort + " requested for: " + mPrefix);
      } else {
         try {
            int exitValue = mProcess.exitValue();
            logger.info("Kill requested for: " + mPrefix + ", already dead, exit value=" + exitValue);
         } catch (IllegalThreadStateException e) {
            // Hasn't exited
            logger.info("Kill requested for: " + mPrefix);
         }
      }

      // Always kill based on port - just in case old version still running.
      SocketServiceClient.killByPort("" + mPort, "./" + mKillByPortExe, mKillByPortPath);

      if (mProcess != null) {
         mProcess.destroy();
      }
      mProcess = null;
   }

   private String mPath;
   private String mExe;
   private String mArgs;
   private String[] mResolvedArgs;
   private String mPrefix;
   private int mPort;
   private String mKillByPortExe;
   private String mKillByPortPath;

   private Process mProcess;

   private boolean mWantRunning;

   private static final Logger logger = LoggerFactory.getLogger(ServerStarter.class);
}
