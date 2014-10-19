package com.sri.bolt.workflow.task.english;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;

public class ConfidenceScorerTask implements WorkflowTask {

   @Override
   public WorkflowState call() throws Exception {

      SessionData resultData = App.getApp().getServiceController().ConfidenceScorerProcessData(data.getInteractionState().getSessionData());
      if (data == null || data.getInteractionState().getSessionData().getSerializedSize() == 0) {
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
      return WorkflowTaskType.CONF_SCORER;
   }

   private WorkflowState data;
}

