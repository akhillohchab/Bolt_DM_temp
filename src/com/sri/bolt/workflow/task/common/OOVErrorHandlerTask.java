package com.sri.bolt.workflow.task.common;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.exception.UnrecoverableErrorException;
import com.sri.bolt.workflow.task.WorkflowTaskType;

public class OOVErrorHandlerTask implements ErrorHandlerTask {
   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public ErrorHandlerTaskType getType() {
      return ErrorHandlerTaskType.OOV;
   }

   public boolean handlesTask(WorkflowTaskType type) {
      return type == WorkflowTaskType.OOV_IA || type == WorkflowTaskType.OOV_EN;
   }

   @Override
   public WorkflowState call() throws Exception {
      // if we fail at UW, set rescored 1 best
         BoltMessages.SessionData.Builder currentData = data.getInteractionState().getSessionData().toBuilder();
         BoltMessages.UtteranceData.Builder currentUtterance = currentData.getUtterancesBuilder(currentData.getCurrentTurn());
         String recogOneBest = currentUtterance.getRecognizer1Best();
         currentUtterance.setRescored1Best(recogOneBest);
         currentData.setUtterances(currentData.getCurrentTurn(), currentUtterance);
         data.getInteractionState().setSessionData(currentData.build());
         data.setError(null);
         return data;

   }

   WorkflowState data;
}
