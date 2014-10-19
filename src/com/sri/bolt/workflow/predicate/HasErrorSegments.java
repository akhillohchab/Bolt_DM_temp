package com.sri.bolt.workflow.predicate;


import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.workflow.WorkflowState;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

public class HasErrorSegments implements Predicate{
   @Override
   public boolean matches(Exchange exchange) {
      SessionData data = ((WorkflowState) exchange.getIn().getBody()).getInteractionState().getSessionData();
      UtteranceData utt = data.getUtterances(data.getCurrentTurn());
      return utt.getErrorSegmentsCount() != 0;
   }
}
