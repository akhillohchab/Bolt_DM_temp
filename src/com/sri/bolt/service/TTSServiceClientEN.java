package com.sri.bolt.service;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TTSServiceClientEN implements TTSServiceClient {
   public TTSServiceClientEN(Properties config) {
      ttsPath = config.getProperty("TTSPath");
      ttsExe = config.getProperty("TTSExe");
   }

   @Override
   public void init() {
   }

   @Override
   public void reinit() {
      logger.info("TTSServiceClientEN reinit called");

      // Do nothing here; no state to reset
   }

   public void textToSpeechFile(String text, String filePath) {
      // replace ASR acronym separators
      String modifiedText = text.replaceAll("\\.\\_", ".");
      ProcessBuilder pb = new ProcessBuilder("./" + ttsExe, "-o", filePath, "\"" + modifiedText + "\"");
      pb.directory(new File(ttsPath));
      try {
         Process proc = pb.start();
         proc.waitFor();
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      } catch (InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public void cleanup() {
   }

   private static final Logger logger = LoggerFactory.getLogger(TTSServiceClientEN.class);
   private String ttsPath;
   private String ttsExe;
}
