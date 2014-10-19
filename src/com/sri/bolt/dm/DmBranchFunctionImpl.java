package com.sri.bolt.dm;

import java.util.List;

import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UserFeedbackType;
import com.sri.bolt.message.BoltMessages.UtteranceData;

public class DmBranchFunctionImpl implements DMBranchFunctionInterface {
	
   private static final int MAX_TURNS = 3;
   private static final int MAX_ERRORS = 2;
   private static final double RATIO_THRESHOLD = 0.5;

   @Override
   public boolean isRestart(SessionData session, ErrorSegmentAnnotation error) {
      UtteranceData utt = session.getUtterances(session.getCurrentTurn());
      return Util.isRestartString(utt.getRescored1Best());
   }

   @Override
   public boolean isMoveOn(SessionData session, ErrorSegmentAnnotation error) {
      UtteranceData utt = session.getUtterances(session.getCurrentTurn());
      return session.getUtterancesCount() > 1 && Util.isMoveOnString(utt.getRescored1Best());
   }

   @Override
	//if this is the last turn
	public boolean isLastTurn(SessionData session, ErrorSegmentAnnotation error){
		return session.getCurrentTurn() >= MAX_TURNS;
	}

    // if error.getErrorType() == ERROR_SEGMENT_MT and spelling is not confirmed
    public boolean isMtErrorWithNonConfirmedSpelling(SessionData session, ErrorSegmentAnnotation error) {
	return error != null
	    && error.hasErrorType()
	    && error.getErrorType() == ErrorSegmentType.ERROR_SEGMENT_MT
	    && error.hasSpelling()
	    && (!error.getSpelling().hasUserFeedback() || error.getSpelling().getUserFeedback() == UserFeedbackType.REJECTED_BY_USER);
    }

	@Override
	//if errorSegment.isMtAmbiguous()==true
	public boolean isMtAmbiguous(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsMtAmbiguous() && error.getIsMtAmbiguous().getValue();
	}

	@Override
	//if errorSegment.isMtOov()==true
	public boolean isMtOov(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsMtOov() && error.getIsMtOov().getValue();
	}

	@Override
	//if errorSegment.isMtQuestionable()==true
	public boolean isMtQuestionable(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsMtQuestionable() && error.getIsMtQuestionable().getValue();
	}

	@Override
	//if errorSegment.isMtWordDropped()==true
	public boolean isMtWordDropped(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsMtWordDropped() && error.getIsMtWordDropped().getValue();
	}

	@Override
	//if the user confirmed the entire utterance
	public boolean isUtteranceConfirmed(SessionData session, ErrorSegmentAnnotation error){
		UtteranceData utt = session.getUtterances(session.getCurrentTurn());
		return utt.hasAnswerMergerOutput() && utt.getAnswerMergerOutput().hasUserFeedback() &&
				utt.getAnswerMergerOutput().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER;
	}
	
	@Override
	//if the user rejected the entire utterance
	public boolean isUtteranceRejected(SessionData session, ErrorSegmentAnnotation error){
		UtteranceData utt = session.getUtterances(session.getCurrentTurn());
		return utt.hasAnswerMergerOutput() && utt.getAnswerMergerOutput().hasUserFeedback() &&
				utt.getAnswerMergerOutput().getUserFeedback() == UserFeedbackType.REJECTED_BY_USER;
	}
	
	@Override
	public boolean isResolvedErrorHomophoneError(SessionData session,
			ErrorSegmentAnnotation error) {
		if (error == null){
			// check if any homophone error exists and whether they are resolved
			UtteranceData utt = session.getUtterances(session.getCurrentTurn());
			List<ErrorSegmentAnnotation> errorSegments = utt.getErrorSegmentsList();
			if (errorSegments == null || errorSegments.isEmpty())
				return false;
			for (ErrorSegmentAnnotation errorSegment : errorSegments){
				if (!(errorSegment.hasIsResolved() && errorSegment.getIsResolved().getValue()))
					return false;
				if (!(errorSegment.hasIsAsrAmbiguous() && 
				      errorSegment.getIsAsrAmbiguous().getValue() && 
					  errorSegment.getIsResolved().getValue()))
					return false;					
			}
			return true;
		}
		return false;
	}

	@Override
	//if there are no errors left, and turn is 0 or 1
	public boolean isNoErrorLeftInTheFirstTwoTurns(SessionData session, ErrorSegmentAnnotation error){
		return session.getCurrentTurn() <= 1 && error == null;
	}
	
	@Override
	//if there are no errors left
	public boolean isNoErrorLeft(SessionData session, ErrorSegmentAnnotation error){
		return error == null;
	}
	
	@Override
	//if there are too many errorful words
	public boolean doesSentenceHaveTooManyErrors(SessionData session, ErrorSegmentAnnotation error){
		UtteranceData utt = session.getUtterances(session.getCurrentTurn());	
		String[] words = utt.getAnswerMergerOutput().getWorkingUtterance().trim().split("\\s+");

		int wordErrors = 0;
		int segmentErrors = 0;
		
      boolean[] errorful = new boolean[words.length];
		for (ErrorSegmentAnnotation errorSegment : utt.getErrorSegmentsList()){
         if (!errorSegment.hasIsResolved() || (errorSegment.hasIsResolved() && !errorSegment.getIsResolved().getValue())) {
            segmentErrors++;
            for (int i=errorSegment.getStartIndex(); i <= errorSegment.getEndIndex(); i++)
               errorful[i] = true;
         }
		}
		for (int i=0; i < words.length; i++) {
			if (errorful[i])
				wordErrors++;
		}
		
		return segmentErrors > MAX_ERRORS || wordErrors > RATIO_THRESHOLD * words.length;
	}

	@Override
	//if errorSegment.isAsrAmbiguous()==true
	public boolean isAsrAmbiguous(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsAsrAmbiguous() && error.getIsAsrAmbiguous().getValue();
	}

	//if errorSegment.isAsrOov()==true and spelling is not confirmed
	public boolean isAsrOovWithNonConfirmedSpelling(SessionData session, ErrorSegmentAnnotation error){
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasSpelling() && (!error.getSpelling().hasUserFeedback() ||
						error.getSpelling().getUserFeedback() == UserFeedbackType.REJECTED_BY_USER);
	}
	
	@Override
	//if errorSegment.isAsrOov()==true and name is confirmed
	public boolean isAsrOovWithConfirmedName(SessionData session, ErrorSegmentAnnotation error){
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasNeTag() && error.getNeTag().hasUserFeedback() &&
				error.getNeTag().getUserFeedback() == UserFeedbackType.CONFIRMED_BY_USER;	
	}

	@Override
	//if errorSegment.isAsrOov()==true and name is rejected
	public boolean isAsrOovWithRejectedName(SessionData session, ErrorSegmentAnnotation error){
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasNeTag() && error.getNeTag().hasUserFeedback() &&
				error.getNeTag().getUserFeedback() == UserFeedbackType.REJECTED_BY_USER;	
	}

	@Override
	//if errorSegment.isAsrOov()==true and neTag==NONE
	public boolean isAsrNonNameOov(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasNeTag() && error.getNeTag().getValue().equals("NONE");
	}

	@Override
	//if errorSegment.isAsrOov()==true and neTag=HUMAN
	public boolean isAsrNameOov(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasNeTag() && error.getNeTag().getValue().equals("HUMAN");
	}

	@Override
	//if errorSegment.isAsrOov()==true, name is not confirmed or rejected, and turn is 0 or 1
	public boolean isAsrOovWithNotConfirmedName(SessionData session, ErrorSegmentAnnotation error){
		return session.getCurrentTurn() <= 1 && 
				error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				error.hasNeTag() && !(error.getNeTag().hasUserFeedback());	
	}
	
	//if errorSegment.isAsrOov()==true, name is NULL
	public boolean isAsrOovWithNullName(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && error.hasIsAsrOov() && error.getIsAsrOov().getValue() &&
				!error.hasNeTag();	
	}

	//if errorSegment.isAsrOov()==false
	private boolean isNotAsrOov(SessionData session, ErrorSegmentAnnotation error) {
		return error != null && (!(error.hasIsAsrOov()) || !(error.getIsAsrOov().getValue()));
	}

	@Override
	//if errorSegment.isAsrOov()==false, error is at the beginning of the sentence
	public boolean isErrorAtTheBeginning(SessionData session, ErrorSegmentAnnotation error){
		return isNotAsrOov(session, error) && error.getStartIndex() <= 1;
	}

	@Override
	//if errorSegment.isAsrOov()==false, depTag==NMOD and posTag==JJ
	public boolean isAdjectiveModifier(SessionData session, ErrorSegmentAnnotation error){
		return isNotAsrOov(session, error) && 
	         error.hasDepWordIndex() && error.getDepWordIndex().getValue() != -1 &&
				error.hasPosTag() && error.getPosTag().getValue().equals("JJ") &&
				error.hasDepTag() && error.getDepTag().getValue().equals("NMOD");
	}

	@Override
	//if errorSegment.isAsrOov()==false, depTag==NMOD|APPO and posTag==NN
	public boolean isNounModifier(SessionData session, ErrorSegmentAnnotation error){
		return isNotAsrOov(session, error) && 
            error.hasDepWordIndex() && error.getDepWordIndex().getValue() != -1 &&
				error.hasPosTag() && error.getPosTag().getValue().equals("JJ") &&
				(error.hasDepTag() && (error.getDepTag().getValue().equals("NMOD") ||
				                       error.getDepTag().getValue().equals("APPO")));
	}

//	@Override
//	//if errorSegment.isAsrOov()==false, neTag=LOCATION
//	public boolean isLocation(SessionData session, ErrorSegmentAnnotation error){
//		return isNotAsrOov(session, error) && 
//				error.hasNeTag() && error.getNeTag().getValue().equals("LOCATION");
//	}
//
//	@Override
//	//if errorSegment.isAsrOov()==false, neTag==PERSON, depTag==OBJ
//	public boolean isObjectPerson(SessionData session, ErrorSegmentAnnotation error){
//		return isNotAsrOov(session, error) && 
//				error.hasNeTag() && error.getNeTag().getValue().equals("HUMAN") &&
//				error.hasDepTag() && error.getDepTag().getValue().equals("OBJ");
//	}
//	
//	@Override
//	//if errorSegment.isAsrOov()==false, neTag==PERSON, depTag!=OBJ
//	public boolean isPerson(SessionData session, ErrorSegmentAnnotation error){
//		return isNotAsrOov(session, error) && 
//				error.hasNeTag() && error.getNeTag().getValue().equals("HUMAN") &&
//				error.hasDepTag() && !(error.getDepTag().getValue().equals("OBJ"));
//	}
	
	@Override
	//if errorSegment.isAsrOov()==false, posTag==Verb
	public boolean isVerb(SessionData session, ErrorSegmentAnnotation error){
		return isNotAsrOov(session, error) && 
				error.hasPosTag() && error.getPosTag().getValue().equals("VB");
	}
	
	@Override
	//default action
	public boolean isAlwaysTrue(SessionData session, ErrorSegmentAnnotation error){
		return true;
	}

	
}
