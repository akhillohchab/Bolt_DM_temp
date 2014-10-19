package com.sri.bolt.workflow.endpoint;

import com.sri.bolt.workflow.WorkflowTaskComponent;
import com.sri.bolt.workflow.producer.WorkflowTaskProducer;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.bolt.workflow.task.common.*;
import com.sri.bolt.workflow.task.english.ConfidenceScorerTask;
import com.sri.interfaces.lang.Language;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class WorkflowTaskEndpoint extends DefaultEndpoint {
   public WorkflowTaskEndpoint(String uri, String remaining, WorkflowTaskComponent component) {
      super(uri, component);
      this.remaining = remaining;
   }

   @Override
   public Producer createProducer() throws Exception {
      this.task = getWorkflowTask(remaining);
      return new WorkflowTaskProducer(this, task, isMulticast);
   }

   @Override
   public Consumer createConsumer(Processor processor) throws Exception {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public boolean isSingleton() {
      return false;
   }

   public void setLang(String lang) {
      this.lang = lang.equalsIgnoreCase("english") ? Language.ENGLISH : Language.IRAQI_ARABIC;
   }

   public void setMulticast(String isMulticast) {
      this.isMulticast = Boolean.parseBoolean(isMulticast);
   }

   private WorkflowTask getWorkflowTask(String taskURI) {
      if (taskURI.equalsIgnoreCase("forced_alignment")) {
         return new ForcedAlignmentTask(lang, isMulticast);
      } else if (taskURI.equalsIgnoreCase("oov_detector")) {
         return new OOVTask(lang);
      } else if (taskURI.equalsIgnoreCase("confidence_scorer")) {
         return new ConfidenceScorerTask();
      } else if (taskURI.equalsIgnoreCase("answer_merger")) {
         return new AnswerMergerTask(lang);
      } else if (taskURI.equalsIgnoreCase("dialog_manager")) {
         return new DialogManagerTask(lang);
      } else if (taskURI.equalsIgnoreCase("homophone_detector")) {
         return new HomophoneDetectorTask(lang);
      } else if (taskURI.equalsIgnoreCase("mt_error_detector")) {
         return new MTErrorDetectorTask(lang);
      } else if (taskURI.equalsIgnoreCase("translation")) {
         return new TranslationTask(lang);
      } else if (taskURI.equalsIgnoreCase("asr_error_detector")) {
         return new ASRErrorDetectorTask(lang);
      } else if (taskURI.equalsIgnoreCase("sense_detector")) {
         return new SenseDetectorTask(lang);
      } else if (taskURI.equalsIgnoreCase("translation_pre_process")) {
         return new TranslationPreProcessTask(lang);
      } else if (taskURI.equalsIgnoreCase("clarification")) {
         return new ClarificationTask();
      } else if (taskURI.equalsIgnoreCase("name_detector")) {
         return new NameDetectorTask(lang);
      } else {
         return null;
      }
   }

   private boolean isMulticast;
   private Language lang;
   private String remaining;
   private WorkflowTask task;
}
