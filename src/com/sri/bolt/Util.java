package com.sri.bolt;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.HashMap;

public class Util {
   public static boolean deleteRecursive(File path) throws FileNotFoundException {
      if (!path.exists()) {
         throw new FileNotFoundException(path.getAbsolutePath());
      } else {
         boolean ret = true;
         if (path.isDirectory()) {
            for (File f : path.listFiles()) {
               ret = ret && deleteRecursive(f);
            }
         }
         return ret && path.delete();
      }
   }

   synchronized public static String reserveUniqueFileName(String basename, String extension) {
      String key = basename + extension;
      String candidateFileName = key;
      Integer checkIndex;
      if (gReservedCount.containsKey(key)) {
         checkIndex = gReservedCount.get(key);
      } else {
         // A bit of a special case since we don't add "-0".
         checkIndex = new Integer(0);
      }
      while (new File(candidateFileName).exists()) {
         checkIndex++;
         candidateFileName = basename + "-" + checkIndex + extension;
      }

      // Store/update last index used
      gReservedCount.put(key, checkIndex);

      return candidateFileName;
   }

   // Split string about whitespace into array then
   // resolve variables of form %var%.
   public static String[] splitAndResolve(Properties config, String in) {
      // Split about whitespace
      // "in" should look something like setting for AMUArgs:
      //   %AMUConfig% %AMUEndpointPort%
      String[] retval = in.split("\\s+");
      for (int i = 0; i < retval.length; i++) {
          if ((retval[i].length() > 2) && retval[i].startsWith("%") && retval[i].endsWith("%")) {
              String lookup = retval[i].substring(1, retval[i].length() - 1);
              // Update value with what we looked up (not recursive to keep looking up)
              retval[i] = config.getProperty(lookup);
          }
      }

      return retval;
   }

   public static String getUniqueTrialId() {
      int suffix = 1000;
      String prefix = "S_S01F01_S_A";
      String id;
      do {
         id = prefix + suffix++;
      } while (hasTrialId(id));

      return id;
   }

   private static boolean hasTrialId(String trialId) {
      File newTrial = new File(App.getApp().getProps().getProperty("OutputDir") + "/" + trialId);
      return newTrial.exists();
   }

   public static String getFilenameTimestamp() {
      return getFilenameTimestamp(System.currentTimeMillis());
   }

   public static String getFilenameTimestamp(long timeMillis) {
      return DATE_FORMAT_FILE.format(timeMillis);
   }

   //CONSTANTS
   public static final int SAMPLE_RATE = 16000;
   public static final int BYTES_PER_SAMPLE = 2;
   // Avoid use of SAMPLE_SIZE to not worry about non-multiple of 8 bits.
   // Simply use BYTES_PER_SAMPLE * 8 where needed.
   //public static final int SAMPLE_SIZE = BYTES_PER_SAMPLE * 8;
   public static final boolean SIGNED_AUDIO = true;
   public static final boolean BIG_ENDIAN_AUDIO = false;
   public static final int FRAME_ADVANCE_IN_SAMPLES = 160;
   public static final int WINDOW_SIZE_IN_SAMPLES = 410;

   public static final SimpleDateFormat DATE_FORMAT_FILE = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");

   // Counter keeps track of where we start our search for new file with
   // matching basename. For example, "foo.txt", {10} means we've assigned
   // foo-10.txt so we will look for available names starting foo-11.txt.
   private static HashMap<String, Integer> gReservedCount = new HashMap<String, Integer>();
}
