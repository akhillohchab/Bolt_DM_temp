package com.sri.bolt.workflow.endpoint;

import com.sri.bolt.workflow.WorkflowTaskComponent;
import com.sri.bolt.workflow.producer.WorkflowFailedProducer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class WorkflowFailedEndpoint extends DefaultEndpoint {
   public WorkflowFailedEndpoint(String uri, WorkflowTaskComponent component) {
      super(uri, component);
   }

   @Override
   public Producer createProducer() throws Exception {
      return new WorkflowFailedProducer(this);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public boolean isSingleton() {
      return false;
   }
}
