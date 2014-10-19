package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.Language;

public class AnswerMergerTask implements WorkflowTask {
   public AnswerMergerTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {

         SessionData resultData = App.getApp().getServiceController().amuProcessSentence(data.getInteractionState().getSessionData(), language);
         if (resultData == null || resultData.getSerializedSize() == 0) {
            return null;
         } else {
            data.getInteractionState().setSessionData(resultData);
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
      return language == Language.ENGLISH ? WorkflowTaskType.ANSWER_MERGER_EN : WorkflowTaskType.ANSWER_MERGER_IA;
   }

   private Language language;
   private WorkflowState data;
}

