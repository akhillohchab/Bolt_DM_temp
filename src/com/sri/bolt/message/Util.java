package com.sri.bolt.message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.protobuf.Message;
import com.sri.bolt.message.BoltMessages.AnswerMergerOutput;
import com.sri.bolt.message.BoltMessages.DmActionType;
import com.sri.bolt.message.BoltMessages.DmClarifyOutput;
import com.sri.bolt.message.BoltMessages.DmClarifySegment;
import com.sri.bolt.message.BoltMessages.DmClarifySegmentActionType;
import com.sri.bolt.message.BoltMessages.DmOutput;
import com.sri.bolt.message.BoltMessages.DmTranslateOutput;
import com.sri.bolt.message.BoltMessages.DmTranslateSegment;
import com.sri.bolt.message.BoltMessages.DmTranslateSegmentActionType;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.MtData;
import com.sri.bolt.message.BoltMessages.OovSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.StringAttribute;
import com.sri.bolt.message.BoltMessages.UserFeedbackType;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small collection of static utility methods for use with messages
 *
 * @author peter.blasco@sri.com
 */
public final class Util {
   public static boolean writeMessageToFile(String path, String methodName, Message msg) {
      File file = new File(path);
      try {
         if (!file.createNewFile()) {
            return false;
         }
      } catch (IOException e) {
//         logger.error(e.getMessage(), e);
         return false;
      }
      byte[] args = msg.toByteArray();
      try {
         BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
         writeMessage(outputStream, methodName, args);
         outputStream.close();
      } catch (IOException e) {
//         logger.error(e.getMessage(), e);
         return false;
      }

      return true;
   }

   public static SessionData readMessageFromFile(File file) {
      try {
         BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
         return SessionData.parseFrom(readMessage(inputStream));
      } catch (IOException e) {
         return null;
      }
   }

   public static void writeMessage(OutputStream stream, String methodName, byte[] data) throws IOException {
      HashMap<String, byte[]> values = new HashMap<String, byte[]>();
      values.put("method", methodName.getBytes(CHARSET));
      values.put("arg0", data);
      writeMessageHash(stream, values);
   }

   /*
    * public static byte[] readMessage(InputStream stream) throws IOException {
    * byte[] buffer = new byte[1024 * 1024]; StringBuilder builder = new
    * StringBuilder(); ByteArrayOutputStream bytes = new
    * ByteArrayOutputStream(); int read = 0; while (true) { read =
    * stream.read(buffer); if (read == -1) { break; } bytes.write(buffer, 0,
    * read); builder.append(new String(buffer, 0, read, CHARSET)); if
    * (builder.toString().endsWith("end\n\n")) { break; } }
    *
    * return parseArgs(builder.toString(), bytes.toByteArray()); }
    */

   public static byte[] readMessage(InputStream stream) throws IOException {
      HashMap<String, byte[]> values = readMessageHash(stream);
      if (values.containsKey("arg0")) {
         return values.get("arg0");
      } else if (values.containsKey("SessionData")) {
         return values.get("SessionData");
      } else {
         return new byte[0];
      }
   }

   public static void writeMessageHash(OutputStream stream, HashMap<String, byte[]> data) throws IOException {
      // XXX Treat method and arg0 special, writing first
      byte[] methData = data.get("method");
      if (methData != null) {
         String name = "method";
         writeValues(stream, name.getBytes(CHARSET), methData);
      }
      byte[] argsData = data.get("arg0");
      if (argsData != null) {
         String name = "arg0";
         writeValues(stream, name.getBytes(CHARSET), argsData);
      }
      for (String key : data.keySet()) {
         if ((key.compareTo("method") == 0) || (key.compareTo("arg0") == 0)) {
            // Would have already sent
            continue;
         } else {
            writeValues(stream, key.getBytes(CHARSET), data.get(key));
         }
      }
      String end = "end\n\n";
      stream.write(end.getBytes(CHARSET));
      stream.flush();
   }

   private static void writeValues(OutputStream stream, byte[] method, byte[] data) throws IOException {
      String len = "(" + data.length + ")";
      String sep = "\n\n";
      stream.write(method);
      stream.write(len.getBytes(CHARSET));
      stream.write(sep.getBytes(CHARSET));
      stream.write(data);
      stream.write(sep.getBytes(CHARSET));
   }

   public static HashMap<String, byte[]> readMessageHash(InputStream stream) throws IOException {
      ByteArrayOutputStream nameBytes = new ByteArrayOutputStream();
      HashMap<String, byte[]> retval = new HashMap<String, byte[]>();
      while (true) {
         // The "name" should be short - read up to \n\n or \r\n\r\n
         // then true to trim the newlines.
         readThroughTwoNewlines(stream, nameBytes, true);
         String name = new String(nameBytes.toByteArray(), CHARSET.name());
         // end of name/values upon "end\n\n"
         if (name.compareTo("end") == 0) {
            break;
         }
         //System.out.println("The name I read is: " + name);
         int parenPos = name.indexOf("(");
         int msgLen = -1;
         if (parenPos >= 0) {
            String msgLenStr = name.substring(parenPos + 1);
            int endParenPos = msgLenStr.indexOf(")");
            if (endParenPos >= 0) {
               msgLenStr = msgLenStr.substring(0, endParenPos);
            }
            name = name.substring(0, parenPos);
            msgLen = Integer.parseInt(msgLenStr);
         }

         if (msgLen < 0) {
            throw new IOException("Message length missing");
         }
         byte[] msgBytes = new byte[msgLen];
         int totRead = 0;
         while (totRead < msgLen) {
            int read = stream.read(msgBytes, totRead, msgBytes.length - totRead);
            if (read < 0) {
               throw new IOException("Stream ended prematurely");
            }
            totRead += read;
         }
         retval.put(name, msgBytes);

         /*
         if (name.compareTo("response") == 0) {
            // Write what method was called
            String value = new String(msgBytes, CHARSET.name());
            System.out.println("Value is: " + value);
         }
         */

         // This is just to skip two lines
         readThroughTwoNewlines(stream, null, false);
      }

      return retval;
   }

   public static HashMap<String, String> readMessageHashAsUTF8Values(InputStream stream) throws IOException {
      HashMap<String, byte[]> bmap = readMessageHash(stream);

      if (bmap == null) {
         return null;
      }

      HashMap<String, String> retval = new HashMap<String, String>();

      Set<String> keys = bmap.keySet();
      for (String key : keys) {
         byte[] val = bmap.get(key);
         // Interpret byte[] data as UTF8 string
         String sval = new String(val, CHARSET);
         retval.put(key, sval);
         //System.out.println("Saved:" + key + "=" + sval);
      }

      return retval;
   }

   /*
   private static byte[] parseArgs(String response, byte[] bytes) {
      String[] args = response.split("\n\n");
      int numChars = 0;
      for (int count = 0; count < args.length; ++count) {
         // account for \n\n
         numChars += args[count].length() + 2;
         if ((args[count].indexOf("arg0") != -1 || args[count].indexOf("SessionData") != -1)
               && args.length != count + 1) {
            int byteStart = numChars;
            // account for \n\nend\n\n
            int end = bytes.length - 7;
            byte[] returnBytes = new byte[end - byteStart];
            System.arraycopy(bytes, byteStart, returnBytes, 0, end - byteStart);
            return returnBytes;
         }
      }

      return new byte[0];
   }
   */

   // If "out" is null, we are simply skipping past bytes in the stream.
   private static void readThroughTwoNewlines(InputStream stream, ByteArrayOutputStream out, boolean stripNewlines)
           throws IOException {
      if (out != null) {
         out.reset();
      }
      boolean foundNewlines = false;
      ByteArrayOutputStream newlineBuf = new ByteArrayOutputStream();
      while (!foundNewlines) {
         int c = stream.read();
         if (c == -1) {
            // Premature socket close
            throw new IOException("Premature socket close");
         } else {
            switch (c) {
               case '\r':
               case '\n':
                  newlineBuf.write(c);
                  byte[] check = newlineBuf.toByteArray();
                  if ((check.length >= 2) && (check[check.length - 2] == '\n') && (check[check.length - 1] == '\n')) {
                     // Found \n\n
                     if (out != null) {
                        if (!stripNewlines) {
                           out.write(check, 0, check.length);
                        } else if (check.length > 2) {
                           // had some other characters before the newlines
                           out.write(check, 0, check.length - 2);
                        }
                     }
                     foundNewlines = true;
                  } else if ((check.length >= 4) && (check[check.length - 4] == '\r') && (check[check.length - 3] == '\n')
                          && (check[check.length - 2] == '\r') && (check[check.length - 1] == '\n')) {
                     if (out != null) {
                        // Found \r\n\r\n
                        if (!stripNewlines) {
                           out.write(check, 0, check.length);
                        } else if (check.length > 4) {
                           // had some other characters before the newlines
                           out.write(check, 0, check.length - 4);
                        }
                     }
                     foundNewlines = true;
                  }
                  break;
               default:
                  if (newlineBuf.size() > 0) {
                     // False alarm on newlines
                     byte[] buf = newlineBuf.toByteArray();
                     out.write(buf, 0, buf.length);
                     newlineBuf.reset();
                  }
                  out.write(c);
            }
         }
      }
   }

   private static String printLabel(String label) {
      if (label != null)
         return String.format("%-20s: ", label);
      else
         return String.format("%-22s", "");
   }

   public static void outputSessionDataSummary(OutputStream stream, List<SessionData> datas, List<Language> languages, List<List<SessionData>> uwSessionDatas, List<List<SessionData>> asrSessionDatas, String trialID) {
      PrintStream printStream = new PrintStream(stream);
      if (gSeenTrials.containsKey(trialID)) {
         // Reset our counters since seeing trial again.
         // Assumption is that only go forward with trials.
         TrialInfo storedTrialInfo = gSeenTrials.get(trialID);
         gTurnCount = storedTrialInfo.mTurnStart;
         gInteractionCount = storedTrialInfo.mInteractionStart;
      } else {
         // Note that we store the pre-incremented values (values last used before trial)
         gSeenTrials.put(trialID, new TrialInfo(gTurnCount, gInteractionCount));
      }
      printStream.println(printLabel("trial_id: ") + trialID);
      int count = 0;
      for (SessionData data : datas) {
         if (data.getUtterancesCount() == 0) {
            logger.warn("No utterances for trial " + trialID);
         }
         gInteractionCount++;
         Language l = languages.get(count);
         List<SessionData> uwSessionDataList = uwSessionDatas.get(count);
         List<SessionData> asrSessionDataList = asrSessionDatas.get(count++);
         for (int turnNum = 0; turnNum < data.getUtterancesCount(); ++turnNum) {
            gTurnCount++;
            UtteranceData utt = data.getUtterances(turnNum);
            printStream.println();
            printStream.println(printLabel("counter") + gTurnCount);
            printStream.println(printLabel("interaction") + gInteractionCount);
            // Changed from 0-based to 1-based on 11/15/2013
            printStream.println(printLabel("turn") + (turnNum + 1));
            printStream.println(printLabel("lang") + l.getAbbreviation());
            // globalcount-lang-inter.turn
            printStream.println(printLabel("tid") + l.getAbbreviation() + "_" + gInteractionCount + "." + (turnNum + 1));
            printStream.println(printLabel("run_rescoring") + utt.getRunRescoring());
            if (utt.hasRecognizer1Best())
               printStream.println(printLabel("recognizer_1best") + "\"" + utt.getRecognizer1Best().trim() + "\"");
            else
               printStream.println(printLabel("recognizer_1best") + "null");
            if (utt.hasRecognizer1BestSecondary())
               printStream.println(printLabel("recognizer_1best_secondary") + "\"" + utt.getRecognizer1BestSecondary().trim() + "\"");
            else
               printStream.println(printLabel("recognizer_1best_secondary") + "null");
            if (utt.hasRescored1Best()) {
               String rescored1best = utt.getRescored1Best().trim();
               printStream.println(printLabel("rescored_1best") + "\"" + rescored1best + "\"");
               String[] words = rescored1best.split("\\s+");
               for (int oovSegmentCount = 0; oovSegmentCount < utt.getAsrOovAnnotationsCount(); ++oovSegmentCount) {
                  OovSegmentAnnotation asrSeg = utt.getAsrOovAnnotations(oovSegmentCount);
                  printStream.print(printLabel("oov_seg_" + oovSegmentCount));
                  printStream.print("[" + asrSeg.getStartIndex() + "," + asrSeg.getEndIndex() + "]=\"");
                  for (int wordCount = asrSeg.getStartIndex(); wordCount <= asrSeg.getEndIndex(); ++wordCount) {
                     if (wordCount > asrSeg.getStartIndex())
                        printStream.print(" ");
                     printStream.print(words[wordCount]);
                  }
                  printStream.print("\"; [Conf=" + String.format("%5.3f", asrSeg.getConfidence()) + "]; ");
                  if (asrSeg.hasSyntacticCategory())
                     printStream.print("[Category=" + asrSeg.getSyntacticCategory().getValue() + "]; ");
                  if (asrSeg.hasIsNamedEntity())
                     printStream.print("[is_NE=" + asrSeg.getIsNamedEntity().getValue() + "]");
                  printStream.println();
               }
            } else
               printStream.println(printLabel("rescored_1best") + "null");

            if (utt.hasAnswerMergerOutput()) {
               AnswerMergerOutput ansMer = utt.getAnswerMergerOutput();
               if (ansMer.hasWorkingUtterance()) {
                  String workUtt = ansMer.getWorkingUtterance().trim();
                  printStream.println(printLabel("working_utterance") + "\"" + workUtt + "\"");
                  if (ansMer.hasMergingOperation()) {
                     printStream.print(printLabel("merging_operation") + ansMer.getMergingOperation());
                     if (ansMer.hasUserFeedback())
                        printStream.print(" [userFeedback=" + ansMer.getUserFeedback() + "]");
                     if (ansMer.hasGoBack())
                        printStream.print(" [goBack=" + ansMer.getGoBack().getValue() + "]");
                     printStream.println();
                  }

                  UtteranceData uwUtt;
                  if (uwSessionDataList.size() != 0) {
                     if (uwSessionDataList.get(turnNum) != null) {
                        uwUtt = uwSessionDataList.get(turnNum).getUtterances(turnNum);
                        printErrorSegments(printStream, uwUtt, workUtt, "uw");
                        printStream.println();
                     }
                  }

                  UtteranceData asrUtt;
                  if (asrSessionDataList.size() != 0) {
                     if (asrSessionDataList.get(turnNum) != null) {
                        asrUtt = asrSessionDataList.get(turnNum).getUtterances(turnNum);
                        printErrorSegments(printStream, asrUtt, workUtt, "asr");
                        printStream.println();
                     }
                  }

                  printErrorSegments(printStream, utt, workUtt, "final");
                  printStream.println();
               } else
                  printStream.print(printLabel("working_utterance") + "null");
            }

            if (utt.hasDmOutput()) {
               DmOutput dmOutput = utt.getDmOutput();
               if (dmOutput.hasDmAction()) {
                  printStream.println(printLabel("dm_action") + dmOutput.getDmAction());
                  if (dmOutput.hasQgRuleId())
                     printStream.println(printLabel(null) + "[dm_rule_id=\"" + dmOutput.getQgRuleId() + "\"]");
                  if (dmOutput.getDmAction().equals(DmActionType.ACTION_CLARIFY_UTTERANCE)) {
                     DmClarifyOutput dmClarify = dmOutput.getDmClarifyOutput();
                     if (dmClarify.hasErrorSegmentIndex())
                        printStream.println(printLabel(null) + "[dm_error_seg=error_seg_" + dmClarify.getErrorSegmentIndex() + "]");
                     if (dmClarify.hasType())
                        printStream.println(printLabel(null) + "[dm_clarify_action=" + dmClarify.getType() + "]");
                     if (dmClarify.hasExpectedOperation())
                        printStream.println(printLabel(null) + "[dm_expected_operation=" + dmClarify.getExpectedOperation() + "]");
                     if (dmClarify.hasTargetedAttribute())
                        printStream.println(printLabel(null) + "[dm_target_attr=" + dmClarify.getTargetedAttribute() + "]");
                     String question = "";
                     for (DmClarifySegment segment : dmClarify.getSegmentsList()) {
                        if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT)) {
                           question += segment.getTtsInput() + " ";
                        } else if (segment.getAction().equals(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT)) {
                           question += "PLAY(error_seg_" + segment.getErrorSegmentIndex() + ") ";
                        }
                     }
                     printStream.println(printLabel(null) + "[dm_question=\"" + question.trim() + "\"]");
                  } else if (dmOutput.getDmAction().equals(DmActionType.ACTION_TRANSLATE_UTTERANCE)) {
                     DmTranslateOutput translateOutput = dmOutput.getDmTranslateOutput();
                     String mtInput = "";
                     for (DmTranslateSegment segment : translateOutput.getSegmentsList()) {
                        if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT)) {
                           mtInput += segment.getMtInput() + " ";
                        } else if (segment.getAction().equals(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT)) {
                           mtInput += "TRANSLITERATE(error_seg_" + segment.getErrorSegmentIndex() + ") ";
                        }
                     }
                     printStream.println(printLabel(null) + "[dm_translate=\"" + mtInput.trim() + "\"]");
                  }
               } else
                  printStream.println(printLabel("dm_action") + "null");
            }
            if (utt.hasMtData()) {
               MtData mtData = utt.getMtData();
               if (mtData.getPostprocessedTranslationsCount() > 0)
                  printStream.println(printLabel("mt_output") + "[translation=\"" + mtData.getPostprocessedTranslations(0).trim() + "\"]");
               else
                  printStream.println(printLabel("mt_output") + "[translation=null]");
               if (mtData.hasOriginalInput())
                  printStream.println(printLabel(null) + "[orig_input=\"" + mtData.getOriginalInput().trim() + "\"]");
               if (mtData.hasPreprocessedInput())
                  printStream.println(printLabel(null) + "[prep_input=\"" + mtData.getPreprocessedInput().trim() + "\"]");
               if (mtData.getOriginalTranslationsCount() > 0)
                  printStream.println(printLabel(null) + "[orig_trans=\"" + mtData.getOriginalTranslations(0).trim() + "\"]");
//               if (mtData.getAlignmentsCount() > 0)
//                  printStream.println(printLabel(null) + "[alignment=\"" + mtData.getAlignments(0).trim() + "\"]");
            }
         }
         printStream.println("=======================");
         printStream.println();
         printStream.println();
      }

      printStream.close();
   }

   public static final Charset CHARSET = Charset.forName("UTF8");


   public static void printErrorSegments(PrintStream printStream, UtteranceData utt, String workUtt, String prefix) {
      String[] words = workUtt.split("\\s+");
      for (int errorSegmentCount = 0; errorSegmentCount < utt.getErrorSegmentsCount(); ++errorSegmentCount) {
         ErrorSegmentAnnotation errorSeg = utt.getErrorSegments(errorSegmentCount);
         printStream.print(printLabel(prefix + "_" + "error_seg_" + errorSegmentCount));
         printStream.print("[" + errorSeg.getStartIndex() + "," + errorSeg.getEndIndex() + "]=\"");
         for (int wordCount = errorSeg.getStartIndex(); wordCount <= errorSeg.getEndIndex() && wordCount < words.length; ++wordCount) {
            if (wordCount > errorSeg.getStartIndex())
               printStream.print(" ");
            printStream.print(words[wordCount]);
         }
         printStream.println("\"");
         printStream.print(printLabel(null));
         if (errorSeg.hasIsResolved())
            printStream.print("[Resolved=" + errorSeg.getIsResolved().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsResolved().getConfidence()) + "]");
         else
            printStream.print("[New error] ");
         if (errorSeg.hasIsModified()) {
            printStream.print("[Is Modified=" + errorSeg.getIsModified().getValue() + "] ");
         }
         if (errorSeg.hasErrorType() && errorSeg.getErrorType() == ErrorSegmentType.ERROR_SEGMENT_ASR)
            printStream.print("[ASR error] ");
         else if (errorSeg.hasErrorType() && errorSeg.getErrorType() == ErrorSegmentType.ERROR_SEGMENT_MT)
            printStream.print("[MT error] ");
         printStream.println("[Conf=" + String.format("%5.3f", errorSeg.getConfidence()) + "]");

         if (errorSeg.hasSpelling() || errorSeg.hasNeTag() || errorSeg.hasPosTag() || errorSeg.hasDepTag() || errorSeg.hasDepWordIndex()) {
            printStream.print(printLabel(null));
            if (errorSeg.hasSpelling()) {
               printStream.print("[Spelling=\"" + errorSeg.getSpelling().getValue().trim() + "\"");
               if (errorSeg.getSpelling().hasUserFeedback()) {
                  if (errorSeg.getSpelling().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER)
                     printStream.print("; Confirmed");
                  else
                     printStream.print("; Rejected");
               }
               printStream.print("] ");
            }
            if (errorSeg.hasNeTag()) {
               printStream.print("[Ne_tag=" + errorSeg.getNeTag().getValue().trim());
               if (errorSeg.getNeTag().hasUserFeedback()) {
                  if (errorSeg.getNeTag().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER)
                     printStream.print("; Confirmed");
                  else
                     printStream.print("; Rejected");
               }
               printStream.print("] ");
            }
            if (errorSeg.hasPosTag())
               printStream.print("[Pos_tag=" + errorSeg.getPosTag().getValue().trim() + "] ");
            if (errorSeg.hasDepTag())
               printStream.print("[Dep_tag=" + errorSeg.getDepTag().getValue().trim() + "] ");
            if (errorSeg.hasDepWordIndex()) {
               int depWordIndex = errorSeg.getDepWordIndex().getValue();
               if (depWordIndex >= 0 && depWordIndex < words.length)
                  printStream.print("[Dep_word=\"" + words[depWordIndex] + "\"]");
               else
                  printStream.print("[Dep_word=INVALID_WORD_INDEX:" + depWordIndex + "]");
            }
            printStream.println();
         }

         if (errorSeg.hasIsAsrOov() || errorSeg.hasIsAsrAmbiguous()) {
            printStream.print(printLabel(null));
            if (errorSeg.hasIsAsrOov())
               printStream.print("[is_asr_oov=" + errorSeg.getIsAsrOov().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsAsrOov().getConfidence()) + "] ");
            if (errorSeg.hasIsAsrAmbiguous())
                printStream.print("[is_asr_ambiguous=" + errorSeg.getIsAsrAmbiguous().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsAsrAmbiguous().getConfidence()) + "] ");
            if (errorSeg.getAmbiguousWordsCount() > 0) {
                printStream.print("[Ambiguous_words=(");
                for (int k = 0; k < errorSeg.getAmbiguousWordsCount(); k++) {
                   if (k > 0)
                      printStream.print(";");
                   StringAttribute att = errorSeg.getAmbiguousWords(k);
                   printStream.print(att.getValue());
                }
                printStream.print(")]");
             }
            printStream.println();
         }

         if (errorSeg.hasIsMtOov() || errorSeg.hasIsMtWordDropped() || errorSeg.hasIsMtQuestionable() || errorSeg.hasIsMtAmbiguous()) {
            printStream.print(printLabel(null));
            if (errorSeg.hasIsMtOov())
               printStream.print("[is_mt_oov=" + errorSeg.getIsMtOov().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsMtOov().getConfidence()) + "] ");
            if (errorSeg.hasIsMtQuestionable())
               printStream.print("[is_mt_questionable=" + errorSeg.getIsMtQuestionable().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsMtQuestionable().getConfidence()) + "] ");
            if (errorSeg.hasIsMtWordDropped())
               printStream.print("[is_mt_word_dropped=" + errorSeg.getIsMtWordDropped().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsMtWordDropped().getConfidence()) + "] ");
            if (errorSeg.hasIsMtAmbiguous())
               printStream.print("[is_mt_ambiguous=" + errorSeg.getIsMtAmbiguous().getValue() + "; Conf=" + String.format("%5.3f", errorSeg.getIsMtAmbiguous().getConfidence()) + "] ");
            if (errorSeg.getAmbiguousWordsCount() > 0) {
               printStream.print("[Word senses=(");
               for (int k = 0; k < errorSeg.getAmbiguousWordsCount(); k++) {
                  if (k > 0)
                     printStream.print(";");
                  StringAttribute att = errorSeg.getAmbiguousWords(k);
                  printStream.print(att.getValue());
               }
               printStream.print(")]");
            }
            printStream.println();
         }
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(Util.class);

   private static class TrialInfo {
      public TrialInfo(long turn, long interaction) {
         mTurnStart = turn;
         mInteractionStart = interaction;
      }

      final long mTurnStart;
      final long mInteractionStart;
   }

   // Store turn and interaction counters based on when new trial first seen
   // but may log same trial many times.
   private static HashMap<String, TrialInfo> gSeenTrials = new HashMap<String, TrialInfo>();
   // Keep track of current highest values used
   private static long gTurnCount = 0;
   private static long gInteractionCount = 0;
}
