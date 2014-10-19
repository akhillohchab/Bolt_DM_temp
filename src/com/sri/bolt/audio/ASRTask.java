package com.sri.bolt.audio;

import com.sri.bolt.App;
import com.sri.bolt.TimeKeeper;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.service.RecognizerFactory.RecognizerType;
import com.sri.bolt.state.ASRState;
import com.sri.interfaces.lang.Language;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.*;

public class ASRTask implements Callable<ASRResult> {
   public ASRTask(Language lang) {
      this.lang = lang;
      inputTask = new LiveASRInputTask();
      endSamplesExecutor = Executors.newFixedThreadPool(2);
   }

   public ASRTask(Language lang, File audioFile) {
      this.lang = lang;
      inputTask = new FileASRInputTask(audioFile);
      endSamplesExecutor = Executors.newFixedThreadPool(2);
   }

   public ASRTask(Language lang, String mixer) {
      this.lang = lang;
      inputTask = new LiveASRInputTask(mixer);
      endSamplesExecutor = Executors.newFixedThreadPool(2);
   }

   @Override
   public ASRResult call() {
      ASRState state = new ASRState();
      state.buttonPressed();
      inputTask.setASRState(state);
      ASRResult errReturn = new ASRResult(state, false, "Error occured during audio input");
      TaskReturn inputTaskReturn = null;
      TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
      try {
         timeKeeper.startTiming("ASR Input " + (lang == Language.IRAQI_ARABIC ? "IA" : "EN"));
         inputTaskReturn = inputTask.call();
         timeKeeper.stopTiming("ASR Input " + (lang == Language.IRAQI_ARABIC ? "IA" : "EN"));
      } catch (Exception e) {
         logger.error("ASR failed with exception:" + e, e);
         e.printStackTrace();
         return errReturn;
      }


      // Note, above might have had some sort of undetected/unmarked failure
      // and the endSamples() should catch the problem.
      if (!inputTaskReturn.success) {
         logger.error("ASR result unsuccessful:" + inputTaskReturn.resultText);
         return errReturn;
      }

      // We exit above since we don't want to call endSamples() on the recognizer
      // to get the result if any steps above failed. Note that if any steps
      // did fail, the recognizer is in a bad state (or dead) so something
      // is detecting this and restarting the recognizer.
      CombinedRecognizerResult primaryResult;
      CombinedRecognizerResult secondaryResult = null;
      if (inputTask.secondRecognizerType != null) {
         EndSamplesCallable firstRecogCallable = new EndSamplesCallable(inputTask.recognizerType);
         EndSamplesCallable secondRecogCallable = new EndSamplesCallable(inputTask.secondRecognizerType);

         Future<CombinedRecognizerResult> firstFuture = endSamplesExecutor.submit(firstRecogCallable);
         Future<CombinedRecognizerResult> secondFuture = endSamplesExecutor.submit(secondRecogCallable);
         try {
            primaryResult = firstFuture.get();
            secondaryResult = secondFuture.get();
         } catch (InterruptedException e) {
            logger.error("ASR failed with exception:" + e, e);
            e.printStackTrace();
            return errReturn;
         } catch (ExecutionException e) {
            logger.error("ASR failed with exception:" + e, e);
            e.printStackTrace();
            return errReturn;
         }
      } else {
         primaryResult = App.getApp().getServiceController().endSamples(inputTask.recognizerType);
      }
      state.setRecogResult(primaryResult);
      state.setSecondRecogResult(secondaryResult);
      state.setType(inputTask.recognizerType);
      state.setAudioData(getAudioData());
      state.setLanguage(lang);

      // Note that we can't pass an empty WCN to UW but
      // we now have code to create a simple WCN.
      boolean allowEmptyWcn = true;
      // HTK Lattice unused so don't require it starting 7/1/2013.
      boolean allowEmptyHtkLattice = true;
      String resText = null;
      if (primaryResult.hasText()) {
         resText = primaryResult.getText();
      }
      // cleanForDisplay added on 9/18/2013 so stripping of um/uh
      // will cause string to be empty/invalid versus cleaning it
      // later and trying to translate an empty string.
      if ((resText == null) || (resText.trim().length() == 0) ||
          (com.sri.bolt.service.TextProcessing.cleanForDisplay(resText, lang).length() == 0) ||
          (!allowEmptyWcn && !primaryResult.hasConfusionNetwork()) ||
          (!allowEmptyHtkLattice && !primaryResult.hasHtkLattice()) ||
          getAudioData().size() == 0) {
          logger.info("Ignoring " + lang + " inadequate primary ASR result, audio size(" + getAudioData().size() + ") text: " + resText);
         return errReturn;
      } else {
         return new ASRResult(state, true, "");
      }
   }

   public void stop() {
      inputTask.stop();
   }

   public void setSessionData(BoltMessages.SessionData data) {
      inputTask.setInput(data, lang);
   }

   public Language getLanguage() {
      return lang;
   }

   private ByteArrayOutputStream getAudioData() {
      return inputTask.getFullData();
   }


   private class EndSamplesCallable implements Callable<CombinedRecognizerResult> {
      public EndSamplesCallable(RecognizerType recognizerType) {
         this.recognizerType = recognizerType;
      }

      @Override
      public CombinedRecognizerResult call() throws Exception {
         return App.getApp().getServiceController().endSamples(recognizerType);
      }

      private RecognizerType recognizerType;
   }

   private ExecutorService endSamplesExecutor;
   private static final Logger logger = LoggerFactory.getLogger(ASRTask.class);
   private ASRInputTask inputTask;
   private Language lang;
}
