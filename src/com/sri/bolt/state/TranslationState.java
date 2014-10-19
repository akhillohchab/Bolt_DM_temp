package com.sri.bolt.state;


import com.sri.bolt.App;
import com.sri.bolt.xml.AudioT;
import com.sri.bolt.xml.LangT;
import com.sri.bolt.xml.SystemTurn;
import com.sri.bolt.xml.SystemTurnT;
import com.sri.bolt.xml.TextT;
import com.sri.interfaces.lang.Language;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

public class TranslationState {
   public TranslationState(String trialId, int turnNum, Language lang, Date trialStartTime) {
      this.trialId = trialId;
      this.turnNum = turnNum;
      this.language = lang;
      this.trialStartTime = trialStartTime;
   }

   public TranslationState(TranslationState state) {
      turnNum = state.turnNum;
      trialId = state.trialId;
      startTime = new Date(state.startTime.getTime());
      endTime = new Date(state.endTime.getTime());
      trialStartTime = new Date(state.trialStartTime.getTime());
      translation = state.translation;
      original = state.original;
      if (ttsAudioFile != null) {
         ttsAudioFile = new File(state.ttsAudioFile.toString());
      }
      language = state.language;
   }

   public void startTranslation() {
      startTime = new Date();
   }

   public void endTranslation() {
      endTime = new Date();
   }

   public void setTranslation(String original, String translation) {
      this.translation = translation;
      this.original = original;
   }

   public void setTTSAudio(ByteArrayOutputStream audioData, long time) {
      String fileName = Util.getFileName(trialId, "TR" + (turnNum + 1), trialStartTime);
      ttsAudioFile = com.sri.bolt.audio.AudioSaver.writeAudioFile(fileName, audioData, true);
      com.sri.bolt.audio.AudioSaver.writeAudioFile(App.getApp().getRunDir().getPath() + "/" + com.sri.bolt.Util.getFilenameTimestamp(time) + "-TRANSLATION", audioData, true);
   }

   public String getOriginal() {
      return original;
   }

   public String getTranslation() {
      return translation;
   }

   public SystemTurn getSystemTurn(Date startTime) {
      SystemTurn systemTurn = new SystemTurn();
      if (ttsAudioFile != null) {
         AudioT file = new AudioT();
         file.setFilename(ttsAudioFile.getName());
         systemTurn.setAudio(file);
      }

      systemTurn.setStartTime(Util.getTimeForDisplay(startTime, this.startTime));

      TextT originalText = new TextT();
      originalText.setLang(language == Language.ENGLISH ? LangT.EN : LangT.ACM);
      originalText.setType(SystemTurnT.TARGET);
      originalText.getContent().add(Util.getStringForDisplay(original, language));
      systemTurn.getText().add(originalText);

      TextT translationText = new TextT();
      translationText.setLang(language == Language.ENGLISH ? LangT.ACM : LangT.EN);
      translationText.setType(SystemTurnT.TRANSLATION);
      translationText.getContent().add(Util.getStringForDisplay(translation, language == Language.ENGLISH ? Language.IRAQI_ARABIC : Language.ENGLISH));
      systemTurn.getText().add(translationText);

      return systemTurn;
   }

   public File getTtsAudioFile() {
      return ttsAudioFile;
   }

   public Language getLanguage() {
      return language;
   }

   private int turnNum;
   private String trialId;
   private Date startTime;
   private Date endTime;
   private Date trialStartTime;
   private String translation = "";
   private String original = "";
   private File ttsAudioFile;
   private Language language;
}
