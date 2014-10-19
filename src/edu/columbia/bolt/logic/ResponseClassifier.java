package edu.columbia.bolt.logic;

import edu.columbia.bolt.commondata.SentenceData;

public class ResponseClassifier {

	public boolean isAffirmative(SentenceData sentData)
	{
		if(sentData.getAsrHypothesis().equals("yes"))
			return true;
		else
			return false;
				
	}
}
