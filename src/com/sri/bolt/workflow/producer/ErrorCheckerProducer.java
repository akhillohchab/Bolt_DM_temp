package com.sri.bolt.workflow.producer;

import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.endpoint.ErrorHandlerEndpoint;
import com.sri.bolt.workflow.endpoint.MulticastErrorCheckerEndpoint;
import com.sri.bolt.workflow.exception.TaskFailedException;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;

public class ErrorCheckerProducer extends DefaultProducer {
   public ErrorCheckerProducer(MulticastErrorCheckerEndpoint endpoint) {
      super(endpoint);
   }

   @Override
   public void process(Exchange exchange) throws Exception {
      WorkflowState state = (WorkflowState)exchange.getIn().getBody();
      if (state.getError() != null) {
         throw new TaskFailedException(state.getError().exception);
      }
   }
}
