package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.Language;

public class HomophoneDetectorTask implements WorkflowTask {
   public HomophoneDetectorTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData resultData = App.getApp().getServiceController().processHomophones(data.getInteractionState().getSessionData(), language);
      data.getInteractionState().setSessionData(resultData);
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
      return language == Language.ENGLISH ? WorkflowTaskType.HOMOPHONE_DETECTOR_EN : WorkflowTaskType.HOMOPHONE_DETECTOR_IA;
   }


   private Language language;
   private WorkflowState data;
}
   