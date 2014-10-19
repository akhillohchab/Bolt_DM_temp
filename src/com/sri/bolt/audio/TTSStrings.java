package com.sri.bolt.audio;

import com.sri.bolt.App;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class TTSStrings {
   public static final String NO_SPEECH = "NO_SPEECH";
   public static final String WAIT_FOR_INTERACTION = "WAIT_FOR_INTERACTION";
   public static final String TRANSLATING = "TRANSLATING";
   public static final String UNRECOVERABLE_ERROR = "UNRECOVERABLE_ERROR";

   public static String getTTSString(String key, Language lang) {
      TTSStrings strings = TTSStrings.getTTSStrings();
      if (lang == Language.ENGLISH) {
         return strings.enStrings.getProperty(key, "");
      } else {
         return strings.iaStrings.getProperty(key, "");
      }
   }

   public static TTSStrings getTTSStrings() {
      return TTSStringsHolder.INSTANCE;
   }

   private static class TTSStringsHolder {
      private static final TTSStrings INSTANCE = new TTSStrings();
   }

   private TTSStrings() {
      enStrings = new Properties();
      iaStrings = new Properties();
      try {
         enStrings.load(new FileReader(App.getApp().getProps().getProperty("ENStringsPath")));
         iaStrings.load(new FileReader(App.getApp().getProps().getProperty("IAStringsPath")));
      } catch (IOException e) {
         logger.error("Unable to load TTS Strings:" + e.getMessage(), e);
      }

   }

   private static Logger logger = LoggerFactory.getLogger(TTSStrings.class);
   private Properties enStrings;
   private Properties iaStrings;
}
