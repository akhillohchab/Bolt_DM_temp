package com.sri.bolt.dm;

import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.DmClarifySegmentActionType;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.StringAttribute;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;

import java.util.List;

public class DmSegment {

   private static final int MAX_INDEX = 9999;
   private static final int MIN_REPEAT_PHRASE_LENGTH = 3;

   private String text;
   private DmSegmentTypeEnum action;
   private TtsFeatureEnum ttsFeature;
   private String prefix;
   private String suffix;
   private String connector;
   private Language language;

   public DmSegment(String text, String action, String ttsFeature, String prefix, String suffix, String connector,
                    Language lang) {

      this.action = DmSegmentTypeEnum.fromString(action);
      if (ttsFeature != null)
         this.ttsFeature = TtsFeatureEnum.fromString(ttsFeature);

      language = lang;
      if (text != null) {
         this.text = language == Language.IRAQI_ARABIC ? BuckwalterUnicodeConverter.unicodeToBuckwalter(text,
                 language, false) : text;
      }
      if (prefix != null) {
         this.prefix = language == Language.IRAQI_ARABIC ? BuckwalterUnicodeConverter.unicodeToBuckwalter(prefix,
                 language, false) : prefix;
      }
      if (suffix != null) {
         this.suffix = language == Language.IRAQI_ARABIC ? BuckwalterUnicodeConverter.unicodeToBuckwalter(suffix,
                 language, false) : suffix;
      }
      if (connector != null) {
         this.connector = language == Language.IRAQI_ARABIC ? BuckwalterUnicodeConverter.unicodeToBuckwalter(
                 connector, language, false) : connector;
      }

   }

   public String getText() {
      return text;
   }

   public DmSegmentTypeEnum getAction() {
      return action;
   }

   public TtsFeatureEnum getTtsFeature() {
      return ttsFeature;
   }

   public String toString() {
      String result = "";
      if (action == DmSegmentTypeEnum.DISPLAY_STRING)
         result += text;
      else
         result += "<" + action + ">";
      if (ttsFeature != null)
         result = "<em>" + result + "</em>";
      return result;
   }

   public DmClarifySegment convertToDmClarifySegment(String workingUtterance, ErrorSegmentAnnotation errorSegment,
                                                     int errorSegmentIndex) {
      DmClarifySegment.Builder dmClarifySegmentBuilder = DmClarifySegment.newBuilder();
      if (action == DmSegmentTypeEnum.PLAY_AUDIO || action == DmSegmentTypeEnum.PLAY_SPELLING) {
         dmClarifySegmentBuilder.setAction(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT);
         dmClarifySegmentBuilder.setErrorSegmentIndex(errorSegmentIndex);
      } else {
         String ttsInput = "";
         String ttsInputWithMarkup = "";
         if (action == DmSegmentTypeEnum.DISPLAY_STRING) {
            ttsInput = text;
            ttsInputWithMarkup = text;
         } else if (action == DmSegmentTypeEnum.DISPLAY_SENTENCE) {
            if (language == Language.IRAQI_ARABIC) {
               ttsInput = "\" " + workingUtterance.trim() + " \"";
            } else {
               ttsInput = "\"" + workingUtterance.trim() + "\"";
            }
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_HEADWORD) {
            int depWordIndex = errorSegment.getDepWordIndex().getValue();
            ttsInput = getWords(workingUtterance, depWordIndex, depWordIndex);
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_ERROR_WORDS) {
            ttsInput = getWords(workingUtterance, errorSegment.getStartIndex(), errorSegment.getEndIndex());
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_NEXT) {
            ttsInput = getWords(workingUtterance, errorSegment.getEndIndex() + 1, MAX_INDEX);
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_PREVIOUS) {
            ttsInput = getWords(workingUtterance, 0, errorSegment.getStartIndex() - 1);
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_PREVIOUS_SHORTER) {
            int depWordIndex = errorSegment.getDepWordIndex().getValue();
            int start = 0;
            if (depWordIndex >= 0 && depWordIndex < errorSegment.getStartIndex()) {
               start = depWordIndex;
               if (start > 0 && errorSegment.getStartIndex() - depWordIndex < MIN_REPEAT_PHRASE_LENGTH) {
                  start = errorSegment.getStartIndex() - MIN_REPEAT_PHRASE_LENGTH;
                  if (start < 0)
                     start = 0;
               }
            }
            ttsInput = getWords(workingUtterance, start, errorSegment.getStartIndex() - 1);
            ttsInputWithMarkup = ttsInput;
         } else if (action == DmSegmentTypeEnum.DISPLAY_AMBIGUOUS_WORDS_SPELLING
                 || action == DmSegmentTypeEnum.DISPLAY_OPTIONS
		 || action == DmSegmentTypeEnum.DISPLAY_AMBIGUOUS_WORDS_DESCRIPTION) {
            ttsInput = getAmbiguousWordString(errorSegment, errorSegmentIndex, false);
            ttsInputWithMarkup = getAmbiguousWordString(errorSegment, errorSegmentIndex, true);
         } else if (action == DmSegmentTypeEnum.DISPLAY_SPELLING) {
            if (errorSegment.hasSpelling()) {
               String word = normalizeSpelling(errorSegment.getSpelling().getValue());
               ttsInput = word;
               ttsInputWithMarkup = TtsFeatureEnum.getSpellingMarkup(word);
            } else {
               String word = normalizeSpelling(getWords(workingUtterance, errorSegment.getStartIndex(),
                       errorSegment.getEndIndex()));
               ttsInput = word;
               ttsInputWithMarkup = TtsFeatureEnum.getSpellingMarkup(word);
            }
         }

         if (ttsInput != null && !ttsInput.isEmpty() && ttsFeature != null) {
            ttsInputWithMarkup = TtsFeatureEnum.getTtsString(ttsFeature, ttsInputWithMarkup);
         }
         dmClarifySegmentBuilder.setAction(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT);
         dmClarifySegmentBuilder.setTtsInput(ttsInput);
         dmClarifySegmentBuilder.setTtsInputWithMarkup(ttsInputWithMarkup);
      }
      return dmClarifySegmentBuilder.build();
   }

   private static String normalizeSpelling(String spelling) {
      return spelling.replaceAll("\\.\\_", "").replaceAll("\\.\\-", "-");
   }

   private static String getWords(String workingUtterance, int startIndex, int endIndex) {
      String result = "";
      String[] words = workingUtterance.trim().split("\\s+");
      for (int i = startIndex; i >= 0 && i < words.length && i <= endIndex; i++)
         result += words[i] + " ";
      return result.trim();
   }

   private String getAmbiguousWordString(ErrorSegmentAnnotation errorSegment,
                                         int errorSegmentIndex, boolean withMarkup) {
      int cnt = errorSegment.getAmbiguousWordsCount();
      List<StringAttribute> ambWords = errorSegment.getAmbiguousWordsList();
      List<StringAttribute> explanations = errorSegment.getAmbiguousWordExplanationsList();
      String result = "";
      for (int i = 0; i < cnt; i++) {
         if (ambWords.get(i) == null)
            continue;
         String word = ambWords.get(i).getValue();
	 int pos = word.lastIndexOf(":");
	 if (pos != -1)
	     word = word.substring(0, pos);
         if (i > 0) {
            if (connector != null) {
               result += " " + connector;
            }
            result += " ";
         }
         if (action == DmSegmentTypeEnum.DISPLAY_AMBIGUOUS_WORDS_SPELLING) {
            if (withMarkup)
               result += TtsFeatureEnum.getSpellingMarkup(normalizeSpelling(word));
            else
               result += normalizeSpelling(word);
         } else if (action == DmSegmentTypeEnum.DISPLAY_OPTIONS) {
            result += (i + 1);
         } else if (action == DmSegmentTypeEnum.DISPLAY_AMBIGUOUS_WORDS_DESCRIPTION) {
	    result += word;
            if (i < explanations.size() && explanations.get(i) != null) {
               if (prefix != null)
                  result += " " + prefix + " ";
               result += explanations.get(i).getValue();
               if (suffix != null)
                  result += " " + suffix;
	    }
	 }
      }
      return result;
   }

}
