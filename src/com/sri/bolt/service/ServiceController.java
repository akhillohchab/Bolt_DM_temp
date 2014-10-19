package com.sri.bolt.service;

import com.sri.bolt.App;
import com.sri.bolt.FileIOUtil;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.service.RecognizerFactory.RecognizerType;
import com.sri.bolt.workflow.task.TranslationTaskReturn;
import com.sri.interfaces.lang.Language;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The service controller controls the lifetime of all the service clients. It
 * also presents a service call interface between the rest of the controller and
 * the service clients. All service calls should be routed through the service
 * controller
 *
 * @author peter.blasco@sri.com
 */
public class ServiceController {
   public ServiceController() {
   }

   public void init(Properties config) {
      iaTTSPronCommand = config.getProperty("ia.TTSPron", null);
      iaTTSCustDict = config.getProperty("ia.TTSCustDict", null);

      buildClients(config);
   }

   public void initNewTrial() {
      // XXX We are selectively calling on what we know needs to be reset

      // Reset DynaSpeak(s) to clean state; same as changing speaker
      logger.info("resetting state");
      App.getApp().getServiceController().changeSpeaker();

      // On subsequent initializations, just clear some state
      ttsClientEN.reinit();
      ttsClientIA.reinit();

      mtErrorServiceClientEN.reset();
      // Reverted restart calls on 6/17/2013.
      // a) No state saved currently for dry run so no need to restart
      // b) Either shutdown is slow, unreliable, or startup is too slow;
      // have seen reconnect issues 10 seconds after translator should have
      // been stopped and restarted and should only take about 2 seconds.
      //translationClientENtoIA.reinit();
      //translationClientIAtoEN.reinit();
   }

   public void buildClients(final Properties config) {
      int numCores = Runtime.getRuntime().availableProcessors();
      ExecutorService executor;
      if (numCores < NUM_CORES_BOLT_LAPTOP) {
         logger.info("Starting single threaded init");
         executor = Executors.newSingleThreadExecutor();
      } else {
         logger.info("Starting multi threaded init");
         executor = Executors.newFixedThreadPool(numCores);
      }

      executor.submit(new Runnable() {
         @Override
         public void run() {
            uwArabicServiceClient = new UWServiceClient(config, Language.IRAQI_ARABIC);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            uwEnglishServiceClient = new UWServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientIA = RecognizerFactory.create(RecognizerType.SOCKET_IA, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientIASecondary = RecognizerFactory.create(RecognizerType.SOCKET_IA_SEC, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientIAFa = RecognizerFactory.create(RecognizerType.SOCKET_IA_FA, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            amuServiceClientEN = new AMUServiceClient(config, "en");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            amuServiceClientIA = new AMUServiceClient(config, "ia");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            ttsClientEN = new TTSServiceClientEN(config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            ttsClientIA = new TTSServiceClientIA(config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientEN = RecognizerFactory.create(RecognizerType.SOCKET_EN, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientENSecondary = RecognizerFactory.create(RecognizerType.SOCKET_EN_SEC, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dynaspeakClientENFa = RecognizerFactory.create(RecognizerType.SOCKET_EN_FA, config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            translationClientENtoIA = new TranslationServiceClient(config, "en_to_ia.Translation", "Translation-ENtoIA");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            translationTLClientENtoIA = new TranslationServiceClient(config, "en_to_ia.TranslationTL", "TranslationTL-ENtoIA");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            translationClientIAtoEN = new TranslationServiceClient(config, "ia_to_en.Translation", "Translation-IAtoEN");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            translationTLClientIAtoEN = new TranslationServiceClient(config, "ia_to_en.TranslationTL", "TranslationTL-IAtoEN");
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            // The last param "true" allows preprocessing and postprocessing commands to automatically
            // run over translation inputs and outputs
            vowelizeIA = new SRInterpServiceClient(config, "ia_vowelize.SRInterp", "SRInterp-IAVowelize", true);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            segIA = new SRInterpServiceClient(config, "ia_seg.SRInterp", "SRInterp-IASeg", true);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            confScoreClient = new ConfidenceScorerServiceClient(config);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dialogManagerEnglishClient = new DialogManagerServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            dialogManagerArabicClient = new DialogManagerServiceClient(config, Language.IRAQI_ARABIC);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            homophoneServiceClientEN = new HomophoneDetectorServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            homophoneServiceClientIA = new HomophoneDetectorServiceClient(config, Language.IRAQI_ARABIC);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            mtErrorServiceClientEN = new MTErrorDetectorSocketServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            mtErrorServiceClientIA = new MTErrorDetectorSocketServiceClient(config, Language.IRAQI_ARABIC);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            asrErrorDetectorIA = new ASRErrorDetectorServiceClient(config, Language.IRAQI_ARABIC);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            asrErrorDetectorEN = new ASRErrorDetectorServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            nameDetectorEN = new NameDetectorServiceClient(config, Language.ENGLISH);
         }
      });
      executor.submit(new Runnable() {
         @Override
         public void run() {
            ttsIAServer = new ServerStarter(config, "tts_ia_server");
         }
      });

      executor.shutdown();
      try {
         if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
            logger.error("Starting up components took too long!");
         }
      } catch (InterruptedException e) {
         logger.error("Starting up components took too long!", e);
      }
   }

   public void cleanup() {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.submit(new Runnable() {
         @Override
         public void run() {
            ttsClientEN.cleanup();
            ttsClientIA.cleanup();
            if (dynaspeakClientEN != null) {
               dynaspeakClientEN.cleanup();
            }
            if (dynaspeakClientENSecondary != null) {
               dynaspeakClientENSecondary.cleanup();
            }
            if (dynaspeakClientENFa != null) {
               dynaspeakClientENFa.cleanup();
            }
            if (dynaspeakClientIA != null) {
               dynaspeakClientIA.cleanup();
            }
            if (dynaspeakClientIASecondary != null) {
               dynaspeakClientIASecondary.cleanup();
            }
            if (dynaspeakClientIAFa != null) {
               dynaspeakClientIAFa.cleanup();
            }

            if (amuServiceClientEN != null) {
               amuServiceClientEN.cleanup();
            }
            if (amuServiceClientIA != null) {
               amuServiceClientIA.cleanup();
            }
            translationClientENtoIA.cleanup();
            translationTLClientENtoIA.cleanup();
            translationClientIAtoEN.cleanup();
            translationTLClientIAtoEN.cleanup();
            if (vowelizeIA != null) {
                vowelizeIA.cleanup();
            }
            if (segIA != null) {
                segIA.cleanup();
            }
            confScoreClient.cleanup();
            dialogManagerEnglishClient.cleanup();
            dialogManagerArabicClient.cleanup();
            homophoneServiceClientEN.cleanup();
            homophoneServiceClientIA.cleanup();
            mtErrorServiceClientEN.cleanup();
            mtErrorServiceClientIA.cleanup();
            uwEnglishServiceClient.cleanup();
            uwArabicServiceClient.cleanup();
            asrErrorDetectorEN.cleanup();
            asrErrorDetectorIA.cleanup();
            nameDetectorEN.cleanup();
            ttsIAServer.kill();
         }
      });

      executor.shutdown();
      try {
         if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            logger.error("Clean shutdown failed, killing services");
            killServices();
         }
      } catch (InterruptedException e) {
         logger.error("Interrupted Clean shutdown failed, killing services");
         killServices();
      }
   }

   public void killServices() {
      dynaspeakClientEN.killEndpoint();
      dynaspeakClientENSecondary.killEndpoint();
      dynaspeakClientENFa.killEndpoint();
      dynaspeakClientIA.killEndpoint();
      dynaspeakClientIASecondary.killEndpoint();
      dynaspeakClientIAFa.killEndpoint();
      amuServiceClientEN.killEndpoint();
      amuServiceClientIA.killEndpoint();
      /*
       * Below are placeholders; don't have this call for these
      translationClientENtoIA.killEndpoint();
      translationTLClientENtoIA.killEndpoint();
      translationClientIAtoEN.killEndpoint();
      translationTLClientIAtoEN.killEndpoint();
      vowelizeIA.killEndpoint();
      segIA.killEndpoint();
      */
      uwEnglishServiceClient.killEndpoint();
      uwArabicServiceClient.killEndpoint();
      asrErrorDetectorEN.killEndpoint();
      asrErrorDetectorIA.killEndpoint();
      ttsIAServer.kill();
   }

   // Outbound ServiceCalls
   public void textToSpeech(String text, String fileName, Language lang) {
      if (lang == Language.IRAQI_ARABIC) {
         ttsClientIA.textToSpeechFile(text, fileName);
      } else {
         ttsClientEN.textToSpeechFile(text, fileName);
      }
   }

   /* New IA TTS Server does this internally
   // Two steps in one, vowelize then call script for pron
   public String getIAPron(String text) {
      if ((iaTTSPronCommand == null) || (text == null)) {
         return null;
      }

      String plainInput = text.trim();
      if (plainInput.length() == 0) {
         return "";
      }

      String spacedInput = "" + plainInput.charAt(0);
      for (int j = 1; j < plainInput.length(); j++) {
         spacedInput += " " + plainInput.charAt(j);
      }

      String vowelizedSpaced = vowelizeIA.getBest(spacedInput);

      // Trim all space
      String vowelized = vowelizedSpaced.replaceAll("\\s+", "");

      // Output form is:
      // word pr on
      // with possibly multiple lines so take only first
      String pronRaw = launchFilter(iaTTSPronCommand, vowelized);

      String[] alts = pronRaw.split("[\r\n]+");
      String pron = null;

      if ((alts != null) && (alts.length >= 0)) {
         int idx = alts[0].indexOf(' ');
         if (idx >= 0) {
            pron = alts[0].substring(idx + 1).trim();
         }
      }

      return pron;
   }
   */

   /* New IA TTS Server does this internally
   public void appendIAProns(ArrayList<String> custDictAppend) {
      if ((custDictAppend == null) || (custDictAppend.size() == 0)) {
         return;
      }

      boolean append = true;
      try {
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(iaTTSCustDict, append)));
         for (int i = 0; i < custDictAppend.size(); i++) {
            out.println(custDictAppend.get(i));
         }
         out.close();
      } catch (IOException e) {
      }
      // Also save parallel log file to show words used, not cleared
      try {
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(iaTTSCustDict + ".log", append)));
         for (int i = 0; i < custDictAppend.size(); i++) {
            out.println(custDictAppend.get(i));
         }
         out.close();
      } catch (IOException e) {
      }
   }
   */

   public String segmentIA(String text) {
      if ((segIA == null) || (text == null)) {
         return text;
      }

      String result = segIA.getBest(text);

      // getBest can return null upon error; and it
      // looks like an empty input string can trigger this.
      return (result != null)?result:"";
   }

    /*
   public void playAudioFile(String program, String filename) {
      // Simple enough to just do process builder directly
      ProcessBuilder pb = new ProcessBuilder(program, filename);
      try {
         pb.start();
      } catch (IOException e) {
         App.getApp().getLogger().logMessage(e.getMessage(), LogType.ERROR);
      }
   }
    */

   // Also used by SRInterpServiceClient to handle pre- and post-processing.
   // (package scope)
   static String launchFilter(String command, String input) {
      if ((command != null) && (command.length() > 0)) {
         try {
            return launchForStdout(command, input + "\n", input).trim();
         } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return input;
         }
      } else {
         return input;
      }
   }

   private static String launchForStdout(String prog, String stdin, String defReturn) throws InterruptedException {
      // Default to the input
      String output = defReturn;
      try {
         Runtime rt = Runtime.getRuntime();
         Process p = rt.exec(prog);
         OutputStream os = p.getOutputStream();
         byte[] inData = stdin.getBytes("UTF8");
         os.write(inData);
         // Need to convince it we won't send more
         os.close();
         p.waitFor();
         InputStream is = p.getInputStream();
         byte[] data = FileIOUtil.readFully(is);
         output = new String(data, "UTF8");
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }

      return output;
   }

   // For a first-pass recognizer (2nd pass forced alignment)
   public void startSamples(RecognizerType whichRecognizer, int whichLM) {
      logger.info("startSamples(" + whichRecognizer + ", " + whichLM + ")");

      boolean useMulti = false;
      DynaSpeakSocketServiceClient curClient = null;

      // For English, choices are:
      // lm index: 0=main, 1=clarify, 2=options, 3=spell
      // grammar names: main, clarify, options, spell
      switch (whichRecognizer) {
      case SOCKET_EN:
         curClient = dynaspeakClientEN;
         useMulti = true;
         break;
      case SOCKET_EN_SEC:
         curClient = dynaspeakClientENSecondary;
         useMulti = true;
         break;
      case SOCKET_IA:
         curClient = dynaspeakClientIA;
         useMulti = true;
         break;
      case SOCKET_IA_SEC:
         curClient = dynaspeakClientIASecondary;
         useMulti = true;
         break;
      default:
         // Shouldn't happen
         return;
      }

      if (useMulti) {
         curClient.setRescoringLMIndex(whichLM);
         String grammarName = null;
         switch (whichLM) {
         case 1:
            grammarName = "clarify";
            break;
         case 2:
            grammarName = "options";
            break;
         case 3:
            grammarName = "spell";
            break;
         default:
            grammarName = "main";
            break;
         }
         curClient.setGrammar(grammarName);
     }

     // All cases:
     curClient.startSamples();
   }

   public void sendSamples(RecognizerType whichRecognizer, byte[] audio) {
      switch (whichRecognizer) {
         case SOCKET_EN:
            dynaspeakClientEN.sendSamples(audio);
            break;
         case SOCKET_EN_SEC:
            dynaspeakClientENSecondary.sendSamples(audio);
            break;
         case SOCKET_IA:
            dynaspeakClientIA.sendSamples(audio);
            break;
         case SOCKET_IA_SEC:
            dynaspeakClientIASecondary.sendSamples(audio);
            break;
         default:
            // Shouldn't happen
      }
   }

   public CombinedRecognizerResult endSamples(RecognizerType whichRecognizer) {
      CombinedRecognizerResult retval = null;
      switch (whichRecognizer) {
         case SOCKET_EN:
            retval = dynaspeakClientEN.endSamples();
            break;
         case SOCKET_EN_SEC:
            retval = dynaspeakClientENSecondary.endSamples();
            break;
         case SOCKET_IA:
            retval = dynaspeakClientIA.endSamples();
            break;
         case SOCKET_IA_SEC:
            retval = dynaspeakClientIASecondary.endSamples();
            break;
         default:
            // Shouldn't happen
      }
      return retval;
   }

   public void setFAGrammar(String rescoredOneBest, Language lang) {
      logger.info("setFAGrammar(" + rescoredOneBest + ", " + lang + ")");
      if (lang == Language.ENGLISH) {
         dynaspeakClientENFa.extendGrammarWrapper(rescoredOneBest);
      } else {
         dynaspeakClientIAFa.extendGrammarWrapper(rescoredOneBest);
      }
   }

   public void startFASamples(Language lang) {
      if (lang == Language.ENGLISH) {
         dynaspeakClientENFa.startSamples();
      } else {
         dynaspeakClientIAFa.startSamples();
      }
   }

   public void sendFASamples(byte[] audio, Language lang) {
      if (lang == Language.ENGLISH) {
         dynaspeakClientENFa.sendSamples(audio);
      } else {
         dynaspeakClientIAFa.sendSamples(audio);
      }
   }

   public CombinedRecognizerResult endFASamples(Language lang) {
      if (lang == Language.ENGLISH) {
         return dynaspeakClientENFa.endSamples();
      } else {
         return dynaspeakClientIAFa.endSamples();
      }
   }

   public SessionData uwProcessData(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return uwEnglishServiceClient.process(data);
      } else {
         return uwArabicServiceClient.process(data);
      }
   }

   public SessionData amuProcessSentence(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return amuServiceClientEN.process(data);
      } else {
         return amuServiceClientIA.process(data);
      }
    }

   public SessionData ConfidenceScorerProcessData(SessionData data) {
      return confScoreClient.process(data);
   }

   public void changeSpeaker() {
      logger.debug("Changing speaker");
      if (!dynaspeakClientEN.resetMllr()) {
         logger.error("Reset mllr failed on Dynaspeak Client EN");
      }
      if (!dynaspeakClientENSecondary.resetMllr()) {
         logger.error("Reset mllr failed on Dynaspeak Client EN Secondary");
      }
      if (!dynaspeakClientENFa.resetMllr()) {
         logger.error("Reset mllr failed on Dynaspeak FA Client EN");
      }
      if (!dynaspeakClientIA.resetMllr()) {
          logger.error("Reset mllr failed on Dynaspeak Client IA");
      }
      if (!dynaspeakClientIASecondary.resetMllr()) {
          logger.error("Reset mllr failed on Dynaspeak Client IA Secondary");
      }
      if (!dynaspeakClientIAFa.resetMllr()) {
         logger.error("Reset mllr failed on Dynaspeak FA Client IA");
      }
   }

   /*
   public String translateText(String text) {
      return translateTextExtended(text).resultText;
   }
   */

   // Just do it inline - will be fast
   public String translatePreprocess(String text, Language lang) {
      // Strip any hesitation words that would have come from ASR that
      // begin with %. Also strip @reject@ in case it showed up.
      text = TextProcessing.cleanForTranslationPreprocessing(text);

      // If set in ini, call IA segmenter as SRInterp service prior to translation
      if (lang == Language.IRAQI_ARABIC) {
         if (translationClientIAtoEN.callSegmenter) {
            logger.info("translatePreprocess segmentIA before: " + text);
            text = segmentIA(text);
            logger.info("translatePreprocess segmentIA after: " + text);
         }
      }

      String preprocessed;
      TranslationServiceClient client;
      switch (lang) {
      case IRAQI_ARABIC:
         client = translationClientIAtoEN;
         break;
      default:
         // Default is English
         client = translationClientENtoIA;
      }
      preprocessed = launchFilter(client.preprocess, text);
      //if (lang == Language.IRAQI_ARABIC) {
         logger.info("translatePreprocess input: " + text);
         boolean isDifferent = (text.compareTo(preprocessed) != 0);
         logger.info("translatePreprocess output (different=" + isDifferent + "): " + preprocessed);
      //}
      if (preprocessed != null && client.specialTmTableExists()) {
         String[] words = preprocessed.trim().toLowerCase().split("\\s+");
         String result = "";
         for (int i = 0; i < words.length; i++) {
            String specialTrans = client.getSpecialTranslation(words[i]);
            if (specialTrans != null)
               result += specialTrans + " ";
            else
               result += words[i] + " ";
         }
         return result.trim();
      }
      return preprocessed;
   }

   // Note that currently TranslationTask calls translatePreprocess or similar so
   // isPreprocessed is expected to be true.
   public TranslationTaskReturn translateTextExtended(String text, Language lang, boolean isPreprocessed) {
      String preprocessedInput = isPreprocessed ? text : translatePreprocess(text, lang);

      TranslationServiceClient client;
      switch (lang) {
      case IRAQI_ARABIC:
         client = translationClientIAtoEN;
         break;
      default:
         // Default is English
         client = translationClientENtoIA;
      }

      TranslationTaskReturn returnVal = client.translateTextExtendedOutput(preprocessedInput);
      //logger.info("translateTextExtended(" + text + ", " + lang + ", " + isPreprocessed + ") for '" + preprocessedInput + "' is " + returnVal);
      String postprocessed = launchFilter(client.postprocess, returnVal.getOriginalTranslation());
      returnVal.setResultText(postprocessed);
      returnVal.setPreprocessedInput(preprocessedInput);
      return returnVal;
   }

   /*
   public String translateTLText(String text) {
      return translateTLTextExtended(text).resultText;
   }
   */

   public TranslationTaskReturn translateTLTextExtended(String text, Language lang) {
      TranslationServiceClient client;
      switch (lang) {
      case IRAQI_ARABIC:
         client = translationTLClientIAtoEN;
         break;
      default:
         client = translationTLClientENtoIA;
      }
      String processedInput = launchFilter(client.preprocess, text);
      TranslationTaskReturn returnVal = client.translateTextExtendedOutput(processedInput);
      String postprocessed = launchFilter(client.postprocess, returnVal.getOriginalTranslation());

      returnVal.setResultText(postprocessed);
      returnVal.setPreprocessedInput(processedInput);
      return returnVal;
   }

   public SessionData DialogManagerProcessData(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return dialogManagerEnglishClient.process(data);
      } else {
         return dialogManagerArabicClient.process(data);
      }
   }

   public SessionData processHomophones(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return homophoneServiceClientEN.process(data);
      } else {
         return homophoneServiceClientIA.process(data);
      }
   }

   public SessionData processMTErrors(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return mtErrorServiceClientEN.process(data);
      } else {
         return mtErrorServiceClientIA.process(data);
      }
   }

   public SessionData detectSenses(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return mtErrorServiceClientEN.detectSenses(data);
      } else {
         return mtErrorServiceClientIA.detectSenses(data);
      }
   }

   public SessionData processASRErrors(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return asrErrorDetectorEN.process(data);
      } else {
         return asrErrorDetectorIA.process(data);
      }
   }

   public SessionData detectNames(SessionData data, Language lang) {
      if (lang == Language.ENGLISH) {
         return nameDetectorEN.process(data);
      } else {
         return nameDetectorEN.process(data);
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
   private DynaSpeakSocketServiceClient dynaspeakClientEN;
   private DynaSpeakSocketServiceClient dynaspeakClientENSecondary;
   private DynaSpeakSocketServiceClient dynaspeakClientENFa;
   private DynaSpeakSocketServiceClient dynaspeakClientIA;
   private DynaSpeakSocketServiceClient dynaspeakClientIASecondary;
   private DynaSpeakSocketServiceClient dynaspeakClientIAFa;
   private TTSServiceClient ttsClientEN;
   private TTSServiceClient ttsClientIA;
   private TranslationServiceClient translationClientENtoIA;
   private TranslationServiceClient translationTLClientENtoIA;
   private TranslationServiceClient translationClientIAtoEN;
   private TranslationServiceClient translationTLClientIAtoEN;
   private SRInterpServiceClient vowelizeIA;
   private SRInterpServiceClient segIA;
   private UWServiceClient uwEnglishServiceClient;
   private UWServiceClient uwArabicServiceClient;
   private AMUServiceClient amuServiceClientEN;
   private AMUServiceClient amuServiceClientIA;
   private ConfidenceScorerServiceClient confScoreClient;
   private DialogManagerServiceClient dialogManagerEnglishClient;
   private DialogManagerServiceClient dialogManagerArabicClient;
   private HomophoneDetectorServiceClient homophoneServiceClientEN;
   private HomophoneDetectorServiceClient homophoneServiceClientIA;
   private MTErrorDetectorSocketServiceClient mtErrorServiceClientEN;
   private MTErrorDetectorSocketServiceClient mtErrorServiceClientIA;
   private ASRErrorDetectorServiceClient asrErrorDetectorIA;
   private ASRErrorDetectorServiceClient asrErrorDetectorEN;
   private NameDetectorServiceClient nameDetectorEN;
   private NameDetectorServiceClient nameDetectorIA;
   private ServerStarter ttsIAServer;

   private static int NUM_CORES_BOLT_LAPTOP = 8;

   private String iaTTSPronCommand;
   private String iaTTSCustDict;
}
