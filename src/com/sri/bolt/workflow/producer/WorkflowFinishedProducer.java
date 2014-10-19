package com.sri.bolt.workflow.producer;

import com.sri.bolt.App;
import com.sri.bolt.workflow.WorkflowState;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class WorkflowFinishedProducer extends DefaultProducer {
   public WorkflowFinishedProducer(Endpoint endpoint) {
      super(endpoint);
   }

   @Override
   public void process(Exchange exchange) throws Exception {
      WorkflowState state = (WorkflowState) exchange.getIn().getBody();
      App.getApp().getWorkflowController().onFinishedWorkflow(state);
   }
}
