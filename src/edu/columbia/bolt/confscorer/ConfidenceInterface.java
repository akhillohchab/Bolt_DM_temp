package edu.columbia.bolt.confscorer;

import java.util.Arrays;

import com.sri.bolt.message.BoltMessages.*;

import weka.core.*;

import edu.columbia.bolt.commondata.*;

/**
 * Confidence Scorer Interface
 * Uses features: ASR confidence, syntactic, prosodic features
 * to generate improved confidence score values using WEKA supervised classification.
 * 
 * The scores are generated for each utterance (TODO) and for each word in an utterance.
 * 
 * The generated confidence score estimates a probability that an utterance 
 * is recognized correctly.
 * To interpret the confidence score: if  above .5 utterance/word is recognized correctly
 * 
 * @author Philipp Salletmayr (p.salletmayr@student.tugraz.at)
 *
 */
 
public final class ConfidenceInterface implements BoltColumbiaInterface {	
	 
	 //the class holds 2 scorers, 1 for the utterance and 1 for the word level
	 //it also holds an instance-builder used to build classifiable data for scorers out of the SentenceData
	 private ProsodicScorer m_wordclassifier;
	 private ProsodicScorer m_uttclassifier;
	 private InstanceBuilderWrapper m_instancebuilder;
	 
/**
* Process input data according to configuration and create confidence values
* 
* @param conf
* Configuration to specify the feature set on which to evaluate data
* 
* @param sessiondata
* The Session Data holding history of an ongoing session
* 
* @return
* Updates sentence data with confidence scores for whole utterance and each word
*
*/
	 	
	 public SessionData process(ConfigurationParameters conf, SessionData sessiondata){
		 
		 //Create new wrapper to convert from BoltMessages to internally used data format
		 MessageWrapper wrapper = new MessageWrapper();
		 SentenceData data = new SentenceData();
		 
		//look up and get the current utterance
		// UtteranceData utterance = sessiondata.getUtterances(sessiondata.getUtterancesCount()-1);		 
			 
		 
			 
		 try{
			 
			 //decode current utterance into SentenceData object		 
			 data = wrapper.decode(sessiondata); 
			
			 //Initialize classifiers and the instance builder based on configuration set			
			 this.m_wordclassifier = new ProsodicScorer(conf.confScorerWordScoreWekaModelpath);
			 //TODO: use utterance classifier as well
			 this.m_uttclassifier = new ProsodicScorer(conf.confScorerUttsScoreWekaModelpath);
				
			 switch (conf.confScorer) {
				case ASRPOS:  
			 				  	this.m_instancebuilder = new InstanceBuilderASRPOS();
			 				  	break;
				case ASR:
			 					this.m_instancebuilder = new InstanceBuilderASR();
			 					break;
				case ASRPROSPOS: 
								this.m_instancebuilder = new InstanceBuilderASRPROSPOS();
								break;
			    default:	System.out.println("Invalid Feature Set parameter: " + conf.confScorer);  
			    			return null;
		    	}
		}catch (Exception e) {
			System.out.println("ERROR Columbia Classifier Loader:" + e.getMessage());
			e.printStackTrace();
		}
			
			//call the scorer and write the confidence for each utterance and word back to the SentenceData
		try{
			switch(conf.confStage){
				case TwoStage:			
							data.setUttConfidence((getUttConfidenceCorrect(data)));
							if(data.getUttConfidence() < 0.5){
								data.setcfConfidence(Arrays.asList((getWordConfidenceCorrect(data))));
							}else{
								int size = data.getWordsCurrentUtt().size();
								Double[] wordconf = new Double[size];
								for(int i=0; i<(size-1); i++)
									wordconf[i] = 1.0;
								data.setcfConfidence(Arrays.asList(wordconf));								
							}
							break;
				case OneStage:
							data.setcfConfidence(Arrays.asList((getWordConfidenceCorrect(data))));
							break;
			}
		} catch (Exception e) {
			System.out.println("ERROR Columbia Scorer:" + e.getMessage());
			e.printStackTrace();
		}
			
		//sessiondata.toBuilder().setUtterances(sessiondata.getUtterancesCount()-1, wrapper.encode(utterance, data));
		return wrapper.encode(sessiondata, data);
	}
	 
	 
	 /**
	  * Classify the data provided in the SentenceData Set.
	  * 
	  * @param data
	  * Sentence data containing all features specified by configuration	  *
	  */
		
	 private Double[] getWordConfidenceCorrect(SentenceData input) throws Exception{
		
		//build instance from InputData
		Instances data = m_instancebuilder.buildInstance(input);		
		
		//Double uttconf = uttclassifier.classify(data)[0];
		Double[] wordconf = new Double[input.getWordsCurrentUtt().size()];
		
		//run classifiers and get confidence score for the word being 'correct'
		for (int i = 0; i < input.getWordsCurrentUtt().size(); i++) {
    	    wordconf[i] = m_wordclassifier.classify(data,i)[0]; //index '0' refers to the confidence of the word/utterance being 'correct'. 
    	    													//index '1' would refer to the confidence being incorrect. both scores sum up to 1
		}		 												
									
		//return confidences (words only in this case)
		return wordconf;
				
}
	
	 private Double getUttConfidenceCorrect(SentenceData input) throws Exception{
		
		//build instance from InputData
		Instances data = m_instancebuilder.buildInstance(input);		
		
		Double uttconf = m_uttclassifier.classify(data)[0];
									
		//return confidences (words only in this case)
		return uttconf;
				
	}
	
}