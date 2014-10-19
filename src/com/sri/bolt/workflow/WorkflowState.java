package com.sri.bolt.workflow;

import com.sri.bolt.App;
import com.sri.bolt.EvalType;
import com.sri.bolt.dm.DialogManager;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.DmActionType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.Language;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;

import java.util.List;

public class WorkflowState {
   public WorkflowState() {
   }
   public WorkflowState(WorkflowState state) {
      if (state.getStartingTask() != null) {
         this.startingTask = state.getStartingTask();
      }
      if (state.getError() != null) {
         this.error = new ErrorState(state.getError());
      }

      this.state = new InteractionState(state.getInteractionState());
   }
   public ErrorState getError() {
      return error;
   }

   public void setError(ErrorState error) {
      this.error = error;
   }

   public InteractionState getInteractionState() {
      return state;
   }

   public void setInteractionState(InteractionState state) {
      this.state = state;
   }

   public void setStartingTask(WorkflowTaskType task) {
      this.startingTask = task;
   }

   public WorkflowTaskType getStartingTask() {
      return startingTask;
   }

   //utility functions to make predicate evaluation easier
   public BoltMessages.UtteranceData getLastUtterance() {
      return state.getSessionData().getUtterances(state.getSessionData().getUtterancesCount() - 1);
   }

   public boolean isClarifyUtterance() {
      return getLastUtterance().getDmOutput().getDmAction() == BoltMessages.DmActionType.ACTION_CLARIFY_UTTERANCE;
   }

   public boolean isTranslateUtterance() {
      return getLastUtterance().getDmOutput().getDmAction() == BoltMessages.DmActionType.ACTION_TRANSLATE_UTTERANCE;
   }

   public boolean isRestart() {
      return getLastUtterance().getDmOutput().getDmAction() == DmActionType.ACTION_RESTART;
   }

   public boolean isMoveOn() {
      return getLastUtterance().getDmOutput().getDmAction() == DmActionType.ACTION_MOVE_ON;
   }

   public boolean isEnglish() {
      return getInteractionState().getLanguage() == Language.ENGLISH;
   }

   public boolean hasMTError() {
      List<BoltMessages.ErrorSegmentAnnotation> errorSegments = state.getSessionData().getUtterances(state.getSessionData().getCurrentTurn())
              .getErrorSegmentsList();
      for (BoltMessages.ErrorSegmentAnnotation errorSegment : errorSegments) {
         if (errorSegment.hasIsMtOov() && !(errorSegment.hasIsResolved())) {
            return true;
         }
      }

      return false;
   }

   public boolean askClarificationQuestion() {
      return App.getApp().getEvalType() == EvalType.WITH_CLARIFICATION || App.getApp().getEvalType() == EvalType.ACTIVITY_B_RETEST;
   }

   public SessionData getSessionData() {
      return state.getSessionData();
   }

   public CombinedRecognizerResult getFaResult() {
      return faResult;
   }

   public void setFaResult(CombinedRecognizerResult faResult) {
      this.faResult = faResult;
   }

   private InteractionState state;
   private WorkflowTaskType startingTask;

   private CombinedRecognizerResult faResult;

   private ErrorState error;
}
