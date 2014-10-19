package com.sri.bolt.dm;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.DmTranslateSegment;
import com.sri.bolt.message.BoltMessages.DmTranslateSegmentActionType;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.UserFeedbackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Util {

   public static List<DmTranslateSegment> getTranslateSegments(String workingUtterance,
                                                               List<ErrorSegmentAnnotation> errorSegments) {
      List<DmTranslateSegment> results = new ArrayList<DmTranslateSegment>();
      String[] words = workingUtterance.trim().split("\\s+");
      int[] spellingIndices = new int[words.length];
      final int NO_ERROR_SEG_OR_NAME = -1;
      final int NAME = -2;

      List<String> names = readNamesFile();
      List<String> vocab = readMtVocabFile();

      for (int i = 0; i < spellingIndices.length; i++) {
         if (names.contains(words[i])) {
            spellingIndices[i] = NAME;
         } else {
            spellingIndices[i] = NO_ERROR_SEG_OR_NAME;
         }
      }
      for (int j = 0; j < errorSegments.size(); j++) {
         ErrorSegmentAnnotation errorSegment = errorSegments.get(j);
         for (int i = errorSegment.getStartIndex(); i <= errorSegment.getEndIndex(); i++) {
            if (errorSegment.hasIsResolved() && errorSegment.hasSpelling()) {
               spellingIndices[i] = j;
            }
         }
      }

      int index = 0;
      while (index < words.length) {
         int curValue = spellingIndices[index];
         if (curValue == NAME) {
            results.add(createTransliterateSegmentForName(words[index]));
            ++index;
         } else {
            int endIndex = index + 1;
            while (endIndex < spellingIndices.length && spellingIndices[endIndex] == curValue) {
               ++endIndex;
            }
            if (curValue == NO_ERROR_SEG_OR_NAME) {
               List<String> transWords = new ArrayList<String>();
               for (int count = index; count < endIndex; ++count) {
                  transWords.add(words[count]);
               }
               results.add(createTranslateSegment(transWords));
            } else {
               ErrorSegmentAnnotation errorSegment = errorSegments.get(curValue);
               String[] split = errorSegment.getSpelling().getValue().split(" ");
               for (int count = 0; count < split.length; ++count) {
                  String wordNoMarkup = split[count].replaceAll("\\_", "").replaceAll("\\.", "");
                  boolean isSpelling = !errorSegment.getSpelling().hasUserFeedback() || (errorSegment.getSpelling().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER);
                  boolean isName = errorSegment.hasNeTag() && (!errorSegment.getNeTag().hasUserFeedback() || (errorSegment.getNeTag().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER));

                  if (vocab.contains(wordNoMarkup)) {
                     results.add(createTranslateSegment(wordNoMarkup));
                  } else if (isSpelling || isName) {
                     results.add(createTransliterateSegmentForError(wordNoMarkup, curValue));
                  } else {
                     results.add(createTranslateSegment(wordNoMarkup));
                  }
               }
            }
            index = endIndex;
         }
      }

      return results;
   }

   private static DmTranslateSegment createTranslateSegment(List<String> words) {
      DmTranslateSegment.Builder dmTranslateSegmentBuilder = DmTranslateSegment.newBuilder();
      String mtInput = "";
      for (int i = 0; i < words.size(); i++) {
         mtInput += words.get(i) + " ";
      }
      mtInput = mtInput.trim();
      dmTranslateSegmentBuilder.setAction(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT);
      dmTranslateSegmentBuilder.setMtInput(mtInput);

      return dmTranslateSegmentBuilder.build();
   }

   private static DmTranslateSegment createTranslateSegment(String word) {
      DmTranslateSegment.Builder dmTranslateSegmentBuilder = DmTranslateSegment.newBuilder();
      dmTranslateSegmentBuilder.setAction(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT);
      dmTranslateSegmentBuilder.setMtInput(word);

      return dmTranslateSegmentBuilder.build();
   }

   private static DmTranslateSegment createTransliterateSegmentForError(String word, int errorSegmentIndex) {
      DmTranslateSegment.Builder dmTranslateSegmentBuilder = DmTranslateSegment.newBuilder();
      dmTranslateSegmentBuilder.setAction(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT);
      dmTranslateSegmentBuilder.setErrorSegmentIndex(errorSegmentIndex);
      StringBuilder wordWhiteSpace = new StringBuilder();
      for (int count = 0; count < word.length(); ++count) {
         wordWhiteSpace.append(word.charAt(count));
         wordWhiteSpace.append(" ");
      }
      dmTranslateSegmentBuilder.setMtInput(word);
      return dmTranslateSegmentBuilder.build();
   }

   private static DmTranslateSegment createTransliterateSegmentForName(String name) {
      DmTranslateSegment.Builder dmTranslateSegmentBuilder = DmTranslateSegment.newBuilder();
      dmTranslateSegmentBuilder.setAction(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT);
      dmTranslateSegmentBuilder.setErrorSegmentIndex(-1);
      StringBuilder nameWhiteSpace = new StringBuilder();
      for (int count = 0; count < name.length(); ++count) {
         nameWhiteSpace.append(name.charAt(count));
         nameWhiteSpace.append(" ");
      }
      dmTranslateSegmentBuilder.setMtInput(nameWhiteSpace.toString().trim());
      return dmTranslateSegmentBuilder.build();
   }

   public static boolean isRestartString(String string) {
      return matchStringToFile(string.trim(), App.getApp().getProps().getProperty("DmRestartFile", ""));
   }

   public static boolean isMoveOnString(String string) {
      return matchStringToFile(string.trim(), App.getApp().getProps().getProperty("DmMoveOnFile", ""));
   }

   private static boolean matchStringToFile(String string, String file) {
      boolean result = false;

      if (!file.equals("")) {
         try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = reader.readLine()) != null) {
               if (string.equalsIgnoreCase(line.trim())) {
                  result = true;
                  break;
               }
            }
            reader.close();
         } catch (FileNotFoundException e) {
            logger.error("Error in matchStringToFile", e);
         } catch (IOException e) {
            logger.error("Error in matchStringToFile", e);
         }
      }

      return result;
   }

   private static List<String> readNamesFile() {
      if (names == null) {
         names = readFile(App.getApp().getProps().getProperty("EnNames", ""));
      }
      return names;
   }

   private static List<String> readMtVocabFile() {
      if (mtVocab == null) {
         mtVocab = readFile(App.getApp().getProps().getProperty("EnMtVocab", ""));
      }
      return mtVocab;
   }

   private static List<String> readFile(String file) {
      List<String> lines = new ArrayList<String>();
      BufferedReader reader;
      if (!file.equals("")) {
         try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = reader.readLine()) != null) {
               lines.add(line.trim());
            }
            reader.close();
         } catch (FileNotFoundException e) {
            logger.error("Error in reading " + file, e);
         } catch (IOException e) {
            logger.error("Error in reading " + file, e);
         }
      }

      return lines;
   }

   private static List<String> mtVocab = null;
   private static List<String> names = null;
   private static final Logger logger = LoggerFactory.getLogger(Util.class);
}
