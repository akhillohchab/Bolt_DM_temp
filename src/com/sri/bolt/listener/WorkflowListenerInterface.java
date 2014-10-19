package com.sri.bolt.listener;

import java.util.ArrayList;
import java.util.EventListener;

import com.sri.bolt.message.BoltMessages.SessionData;

public interface WorkflowListenerInterface extends EventListener {
   public void onStartWorkflow(String trialId, long timeWorkflowStarted);
 
	public void workflowTaskComplete(SessionData sessionData, boolean successful, String name);
	
	public void onExceptionFound(String exception);
	
}
