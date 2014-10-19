package com.sri.bolt.workflow.producer;


import com.sri.bolt.App;
import com.sri.bolt.TimeKeeper;
import com.sri.bolt.workflow.ErrorState;
import com.sri.bolt.workflow.Util;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.endpoint.WorkflowTaskEndpoint;
import com.sri.bolt.workflow.exception.*;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.Language;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowTaskProducer extends DefaultProducer {
   public WorkflowTaskProducer(WorkflowTaskEndpoint endpoint, WorkflowTask task, boolean isMulticast) {
      super(endpoint);
      this.isMulticast = isMulticast;
      this.task = task;
   }

   @Override
   public void process(Exchange exchange) throws Exception {
      WorkflowState input = (WorkflowState) exchange.getIn().getBody();
      if (input.getStartingTask() == null || input.getStartingTask().equals(task.getType())) {
         String name = com.sri.bolt.workflow.Util.getNameForWorkflowType(task.getType());
         logger.info("Starting " + name + " processing");
         task.setInput(input);

         boolean validates = validate(input);
         boolean hasError = false;
         WorkflowState output = null;
         if (validates) {
            try {
               TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
               String taskType;
               if (task.getType() == WorkflowTaskType.CLARIFICATION) {
                  taskType = "Clarification " + (input.getInteractionState().getLanguage() == Language.IRAQI_ARABIC ? "IA" : "EN");
               } else {
                  taskType = Util.getNameForWorkflowType(task.getType());
               }
               timeKeeper.startTiming(taskType);
               output = task.call();
               timeKeeper.stopTiming(taskType);
            } catch (Exception e) {
               logger.error("Exception " + e + " caught while running " + name, e);
               App.getApp().getWorkflowController().notifyListenersError("Exception " + e + " caught while running " + name);
               hasError = true;
               handleError(input, e);
            }

            if (output == null) {
               logger.error("Task " + name + " returned null");
               App.getApp().getWorkflowController().notifyListenersError("Task " + name + " returned null");
               hasError = true;
               handleError(input, null);
            }
         }

         if (!hasError && validates) {
            output.setStartingTask(null);
            exchange.getIn().setBody(output);
            //TODO Readd
            //output.getTrial().write();

            App.getApp().getWorkflowController().notifyWorkflowTaskComplete(output.getInteractionState().getSessionData(), true, name);
            logger.info("Finished " + name + " processing");
         }
      }
   }

   private void handleError(WorkflowState input, Exception e) throws Exception {
      String name = com.sri.bolt.workflow.Util.getNameForWorkflowType(task.getType());
      App.getApp().getWorkflowController().notifyWorkflowTaskComplete(input.getInteractionState().getSessionData(), false, name);
      if (task.getType() == WorkflowTaskType.HOMOPHONE_DETECTOR_EN
              || task.getType() == WorkflowTaskType.HOMOPHONE_DETECTOR_IA
              || task.getType() == WorkflowTaskType.MT_ERROR_DETECTOR_EN
              || task.getType() == WorkflowTaskType.MT_ERROR_DETECTOR_IA
              || task.getType() == WorkflowTaskType.ASR_ERROR_DETECTOR_EN
              || task.getType() == WorkflowTaskType.ASR_ERROR_DETECTOR_IA
              || task.getType() == WorkflowTaskType.SENSE_DETECTOR_EN
              || task.getType() == WorkflowTaskType.SENSE_DETECTOR_IA
              || task.getType() == WorkflowTaskType.NAME_DETECTOR_EN
              || task.getType() == WorkflowTaskType.NAME_DETECTOR_IA) {
         //do nothing for these errors
      } else if (task.getType() == WorkflowTaskType.EN_TRANSLATION || task.getType() == WorkflowTaskType.IA_TRANSLATION || task.getType() == WorkflowTaskType.CLARIFICATION
              || task.getType() == WorkflowTaskType.TRANSLATION_PREPROCESS_EN || task.getType() == WorkflowTaskType.TRANSLATION_PREPROCESS_IA) {
         ErrorState err = new ErrorState();
         err.exception = e;
         err.errorSource = task.getType();
         err.runRest = false;

         input.setError(err);
         throw new UnrecoverableErrorException();
      } else {
         ErrorState err = new ErrorState();
         err.exception = e;
         err.errorSource = task.getType();
         err.runRest = false;

         input.setError(err);
         Exception taskEx = getException(e);
         throw taskEx;
      }
   }

   private boolean validate(WorkflowState input) throws Exception{
      if (!task.validate()) {
         String name = com.sri.bolt.workflow.Util.getNameForWorkflowType(task.getType());
         logger.error("Validation failed while running " + name);
         handleError(input, null);
         return false;
      }

      return true;
   }

   private Exception getException(Exception parent) {
      if (task.getType() == WorkflowTaskType.OOV_EN || task.getType() == WorkflowTaskType.OOV_IA) {
         return new OOVException(parent);
      } else if (task.getType() == WorkflowTaskType.DIALOG_MANAGER_EN || task.getType() == WorkflowTaskType.DIALOG_MANAGER_IA) {
         return new DialogManagerException(parent);
      } else if (task.getType() == WorkflowTaskType.ANSWER_MERGER_EN || task.getType() == WorkflowTaskType.ANSWER_MERGER_IA) {
         return new AnswerMergerException(parent);
      } else if (task.getType() == WorkflowTaskType.FORCED_ALIGNMENT_EN || task.getType() == WorkflowTaskType.FORCED_ALIGNMENT_IA) {
         return new ForcedAlignmentException(parent);
      } else {
         return new UnrecoverableErrorException();
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(WorkflowTaskProducer.class);
   private boolean isMulticast;
   private WorkflowTask task;
}
