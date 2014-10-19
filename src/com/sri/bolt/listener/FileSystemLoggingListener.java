package com.sri.bolt.listener;


import com.sri.bolt.message.BoltMessages.SessionData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileSystemLoggingListener implements WorkflowListenerInterface {

   public FileSystemLoggingListener(String dir) {
      binaryLogDir = dir;
      workflowReadableList = new ArrayList<String>();
   }

   @Override
   public void onStartWorkflow(String trialId, long timeWorkflowStarted) {
      binaryLogBasename = binaryLogDir + "/" + com.sri.bolt.Util.getFilenameTimestamp(timeWorkflowStarted);
      workflowReadableList.clear();
      this.timeWorkflowStarted = timeWorkflowStarted;
   }

   @Override
   public void workflowTaskComplete(SessionData sessionData, boolean successful, String name) {
      if (successful) {
         workflowReadableList.add(name + ": complete");
      } else {
         // IMPORTANT: any text added to name should start with ":"
         workflowReadableList.add(name + ": failed");
      }

      long elapsed = System.currentTimeMillis() - timeWorkflowStarted;

      int step = workflowReadableList.size();
      String lastRanComponent = workflowReadableList.get(step - 1);
      // Strip some characters and avoid whitespace in filenames
      String simpleStepName = lastRanComponent.
              replaceAll("[:]", "").
              replaceAll("\\s+", "-");

//            SessionData sessionData = App.getApp().getTrial().getCurrentSessionData();
      String stepDisplay = String.format("%02d", step);
      String basename = binaryLogBasename + "-STEP-" + stepDisplay + "-" + simpleStepName;
      if ((step > 0) && !lastRanComponent.endsWith("complete")) {
         basename += "-failed-" + com.sri.bolt.Util.getFilenameTimestamp();
      }

      try {
         FileOutputStream infoStream = new FileOutputStream(basename + ".info");
         String elapsedString = "elapsedMillis " + elapsed + "\n";
         infoStream.write(elapsedString.getBytes(CHARSET));
         for (int i = 0; i < workflowReadableList.size(); i++) {
            infoStream.write(workflowReadableList.get(i).getBytes(CHARSET));
            infoStream.write('\n');
         }
         if (sessionData != null) {
            String s = "SessionData as String:\n\n";
            infoStream.write(s.getBytes(CHARSET));
            String readableProtobuf = sessionData.toString();
            // As of 6/27/2013, shouldn't have HTK lattice
            // and printing it would confirm it's gone.
            // So, no longer strip it.
            //readableProtobuf = readableProtobuf.replaceAll("(recognizer_lattice: ).*", "$1OMITTED");
            infoStream.write(readableProtobuf.getBytes(CHARSET));

            FileOutputStream dataStream = new FileOutputStream(basename + ".SessionData");
            dataStream.write(sessionData.toByteArray());
            dataStream.close();
         }
         elapsed = System.currentTimeMillis() - timeWorkflowStarted;
         elapsedString = "elapsedMillis " + elapsed + "\n";
         infoStream.write(elapsedString.getBytes(CHARSET));
         infoStream.close();
      } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public void onExceptionFound(String exception) {
      //To change body of implemented methods use File | Settings | File Templates.
   }


   private final String CHARSET = "UTF8";
   private long timeWorkflowStarted;
   private String binaryLogDir;
   private String binaryLogBasename;
   ArrayList<String> workflowReadableList;
}
