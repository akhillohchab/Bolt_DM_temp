package com.sri.bolt.workflow.task;

import com.sri.bolt.workflow.WorkflowState;

import java.util.concurrent.Callable;

public interface WorkflowTask extends Callable<WorkflowState> {

   public void setInput(WorkflowState data);

   public boolean validate();

   public WorkflowTaskType getType();
}
