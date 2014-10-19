package com.sri.bolt.workflow.task.common;

import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.util.concurrent.Callable;

public interface ErrorHandlerTask extends Callable<WorkflowState> {
   public void setInput(WorkflowState data);

   public ErrorHandlerTaskType getType();

   //represents the workflow task this is designed to handle
   public boolean handlesTask(WorkflowTaskType type);
}
