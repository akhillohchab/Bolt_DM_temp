package com.sri.bolt.workflow.task.common;

import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

public class ForcedAlignmentErrorHandlerTask implements ErrorHandlerTask {

   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public ErrorHandlerTaskType getType() {
      return ErrorHandlerTaskType.FORCED_ALIGNMENT;
   }

   @Override
   public boolean handlesTask(WorkflowTaskType type) {
      return type == WorkflowTaskType.FORCED_ALIGNMENT_EN || type == WorkflowTaskType.FORCED_ALIGNMENT_IA;
   }

   @Override
   public WorkflowState call() throws Exception {
      //Do we have anythign to do here?
      return data;
   }

   WorkflowState data;
}
