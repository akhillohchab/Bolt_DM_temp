package com.sri.bolt.workflow;

import com.sri.bolt.workflow.endpoint.*;
import com.sri.bolt.workflow.task.common.AnswerMergerErrorHandlerTask;
import com.sri.bolt.workflow.task.common.ConfidenceScorerErrorHandlerTask;
import com.sri.bolt.workflow.task.common.DialogManagerErrorHandlerTask;
import com.sri.bolt.workflow.task.common.ErrorHandlerTask;
import com.sri.bolt.workflow.task.common.ForcedAlignmentErrorHandlerTask;
import com.sri.bolt.workflow.task.common.OOVErrorHandlerTask;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

import java.util.Map;

public class WorkflowTaskComponent extends DefaultComponent {
   @Override
   protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
      if (remaining.equalsIgnoreCase("ASR")) {
         return new ASREndpoint(uri, this);
      } else if (remaining.equalsIgnoreCase("workflow_finished")) {
         return new WorkflowFinishedEndpoint(uri, this);
      } else if (remaining.equalsIgnoreCase("workflow_failed")) {
         return new WorkflowFailedEndpoint(uri, this);
      } else if (remaining.contains("error_handler")) {
         return new ErrorHandlerEndpoint(uri, this, getErrorHandlerTask(remaining));
      } else if (remaining.contains("multicast_error_checker")) {
         return new MulticastErrorCheckerEndpoint(uri, this);
      } else {
         return new WorkflowTaskEndpoint(uri, remaining, this);
      }
   }

   private ErrorHandlerTask getErrorHandlerTask(String taskUri) {
      if (taskUri.equalsIgnoreCase("oov_detector_error_handler")) {
         return new OOVErrorHandlerTask();
      } else if (taskUri.equalsIgnoreCase("forced_alignment_error_handler")) {
         return new ForcedAlignmentErrorHandlerTask();
      } else if (taskUri.equalsIgnoreCase("confidence_scorer_error_handler")) {
         return new ConfidenceScorerErrorHandlerTask();
      } else if (taskUri.equalsIgnoreCase("answer_merger_error_handler")) {
         return new AnswerMergerErrorHandlerTask();
      } else if (taskUri.equalsIgnoreCase("dialog_manager_error_handler")) {
         return new DialogManagerErrorHandlerTask();
      } else {
         return null;
      }
   }
}
