package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.dm.Util;
import com.sri.bolt.message.BoltMessages.*;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.Language;

import java.util.List;

public class TranslationPreProcessTask implements WorkflowTask {
   public TranslationPreProcessTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData sessionData = data.getSessionData();
      final int currentTurn = sessionData.getCurrentTurn();
      UtteranceData currentUtterance = sessionData.getUtterances(currentTurn);

      String workingUtterance = "";
      if (currentUtterance.hasAnswerMergerOutput()) {
         AnswerMergerOutput answerMergerOutput = currentUtterance.getAnswerMergerOutput();
         if (answerMergerOutput.hasWorkingUtterance())
            workingUtterance = answerMergerOutput.getWorkingUtterance();
      }

      List<DmTranslateSegment> translateOutput = Util.getTranslateSegments(workingUtterance,
              currentUtterance.getErrorSegmentsList());

      // This is what we log for originalInput
      StringBuilder originalInputString = new StringBuilder();
      // Manually build up a processed string we can send to translator
      StringBuilder manualPreprocessedInputString = new StringBuilder();

      for (DmTranslateSegment segment : translateOutput) {
         if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT)) {
            String rawMtInput = segment.getMtInput();
            originalInputString.append(rawMtInput + " ");
            String preprocessedResult = App.getApp().getServiceController()
                    .translatePreprocess(rawMtInput, language);
            manualPreprocessedInputString.append(preprocessedResult + " ");
         } else if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT)) {
            // Process language-specific transliteration
            String rawTlInput = segment.getMtInput();
            if (rawTlInput != null) {
               originalInputString.append(rawTlInput.replaceAll("\\s+", "") + " ");
               String tlResult = App.getApp().getServiceController().translateTLTextExtended(rawTlInput, language)
                       .getResultText();
               manualPreprocessedInputString.append("$name { " + tlResult + " } ");
            }
         }
      }

      SessionData.Builder sessionDataBuilder = sessionData.toBuilder();
      UtteranceData.Builder currentUtteranceBuilder = currentUtterance.toBuilder();
      MtData.Builder mtBuilder = currentUtteranceBuilder.getMtDataBuilder();
      mtBuilder.setOriginalInput(originalInputString.toString().trim());
      mtBuilder.setPreprocessedInput(manualPreprocessedInputString.toString().trim());
      currentUtteranceBuilder.setMtData(mtBuilder);
      sessionDataBuilder.setUtterances(currentTurn, currentUtteranceBuilder);

      data.getInteractionState().setSessionData(sessionDataBuilder.build());

      return data;
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
      return language == Language.ENGLISH ? WorkflowTaskType.TRANSLATION_PREPROCESS_EN
              : WorkflowTaskType.TRANSLATION_PREPROCESS_IA;
   }

   private Language language;
   private WorkflowState data;
}
