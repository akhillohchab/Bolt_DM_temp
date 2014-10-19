package com.sri.bolt.service;

import java.util.Properties;

import com.sri.bolt.message.BoltMessages.SessionData;

import edu.columbia.bolt.commondata.ConfigurationParameters;
import edu.columbia.bolt.confscorer.ConfidenceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfidenceScorerServiceClient implements SessionDataServiceClient {
   public ConfidenceScorerServiceClient(Properties props) {

      confParams = ConfigurationParameters.getInstance();
      confParams.confScorerWordScoreWekaModelpath = props.getProperty("ConfScorerWordScoreWekaModelpath");
      confParams.confScorerUttsScoreWekaModelpath = props.getProperty("ConfScorerUttsScoreWekaModelpath");
      init();
   }

   @Override
   public void init() {
      confidenceScorer = new ConfidenceInterface();
   }

   @Override
   public void reinit() {
   }

   @Override
   public void cleanup() {
      // TODO Auto-generated method stub

   }
   
   @Override
   public SessionData checkInput(SessionData data) {
      //TODO add error checking
      return data;
   }

   public SessionData process(SessionData data) {
      SessionData checkedData = checkInput(data);
      int numTry = 0;
      while (numTry < MAX_RETRIES) { 
         try {
            return confidenceScorer.process(confParams, checkedData);
         } catch (Throwable t) {
            logger.error("Confidence Scorer process failed: " + t.getMessage() + ". Retrying", t);
            init();
            numTry++;
         }
      }
      
      return null;
   }

   private static final Logger logger = LoggerFactory.getLogger(ConfidenceScorerServiceClient.class);
   ConfidenceInterface confidenceScorer;
   ConfigurationParameters confParams;
   static final int MAX_RETRIES = 2;
}
