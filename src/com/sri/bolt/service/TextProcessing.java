package com.sri.bolt.service;

import com.sri.interfaces.lang.Language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility routines helpful to services for
 * processing text.
 */
public class TextProcessing {
   // Note that we don't take a language and have added
   // some English hesitation words - but they should
   // not appear in IA Buckwalter so any checks for them
   // should not result in matches so shouldn't hurt.
   private static String commonCleaning(String input) {
      if (input == null) {
         return input;
      }

      // Currently process one word at a time since
      // most processing is word-based.
      String[] words = input.split("\\s+");
      StringBuffer out = new StringBuffer(input.length() + 1);
      for (String word: words) {
         boolean keep = true;
         if (word.compareTo("@reject@") == 0) {
            keep = false;
         } else if (word.startsWith("%")) {
            keep = false;
         } else if (word.startsWith("-")) {
            keep = false;
         } else if (word.endsWith("-")) {
            keep = false;
         } else if (word.compareTo("eh") == 0) {
            keep = false;
         } else if (word.compareTo("huh") == 0) {
            keep = false;
         } else if (word.compareTo("uh") == 0) {
            keep = false;
         } else if (word.compareTo("um") == 0) {
            keep = false;
         } else if (word.length() == 0) {
            keep = false;
         }
         if (keep) {
             if (out.length() > 0) {
                out.append(" ");
             }
             out.append(word);
         }
      }

      return out.toString().trim();
   }

   public static String cleanForTranslationPreprocessing(String input) {
      logger.debug("cleanForTranslationPreprocessing(\"" + input + "\")");
      return commonCleaning(input);
   }

   // NOTE: May include -pau- which is meant for TTS so
   // should not alter that string.
   // Added 1-5 on 6/15/2013 - which should already
   // have surrounding space.
   public static String cleanForTTS(String input, Language l) {
      logger.debug("cleanForTTS(\"" + input + "\")");
      if (input == null) {
         return input;
      }

      // First, apply some common cleaning
      input = commonCleaning(input);

      switch (l) {
      case IRAQI_ARABIC:
      {
         String[] words = input.split("\\s+");
         StringBuffer out = new StringBuffer(input.length() + 1);
         for (String word: words) {
            boolean keep = true;
            // Added feff on 9/28 since showing up for IA.
            word = word.
                    replaceAll("[?]", "").
                    replaceAll("1", "wAHd").
                    replaceAll("2", "<vnyn").
                    replaceAll("3", "vlAvp").
                    replaceAll("4", ">rbEp").
                    replaceAll("5", "xmsp").
                    replaceAll("\ufeff", "");
            if (word.length() == 0) {
               keep = false;
            }
            if (keep) {
               out.append(" ");
               out.append(word);
           }
         }

         return out.toString().trim();
         // return above; no break needed
      }
      default:
         // Do nothing;
      }

      return input;
   }

   public static String cleanForDisplay(String input, Language l) {
      logger.debug("cleanForDisplay(\"" + input + "\")");
      return commonCleaning(input);
   }

   private static final Logger logger = LoggerFactory.getLogger(TextProcessing.class);
}
