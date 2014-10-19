package com.sri.bolt.workflow.task.common;

import com.sri.bolt.dm.DialogManager;
import com.sri.bolt.dm.Util;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.AnswerMergerOutput;
import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.DmOutput;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.util.Collections;

public class DialogManagerErrorHandlerTask implements ErrorHandlerTask {
   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public ErrorHandlerTaskType getType() {
      return ErrorHandlerTaskType.DIALOG_MANAGER;
   }

   @Override
   public boolean handlesTask(WorkflowTaskType type) {
      return type == WorkflowTaskType.DIALOG_MANAGER_EN || type == WorkflowTaskType.DIALOG_MANAGER_IA;
   }

   @Override
   public WorkflowState call() throws Exception {
      BoltMessages.SessionData.Builder sessionData = data.getInteractionState().getSessionData().toBuilder();

      final int currentTurn = sessionData.getCurrentTurn();
      final UtteranceData currentUtterance = sessionData.getUtterances(currentTurn);

      DmOutput.Builder dmOutputBuilder = DmOutput.newBuilder();

      // Need to build a translation segment here
      if (currentTurn > 2) {
         String workingUtterance = "";
         if (currentUtterance.hasAnswerMergerOutput()) {
            AnswerMergerOutput answerMergerOutput = currentUtterance.getAnswerMergerOutput();
            if (answerMergerOutput.hasWorkingUtterance())
               workingUtterance = answerMergerOutput.getWorkingUtterance();
         }
         DialogManager.translateWorkingUtterance(dmOutputBuilder, Util.getTranslateSegments(workingUtterance, null));
      } else {
         DmClarifySegment.Builder clarifySegment = DmClarifySegment.newBuilder();
         clarifySegment.setAction(BoltMessages.DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT);
         clarifySegment.setTtsInputWithMarkup("Please rephrase your previous utterance.");
         clarifySegment.setTtsInput("Please rephrase your previous utterance.");
         DialogManager.clarifyErrorSegment(dmOutputBuilder, -1, "ACTION_REJECT", null,
                 Collections.singletonList(clarifySegment.build()));
      }

      UtteranceData.Builder currentUtteranceBuilder = currentUtterance.toBuilder();
      currentUtteranceBuilder.setDmOutput(dmOutputBuilder);
      sessionData.setUtterances(sessionData.getCurrentTurn(), currentUtteranceBuilder);
      data.getInteractionState().setSessionData(sessionData.build());
      return data;
   }

   WorkflowState data;
}
