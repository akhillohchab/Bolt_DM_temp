package com.sri.bolt.audio;

import com.sri.bolt.message.BoltMessages.*;
import com.sri.bolt.service.RecognizerFactory.RecognizerType;
import com.sri.bolt.state.ASRState;
import com.sri.interfaces.lang.Language;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

public abstract class ASRInputTask implements Callable<TaskReturn> {

   public void setInput(SessionData data, Language lang) {
      this.language = lang;
      sessionData = data;
      secondRecognizerType = null;
      // Default to 0 unless specified.
      lmIndex = 0;

      // Default recognizers based on language
      switch (lang) {
      case IRAQI_ARABIC:
         recognizerType = RecognizerType.SOCKET_IA;
         secondRecognizerType = RecognizerType.SOCKET_IA_SEC;
         break;
      default:
         recognizerType = RecognizerType.SOCKET_EN;
         secondRecognizerType = RecognizerType.SOCKET_EN_SEC;
      }

      // for the first turn, the data is null because this function is called before we add the utterance to session data.
      if (data == null) {
         return;
      }
      int curTurn = data.getCurrentTurn();
      // the current utterance is not yet added to session data.
      // Therefore, when we call this function during the second turn, current turn is actually still 0.
      //if (curTurn > 0) {
      if (curTurn >= 0) {
         // See if DM asked for spelling in last turn
         //UtteranceData lastData = data.getUtterances(curTurn - 1);
         UtteranceData lastData = data.getUtterances(curTurn);
         DmOutput dmOutput = lastData.getDmOutput();

         // Check dm_output::response enum
         if (dmOutput.hasDmResponse()) {
            switch (dmOutput.getDmResponse()) {
            case RESPONSE_SENTENCE:
                // "main"
                lmIndex = 0;
                break;
            case RESPONSE_PARTIAL:
                // "clarify"
                lmIndex = 1;
                break;
            case RESPONSE_CHOICE:
                // "options"
                lmIndex = 2;
                break;
            case RESPONSE_SPELL:
                // "spell"
                lmIndex = 3;
                break;
            }
         }
      }
   }

   void setASRState(ASRState state) {
      this.state = state;
   }

   public abstract ByteArrayOutputStream getFullData();

   public abstract void stop();

   protected Language language;
   protected ASRState state;
   private SessionData sessionData;
   protected RecognizerType recognizerType;
   protected RecognizerType secondRecognizerType;
   protected int lmIndex;
}
