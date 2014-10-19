package com.sri.bolt.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.sri.bolt.workflow.task.TranslationTaskReturn;

public class TranslationServiceClient extends SRInterpServiceClient {

   public TranslationServiceClient(Properties config, String prefix, String logname) {
      super(config, prefix, logname);

      this.callSegmenter = Boolean.parseBoolean(config.getProperty(prefix + "CallSegmenter", "false"));

      readSpecialTmTable(config.getProperty(prefix + "SpecialTM", null));
   }

   private void readSpecialTmTable(String filename) {
      if (filename != null) {
         try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            //App.getApp().getLogger().debug("Reading special TM table " + filename);
            String line;
            specialTm = new HashMap<String, String>();
            while ((line = br.readLine()) != null){
               String[] fields = line.split(" \\|\\|\\| ");
               String key = fields[1];
               String value = fields[0] + " { " + fields[2] + " }";
               specialTm.put(key, value);
            }
         }
         catch (Exception e) {

         }
      }
   }

   public boolean specialTmTableExists(){
      return specialTm != null && specialTm.size() > 0;
   }

   public String getSpecialTranslation(String key){
      if (specialTm != null)
         return specialTm.get(key);
      return null;
   }

   public TranslationTaskReturn translateTextExtendedOutput(String text) {
      HashMap<String, String> rawValues = getRawTranslationOutputs(text);

      TranslationTaskReturn returnVal = null;

      if (rawValues != null) {
         returnVal = new TranslationTaskReturn();

         returnVal.setnBest(extractNBestList(rawValues));
         returnVal.setOriginalTranslation(rawValues.get(TRANSLATION_KEY));
         returnVal.setAlignment(rawValues.get(ALIGNMENT_KEY));
         return returnVal;
      }

      return returnVal;
   }

   /**
    * @deprecated Use translateTextExtendedOutput then
    * lookup TRANSLATION_KEY and ALIGNMENT_KEY.
    */
   // Call with output from translateTextExtendedOutput(String text)
   private static String extractTranslationAndAlignment(Map<String, String> translationResult) {
      if ((translationResult == null) || (!translationResult.containsKey(TRANSLATION_KEY))) {
         return EMPTY_TRANSLATION;
      }

      // Sample value of what we should set string to:
      // <s> knt </s> ||| 0-0,1-1,2-1,3-2
      String translationAndAlignment = EMPTY_TRANSLATION;
      // read output
      translationAndAlignment = translationResult.get(TRANSLATION_KEY);
      if (translationResult.containsKey(ALIGNMENT_KEY)) {
         String align = translationResult.get(ALIGNMENT_KEY);
         translationAndAlignment = translationAndAlignment +
               TranslationTaskReturn.ALIGNMENT_DELIMETER +
               align;
      }

      return translationAndAlignment;
   }

   private Map<String, String> specialTm;

   // Just need package visibility
   final boolean callSegmenter;
}
