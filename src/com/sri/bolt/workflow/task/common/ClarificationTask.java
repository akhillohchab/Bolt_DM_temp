package com.sri.bolt.workflow.task.common;

import com.sri.bolt.App;
import com.sri.bolt.audio.AudioSequence;
import com.sri.bolt.audio.Playback;
import com.sri.bolt.message.BoltMessages.DmClarifyOutput;
import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.DmClarifySegmentActionType;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.WordAnnotation;
import com.sri.bolt.message.BoltMessages.WordIndexPointer;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.workflow.WorkflowState;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.workflow.task.WorkflowTask;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

public class ClarificationTask implements WorkflowTask {

   @Override
   public WorkflowState call() throws Exception {
      SessionData currentData = data.getInteractionState().getSessionData();
      UtteranceData currentUtterance = currentData.getUtterances(currentData.getCurrentTurn());

      data.getInteractionState().startClarificationTurn();

      StringBuffer systemResponse = new StringBuffer();
      AudioSequence sequence = new AudioSequence();
      DmClarifyOutput dmClarify = currentUtterance.getDmOutput().getDmClarifyOutput();
      for (DmClarifySegment segment : dmClarify.getSegmentsList()) {
         if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT)) {
            sequence.addFileData(new File(Playback.doTTS(data.getInteractionState().getLanguage() == Language.ENGLISH ? segment.getTtsInputWithMarkup() : segment.getTtsInput()
                    , data.getInteractionState().getLanguage())));
            systemResponse.append(segment.getTtsInput());
         } else if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT)) {
            if (data.getInteractionState().getLanguage() == Language.IRAQI_ARABIC) {
               systemResponse.append("[ klAm Almstxdm ]");
            } else {
               systemResponse.append("[USER SPEECH]");
            }
            ErrorSegmentAnnotation errorSeg = currentUtterance.getErrorSegments(segment.getErrorSegmentIndex());
            int startNdx = errorSeg.getStartIndex();
            int endNdx = errorSeg.getEndIndex();
            // sanity check: swap start and end indices if start index is greater than end index
            if (startNdx > endNdx) {
               int tmp = startNdx;
               startNdx = endNdx;
               endNdx = tmp;
            }
            // get the right words to be played using word_index_pointer array
            // note that the error segment might come from multiple utterances
            // for each word in the error segment, we need to find the corresponding utt and word index and play each one after another

            List<WordIndexPointer> wordIndexPointers = currentUtterance.getAnswerMergerOutput().getWordIndexPointersList();
            int prevWordUttIndex = wordIndexPointers.get(startNdx).getUtteranceIndex();
            int prevWordStartIndex = wordIndexPointers.get(startNdx).getWordIndex();
            int prevWordEndIndex = prevWordStartIndex;
            for (int i = startNdx; i <= endNdx; i++) {
               // skip invalid utterance indices
               if (i < 0 || i >= wordIndexPointers.size())
                  continue;
               int wordUttIndex = wordIndexPointers.get(i).getUtteranceIndex();
               if (wordUttIndex < 0 || wordUttIndex > currentData.getCurrentTurn())
                  continue;
               if (wordUttIndex != prevWordUttIndex) {
                  if (prevWordUttIndex >= 0)
                     addByteDataBySeconds(sequence, currentData.getUtterances(prevWordUttIndex), prevWordUttIndex, prevWordStartIndex, prevWordEndIndex);
                  prevWordUttIndex = wordUttIndex;
                  prevWordStartIndex = wordIndexPointers.get(i).getWordIndex();
                  prevWordEndIndex = prevWordStartIndex;
               } else {
                  prevWordEndIndex = wordIndexPointers.get(i).getWordIndex();
               }
            }
            if (prevWordUttIndex >= 0)
               addByteDataBySeconds(sequence, currentData.getUtterances(prevWordUttIndex), prevWordUttIndex, prevWordStartIndex, prevWordEndIndex);
         }
         systemResponse.append(" ");
         sequence.addBuffer(250);
      }

      InteractionState state = data.getInteractionState();
      data.getInteractionState().setTTSAudio(sequence.getBytes());
      Playback.playAudioFile(state.getLastSystemCommand().getPath(), data.getInteractionState().getLanguage());
      state.setSystemResponseText(systemResponse.toString());
      logger.debug("DM Response: " + systemResponse.toString());
      com.sri.bolt.ui.Util.addSystemMessage(data.getInteractionState().getLanguage() == Language.ENGLISH ? systemResponse.toString() :
              BuckwalterUnicodeConverter.buckwalterToUnicode(systemResponse.toString(), Language.IRAQI_ARABIC), data.getInteractionState().getLanguage());

      return data;
   }

   private void addByteDataBySeconds(AudioSequence sequence, UtteranceData utterance, int uttIndex, int startNdx, int endNdx) {
      if (utterance != null && startNdx < utterance.getWordLevelAnnotationsCount() && endNdx < utterance.getWordLevelAnnotationsCount()) {
         InteractionState state = data.getInteractionState();
         WordAnnotation startWord = utterance.getWordLevelAnnotations(startNdx);
         WordAnnotation endWord = utterance.getWordLevelAnnotations(endNdx);

         ByteArrayOutputStream audio = state.getUtteranceAudio(uttIndex);
         sequence.addByteDataBySeconds(audio.toByteArray(), startWord.getStartOffsetSeconds(), endWord.getEndOffsetSeconds());
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
      return WorkflowTaskType.CLARIFICATION;
   }


   private static final Logger logger = LoggerFactory.getLogger(ClarificationTask.class);
   WorkflowState data;
}
