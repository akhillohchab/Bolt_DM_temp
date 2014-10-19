package com.sri.bolt.audio;

import com.sri.bolt.FileIOUtil;
import java.io.InputStream;

/**
 * Utility classes to extract ALSA device strings and
 * card numbers from String device names.
 */
public class AudioNameConversion {
   /**
    * Get ALSA card number for specified device name.
    * For example, use getHWName() to return hw:2,0
    * then use 2 as card number.
    * @return card number, currently defaulting to 0 if not found.
    */
   public static int getCard(String device) {
      int c = 0;
      // For our match, strip anything in name after the "[".
      // Example: "Headset_1 [plughw:0,0]" becomes "Headset_1 ["
      device = device.replaceFirst("\\[.*", "[");
      String hwName = getHWName(device);
      if ((hwName != null) && (hwName.length() > 0)) {
         String sCard = hwName;
         sCard = sCard.replaceFirst(".*hw:", "");
         sCard = sCard.replaceFirst(",.*$", "");
         try {
             c = Integer.parseInt(sCard);
         } catch (NumberFormatException nfe) {
            // Ignore;
         }
      }
      //System.out.println("device=" + device + ", hwname=" + hwName + ", card=" + c);
      return c;
   }

   /**
    * Get ALSA hardware name (hw:N,M) for specified device string.
    * NOTE: Relies on external script to perform the conversion.
    * NOTE: Names such as "Headset_1 [plughw:0,0]" should first be
    * simplified to "Headset_1 [" before lookup since match will
    * be against string like:
    * card 0: Headset_1 [Logitech G930 Headset], device 0: USB Audio [USB Audio]
    * @return hardware name or empty string if not found.
    */
   public static String getHWName(String device) {
      String retval = "";
      try {
         Runtime rt = Runtime.getRuntime();
         String[] args = {"../../../../bin/alsa-name-to-hw.sh", device};
         Process p = rt.exec(args);
         p.waitFor();
         InputStream is = p.getInputStream();
         byte[] data = FileIOUtil.readFully(is);
         String s = new String(data, "UTF8");
         // Only use first if multiple devices match
         String[] devices = s.split("\\s+");
         if ((devices != null) && (devices.length > 0)) {
            retval = devices[0];
         }
      } catch (Exception e) {
         // Ignore
      }
      return retval;
   }
}
