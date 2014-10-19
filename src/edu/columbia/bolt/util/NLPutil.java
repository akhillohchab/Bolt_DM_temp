package edu.columbia.bolt.util;

import java.util.Arrays;

import edu.columbia.bolt.commondata.ConfigurationParameters;

public class NLPutil {
	
	private NLPutil(){}

	public synchronized static NLPutil getInstance(){
		if (_instance==null)
			_instance = new NLPutil();
		return _instance;
	}

	static String[] m_stanford_postags = {"PRP$","FWFW","NNNN","NNSRB","NNCD","WDT","JJ","WP","PRPVB","RP","NNPNN","FW","CDNN","PRP","RB","FWNN","NNS","NNP","WRB","CCIN","EX","UH","VBG","VBD","IN","VBN","NNVBN","VBP","VBZ","NN","MD","CC","MDRB","CD","NNPS","WP$,FWCD","JJS","JJR","CCJJ","DT","TO","LS","LSMD","VB","PDT","RBS","RBR","XX","Null"};
	static String[] m_stanford_postags_content = {"NNNN","NNSRB","NNCD","JJ","WP","PRPVB","RP","NNPNN","FW","CDNN","FWNN","NNS","NNP","WRB","CCIN","EX","VBG","VBD","IN","VBN","NNVBN","VBP","VBZ","NN","MDRB","CD","NNPS","FWCD","JJS","JJR","CCJJ","LS","LSMD","VB","PDT"};

	private static NLPutil _instance;
	
	/**
	 * 
	 * @return true if tag is int he list of m_stanford_postags_content
	 */
	public  boolean isContentPOS(String tag)
	{
		if(Arrays.asList(m_stanford_postags_content).contains(tag))
			return true;
		return false;
	}

}
