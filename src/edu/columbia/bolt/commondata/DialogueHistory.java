package edu.columbia.bolt.commondata;

import java.util.ArrayList;

import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.dmdata.DialogueEntry.DMDecision;

/**
 * store the previous interactions with the system.
 * @author sveta
 *
 */
public class DialogueHistory extends ArrayList<SentenceData>{
	
	/**
	 * 
	 * @return the size of the history 
	 */
	public int getDialogueCounter()
	{
		return this.size();
	}	
	
	/**
	 * 
	 * @return the last dialogue act
	 * @param num number acts back (1 = last)
	 */
	public DMDecision getLastDMDecision(int num)
	{
		if(this.size()<num) return DMDecision.NONE;
		
		SentenceData sd = this.get(this.size()-num);
		return sd.getDmEntry().getDmDecision();
			
	}
	
	/**
	 * return history object 1=last
	 * @param num
	 * @return
	 */
	public SentenceData getHistoryObject(int num)
	{
		if(this.size()<num) return null;
		return this.get(this.size()-num);
	}
	
	/**
	 * get the index of previously addressed error
	 * 
	 * @return the last dialogue act
	 * @param num number acts back (-1 = last)
	 */
	public int getLastErrorAddressed(int num)
	{
		if(this.size()<num) return -1;
		
		SentenceData sd = this.get(this.size()-num);
		return sd.getM_addressErrorSegmentIndex();
			
	}
	
	
	/**
	 * 
	 * @return the last dialogue act
	 */
	public DMDecision getLastDMDecision()
	{
		return getLastDMDecision(1);
			
	}
	
	/**
	 * 
	 * @return the last dialogue act
	 */
	public boolean isLastUserActionGoBack()
	{
		SentenceData sd = getHistoryObject(1);
		return sd.getUserActionGoBack();
			
	}
	
	/**
	 * 
	 * @return the last dialogue act
	 */
	public int getLastErrorAddressed()
	{
		return getLastErrorAddressed(1);
			
	}

	
	/**
	 * 
	 * @return TODO: the last utterance that was spoken
	 * currently just return the first utterance
	 */
	public SentenceData getLastFullUtterance()
	{
		
		if(this.size()==0) return null;
		
		return this.get(0);
			
	}	

}
