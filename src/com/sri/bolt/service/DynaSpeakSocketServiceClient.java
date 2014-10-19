package com.sri.bolt.service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.sri.recognizer.message.RecognizerMessages;
import com.sri.recognizer.message.RecognizerMessages.AudioData;
import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;
import com.sri.recognizer.message.RecognizerMessages.RecognizerResultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynaSpeakSocketServiceClient extends SocketServiceClient implements BoltDynaSpeakInterface {
   public DynaSpeakSocketServiceClient(Properties props, DynaSpeakConfig config) {
      super(props);
      name = "DynaSpeakSocketServiceClient-" + config.uniqueId;
      this.config = config;
      this.port = config.port;
      maxAttempts = 1;
      init();
   }

   @Override
   public void init() {
      buildEndpoint();

      InetSocketAddress addr = new InetSocketAddress(config.endpointAddress, Integer.parseInt(config.port));

      // Helper in parent class SocketServiceConnector
      // Try for up to 50 seconds
      socket = SocketServiceConnector.getConnected(name, config.initTimeoutSeconds, addr, endpoint);

      // clientSocket could be null if connection failed

      // XXX Until this point, better not have other threads trying to use socket
   }

   // Use 60 second timeout for DynaSpeak timeout.
   // (30 is the current default in base class.)
   // Want high for long inputs on slow machines.
   @Override
   protected int getTimeout() {
      return 60;
   }

   @Override
   public boolean startSamples() {
      //TODO make this return a value
      byte[] result = makeServiceCall("Recognizer.recognizeStream", true);
      if (result == null) {
         logger.error("Recognizer.recognizeStream failed");
         return false;
      }

      return true;
   }

   @Override
   public boolean setGrammar(String rule) {
      if (rule == null) {
         return false;
      }

      logger.info("Setting grammar rule to: " + rule);

      byte[] result = makeServiceCall("Recognizer.setGrammar", rule, true);
      if (result == null) {
         return false;
      }

      return true;
   }

   @Override
   public boolean extendGrammarWrapper(String rescoredOneBest) {
      if (rescoredOneBest == null) {
         return false;
      }

      rescoredOneBest = rescoredOneBest.trim();
      if (rescoredOneBest.length() == 0) {
         return false;
      }

      // Escape any symbols used by Buckwalter that could cause problems
      // in JSGF grammar.
      // OK: _-&%@
      // Escape: ^~<>|'}$*+
      // On 12/20/2013, added other necessary characters (just in case) since full list is: \/#;=|<>[]()*+{}
      // ^~'$ technically not necessary but doesn't hurt to escape them.
      // Note that \\ is escaped first since otherwise will escape backslashes added and not meant
      // to be escaped.
      rescoredOneBest = rescoredOneBest.
         replace("\\", "\\\\").
         replace("/", "\\/").
         replace("#", "\\#").
         replace(";", "\\;").
         replace("=", "\\=").
         replace("|", "\\|").
         replace("<", "\\<").
         replace(">", "\\>").
         replace("[", "\\[").
         replace("]", "\\]").
         replace("(", "\\(").
         replace(")", "\\)").
         replace("*", "\\*").
         replace("+", "\\+").
         replace("{", "\\{").
         replace("}", "\\}").
         replace("^", "\\^").
         replace("~", "\\~").
         replace("'", "\\'").
         replace("$", "\\$");

      // Above, avoided most common problems with bad grammar
      logger.info("Setting grammar to: " + rescoredOneBest);
      Message grammar = RecognizerMessages.RecognizerGrammar.newBuilder()
            .setData("@reject@ = rej rej rej;\npublic <TopLevel> = " + rescoredOneBest + ";").setKeepInternalCompiler(true).build();

      byte[] result = makeServiceCall("Recognizer.extendGrammar", grammar, true);
      if (result == null) {
         return false;
      }

      if (!endpointProcessAlive()) {
         // Return false on subsequent calls if died
         return false;
      }

      return setGrammar("TopLevel");
   }

   @Override
   public boolean setRescoringLMIndex(int index) {
      byte[] result = makeServiceCall("Recognizer.setRescoringLMIndex", "" + index, true);
      if (result == null) {
         logger.error("Recognizer.setRescoringLMIndex failed");
         return false;
      }

      String stringResult = new String(result);
      logger.info("Recognizer.setRescoringLMIndex(" + index + ") returned " + stringResult);

      return true;
   }

   @Override
   public boolean sendSamples(byte[] audio) {
      if (!endpointProcessAlive()) {
         // Return false on subsequent calls if died
         return false;
      }

      if (audio.length % 2 != 0) {
         logger.debug("Recognizer.addSamples: not even number of bytes");
      }
      AudioData audioData = AudioData.newBuilder().setData(ByteString.copyFrom(audio)).build();
      byte[] result = makeServiceCall("Recognizer.addSamples", audioData.toByteArray(), true);
      if (result == null) {
         return false;
      }

      return true;
   }

   @Override
   public CombinedRecognizerResult endSamples() {
      if (!endpointProcessAlive()) {
         // Return on subsequent calls if died
         return null;
      }

      makeServiceCall("Recognizer.endSamples", true);

      byte[] methResult = makeServiceCall("Recognizer.getCombinedRecognizerResult",
            RecognizerResultRequest.newBuilder().setTimeoutMillis(-1).build(), true);
      CombinedRecognizerResult result;
      try {
         result = CombinedRecognizerResult.parseFrom(methResult);
         return result;
      } catch (InvalidProtocolBufferException e) {
         logger.error("Recognizer.endSamples building protobuf failed", e);
         return null;
      }
   }

   @Override
   public boolean resetMllr() {
      // Must match value in DSSimpleConnection.h
      final int DS_RESET_MLLR = 1;

      if (!endpointProcessAlive()) {
         // Return on subsequent calls if died
         return false;
      }

      byte[] methResult = makeServiceCall("Recognizer.reset", "" + DS_RESET_MLLR, false);
      boolean retval = (methResult == null)?false:true;

      return retval;
   }

   @Override
   public void cleanup() {
      //TODO Make call for cleanup that doesn't retry
      try {
         // Sending the Shutdown should tell the server to exit *after* we close
         // the socket.
         if (socket != null) {
            if (endpointProcessAlive()) {
               // Don't make additional service calls if died
               makeServiceCall("Recognizer.shutdown", false);
            }
            //clientSocket.getOutputStream().close();
            //clientSocket.getInputStream().close();
            socket.close();
         }
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      // Call waitFor so process finishes (nothing in TIMED_WAIT)
      try {
         endpoint.waitFor();
      } catch (InterruptedException e) {
         e.printStackTrace();
         logger.error("Exception", e);
      }
   }

   public void buildEndpoint() {
      List<String> args = new ArrayList<String>();

      args.add("./" + config.dynaspeakExe);

      for (int i = 0; i < config.dynaspeakArgs.length; i++) {
         args.add(config.dynaspeakArgs[i]);
      }

      ProcessBuilder pb = new ProcessBuilder(args);
      // Set working directory
      pb.directory(new File(config.dynaspeakPath));
      try {
         endpoint = startAndHandleLogging(pb, name);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(DynaSpeakSocketServiceClient.class);
   protected DynaSpeakConfig config;
   private final String name;
}
