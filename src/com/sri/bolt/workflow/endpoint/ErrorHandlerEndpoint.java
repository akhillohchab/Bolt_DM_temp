package com.sri.bolt.workflow.endpoint;

import com.sri.bolt.workflow.WorkflowTaskComponent;
import com.sri.bolt.workflow.producer.ErrorHandlerTaskProducer;
import com.sri.bolt.workflow.task.common.ErrorHandlerTask;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class ErrorHandlerEndpoint extends DefaultEndpoint {
   public ErrorHandlerEndpoint(String uri, WorkflowTaskComponent component, ErrorHandlerTask task) {
      super(uri, component);
      this.task = task;
   }

   @Override
   public Producer createProducer() throws Exception {
      return new ErrorHandlerTaskProducer(this, task);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public boolean isSingleton() {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   private ErrorHandlerTask task;
}
