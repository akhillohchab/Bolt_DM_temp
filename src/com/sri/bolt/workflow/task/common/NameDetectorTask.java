package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.Language;

/**
 * Created with IntelliJ IDEA.
 * User: pblasco
 * Date: 10/7/13
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class NameDetectorTask implements WorkflowTask {
   public NameDetectorTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData resultData = null;//App.getApp().getServiceController().detectNames(data.getInteractionState().getSessionData(), language);
      if (resultData == null || resultData.getSerializedSize() == 0) {
         return null;
      } else {
         data.getInteractionState().setSessionData(resultData);
         return data;
      }
   }

   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public boolean validate() {
      return true;
   }

   @Override
   public WorkflowTaskType getType() {
      return language == Language.ENGLISH ? WorkflowTaskType.NAME_DETECTOR_EN : WorkflowTaskType.NAME_DETECTOR_IA;
   }

   private Language language;
   WorkflowState data;
}
