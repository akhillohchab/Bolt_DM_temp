package com.sri.bolt.audio;

import com.sri.bolt.FileIOUtil;

import java.io.InputStream;

public class AudioLevelUtil {
   public static int deviceToCard(String device) {
      return AudioNameConversion.getCard(device);
   }

   public static AudioLevelValues getMicValues(String device) {
      int c = deviceToCard(device);
      try {
         String[] args = {"amixer", "-c", Integer.toString(c), "sget", "Mic"};
         return parseMicValues(getOutput(args));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   public static AudioLevelValues getPlayValues(String device) {
      int c = deviceToCard(device);
      String playControl = getPlayControl(c);
      if (playControl == null) {
         return null;
      }
      try {
         String[] args = {"amixer", "-c", Integer.toString(c), "sget", playControl};
         return parsePlayValues(getOutput(args));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   public static void setMicLevel(String device, int level) {
      int c = deviceToCard(device);
      String[] args = {"amixer", "-c", Integer.toString(c), "sset", "Mic", Integer.toString(level)};
      // Just need to execute the command; don't even need output
      getOutput(args);
   }

   /**
    * Auto-adjust mic level based on last recordedEnergy level in 0-255 range.
    * @param recordedEnergy Energy value of last audio from AudioEnergyUtil.
    * @param device Current name of audio device from settings.
    * @return amount energy level changed where positive means record level
    * bumped up and 0 means left unchanged.
    */
   public static int autoAdjustMicLevel(int recordedEnergy, String device) {
      AudioLevelValues v = getMicValues(device);
      if ((v == null) || (v.LMIN == v.LMAX)) {
         return 0;
      }

      // Don't adjust if level in range [130, 180];
      if ((recordedEnergy >= 130) && (recordedEnergy <= 180)) {
         return 0;
      }

      // Don't adjust more than 20% of difference from min to max
      final float maxAdjustRatio = 0.2f;
      final int desired = 150;
      int adjustRaw = Math.round(((desired - recordedEnergy) / 255.0f) * (v.LMAX - v.LMIN));
      int maxAdjust = Math.round(maxAdjustRatio * (v.LMAX - v.LMIN));
      // Make exception to allow 1 unit if maxAdjustRatio limits us to less than one
      if (maxAdjust == 0) {
         maxAdjust = 1;
      }
      int adjust = adjustRaw;
      if (adjustRaw > maxAdjust) {
         adjust = maxAdjust;
      } else if (adjustRaw < -maxAdjust) {
         adjust = -maxAdjust;
      }
      int newLevel = v.LVALUE + adjust;
      if (newLevel > v.LMAX) {
         newLevel = v.LMAX;
         adjust = newLevel - v.LVALUE;
      } else if (newLevel < v.LMIN) {
         newLevel = v.LMIN;
         adjust = newLevel - v.LVALUE;
      }
      if (adjust != 0) {
         // Nothing to do unless adjustment is non-zero
         setMicLevel(device, newLevel);
      }
      return adjust;
   }

   public static void setPlayLevel(String device, int level) {
      int c = deviceToCard(device);
      String playControl = getPlayControl(c);
      if (playControl == null) {
         return;
      }
      String[] args = {"amixer", "-c", Integer.toString(c), "sset", playControl, Integer.toString(level)};
      // Just need to execute the command; don't even need output
      getOutput(args);
   }

   public static String getOutput(String[] args) {
      String s = "";
      int exitValue = -1;
      try {
         Runtime rt = Runtime.getRuntime();
         Process p = rt.exec(args);
         exitValue = p.waitFor();
         InputStream is = p.getInputStream();
         byte[] data = FileIOUtil.readFully(is);
         s = new String(data, "UTF8");
      } catch (Exception e) {
         // Ignore
      }

      if (exitValue != 0) {
         throw new IllegalArgumentException("Command failed");
      }

      return s;
   }

    /*
      Sample:
Simple mixer control 'Mic',0
  Capabilities: cvolume cvolume-joined cswitch cswitch-joined penum
  Capture channels: Mono
  Limits: Capture 0 - 28
  Mono: Capture 24 [86%] [-4.00dB] [on]
     */
   public static AudioLevelValues parseMicValues(String s) {
      return parseValues(s, "Capture");
   }

    /*
      Sample:
Simple mixer control 'PCM',0
  Capabilities: pvolume pvolume-joined pswitch pswitch-joined penum
  Playback channels: Mono
  Limits: Playback 0 - 66
  Mono: Playback 37 [56%] [-29.00dB] [on]
     */
   public static AudioLevelValues parsePlayValues(String s) {
       return parseValues(s, "Playback");
   }

   private static AudioLevelValues parseValues(String s, String match) {
      String[] lines = s.split("\n");
      int min = -1;
      int max = -1;
      int value = -1;
      for (String line: lines) {
         if (line.contains("Limits:")) {
            String[] vals = line.split("\\s+");
            if (vals.length >= 3) {
               try {
                   min = Integer.parseInt(vals[vals.length - 3]);
                   max = Integer.parseInt(vals[vals.length - 1]);
               } catch (NumberFormatException nfe) {
                   // Ignore
               }
            }
         }
         // Currently value typically either on a "Mono: " line:
         //   Mono: Playback 37 [56%] [-29.00dB] [on]
         // or specified for individual channels; we take any one
         //   Front Left: Playback 250 [98%] [-1.00dB]
         if (line.contains("Mono:") || (line.contains("Front Left:"))) {
            String watchFor = new String(".*") + match + "\\s+";
            if (line.matches(watchFor + ".*")) {
               //System.out.println("Got match: " + line);
               String newStr = line.replaceFirst(watchFor, "");
               newStr = newStr.replaceFirst("\\s+.*", "");
               try {
                  value = Integer.parseInt(newStr);
               } catch (NumberFormatException nfe) {
                  // Ignore
               }
            } else {
               //System.out.println("No match: " + line + "; wanted: " + watchFor);
            }
         }
      }
      if ((min < 0) || (max < 0) || (value < 0)) {
         return null;
      } else {
         return new AudioLevelValues(min, max, value);
      }
   }

   // For the specified card, try to get best name of play control.
   // Use "Master" if found or fall back on "PCM". Else return null.
   private static String getPlayControl(int card) {
      String s = null;
      try {
         String[] args = {"amixer", "-c", Integer.toString(card), "scontrols"};
         s = getOutput(args);
      } catch (IllegalArgumentException e) {
         return null;
      }
      String[] preferred = {"Master", "PCM"};
      String[] lines = s.split("\n");
      for (int i = 0; i < preferred.length; i++) {
         for (String line: lines) {
            if (line.contains(preferred[i])) {
               return preferred[i];
            }
         }
      }
      return null;
   }

}
