package com.sri.bolt.workflow.predicate;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.workflow.WorkflowState;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

public class InitialTranslation implements Predicate {
   @Override
   public boolean matches(Exchange exchange) {
      SessionData data = ((WorkflowState) exchange.getIn().getBody()).getInteractionState().getSessionData();
      return data.getUtterancesCount() == 1 && App.getApp().getProps().getProperty("InitialTranslation", "true").equalsIgnoreCase("true");
   }
}
