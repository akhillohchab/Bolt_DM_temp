package edu.columbia.bolt.commondata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.sri.bolt.message.BoltMessages.*;

import edu.columbia.bolt.dmdata.DMOutputDataSegment;
import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.dmdata.DialogueEntry.DMDecision;




/**
 * This class is used to convert data from 'BoltMessages.UtteranceData' to 'SentenceData' and vice versa.
 */

public final class MessageWrapper {
	
	public SentenceData decode(SessionData protoSessionData) throws SentenceDataException{
		return decode(protoSessionData, true);
	}
		
/**
  * Creates a new SentenceData structure, sets last utterance to the sentence data
  * for each history utterance, add them to the history
  * 
  * the last utterance in sessiondata corresponds to sentenceData
  * all previous utterances in sessiondata are set to history
  * 
  * @param protoSessionData
  * @return the new SenteceData
  */
	public SentenceData decode(SessionData protoSessionData, boolean decodeForDm) throws SentenceDataException{
				
			//get last utterance from sessionData
			UtteranceData protoLastUtt = protoSessionData.getUtterances(protoSessionData.getUtterancesCount()-1);	
			DialogueHistory hist = null;
			if (decodeForDm){
				hist = new DialogueHistory();
				//add most recent first
				for (int i = protoSessionData.getUtterancesCount()-2; i>=0; i--)
					{				
						//do not care about history of non-last
						hist.add(decodeSentenceDataForUtterance(new DialogueHistory(), protoSessionData.getUtterances(i)));
					}		
			}	
			//create sentence data with history
			SentenceData sdata = decodeSentenceDataForUtterance(hist, protoLastUtt, decodeForDm);
			
		return sdata;
}
	
	protected SentenceData decodeSentenceDataForUtterance(DialogueHistory history, UtteranceData protoUtt) throws SentenceDataException{
		return decodeSentenceDataForUtterance(history, protoUtt, true);
	}
	
	/**
	* Returns sentence data object that corresponds to protoUtt object
	* @param protoUtt utterance object from protocol message
	* @return sentenceData for one utterance
	*/
protected SentenceData decodeSentenceDataForUtterance(DialogueHistory history, UtteranceData protoUtt, boolean decodeForDm) throws SentenceDataException
{
		
		//create all List objects required by the SentenceData structure
		List<String> words = new ArrayList<String>();
		List<String> postags = new ArrayList<String>();
		List<String> oovtags = new ArrayList<String>();
		
		List<Double> oovConf = new ArrayList<Double>();
		List<Double> asrConf = new ArrayList<Double>();
		List<Double> parseConf = new ArrayList<Double>();
		List<Double>  neConf = new ArrayList<Double>();
		
		List<Double>  duration = new ArrayList<Double>();
		List<Double>  f0min = new ArrayList<Double>();
		List<Double>  f0max = new ArrayList<Double>();
		List<Double>  f0mean = new ArrayList<Double>();
		List<Double>  f0stdev = new ArrayList<Double>();
		List<Double>  engmin = new ArrayList<Double>();
		List<Double>  engmax = new ArrayList<Double>();
		List<Double>  engstdev = new ArrayList<Double>();
		List<Double>  engmean = new ArrayList<Double>();
		List<Double>  vcd2tot = new ArrayList<Double>();
				
		//create empty SentenceData object and set its history
		SentenceData sdata = new SentenceData();
		sdata.setM_dialogHistory(history);

		//read current utterance as understood by ASR
		String asrHyp = "";
		if(protoUtt.hasRescored1Best()) 
			asrHyp = protoUtt.getRescored1Best();
		
		
		int temp_errorIndex = -1;
		
		//split current utterance to single words
		String [] wordasr = asrHyp.split("\\s+");
		int wordcount = 0;
		double starttime, endtime;
		int oovs,oovstart, oovend;
		oovs = protoUtt.getAsrOovAnnotationsCount();
		if(oovs > 0 && oovs < 2 ){
			oovstart = protoUtt.getAsrOovAnnotations(0).getStartIndex();
			oovend = protoUtt.getAsrOovAnnotations(0).getEndIndex();
		}else{
			oovstart = -1;
			oovend = -2;
		}
		
		//loop through every word saved in 'repeated WordLevelAnnotation'
		//extract features and add to List objects
		for (WordAnnotation protoWord: protoUtt.getWordLevelAnnotationsList()) {
				wordcount = protoWord.getWordIndex();
				words.add(wordasr[wordcount]);
				starttime = protoWord.getStartOffsetSeconds();
				endtime = protoWord.getEndOffsetSeconds();
				duration.add((endtime - starttime)+1);
		        asrConf.add(protoWord.getAsrPosterior().getValue());
		        parseConf.add(protoWord.getParserConfidence().getValue());
		        oovConf.add(protoWord.getUwConfidence().getValue());
		        
		        //get pos tags from a working hypothesis if it exists		        
		        
		        postags.add(protoWord.getPosTag().getValue());
		        if(wordcount >= oovstart && wordcount <= oovend){
		        	oovtags.add("OOV");
		        }else{
		        	oovtags.add("NOOOV");
		        }				
				f0mean.add(protoWord.getF0Average().getValue());
				f0max.add(protoWord.getF0Maximum().getValue());
				f0min.add(protoWord.getF0Minimum().getValue());
				f0stdev.add(protoWord.getF0Stdev().getValue());
				engmean.add(protoWord.getRmsAverage().getValue());
				engmax.add(protoWord.getRmsMaximum().getValue());
				engmin.add(protoWord.getRmsMinimum().getValue());
				engstdev.add(protoWord.getRmsStdev().getValue());
				vcd2tot.add(protoWord.getVoicedProportion().getValue());
		      }
		
		//fill SentenceData structure with word level information
		sdata.setAsrHypothesis(asrHyp);
		sdata.setWordsCurrentUtt(words);
		sdata.setAsrConfidence(asrConf);
		sdata.setParseConfidence(parseConf);
		sdata.setOovConfidence(oovConf);
		sdata.setPostags(postags);		
		sdata.setOovTags(oovtags);
		sdata.setDuration(duration);
		sdata.setProsodicsForWords(f0max, f0min, f0mean, f0stdev, engmax, engmin, engmean, engstdev, vcd2tot);
		
		if (!decodeForDm)
			return sdata;
			
		sdata.setErrorSegmentsFromAMU(decodeErrorSegmentsFromAMU(protoUtt));
		
		AnswerMergerOutput  protoAnsMergerOut = null;
		if(protoUtt.hasAnswerMergerOutput())
		{
		protoAnsMergerOut = protoUtt.getAnswerMergerOutput();
			
		//from answer merger output, get 
		// *workingutterance 
		// ?mergeraction (do we need it?)
		// *word index pointers
		// *go back attribute
		// *user_feedback attribute
		if(protoAnsMergerOut.hasWorkingUtterance() )
		{
			String workingUtt = protoAnsMergerOut.getWorkingUtterance();
			int workUttLength = workingUtt.split("\\s+").length;
			
			if(workUttLength!= protoAnsMergerOut.getWordIndexPointersCount())
				throw new SentenceDataException("Length of working utterance=" + workUttLength +
						" wordIndexPointerCount=" +protoAnsMergerOut.getWordIndexPointersCount() + 
						" MUST BE EQUAL");
			
			List<Integer> utteranceIndexList = new ArrayList<Integer>();
			List<Integer> wordIndexList = new ArrayList<Integer>();
			List<String> workingUttPosTags = new ArrayList<String>();

			sdata.setWorkingHypothesis(workingUtt);
			for(int i = 0; i < workUttLength; i++)
			{
				int uttIndex, wordIndex;
				if(protoAnsMergerOut.getWordIndexPointers(i).hasUtteranceIndex())			
					uttIndex = protoAnsMergerOut.getWordIndexPointers(i).getUtteranceIndex();									
				else
					throw new SentenceDataException("Length of utterance index differs from workingUtterance");
			
				if(protoAnsMergerOut.getWordIndexPointers(i).hasWordIndex())
					wordIndex = protoAnsMergerOut.getWordIndexPointers(i).getWordIndex();
				else
					throw new SentenceDataException("Length of utterance index differs from workingUtterance");		
				
				//get pos tags in the history
				if(history==null || uttIndex==history.size()){
					// NFA: this statement is going to throw an exception every time the rescored 1best is shorter than working utterance
					workingUttPosTags.add(postags.get(wordIndex));
				}
					else
				{
					
					SentenceData histData = history.getHistoryObject(history.size()-uttIndex-1);
					if(histData==null) 
						throw new SentenceDataException("WordIndexPointer: utterance Index=" + uttIndex + " but the size of history is " + history.size());
					
					if(histData.getPostags().size()<wordIndex)
						throw new SentenceDataException("WordIndexPointer: wordIndex is out of bounds");
					
					workingUttPosTags.add(histData.getPostags().get(wordIndex));
				}
					
				
				utteranceIndexList.add(uttIndex);
				wordIndexList.add(wordIndex);
				
			}
			
			List<String> workingUttWords = Arrays.asList(workingUtt.split("\\s+"));
			
			sdata.setM_wordsWorkingUtt(workingUttWords);
			sdata.setM_postagsWorkingUtt(workingUttPosTags);
			sdata.setPointerUtteranceAndWordIndex(utteranceIndexList, wordIndexList);
		}
		else
			throw new SentenceDataException("DM MessageWrapper: working utterance is not set");
		
/* TODO: uncomment it when answer merger does set an operation
 * 		if(protoAnsMergerOut.hasMergingOperation())
			sdata.setAnswerMergerOperationType(protoAnsMergerOut.getMergingOperation());
		else
			throw new SentenceDataException("DM MessageWrapper: protoAnsMergerOut is not set");
*/			
					
		//set feedback value
		if(protoAnsMergerOut.hasUserFeedback())
			sdata.setUserFeedbackUtt(convertUserFeedback(protoAnsMergerOut.getUserFeedback()));
		
		if(protoAnsMergerOut.hasGoBack() && protoAnsMergerOut.getGoBack().getValue()==true)
			sdata.setUserActionGoBack(true);
		}
		
		//set dialogue entry
		DialogueEntry dEntry = decodeDialogueEntry(protoUtt);
		sdata.setDmEntry(dEntry );
		//tempErrorIndex should be set by decodeDialogueEntry if it was present
		//this is a hack because protobuffer has it in ClarifyOutput but DM needs it in sentence data
		sdata.setM_addressErrorSegmentIndex(temp_errorIndex);



  return sdata;

  }


/**
 * convert DmOutput object from the protobuffer into DialogueEntry object
 * @param protoUtt
 * @return
 * @throws SentenceDataException 
 */
	private DialogueEntry decodeDialogueEntry(UtteranceData protoUtt) throws SentenceDataException
	{
		DialogueEntry dEntry = null;
		if(protoUtt.hasDmOutput())
		{
			DmOutput protoDMOutput = protoUtt.getDmOutput();
			
			//TODO: get the index of the dm entry from the protoUtt
			dEntry = new DialogueEntry(0);
			
			if(protoDMOutput.hasQgRuleId())
				dEntry.setRuleID(protoDMOutput.getQgRuleId());
			
			//if DmClarifyOutput is set, set dEntry from it
			if(protoDMOutput.hasDmClarifyOutput()) 
			{			    
				
				DmClarifyOutput clarifyOut = protoDMOutput.getDmClarifyOutput();
				
				int temp_errorIndex = clarifyOut.getErrorSegmentIndex();
				
				if(DmClarificationType.ACTION_ASK_REPHRASE_PART ==  clarifyOut.getType())
                    dEntry.setDmDecision(DMDecision.ASK_TO_REPHRASE_PART); 					
				else
				if(DmClarificationType.ACTION_ASK_REPEAT_PART ==  clarifyOut.getType())
                    dEntry.setDmDecision(DMDecision.ASK_TO_REPEAT_PART); 					
				else
				if(DmClarificationType.ACTION_CLARIFY ==  clarifyOut.getType())
	                dEntry.setDmDecision(DMDecision.CLARIFY); 					
				else
				if(DmClarificationType.ACTION_CONFIRM ==  clarifyOut.getType())
	                dEntry.setDmDecision(DMDecision.CONFIRM_UTT); 					
				else
				if(DmClarificationType.ACTION_CONFIRM_ATTRIBUTE == clarifyOut.getType())
					//TODO: when there are multiple attributes to confirm, we need to check which it is
				{
		            dEntry.setDmDecision(DMDecision.CONFIRM_SPELL); 					
				}
				if(DmClarificationType.ACTION_REJECT == clarifyOut.getType())
		            dEntry.setDmDecision(DMDecision.REJECT); 					
						
				
				//get clarification segments
				List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();

				for(int i = 0; i < clarifyOut.getSegmentsCount(); i++)
				{
					DMOutputDataSegment seg; 
					DmClarifySegment clSeg = clarifyOut.getSegments(i);
					if(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT==clSeg.getAction())
					{
						String strval = clSeg.getTtsInput();
						seg = new DMOutputDataSegment(strval);
					}
					else
					if(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT==clSeg.getAction())
					{
						int errorIndex = clSeg.getErrorSegmentIndex();
						seg = new DMOutputDataSegment(errorIndex);
					}
					else 
						throw new SentenceDataException("Unknown value of DmTranslateSegmentActionType " + 
								clSeg.getAction().toString());
					segs.add(seg);
				}
				dEntry.setOutputData(segs);
				
			}
			else
			//if TranslateOutput	
			if(protoDMOutput.hasDmTranslateOutput())
			{
				DmTranslateOutput transOut = protoDMOutput.getDmTranslateOutput();
	            dEntry.setDmDecision(DMDecision.TRANSLATE); 					
				
				//get translation segments
				List<DMOutputDataSegment> segs = new ArrayList<DMOutputDataSegment>();
				
				for(int i = 0; i < transOut.getSegmentsCount(); i++)
				{
					DMOutputDataSegment seg; 
					DmTranslateSegment transSeg = transOut.getSegments(i);
					if(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT==transSeg.getAction())
					{
						String strval = transSeg.getMtInput();
						seg = new DMOutputDataSegment(strval);
					}
					else
					if(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT==transSeg.getAction())
					{
						int errorIndex = transSeg.getErrorSegmentIndex();
						seg = new DMOutputDataSegment(errorIndex);
					}
					else 
						throw new SentenceDataException("Unknown value of DmTranslateSegmentActionType " + 
								transSeg.getAction().toString());
					segs.add(seg);
				}
				dEntry.setOutputData(segs);
			}
			else //neither translate nor clarify segment were set, throw and exception
				new SentenceDataException("Nither hasDmClarifyOutput nor hasDmTranslateOutput were set");
		}
		return dEntry;		
			
		}


	/**
	 * 
	 * @return CONFIRM REJECT or throw exception
	 */
	private ErrorSegment.UserFeedback convertUserFeedback(UserFeedbackType protoFb)
	throws SentenceDataException
	{
		switch(protoFb) 
		{
		case CONFIRMED_BY_USER:
			return ErrorSegment.UserFeedback.CONFIRM;
		case REJECTED_BY_USER:
			return ErrorSegment.UserFeedback.REJECT;
		default:
			throw new SentenceDataException( "User feedback is neither CONFIRM nor REJECT ");
		}
		
	}
	
	/**
	 * Writes information from the SentenceData structure to the UtteranceData
	 * Updates and fills fields for Confidence Scores and DM Strategies
	 * 
	 * Fields that get encoded:
	 * DM Output
	 * 
	 * Fillds that do not get encoded: 
	 * Error Segments (because DM does not modify them)
	 * 
	 * @param utterance
	 * @param sdata
	 * @return modified sessionData, where last utterance is updated with information from SentenceData
	 */
	public SessionData encode(SessionData sessionData, SentenceData sentData){
		SessionData.Builder sessionBuilder = sessionData.toBuilder();
		//update the last utterance only
		UtteranceData.Builder utteranceBuilder = sessionBuilder.getUtterancesBuilder(
				sessionBuilder.getUtterancesCount()-1);
		
		if (sentData.getcfConfidence() != null) {
   		//set CU confidence values
   		int wid = 0;
   		for (WordAnnotation.Builder word: utteranceBuilder.getWordLevelAnnotationsBuilderList()){
   			word.setCuConfidence(word.getCuConfidence().toBuilder().setValue((sentData.getcfConfidence().get(wid))));
   			wid++;
   		}
		}
		
		//set DM output
		DialogueEntry dmEntry = sentData.getDmEntry();
	
	   if(dmEntry!=null)
		   utteranceBuilder =  encodeDMentry(utteranceBuilder, dmEntry, sentData.getM_addressErrorSegmentIndex());
	   
	   sessionBuilder.setUtterances(sessionBuilder.getUtterancesCount()-1, utteranceBuilder );		
	   return sessionBuilder.build();
	

	} 
	
	
	/**
	 * encodes dmEntry into protobuffer
	 * @param protoUtteranceBuilder
	 * @param dmEntry
	 * @param errorSegmentIndex
	 */
 	protected UtteranceData.Builder encodeDMentry(UtteranceData.Builder protoUtteranceBuilder, 
 			DialogueEntry dmEntry, int errorSegmentIndex )
 	{
		// set values for the  output segments in dm entry 
		List<DMOutputDataSegment> segs = dmEntry.getOutputData();
		
		com.sri.bolt.message.BoltMessages.DmOutput.Builder protoDMoutput = 
			com.sri.bolt.message.BoltMessages.DmOutput.newBuilder();
		
		//cover all DM decisions 
		//set DM action to either translate or clarify
		//TRANSLATE, TRANSLATE_TRANSLITERATE -> ACTION_TRANSLATE_UTTERANCE
		//ASK_TO_SPELL, ASK_TO_REPHRASE, CLARIFY,  CONFIRM_UTT, CONFIRM_SPELL, REJECT -> ACTION_CLARIFY_UTTERANCE
		if (dmEntry.getDmDecision()==DMDecision.TRANSLATE || 
				dmEntry.getDmDecision()==DMDecision.TRANSLATE_TRANSLITERATE)
		{
			
			
			com.sri.bolt.message.BoltMessages.DmTranslateOutput.Builder protoTranslateOut = 
				com.sri.bolt.message.BoltMessages.DmTranslateOutput.newBuilder();
			
			protoDMoutput.setDmAction(DmActionType.ACTION_TRANSLATE_UTTERANCE);
			
			//iterate over segments
			//if segment type is ERRORINDEX, set ACTION_TRANSLITERATE_SEGMENT
			//otherwize ACTION_TRANSLATE_SEGMENT
			for (int i = 0; i< segs.size(); i++)
			{
				DMOutputDataSegment seg = segs.get(i);
				com.sri.bolt.message.BoltMessages.DmTranslateSegment.Builder protoSegment = 
					com.sri.bolt.message.BoltMessages.DmTranslateSegment.newBuilder();
				//if its error index, set to transliterate 
				if(seg.getType()==DMOutputDataSegment.DataType.ERRORINDEX) 
				{
					protoSegment.setAction(DmTranslateSegmentActionType.ACTION_TRANSLITERATE_SEGMENT);
					protoSegment.setErrorSegmentIndex(errorSegmentIndex);
				}
				else
				{
					protoSegment.setAction(DmTranslateSegmentActionType.ACTION_TRANSLATE_SEGMENT);
					protoSegment.setMtInput(seg.getM_outputString());
				}
					
				protoTranslateOut.addSegments(i, protoSegment);
			}		
			
			protoDMoutput.setDmTranslateOutput(protoTranslateOut);
			protoUtteranceBuilder.setDmOutput(protoDMoutput);
         
		}
		else
		if (dmEntry.getDmDecision()==DMDecision.CLARIFY || 
				dmEntry.getDmDecision()==DMDecision.ASK_TO_SPELL ||
				dmEntry.getDmDecision()==DMDecision.ASK_TO_REPHRASE_PART ||
				dmEntry.getDmDecision()==DMDecision.ASK_TO_REPEAT_PART ||
				dmEntry.getDmDecision()==DMDecision.CONFIRM_SPELL ||
				dmEntry.getDmDecision()==DMDecision.CONFIRM_UTT	 ||
				dmEntry.getDmDecision()==DMDecision.REJECT	)
		{
			
			protoDMoutput.setDmAction(DmActionType.ACTION_CLARIFY_UTTERANCE);
			
			if(dmEntry.getRuleID()!=null)
				protoDMoutput.setQgRuleId(dmEntry.getRuleID());
			
			com.sri.bolt.message.BoltMessages.DmClarifyOutput.Builder protoClarifyOut = 
				com.sri.bolt.message.BoltMessages.DmClarifyOutput.newBuilder();
			protoClarifyOut.setErrorSegmentIndex(errorSegmentIndex);
			
			//set Clarification Type (1-1 mapping between DMDecision and DmClarificationType)
			switch(dmEntry.getDmDecision()){
			    case CLARIFY: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_CLARIFY);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP);
			    	break;
			    case ASK_TO_SPELL: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_SPELL);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_ADD_SPELLING);
			    	break;			                  			    				
			    case ASK_TO_REPHRASE_PART: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_ASK_REPHRASE_PART);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP);
			    	break;
			    case ASK_TO_REPEAT_PART: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_ASK_REPEAT_PART);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_ADD_ALT_MERGE_HYP);
			    	break;
			    case CONFIRM_SPELL: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_CONFIRM_ATTRIBUTE);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_CONFIRM_ERROR_SEGMENT_ATTRIBUTE);
			    	/** SS: this seems to be redundant */
			    	protoClarifyOut.setTargetedAttribute(ErrorSegmentAttributeType.ERROR_SEGMENT_ATTR_SPELLING);
			    	break;
			    case CONFIRM_UTT: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_CONFIRM);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_CONFIRM_UTTERANCE);
			    	break;
			    case REJECT: 
			    	protoClarifyOut.setType(DmClarificationType.ACTION_REJECT);
			    	protoClarifyOut.setExpectedOperation(AnswerMergerOperationType.OPERATION_USE_LAST_RESCORED);
			    	break;
				
			}			
			
			//set values of segments. For each segment:
			//if segment type is ERRORINDEX, set ACTION_PLAY_AUDIO_SEGMENT
			//otherwize ACTION_PLAY_TTS_SEGMENT
			for (int i = 0; i< segs.size(); i++)
			{
				DMOutputDataSegment seg = segs.get(i);
				com.sri.bolt.message.BoltMessages.DmClarifySegment.Builder protoSegment = 
					com.sri.bolt.message.BoltMessages.DmClarifySegment.newBuilder();
				//if its error index, play audio, otherwise play tts 
				if(seg.getType()==DMOutputDataSegment.DataType.ERRORINDEX) 
				{
					protoSegment.setAction(DmClarifySegmentActionType.ACTION_PLAY_AUDIO_SEGMENT);
					protoSegment.setErrorSegmentIndex(errorSegmentIndex);
				}
				else
				{
					protoSegment.setAction(DmClarifySegmentActionType.ACTION_PLAY_TTS_SEGMENT);
					protoSegment.setTtsInput(seg.getM_outputString());
				}
					
				protoClarifyOut.addSegments(i, protoSegment);
			}
			

			protoDMoutput.setDmClarifyOutput(protoClarifyOut);
			protoUtteranceBuilder.setDmOutput(protoDMoutput);
		}
		
		
		return protoUtteranceBuilder;
 	
	}
	
	/** 
	 * Extracts a list of ErrorSegment objects from UtteranceData
 	 * @param uttData
 	 * @return
 	 */
	protected List<ErrorSegment> decodeErrorSegmentsFromAMU(UtteranceData uttData) 
	  										throws SentenceDataException
 	{
 		List<ErrorSegmentAnnotation> protoErrorSegments = uttData.getErrorSegmentsList();
 		List<ErrorSegment> errorSegmentList = new ArrayList<ErrorSegment>();
 		Iterator<ErrorSegmentAnnotation> iter = protoErrorSegments.iterator();
 		int segmentIndex = 0;
 		while(iter.hasNext())
 		{
			ErrorSegmentAnnotation protoErrorSeg = iter.next();					
 			ErrorSegment errSegment = new ErrorSegment(segmentIndex);
 			segmentIndex++;
						
 			errSegment.setM_start(protoErrorSeg.getStartIndex());
 			errSegment.setM_end(protoErrorSeg.getEndIndex());
 			errSegment.setM_confidenceErrorSegment(protoErrorSeg.getConfidence());
 			
 			//****************************************************
 			//resolved flag and confidence
 			if(protoErrorSeg.hasIsResolved())
 			{
 				if(protoErrorSeg.getIsResolved().hasValue())			
 					errSegment.setM_resolved(protoErrorSeg.getIsResolved().getValue());
 				else
 					throw new SentenceDataException("getIsResolved is set but no value");

 				if(protoErrorSeg.getIsResolved().hasConfidence())			
 					errSegment.setM_resolvedConf(protoErrorSeg.getIsResolved().getConfidence());
 				else
 					throw new SentenceDataException("getIsResolved is set but no confidence");

 			}
 			
 			//****************************************************
 			//previous error segment index
 			if(protoErrorSeg.hasPreviousErrorSegmentIndex() )
 			{
 				errSegment.setPreviousSegmentIndex(protoErrorSeg.getPreviousErrorSegmentIndex());
 			}
 			
 			//****************************************************
 			//get all confidence values
			//if there is an oov tag and tag is true, set its values
 			//SS: Do I need to check if the value is true or false?
			//DM is currently not using feedback (OOV feedback makes no sense, we will never ask  Is it an OOV??)
 			
 			//ASR OOV
			if(protoErrorSeg.hasIsAsrOov() )
			{
				if( protoErrorSeg.getIsAsrOov().hasConfidence())				
					errSegment.setM_confidenceAsrOOV(protoErrorSeg.getIsAsrOov().getConfidence());
				else
					throw new SentenceDataException(uttData, "ASR OOV value is set but confidence is not");
				
			}

 			//MT OOV
			if(protoErrorSeg.hasIsMtOov() )
			{
				if( protoErrorSeg.getIsMtOov().hasConfidence())				
					errSegment.setConfidenceMTOOV(protoErrorSeg.getIsMtOov().getConfidence());
				else
					throw new SentenceDataException(uttData, "MT OOV value is set but confidence is not");
				
			}

 			//ASR ambiguous
			if(protoErrorSeg.hasIsAsrAmbiguous() )
			{
				if( protoErrorSeg.getIsAsrAmbiguous().hasConfidence())				
					errSegment.setConfidenceAsrAmbiguous(protoErrorSeg.getIsAsrAmbiguous().getConfidence());
				else
					throw new SentenceDataException(uttData, "ASR Ambig value is set but confidence is not");
				
			}
			
 			//MT ambiguous
			if(protoErrorSeg.hasIsMtAmbiguous() )
			{
				if( protoErrorSeg.getIsMtAmbiguous().hasConfidence())				
					errSegment.setConfidenceMTAmbiguous(protoErrorSeg.getIsMtAmbiguous().getConfidence());
				else
					throw new SentenceDataException(uttData, "MT Ambig value is set but confidence is not");
				
			}
			
			//get ambiguous words
			List<String> ambigWords = new ArrayList<String>();
			List<Double> ambigWordConfidence = new ArrayList<Double>();
			for(int i = 0; i<protoErrorSeg.getAmbiguousWordsCount(); i++)
			{
				String word = protoErrorSeg.getAmbiguousWords(i).getValue();
				double conf = protoErrorSeg.getAmbiguousWords(i).getConfidence();
				ambigWords.add(word);
				ambigWordConfidence.add(conf);				
			}
			errSegment.setAmbiguousWordsAndConf(ambigWords, ambigWordConfidence);

			//get ambiguous words explanations
			List<String> ambigWordsExpl = new ArrayList<String>();

			for(int i = 0; i<protoErrorSeg.getAmbiguousWordExplanationsCount(); i++)
			{
				String expl = protoErrorSeg.getAmbiguousWordExplanations(i).getValue();
				ambigWordsExpl.add(expl);
			}
			errSegment.setAmbiguousExplanations(ambigWordsExpl);

			
			//translate NE tag
			if(protoErrorSeg.getNeTag().hasValue())
			{
				com.sri.bolt.message.BoltMessages.StringAttribute attr = protoErrorSeg.getNeTag();
				//convert NE name into an EntityCategory
				//SS? check the actual names AMU uses
				String entityString = attr.getValue();
				if(entityString.equals("HUMAN"))
					errSegment.setM_neCategory(ErrorSegment.EntityCategory.HUMAN);
				else
				if(entityString.equals("LOCATION"))
					errSegment.setM_neCategory(ErrorSegment.EntityCategory.LOCATION);
				else
				if(entityString.equals("ORGANIZATION"))
					errSegment.setM_neCategory(ErrorSegment.EntityCategory.ORGANIZATION);
				else
	            errSegment.setM_neCategory(ErrorSegment.EntityCategory.NONE);

				
				if(attr.hasConfidence())
						errSegment.setM_confidenceEntityCategory(attr.getConfidence());
				else
					throw new SentenceDataException(uttData, "NE value is set but confidence is not");


				//set feedback value
				if(attr.hasUserFeedback())
				{
					errSegment.setUserFeedbackName(convertUserFeedback(attr.getUserFeedback()));
				}

			}
			
			//POS tag
			if(protoErrorSeg.getPosTag().hasValue())
			{
				com.sri.bolt.message.BoltMessages.StringAttribute attr = protoErrorSeg.getPosTag();
				
				errSegment.setM_posTagHead(attr.getValue());
				
				if(attr.hasConfidence())
					errSegment.setM_posConfidence(attr.getConfidence());
/*				we do not use conf now
 * 				else
					throw new SentenceDataException(uttData, "POS value is set but confidence is not");
*/								
			}
	
			//DEP tag
			if(protoErrorSeg.getDepTag().hasValue())
			{
				com.sri.bolt.message.BoltMessages.StringAttribute attr = protoErrorSeg.getDepTag();

				errSegment.setM_depLabel(attr.getValue());
 				
				if(attr.hasConfidence())
					errSegment.setM_confDepLabel(attr.getConfidence());
/*				we do not use conf now
 * 				else
					throw new SentenceDataException(uttData, "POS value is set but confidence is not");
*/								
			}
			
			//parent word in dependency tree
			if(protoErrorSeg.getDepWordIndex().hasValue())
			{
				errSegment.setDepWordIndex(protoErrorSeg.getDepWordIndex().getValue());
			}

			
			//TODO get spelling here (need a new version of BoltMessages.java)
			if(protoErrorSeg.hasSpelling())
			{
				StringAttribute spellAttr = protoErrorSeg.getSpelling();
				errSegment.setM_spell(spellAttr.getValue());
				
				if(spellAttr.hasUserFeedback())
				{
					errSegment.setUserFeedbackSpell(convertUserFeedback(spellAttr.getUserFeedback()));
				}		

			}
			
			errorSegmentList.add(errSegment);
 		}
 		
 		return errorSegmentList;
 	}
	

}