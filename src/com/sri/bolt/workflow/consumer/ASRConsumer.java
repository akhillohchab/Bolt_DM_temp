package com.sri.bolt.workflow.consumer;


import com.sri.bolt.App;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.endpoint.ASREndpoint;
import com.sri.interfaces.lang.Language;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultScheduledPollConsumer;

import java.util.concurrent.BlockingQueue;

public class ASRConsumer extends DefaultScheduledPollConsumer {
   public ASRConsumer(ASREndpoint endpoint, Processor processor, String lang) {
      super(endpoint, processor);
      if (lang.equalsIgnoreCase("english")) {
         this.language = Language.ENGLISH;
      } else {
         this.language = Language.IRAQI_ARABIC;
      }
   }

   @Override
   protected int poll() throws Exception {
      BlockingQueue<WorkflowState> queue = App.getApp().getWorkflowController().getWorkflowQueue(language);
      if (queue.size() == 0) {
         return 0;
      } else {
         WorkflowState state = queue.take();
         Exchange ex = getEndpoint().createExchange();
         ex.getIn().setBody(state);
         getProcessor().process(ex);

         return 1;
      }
   }

   private Language language;
}
