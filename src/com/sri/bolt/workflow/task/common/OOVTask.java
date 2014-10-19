package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OOVTask implements WorkflowTask {
   public OOVTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      //we need to reset the rescored one best if ASR Error Detector set it.  If the ASR error detector is "gating"
      //this component, then it will set the rescored one best if there are no error segments.  In the non gating situation,
      //we want to make sure that the rescored one best is unset before this component.
      SessionData session = data.getInteractionState().getSessionData();
      UtteranceData.Builder utt = session.getUtterances(session.getCurrentTurn()).toBuilder();
      if (utt.getErrorSegmentsCount() == 0) {
         utt.setRescored1Best("");
         session = session.toBuilder().setUtterances(session.getCurrentTurn(), utt).build();
      }
      SessionData resultData = App.getApp().getServiceController().uwProcessData(session, language);
      if (resultData == null || resultData.getSerializedSize() == 0) {
         return null;
      } else {
         BoltMessages.UtteranceData currentUtterance = resultData.getUtterances(resultData.getCurrentTurn());
         boolean validRescored1Best = false;
         if (currentUtterance.hasRescored1Best()) {
            String checkResult = currentUtterance.getRescored1Best().trim();
            if (checkResult.length() > 0) {
               validRescored1Best = true;
            }
         }
         if (!validRescored1Best) {
            // Set from the recognizer1Best directly
            String recognizer1Best = currentUtterance.getRecognizer1Best();
            logger.info("Overriding missing rescored1Best with recognizer1Best: " + recognizer1Best);
            utt = currentUtterance.toBuilder();
            utt.setRescored1Best(recognizer1Best);
            resultData = resultData.toBuilder().setUtterances(resultData.getCurrentTurn(), utt).build();
            currentUtterance = utt.build();
         }
         String curResult = currentUtterance.getRescored1Best();
         logger.info("Updated ASR result: " + curResult);
         logger.debug("Updated ASR result: " + curResult);
         data.getInteractionState().setSessionData(resultData);
         data.getInteractionState().setUWSessionData(resultData);
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
      return language == Language.ENGLISH ? WorkflowTaskType.OOV_EN : WorkflowTaskType.OOV_IA;
   }

   private static final Logger logger = LoggerFactory.getLogger(OOVTask.class);
   private Language language;
   private WorkflowState data;
}

