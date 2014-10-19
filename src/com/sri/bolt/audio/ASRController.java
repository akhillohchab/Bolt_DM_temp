package com.sri.bolt.audio;

import com.sri.bolt.App;
import com.sri.bolt.EvalType;
import com.sri.bolt.TimeKeeper;
import com.sri.bolt.state.ASRState;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.state.TrialState;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: pblasco
 * Date: 4/1/13
 * Time: 10:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class ASRController implements Runnable {
   public ASRController() {
      executor = Executors.newSingleThreadExecutor();
   }

   public boolean startASR(Language lang) {

      boolean retval = false;
      if (!App.getApp().getIsProcessing().get()) {
         if (canDoASR(lang)) {

            //we cannot save state between inputs for activity b retest
            if (App.getApp().getEvalType() == EvalType.ACTIVITY_B_RETEST) {
               App.getApp().getServiceController().initNewTrial();
            }
            // Before capturing, play a short beep to indicate recording
            Playback.playAudioResource(App.AUDIO_RESOURCE_PTT_PRESSED, lang, null, true);

            String prefix = "";
            if (lang == Language.IRAQI_ARABIC) {
                prefix = "ia.";
            }

            String audioCaptureDevice = "";
            if (App.getApp().getProps().getProperty(prefix + "AudioCaptureDevice") != null) {
               audioCaptureDevice = App.getApp().getProps().getProperty(prefix + "AudioCaptureDevice");
               logger.debug("The user selected audio capture device: " + audioCaptureDevice);
            }

            if (audioCaptureDevice.equals("")) {
               asrTask = new ASRTask(lang);
            } else {
               asrTask = new ASRTask(lang, audioCaptureDevice);
            }

            executor.submit(this);
            retval = true;
         } else {
            Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.WAIT_FOR_INTERACTION, lang), lang);
            return false;
         }
      }

      return retval;
   }

   public ASRState startASR(File audioFile, Language lang, boolean waitForCompletion) {
      if (!App.getApp().getIsProcessing().get()) {
         if (canDoASR(lang)) {
            asrTask = new ASRTask(lang, audioFile);
            if (waitForCompletion) {
               return runASR();
            } else {
               executor.submit(this);
            }
         } else {
            Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.WAIT_FOR_INTERACTION, lang), lang);
         }
      }

      return null;
   }

   public void stopASR() {
      if (App.getApp().getIsProcessing().get() && asrTask != null) {
         if (App.getApp().playAudio()) {
            // Just let it run in background
            Playback.playAudioResource(App.AUDIO_RESOURCE_PROCESSING, asrTask.getLanguage());
         }
         asrTask.stop();
      }
   }

   /* Unused
   public void abort() {
      cleanup();
      executor = Executors.newSingleThreadExecutor();
   }
   */

   public void cleanup() {
      executor.shutdown();
      try {
         if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
         }
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   @Override
   public void run() {
      ASRState state = runASR();
      if (state != null) {
         App.getApp().getWorkflowController().addASRState(state);
      }
   }

   private ASRState runASR() {
      if (App.getApp().getIsProcessing().getAndSet(true)) {
         return null;
      }

      TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
      timeKeeper.startRun();
      timeKeeper.startTiming("Total workflow " + (asrTask.getLanguage() == Language.IRAQI_ARABIC ? "IA" : "EN"));

      TrialState trial = App.getApp().getTrial();
      trial.onStartASR(asrTask.getLanguage());
      InteractionState interaction = trial.getCurrentInteraction();

      asrTask.setSessionData(interaction.getSessionData());
      try {
         timeKeeper.startTiming("ASR");
         ASRResult result = asrTask.call();
         timeKeeper.startTiming("ASR");
         if (result.successful) {
            ASRState asrState = result.state;
            asrState.setAsrComplete(new Date());
            // Don't log this to screen, just stdout (since changes often when rescoring)
            //App.getApp().getLogger().result("DynaSpeak ASR result: " + recogResult.getText());
            logger.info("DynaSpeak ASR result: " + asrState.getAsrResultString());
            return asrState;

         } else {
            Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.NO_SPEECH, interaction.getLanguage()), interaction.getLanguage());
            logger.error(TTSStrings.getTTSString(TTSStrings.NO_SPEECH, Language.ENGLISH));
            App.getApp().getIsProcessing().set(false);
            return null;
         }
      } catch (Exception e) {
         Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.NO_SPEECH, interaction.getLanguage()), interaction.getLanguage());
         logger.error("Exception: " + e + " found while doing ASR", e);
         e.printStackTrace();
      }



      App.getApp().getIsProcessing().set(false);
      return null;
   }

   private boolean canDoASR(Language lang) {
      TrialState trial = App.getApp().getTrial();
      //if we haven't yet had an interaction OR
      //we are still on the current speaker OR
      //we are on a new speaker but have finished the last speaker's utterance
      // OR might have started but no real data yet.
      //then we can record
      //otherwise we make them wait
      // If language mismatch, see if finished or can back out
      boolean retval = false;
      if (trial.getCurrentInteraction() == null) {
          retval = true;
      } else if (trial.getCurrentInteraction().getLanguage() == lang) {
          retval = true;
      } else {
          // Language mismatch
          // See if interaction finished or not sufficiently started.
          // XXX Check for only empty inputs so far not yet in place.
          retval = trial.getCurrentInteraction().attemptUserSwitch(lang);
      }
      return retval;
   }

   private static final Logger logger = LoggerFactory.getLogger(ASRController.class);
   private ExecutorService executor;
   private ASRTask asrTask;
}
