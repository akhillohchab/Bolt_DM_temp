package com.sri.bolt.dm;

import com.sri.bolt.message.BoltMessages.*;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DialogManager {

   public static final String DM_ACTION_RESTART = "ACTION_RESTART";
   public static final String DM_ACTION_MOVE_ON = "ACTION_MOVE_ON";
   public static final String DM_ACTION_TRANSLATE = "ACTION_TRANSLATE";
   public static final String DM_ACTION_PREAMBLE_SORRY = "ACTION_PREAMBLE_SORRY";
   public static final String DM_ACTION_PREAMBLE_SORRY_RULEID = "R9.0";

   private DmResponseTable responseTable;
   private DmBranchTable branchTable;
   private Map<String, Double> dmPriorities;

   public DialogManager(String responseXmlfile, String branchXmlfile, String priorities, Language lang,
         boolean randomize) {
	  System.out.println ("hello");
      responseTable = new DmResponseTable(responseXmlfile, lang, randomize);
      branchTable = new DmBranchTable(branchXmlfile);
      dmPriorities = new HashMap<String, Double>();
      for (String priority : priorities.split(",")) {
         int pos = priority.lastIndexOf(":");
         if (pos > 0 && pos + 1 < priority.length()) {
            try {
               double value = Double.parseDouble(priority.substring(pos + 1));
               dmPriorities.put(priority.substring(0, pos), value);
            } catch (NumberFormatException e) {
            }
         }
      }
   }
   
   
   public SessionData process(SessionData sessionData) {
      final int currentTurn = sessionData.getCurrentTurn();
      final UtteranceData currentUtterance = sessionData.getUtterances(currentTurn);

      String workingUtterance = "";
      if (currentUtterance.hasAnswerMergerOutput()) {
         AnswerMergerOutput answerMergerOutput = currentUtterance.getAnswerMergerOutput();
         if (answerMergerOutput.hasWorkingUtterance())
            workingUtterance = answerMergerOutput.getWorkingUtterance();
      }

      SessionData.Builder sessionDataBuilder = sessionData.toBuilder();
      UtteranceData.Builder currentUtteranceBuilder = currentUtterance.toBuilder();

      List<ErrorSegmentAnnotation> errorSegments = new ArrayList<ErrorSegmentAnnotation>(
            currentUtterance.getErrorSegmentsList());
      final int errorSegmentIndex = selectAndPruneErrorSegments(errorSegments);
      final ErrorSegmentAnnotation errorSegment = errorSegmentIndex == -1 ? null : errorSegments.get(errorSegmentIndex);

      currentUtteranceBuilder.clearErrorSegments();
      currentUtteranceBuilder.addAllErrorSegments(errorSegments);

      // guaranteed to return a branch because of the default state
      DmBranch dmBranch = branchTable.getFirstValidBranch(sessionData, errorSegment);

      // if this is an error we worked on before and could not resolve it
      final boolean repeatQuestion = isQuestionRepeated(sessionData, errorSegment, dmBranch);
      final DmResponseTypeRuleIdPair actionRulePair = repeatQuestion ? dmBranch.getRepeatAction() : dmBranch.getFirstAction();

      DmOutput.Builder dmOutputBuilder = DmOutput.newBuilder();
      dmOutputBuilder.setQgRuleId(generateQgRuleId(dmBranch.getId(), actionRulePair.getRuleId()));

      final String actionType = actionRulePair.getType();
      if (actionType.equals(DM_ACTION_TRANSLATE)) {
         translateWorkingUtterance(dmOutputBuilder, Util.getTranslateSegments(workingUtterance, errorSegments));
      } else if (actionType.equals(DM_ACTION_RESTART)) {
         restartDialog(dmOutputBuilder);
      } else if (actionType.equals(DM_ACTION_MOVE_ON)) {
         moveOn(dmOutputBuilder);
      } else {
         DmResponse dmResponse = responseTable.getResponse(actionType, actionRulePair.getRuleId());
         if (repeatQuestion) {
            DmResponseTypeRuleIdPair preambleRule = new DmResponseTypeRuleIdPair(DM_ACTION_PREAMBLE_SORRY,
                  DM_ACTION_PREAMBLE_SORRY_RULEID);
            DmResponse dmPreambleResponse = responseTable.getResponse(preambleRule);
            dmResponse = dmPreambleResponse.mergeWith(dmResponse);
         }
         clarifyErrorSegment(dmOutputBuilder, errorSegmentIndex, actionType, dmResponse.getTargetedAttribute(),
               dmResponse.getClarifySegments(workingUtterance, errorSegment, errorSegmentIndex));
      }

      currentUtteranceBuilder.setDmOutput(dmOutputBuilder);
      sessionDataBuilder.setUtterances(currentTurn, currentUtteranceBuilder);
      return sessionDataBuilder.build();
   }

   private boolean isQuestionRepeated(SessionData sessionData, ErrorSegmentAnnotation errorSegment, DmBranch dmBranch) {
      if (errorSegment == null || !errorSegment.hasIsResolved())
         return false;
      DmClarificationType previousAction = null;
      int currentTurn = sessionData.getCurrentTurn();
      if (currentTurn > 0) {
         DmOutput prevDmOutput = sessionData.getUtterances(currentTurn - 1).getDmOutput();
         if (prevDmOutput != null && prevDmOutput.getDmAction() == DmActionType.ACTION_CLARIFY_UTTERANCE)
            previousAction = prevDmOutput.getDmClarifyOutput().getType();
      }
      if (previousAction != null) {
         DmClarificationType currentAction = DmResponseTypeRuleIdPair.getDmClarificationEnum(dmBranch.getRepeatAction()
               .getType());
         if (previousAction == currentAction)
            return true;
         return previousAction != DmClarificationType.ACTION_CONFIRM
               && previousAction != DmClarificationType.ACTION_CONFIRM_ATTRIBUTE
               && currentAction != DmClarificationType.ACTION_CONFIRM
               && currentAction != DmClarificationType.ACTION_CONFIRM_ATTRIBUTE;
      }
      return false;
   }

   public static void translateWorkingUtterance(DmOutput.Builder dmOutputBuilder,
         List<DmTranslateSegment> dmTranslateSegments) {
      DmTranslateOutput.Builder dmTranslateOutputBuilder = DmTranslateOutput.newBuilder();
      dmTranslateOutputBuilder.addAllSegments(dmTranslateSegments);
      dmOutputBuilder.setDmTranslateOutput(dmTranslateOutputBuilder);
      dmOutputBuilder.setDmAction(DmActionType.ACTION_TRANSLATE_UTTERANCE);
      dmOutputBuilder.setDmResponse(DmResponseType.RESPONSE_SENTENCE);
   }

   private void restartDialog(DmOutput.Builder dmOutputBuilder) {
      // This seems wrong
      dmOutputBuilder.setDmAction(DmActionType.ACTION_RESTART);
      dmOutputBuilder.setDmResponse(DmResponseType.RESPONSE_SENTENCE);
   }

   private void moveOn(DmOutput.Builder dmOutputBuilder) {
      // This seems wrong
      dmOutputBuilder.setDmAction(DmActionType.ACTION_MOVE_ON);
      dmOutputBuilder.setDmResponse(DmResponseType.RESPONSE_SENTENCE);
   }

   public static void clarifyErrorSegment(DmOutput.Builder dmOutputBuilder, int errorSegmentIndex, String actionType,
         ErrorSegmentAttributeType clarifyAttribute, List<DmClarifySegment> dmClarifySegments) {
      final DmClarificationType clarifyType = DmResponseTypeRuleIdPair.getDmClarificationEnum(actionType);
      final AnswerMergerOperationType clarifyOperation = getExpectedOperation(clarifyType);

      DmClarifyOutput.Builder dmClarifyOutputBuilder = DmClarifyOutput.newBuilder();
      if (errorSegmentIndex >= 0)
         dmClarifyOutputBuilder.setErrorSegmentIndex(errorSegmentIndex);
      dmClarifyOutputBuilder.setType(clarifyType);
      dmClarifyOutputBuilder.setExpectedOperation(clarifyOperation);
      if (clarifyAttribute != null)
         dmClarifyOutputBuilder.setTargetedAttribute(clarifyAttribute);
      dmClarifyOutputBuilder.addAllSegments(dmClarifySegments);

      dmOutputBuilder.setDmClarifyOutput(dmClarifyOutputBuilder);
      dmOutputBuilder.setDmAction(DmActionType.ACTION_CLARIFY_UTTERANCE);
      dmOutputBuilder.setDmResponse(getResponse(clarifyType));
   }

   private static String generateQgRuleId(int branchId, String ruleId) {
      return "Branch=" + branchId + ";" + ruleId;
   }

   private static AnswerMergerOperationType getExpectedOperation(DmClarificationType type) {
      switch (type) {
         case ACTION_CLARIFY:
            return AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP;
         case ACTION_CONFIRM:
            return AnswerMergerOperationType.OPERATION_CONFIRM_UTTERANCE;
         case ACTION_CONFIRM_ATTRIBUTE:
            return AnswerMergerOperationType.OPERATION_CONFIRM_ERROR_SEGMENT_ATTRIBUTE;
         case ACTION_ASK_REPHRASE_PART:
            return AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP;
         case ACTION_REJECT:
            return AnswerMergerOperationType.OPERATION_USE_LAST_RESCORED;
         case ACTION_SPELL:
            return AnswerMergerOperationType.OPERATION_ADD_SPELLING;
         case ACTION_DISAMBIGUATE:
            return AnswerMergerOperationType.OPERATION_CHOOSE_EITHER_OR;
         case ACTION_DISAMBIGUATE_MT:
            return AnswerMergerOperationType.OPERATION_STORE_EITHER_OR;
         case ACTION_ASK_REPEAT_PART:
            return AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP;
      }
      return AnswerMergerOperationType.OPERATION_USE_LAST_RESCORED;
   }

   private static DmResponseType getResponse(DmClarificationType type) {
      switch (type) {
         case ACTION_REJECT:
            return DmResponseType.RESPONSE_SENTENCE;

         case ACTION_CLARIFY:
         case ACTION_ASK_REPHRASE_PART:
         case ACTION_ASK_REPEAT_PART:
            return DmResponseType.RESPONSE_PARTIAL;

         case ACTION_CONFIRM:
         case ACTION_CONFIRM_ATTRIBUTE:
         case ACTION_DISAMBIGUATE:
         case ACTION_DISAMBIGUATE_MT:
            return DmResponseType.RESPONSE_CHOICE;

         case ACTION_SPELL:
            return DmResponseType.RESPONSE_SPELL;
      }
      return DmResponseType.RESPONSE_SENTENCE;
   }

   private double scoreAttribute(BoolAttribute attribute, String key, double priority, double confidence) {
      Double value = dmPriorities.get(key);
      if (value != null)
         priority += value;
      if (attribute.hasConfidence())
         confidence *= attribute.getConfidence();
      return priority * confidence;
   }

   private int selectAndPruneErrorSegments(List<ErrorSegmentAnnotation> errorSegments) {
      if (errorSegments == null || errorSegments.isEmpty())
         return -1;

      final class AnnotationInfo {
         double score;
         boolean isResolved;
         boolean isPersistent;
      }
      final Map<ErrorSegmentAnnotation, AnnotationInfo> infos = new HashMap<ErrorSegmentAnnotation, AnnotationInfo>();

      for (ErrorSegmentAnnotation errorSegment : errorSegments) {
         AnnotationInfo info = new AnnotationInfo();
         info.score = 0;

         info.isResolved = errorSegment.hasIsResolved();
         if (info.isResolved) {
            BoolAttribute attribute = errorSegment.getIsResolved();
            info.isResolved = attribute.hasValue() && attribute.getValue();
         }

         info.isPersistent = true;

         String key = null;
         switch (errorSegment.getErrorType()) {
            case ERROR_SEGMENT_ASR:
               key = "is_asr_error";
               break;
            case ERROR_SEGMENT_DF:
               key = "is_df_error";
               break;
            case ERROR_SEGMENT_MT:
               key = "is_mt_error";
               break;
         }
         Double value = key == null ? null : dmPriorities.get(key);
         double priority = value == null ? 0.0 : value;
         double confidence = errorSegment.hasConfidence() ? errorSegment.getConfidence() : 1.0;

         boolean hasAttribute = false;
         if (errorSegment.hasIsAsrOov()) {
            BoolAttribute attribute = errorSegment.getIsAsrOov();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_asr_oov", priority, confidence);
               hasAttribute = true;
            }
         }
         if (errorSegment.hasIsAsrAmbiguous()) {
            BoolAttribute attribute = errorSegment.getIsAsrAmbiguous();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_asr_ambiguous", priority, confidence);
               info.isPersistent = false;
               hasAttribute = true;
            }
         }
         if (errorSegment.hasIsMtOov()) {
            BoolAttribute attribute = errorSegment.getIsMtOov();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_mt_oov", priority, confidence);
               info.isPersistent = false;
               hasAttribute = true;
            }
         }

         if (errorSegment.hasIsMtAmbiguous()) {
            BoolAttribute attribute = errorSegment.getIsMtAmbiguous();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_mt_ambiguous", priority, confidence);
               info.isPersistent = false;
               hasAttribute = true;
            }
         }
         if (errorSegment.hasIsMtQuestionable()) {
            BoolAttribute attribute = errorSegment.getIsMtQuestionable();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_mt_questionable", priority, confidence);
               info.isPersistent = false;
               hasAttribute = true;
            }
         }
         if (errorSegment.hasIsMtWordDropped()) {
            BoolAttribute attribute = errorSegment.getIsMtWordDropped();
            if (attribute.hasValue() && attribute.getValue()) {
               info.score += scoreAttribute(attribute, "is_mt_word_dropped", priority, confidence);
               info.isPersistent = false;
               hasAttribute = true;
            }
         }

         if (!hasAttribute)
            info.score += priority * confidence;

         infos.put(errorSegment, info);
      }

      if (errorSegments.size() > 1) {
         Collections.sort(errorSegments, new Comparator<ErrorSegmentAnnotation>() {

            public int compare(ErrorSegmentAnnotation errorSegmentA, ErrorSegmentAnnotation errorSegmentB) {
               AnnotationInfo infoA = infos.get(errorSegmentA);
               AnnotationInfo infoB = infos.get(errorSegmentB);
               int cmp = Double.compare(infoB.score, infoA.score);
               if (cmp == 0) {
                  int lenA = errorSegmentA.hasStartIndex() && errorSegmentA.hasEndIndex() ? errorSegmentA.getEndIndex()
                        - errorSegmentA.getStartIndex() : 0;
                  int lenB = errorSegmentB.hasStartIndex() && errorSegmentB.hasEndIndex() ? errorSegmentB.getEndIndex()
                        - errorSegmentB.getStartIndex() : 0;
                  cmp = lenB - lenA;
                  if (cmp == 0 && errorSegmentA.hasStartIndex() && errorSegmentB.hasStartIndex())
                     cmp = errorSegmentA.getStartIndex() - errorSegmentB.getStartIndex();
               }
               return cmp;
            }

         });
      }

      int selectedIndex = -1;
      for (int index = 0; index < errorSegments.size();) {
         ErrorSegmentAnnotation errorSegment = errorSegments.get(index);
         AnnotationInfo info = infos.get(errorSegment);

         if (selectedIndex == -1 && !info.isResolved && info.score > Math.pow(10, -6))
            selectedIndex = index;

         if (selectedIndex != index && !info.isResolved && !info.isPersistent)
            errorSegments.remove(index);
         else
            index++;
      }

      return selectedIndex;
   }

   private static final Logger logger = LoggerFactory.getLogger(DialogManager.class);
}
