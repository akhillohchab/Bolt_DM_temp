package com.sri.bolt.workflow.aggregators;

import com.sri.bolt.App;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.common.ForcedAlignmentTask;
import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OovFaAggregationStrategy implements AggregationStrategy{
   @Override
   public Exchange aggregate(Exchange oldEx, Exchange newEx) {
      if (oldEx == null) {
         return newEx;
      } else {
         WorkflowState oldExState = (WorkflowState)oldEx.getIn().getBody();
         if (oldExState.getStartingTask() == null
                 || oldExState.getStartingTask().equals(WorkflowTaskType.FORCED_ALIGNMENT_EN)
                 || oldExState.getStartingTask().equals(WorkflowTaskType.FORCED_ALIGNMENT_IA)
                 || oldExState.getStartingTask().equals(WorkflowTaskType.OOV_EN)
                 || oldExState.getStartingTask().equals(WorkflowTaskType.OOV_IA)) {

            WorkflowState oovState;
            WorkflowState faState;
            if (((WorkflowState)oldEx.getIn().getBody()).getFaResult() != null) {
               faState = (WorkflowState)oldEx.getIn().getBody();
               oovState = (WorkflowState)newEx.getIn().getBody();
            } else {
               faState = (WorkflowState)newEx.getIn().getBody();
               oovState = (WorkflowState)oldEx.getIn().getBody();
            }

            //if we have an error in the oov, just return the state, since the fa error recovery will run after the oov error
            //recovery.  If the error is in the fa, we have to make sure to set the oov correctly
            oovState.setFaResult(faState.getFaResult());
            ForcedAlignmentTask.setForcedAlignmentResults(faState.getFaResult(), oovState);

            oldEx.getIn().setBody(oovState);
            App.getApp().getTrial().setCurrentInteraction(oovState.getInteractionState());
            String name = "OOV_FA_MERGE";
            App.getApp().getWorkflowController().notifyWorkflowTaskComplete(oovState.getInteractionState().getSessionData(), true, name);
            logger.debug("Finished " + name + " processing");

            return oldEx;

         } else {
            return oldEx;
         }
      }

   }

   private static final Logger logger = LoggerFactory.getLogger(OovFaAggregationStrategy.class);
}
