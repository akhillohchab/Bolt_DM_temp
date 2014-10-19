package com.sri.bolt.workflow.endpoint;


import com.sri.bolt.workflow.WorkflowTaskComponent;
import com.sri.bolt.workflow.producer.WorkflowFinishedProducer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class WorkflowFinishedEndpoint extends DefaultEndpoint {
   public WorkflowFinishedEndpoint(String uri, WorkflowTaskComponent component) {
      super(uri, component);
   }

   @Override
   public Producer createProducer() throws Exception {
      return new WorkflowFinishedProducer(this);
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
