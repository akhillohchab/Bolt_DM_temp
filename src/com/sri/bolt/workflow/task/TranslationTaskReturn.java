package com.sri.bolt.workflow.task;

import java.util.ArrayList;
import java.util.List;

public class TranslationTaskReturn {
   public static final String ALIGNMENT_DELIMETER = " ||| ";
   private static final String SENTENCE_BEGIN = "<s>";
   private static final String SENTENCE_END = "</s>";
   private static final String ALIGNMENT_PAIR_DELIMETER = ",";
   private static final String ALIGNMENT_WORD_DELIMETER = "-";

   public TranslationTaskReturn() {
      this.success = true;
   }

   public TranslationTaskReturn(boolean success, String resultText, String preprocessedInput, String originalTranslation, String alignment, ArrayList<String> nbest) {
      this.success = success;
      this.resultText = resultText;
      this.preprocessedInput = cleanSentence(preprocessedInput);
      this.originalTranslation = cleanSentence(originalTranslation);
      this.alignment = updateAlignments(originalTranslation, alignment);
   }

   private static String cleanSentence(String text) {
      String result = new String(text).trim();
      result = result.replaceAll(SENTENCE_BEGIN, "");
      result = result.replaceAll(SENTENCE_END, "");
      // MWF: Updated on 6/13/2013 so output will be
      //  $ANY { ANYTHING ELSE }
      // instead of
      //  $ANY{ANYTHING ELSE}
      // See TBBC-96 which requires $name { translit text }
      // Normalize whitespace for these markers.
      result = result.replaceAll("\\$(name|number|hour|date|eng|url|email|acronym)\\s*\\{\\s*(.*?)\\s*\\}", "\\$$1 { $2 }");
      return result.trim();
   }

   private static String updateAlignments(String text, String alignment) {
      if (alignment == null || alignment.isEmpty())
         return null;
      boolean sentenceBeginExists = false;
      boolean sentenceEndExists = false;
      if (text.indexOf(SENTENCE_BEGIN) != -1)
         sentenceBeginExists = true;
      if (text.indexOf(SENTENCE_END) != -1)
         sentenceEndExists = true;

      if (!sentenceBeginExists && !sentenceEndExists)
         return alignment;
      String[] pairs = alignment.trim().split(ALIGNMENT_PAIR_DELIMETER);
      List<String> newAlignments = new ArrayList<String>();
      for (int i = 0; i < pairs.length; i++) {
         // skip the last pair if sentence end exists
         if (sentenceEndExists && i == pairs.length - 1)
            continue;
         String pair = pairs[i];
         String[] indices = pair.split(ALIGNMENT_WORD_DELIMETER);
         int srcIndex = Integer.parseInt(indices[0]);
         int tgtIndex = Integer.parseInt(indices[1]);
         if (sentenceBeginExists) {
            srcIndex--;
            tgtIndex--;
         }
         if (srcIndex >= 0 && tgtIndex >= 0)
            newAlignments.add(srcIndex + ALIGNMENT_WORD_DELIMETER + tgtIndex);
      }
      String result = "";
      for (int i = 0; i < newAlignments.size(); i++) {
         if (i > 0)
            result += ALIGNMENT_PAIR_DELIMETER;
         result += newAlignments.get(i);
      }
      return result.trim();
   }

   public String getPreprocessedInput() {
      return preprocessedInput;
   }

   public void setPreprocessedInput(String preprocessedInput) {
      this.preprocessedInput = cleanSentence(preprocessedInput);
   }

   public String getOriginalTranslation() {
      return originalTranslation;
   }

   public void setOriginalTranslation(String originalTranslation) {
      this.originalTranslation = cleanSentence(originalTranslation);
   }

   public String getAlignment() {
      return alignment;
   }

   public void setAlignment(String alignment) {
      this.alignment = updateAlignments(originalTranslation, alignment);
   }

   public boolean isSuccess() {
      return success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public String getResultText() {
      return resultText;
   }

   public void setResultText(String resultText) {
      this.resultText = resultText;
   }

   public List<String> getnBest() {
      return nBest;
   }

   public void setnBest(List<String> nBests) {
      this.nBest = nBests;
   }

   private List<String> nBest;
   private String preprocessedInput;
   private String originalTranslation;
   private String alignment;
   private boolean success;
   private String resultText;
}
