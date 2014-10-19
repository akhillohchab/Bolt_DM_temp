package com.sri.bolt.homophone;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.sri.bolt.message.BoltMessages.BoolAttribute;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.StringAttribute;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;

public class HomophoneDetector {
   private static final double DEFAULT_HOMOPHONE_CONFIDENCE = 0.1;
   private static final String NULL_EXP_STRING = "NULL";
   public HomophoneDetector(String homophonefilePath, Language language) {
      this.language = language;
      try {
         homophoneMap = new HashMap<String, String[]>();
         explanationMap = new HashMap<String, String[]>();
         BufferedReader reader;
         reader = new BufferedReader(new FileReader(homophonefilePath));
         String line;
         while ((line = reader.readLine()) != null) {
         if (line.indexOf(NULL_EXP_STRING) != -1)
            continue;
            String[] entries = line.split(" # ");
            String[] homophones = new String[entries.length];
            String[] explanations = new String[entries.length];
            for (int i=0; i < entries.length; i ++) {
               String wordAndExp = entries[i];
               int colonIndex = wordAndExp.indexOf(":");
               int firstQuoteIndex = wordAndExp.indexOf("\"");
               int lastQuoteIndex = wordAndExp.lastIndexOf("\"");
               String word = wordAndExp.substring(0, colonIndex).trim();
               String exp = wordAndExp.substring(firstQuoteIndex+1, lastQuoteIndex).trim();
               homophones[i] = word;
               explanations[i] = exp;
            }
            for (int i=0; i < entries.length; i ++) {
               homophoneMap.put(homophones[i], homophones);
               explanationMap.put(homophones[i], explanations);
            }
         }
         reader.close();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public SessionData processHomophones(SessionData data) {

      SessionData.Builder sessionBuilder = data.toBuilder();
      UtteranceData.Builder currentUtterance = sessionBuilder.getUtterancesBuilder(data.getCurrentTurn());
      String[] workingUtterance = currentUtterance.getAnswerMergerOutput().getWorkingUtterance().split("\\s+");
      for (int wordCount = 0; wordCount < workingUtterance.length; ++wordCount) {
         String word = workingUtterance[wordCount];
         if (homophoneMap.containsKey(word)) {
            boolean overlapsOtherErrorSegment = false;
            for (ErrorSegmentAnnotation errorSeg : currentUtterance.getErrorSegmentsList()) {
               // skip if there is already an unresolved ASR error on the same word
                if (wordCount >= errorSeg.getStartIndex() && wordCount <= errorSeg.getEndIndex() && 
                		errorSeg.getErrorType() == ErrorSegmentType.ERROR_SEGMENT_ASR &&
                		((errorSeg.getIsResolved() != null && !errorSeg.getIsResolved().getValue()) || 
                		(errorSeg.hasIsAsrAmbiguous() && errorSeg.getIsAsrAmbiguous().getValue())) ) {
                	overlapsOtherErrorSegment = true;
               }
            }
            if (!overlapsOtherErrorSegment) {
               ErrorSegmentAnnotation.Builder error = ErrorSegmentAnnotation.newBuilder();
               error.setStartIndex(wordCount);
               error.setEndIndex(wordCount);
               error.setErrorType(ErrorSegmentType.ERROR_SEGMENT_ASR);
               error.setConfidence(DEFAULT_HOMOPHONE_CONFIDENCE);
               BoolAttribute.Builder isAsrAmbiguous = BoolAttribute.newBuilder();
               isAsrAmbiguous.setValue(true);
               isAsrAmbiguous.setConfidence(DEFAULT_HOMOPHONE_CONFIDENCE);
               error.setIsAsrAmbiguous(isAsrAmbiguous);
               String[] homophones = homophoneMap.get(word);
               for (String homophone : homophones) {
                  StringAttribute.Builder ambiguousWord = StringAttribute.newBuilder();
                  ambiguousWord.setValue(homophone);
                  ambiguousWord.setConfidence(DEFAULT_HOMOPHONE_CONFIDENCE);
                  error.addAmbiguousWords(ambiguousWord);
               }
               String[] explanations = explanationMap.get(word);
               for (String explanation : explanations) {
                   StringAttribute.Builder ambiguousExp = StringAttribute.newBuilder();
                   ambiguousExp.setValue(explanation);
                   ambiguousExp.setConfidence(DEFAULT_HOMOPHONE_CONFIDENCE);
                   error.addAmbiguousWordExplanations(ambiguousExp);
                }

               boolean resolvedSameError = false;
               for (UtteranceData utt : data.getUtterancesList()) {
                  for (ErrorSegmentAnnotation errorSeg : utt.getErrorSegmentsList()) {
                     if (errorSeg.hasIsResolved() && errorSeg.getIsResolved().getValue() &&
                             errorSeg.hasErrorType() && errorSeg.getErrorType() == ErrorSegmentType.ERROR_SEGMENT_ASR &&
                             errorSeg.hasConfidence() && errorSeg.getConfidence() == DEFAULT_HOMOPHONE_CONFIDENCE &&
                             errorSeg.hasStartIndex() && errorSeg.getStartIndex() == error.getStartIndex() &&
                             errorSeg.hasEndIndex() && errorSeg.getEndIndex() == error.getEndIndex() &&
                             errorSeg.hasIsAsrAmbiguous() && errorSeg.getIsAsrAmbiguous().getValue() &&
                             errorSeg.getAmbiguousWordsCount() == error.getAmbiguousWordsCount()) {
                        for (int count = 0; count < errorSeg.getAmbiguousWordsCount(); ++ count) {
                           if (!errorSeg.getAmbiguousWordsList().get(count).getValue().equals(error.getAmbiguousWordsList().get(count).getValue())) {
                              continue;
                           }
                        }
                        resolvedSameError = true;
                        break;
                     }
                  }
               }

               if (!resolvedSameError) {
                  currentUtterance.addErrorSegments(error);
               }
            }
         }
      }

      return sessionBuilder.build();
   }

   private Language language;
   private Map<String, String[]> homophoneMap;
   private Map<String, String[]> explanationMap;
}
