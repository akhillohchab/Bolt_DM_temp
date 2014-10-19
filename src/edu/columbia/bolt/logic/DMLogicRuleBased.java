package edu.columbia.bolt.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.columbia.bolt.commondata.ConfigurationParameters;
import edu.columbia.bolt.commondata.ErrorSegment;
import edu.columbia.bolt.commondata.ErrorSegment.EntityCategory;
import edu.columbia.bolt.commondata.DialogueHistory;
import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.dmdata.DMOutputDataSegment;
import edu.columbia.bolt.dmdata.DMOutputDataSegment.DataType;
import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.dmdata.DialogueEntry.DMDecision;
import edu.columbia.bolt.logging.Log;
import edu.columbia.bolt.logic.ResponseClassifier;

/**
 * Implements rule-based Dialogue manager
 * 
If Counter of iterations (length of history) >3  goto Translate state
If userAction=GOBACK HandleGoBack state
If there is MtErrorSegment (see slide 4)
If there are no errors (or all errors are resolved) → go to Translate state
Identify the most prominent ASR error – with highest confidence.
If  segment length >50%  →  go to RejectState 
If previous DM action=ClarifyQ 
If conf_merged=HIGH → mark error as resolved, go to StartState
If conf_merged=MED → go to ConfirmUttState
If conf_merged=LOW → go to askRephrasePart
Else If previous DM action=AskToSpell and prev -1 DM action !=AskToSpell  //this is  a first AskToSpell
If conf_merged=HIGH → mark error as resolved, go to StartState 
If conf_merged=MED → go to ConfirmSpell (use spellling from errorsegment)
If conf_merged=LOW → go to AskToSpellState (using error segment from starting_utterance)
Else If previous DM action=AskToSpell and prev -1 DM action ==AskToSpell //second AskToSpell
If conf_merged=HIGH → mark error as resolved, go to StartState 
If conf_merged=MED → go to ConfirmSpellState
If merged=false or conf_merged=LOW → go to RejectState
Else If previous DM action=askRephrasePart || askRepeatPart
If conf_merged=HIGH → mark error as resolved, go to StartState
If conf_merged=MED → go to ConfirmUttState (use merged1Best) 
If conf_merged=LOW → go to askRephrasePart state
Else If previous DM action=ConfirmSpell 
If userFeedback = CONFIRMED and conf_merged=HIGH or MED → mark error as resolved, go to StartState
If userFeedback=REJECTECTED and conf_merged=HIGH or MED → go to AskToSpellState (using error segment from starting_utterance)
If  and conf_merged=LOW → got to ConfirmSpellState
Else If previous DM action=ConfirmUtt
If userFeedback = CONFIRMED and conf_merged=HIGH or MED → mark error as resolved, go to StartState
If userFeedback=REJECTECTED and conf_merged=HIGH or MED → go to go to prev-1 state (ask the same question as before confirmUtt)(using error segment from starting_utterance)
If  and conf_merged=LOW → goto ConfirmUtt (repeat confirmation question, used Merged1Best)
Else If previous DM action=ConfirmName //this happens after ClarifyQ+GoBack
If userFeedback = CONFIRMED  → goto AsktoSpell state
If userFeedback=REJECTECTED → goto RejectState
Else If previous DM action=askDisambiguate 
If conf_merged = HIGH/MED  → goto TranslateState state
If conf_merged = LOW → goto StartState  //address another error
//INITIAL STATE OR AFTER REJECT 
Else //prev action was NONE or Reject
If conf OOV is HIGH and entity != none and conf Entity is not LOW  → go to AskToSpell
If conf OOV is HIGH and entity != none and conf Entity is LOW  → go to askRephrasePart
If conf OOV is HIGH and Entity = NONE  → go to askRephrasePart
go to Clarif. Q  state //If error is nonOOV 
 * 
 * @author sstoyanchev
 *
 */
public class DMLogicRuleBased implements DMLogicWrapper{
	


	protected String REJECT_UTTERANCE = "Please repeat, I did not understand you";
	

	
	
	/**
	 * store dialogue history
	 */
	//protected DialogueHistory dialogueHistory = new DialogueHistory();
	
	/**
	 * set level of verbosity for logging 
	 */
	Log m_log = new Log("DMLogicRuleBased");
	public void setVerbose(int verb){m_log.setVerbose(verb);}

	
	public DMLogicRuleBased() {
		super();
		m_log.print(1,"Creating DMLogicRuleBased version " + ConfigurationParameters.getInstance().getReleaseVersion());
	}
	

	public enum ConfidenceBracket{HIGH, MEDIUM, LOW};
	
	/**
	 * 
	 * @param conf
	 * @return the bracket of this conf value
	 */
	ConfidenceBracket getConfBracketResolved(double conf)
	{
		if(conf>.66) return ConfidenceBracket.HIGH;
		if(conf>.33) return ConfidenceBracket.MEDIUM;
		return ConfidenceBracket.LOW;
	}
	
	/**
	 * According to Benoit's message on 07/26/12, confidence 
	 * threshold on confirm are .9, .8, .7
	 * @param conf
	 * @return the bracket of this conf value
	 */
	ConfidenceBracket getConfBracketConfirm(double conf)
	{
		if(conf>.9) return ConfidenceBracket.HIGH;
		if(conf>.8) return ConfidenceBracket.MEDIUM;
		return ConfidenceBracket.LOW;
	}
	/**
	 * interface method: 
	 * @param in input sentence Data
	 * @return DialogueEntry (action)
	 */
	public DialogueEntry decide(SentenceData in) throws SentenceDataException	{
		
		//check that mergeUtterance is set
		if(in.getWorkingHypothesis()==null)
			throw new SentenceDataException("MergedUtterance is not set");

		
		//call the start state
		DialogueEntry de = startState(in);
		
		in.setDmEntry(de);		
		//insert sentence data into dialogue history
		//dialogueHistory.add(in);
		
		return de;

	}
	
	
	
	/**
	 * logic described above the class definition
	 */
	protected DialogueEntry startState(SentenceData currentSentenceData) throws SentenceDataException
	{
		m_log.print(1,"Entering startState " + currentSentenceData);
		DialogueHistory dialogueHistory = currentSentenceData.getM_dialogHistory();
		m_log.print(1,"DialogueHistory size:" + dialogueHistory.size());


		//first check if the history is large enough, go to tranlsate state
		if(dialogueHistory.size()>=3)
			return translateState(currentSentenceData);
		
		//if user issued, go back command, execute it
		if(currentSentenceData.getUserActionGoBack()==true)
			return handleGoBackCommand(currentSentenceData, dialogueHistory);

			// NFA: force translate
			//previous statement: return translateState(currentSentenceData);
			//return alwaysTranslateState(currentSentenceData);
			
		//if there are no errors, translate
		//if(currentSentenceData.areAllErrorsCleared())
		//	return translateState(currentSentenceData);
		
		
		//if the error was not resolved, try another atempt at resolving it
		if(currentSentenceData.findPreviouslyAddressedError(dialogueHistory)==null)
			return addressNewError(currentSentenceData);
		else
			return continueAddressingUnresolvedError(currentSentenceData, dialogueHistory);
	}

	/**
	 * 
	 * @return
	 */
	private DialogueEntry handleGoBackCommand(SentenceData currentSentenceData,
			DialogueHistory dialogueHistory ) throws SentenceDataException
	{
		m_log.print(1,"Entering handleGoBackCommand " + currentSentenceData);

		//if this is a second go back in a row
		if(dialogueHistory.isLastUserActionGoBack())
		{
			m_log.print(1,"This is a secong go back in a row ");
			ErrorSegment currenterr = currentSentenceData.getCurrentlyAddressedErrorSegment();
			currenterr.setSkipFlag(true);
			return addressNewError(currentSentenceData);
		}
		
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.CLARIFY)
		{
			m_log.print(1,"Previous DM action was CLARIFY ");
			return askRepeatPartState(currentSentenceData);
		}	
		
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.ASK_TO_SPELL)
		{
			m_log.print(1,"Previous DM action was ASK_TO_SPELL ");
			ErrorSegment currenterr = currentSentenceData.getCurrentlyAddressedErrorSegment();
			currenterr.setSkipFlag(true);
			DialogueEntry de =  addressNewError(currentSentenceData);
			if(de!=null)
				return de;
			else
				return rejectState(currentSentenceData);			
		}	
		
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.ASK_TO_REPHRASE_PART)
		{
			m_log.print(1,"Previous DM action was ASK_TO_REPHRASE_PART ");
			return askToSpellState(currentSentenceData);
		}			
		
		m_log.print(1,"Previous DM action did not match checks ");
		return rejectState(currentSentenceData);
	}
	
	/**
	 * this fn is called when an error was not resolved in previous turn
	 * pick a dialogue act for addressing this error again
	 * @param currentSentenceData
	 * @return
	 */
	private DialogueEntry continueAddressingUnresolvedError(SentenceData currentSentenceData,
			DialogueHistory dialogueHistory  ) throws SentenceDataException
	{
		m_log.print(1,"Entering continueAddressingUnresolvedError " + currentSentenceData);

		
        double confResolved = currentSentenceData.getCurrentlyAddressedErrorSegment().getM_resolvedConf();
		//last move was ClarifyQ
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.CLARIFY)
		{			
			//SentenceData prevSentenceData = dialogueHistory.get(dialogueHistory.size()-1);
			if(getConfBracketResolved(confResolved)==ConfidenceBracket.HIGH)
			{
				throw new SentenceDataException("DMLogicRulebased:StartState: error is cleared, continue AddressUnresolvedError should not be called");
			}
			
			if(getConfBracketResolved(confResolved)==ConfidenceBracket.MEDIUM )
			{
				if(dialogueHistory.size()<2)
					return confirmUttState(currentSentenceData);
				else
					return translateState(currentSentenceData);
			}
			//low
				return askRephrasePartState(currentSentenceData);
		}
		
		//last move was ask to spell
		if(dialogueHistory.getLastDMDecision(1)==DialogueEntry.DMDecision.ASK_TO_SPELL) 
		{
			if(getConfBracketResolved(confResolved)==ConfidenceBracket.HIGH)
			{
				return addressNewError(currentSentenceData);
			}

			if(getConfBracketResolved(confResolved)==ConfidenceBracket.MEDIUM)
			{
				if(dialogueHistory.size()<2)
					return confirmUttState(currentSentenceData);
				else
					return translateState(currentSentenceData);
			}
			//low //ask at to spell at most twice and then go to reject 
			if(dialogueHistory.getLastDMDecision(2)==DialogueEntry.DMDecision.ASK_TO_SPELL)
				return rejectState(currentSentenceData);
			else 
				return askToSpellState(currentSentenceData);
		}

		//last move was ask to rephrase
		if(dialogueHistory.getLastDMDecision(1)==DialogueEntry.DMDecision.ASK_TO_REPHRASE_PART) 
		{
			//to mark an error segment as 'resolved', remove it from the list
			if(getConfBracketResolved(confResolved)==ConfidenceBracket.HIGH)
			{
				return addressNewError(currentSentenceData);
			}

			if(getConfBracketResolved(confResolved)==ConfidenceBracket.MEDIUM)
			{
				if(dialogueHistory.size()<2)
					return confirmUttState(currentSentenceData);
				else
					return translateState(currentSentenceData);
			}
			//low  
				return rejectState(currentSentenceData);
		}

		//last move was to confirm spell
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.CONFIRM_SPELL)
		{

			ErrorSegment errSeg = currentSentenceData.getCurrentlyAddressedErrorSegment();
			ErrorSegment.UserFeedback spellFeedback = errSeg.getUserFeedbackSpell();
			if(spellFeedback==ErrorSegment.UserFeedback.CONFIRM && 
					(getConfBracketConfirm(confResolved)==ConfidenceBracket.HIGH || 
							getConfBracketConfirm(confResolved)==ConfidenceBracket.MEDIUM))				
				//to mark an error segment as 'resolved', remove it from the list(getConfBracket(confMerged)==ConfidenceBracket.HIGH)
				{
					return addressNewError(currentSentenceData);
				}
			if(spellFeedback==ErrorSegment.UserFeedback.REJECT && 
					(getConfBracketResolved(confResolved)==ConfidenceBracket.HIGH || 
							getConfBracketResolved(confResolved)==ConfidenceBracket.MEDIUM))				
				{
					return askToSpellState(currentSentenceData);
				}
		    //confidence is low - not sure what user said, ask to confirm again
			return confirmSpellState(currentSentenceData);
			
		}
		

		//last move was to confirm utt
		if(dialogueHistory.getLastDMDecision()==DialogueEntry.DMDecision.CONFIRM_UTT)
		{

			if(currentSentenceData.getUserFeedbackUtt()==ErrorSegment.UserFeedback.CONFIRM && 
					(getConfBracketConfirm(confResolved)==ConfidenceBracket.HIGH || getConfBracketConfirm(confResolved)==ConfidenceBracket.MEDIUM))				
				//to mark an error segment as 'resolved', remove it from the list(getConfBracket(confMerged)==ConfidenceBracket.HIGH)
				{
					return addressNewError(currentSentenceData);
				}
			if(currentSentenceData.getUserFeedbackUtt()==ErrorSegment.UserFeedback.REJECT && 
					(getConfBracketResolved(confResolved)==ConfidenceBracket.HIGH || getConfBracketResolved(confResolved)==ConfidenceBracket.MEDIUM))				
				{
				if(dialogueHistory.getLastDMDecision(2)==DialogueEntry.DMDecision.ASK_TO_REPHRASE_PART)
					return askRephrasePartState(currentSentenceData);
				if(dialogueHistory.getLastDMDecision(2)==DialogueEntry.DMDecision.ASK_TO_SPELL)
					return askToSpellState(currentSentenceData);
				if(dialogueHistory.getLastDMDecision(2)==DialogueEntry.DMDecision.CLARIFY)
					return clarifQState(currentSentenceData);

				//otherwise reject (if previous-2 state is NONE or REJECT)
					return rejectState(currentSentenceData);
				}
			//else: conf= LOW -> repeat the confirmation question
			if(dialogueHistory.size()<2)
				return confirmUttState(currentSentenceData);
			else
				return translateState(currentSentenceData);
			
		}
		
		return addressNewError(currentSentenceData);
		
	}
	
	/**
	 * previous action on this error should be either NONE or REJECT		
	 * choose an error to address and pick a dialogue act for addressing this error
	 * 
	 * @param currentSentenceData
	 * @return
	 */
     private DialogueEntry addressNewError (SentenceData currentSentenceData) throws SentenceDataException{
    	 
 		m_log.print(1,"Entering addressNewError " + currentSentenceData);
		//get the error which is not yet resolved and has the highest confidence 
		//TODO: give higher priority to OOV and homophone errors over the rest of errors
		currentSentenceData.setM_addressErrorSegmentIndex(currentSentenceData.chooseErrorToAddress());
		//no errors to translate
		if(currentSentenceData.getM_addressErrorSegmentIndex()<0)
			return translateState(currentSentenceData);
		
		
		//if the size of error is more than 50% of the whole utterance, reject
		double ratio = currentSentenceData.getRatioErrorWords();
		m_log.print(1, "ratio or error words is " + ratio);
		if(ratio > .5)
			return rejectState(currentSentenceData);

		//only one of these should be set
		//oov ASR confidence, oov MT confidence, or ambig ASR or ambig MT
		
		int indexAddressedErrorSegment = currentSentenceData.getM_addressErrorSegmentIndex();
		//if there are no more errors to address, go to translate state
		if(indexAddressedErrorSegment == -1)
			return translateState(currentSentenceData);
		
		double confAsrOOV = -1;
		double confMTOOV = -1;
		double confASRambig = -1;
		double confMTambig = -1;
		double confEntity = -1;
		ErrorSegment errSegment= null;
		if( currentSentenceData.getErrorSegmentsFromAMU().get(indexAddressedErrorSegment)!=null)
		{
			errSegment = currentSentenceData.getErrorSegmentsFromAMU().get(indexAddressedErrorSegment);		
			confAsrOOV = errSegment.getM_confidenceAsrOOV();
			confMTOOV = errSegment.getConfidenceMTOOV();
			confASRambig = errSegment.getConfidenceAsrAmbiguous();
			confMTambig = errSegment.getConfidenceMTAmbiguous();
			confEntity = errSegment.getM_confidenceEntityCategory();
		}
		else
			throw new SentenceDataException(
				currentSentenceData.getErrorSegmentsFromAMU(), "Can not get error number" +
				indexAddressedErrorSegment );
		
		
		//if error is ASR OOV and entity ask to spell
		if( confAsrOOV>ConfigurationParameters.getInstance().ASR_OOV_CONF_THRESHOLD)
			if(
				errSegment.getM_neCategory()!=EntityCategory.NONE && 
				confEntity > ConfigurationParameters.getInstance().NE_CONF_THRESHOLD 
				//work-around - check for NNP instead of NE
				//errSegment.getM_posTagHead().equals("NNP")
				)
				{
					m_log.print(1, "Addressing ASR OOV NE error, confASr=" + confAsrOOV + " , confNE=" + confEntity);
					return askToSpellState(currentSentenceData);
				}
				else
				{
					m_log.print(1, "Addressing ASR OOV NonNE error, confAsrOOV=" + confAsrOOV + " , confNE=" + confEntity);
					return askRephrasePartState(currentSentenceData);
				}
		
		//if error is MT OOV and NE then ask to spell (Assuming MT would not be set if this was an ASR error)
		if( confMTOOV>ConfigurationParameters.getInstance().MT_OOV_CONF_THRESHOLD)
				if(
				errSegment.getM_neCategory()!=EntityCategory.NONE && 
				confEntity > ConfigurationParameters.getInstance().NE_CONF_THRESHOLD 
				//work-around - check for NNP instead of NE
				//errSegment.getM_posTagHead().equals("NNP")
				)
				{
					m_log.print(1, "Addressing MT OOV NE error, confMTOOV=" + confMTOOV + " , confNE=" + confEntity);
					return askToSpellState(currentSentenceData);
				}
				else
				{
					m_log.print(1, "Addressing MT OOV NonNE error, confMTOOV=" + confMTOOV + " , confNE=" + confEntity);
					return askRephrasePartState(currentSentenceData);
				}

		//if error is ASR Ambiguous then ask to disambiguate 
		if( confASRambig>ConfigurationParameters.getInstance().ASR_AMBIG_CONF_THRESOLD )
		{
			m_log.print(1, "Addressing ASR Ambig error, confAsrAmbig=" + confASRambig);
			setWordExplanationsToSpelling(currentSentenceData);
			return askTodisambiguateState(currentSentenceData);
		}
		
		//if error is MT Ambiguous then ask to rephrase 
		if( confMTambig>ConfigurationParameters.getInstance().MT_AMBIG_CONF_THRESOLD )
		{
			m_log.print(1, "Addressing MT Ambig error, confMTAmbig=" + confMTambig);
			return askRephrasePartState(currentSentenceData);
		}

		

		//if no OOV flags are set, the error must be non-oov, non-NE
		//ask clarification question
		m_log.print(1, "Addressing ASR non-OOV non-NE error" );
		return clarifQState(currentSentenceData);

		
	}
	
	/**
	 * ask user to spell
	 */
	protected DialogueEntry askToSpellState(SentenceData currentSentenceData)
	{
		m_log.print(1,"askToSpellState " + currentSentenceData);		
		
		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());

		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.ASK_TO_SPELL);		
		
		DMOutputDataSegment seg1 = new DMOutputDataSegment("Please Spell the word");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); //error index is 0
		DMOutputDataSegment seg3 = new DMOutputDataSegment("Using phonetic alphabet");
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);
		de.setOutputData(segs);
		return de;
		
	}	
	
	
	/**
	 * reject state rejects an utterance by telling user to repeat
	 */
	protected DialogueEntry rejectState(SentenceData currentSentenceData)
	{
		m_log.print(1,"rejectState " + currentSentenceData);
		m_log.print(2, Arrays.toString(new Exception().getStackTrace()));

		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
;
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.REJECT);		
		de.setOutputData(REJECT_UTTERANCE);

		return de;
		
	}
	
	/**
	 * Creates a clarification question
	 */
	protected DialogueEntry clarifQState(SentenceData currentSentenceData)  throws SentenceDataException
	{
		m_log.print(1, "clarifQState " + currentSentenceData);
		
		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CLARIFY);		
		DMClarificationQuestion dmClarifQ = new DMClarificationQuestion(currentSentenceData);
		
		String questionString = dmClarifQ.getGeneratedQuestion();
		if(dmClarifQ.getRuleID()!=null && !dmClarifQ.getRuleID().equals("R3.0"))
			questionString+= " Please provide the shortest possible answer.";

		de.setOutputData(questionString);
		de.setRuleID(dmClarifQ.getRuleID());
		
		return de;
		
	}

	/**
	 * Creates a clarification question, please rephrase <play segment>
	 */
	protected DialogueEntry askRephrasePartState(SentenceData currentSentenceData)
	{
		m_log.print(1,"askRephraseState " + currentSentenceData);		
		
		DialogueEntry de =
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		
		de.setDmDecision(DMDecision.ASK_TO_REPHRASE_PART);		
		

		DMOutputDataSegment seg1 = new DMOutputDataSegment("I did not understand when you said ");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); 
		DMOutputDataSegment seg3 = new DMOutputDataSegment("Please rephrase only the phrase ");
		DMOutputDataSegment seg4 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); 
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);
		segs.add(seg4);
		de.setOutputData(segs);
		return de;
		
	}	
	
	/**
	 * Creates a clarification question, please repeat <play segment>
	 */
	protected DialogueEntry askRepeatPartState(SentenceData currentSentenceData)
	{
		m_log.print(1,"askRephraseState " + currentSentenceData);		
		
		DialogueEntry de =
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		
		de.setDmDecision(DMDecision.ASK_TO_REPEAT_PART);		
		

		DMOutputDataSegment seg1 = new DMOutputDataSegment("I did not understand when you said ");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); 
		DMOutputDataSegment seg3 = new DMOutputDataSegment("Please repeat only the phrase ");
		DMOutputDataSegment seg4 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); 
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);
		segs.add(seg4);
		de.setOutputData(segs);
		return de;
		
	}	

	/**
	 * Confirm that an error segment is a name is a name
	 */
	protected DialogueEntry confirmSpellingState(SentenceData currentSentenceData)
	throws SentenceDataException
	{
		m_log.print(1,"confirmNameState" + currentSentenceData);		
		
		DialogueEntry de = 
		new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());

		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CONFIRM_SPELL);
		
		ErrorSegment currentErrorSeg = currentSentenceData.getCurrentlyAddressedErrorSegment();
		if(currentErrorSeg==null)
			throw new SentenceDataException(currentSentenceData.getErrorSegmentsFromAMU(), " failed to get error segment " );

		DMOutputDataSegment seg1 = new DMOutputDataSegment("You said");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(
				currentErrorSeg.getM_spell()); 
		DMOutputDataSegment seg3 = new DMOutputDataSegment("Is that correct?");
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);
		de.setOutputData(segs);
		return de;
		
	}
	
	
	/**
	 * Confirm that an error segment is a name is a name
	 *
	protected DialogueEntry confirmNameState(SentenceData currentSentenceData)
	{
		m_log.print(1,"confirmNameState" + currentSentenceData);		
		
		DialogueEntry de = new DialogueEntry();
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CONFIRM_NAME);
		
		DMOutputDataSegment seg1 = new DMOutputDataSegment("Is");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); //error index is 0
		DMOutputDataSegment seg3 = new DMOutputDataSegment("a name? Please say yes or no.");
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);
		de.setOutputData(segs);
		return de;
		
	}
	*/
	/**
	 * Confirm utterance 
	 */
	protected DialogueEntry confirmUttState(SentenceData currentSentenceData)
	{
		m_log.print(1,"confirmUttState " + currentSentenceData);		

		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CONFIRM_UTT);		
		de.setOutputData("Did you say " + currentSentenceData.getWorkingHypothesis() + "?");

		return de;
		
	}
		
	/**
	 * Confirm previously specified spelling
	 */
	protected DialogueEntry confirmSpellState(SentenceData currentSentenceData)
	{
		m_log.print(1,"confirmSpellState " + currentSentenceData);		

		DialogueEntry de =
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
;
		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		de.setDmDecision(DMDecision.CONFIRM_SPELL);		
		String spellstring = currentSentenceData.getCurrentlyAddressedErrorSegment().getM_spell();
		de.setOutputData("You said " + spellstring + "is that right?");
		
		return de;
		
	}
	
	/**
	 * modifies currently addressed error in currentSentenceData to
	 * include ambiguousWordExplanations with the values of spelling
	 * 
	 * @param currentSentenceData
	 */
	protected void setWordExplanationsToSpelling(SentenceData currentSentenceData)
	{
		ErrorSegment errSegment = currentSentenceData.getCurrentlyAddressedErrorSegment();

		List<String> ambigWords = errSegment.getAmbiguousWords();
		List<String> ambigWordsExplanation = new ArrayList<String>();
		
		ambigWordsExplanation = new ArrayList<String>(ambigWords.size());
		
		for (String w: ambigWords)
		{
			String spellStr = "<prosody volume='loud' rate='.75'>";
			
			for(int i = 0; i< w.length(); i++)
				{
				String character = w.substring(i, i+1);				
				spellStr += " <break strength='medium' />" + 
							character + " ";
				}
			
			spellStr += "</prosody>";
			ambigWordsExplanation.add(spellStr);
		}
		errSegment.setAmbiguousExplanations(ambigWordsExplanation);
		
	}
	
	
	/**
	 * enter this state to disambiguate an error
	 * ambiguousWords and ambiguousWordsExplanations in the currently addressed 
	 * error segment should be set 
	 * @param currentSentenceData
	 * @return
	 */
	protected DialogueEntry askTodisambiguateState(SentenceData currentSentenceData) throws SentenceDataException
	{
		m_log.print(1,"disambiguateState " + currentSentenceData);		
		
		ErrorSegment errSegment = currentSentenceData.getCurrentlyAddressedErrorSegment();
		
		List<String> ambigWords = errSegment.getAmbiguousWords();
		List<String> ambigWordsExplanation = errSegment.getAmbiguousExplanations();
		
		if(ambigWords==null)  
			throw new SentenceDataException("AmbiguousWords list is null");
		if( ambigWordsExplanation==null)
			throw new SentenceDataException("AmbiguousWordsExplanations list is null");
		if( ambigWordsExplanation.size()!=ambigWords.size())
			throw new SentenceDataException("AmbiguousWordsExplanations size="+ ambigWordsExplanation.size() +
					                        "Ambiguouswords size = " + ambigWords.size() + " are not equal");
		
		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());
		
		de.setDmDecision(DMDecision.ASK_TO_DISAMBIGUATE);			
		

		DMOutputDataSegment seg1 = new DMOutputDataSegment("Ambiguous word  ");
		DMOutputDataSegment seg2 = new DMOutputDataSegment(currentSentenceData.getM_addressErrorSegmentIndex()); 
		
		String segmentStr = "";

		for(int i = 1; i <= ambigWords.size(); i++)
		{
			segmentStr += "Please say <break strength=medium/> <prosody volume='loud'> " + i + 
			              "</prosody> <prosody rate='.85'> if you mean</prosody> " + 
			              ambigWordsExplanation.get(i-1) + ". ";
		}
		
		DMOutputDataSegment seg3 = new DMOutputDataSegment(segmentStr);
		
		List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
		segs.add(seg1);
		segs.add(seg2);
		segs.add(seg3);

		de.setOutputData(segs);
		return de;


		
	}

	
	/**
      * If there are no errors:
      *    if merged hypothesis exists, translate merged hypothesis
      *    else pass asr hypothesis
      * If an error is resolved and contains spelling, go to TranslateTransliterate state
      * If an error is resolved pass merged1Best
      * currently handles only one error
      * TODO: handle multiple errors situation
	 */
	protected DialogueEntry translateState(SentenceData currentSentenceData) throws SentenceDataException
	{
		m_log.print(1,"translateState " + currentSentenceData);		
		//if there are no errors index = -1
		int addressedErrorIndex = -1;
		if(currentSentenceData.getCurrentlyAddressedErrorSegment()!=null)
			addressedErrorIndex = currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList();
		
		DialogueEntry de = 
			new DialogueEntry(addressedErrorIndex);

						
		List<ErrorSegment> errSegs = currentSentenceData.getErrorSegmentsFromAMU();
		//there are no errors
		if(errSegs.size()==0)
		{			
			de.setDmDecision(DMDecision.TRANSLATE);	
			//if merged hypothesis exists, use merged hypothesis, else use asr
			if(currentSentenceData.getWorkingHypothesis()!=null)
				de.setOutputData(currentSentenceData.getWorkingHypothesis());
			else
			if(currentSentenceData.getAsrHypothesis()!=null)
				de.setOutputData(currentSentenceData.getAsrHypothesis());			
			else
				//neither merged nor asr hypothesis is set, throw an exception
				throw new SentenceDataException("Neither merged_hypothesis nor asr(rescored)_hypothesis were set");
			return de;
		}

		//get 0th segment (TODO: iterate over all segments)
		ErrorSegment currentErrorSeg = currentSentenceData.getCurrentlyAddressedErrorSegment();
		if(currentErrorSeg==null)
			throw new SentenceDataException(currentSentenceData.getErrorSegmentsFromAMU(), " failed to get error segment " );

		//error exists, check if its spelling
		if(currentErrorSeg.isM_resolved() && currentErrorSeg.getM_spell()!=null)
			return translateTransliterateState(currentSentenceData);
		else		
		{			
			de.setDmDecision(DMDecision.TRANSLATE);		
			if(currentSentenceData.getWorkingHypothesis()!=null)
				de.setOutputData(currentSentenceData.getAsrHypothesis());			
			else
				//neither merged nor asr hypothesis is set, throw an exception
				throw new SentenceDataException("merged_hypothesis was not set");

			return de;
		}
						
		
	}
	
	/**
	 * create a DialogueEntry for the transliteration, 
	 * dialogue entry: segment1= asr before error, segment2=error index, segment3=asr after error 
	 */
	protected DialogueEntry translateTransliterateState(SentenceData currentSentenceData)
	{
		m_log.print(1,"translateTransliterateState " + currentSentenceData);		
			
		DialogueEntry de = 
			new DialogueEntry(currentSentenceData.getCurrentlyAddressedErrorSegment().getIndexInTheList());

		//if previous output was CONFIRM, CLARIFY, or REPEAT, assume utterance is clarification
		
		if(currentSentenceData.getErrorSegmentsFromAMU().size()==0)
		{
			new Exception("ERROR in translateTransliterateState: No Error Segment").printStackTrace();
		}
			
		de.setDmDecision(DMDecision.TRANSLATE_TRANSLITERATE);	
				
		int addressErrorIndex = currentSentenceData.getM_addressErrorSegmentIndex();
		DMOutputDataSegment seg1 = new DMOutputDataSegment(currentSentenceData.getWorkingHypothesisBeforeError(addressErrorIndex));
		DMOutputDataSegment seg2 = new DMOutputDataSegment(addressErrorIndex); 
		DMOutputDataSegment seg3 = new DMOutputDataSegment(currentSentenceData.getWorkingHypothesisAfterError(addressErrorIndex));
				
		List<DMOutputDataSegment> segList = new ArrayList<DMOutputDataSegment>();
		segList.add(seg1);
		segList.add(seg2);
		segList.add(seg3);
		
		de.setOutputData(segList);
		return de;
	}
	
}
