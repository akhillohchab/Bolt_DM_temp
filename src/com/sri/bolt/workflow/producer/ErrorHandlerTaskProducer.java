package com.sri.bolt.workflow.producer;


import com.sri.bolt.App;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.endpoint.ErrorHandlerEndpoint;
import com.sri.bolt.workflow.exception.UnrecoverableErrorException;
import com.sri.bolt.workflow.task.common.ErrorHandlerTask;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandlerTaskProducer extends DefaultProducer {
   public ErrorHandlerTaskProducer(ErrorHandlerEndpoint endpoint, ErrorHandlerTask task) {
      super(endpoint);
      this.task = task;
   }

   @Override
   public void process(Exchange exchange) throws Exception {
      String name = task.getType().toString();
      logger.debug("Starting " + name + " processing");
      WorkflowState input = (WorkflowState) exchange.getIn().getBody();

      task.setInput(input);
      WorkflowState output = input;
      try {
         if (input.getError().runRest || task.handlesTask(input.getError().errorSource)) {
            output = task.call();
            output.getError().runRest = true;
         }
      } catch (Exception e) {
         logger.error("Exception " + e + " caught while running " + name, e);
         App.getApp().getWorkflowController().notifyWorkflowTaskComplete(input.getInteractionState().getSessionData(), false, name);

         throw new UnrecoverableErrorException();
      }

      exchange.getIn().setBody(output);

     // output.getTrial().write();

      App.getApp().getWorkflowController().notifyWorkflowTaskComplete(output.getInteractionState().getSessionData(), true, name);

      logger.debug("Finished " + name + " processing");
   }

   private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerTaskProducer.class);
   ErrorHandlerTask task;
}
