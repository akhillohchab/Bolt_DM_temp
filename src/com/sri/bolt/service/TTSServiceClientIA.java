package com.sri.bolt.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import com.sri.bolt.App;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TTSServiceClientIA implements TTSServiceClient {
   public TTSServiceClientIA(Properties config) {
      ttsPath = config.getProperty("ia.TTSPath");
      ttsExe = config.getProperty("ia.TTSExe");
      ttsVocabFilename = config.getProperty("ia.TTSVocab");
      ttsCustDict = config.getProperty("ia.TTSCustDict");
      ttsPausePeriod = config.getProperty("ia.TTSPausePeriod", "-pau- -pau- -pau- -pau-");
      ttsPauseComma = config.getProperty("ia.TTSPauseComma", "-pau- -pau-");

      /* Not used for TTS Server
      ttsVocab = new HashMap<String, String>();
      ttsVocabCust = new HashMap<String, String>();

      File ttsFile = null;
      if (ttsVocabFilename != null) {
         ttsFile = new File(ttsVocabFilename);
      }
      if ((ttsFile != null) && (ttsFile.exists())) {
         try {
            BufferedReader r = new BufferedReader(new FileReader(ttsFile));
            String word;
            while ((word = r.readLine()) != null) {
               word = word.trim();
               if (word.length() > 0) {
                  // We just want to look up the word.
                  ttsVocab.put(word, "");
               }
            }
         } catch (IOException e) {
            logger.error("Error loading IA TTS prons: " + ttsVocabFilename, e);
            e.printStackTrace();
         }
      }

      logger.info("Loaded " + ttsVocab.size() + " IA TTS vocab words from " + ttsVocabFilename);

      // loadTTSCustProns();
      clearTTSCustProns();
      */
   }

   /* Unused for TTS Server
   // Not called currently since we want to start with a clean
   // state.
   private void loadTTSCustProns() {
      File ttsFileCust = null;
      if (ttsCustDict != null) {
         ttsFileCust = new File(ttsCustDict);
      }
      // Format of this file is:
      // ("word", (pron))
      if ((ttsFileCust != null) && (ttsFileCust.exists())) {
         try {
            BufferedReader r = new BufferedReader(new FileReader(ttsFileCust));
            String word;
            while ((word = r.readLine()) != null) {
               word = word.trim();
               int idx = word.indexOf('\"');
               if (idx >= 0) {
                  word = word.substring(idx + 1);
                  idx = word.indexOf('\"');
                  if (idx >= 0) {
                     word = word.substring(0, idx).trim();
                     if (word.length() > 0) {
                        // We just want to look up the word.
                        ttsVocabCust.put(word, "");
                     }
                  }
               }
            }
         } catch (IOException e) {
            logger.error("Error loading IA cust TTS prons: " + ttsCustDict, e);
            e.printStackTrace();
         }

         logger.info("Total " + (ttsVocab.size() + ttsVocabCust.size()) + " IA TTS vocab after loading " + ttsCustDict);
      }
   }
   */

   /* Unused for new TTS server
   // Reset pronunciations
   private void clearTTSCustProns() {
      // Remove existing file.
      File ttsFileCust = null;
      if (ttsCustDict != null) {
         ttsFileCust = new File(ttsCustDict);
         if (ttsFileCust.exists()) {
            ttsFileCust.delete();
         }
      }

      // Create simple file. This is to have a non-empty file that
      // exists.
      ArrayList<String> custDictAppend = new ArrayList<String>();
      custDictAppend.add("(\"himike\" nil (m i k))");
      App.getApp().getServiceController().appendIAProns(custDictAppend);

      // Clear our internal data structure, except add the himike
      // example above just to be consistent.
      ttsVocabCust.clear();
      ttsVocabCust.put("himike", "");
   }
   */

   /* Unused for new TTS server
   // Strip unknown IA words from string prior to generation (temp)
   // and add unknowns to out variable.
   private String stripUnknowns(String input, ArrayList<String> unknowns) {
      // Assume all words are known if couldn't load vocab
      if (ttsVocab.size() == 0) {
         return input;
      }

      if (input == null) {
         return input;
      }

      input = input.trim();

      if (input.length() == 0) {
         return input;
      }

      String[] words = input.split("\\s+");

      String output = "";
      for (int i = 0; i < words.length; i++) {
         if (ttsVocab.containsKey(words[i]) || ttsVocabCust.containsKey(words[i])) {
            output += words[i] + " ";
         } else if (unknowns != null) {
            unknowns.add(words[i]);
         }
      }

      return output.trim();
   }
   */

   @Override
   public void init() {
      logger.info("TTSServiceClientIA init called");
   }

   @Override
   public void reinit() {
      /* Unused for new TTS server
      logger.info("TTSServiceClientIA reinit called; clearing custom pron state");

      // Goal of this is to return to initial state.
      clearTTSCustProns();
      */
   }

   // Text may be in UTF-8 or Buckwalter. We will convert to Buckwalter
   // in either case which should essentially be a no-op if already
   // in Buckwalter.
   public void textToSpeechFile(String text, String filePath) {
      logger.info("Raw IA TTS: " + text);

      String modifiedText = BuckwalterUnicodeConverter.unicodeToBuckwalter(text, Language.IRAQI_ARABIC, false);

      // Convert . and , to pauses marked with -pau-
      modifiedText = modifiedText.
         replaceAll("[.]", " " + ttsPausePeriod + " ").
         replaceAll("[,]", " " + ttsPauseComma + " ").
         replaceAll("\\s+", " ");

      // Strip any @reject@ since TTS fails on it.
      // Also replace words that start with % and
      // strip ? (since looks like attached to word then word interpreted as unknown).

      modifiedText = TextProcessing.cleanForTTS(modifiedText, Language.IRAQI_ARABIC);

      /* Unused for TTS Server
      logger.info("Cleaned IA TTS: " + modifiedText);

      ArrayList<String> unknowns = new ArrayList<String>();

      // Strip %hesitation words, @reject@, ?, etc.
      String strippedText = stripUnknowns(modifiedText, unknowns);

      if (unknowns.size() > 0) {
         String msg = "IA TTS Unknowns (added to cust dict):";
         ArrayList<String> custDictAppend = new ArrayList<String>();
         // Since we will be adding the words to dictionary, we will mark them as known
         for (int i = 0; i < unknowns.size(); i++) {
            // Possible very rare case where unknown appears twice in initial
            // sentence and we add to output file twice.
            if (ttsVocabCust.containsKey(unknowns.get(i))) {
               continue;
            }

            String pron = App.getApp().getServiceController().getIAPron(unknowns.get(i));
            if (pron == null) {
               logger.error("getIAPron returned null for: " + unknowns.get(i));
            } else {
               pron = pron.trim();

               // Write output of form:
               // ("himike" nil (m i k))
               // aka: ("word" nil (pr on))
               String dictForm = "(\"" + unknowns.get(i) + "\" nil (" + pron + "))";
               custDictAppend.add(dictForm);

               // The word is now known so don't look it up next time
               ttsVocabCust.put(unknowns.get(i), "");

               msg += " " + unknowns.get(i) + "(pron:" + pron + ")";
            }
         }
         logger.info(msg);

         App.getApp().getServiceController().appendIAProns(custDictAppend);
      }
      // Can pass strippedText for unknowns stripped but instead pass modifiedText
      // since should have added unknowns to dictionary.
      ProcessBuilder pb = new ProcessBuilder("perl", "./" + ttsExe, "-o", filePath, modifiedText);
      */

      logger.info("Generating IA TTS for '" + modifiedText + "' to file " + filePath);

      ProcessBuilder pb = new ProcessBuilder("./" + ttsExe, "-o", filePath, modifiedText);
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

   private static final Logger logger = LoggerFactory.getLogger(TTSServiceClientIA.class);
   private final String ttsPath;
   private final String ttsExe;
   private final String ttsVocabFilename;
   private final String ttsCustDict;
   private final String ttsPausePeriod;
   private final String ttsPauseComma;

   /* Unused for TTS Server
   private HashMap<String, String> ttsVocab;
   private HashMap<String, String> ttsVocabCust;
   */
}
