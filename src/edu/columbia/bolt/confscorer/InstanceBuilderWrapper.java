package edu.columbia.bolt.confscorer;

import weka.core.*;

import edu.columbia.bolt.commondata.*;
import edu.columbia.bolt.logging.Log;


/**
 * Builds an instances file usable by weka. 
 * Each class implementing InstanceBuilderWrapper interface 
 * uses a different set of features.
 * 
 * @author Philipp Salletmayr
 *
 */
public interface InstanceBuilderWrapper {
	
		String[] m_stanford_postags = {"PRP$","FWFW","NNNN","NNSRB","NNCD","WDT","JJ","WP","PRPVB","RP","NNPNN","FW","CDNN","PRP","RB","FWNN","NNS","NNP","WRB","CCIN","EX","UH","VBG","VBD","IN","VBN","NNVBN","VBP","VBZ","NN","MD","CC","MDRB","CD","NNPS","WP$,FWCD","JJS","JJR","CCJJ","DT","TO","LS","LSMD","VB","PDT","RBS","RBR","XX","Null"};
		Log m_log = new Log("DMLogicRuleBased");
		
		 /**
		  * Builds Instances classifiable by weka classifiers
		  * 
		  * @param
		  * Sentence data containing all features relevant to classification
		  */		
		Instances buildInstance(SentenceData input);
}