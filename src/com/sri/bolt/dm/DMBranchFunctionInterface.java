package com.sri.bolt.dm;

import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.SessionData;

public interface DMBranchFunctionInterface {
   //if this is a restart
	public boolean isRestart(SessionData session, ErrorSegmentAnnotation error);

   public boolean isMoveOn(SessionData session, ErrorSegmentAnnotation error);

	//if this is the last turn
	public boolean isLastTurn(SessionData session, ErrorSegmentAnnotation error);

        // if error.getErrorType() == ERROR_SEGMENT_MT and spelling is not confirmed
        public boolean isMtErrorWithNonConfirmedSpelling(SessionData session, ErrorSegmentAnnotation error);
	
	//if errorSegment.isMtAmbiguous()==true
	public boolean isMtAmbiguous(SessionData session, ErrorSegmentAnnotation error);
	
	//if errorSegment.isMtOov()==true
	public boolean isMtOov(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isMtQuestionable()==true
	public boolean isMtQuestionable(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isMtWordDropped()==true
	public boolean isMtWordDropped(SessionData session, ErrorSegmentAnnotation error);

	//if the user confirmed the entire utterance
	public boolean isUtteranceConfirmed(SessionData session, ErrorSegmentAnnotation error);
	
	//if the user rejected the entire utterance
	public boolean isUtteranceRejected(SessionData session, ErrorSegmentAnnotation error);

	//if there are no errors left, and the resolved error is a homophone error
	public boolean isResolvedErrorHomophoneError(SessionData session, ErrorSegmentAnnotation error);		

	//if there are no errors left, and turn is 0 or 1
	public boolean isNoErrorLeftInTheFirstTwoTurns(SessionData session, ErrorSegmentAnnotation error);		
	
	//if there are no errors left
	public boolean isNoErrorLeft(SessionData session, ErrorSegmentAnnotation error);
	
	//if there are too many errorful words
	public boolean doesSentenceHaveTooManyErrors(SessionData session, ErrorSegmentAnnotation error);
	
    //if errorSegment.isAsrAmbiguous()==true
	public boolean isAsrAmbiguous(SessionData session, ErrorSegmentAnnotation error);
		
	//if errorSegment.isAsrOov()==true and spelling is not confirmed
	public boolean isAsrOovWithNonConfirmedSpelling(SessionData session, ErrorSegmentAnnotation error);
	
	//if errorSegment.isAsrOov()==true and name is confirmed
	public boolean isAsrOovWithConfirmedName(SessionData session, ErrorSegmentAnnotation error);
		
	//if errorSegment.isAsrOov()==true and name is rejected
	public boolean isAsrOovWithRejectedName(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isAsrOov()==true and neTag==NONE
	public boolean isAsrNonNameOov(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isAsrOov()==true and neTag=HUMAN
	public boolean isAsrNameOov(SessionData session, ErrorSegmentAnnotation error);
		
	//if errorSegment.isAsrOov()==true, name is not confirmed or rejected, and turn is 0 or 1
	public boolean isAsrOovWithNotConfirmedName(SessionData session, ErrorSegmentAnnotation error);
        	
	//if errorSegment.isAsrOov()==true, name is NULL
	public boolean isAsrOovWithNullName(SessionData session, ErrorSegmentAnnotation error);
	   
	//if errorSegment.isAsrOov()==false, error is at the beginning of the sentence
	public boolean isErrorAtTheBeginning(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isAsrOov()==false, depTag==NMOD and posTag==JJ
	public boolean isAdjectiveModifier(SessionData session, ErrorSegmentAnnotation error);

	//if errorSegment.isAsrOov()==false, depTag==NMOD|APPO and posTag==NN
	public boolean isNounModifier(SessionData session, ErrorSegmentAnnotation error);

//	//if errorSegment.isAsrOov()==false, neTag=LOCATION
//	public boolean isLocation(SessionData session, ErrorSegmentAnnotation error);
//
//	//if errorSegment.isAsrOov()==false, neTag==PERSON, depTag==OBJ
//	public boolean isObjectPerson(SessionData session, ErrorSegmentAnnotation error);
//	
//	//if errorSegment.isAsrOov()==false, neTag==PERSON, depTag!=OBJ
//	public boolean isPerson(SessionData session, ErrorSegmentAnnotation error);
	
	//if errorSegment.isAsrOov()==false, posTag==Verb
	public boolean isVerb(SessionData session, ErrorSegmentAnnotation error);
	
	//default action
	public boolean isAlwaysTrue(SessionData session, ErrorSegmentAnnotation error);
	
}
