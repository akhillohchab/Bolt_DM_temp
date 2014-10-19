package com.sri.bolt.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.StringBuffer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SRInterpServiceClient implements ServiceClient {
   public static final String EMPTY_TRANSLATION = "";

   // Keys for field lookups
   public final static String TRANSLATION_KEY = "translation";
   public final static String ALIGNMENT_KEY = "alignment";

   // This is an array so must append [0], [1], etc.
   public final static String NBEST_KEY = "nbest";

   // 10 seconds
   public final static long SRINTERP_CONNECT_TIMEOUT_MILLIS = 10000;

   public SRInterpServiceClient(Properties config, String prefix, String logname) {
      // Ignore the PreprocessingCommand and PostprocessingCommand for children to
      // handle appropriately *unless* the last param is true.
      this(config, prefix, logname, false);
   }

   public SRInterpServiceClient(Properties config, String prefix, String logname, boolean autoPrePostProcessing) {
      enableComponentLogging = Boolean.parseBoolean(config.getProperty("EnableComponentLogging", "false"));

      this.autoPrePostProcessing = autoPrePostProcessing;

      this.descriptiveName = prefix + "-" + logname;
      this.logName = prefix + "-" + logname;
      this.address = config.getProperty(prefix + "EndpointAddr");
      this.port = config.getProperty(prefix + "EndpointPort");
      this.path = config.getProperty(prefix + "Path");
      this.exe = config.getProperty(prefix + "Exe");
      this.preprocess = config.getProperty(prefix + "PreprocessingCommand", null);
      this.postprocess = config.getProperty(prefix + "PostprocessingCommand", null);

      // Note that we expand %var% in args
      this.args = config.getProperty(prefix + "Args").trim();

      // Break arguments into array and resolve %var%
      this.resolvedArgs = com.sri.bolt.Util.splitAndResolve(config, args);

      init();
   }

   @Override
   public void init() {
      buildEndpoint();
      client = new Socket();
   }

   @Override
   public void reinit() {
      logger.info(logName + " reinit called; restarting translator");

      if (!isConnected) {
          logger.info(logName + " not yet used; nothing to do");
          return;
      }

      try {
         // No need to connect since above checked if isConnected
         // connect();

         // Tell SRInterp to stop
         HashMap<String, byte[]> data = new HashMap<String, byte[]>();
         data.put("request", "shutdown".getBytes("UTF8"));
         com.sri.bolt.message.Util.writeMessageHash(client.getOutputStream(), data);

         logger.info(logName + " shutdown request sent to translator");

         isConnected = false;
         client.close();
         client = new Socket();

         logger.info(logName + " restarting translator");
         init();

         return;
      } catch (IOException e) {
          logger.error(logName + ":" + e.getMessage(), e);
      }

      // Typically, should have returned already
      logger.error(logName + " translator did not restart cleanly; killing");
      if (endpoint != null) {
          isConnected = false;
          client = null;
          endpoint.destroy();
      }

     logger.info(logName + " restarting translator");
      init();
   }

   /**
    * This is the main routine to call translator and get all outputs
    * from translator as name/value pairs.
    *
    * @param input Input string to send to translator.
    * @return HashMap of name/value pairs or null upon failure. Common are
    * translation, alignment, nbest.
    */
   public HashMap<String, String> getRawTranslationOutputs(String input) {
      if ((input == null) || (input.length() == 0)) {
         return null;
      }

      // Previously, performed some cleaning/filtering here but
      // now assume done in preprocessor stage

      if (autoPrePostProcessing && (preprocess != null)) {
         input = ServiceController.launchFilter(preprocess, input);
         if ((input == null) || (input.length() == 0)) {
            return null;
         }
      }

      try {
         if (!connect(SRINTERP_CONNECT_TIMEOUT_MILLIS)) {
            throw new IOException("Couldn't connect within timeout millis " + SRINTERP_CONNECT_TIMEOUT_MILLIS);
         }
         OutputStream stream = client.getOutputStream();

         boolean useChanges = false;
         if (useChanges) {
            // XXX More straightforward to set in HashMap and use writeMessageHash
            HashMap<String, byte[]> data = new HashMap<String, byte[]>();
            data.put("request", "translate".getBytes("UTF8"));
            data.put("input", input.getBytes("UTF8"));
            com.sri.bolt.message.Util.writeMessageHash(client.getOutputStream(), data);
         } else {
            String newText = "request(9)\r\n\r\ntranslate\r\n\r\ninput(" + input.length() + ")\r\n\r\n" +
                    input + "\r\n\r\nend\r\n\r\n";
            stream.write(newText.getBytes(Charset.forName("US-ASCII")));
         }
         stream.flush();

         InputStream inputStream = client.getInputStream();
         HashMap<String, String> outputs = com.sri.bolt.message.Util.readMessageHashAsUTF8Values(inputStream);

         if (autoPrePostProcessing && (postprocess != null)) {
            if (outputs.containsKey(TRANSLATION_KEY)) {
               String postprocessed = ServiceController.launchFilter(postprocess, outputs.get(TRANSLATION_KEY));
               // Override translation
               outputs.put(TRANSLATION_KEY, postprocessed);
            }
         }

         return outputs;
      } catch (IOException e) {
         logger.error(logName + ":" + e.getMessage(), e);
      }

      checkTranslationFailure();

      // Return null in case of bad input or exception
      return null;
   }

   /**
    * Get just the best output for input.
    * @param input Raw input to process.
    * @return raw 1-best output, null upon error.
    */
   public String getBest(String input) {
      String retval = null;

      HashMap<String, String> rawValues = getRawTranslationOutputs(input);

      if (rawValues != null) {
         retval = rawValues.get(TRANSLATION_KEY);
      }

      return retval;
   }

   // Call with output from translateTextExtendedOutput(String text).
   // Note: No post-processing (if enabled) applied to nbest outputs.
   public static ArrayList<String> extractNBestList(Map<String, String> translationResult) {
      ArrayList<String> nbest = new ArrayList<String>();

      if (translationResult == null) {
         return nbest;
      }

      int inbest = 0;
      // This is a 0-based array
      while (translationResult.containsKey(NBEST_KEY + "[" + inbest + "]")) {
          String val = translationResult.get(NBEST_KEY + "[" + inbest + "]");
          //App.getLog4jLogger().info("nbest[" + inbest + "] = " + val);
          nbest.add(val);
          inbest++;
      }

      return nbest;
   }

   // Check status of socket and process; close socket and restart
   // process if died. Otherwise, hope process still responsive.
   private void checkTranslationFailure() {
      // Had failure so no longer connected
      isConnected = false;

      if (client != null) {
         try {
            client.close();
         } catch (IOException ie) {
            // Ignore
         }
         client = new Socket();
      }

      try {
         if (endpoint != null) {
            int exitValue = endpoint.exitValue();
            logger.error(logName + ": server process has died, exit status " + exitValue);
         } else {
            logger.error(logName + ": endpoint null");
         }
         // Either null or not alive since alive would have thrown exception; restart
         if (endpoint != null) {
            try {
               endpoint.waitFor();
            } catch (InterruptedException e) {
               // Ignore
            }
            endpoint = null;
         }
         logger.error(logName + ": restarting dead endpoint");
         init();
      } catch (IllegalThreadStateException e) {
         logger.error(logName + ": server still running but failed responding to last request", e);
      }

   }

   private void buildEndpoint() {
      List<String> args = new ArrayList<String>();

      args.add("./" + exe);

      for (int i = 0; i < resolvedArgs.length; i++) {
         args.add(resolvedArgs[i]);
      }

      StringBuffer command = new StringBuffer();
      for (String arg : args) {
         if (command.length() > 0)
             command.append(" ");
         command.append(arg);
      }
      logger.info(logName + " path=" + this.path + " command=" + command.toString());

      ProcessBuilder pb = new ProcessBuilder(args);
      // Set working directory
      pb.directory(new File(path));
      try {
         endpoint = SocketServiceClient.startAndHandleLogging(pb, descriptiveName, enableComponentLogging, null);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   @Override
   public void cleanup() {
      endpoint.destroy();
      try {
         endpoint.waitFor();
      } catch (InterruptedException e) {

      }
   }

   private boolean connect(long srinterpConnectTimeoutMillis) {
      long startTime = System.currentTimeMillis();
      boolean loggedError = false;
      while (!isConnected) {
         try {
            client = new Socket();
            client.connect(new InetSocketAddress(address, Integer.parseInt(port)));
            isConnected = true;
            return true;
         } catch (NumberFormatException e) {
            if (!loggedError) {
               loggedError = true;
               logger.error(logName + ": " + e.getMessage(), e);
            }
         } catch (IOException e) {
            if (!loggedError) {
               loggedError = true;
               logger.error(logName + ": " + e.getMessage(), e);
            }
         }
         long elapsed = System.currentTimeMillis() - startTime;
         if (elapsed < srinterpConnectTimeoutMillis) {
            try {
               // Just poll until ready
               Thread.sleep(50);
            } catch (InterruptedException ie) {
               // Ignore
            }
         } else {
            // Give up
            break;
         }
      }

      if (loggedError) {
         // Report status
         long elapsedMillis = System.currentTimeMillis() - startTime;
         if (!isConnected) {
            logger.error(logName + ": still not connected after " + elapsedMillis + " millis");
         } // else reconnected successfully
      }

      return isConnected;
   }

   private static final Logger logger = LoggerFactory.getLogger(SRInterpServiceClient.class);
              // If true, we automatically pre- and post-process as part of translation
   private boolean autoPrePostProcessing;
   private Process endpoint;
   private boolean enableComponentLogging;
   private Socket client;
   private String port;
   private String path;
   private String exe;
   public final String preprocess;
   public final String postprocess;
   private String args;
   private String[] resolvedArgs;
   private String address;
   protected boolean isConnected;
   public final String descriptiveName;
   public final String logName;
}
