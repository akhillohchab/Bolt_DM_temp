package com.sri.bolt.state;


import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.xml.AudioT;
import com.sri.bolt.xml.Clarification;
import com.sri.bolt.xml.HumanTurn;
import com.sri.bolt.xml.LangT;
import com.sri.bolt.xml.SystemTurn;
import com.sri.bolt.xml.TextT;
import com.sri.interfaces.lang.Language;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

public class TurnState {
   public TurnState(String trialId, int turnNum, Language lang, Date trialStartTime) {
      this.trialId = trialId;
      this.turnNum = turnNum;
      this.startTime = new Date();
      this.language = lang;
      this.trialStartTime = trialStartTime;
   }

   public TurnState(TurnState state) {
      startTime = new Date(state.startTime.getTime());
      trialStartTime = new Date(state.trialStartTime.getTime());
      trialId = state.trialId;
      turnNum = state.turnNum;

      if (state.humanResponse != null) {
         humanResponse = new ASRState(state.humanResponse);
      }
      asrFile = new File(state.asrFile.toString());

      systemResponseText = state.systemResponseText;
      ttsAudioFile = new File(state.ttsAudioFile.toString());
      if (systemSessionData != null) {
         systemSessionData = state.systemSessionData.toBuilder().clone().build();
      }

      sessionData = state.sessionData.toBuilder().clone().build();

      if (state.uwSessionData != null) {
         uwSessionData = state.uwSessionData.toBuilder().clone().build();
      }

      language = state.language;
   }

   public void setASR(ASRState asr, long time) {
      this.humanResponse = asr;
      String fileName = Util.getFileName(trialId, "CL" + (turnNum + 2), trialStartTime);
      asrFile = com.sri.bolt.audio.AudioSaver.writeAudioFile(fileName, humanResponse.getAudioData(), false);
      com.sri.bolt.audio.AudioSaver.writeAudioFile(App.getApp().getRunDir().getPath() + "/" + com.sri.bolt.Util.getFilenameTimestamp(time) + "-UTTERANCE", humanResponse.getAudioData(), false);
   }

   public ASRState getHumanResponse() {
      return humanResponse;
   }

   public File getHumanResponseFile() {
      return asrFile;
   }

   public void setSessionData(SessionData data) {
      sessionData = data;
   }

   public SessionData getUWSessionData() {
      return uwSessionData;
   }

   public void setUWSessionData(SessionData uwSessionData) {
      this.uwSessionData = uwSessionData;
   }

   public SessionData getASRSessionData() {
      return asrSessionData;
   }

   public void setASRSessionData(SessionData asrSessionData) {
      this.asrSessionData = uwSessionData;
   }

   public SessionData getSessionData() {
      return sessionData;
   }

   public void setSystemReponseText(String text) {
      systemResponseText = text;
   }

   public void onSystemResponseFinished() {
      systemSessionData = sessionData;
   }

   public void rollbackTurn() {
      this.sessionData = systemSessionData;
      humanResponse = null;
      asrFile = null;
   }

   public void setTTSAudio(ByteArrayOutputStream audioData, long time) {
      String fileName = Util.getFileName(trialId, "CL" + (turnNum + 1), trialStartTime);
      ttsAudioFile = com.sri.bolt.audio.AudioSaver.writeAudioFile(fileName, audioData, false);
      com.sri.bolt.audio.AudioSaver.writeAudioFile(App.getApp().getRunDir().getPath() + "/" + com.sri.bolt.Util.getFilenameTimestamp(time) + "-CLARIFICATION", audioData, false);
   }

   public File getSystemCommand() {
      return ttsAudioFile;
   }

   public Date getStartTime() {
      return startTime;
   }

   public Clarification getClarification(Date startTime) {
      Clarification clarification = new Clarification();

      SystemTurn sysTurn = new SystemTurn();
      sysTurn.setStartTime(Util.getTimeForDisplay(startTime, this.startTime));

      AudioT sysAudio = new AudioT();
      sysAudio.setFilename(ttsAudioFile.getName());
      sysTurn.setAudio(sysAudio);

      TextT text = new TextT();
      text.setLang(language == Language.ENGLISH ? LangT.EN : LangT.ACM);
      text.getContent().add(Util.getStringForDisplay(systemResponseText, language));
      sysTurn.getText().add(text);

      clarification.setSystemTurn(sysTurn);
      if (humanResponse != null) {
         HumanTurn humanTurn = humanResponse.getHumanTurn(startTime);
         AudioT file = new AudioT();
         file.setFilename(asrFile.getName());
         humanTurn.setAudio(file);
         clarification.setHumanTurn(humanTurn);
      }

      return clarification;
   }

   private Date startTime;
   private Date trialStartTime;

   private String trialId;
   private int turnNum;

   private ASRState humanResponse;
   private File asrFile;

   private String systemResponseText;
   private File ttsAudioFile;
   private SessionData systemSessionData;

   private SessionData sessionData;

   private SessionData uwSessionData;
   private SessionData asrSessionData;

   private Language language;
}
