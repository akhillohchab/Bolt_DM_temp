package edu.columbia.bolt.logic;

import java.util.Arrays;
import java.util.List;

import edu.columbia.bolt.commondata.ErrorSegment;
import edu.columbia.bolt.commondata.ErrorSegment.EntityCategory;
import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.logging.Log;

/**
 * 
 * The class is instantiated with SentenceData object. 
 * The constructor calls method to generate question.
 * result string and rule id are stored in class memeber variables which can be accessed by the caller
 * 
 * Implements clarification question generation according to the rules:
 * 
 * R0.0		Utt[1..error-1] what?
 * R0.1	utt contains verb after error	Utt[1..error-1] what utt [error+1 ..end]?
 * R0.2	POS/DEP(error) = VB/VC	Utt[1..error-1] do what?
 * R1.0	if Entity=LOCATION, prepi=index of preceeding(IN, TO,AT)	Utt[1..prepi] where?
 * R1.1	if Entity=PERSON, dep TAG error=OBJ	Utt[1..error-1] whom?
 * R1.2	if Entity=PERSON, dep TAG error=not OBJ	Utt[1..error-1] who?
 * R2.0	POS error=NN|JJ; TAG error=NMOD |APPO, parent tag=NN |NNS	which <parent word>
 * R3.0	error is in the first word	What about Utt[error+1..end]

 * @author sveta
 *
 */
public class DMClarificationQuestion {
	Log m_log = new Log("DMClarificationQuestion");
	
	//stores output 
	String generatedQuestion;
	//stores output
	String ruleID;

	public DMClarificationQuestion(SentenceData sent) throws SentenceDataException{
		super();
		constructClarification(sent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Constuct a clarification question based on sentenceData information
	 * use merged1best to construct a question
	 * 
	 * @param sent
	 * @return
	 */
	public void constructClarification(SentenceData sent) throws SentenceDataException
	{
		ErrorSegment errSeg = sent.getCurrentlyAddressedErrorSegment();
		if (errSeg==null)
		{
			throw new SentenceDataException("Currently Addressed error segment is NULL, can not construct clarification");
		}	

		String[] mergedHyp = sent.getWorkingHypothesis().split(" ");				
		
		String[] posTagsMerged = getPOSTagsWorkingHypothesis(sent);

		m_log.print(1,"constructClarification " + Arrays.toString(mergedHyp) + Arrays.toString(posTagsMerged));
		
		if(mergedHyp.length!=posTagsMerged.length)
			throw new SentenceDataException("DMClarificationQuestion:ConstructClarificaton POS tags and words arrays are different sizes");
		

		//if error starts after second word and contains content words before error and 
		//there are at least 3 words after the errorapply rule 3.0
		if((errSeg.getM_start()<=1 || !sent.hasContentWordsBeforeCurrentError()) && 
		(mergedHyp.length-errSeg.getM_end()) > 3)
		{
			String retQ =  clarifyRule3_0(mergedHyp, posTagsMerged, errSeg);
			if(retQ!=null) 
				{
				   this.generatedQuestion = retQ;
				   this.ruleID= "R3.0";
				   return;
				}
		}

		//R2.0	POS error=NN|JJ; DEP TAG error=NMOD |APPO, parent tag=NN |NNS	which <parent word>
		if((errSeg.getM_posTagHead().equals("NN") ||  errSeg.getM_posTagHead().equals("JJ")) &&
				(errSeg.getM_depLabelHead().startsWith("NMOD") || errSeg.getM_depLabelHead().startsWith("APPO")) &&
				errSeg.getDepWordIndex() >0 
		//TODO match parent POS tag to NN		
		)	
		{			
			String retQ =   clarifyRule2_0(mergedHyp, posTagsMerged, errSeg);
			if(retQ!=null)				
			{
				this.generatedQuestion = retQ;
				this.ruleID= "R2.0";
				return;
			}
		}

		 // R1.1	if Entity=PERSON, TAG error=OBJ	Utt[1..error-1] whom?
		 //R1.2	if Entity=PERSON, TAG error=not OBJ	Utt[1..error-1] who?
		if(errSeg.getM_neCategory() == EntityCategory.HUMAN)
			if(errSeg.getM_depLabelHead().equals("OBJ"))
			{
				String retQ =  clarifyRule1_2(mergedHyp, posTagsMerged, errSeg);
				if(retQ!=null)
				{
					this.generatedQuestion = retQ;
					this.ruleID= "R1.2";
					return;
				}
			}
			else
			{
				String retQ =   clarifyRule1_1(mergedHyp, posTagsMerged, errSeg);
				if(retQ!=null)
				{
					this.generatedQuestion = retQ;
					this.ruleID= "R1.1";
					return;
				}	
			}
	   //R1.0	if Entity=LOCATION, prepi=index of preceeding(IN, TO,AT)	Utt[1..prepi] where?						 
		if(errorIsLocation(mergedHyp, posTagsMerged, errSeg))
		{
			String retQ =   clarifyRule1_0(mergedHyp, posTagsMerged, errSeg);
			if(retQ!=null)
			{
				this.generatedQuestion = retQ;
				this.ruleID= "R1.0";
				return;
			}
		}

		// R0.2	POS/DEP(error) = VB/VC	Utt[1..error-1] do what?
		if(errSeg.getM_posTagHead().equals("VB") || errSeg.getM_depLabelHead().equals("VC"))
		{
			String retQ =   clarifyRule0_2(mergedHyp, posTagsMerged, errSeg);
			if(retQ!=null)
			{
				this.generatedQuestion = retQ;
				this.ruleID= "R0.2";
				return;
			}
		}
						 
		// R0.1	utt contains verb after error	Utt[1..error-1] what utt [error+1 ..end]?
		if(containsVerbAfterError(mergedHyp, posTagsMerged, errSeg))
		{
			String retQ =   clarifyRule0_1(mergedHyp, posTagsMerged, errSeg);
			if(retQ!=null) 			
			{
				this.generatedQuestion = retQ;
				this.ruleID= "R0.1";
				return;
			}
		}

		//defauld question:  R0.0		Utt[1..error-1] what?

		this.generatedQuestion = clarifyRule0_0(mergedHyp, posTagsMerged, errSeg);
		this.ruleID= "R0_0";
		return;
	    	
	}

	/**
	 * R0.0		Utt[1..error-1] what?
	 * @param sent
	 * @return
	 */
	public String clarifyRule0_0(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg){
		m_log.print(1, "applying R0.0"); 
		String questionString = "";

		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 
		
		questionString += " <prosody  pitch='+15Hz' volume='x-loud' >what</prosody> <break strength='medium' />?";
		
		return questionString;		
	}
	/**
	 * R0.2	POS/DEP(error) = VB/VC	Utt[1..error-1] do what?
	 * When was the WHAT contacted?

	 * @param sent
	 * @return
	 */
	public String clarifyRule0_2(String[] mergedHyp, String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R0.2"); 
		String questionString = "";
		
		//TODO: getM_startMerged()???
		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 
        
		if(!mergedHyp[errSeg.getM_start()-1].equals("to")) 
			questionString += "to "; 

		questionString += "do  <prosody  pitch='+15Hz' volume='x-loud' >what</prosody> <break strength='medium' />?";

		return questionString;		
	}

	/**
	 * R0.1	utt contains verb after error	Utt[1..error-1] what utt [error+1 ..end]?
	 * When was the WHAT contacted?

	 * @param sent
	 * @return
	 */
	public String clarifyRule0_1(String[] mergedHyp, String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R0.1"); 
		String questionString = "";
		
		//TODO: getM_startMerged()???
		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 

		questionString += "  <prosody  pitch='+15Hz' volume='x-loud' >what</prosody> <break strength='medium' />  ";


		//TODO: getM_endMerged()???
		for (int i = errSeg.getM_end()+1; i<mergedHyp.length; i++)
			questionString += mergedHyp[i] + " "; 

		questionString += "?";

		return questionString;		
	}

	/**
	 * R1.0	if Entity=LOCATION, prepi=index of preceeding(IN, TO,AT)	Utt[1..prepi] where?
	 * We will build where?
	 * @param sent
	 * @return
	 */
	public String clarifyRule1_0(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg){
		m_log.print(1, "applying R1.0"); 
		String questionString = "";
		//string until the error
		for (int i = 0; i<getIndexOfPreErrorPreposition(mergedHyp, mergedPOStags, errSeg); i++)
			questionString += mergedHyp[i] + " "; 

		questionString += "WHERE?";

		for (int i = errSeg.getM_end()+1; i<mergedHyp.length; i++)
			questionString += mergedHyp[i] + " "; 

		return questionString;		
	}


	/**
	 * R1.1	if Entity=PERSON, TAG error=OBJ	Utt[1..error-1] whom?
	 * I know your whom?

	 * @param sent
	 * @return
	 */
	public String clarifyRule1_1(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R1.1"); 
		String questionString = "";
		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 
		
		questionString += "WHOM?";
		return questionString;		
	}
	
	/**
     * R1.2	if Entity=PERSON, TAG error=not OBJ	Utt[1..error-1] who?
	 * I know your whom?

	 * @param sent
	 * @return
	 */
	public String clarifyRule1_2(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R1.2"); 
		String questionString = "";
		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 
		
		questionString += "WHO?";
		return questionString;		
	}
	
	/**
	 * R2.0	POS error=NN|JJ; TAG error=NMOD |APPO, parent tag=NN |NNS	which <parent word>
	 * Which car?

	 * @param sent
	 * @return
	 */
	public String clarifyRule2_0(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R2.0"); 
		String questionString = "";
		for (int i = 0; i<errSeg.getM_start(); i++)
			questionString += mergedHyp[i] + " "; 
		

		questionString = "which " ;
		int parentWordIndex = errSeg.getDepWordIndex();
		//TODO: get a parent word?
		//workaround - use next word
		if(mergedHyp.length>=parentWordIndex)
			questionString += mergedHyp[parentWordIndex];
		else 
			return null;
		
		questionString += 	"?";
		
		return questionString;		
	}
	/**
	 * R3.0	error is in the first word	What about Utt[error+1..end]
	 * What about <recognized part>?

	 * @param sent
	 * @return
	 */
	public String clarifyRule3_0(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg) {
		m_log.print(1, "applying R3.0"); 
		String questionString = "What about ";

		for (int i = errSeg.getM_end()+1; i< mergedHyp.length; i++)
			questionString += mergedHyp[i] + " "; 
		
		questionString += "?";
		return questionString;		
	}
	
	
	/**
	 *  return true if there is a verb after the error
	 *  a question is constructed using merged1best but
	 *  POS tags are used from rescored1best
	 *  get rescored1best using index specified in error segment
	 *  
	 * @param sent
	 * @return
	 */
	private boolean containsVerbAfterError(String[] mergedHyp,  String[] mergedPOStags, ErrorSegment errSeg)
	{
		for(int i = errSeg.getM_end()+1; i< mergedPOStags.length; i++)
			if(mergedPOStags[i].startsWith("VB"))
				return true;
		return false;
	}
	
	/**
	 * 
	 * @param sent
	 * @return true if error NE is set to be location
	 */
	private boolean errorIsLocation(String[] mergedHyp, String[] posTagsMerged, ErrorSegment errSeg)
	{
		return false;
	}

	/**
	 * 
	 * return index in the MERGED string, for the location of 
	 * preposition prior to the currentlyAddressed error 
	 * If preposition is not found, return index of beginning of the error 
	 * @param sent
	 * @return true if error NE is set to be location
	 */
	private int getIndexOfPreErrorPreposition(String[] mergedHyp, String[] mergedPOStags, ErrorSegment errSeg)
	{
		int indexErrorStart = errSeg.getM_start();
		return indexErrorStart;
	}
	
	/**
	 * generate array of POS tags of rescored1 best
	 * TODO: this will be rescored 1 best from one of the previous utterances
	 * specified by an index
	 * words inside any error segment, replace tags with predicted POS.
	 * @param str
	 * @return
	 */
	private String[] getPOSTagsWorkingHypothesis(SentenceData sentData)
	{
		List<String> taglist = sentData.getM_postagsWorkingUtt();
		
		//TODO: replace tags inside error segment with POS predictions		
		
		return (String [])taglist.toArray(new String[taglist.size()]); 
	}

	public String getGeneratedQuestion() {
		return generatedQuestion;
	}

	public String getRuleID() {
		return ruleID;
	}
	
}
