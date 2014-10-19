package com.sri.bolt.workflow;

import com.sri.bolt.App;
import com.sri.bolt.EvalType;
import com.sri.bolt.TimeKeeper;
import com.sri.bolt.audio.AudioSequence;
import com.sri.bolt.audio.Playback;
import com.sri.bolt.audio.TTSStrings;
import com.sri.bolt.listener.WorkflowListenerInterface;
import com.sri.bolt.message.BoltMessages.DmActionType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.state.ASRState;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.state.TranslationState;
import com.sri.bolt.state.TrialState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ShutdownStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.sri.bolt.workflow.Util.buildWorkflowState;

public class WorkflowController implements Runnable {
   public WorkflowController() {
      enWorkflowQueue = new LinkedBlockingQueue<WorkflowState>();
      iaWorkflowQueue = new LinkedBlockingQueue<WorkflowState>();
      processingLock = new Object();
      asrQueue = new LinkedBlockingDeque<ASRState>();
      workflowExecutor = Executors.newSingleThreadExecutor();
      workflowExecutor.submit(this);
   }

   public void addASRState(ASRState state) {
      try {
         asrQueue.put(state);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void run() {
      try {
         while (true) {
            ASRState state = asrQueue.take();
            processASRState(state, false);
         }
      } catch (Exception e) {
         logger.error("Error in processing asr: " + e.toString(), e);
      }
   }

   public void processASRState(ASRState asrState, boolean waitForCompletion) {
      App.getApp().getIsProcessing().set(true);
      try {
         workflowReadableList.clear();
         timeWorkflowStarted = System.currentTimeMillis();
         notifyStartTrial();

         InteractionState interaction = App.getApp().getTrial().getCurrentInteraction();
         WorkflowState state = buildWorkflowState(interaction, asrState, timeWorkflowStarted);
         notifyWorkflowTaskComplete(interaction.getSessionData(), true, "DynaSpeak ASR");
         if (waitForCompletion) {
            synchronized (processingLock) {
               getWorkflowQueue(asrState.getLanguage()).put(state);
               processingLock.wait();
            }
         } else {
            getWorkflowQueue(asrState.getLanguage()).put(state);
         }
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   public void onWorkflowFailed(WorkflowState state) {
      notifyListenersError("Unrecoverable error found in run: " + state.getError().exception.toString());
      logger.error("Unrecoverable error found in run", state.getError().exception);
      com.sri.bolt.ui.Util.addUserMessage("We have encountered an error. Please close the program and retry", Language.ENGLISH);
      com.sri.bolt.ui.Util.addUserMessage("We have encountered an error. Please close the program and retry", Language.IRAQI_ARABIC);
      Language lang = state.getInteractionState().getLanguage();
      Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.UNRECOVERABLE_ERROR, lang), lang);
      state.getError().exception.printStackTrace();
      state.getInteractionState().setInteractionFinished(true);
      App.getApp().getIsProcessing().set(false);
      TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
      timeKeeper.runFailed();
      synchronized (processingLock) {
         processingLock.notifyAll();
      }
   }

   public void onFinishedWorkflow(WorkflowState state) {
      SessionData sessionData = state.getInteractionState().getSessionData();
      UtteranceData currentUtterance = sessionData.getUtterances(sessionData.getCurrentTurn());
      App.getApp().getTrial().writeTrialSummaries();
      if (App.getApp().getEvalType() == EvalType.NO_CLARIFICATION || currentUtterance.getDmOutput().getDmAction().equals(DmActionType.ACTION_TRANSLATE_UTTERANCE) ||
              currentUtterance.getDmOutput().getDmAction().equals(DmActionType.ACTION_RESTART) || currentUtterance.getDmOutput().getDmAction().equals(DmActionType.ACTION_MOVE_ON)) {
         state.getInteractionState().setInteractionFinished(true);
         if (currentUtterance.getDmOutput().getDmAction().equals(DmActionType.ACTION_RESTART)) {
            InteractionState inter = state.getInteractionState();
            inter.startClarificationTurn();
            String ttsFile = Playback.doTTS("Starting over", Language.ENGLISH);
            AudioSequence ttsData = new AudioSequence();
            ttsData.addFileData(new File(ttsFile));
            inter.setTTSAudio(ttsData.getBytes());
            Playback.playAudioFile(ttsFile, state.getInteractionState().getLanguage());
            inter.setSystemResponseText("Starting over");
            logger.debug("DM Response: Starting over");
            com.sri.bolt.ui.Util.addSystemMessage("Starting over", state.getInteractionState().getLanguage());
         } else {
            // Behavior depends on if each person has own output device. If two
            // output devices, can play both at once. If one output device, need
            // to play sequentially such that the first call is blocking.
            // Note that TTS generation takes time so *could* optimize by
            // generating in the background if first call is blocking.
            boolean waitForFirstFinished = Playback.needsSequentialPlaybackForDifferentLanguages();
            Language language = state.getInteractionState().getLanguage();
            Language oppositeLanguage = language == Language.ENGLISH ? Language.IRAQI_ARABIC : Language.ENGLISH;
            //App.getLog4jLogger().info("Playing translation, waitForFirstFinished=" + waitForFirstFinished);
            String originalInputDisplay = currentUtterance.getMtData().getOriginalInput().trim();
            // Strip hesitation words (and @reject@ if present)
            originalInputDisplay = com.sri.bolt.service.TextProcessing.cleanForDisplay(originalInputDisplay, language);
            if (language == Language.IRAQI_ARABIC) {
               // Convert from Buckwalter to Unicode
               originalInputDisplay = BuckwalterUnicodeConverter.buckwalterToUnicode(originalInputDisplay, language);
            }
            TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
            timeKeeper.startTiming("Translation TTS " + (state.getInteractionState().getLanguage() == Language.IRAQI_ARABIC ? "IA" : "EN"));
            TranslationState translationState = state.getInteractionState().getTranslationState();
            if (translationState == null) {
                logger.error("translationState null; possibly from running from SessionData input at late stage where translation not explicitly stored");
            }
            String ttsFile = Playback.doTTS((translationState != null)?translationState.getTranslation():"", language == Language.ENGLISH ? Language.IRAQI_ARABIC : Language.ENGLISH);
            AudioSequence ttsData = new AudioSequence();
            ttsData.addFileData(new File(ttsFile));
            if (translationState != null) {
               translationState.setTTSAudio(ttsData.getBytes(), timeWorkflowStarted);
            }
            com.sri.bolt.ui.Util.addSystemMessage(Util.formatForScreen("Translating:", originalInputDisplay), language);

            // Get Unicode variant of the Buckwalter result
            final String postprocessedTranslationDisplay = language == Language.ENGLISH ? BuckwalterUnicodeConverter.buckwalterToUnicode(
                    currentUtterance.getMtData().getPostprocessedTranslations(0), Language.IRAQI_ARABIC) :
                    currentUtterance.getMtData().getPostprocessedTranslations(0);
            com.sri.bolt.ui.Util.addTranslationMessage(Util.formatForScreen("Translation:", postprocessedTranslationDisplay), oppositeLanguage);

            final String originalInputTTS = com.sri.bolt.service.TextProcessing.cleanForTTS(currentUtterance.getMtData().getOriginalInput(), language);
            Playback.playTTSText(TTSStrings.getTTSString(TTSStrings.TRANSLATING, language) + " " + originalInputTTS, language);
            // By default, this 2nd play would wait for first play to finish *but* in case where using
            // two headphones and no monitor, can allow them to overlap.
            Playback.playAudioFile(ttsFile, oppositeLanguage, waitForFirstFinished);
            timeKeeper.stopTiming("Translation TTS " + (state.getInteractionState().getLanguage() == Language.IRAQI_ARABIC ? "IA" : "EN"));
         }
      }
      state.getInteractionState().onSystemResponseFinished();
      TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
      timeKeeper.stopTiming("Total workflow " + (state.getInteractionState().getLanguage() == Language.IRAQI_ARABIC ? "IA" : "EN"));
      App.getApp().getIsProcessing().set(false);
      synchronized (processingLock) {
         processingLock.notifyAll();
      }
   }

   /* Unused
   public void abort() {
      // Just let it run in background
      //Playback.playAudioResource(App.AUDIO_RESOURCE_ABORT);
      aborted = true;
      CamelContext context = App.getApp().getCamelContext();
      try {
         ShutdownStrategy strategy = context.getShutdownStrategy();
         strategy.setTimeout(2);
         strategy.setTimeUnit(TimeUnit.SECONDS);
         context.stop();

         com.sri.bolt.ui.Util.addSystemMessage("Trial Aborted", Language.ENGLISH);
         logger.info("trial aborted");
        // Playback.playTTSText("Trial aborted");
      } catch (InterruptedException e) {
         notifyListenersError("Interrupted exception caused by abort: " + e.toString());
      } catch (Exception e) {
         notifyListenersError("Exception found in abort: " + e.toString());
         e.printStackTrace();
      }

      try {
         context.start();
      } catch (Exception e) {
         logger.error("Exception in abort", e);
         notifyListenersError("Unable to start camel route due to: " + e.toString());
         com.sri.bolt.ui.Util.addUserMessage("We have encountered an error. Please close the program and retry", Language.ENGLISH);
         com.sri.bolt.ui.Util.addUserMessage("We have encountered an error. Please close the program and retry", Language.IRAQI_ARABIC);
         e.printStackTrace();
      }

      aborted = false;
      App.getApp().getIsProcessing().set(false);
      synchronized (processingLock) {
         processingLock.notifyAll();
      }
   }
   */

   public void cleanup() {
      aborted = true;
   }

   public void rollbackLastTurn() {
      App.getApp().getTrial().rollback();
      //Playback.playTTSText("Turn rolled back");
      com.sri.bolt.ui.Util.addSystemMessage("Turn rolled back", Language.ENGLISH);
      com.sri.bolt.ui.Util.addSystemMessage("Turn rolled back", Language.IRAQI_ARABIC);
   }

   public void startCustomWorkflow(WorkflowTaskType workflowStart, File sessionDataFile, File audioData, Language lang, boolean waitForCompletion) {
      try {
         TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
         timeKeeper.startRun();
         workflowReadableList.clear();
         timeWorkflowStarted = System.currentTimeMillis();
         notifyStartTrial();

         TrialState trial = App.getApp().getTrial();
         if (trial.getCurrentInteraction() == null || trial.getCurrentInteraction().getLanguage() != lang) {
            trial.startNewInteraction(lang);
         }

         InteractionState interaction = trial.getCurrentInteraction();

         WorkflowState state = buildWorkflowState(interaction, workflowStart, sessionDataFile, audioData, timeWorkflowStarted);
         getWorkflowQueue(lang).put(state);
         if (waitForCompletion) {
            try {
               synchronized (processingLock) {
                  processingLock.wait();
               }
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }
         }
      } catch (Exception e) {
         logger.error("Exception found in startCustomWorkflow: " + e.toString(), e);
         notifyListenersError("Exception found in startCustomWorkflow: " + e.toString());
      }
   }

   public boolean isInteractionFinished() {
      InteractionState state = App.getApp().getTrial().getCurrentInteraction();
      if (state == null || state.getTranslationState() == null) {
         return false;
      } else {
         return true;
      }
   }

   public boolean addListener(WorkflowListenerInterface listener) {
      return listeners.add(listener);
   }

   public boolean removeListener(WorkflowListenerInterface listener) {
      return listeners.remove(listener);
   }

   private void notifyStartTrial() {
      for (WorkflowListenerInterface listener : listeners) {
         listener.onStartWorkflow(App.getApp().getTrial().getTrialId(), timeWorkflowStarted);
      }
   }

   public void notifyWorkflowTaskComplete(SessionData sessionData, boolean successful, String name) {
      for (WorkflowListenerInterface listener : listeners) {
         listener.workflowTaskComplete(sessionData, successful, name);
      }
   }

   public BlockingQueue<WorkflowState> getWorkflowQueue(Language lang) {
      if (lang == Language.ENGLISH) {
         return enWorkflowQueue;
      } else if (lang == Language.IRAQI_ARABIC) {
         return iaWorkflowQueue;
      } else {
         return null;
      }
   }

   public void notifyListenersError(String exception) {
      logger.error(exception);
      for (WorkflowListenerInterface listener : listeners) {
         listener.onExceptionFound(exception);
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);
   public BlockingQueue<ASRState> asrQueue;

   private ArrayList<String> workflowReadableList = new ArrayList<String>();
   private volatile boolean aborted = false;

   private List<WorkflowListenerInterface> listeners = new ArrayList<WorkflowListenerInterface>();
   private long timeWorkflowStarted = 0;

   private ExecutorService workflowExecutor;

   private BlockingQueue<WorkflowState> enWorkflowQueue;
   private BlockingQueue<WorkflowState> iaWorkflowQueue;

   private final Object processingLock;
}
