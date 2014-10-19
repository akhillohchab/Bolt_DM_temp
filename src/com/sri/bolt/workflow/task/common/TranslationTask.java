package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.EvalType;
import com.sri.bolt.message.BoltMessages.*;
import com.sri.bolt.state.TranslationState;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.TranslationTaskReturn;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;

public class TranslationTask implements WorkflowTask {
   public TranslationTask(Language lang) {
      language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData sessionData = data.getInteractionState().getSessionData();
      SessionData.Builder sessionDataBuilder = sessionData.toBuilder();
      final int curTurn = sessionData.getCurrentTurn();
      UtteranceData currentUtterance = sessionData.getUtterances(curTurn);
      data.getInteractionState().startTranslation();

      UtteranceData.Builder currentUtteranceBuilder = sessionDataBuilder.getUtterancesBuilder(curTurn);
      MtData.Builder mtBuilder = currentUtteranceBuilder.getMtDataBuilder();

      if (App.getApp().getEvalType() == EvalType.NO_CLARIFICATION) {
         mtBuilder.setOriginalInput(currentUtterance.getRecognizer1Best());
         mtBuilder.setPreprocessedInput(App.getApp().getServiceController().translatePreprocess(mtBuilder.getOriginalInput(), language));

         currentUtteranceBuilder.setMtData(mtBuilder);
         sessionDataBuilder.setUtterances(curTurn, currentUtteranceBuilder);
      }

      TranslationState transState = data.getInteractionState().getTranslationState();
      transState.startTranslation();

      String translationResult;
      // Setting "true" since already preprocessed
      TranslationTaskReturn r = App.getApp().getServiceController()
              .translateTextExtended(mtBuilder.getPreprocessedInput(), language, true);
      // This is the cleaned final translation, in Buckwalter as
      // of 5/8/2013.
      final String postprocessedTranslationResult = r.getResultText();

      // The next two are intermediate versions of text

      // Results after filtering the trans input prior to translation.
      // Note that since we did the preprocessing, we expect this to be
      // idential to actualInput.
      // Raw translation results before post-processing
      final String originalTranslation = r.getOriginalTranslation();
      // alignment between raw translation and preprocessed input
      final String alignment = r.getAlignment();

      // Set MT 1 best and update translation in object
      mtBuilder.addAllNbestComposite(r.getnBest());

      mtBuilder.addOriginalTranslations(originalTranslation);
      mtBuilder.addPostprocessedTranslations(postprocessedTranslationResult);
      if (alignment != null && !alignment.isEmpty())
         mtBuilder.addAlignments(alignment);
      currentUtteranceBuilder.setMtData(mtBuilder);
      sessionDataBuilder.setUtterances(curTurn, currentUtteranceBuilder);
      translationResult = postprocessedTranslationResult;

      transState.endTranslation();
      transState.setTranslation(mtBuilder.getOriginalInput(), language == Language.ENGLISH ? BuckwalterUnicodeConverter.buckwalterToUnicode(translationResult, Language.IRAQI_ARABIC) : translationResult);

      data.getInteractionState().setTranslation(transState);
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
      return language == Language.ENGLISH ? WorkflowTaskType.EN_TRANSLATION : WorkflowTaskType.IA_TRANSLATION;
   }

   private Language language;
   WorkflowState data;
}
