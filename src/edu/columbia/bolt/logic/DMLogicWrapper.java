package edu.columbia.bolt.logic;

import edu.columbia.bolt.commondata.DialogueHistory;
import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.dmdata.DialogueEntry;

public interface DMLogicWrapper {

	

	public void setVerbose(int verbose);

	/**
	 * based on the input, decide what the output should be
	 * @param in
	 * @return
	 */
	DialogueEntry decide(SentenceData in) throws SentenceDataException;
}
