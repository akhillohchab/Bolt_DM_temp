package com.sri.bolt.state;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.xml.AudioT;
import com.sri.bolt.xml.Clarification;
import com.sri.bolt.xml.Dsegment;
import com.sri.bolt.xml.HumanTurn;
import com.sri.interfaces.lang.Language;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InteractionState {
   public InteractionState(String trialId, Language lang, int interactionNum, int curNumAudioFiles, Date trialStartTime) {
      clarificationTurns = new ArrayList<TurnState>();
      this.trialId = trialId;
      this.language = lang;
      this.interactionNum = interactionNum;
      this.curNumAudioFiles = curNumAudioFiles;
      this.trialStartTime = trialStartTime;
   }

   public InteractionState(InteractionState state) {
      initialUtterance = new ASRState(state.initialUtterance);
      asrFile = new File(state.asrFile.toString());
      if (state.uwSessionData != null) {
         uwSessionData = state.uwSessionData.toBuilder().clone().build();
      }

      clarificationTurns = new ArrayList<TurnState>();
      for (TurnState turn : state.clarificationTurns) {
         clarificationTurns.add(new TurnState(turn));
      }

      if (state.translation != null) {
         translation = new TranslationState(state.translation);
      }
      currentData = state.currentData.toBuilder().clone().build();

      trialId = state.trialId;

      language = state.language;

      interactionNum = state.interactionNum;
      curNumAudioFiles = state.curNumAudioFiles;

      trialStartTime = new Date(state.trialStartTime.getTime());

      isInteractionFinished = state.isInteractionFinished;

      latestWorkflowStartTime = state.latestWorkflowStartTime;
   }

   public SessionData getSessionData() {
      return currentData;
   }

   public void setSessionData(SessionData data) {
      currentData = data;
      if (clarificationTurns.size() != 0) {
         TurnState turnState = clarificationTurns.get(clarificationTurns.size() - 1);
         turnState.setSessionData(data);
      }
   }

   public void setUWSessionData(SessionData data) {
      if (clarificationTurns.size() == 0) {
         uwSessionData = data;
      } else {
         TurnState turnState = clarificationTurns.get(clarificationTurns.size() - 1);
         turnState.setUWSessionData(data);
      }
   }

   public List<SessionData> getUWSessionDatas() {
      List<SessionData> sessionDataList = new ArrayList<SessionData>();
      sessionDataList.add(uwSessionData);
      for (TurnState turn : clarificationTurns) {
         sessionDataList.add(turn.getUWSessionData());
      }

      return sessionDataList;
   }

   public void setASRSessionData(SessionData data) {
      if (clarificationTurns.size() == 0) {
         asrSessionData = data;
      } else {
         TurnState turnState = clarificationTurns.get(clarificationTurns.size() - 1);
         turnState.setASRSessionData(data);
      }
   }

   public List<SessionData> getASRSessionDatas() {
      List<SessionData> sessionDataList = new ArrayList<SessionData>();
      sessionDataList.add(asrSessionData);
      for (TurnState turn : clarificationTurns) {
         sessionDataList.add(turn.getASRSessionData());
      }

      return sessionDataList;
   }

   public void addASR(ASRState state, long time) {
      latestWorkflowStartTime = time;
      if (initialUtterance == null) {
         initialUtterance = state;
         saveAudio(time);
      } else {
         TurnState turnState = clarificationTurns.get(clarificationTurns.size() - 1);
         turnState.setASR(state, time);
      }
   }

   public ASRState getLastASR() {
      if (clarificationTurns.size() != 0) {
         return clarificationTurns.get(clarificationTurns.size() - 1).getHumanResponse();
      } else {
         return initialUtterance;
      }
   }

   public void startClarificationTurn() {
      TurnState state = new TurnState(trialId, curNumAudioFiles + (clarificationTurns.size() * 2) + 100, language, trialStartTime);
      clarificationTurns.add(state);
   }

   public void setTTSAudio(ByteArrayOutputStream data) {
      clarificationTurns.get(clarificationTurns.size() - 1).setTTSAudio(data, latestWorkflowStartTime);
   }

   public void setSystemResponseText(String text) {
      clarificationTurns.get(clarificationTurns.size() - 1).setSystemReponseText(text);
   }

   public File getLastSystemCommand() {
      File returnFile = null;
      if (clarificationTurns.size() != 0) {
         returnFile = clarificationTurns.get(clarificationTurns.size() - 1).getSystemCommand();
      }

      return returnFile;
   }

   public File getLastHumanUtterance() {
      int numTurns = clarificationTurns.size();

      if (numTurns == 1) {
         if (clarificationTurns.get(0).getHumanResponseFile() != null) {
            return clarificationTurns.get(0).getHumanResponseFile();
         } else {
            return asrFile;
         }
      } else if (numTurns > 1) {
         if (clarificationTurns.get(numTurns - 1).getHumanResponseFile() != null) {
            return clarificationTurns.get(numTurns - 1).getHumanResponseFile();
         } else {
            return clarificationTurns.get(numTurns - 2).getHumanResponseFile();
         }
      } else if (asrFile != null) {
         return asrFile;
      } else {
         return null;
      }
   }

   public ByteArrayOutputStream getUtteranceAudio(int uttIndex) {
      if (uttIndex == 0) {
         return initialUtterance.getAudioData();
      } else {
         return clarificationTurns.get(uttIndex - 1).getHumanResponse().getAudioData();
      }
   }

   public void onSystemResponseFinished() {
      if (clarificationTurns.size() != 0) {
         clarificationTurns.get(clarificationTurns.size() - 1).onSystemResponseFinished();
      }
   }

   public void startTranslation() {
      translation = new TranslationState(trialId, interactionNum, language, trialStartTime);
   }

   public void setTranslation(TranslationState state) {
      translation = state;
   }

   public TranslationState getTranslationState() {
      return translation;
   }

   public void removeTranslation() {
      translation = null;
   }

   public void rollbackTurn() {
      if (clarificationTurns.size() != 0) {
         if (translation != null) {
            translation = null;
         }
         TurnState lastTurn = clarificationTurns.get(clarificationTurns.size() - 1);
         if (lastTurn.getHumanResponse() != null) {
            lastTurn.rollbackTurn();
         } else {
            clarificationTurns.remove(clarificationTurns.size() - 1);
            if (clarificationTurns.size() != 0) {
               clarificationTurns.get(clarificationTurns.size() - 1).rollbackTurn();
            }
         }
      }

      if (clarificationTurns.size() == 0) {
         currentData = null;
         initialUtterance = null;
         asrFile = null;
      }
   }

   private void saveAudio(long time) {
      String fileName = Util.getFileName(trialId, "UT" + (interactionNum + 1), trialStartTime);
      asrFile = com.sri.bolt.audio.AudioSaver.writeAudioFile(fileName, initialUtterance.getAudioData(), false);
      com.sri.bolt.audio.AudioSaver.writeAudioFile(App.getApp().getRunDir().getPath() + "/" + com.sri.bolt.Util.getFilenameTimestamp(latestWorkflowStartTime) + "-UTTERANCE", initialUtterance.getAudioData(), false);
   }

   public Language getLanguage() {
      return language;
   }

   public void setLanguage(Language language) {
      this.language = language;
   }

   public Dsegment getDsegment(Date startTime) {
      Dsegment segment = new Dsegment();

      HumanTurn humanTurn = initialUtterance.getHumanTurn(startTime);
      AudioT file = new AudioT();
      file.setFilename(asrFile.getName());
      humanTurn.setAudio(file);
      segment.setHumanTurn(humanTurn);

      for (TurnState turn : clarificationTurns) {
         segment.getClarification().add(turn.getClarification(startTime));
      }

      if (translation != null) {
         segment.setSystemTurn(translation.getSystemTurn(startTime));
      }

      return segment;
   }

   public int getNumClarificationAudioFiles() {
      int numAudioFiles = 0;
      numAudioFiles += clarificationTurns.size() * 2;

      return numAudioFiles;
   }

   public boolean isInteractionFinished() {
      return isInteractionFinished;
   }

   public void setInteractionFinished(boolean interactionFinished) {
      isInteractionFinished = interactionFinished;
   }

   /**
    * This is called if non-active user tries to speak to see if active
    * user has only given empty inputs so far.
    * XXX Check for only empty inputs so far not yet in place.
    * @param lang Target language.
    * @return true if specified lang is or can be the active language.
    */
   public boolean attemptUserSwitch(Language lang) {
      boolean retval = false;
      if (isInteractionFinished() || (lang == language)) {
         retval = true;
      } else {
         // Not finished and lang doesn't match.
         // XXX Will return false unless we can detect if only
         // empty inputs given so far...
      }
      return retval;
   }

   private ASRState initialUtterance;
   private File asrFile;
   private SessionData uwSessionData;
   private SessionData asrSessionData;

   private List<TurnState> clarificationTurns;

   private TranslationState translation;

   private SessionData currentData;

   private String trialId;

   private Language language;

   private int interactionNum;
   private int curNumAudioFiles;

   private Date trialStartTime;

   private boolean isInteractionFinished;
   private long latestWorkflowStartTime;
}
