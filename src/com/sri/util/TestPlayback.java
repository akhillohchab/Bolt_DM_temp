package com.sri.util;

import java.io.File;

import com.sri.audio.AudioReader;
import com.sri.audio.SJAudio;
import com.sri.interfaces.audio.AudioProperties;
import com.sri.interfaces.log.DLog;
import com.sri.interfaces.log.DLogInterface;
import com.sri.jsound.JSoundAudioPlayerFactory;

// Usage: TestPlayback filename ninst
// With ninst parameter, can try playing overlapping
// output (which doesn't appear to work).
public class TestPlayback {
   public static void printUsage() {
      System.out.println("Usage: TestPlayback filename ninst\n");
   }

   public static void main(String[] args) {
      if (args.length == 0) {
         printUsage();
         System.exit(0);
      }

      DLog.setLogger(new DLogInterface() {
          public void ll(String prefix, String comp, String msg) {
              System.out.println(prefix + comp + "::" + msg);
          }
          public void w(String comp, String msg) {
              ll("W/", comp, msg);
          }
          public void v(String comp, String msg) {
              ll("V/", comp, msg);
          }
          public void e(String comp, String msg) {
              ll("E/", comp, msg);
	      throw new RuntimeException("Making RuntimeException for error");
          }
          public void i(String comp, String msg) {
              ll("I/", comp, msg);
          }
          public void d(String comp, String msg) {
              ll("D/", comp, msg);
          }
      });

      String filename = args[0];
      int ninst = 1;
      if (args.length > 1) {
         try {
            ninst = Integer.parseInt(args[1]);
         } catch (Exception e) {
            printUsage();
            System.exit(1);
         }
      }

      System.out.println("Audio filename: " + filename);
      System.out.println("Number of instances: " + ninst);

      short[] samples = AudioReader.loadFileNE(new File(filename));

      // Try to play output multiple times to default device
      for (int i = 0; i < ninst; i++) {
         if (i > 0) {
            // Wait a bit before starting subsequent playback
            try {
               Thread.sleep(100);
            } catch (Exception e) {
               // Ignore
            }
         }
         AudioProperties p = new AudioProperties(16000, 1);
         final SJAudio audio = new SJAudio(p, null, new JSoundAudioPlayerFactory());
         System.out.println("Starting playback[" + i + "]");
         final int inst = i;
         Runnable r = new Runnable() {
            public void run() {
               System.out.println("Inst " + inst + " finished");
            }
         };
         boolean success = audio.play(samples, r);
         System.out.println("Starting playback[" + i + "] returned " + success);
      }
   }
}
