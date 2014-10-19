package com.sri.bolt.message;

import com.sri.bolt.message.BoltMessages.DoubleAttribute;
import com.sri.bolt.message.BoltMessages.StringAttribute;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.WordAnnotation;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;
import com.sri.recognizer.message.RecognizerMessages.FrameValues;
import com.sri.recognizer.message.RecognizerMessages.SegmentValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience routines for converting to BOLT messages.
 *
 * @author frandsen
 */
public class RecognizerToBoltMessage {
   // XXX Change to enum
   public static final int MIDDLE_FRAME = 0;
   public static final int START_FRAME = 1;
   public static final int END_FRAME = 2;

   /**
    * Pull all available fields from CombinedRecognizerResult to UtteranceData.
    * @param r Input message from recognizer.
    * @return new UtteranceData.
   public static UtteranceData toUtteranceDataAll(CombinedRecognizerResult r) {
   UtteranceData.Builder u = UtteranceData.newBuilder();
   setCombinedRecognizerResultFieldsAll(u, r);
   return u.build();
   }
    */

   /**
    * Pull select fields from CombinedRecognizerResult to UtteranceData.
    * 1-best, wcn, and lattice.
    *
    * @param r Input message from recognizer.
    * @return new UtteranceData.
    */
   public static UtteranceData toUtteranceDataInitialPass(CombinedRecognizerResult primaryRecogResult, CombinedRecognizerResult secondaryRecogResult) {
      UtteranceData.Builder u = UtteranceData.newBuilder();
      setCombinedRecognizerResultFieldsInitialPass(u, primaryRecogResult, secondaryRecogResult);
      //App.getApp().getLogger().debug("CombinedRecognizerResult\n" + r);
      return u.build();
   }

   /**
    * Pull select fields from CombinedRecognizerResult to UtteranceData.
    * These are the per-word values such as start/end times and
    * prosody information.
    * @param r Input message from recognizer.
    * @return new UtteranceData.
   public static UtteranceData toUtteranceDataForcedAlignmentPass(CombinedRecognizerResult r) {
   UtteranceData.Builder u = UtteranceData.newBuilder();
   setCombinedRecognizerResultFieldsForcedAlignmentPass(u, r);
   return u.build();
   }
    */

   /**
    * Copy info out of CombinedRecognizerResult into UtteranceData. Existing
    * fields are preserved (that is, we add to the existing message).
    * Any fields we don't set that are expected to be set by us will be
    * cleared to prevent stale values.
    * @param u Output UtteranceData message.
    * @param r Input CombinedRecognizerResult message.
   public static void setCombinedRecognizerResultFieldsAll(UtteranceData.Builder u, CombinedRecognizerResult r) {
   setCombinedRecognizerResultFieldsInitialPass(u, r);
   setCombinedRecognizerResultFieldsForcedAlignmentPass(u, r);
   }
    */

   /**
    * Updates fields for pass1 only.
    */
   public static void setCombinedRecognizerResultFieldsInitialPass(UtteranceData.Builder u, CombinedRecognizerResult primaryRecogResult, CombinedRecognizerResult secondaryRecogResult) {
      String simpleResult = "";
      if (primaryRecogResult.hasText()) {
         simpleResult = primaryRecogResult.getText().trim();
         u.setRecognizer1Best(simpleResult);
      } else {
         u.clearRecognizer1Best();
      }

      if (secondaryRecogResult != null && secondaryRecogResult.hasText()) {
         simpleResult = secondaryRecogResult.getText().trim();
         u.setRecognizer1BestSecondary(simpleResult);
      } else {
         u.clearRecognizer1BestSecondary();
      }

      if (secondaryRecogResult != null && secondaryRecogResult.hasConfusionNetwork()) {
         u.setRecognizerWcnSecondary(secondaryRecogResult.getConfusionNetwork());
      }

      if (primaryRecogResult.hasConfusionNetwork()) {
         u.setRecognizerWcn(primaryRecogResult.getConfusionNetwork());
      } else {
         // Fake one
         if (simpleResult.length() > 0) {
            // Form:
            // numaligns 3
            // posterior 1
            // align 0 <s> 1
            // align 1 yeah 1
            // align 2 </s> 1
            String[] words = simpleResult.split("\\s+");
            String out = "numaligns " + (2 + words.length) + "\n" +
                    "align 0 <s> 1\n";
            for (int i = 0; i < words.length; i++) {
               out += "align " + (i + 1) + " " + words[i] + " 1\n";
            }
            out += "align " + (words.length + 1) + " </s> 1\n";
            u.setRecognizerWcn(out);
         } else {
            u.clearRecognizerWcn();
         }
      }
      // HTK Lattice unused so removed 7/1/2013
      boolean useHTKLattice = false;
      if (useHTKLattice && primaryRecogResult.hasHtkLattice()) {
         u.setRecognizerLattice(primaryRecogResult.getHtkLattice());
      } else {
         // Clearing causes answer merger to fail but
         // setting to "" is allowed.
         //u.clearRecognizerLattice();
         u.setRecognizerLattice("");
      }
   }

   /**
    * Updates fields for forced alignment only.
    */
   public static void setCombinedRecognizerResultFieldsForcedAlignmentPass(UtteranceData.Builder u, CombinedRecognizerResult r, String desiredResult, double audioLengthSeconds) {
      boolean resultMatches = false;
      if (r.hasText()) {
         String actual = r.getText().trim();
         if (actual.compareTo(desiredResult.trim()) == 0) {
            // Result matches - we assume forced alignment succeeded and we have good values
            resultMatches = true;
         }
      }
      if (!resultMatches) {
         logger.error("'" + r.getText() + "' doesn't match '" + desiredResult + "'");
      }

      // Our behavior can be either to clear or zero
      boolean zeroValues = true;

      if (resultMatches) {
         int wc = r.getWordsCount();
         for (int i = 0; i < wc; i++) {
            SegmentValues wv = r.getWords(i);
            // Either find existing word marked with index i or create
            // new word.
            boolean createdWord = false;
            WordAnnotation.Builder wa;
            int wordIndex = lookupWordAnnotationIndex(u, i);
            if (wordIndex >= 0 && i < u.getWordLevelAnnotationsCount()) {
               wa = u.getWordLevelAnnotationsBuilder(i);
            } else {
               createdWord = true;

               // Didn't find it so create new one
               wa = WordAnnotation.newBuilder();

               wa.setWordIndex(i);
            }

            // Clear or zero any existing settings
            unsetWordValues(wa, zeroValues, audioLengthSeconds);

            if (wv.hasStartFrame()) {
               wa.setStartOffsetSeconds(frameToOffsetSeconds(wv.getStartFrame(), START_FRAME));
            }
            if (wv.hasEndFrame()) {
               wa.setEndOffsetSeconds(frameToOffsetSeconds(wv.getEndFrame(), END_FRAME));
            }
                /*
                // Use posterior from UW
                if (wv.hasPosterior()) {
                    wa.setAsrPosterior(DoubleAttribute.newBuilder().setValue(wv.getPosterior()).setConfidence(1.0));
                } else {
                    wa.clearAsrPosterior();
                }
                */
            if (wv.hasF0()) {
               FrameValues f0 = wv.getF0();
               if (f0.hasAverage()) {
                  wa.setF0Average(DoubleAttribute.newBuilder().setValue(f0.getAverage()).setConfidence(1.0));
               }
               if (f0.hasStdev()) {
                  wa.setF0Stdev(DoubleAttribute.newBuilder().setValue(f0.getStdev()).setConfidence(1.0));
               }
               if (f0.hasMaximum()) {
                  wa.setF0Maximum(DoubleAttribute.newBuilder().setValue(f0.getMaximum()).setConfidence(1.0));
               }
               if (f0.hasMinimum()) {
                  wa.setF0Minimum(DoubleAttribute.newBuilder().setValue(f0.getMinimum()).setConfidence(1.0));
               }
            }
            if (wv.hasEnergy()) {
               FrameValues rms = wv.getEnergy();
               if (rms.hasAverage()) {
                  wa.setRmsAverage(DoubleAttribute.newBuilder().setValue(rms.getAverage()).setConfidence(1.0));
               }
               if (rms.hasStdev()) {
                  wa.setRmsStdev(DoubleAttribute.newBuilder().setValue(rms.getStdev()).setConfidence(1.0));
               }
               if (rms.hasMaximum()) {
                  wa.setRmsMaximum(DoubleAttribute.newBuilder().setValue(rms.getMaximum()).setConfidence(1.0));
               }
               if (rms.hasMinimum()) {
                  wa.setRmsMinimum(DoubleAttribute.newBuilder().setValue(rms.getMinimum()).setConfidence(1.0));
               }
            }
            if (wv.hasVoicedProportion()) {
               wa.setVoicedProportion(DoubleAttribute.newBuilder().setValue(wv.getVoicedProportion()).setConfidence(1.0));
            }
            //if (wv.hasConfidence()) {
            // Don't use any DynaSpeak confidence
            //}

            // XXX Hack in case UW left off values
            addUwValuesIfNeeded(wa);

            // Don't add if already existed...
            if (createdWord) {
               // Warning: if initial state had partial words only,
               // words might not be in same order as word indices.
               u.addWordLevelAnnotations(wa.build());
            } else {
               // XXX This is probably not necessary because wa is a builder at i
               // that we requested.
               u.setWordLevelAnnotations(i, wa);
            }
         }
         //System.out.println("r.getPhoneDataCount() == " + r.getPhoneDataCount());
         if (r.getPhoneDataCount() > 0) {
            for (int i = 0; i < r.getPhoneDataCount(); i++) {
               u.addRecognizerForcedAlignmentPhoneData(r.getPhoneData(i));
               //System.out.println("Adding " + r.getPhoneData(i));
            }
         } else {
            u.clearRecognizerForcedAlignmentPhoneData();
         }
      } else {
         // Result didn't match so clear all fields in output
         String[] expectedWords = desiredResult.trim().split("\\s+");
         for (int i = 0; i < expectedWords.length; i++) {
            // Either find existing word marked with index i or create
            // new word.
            boolean createdWord = false;
            WordAnnotation.Builder wa;
            int wordIndex = lookupWordAnnotationIndex(u, i);
            if (wordIndex >= 0) {
               wa = u.getWordLevelAnnotationsBuilder(i);
            } else {
               createdWord = true;

               // Didn't find it so create new one
               wa = WordAnnotation.newBuilder();

               wa.setWordIndex(i);
            }

            // Clear or zero any existing settings
            unsetWordValues(wa, zeroValues, audioLengthSeconds);

            // Don't add if already existed...
            if (createdWord) {
               // Warning: if initial state had partial words only,
               // words might not be in same order as word indices.
               u.addWordLevelAnnotations(wa.build());
            } else {
               // XXX This is probably not necessary because wa is a builder at i
               // that we requested.
               u.setWordLevelAnnotations(i, wa);
            }
         }
         u.clearRecognizerForcedAlignmentPhoneData();
      }
   }

   // 2nd param if true will zero, false will clear
   private static void unsetWordValues(WordAnnotation.Builder wa, boolean zeroValues, double endOffsetSeconds) {
      if (!zeroValues) {
         wa.clearStartOffsetSeconds();
         wa.clearEndOffsetSeconds();
         wa.clearF0Average();

         wa.clearF0Average();
         wa.clearF0Stdev();
         wa.clearF0Maximum();
         wa.clearF0Minimum();

         wa.clearRmsAverage();
         wa.clearRmsStdev();
         wa.clearRmsMaximum();
         wa.clearRmsMinimum();

         wa.clearVoicedProportion();
      } else {
         wa.setStartOffsetSeconds(0.0);
         wa.setEndOffsetSeconds(endOffsetSeconds);

         double defVal = 0.0;
         double defConf = 0.0;

         wa.setF0Average(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setF0Stdev(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setF0Maximum(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setF0Minimum(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));

         wa.setRmsAverage(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setRmsStdev(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setRmsMaximum(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
         wa.setRmsMinimum(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));

         wa.setVoicedProportion(DoubleAttribute.newBuilder().setValue(defVal).setConfidence(defConf));
      }
   }

   private static void addUwValuesIfNeeded(WordAnnotation.Builder wa) {
      if (!wa.hasAsrPosterior()) {
         wa.setAsrPosterior(DoubleAttribute.newBuilder().setValue(0.0).setConfidence(0.0));
      }
      if (!wa.hasUwConfidence()) {
         wa.setUwConfidence(DoubleAttribute.newBuilder().setValue(0.0).setConfidence(0.0));
      }
      if (!wa.hasParserConfidence()) {
         wa.setParserConfidence(DoubleAttribute.newBuilder().setValue(0.0).setConfidence(0.0));
      }
      if (!wa.hasPosTag()) {
         wa.setPosTag(StringAttribute.newBuilder().setValue("NONE").setConfidence(0.0));
      }
   }

   /**
    * Convenience routine that calls method using default
    * values for additional args.
    *
    * @param frame      index from 0 to < ~32000.
    * @param offsetType MIDDLE_FRAME, START_FRAME, or END_FRAME.
    * @return frame offset in seconds.
    */
   public static final double frameToOffsetSeconds(int frame, int offsetType) {
      // XXX Should get from parameters file. But, is constant
      // for the models we plan to use.
      // Below are based on frame advance 160 for 16000 Hz and
      // window size 410.
      return frameToOffsetSeconds(frame, offsetType, 0.01, 0.025625);
   }

   /**
    * Convert from recognizer frame index to offset in seconds.
    *
    * @param frame               index from 0 to < ~32000.
    * @param offsetType          MIDDLE_FRAME, START_FRAME, or END_FRAME.
    * @param frameAdvanceSeconds how much block of samples advances
    *                            each frame.
    * @param frameSizeSeconds    generally frames overlap so the size of
    *                            a frame is usually larger than the frame advance.
    * @return frame offset in seconds.
    */
   public static final double frameToOffsetSeconds(int frame, int offsetType, double frameAdvanceSeconds, double frameSizeSeconds) {
      // Frame start
      double secs = frame * frameAdvanceSeconds;
      switch (offsetType) {
         case MIDDLE_FRAME:
            secs += frameSizeSeconds / 2.0;
            break;
         case END_FRAME:
            secs += frameSizeSeconds;
            break;
      }

      return secs;
   }

   // Return -1 if not found
   private static int lookupWordAnnotationIndex(UtteranceData.Builder u, int wordIndex) {
      int retval = -1;

      // Need to loop through all words since there's
      // no requirement they are in order.
      for (int i = 0; i < u.getWordLevelAnnotationsCount(); i++) {
         WordAnnotation checkWord = u.getWordLevelAnnotations(i);
         if (checkWord.hasWordIndex() && (checkWord.getWordIndex() == i)) {
            retval = i;
            break;
         }
      }

      return retval;
   }

   private static final Logger logger = LoggerFactory.getLogger(RecognizerToBoltMessage.class);
}
