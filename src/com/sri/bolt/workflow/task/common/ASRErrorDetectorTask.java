package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.UtteranceDataOrBuilder;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.Language;

public class ASRErrorDetectorTask implements WorkflowTask {
   public ASRErrorDetectorTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData resultData = App.getApp().getServiceController().processASRErrors(data.getInteractionState().getSessionData(), language);
      if (resultData == null || resultData.getSerializedSize() == 0) {
         return null;
      } else {
         UtteranceData.Builder utt = resultData.getUtterances(resultData.getCurrentTurn()).toBuilder();
         if (utt.getErrorSegmentsCount() == 0) {
            utt.setRescored1Best(utt.getRecognizer1Best());
            resultData = resultData.toBuilder().setUtterances(resultData.getCurrentTurn(), utt).build();
         }

         data.getInteractionState().setSessionData(resultData);
         data.getInteractionState().setASRSessionData(resultData);
         return data;
      }
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
      return language == Language.ENGLISH ? WorkflowTaskType.ASR_ERROR_DETECTOR_EN : WorkflowTaskType.ASR_ERROR_DETECTOR_IA;
   }

   private Language language;
   private WorkflowState data;
}
