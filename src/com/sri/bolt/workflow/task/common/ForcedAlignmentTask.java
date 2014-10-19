package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.Util;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.RecognizerToBoltMessage;
import com.sri.bolt.service.ServiceController;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.Language;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;

public class ForcedAlignmentTask implements WorkflowTask {
   public ForcedAlignmentTask(Language lang, boolean isMultiplex) {
      this.language = lang;
      this.isMultiplex = isMultiplex;
   }

   @Override
   public WorkflowState call() throws Exception {
      ServiceController controller = App.getApp().getServiceController();
      SessionData sessionData = data.getInteractionState().getSessionData();
      int currentTrial = sessionData.getCurrentTurn();

      UtteranceData currentUtterance = sessionData.getUtterances(currentTrial);

      byte[] samples = data.getInteractionState().getLastASR().getAudioData().toByteArray();
      CombinedRecognizerResult result;
      try {
         controller.setFAGrammar(currentUtterance.getRescored1Best(), language);
         controller.startFASamples(language);
         controller.sendFASamples(samples, language);
         result = controller.endFASamples(language);
      } catch (Exception e) {
         e.printStackTrace();

         // Will make up fields in this case
         result = CombinedRecognizerResult.newBuilder().build();
      }
      data.setFaResult(result);
      setForcedAlignmentResults(result, data);

      return data;
   }

   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public boolean validate() {
      return true;
   }

   @Override
   public WorkflowTaskType getType() {
      return language == Language.ENGLISH ? WorkflowTaskType.FORCED_ALIGNMENT_EN : WorkflowTaskType.FORCED_ALIGNMENT_IA;
   }

   public static void setForcedAlignmentResults(CombinedRecognizerResult result, WorkflowState data) {
      SessionData sessionData = data.getInteractionState().getSessionData();
      int currentTrial = sessionData.getCurrentTurn();
      UtteranceData currentUtterance = sessionData.getUtterances(currentTrial);
      byte[] samples = data.getInteractionState().getLastASR().getAudioData().toByteArray();
      double naudioSeconds = ((double) samples.length) / (Util.SAMPLE_RATE * Util.BYTES_PER_SAMPLE);
      UtteranceData.Builder builder = currentUtterance.toBuilder();
      // The last field is used to see if the desired forced alignment result matches the returned result
      RecognizerToBoltMessage.setCombinedRecognizerResultFieldsForcedAlignmentPass(builder, result, currentUtterance.getRescored1Best().trim(), naudioSeconds);
      SessionData.Builder sessionBuilder = sessionData.toBuilder().setUtterances(currentTrial, builder);
      data.getInteractionState().setSessionData(sessionBuilder.build());
   }

   private boolean isMultiplex;
   private Language language;
   private WorkflowState data;
}
