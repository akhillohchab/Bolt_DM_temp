package edu.columbia.bolt.logic;

import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.dmdata.DialogueEntry.DMDecision;
import edu.columbia.bolt.logging.Log;

/**
 * Implements stub dialogue manager which always returns a question Did you say ...?
 * @author sstoyanchev
 *
 */
class DMLogicSimple implements DMLogicWrapper{
	
	Log m_log = new Log("DMLogicRuleBased");
	public void setVerbose(int verb){m_log.setVerbose(verb);}

	/**
	 * stub 
	 */
	public DialogueEntry decide(SentenceData in) throws SentenceDataException
	{
		DialogueEntry de = 
			new DialogueEntry(in.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CONFIRM_UTT);		
		de.setOutputData("Did you say " + in.getAsrHypothesis() + "?");

		return de;
		
	}
}
