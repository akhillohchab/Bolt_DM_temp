package com.sri.bolt;

import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.sri.bolt.workflow.Util.getNameForWorkflowType;

public class TimeKeeper {
   public TimeKeeper() {
      runs = new ArrayList<Run>();
   }

   public synchronized void startRun() {
      runs.add(new Run());
   }

   public synchronized void runFailed() {
      runs.remove(runs.size() - 1);
   }

   public synchronized void startTiming(String task) {
      getCurrentRun().startTiming(task);
   }

   public synchronized void addTime(String task, long timeInMillis) {
      getCurrentRun().addTime(task, timeInMillis);
   }

   public synchronized void stopTiming(String task) {
      getCurrentRun().stopTiming(task);
   }

   private Run getCurrentRun() {
      return runs.get(runs.size() - 1);
   }

   public synchronized void writeOut() {
      String outputFileName = Util.reserveUniqueFileName(App.getApp().getProps().getProperty("OutputDir") + "/timings", ".out");
      Writer writer = null;
      try {
         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
         Map<String, List<Long>> times = new HashMap<String, List<Long>>();
         for (Run run : runs) {
            for (Entry<String, Long> entry : run.elapsedTimes.entrySet()) {
               if (!times.containsKey(entry.getKey())) {
                  times.put(entry.getKey(), new ArrayList<Long>());
               }
               times.get(entry.getKey()).add(entry.getValue());
            }
         }
         for (Entry<String, List<Long>> entry : times.entrySet()) {
            writer.write(entry.getKey() + ": ");
            for (Long time : entry.getValue()) {
               writer.write(time.toString() + ", ");
            }
            writer.write("\n");
         }
         writer.flush();
         writer.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (IOException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }

   private class Run {
      public Run() {
         startTimes = new HashMap<String, Long>();
         elapsedTimes = new HashMap<String, Long>();
         elapsedTimes.put("Total workflow EN", new Long(0));
         elapsedTimes.put("Total workflow IA", new Long(0));
         elapsedTimes.put("ASR Input EN", new Long(0));
         elapsedTimes.put("ASR Input IA", new Long(0));
         elapsedTimes.put("ASR File Length EN", new Long(0));
         elapsedTimes.put("ASR File Length IA", new Long(0));
         elapsedTimes.put("Clarification EN", new Long(0));
         elapsedTimes.put("Clarification IA", new Long(0));
         elapsedTimes.put("Translation TTS EN", new Long(0));
         elapsedTimes.put("Translation TTS IA", new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.FORCED_ALIGNMENT_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.FORCED_ALIGNMENT_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.OOV_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.OOV_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.ANSWER_MERGER_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.ANSWER_MERGER_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.DIALOG_MANAGER_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.DIALOG_MANAGER_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.HOMOPHONE_DETECTOR_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.HOMOPHONE_DETECTOR_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.MT_ERROR_DETECTOR_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.MT_ERROR_DETECTOR_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.EN_TRANSLATION), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.IA_TRANSLATION), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.ASR_ERROR_DETECTOR_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.ASR_ERROR_DETECTOR_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.SENSE_DETECTOR_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.SENSE_DETECTOR_IA), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.TRANSLATION_PREPROCESS_EN), new Long(0));
         elapsedTimes.put(getNameForWorkflowType(WorkflowTaskType.TRANSLATION_PREPROCESS_IA), new Long(0));
      }


      public void startTiming(String task) {
         startTimes.put(task, System.currentTimeMillis());
      }

      public void addTime(String task, long timeInMillis) {
         elapsedTimes.put(task, timeInMillis);
      }

      public void stopTiming(String task) {
         if (startTimes.containsKey(task)) {
            long startTime = startTimes.get(task);
            long endTime = System.currentTimeMillis();
            elapsedTimes.put(task, endTime - startTime);
            startTimes.remove(task);
         }
      }

      public Map<String, Long> startTimes;
      public Map<String, Long> elapsedTimes;
   }


   private List<Run> runs;
}
