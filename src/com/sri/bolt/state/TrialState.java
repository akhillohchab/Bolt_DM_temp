package com.sri.bolt.state;

import com.sri.bolt.App;
import com.sri.bolt.EvalType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.xml.Dsegment;
import com.sri.bolt.xml.Event;
import com.sri.bolt.xml.Logfile;
import com.sri.bolt.xml.Team;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class TrialState {
   public TrialState(String trialId) {
      interactions = new ArrayList<InteractionState>();
      startTime = new Date();
      this.trialId = trialId;
   }

   public void startNewInteraction(Language lang) {
      int numClarificationAudio = 0;
      for (InteractionState interaction : interactions) {
         numClarificationAudio += interaction.getNumClarificationAudioFiles();
      }
      interactions.add(new InteractionState(trialId, lang, interactions.size() + 100, numClarificationAudio, startTime));
   }

   public InteractionState getCurrentInteraction() {
      if (interactions.size() == 0) {
         return null;
      }

      return interactions.get(interactions.size() - 1);
   }

   public void setCurrentInteraction(InteractionState state) {
      interactions.set(interactions.size() - 1, state);
   }

   public void onStartASR(Language lang) {
      if (getCurrentInteraction() == null || getCurrentInteraction().isInteractionFinished()) {
         startNewInteraction(lang);
      }
   }

   public TranslationState getLastTranslation() {
      TranslationState state = null;
      for (ListIterator<InteractionState> itr = interactions.listIterator(interactions.size()); itr.hasPrevious();) {
         InteractionState interaction = itr.previous();
         if (interaction.isInteractionFinished() && interaction.getTranslationState() != null) {
            state = interaction.getTranslationState();
            break;
         }
      }

      return state;
   }

   public String getTrialId() {
      return trialId;
   }

   public void rollback() {
      if (interactions.size() != 0) {
         interactions.get(interactions.size() - 1).rollbackTurn();
      }
   }

   public void writeTrialSummaries() {
      try {
         File xmlOutputFile = new File(Util.getFileName(trialId, "", startTime) + ".xml");
         Logfile log = new Logfile();
         log.setTrialid(trialId);

         XMLGregorianCalendarImpl impl = new XMLGregorianCalendarImpl();
         GregorianCalendar startTime = new GregorianCalendar();
         startTime.setTime(this.startTime);
         log.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));

         GregorianCalendar date = new GregorianCalendar();
         date.setTime(new Date());
         log.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(date));

         if (App.getApp().getEvalType() == EvalType.WITH_CLARIFICATION) {
            log.setEvent(Event.BOLT_BC_P_2);
         } else if (App.getApp().getEvalType() == EvalType.NO_CLARIFICATION) {
            log.setEvent(Event.BOLT_C_P_2);
         } else if (App.getApp().getEvalType() == EvalType.ACTIVITY_B_RETEST) {
            log.setEvent(Event.BOLT_BOFFLINE_P_2);
         }
         log.setTeam(Team.SRI);

         for (InteractionState interaction : interactions) {
            Dsegment dseg = interaction.getDsegment(this.startTime);
            log.getDsegment().add(dseg);
         }

         JAXBContext context = JAXBContext.newInstance(Logfile.class);
         Marshaller marshaller = context.createMarshaller();
         marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
         marshaller.marshal(log, xmlOutputFile);

         if (interactions.size() != 0 && App.getApp().getProps().getProperty("FullLogging", "true").equals("true")) {
            File summaryOutputFile = new File(App.getApp().getRunDir().getPath() + "/" + (trialId) + ".summary");
            FileOutputStream outputStream = new FileOutputStream(summaryOutputFile);
            List<Language> languages = new ArrayList<Language>();
            List<SessionData> sessionDatas = new ArrayList<SessionData>();
            List<List<SessionData>> uwSessionDatas = new ArrayList<List<SessionData>>();
            List<List<SessionData>> asrSessionDatas = new ArrayList<List<SessionData>>();
            for (InteractionState state : interactions) {
               languages.add(state.getLanguage());
               uwSessionDatas.add(state.getUWSessionDatas());
               sessionDatas.add(state.getSessionData());
               asrSessionDatas.add(state.getASRSessionDatas());
            }
            com.sri.bolt.message.Util.outputSessionDataSummary(outputStream, sessionDatas, languages, uwSessionDatas, asrSessionDatas, trialId);
            outputStream.close();
            outputStream.flush();
         }

      } catch (Exception e) {
         logger.error("Exception writing trial summaries: " + e.getMessage(), e);
      }
   }

   private String trialId;
   private Date startTime;
   private List<InteractionState> interactions;
   private static final Logger logger = LoggerFactory.getLogger(TrialState.class);
}
