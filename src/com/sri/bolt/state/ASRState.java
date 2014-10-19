package com.sri.bolt.state;

import com.sri.bolt.service.RecognizerFactory;
import com.sri.bolt.xml.HumanTurn;
import com.sri.bolt.xml.LangT;
import com.sri.bolt.xml.TextT;
import com.sri.interfaces.lang.Language;
import com.sri.recognizer.message.RecognizerMessages;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ASRState {
   public ASRState() {

   }

   public ASRState(ASRState state) {
      language = state.language;
      buttonPressed = new Date(state.buttonPressed.getTime());
      buttonReleased = new Date(state.buttonReleased.getTime());
      asrComplete = new Date(state.asrComplete.getTime());
      if (recogResult != null) {
         recogResult = state.getRecogResult().toBuilder().clone().build();
      }

      audioData = new ByteArrayOutputStream();
      byte[] audio = state.audioData.toByteArray();
      audioData.write(audio, 0, audio.length);

      type = state.type;
      asrResultString = state.asrResultString;
   }

   public void buttonPressed() {
      buttonPressed = new Date();
   }

   public void buttonReleased() {
      buttonReleased = new Date();
   }

   public Date getAsrComplete() {
      return asrComplete;
   }

   public void setAsrComplete(Date asrComplete) {
      this.asrComplete = asrComplete;
   }

   public RecognizerMessages.CombinedRecognizerResult getRecogResult() {
      return recogResult;
   }

   public void setRecogResult(RecognizerMessages.CombinedRecognizerResult recogResult) {
      this.recogResult = recogResult;
      setAsrResultString(recogResult.getText());
   }

   public CombinedRecognizerResult getSecondRecogResult() {
      return secondRecogResult;
   }

   public void setSecondRecogResult(CombinedRecognizerResult secondRecogResult) {
      this.secondRecogResult = secondRecogResult;
   }

   public ByteArrayOutputStream getAudioData() {
      return audioData;
   }

   public void setAudioData(ByteArrayOutputStream audioData) {
      this.audioData = audioData;
   }

   public RecognizerFactory.RecognizerType getType() {
      return type;
   }

   public void setType(RecognizerFactory.RecognizerType type) {
      this.type = type;
   }

   public Language getLanguage() {
      return language;
   }

   public void setLanguage(Language lang) {
      this.language = lang;
   }

   public HumanTurn getHumanTurn(Date startTime) {
      HumanTurn turn = new HumanTurn();
      turn.setAsrComplete(Util.getTimeForDisplay(startTime, asrComplete));
      turn.setButtonPressed(Util.getTimeForDisplay(startTime, buttonPressed));
      turn.setButtonReleased(Util.getTimeForDisplay(startTime, buttonReleased));

      TextT text = new TextT();
      text.setLang(language == Language.ENGLISH ? LangT.EN : LangT.ACM);
      text.getContent().add(Util.getStringForDisplay(asrResultString, language));

      turn.getText().add(text);

      return turn;
   }

   public String getAsrResultString() {
      return asrResultString;
   }

   public void setAsrResultString(String asrResultString) {
      this.asrResultString = asrResultString;
   }

   private Language language;
   private Date buttonPressed;
   private Date buttonReleased;
   private Date asrComplete;
   private RecognizerMessages.CombinedRecognizerResult recogResult;
   private RecognizerMessages.CombinedRecognizerResult secondRecogResult;
   private ByteArrayOutputStream audioData;
   private RecognizerFactory.RecognizerType type;
   private String asrResultString;
}
