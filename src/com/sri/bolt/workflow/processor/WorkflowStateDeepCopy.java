package com.sri.bolt.workflow.processor;

import com.sri.bolt.workflow.WorkflowState;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class WorkflowStateDeepCopy implements Processor{
   @Override
   public void process(Exchange exchange) throws Exception {
      WorkflowState state = exchange.getIn().getBody(WorkflowState.class);
      WorkflowState copy = new WorkflowState(state);
      exchange.getIn().setBody(copy);
   }
}
