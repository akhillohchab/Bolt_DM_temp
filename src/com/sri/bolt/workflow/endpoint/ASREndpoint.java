package com.sri.bolt.workflow.endpoint;

import com.sri.bolt.workflow.consumer.ASRConsumer;
import com.sri.bolt.workflow.WorkflowTaskComponent;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class ASREndpoint extends DefaultEndpoint {
   public ASREndpoint(String uri, WorkflowTaskComponent component) {
      super(uri, component);
   }

   @Override
   public Producer createProducer() throws Exception {
      //Doesn't need producer right now
      return null;
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception {
      return new ASRConsumer(this, processor, getLang());
   }

   public String getLang() {
      return lang;
   }

   public void setLang(String lang) {
      this.lang = lang;
   }

   @Override
   public boolean isSingleton() {
      return true;
   }

   private String lang;
}
