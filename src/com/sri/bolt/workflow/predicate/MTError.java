package com.sri.bolt.workflow.predicate;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.workflow.WorkflowState;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import java.util.List;

public class MTError implements Predicate {
   @Override
   public boolean matches(Exchange exchange) {
      BoltMessages.SessionData data = ((WorkflowState) exchange.getIn().getBody()).getInteractionState().getSessionData();
      List<BoltMessages.ErrorSegmentAnnotation> errorSegments = data.getUtterances(data.getCurrentTurn())
              .getErrorSegmentsList();
      for (BoltMessages.ErrorSegmentAnnotation errorSegment : errorSegments) {
         if (errorSegment.hasIsMtOov() && !(errorSegment.hasIsResolved())) {
            return true;
         }
      }

      return false;
   }
}
