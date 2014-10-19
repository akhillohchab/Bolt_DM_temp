package com.sri.bolt.workflow;

import com.sri.bolt.workflow.task.WorkflowTaskType;

public class ErrorState {
   public ErrorState() {
   }

   public ErrorState(ErrorState state) {
      this.exception = state.exception;
      this.errorSource = state.errorSource;
      this.runRest = state.runRest;
   }

   public Exception exception;
   public WorkflowTaskType errorSource;
   public boolean runRest;
}
