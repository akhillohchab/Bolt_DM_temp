/**
 * 
 */
package edu.columbia.bolt.dmdata;

import java.util.ArrayList;
import java.util.List;




/**
 * @author sveta
 *
 */
public class DialogueEntry {

//	SentenceData inputData;
	//0 if this is initial entry, 1,2,3 for the number of clarifications
	//int clarificationIndex = 0;
	public enum DMDecision{NONE, TRANSLATE, TRANSLATE_TRANSLITERATE,  //-> ACTION_TRANSLATE
		ASK_TO_DISAMBIGUATE, ASK_TO_SPELL, ASK_TO_REPHRASE_PART,ASK_TO_REPEAT_PART, CLARIFY,/* CONFIRM_NAME,*/ 
		CONFIRM_UTT, CONFIRM_SPELL, REJECT //->ACTION_CLARIFY
		};
	DMDecision dmDecision = DMDecision.NONE;

	//dm sets this to the index of the error that was addressed
	int errorIndexAddressed = -1;
	
	//rule id for a clarification question if not clarification, set to NULL
	String ruleID = null;
	
	List<DMOutputDataSegment> outputData = new ArrayList<DMOutputDataSegment>();	
	
	
	/**
	 * creates a dialogue entry with an error segment index set.
	 * @param errorSegmentIndex
	 */
	public DialogueEntry(int errorSegmentIndex) {
		super();
		errorIndexAddressed = errorSegmentIndex;
	}
	
	/**
	 * creates a dialogue entry with no error segment index set.
	 * @param errorSegmentIndex
	 */
	public DialogueEntry() {
		super();
	}
	
/*	public DialogueEntry(SentenceData inputData) {
		super();
		this.inputData = inputData;
	}
*/
	public DMDecision getDmDecision() {
		return dmDecision;
	}

	public void setDmDecision(DMDecision dmDecision) {
		this.dmDecision = dmDecision;
	}
	

	/**
	 * construct outputData with one string segment
	 * @param question
	 */
	public void setOutputData(String question) {
		this.outputData.clear();
		this.outputData.add(new DMOutputDataSegment(question));
	}
	
	/**
	 * construct outputData with one segment
	 * @param question
	 */
	public void setOutputData(List<DMOutputDataSegment> segs) {
		this.outputData.clear();
		this.outputData.addAll(segs);
	}

	
	/**
	 * print output string 
	 */
	public String toString(){
		String retstr = "(" + dmDecision.name() + ") " ;
		if(outputData==null) 
			return retstr + "NONE" ;
		
		for (int i = 0; i< outputData.size(); i++)
		{
			retstr += outputData.get(i).toString() + " ";
		}
		
		return retstr;
	}

	public List<DMOutputDataSegment> getOutputData() {
		return outputData;
	}

	public int getErrorIndexAddressed() {
		return errorIndexAddressed;
	}

	public void setErrorIndexAddressed(int errorIndexAddressed) {
		this.errorIndexAddressed = errorIndexAddressed;
	}

	public String getRuleID() {
		return ruleID;
	}

	public void setRuleID(String ruleID) {
		this.ruleID = ruleID;
	}


}
