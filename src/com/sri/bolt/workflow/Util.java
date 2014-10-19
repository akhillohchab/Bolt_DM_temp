package com.sri.bolt.workflow;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.RecognizerToBoltMessage;
import com.sri.bolt.service.RecognizerFactory;
import com.sri.bolt.state.ASRState;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.workflow.task.WorkflowTaskType;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Util {
   public static String getNameForWorkflowType(WorkflowTaskType type) {
      if (type == WorkflowTaskType.AUDIO) {
         return "Dynaspeak ASR Processing";
      } else if (type == WorkflowTaskType.FORCED_ALIGNMENT_EN) {
         return "Forced Alignment EN";
      } else if (type == WorkflowTaskType.FORCED_ALIGNMENT_IA) {
         return "Forced Alignment IA";
      } else if (type == WorkflowTaskType.OOV_EN) {
         return "OOV_EN";
      } else if (type == WorkflowTaskType.OOV_IA) {
         return "OOV_IA";
      } else if (type == WorkflowTaskType.CONF_SCORER) {
         return "Confidence Scorer";
      } else if (type == WorkflowTaskType.ANSWER_MERGER_EN) {
         return "Answer Merger EN";
      } else if (type == WorkflowTaskType.ANSWER_MERGER_IA) {
            return "Answer Merger IA";
      } else if (type == WorkflowTaskType.DIALOG_MANAGER_EN) {
         return "Dialog Manager EN";
      } else if (type == WorkflowTaskType.DIALOG_MANAGER_IA) {
         return "Dialog Manager IA";
      } else if (type == WorkflowTaskType.HOMOPHONE_DETECTOR_EN) {
         return "Homophone Detector EN";
      } else if (type == WorkflowTaskType.HOMOPHONE_DETECTOR_IA) {
         return "Homophone Detector IA";
      } else if (type == WorkflowTaskType.MT_ERROR_DETECTOR_EN) {
         return "MT Error Detector EN";
      } else if (type == WorkflowTaskType.MT_ERROR_DETECTOR_IA) {
         return "MT Error Detector IA";
      } else if (type == WorkflowTaskType.CLARIFICATION) {
         return "Clarification";
      } else if (type == WorkflowTaskType.EN_TRANSLATION) {
         return "English Translation";
      } else if (type == WorkflowTaskType.IA_TRANSLATION) {
         return "IA Translation";
      } else if (type == WorkflowTaskType.ASR_ERROR_DETECTOR_IA) {
         return "ASR Error Detector IA";
      } else if (type == WorkflowTaskType.ASR_ERROR_DETECTOR_EN) {
         return "ASR Error Detector EN";
      } else if (type == WorkflowTaskType.SENSE_DETECTOR_EN) {
         return "Sense Detector EN";
      } else if (type == WorkflowTaskType.SENSE_DETECTOR_IA) {
         return "Sense Detector IA";
      } else if (type == WorkflowTaskType.TRANSLATION_PREPROCESS_EN) {
           return "Translation Preprocess EN";
      } else if (type == WorkflowTaskType.TRANSLATION_PREPROCESS_IA) {
         return "Translation Preprocess IA";
      } else if (type == WorkflowTaskType.NAME_DETECTOR_EN) {
         return "Name Detector EN";
      } else if (type == WorkflowTaskType.NAME_DETECTOR_IA) {
         return "Name Detector IA IA";
      } else {
         return "";
      }
   }

   public static WorkflowTaskType getTypeForConfigName(String configName) {
      if (configName.equalsIgnoreCase("OOV_EN")) {
         return WorkflowTaskType.OOV_EN;
      } else if (configName.equalsIgnoreCase("OOV_IA")) {
         return WorkflowTaskType.OOV_IA;
      } else if (configName.equalsIgnoreCase("FA_EN")) {
         return WorkflowTaskType.FORCED_ALIGNMENT_EN;
      } else if (configName.equalsIgnoreCase("FA_IA")) {
         return WorkflowTaskType.FORCED_ALIGNMENT_IA;
      } else if (configName.equalsIgnoreCase("CONF")) {
         return WorkflowTaskType.CONF_SCORER;
      } else if (configName.equalsIgnoreCase("ANSW_EN")) {
         return WorkflowTaskType.ANSWER_MERGER_EN;
      } else if (configName.equalsIgnoreCase("ANSW_IA")) {
         return WorkflowTaskType.ANSWER_MERGER_IA;
      } else if (configName.equalsIgnoreCase("DM_EN")) {
         return WorkflowTaskType.DIALOG_MANAGER_EN;
      } else if (configName.equalsIgnoreCase("DM_IA")) {
         return WorkflowTaskType.DIALOG_MANAGER_IA;
      } else if (configName.equalsIgnoreCase("HD_EN")) {
         return WorkflowTaskType.HOMOPHONE_DETECTOR_EN;
      } else if (configName.equalsIgnoreCase("HD_IA")) {
         return WorkflowTaskType.HOMOPHONE_DETECTOR_IA;
      } else if (configName.equalsIgnoreCase("MT_EN")) {
         return WorkflowTaskType.MT_ERROR_DETECTOR_EN;
      } else if (configName.equalsIgnoreCase("MT_IA")) {
         return WorkflowTaskType.MT_ERROR_DETECTOR_IA;
      } else if (configName.equalsIgnoreCase("CL")) {
         return WorkflowTaskType.CLARIFICATION;
      } else if (configName.equalsIgnoreCase("TR")) {
         return WorkflowTaskType.EN_TRANSLATION;
      } else {
         return null;
      }
   }

   public static String formatForScreen(String text) {
      return text.trim().replaceAll("\\.\\_", ".");
   }

   public static String formatForScreen(String header, String text) {
      return "<b>" + header + "</b> " + formatForScreen(text);
   }

   public static WorkflowState buildWorkflowState(InteractionState interaction, WorkflowTaskType workflowStart, File sessionDataFile, File audioData, long time) throws Exception {
      byte[] fileBytes = new byte[(int) sessionDataFile.length()];
      BoltMessages.SessionData session;
      ByteArrayOutputStream byteStream;
      (new BufferedInputStream(new FileInputStream(sessionDataFile))).read(fileBytes);
      session = BoltMessages.SessionData.parseFrom(fileBytes);
      fileBytes = new byte[(int) audioData.length()];
      (new BufferedInputStream(new FileInputStream(audioData))).read(fileBytes);
      byteStream = new ByteArrayOutputStream();
      byteStream.write(fileBytes);

      ASRState state = new ASRState();
      state.buttonPressed();
      state.buttonReleased();
      state.setAsrComplete(new Date());
      state.setAudioData(byteStream);
      state.setAsrResultString(session.getUtterances(session.getUtterancesCount() -1).getRecognizer1Best());
      state.setLanguage(interaction.getLanguage());

      interaction.addASR(state, time);
      interaction.setSessionData(session);

      WorkflowState wfState = new WorkflowState();
      wfState.setInteractionState(interaction);
      wfState.setStartingTask(workflowStart);

      return wfState;
   }

   public static WorkflowState buildWorkflowState(InteractionState interaction, ASRState state, long time) {
      BoltMessages.UtteranceData.Builder data = RecognizerToBoltMessage.toUtteranceDataInitialPass(state.getRecogResult(), state.getSecondRecogResult()).toBuilder();
      // Currently, we want to run rescoring unless we had a spelling recognizer run
      if (state.getType() == RecognizerFactory.RecognizerType.SOCKET_EN) {
         data.setRunRescoring(true);
      } else if (state.getType() == RecognizerFactory.RecognizerType.SOCKET_IA) {
         data.setRunRescoring(true);
      } else {
         // spelling recognizer or fixed grammar recognizer
         data.setRunRescoring(false);
      }

      SessionData sessionData = interaction.getSessionData();
      SessionData.Builder sessionBuilder;
      int currentTurn = 0;
      if (sessionData == null) {
         sessionBuilder = BoltMessages.SessionData.newBuilder();
      } else {
         sessionBuilder = sessionData.toBuilder();
         currentTurn = sessionBuilder.getCurrentTurn() + 1;
      }
      sessionBuilder.setCurrentTurn(currentTurn);
      sessionBuilder.addUtterances(data);
      SessionData sessionResult = sessionBuilder.build();

      interaction.setSessionData(sessionResult);
      interaction.addASR(state, time);
      WorkflowState wfState = new WorkflowState();
      wfState.setInteractionState(interaction);

      return wfState;
   }

   public static List<WorkflowTaskType> getAllWorkflowTasks() {
      List<WorkflowTaskType> tasks = new ArrayList<WorkflowTaskType>();

      tasks.add(WorkflowTaskType.OOV_EN);
      tasks.add(WorkflowTaskType.OOV_IA);
      tasks.add(WorkflowTaskType.FORCED_ALIGNMENT_EN);
      tasks.add(WorkflowTaskType.FORCED_ALIGNMENT_IA);
      //tasks.add(WorkflowTaskType.CONF_SCORER);
      tasks.add(WorkflowTaskType.ANSWER_MERGER_EN);
      tasks.add(WorkflowTaskType.ANSWER_MERGER_IA);
      tasks.add(WorkflowTaskType.HOMOPHONE_DETECTOR_EN);
      tasks.add(WorkflowTaskType.HOMOPHONE_DETECTOR_IA);
      tasks.add(WorkflowTaskType.DIALOG_MANAGER_EN);
      tasks.add(WorkflowTaskType.DIALOG_MANAGER_IA);
      tasks.add(WorkflowTaskType.EN_TRANSLATION);
      tasks.add(WorkflowTaskType.IA_TRANSLATION);
      tasks.add(WorkflowTaskType.CLARIFICATION);
      tasks.add(WorkflowTaskType.MT_ERROR_DETECTOR_EN);
      tasks.add(WorkflowTaskType.MT_ERROR_DETECTOR_IA);
      tasks.add(WorkflowTaskType.ASR_ERROR_DETECTOR_EN);
      tasks.add(WorkflowTaskType.ASR_ERROR_DETECTOR_IA);
      tasks.add(WorkflowTaskType.SENSE_DETECTOR_EN);
      tasks.add(WorkflowTaskType.TRANSLATION_PREPROCESS_EN);
      tasks.add(WorkflowTaskType.TRANSLATION_PREPROCESS_IA);
      tasks.add(WorkflowTaskType.NAME_DETECTOR_EN);

      return tasks;
   }
}

