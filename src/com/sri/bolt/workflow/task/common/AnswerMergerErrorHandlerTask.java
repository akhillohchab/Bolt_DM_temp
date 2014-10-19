package com.sri.bolt.workflow.task.common;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.util.List;

public class AnswerMergerErrorHandlerTask implements ErrorHandlerTask {
   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public ErrorHandlerTaskType getType() {
      return ErrorHandlerTaskType.ANSWER_MERGER;
   }

   public boolean handlesTask(WorkflowTaskType type) {
      return type == WorkflowTaskType.ANSWER_MERGER_EN || type == WorkflowTaskType.ANSWER_MERGER_IA;
   }

   @Override
   public WorkflowState call() throws Exception {
      BoltMessages.SessionData.Builder currentData = data.getInteractionState().getSessionData().toBuilder();
      BoltMessages.UtteranceData.Builder currentUtterance = currentData.getUtterancesBuilder(currentData.getCurrentTurn());
      // fill out answer merger output with recog one best and word index
      // pointers
      String rescoredOneBest = currentUtterance.getRescored1Best();
      BoltMessages.AnswerMergerOutput.Builder amBuilder = BoltMessages.AnswerMergerOutput.newBuilder();
      String[] splitOneBest;
      if (currentData.getCurrentTurn() > 0) {
         BoltMessages.UtteranceData prevUtterance = currentData.getUtterances(currentData.getCurrentTurn() - 1);
         String prevWorkingUtt = prevUtterance.getAnswerMergerOutput().getWorkingUtterance();
         splitOneBest = prevWorkingUtt.split("\\s+");
         amBuilder.setWorkingUtterance(prevWorkingUtt);
         // use the word index pointers from previous utterance
         List<BoltMessages.WordIndexPointer> prevWordIndexPointers = prevUtterance.getAnswerMergerOutput().getWordIndexPointersList();
         for (int count = 0; count < prevWordIndexPointers.size(); ++count) {
            amBuilder.addWordIndexPointers(BoltMessages.WordIndexPointer.newBuilder()
                    .setUtteranceIndex(prevWordIndexPointers.get(count).getUtteranceIndex()).setWordIndex(prevWordIndexPointers.get(count).getWordIndex()));
         }
         amBuilder.setMergingOperation(BoltMessages.AnswerMergerOperationType.OPERATION_USE_PREVIOUS_RESCORED);
      } else {
         splitOneBest = rescoredOneBest.split("\\s+");
         amBuilder.setWorkingUtterance(rescoredOneBest);
         for (int count = 0; count < splitOneBest.length; ++count) {
            amBuilder.addWordIndexPointers(BoltMessages.WordIndexPointer.newBuilder()
                    .setUtteranceIndex(currentData.getCurrentTurn()).setWordIndex(count));
         }
         amBuilder.setMergingOperation(BoltMessages.AnswerMergerOperationType.OPERATION_USE_LAST_RESCORED);
      }
      currentUtterance.setAnswerMergerOutput(amBuilder);
      currentData.setUtterances(currentData.getCurrentTurn(), currentUtterance);
      data.getInteractionState().setSessionData(currentData.build());
      return data;
   }

   WorkflowState data;
}
