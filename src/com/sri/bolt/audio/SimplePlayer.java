package com.sri.bolt.audio;

import com.sri.bolt.FileIOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

// Relies on ALSA and Sox command-line
public class SimplePlayer implements SimplePlayerInterface {
   public SimplePlayer(String filename, String device) {
      mFilename = filename;
      mDevice = device;

      mHWName = AudioNameConversion.getHWName(device);

      logger.info("device(" + mDevice + ") hwname(" + mHWName + ")");
   }

   @Override
   public boolean play(final Runnable onFinished) {
      boolean retval = false;
      // Only use once
      if (mPlayProcess != null) {
         return retval;
      }
      if ((mHWName != null) && (mHWName.length() > 0)) {
         Runtime rt = Runtime.getRuntime();
         String[] args = {"sox", mFilename, "-t", "alsa", mHWName};
         logger.info(args[0] + " " + args[1] + " " + args[2] + " " + args[3] + " " + args[4]);
         try {
            mPlayProcess = rt.exec(args);
         } catch (Exception e) {
            // mPlayProcess will be null
         }
         if (mPlayProcess != null) {
            retval = true;
            if (onFinished != null) {
                Runnable r = new Runnable() {
                   @Override
                   public void run() {
                      try {
                         mPlayProcess.waitFor();
                      } catch (Exception e) {
                         // Ignore
                      }
                      onFinished.run();
                   }
                };
                Thread t = new Thread(r);
                t.start();
            }
         }
      }
      return retval;
   }

   @Override
   public void waitFor() {
      if (mPlayProcess != null) {
         try {
            mPlayProcess.waitFor();
         } catch (InterruptedException e) {
            // Ignore
         }
      }
   }

   @Override
   public void stopPlaying(boolean blocking) {
      if (mPlayProcess != null) {
         if (!mDestroyed) {
            mDestroyed = true;
            mPlayProcess.destroy();
         }
         if (blocking) {
            waitFor();
         }
      }
   }

   private final String mFilename;
   private final String mDevice;
   private String mHWName;

   // Set true if destroy
   private boolean mDestroyed = false;

   private Process mPlayProcess;

   private static final Logger logger = LoggerFactory.getLogger(SimplePlayer.class);
}
