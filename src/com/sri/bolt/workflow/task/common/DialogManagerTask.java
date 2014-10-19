package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.audio.AudioSequence;
import com.sri.bolt.audio.Playback;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.DmActionType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.workflow.Util;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogManagerTask implements WorkflowTask {
   public DialogManagerTask(Language lang) {
      this.language = lang;
   }

   @Override
   public WorkflowState call() throws Exception {
      SessionData session = data.getInteractionState().getSessionData();
      com.sri.bolt.ui.Util.addUserMessage(getDialogDisplayString(session), language);
      if (App.getApp().getProps().getProperty("EvalMode").equalsIgnoreCase("false")) {
         String workingUtt = getWorkingUtteranceString(session);
         String translation = getTranslation(session);
         com.sri.bolt.ui.Util.addSystemMessage("<b>Working Utterance:</b> " + workingUtt, language);
         com.sri.bolt.ui.Util.addSystemMessage("<b>Translation:</b> " + translation, language);
      }

      SessionData resultData = App.getApp().getServiceController().DialogManagerProcessData(data.getInteractionState().getSessionData(), language);

      if (resultData == null || resultData.getSerializedSize() == 0)
    	  return null;

      //if we are doing a "move on" use the previous translation
      if (resultData.getUtterances(resultData.getCurrentTurn()).getDmOutput().getDmAction() == DmActionType.ACTION_MOVE_ON) {
         UtteranceData curUtt = resultData.getUtterances(resultData.getCurrentTurn());
         UtteranceData prevUtt = resultData.getUtterances(resultData.getCurrentTurn() - 1);

         curUtt = curUtt.toBuilder().setMtData(prevUtt.getMtData()).build();
         resultData = resultData.toBuilder().setUtterances(resultData.getCurrentTurn(), curUtt).build();
      }

      if (resultData == null || resultData.getSerializedSize() == 0)
         return null;

      data.getInteractionState().setSessionData(resultData);
      return data;
   }

   @Override
   public void setInput(WorkflowState data) {
      this.data = data;
   }

   @Override
   public WorkflowTaskType getType() {
      return language == Language.ENGLISH ? WorkflowTaskType.DIALOG_MANAGER_EN : WorkflowTaskType.DIALOG_MANAGER_IA;
   }

   private String getDialogDisplayString(SessionData input) {
      if (input.getCurrentTurn() == 0) {
         return generateAnnotatedUserUtterance(input);
      } else {
         UtteranceData utt = input.getUtterances(input.getCurrentTurn());
         String retval = utt.getRescored1Best();
         // Strip %hesitation words, reject, etc.
         retval = com.sri.bolt.service.TextProcessing.cleanForDisplay(retval, language);
         if (language == Language.IRAQI_ARABIC) {
            // Convert to Unicode for display.
            retval = BuckwalterUnicodeConverter.buckwalterToUnicode(retval, language);
         }
         return retval;
      }
   }

   private String getTranslation(SessionData input) {
      UtteranceData utt = input.getUtterances(input.getCurrentTurn());
      String translation = utt.getMtData().getPostprocessedTranslations(0);
      return language == Language.ENGLISH ? BuckwalterUnicodeConverter.buckwalterToUnicode(translation, Language.IRAQI_ARABIC) : translation;
   }

   private String getWorkingUtteranceString(SessionData input) {
      return generateAnnotatedUserUtterance(input);
   }

   private String generateAnnotatedUserUtterance(SessionData input) {
      UtteranceData utt = input.getUtterances(input.getCurrentTurn());

      // NOTE: Not yet converted to Unicode if IA; this is so we can still work in Buckwalter for now
      String displayString = utt.getAnswerMergerOutput().getWorkingUtterance();

      // This just replaces ._ with .
      displayString = Util.formatForScreen(displayString);

      if (utt.getErrorSegmentsList().size() == 0) {
         // Strip %hesitation words, reject, etc.
         displayString = com.sri.bolt.service.TextProcessing.cleanForDisplay(displayString, language);

         if (language == Language.IRAQI_ARABIC) {
            displayString = BuckwalterUnicodeConverter.buckwalterToUnicode(displayString, language);
         }

         return displayString;
      }

      String[] origWords = displayString.trim().split("\\s+");
      String[] words = new String[origWords.length];
      for (int i = 0; i < origWords.length; i++) {
         // Update the words for display. This means stripping anything and
         // converting from Buckwalter to Unicode.
         words[i] = com.sri.bolt.service.TextProcessing.cleanForDisplay(origWords[i], language);

         if (language == Language.IRAQI_ARABIC) {
            words[i] = BuckwalterUnicodeConverter.buckwalterToUnicode(words[i], language);
         }
         /* Debug - show when word stripped
         if (words[i].length() == 0) {
            words[i] = "STRIPPED(" + origWords[i] + ")";
         }
         */
      }

      StringBuilder builder = new StringBuilder();

      // the error segments may not be ordered so we need to keep track of which words should be highlighted
      String[] marked = new String[words.length];
      for (int i = 0; i < marked.length; i++)
         marked[i] = "";
      for (BoltMessages.ErrorSegmentAnnotation err : utt.getErrorSegmentsList()) {
         //System.out.println("Error annotated: [" + err.getStartIndex() + "," + err.getEndIndex() + "]; " + err.getErrorType());
         for (int i = err.getStartIndex(); i <= err.getEndIndex(); i++) {
            if (!err.hasIsResolved() || (err.hasIsResolved() && !err.getIsResolved().getValue())) {
               if (err.getErrorType() == BoltMessages.ErrorSegmentType.ERROR_SEGMENT_ASR) {
                  if (err.hasIsAsrOov() && err.getIsAsrOov().getValue()) {
                     marked[i] += "R";
                  } else if (err.hasIsAsrAmbiguous() && err.getIsAsrAmbiguous().getValue()) {
                     marked[i] += "M";
                  } else {
                     marked[i] += "P";
                  }
               } else if (err.getErrorType() == BoltMessages.ErrorSegmentType.ERROR_SEGMENT_MT) {
                  if (err.hasIsMtOov() && err.getIsMtOov().getValue()) {
                     marked[i] += "B";
                  } else if (err.hasIsMtWordDropped() && err.getIsMtWordDropped().getValue()) {
                     marked[i] += "C";
                  } else if (err.hasIsMtAmbiguous() && err.getIsMtAmbiguous().getValue()) {
                     marked[i] += "G";
                  } else if (err.hasIsMtQuestionable() && err.getIsMtQuestionable().getValue()) {
                     marked[i] += "Y";
                  } else {
                     marked[i] += "L";
                  }
               }
            }
         }
      }
      for (int i = 0; i < marked.length; i++) {
         if (marked[i].equals("")) {
            continue;
         } else if (marked[i].equals("R")) {
            marked[i] = "#FF0000"; //red
         } else if (marked[i].equals("M")) {
            marked[i] = "#800000"; //brown
         } else if (marked[i].equals("P")) {
            marked[i] = "#FF00FF"; // pink
         } else if (marked[i].equals("B")) {
            marked[i] = "#0000FF";  //blue
         } else if (marked[i].equals("C")) {
            marked[i] = "#00FFFF"; // cyan
         } else if (marked[i].equals("G")) {
            marked[i] = "#008000";  //green
         } else if (marked[i].equals("Y")) {
            marked[i] = "#FFFF00";  //yellow
         } else if (marked[i].equals("L")) {
            marked[i] = "#808000"; // olive
         } else if (marked[i].length() > 1) { // both ASR and MT error
            marked[i] = "#800080"; // purple
         } else {
            // this should never happen; here just as sanity check
            marked[i] = "#FFFFFF"; //white
         }

      }
      int lastWord = 0;
      while (lastWord < words.length) {
         if (!marked[lastWord].equals("")) {
            String color = marked[lastWord];
            // find the next word that is not marked up
            int count = lastWord + 1;
            while (count < words.length && !marked[count].equals("") && marked[count].equals(color)) {
               count++;
            }
            builder.append("<font color=\"" + color + "\"><b><i>");
            for (int i = lastWord; i < count; i++) {
               builder.append(words[i] + " ");
            }
            builder.append("</i></b></font>");
            lastWord = count;
         } else {
            builder.append(words[lastWord] + " ");
            lastWord++;
         }
      }
      return builder.toString();
   }

   @Override
   public boolean validate() {
      return true;
   }

   private static final Logger logger = LoggerFactory.getLogger(DialogManagerTask.class);
   private Language language;
   private WorkflowState data;
}
