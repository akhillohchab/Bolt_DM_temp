package com.sri.bolt.workflow.endpoint;

import com.sri.bolt.workflow.WorkflowTaskComponent;
import com.sri.bolt.workflow.producer.ErrorCheckerProducer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class MulticastErrorCheckerEndpoint extends DefaultEndpoint {
   public MulticastErrorCheckerEndpoint(String uri, WorkflowTaskComponent component) {
      super(uri, component);
   }
   @Override
   public Producer createProducer() throws Exception {
      return new ErrorCheckerProducer(this);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception {
      return null;
   }

   @Override
   public boolean isSingleton() {
      return true;
   }
}
