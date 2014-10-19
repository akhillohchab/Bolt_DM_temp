package com.sri.bolt.service;

import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.MtData;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.sri.bolt.Util.reserveUniqueFileName;
import static com.sri.bolt.service.SocketServiceClient.startAndHandleLogging;

public class MTErrorDetectorServiceClient implements SessionDataServiceClient {
   public MTErrorDetectorServiceClient(Properties config, Language lang) {
      language = lang;
      mtErrorPath = config.getProperty(lang == Language.ENGLISH ? "en.mtErrorPath" : "ia.mtErrorPath");
      mtErrorExe = config.getProperty("mtErrorScriptExe");
      init();
   }

   @Override
   public void init() {

   }

   @Override
   public void reinit() {

   }

   @Override
   public SessionData checkInput(SessionData data) {
      //TODO add error checking
      return data;
   }

   @Override
   public SessionData process(SessionData data) {
      String originalIn = App.getApp().getRunDir().getAbsolutePath() + "/original.in";
      String preprocessIn = App.getApp().getRunDir().getAbsolutePath() + "/preprocess.in";
      String nbestIn = App.getApp().getRunDir().getAbsolutePath() + "/in.nbest";
      String nBestInGzip = nbestIn + ".gz";
      String protoOut = reserveUniqueFileName(App.getApp().getRunDir().getAbsolutePath() + "/mtErr", ".out");
      Writer writer = null;
      try {
         UtteranceData lastUtt = data.getUtterances(data.getUtterancesCount() - 1);
         MtData mtData = lastUtt.getMtData();
         writer = new BufferedWriter(new OutputStreamWriter(
                 new FileOutputStream(originalIn), "utf-8"));
         writer.write(mtData.getOriginalInput());
         writer.flush();
         writer.close();

         writer = new BufferedWriter(new OutputStreamWriter(
                 new FileOutputStream(preprocessIn), "utf-8"));
         writer.write(mtData.getPreprocessedInput());
         writer.flush();
         writer.close();

         writer = new BufferedWriter(new OutputStreamWriter(
                 new FileOutputStream(nbestIn), "utf-8"));
         for (String nbest : mtData.getNbestCompositeList()) {
            writer.write(nbest + "\n");
         }
         writer.flush();
         writer.close();

         List<String> args = new ArrayList<String>();
         args.add("gzip");
         args.add(nbestIn);

         ProcessBuilder pb = new ProcessBuilder(args);
         pb.directory(new File(mtErrorPath));
         try {
            Process endpoint = startAndHandleLogging(pb, "MTError", true, null);
            endpoint.waitFor();
         } catch (IOException e) {
            logger.error(e.getMessage(), e);
         } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }

      } catch (IOException ex){
         // report
      } finally {
         try {writer.close();} catch (Exception ex) {}
      }

      List<String> args = new ArrayList<String>();
      args.add("./" + mtErrorExe);
      args.add(nBestInGzip);
      args.add(preprocessIn);
      args.add(originalIn);
      args.add(protoOut);

      ProcessBuilder pb = new ProcessBuilder(args);
      pb.directory(new File(mtErrorPath));
      try {
         Process endpoint = startAndHandleLogging(pb, "MTError", true, null);
         endpoint.waitFor();
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

      File protoOutFile = new File(protoOut);
      if (protoOutFile.exists()) {
         try {
            byte[] fileBytes = new byte[(int) protoOutFile.length()];
            if (fileBytes.length > 0) {
               ErrorSegmentAnnotation errorSegmentAnnotation;
               (new BufferedInputStream(new FileInputStream(protoOutFile))).read(fileBytes);
               errorSegmentAnnotation = ErrorSegmentAnnotation.parseFrom(fileBytes);
               SessionData.Builder builder = data.toBuilder();
               UtteranceData.Builder lastUtt = builder.getUtterancesBuilder(data.getUtterancesCount() - 1);
               lastUtt.addErrorSegments(errorSegmentAnnotation.toBuilder());
               lastUtt.clearDmOutput();
               lastUtt.clearMtData();
               int currentTurn = builder.getCurrentTurn();
               builder.setUtterances(currentTurn, lastUtt);
               data = builder.build();
            }

         } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }
      }

      return data;
   }

   @Override
   public void cleanup() {
   }

   private static final Logger logger = LoggerFactory.getLogger(MTErrorDetectorServiceClient.class);
   private Language language;
   private String mtErrorPath;
   private String mtErrorExe;
}
