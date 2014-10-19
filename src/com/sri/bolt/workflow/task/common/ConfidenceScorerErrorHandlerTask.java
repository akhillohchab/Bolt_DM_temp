package com.sri.bolt.workflow.task.common;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.util.List;

public class ConfidenceScorerErrorHandlerTask implements ErrorHandlerTask {
   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public ErrorHandlerTaskType getType() {
      return ErrorHandlerTaskType.CONF_SCORER;
   }

   @Override
   public boolean handlesTask(WorkflowTaskType type) {
      return type == WorkflowTaskType.CONF_SCORER;
   }

   @Override
   public WorkflowState call() throws Exception {
      BoltMessages.SessionData.Builder currentData = data.getInteractionState().getSessionData().toBuilder();
      BoltMessages.UtteranceData.Builder currentUtterance = currentData.getUtterancesBuilder(currentData.getCurrentTurn());
      List<BoltMessages.WordAnnotation.Builder> annotations = currentUtterance.getWordLevelAnnotationsBuilderList();
      for (BoltMessages.WordAnnotation.Builder annotation : annotations) {
         annotation.setCuConfidence(BoltMessages.DoubleAttribute.newBuilder().setValue(1.0));
      }

      for (int count = 0; count < annotations.size(); ++count) {
         currentUtterance.setWordLevelAnnotations(count, annotations.get(count));
      }
      currentData.setUtterances(currentData.getCurrentTurn(), currentUtterance);
      data.getInteractionState().setSessionData(currentData.build());
      return data;

   }

   WorkflowState data;
}
