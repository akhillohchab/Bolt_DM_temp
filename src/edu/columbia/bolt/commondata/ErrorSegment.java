package edu.columbia.bolt.commondata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * class that stores inoformation on the error segment
 * 
 * @author sstoyanchev
 *
 */
public class ErrorSegment implements Comparable{
	//type of error
	public enum ErrorType{INSERT, SUBSTITUTE, DELETE};
	ErrorType m_ErrorType;
	
	//this identifies error segment in the list for the protobuffer
	int indexInTheList = -1;
	
	double m_confErrorSegment = 0.0;
	
	//this value is set according to AMU's threshold
	//not currently used
	boolean amuResolvedFlag = false;
	
	//this flag is set if DM decides to give up on the error
	boolean skipFlag = false;
		
	//conf = -1.0 means the error was not attempted to be resolved
	//conf [0,1] represents confidence of answer merger that the error is resolved correctly
	double m_resolvedConf = -1.0;
	
	//this is an index of the same error segment in previous utterance's segment list
	//when error segment is new previousSegmentIndex = -1
	int previousSegmentIndex = -1;
	
	//index of the start of an error segment in merged utterance
	int m_start;
	//index of the end of an error segment in merged utterance
	int m_end;

	
	//confidence that this error contains ASR OOV
	double confidenceAsrOOV = 0.0;
	//confidence that this error contains MT OOV
	double confidenceMTOOV = 0.0;
	//confidence that this error contains homophones
	double confidenceAsrAmbiguous = 0.0;	
	//confidence that this error contains words ambiguous for Mt
	double confidenceMTAmbiguous = 0.0;	
	
	//if either of Ambiguous conf >0, ambiguousWords contain a list of alternative ambig. words
	List<String> ambiguousWords = new ArrayList<String>();
	List<Double> ambiguousWordsConf = new ArrayList<Double>();
	//if either of Ambiguous conf >0, ambiguousExplanations contain a list of alternative ambig. words
	//DM sets Ambig. explanations for Homophones
	List<String> ambiguousExplanations = new ArrayList<String>();
	
	//expected NE category	
//	public enum EntityCategory{NONE, HUMAN, LOCATION, DISFLUENCY, REPETITION, PRD};
	public enum EntityCategory{NONE, HUMAN, LOCATION, ORGANIZATION};
	EntityCategory m_eCategory = EntityCategory.NONE;
	double m_confidenceEntityCategory;
	
	String m_posTagHead = "";
	double m_posConfidence = 0.0;
	
	//label of a dependency
    String m_depLabelHead = "";
    double m_confDepLabel = 0.0;
    
    
	/*	AMOD,
		APPO,
		CONJ,
		COORD,
		DEP,
		DIR,
		DIR-GAP,
		DTV,
		EXT,
		EXTR,
		GAP-LOC,
		GAP-NMOD,
		GAP-OBJ,
		GAP-PRD,
		GAP-SBJ,
		GAP-VC,
		HMOD,
		HYPH,
		IM,
		LGS,
		LOC,
		LOC-PRD,
		MNR,
		NAME,
		NMOD,
		OBJ,
		OPRD,
		P,
		PMOD,
		PRD,
		PRD-TMP,
		PRN,
		PRP,
		PRT,
		PUT,
		ROOT,
		SBJ,
		SUB,
		SUFFIX,
		TMP,
		VC,
		VOC,*/
    

    //index of the word that error is dependent on (parent word in the tree)
    int depWordIndex = -1;
    
    public enum UserFeedback{CONFIRM,REJECT,UNKNOWN};
    UserFeedback userFeedbackNE = UserFeedback.UNKNOWN;
    UserFeedback userFeedbackSpell = UserFeedback.UNKNOWN;
    
    //spelling
    String m_spell = null;

    public ErrorSegment(int segmentIndex)
    {
    	super();
    	this.indexInTheList = segmentIndex;
    }

    /**
     * Constructor: mainly to be used by tester module
     */
	public ErrorSegment(int segmentIndex, double errorConf, ErrorType m_ErrorType, EntityCategory m_eCategory,
			double m_confidenceEntityCategory, int m_start, int m_end,
			double m_confidenceOOV, String m_posTagHead,
			double m_posConfidence, String m_depLabelHead, double m_confDepLabel) {
		super();
		this.indexInTheList = segmentIndex;
		this.m_confErrorSegment = errorConf;
		this.m_ErrorType = m_ErrorType;
		this.m_eCategory = m_eCategory;
		this.m_confidenceEntityCategory = m_confidenceEntityCategory;
		this.m_start = m_start;
		this.m_end = m_end;
		this.confidenceAsrOOV = m_confidenceOOV;
		this.m_posTagHead = m_posTagHead;
		this.m_posConfidence = m_posConfidence;
		this.m_depLabelHead = m_depLabelHead;
		this.m_confDepLabel = m_confDepLabel;
	}

	@Override
	public String toString()
	{
		DecimalFormat df = new DecimalFormat("##.##");
 		String retstr = "(" + m_start +"," +m_end+")conf=" + df.format(m_confErrorSegment) ;
 		if(m_eCategory!=null)
 			retstr += "Entity:"+m_eCategory.toString()+"/"+ df.format(m_confidenceEntityCategory);
 		retstr +=  " OOV:" + df.format(confidenceAsrOOV);
 		retstr += " POS:" + m_posTagHead + "/" + df.format(m_posConfidence);
		retstr += " DEP:" + m_depLabelHead + "/" + df.format(m_confDepLabel) ;
		return retstr;
	}
	
	public ErrorType getM_ErrorType() {
		return m_ErrorType;
	}

	public void setM_ErrorType(ErrorType m_ErrorType) {
		this.m_ErrorType = m_ErrorType;
	}

	public EntityCategory getM_neCategory() {
		return m_eCategory;
	}

	public void setM_neCategory(EntityCategory m_neCategory) {
		this.m_eCategory = m_neCategory;
	}

	public int getM_start() {
		return m_start;
	}

	public void setM_start(int m_start) {
		this.m_start = m_start;
	}

	public int getM_end() {
		return m_end;
	}

	public void setM_end(int m_end) {
		this.m_end = m_end;
	}

	public double getM_confidenceErrorSegment() {
		return m_confErrorSegment;
	}

	public void setM_confidenceErrorSegment(double m_confidence) {
		this.m_confErrorSegment = m_confidence;
	}


	public void setM_depLabel(String m_depLabel) {
		this.m_depLabelHead = m_depLabel;
	}

	public double getM_confidenceAsrOOV() {
		return confidenceAsrOOV;
	}

	public void setM_confidenceAsrOOV(double m_confidenceOOV) {
		this.confidenceAsrOOV = m_confidenceOOV;
	}

	public String getM_posTagHead() {
		return m_posTagHead;
	}

	public void setM_posTagHead(String m_posTagHead) {
		this.m_posTagHead = m_posTagHead;
	}

	public double getM_posConfidence() {
		return m_posConfidence;
	}

	public void setM_posConfidence(double m_posConfidence) {
		this.m_posConfidence = m_posConfidence;
	}

	public String getM_depLabelHead() {
		return m_depLabelHead;
	}

	public void setM_depLabelHead(String m_depLabelHead) {
		this.m_depLabelHead = m_depLabelHead;
	}

	public double getM_confDepLabel() {
		return m_confDepLabel;
	}

	public void setM_confDepLabel(double m_confDepLabel) {
		this.m_confDepLabel = m_confDepLabel;
	}

	public double getM_confidenceEntityCategory() {
		return m_confidenceEntityCategory;
	}

	public void setM_confidenceEntityCategory(double m_confidenceEntityCategory) {
		this.m_confidenceEntityCategory = m_confidenceEntityCategory;
	}

	public UserFeedback getUserFeedbackName() {
		return userFeedbackNE;
	}

	public void setUserFeedbackName(UserFeedback userFeedback) {
		this.userFeedbackNE = userFeedback;
	}

	public UserFeedback getUserFeedbackSpell() {
		return userFeedbackSpell;
	}

	public void setUserFeedbackSpell(UserFeedback userFeedbackSpell) {
		this.userFeedbackSpell = userFeedbackSpell;
	}

	public String getM_spell() {
		return m_spell;
	}

	public void setM_spell(String m_spell) {
		this.m_spell = m_spell;
	}

	public boolean isM_resolved() {
		return amuResolvedFlag;
	}
	
	public boolean isAddressed() {
		return m_resolvedConf>=0;
	}

	public void setM_resolved(boolean m_resolved) {
		this.amuResolvedFlag = m_resolved;
	}

	public double getM_resolvedConf() {
		return m_resolvedConf;
	}

	public void setM_resolvedConf(double m_resolvedConf) {
		this.m_resolvedConf = m_resolvedConf;
	}

	public double getConfidenceAsrOOV() {
		return confidenceAsrOOV;
	}

	public void setConfidenceAsrOOV(double confidenceAsrOOV) {
		this.confidenceAsrOOV = confidenceAsrOOV;
	}

	public double getConfidenceMTOOV() {
		return confidenceMTOOV;
	}

	public void setConfidenceMTOOV(double confidenceMTOOV) {
		this.confidenceMTOOV = confidenceMTOOV;
	}

	public double getConfidenceAsrAmbiguous() {
		return confidenceAsrAmbiguous;
	}

	public void setConfidenceAsrAmbiguous(double confidenceAsrAmbiguous) {
		this.confidenceAsrAmbiguous = confidenceAsrAmbiguous;
	}

	public double getConfidenceMTAmbiguous() {
		return confidenceMTAmbiguous;
	}

	public void setConfidenceMTAmbiguous(double confidenceMTAmbiguous) {
		this.confidenceMTAmbiguous = confidenceMTAmbiguous;
	}

	public List<String> getAmbiguousWords() {
		return ambiguousWords;
	}

	public void setAmbiguousWordsAndConf(List<String> ambiguousWords, List<Double> ambigConf) 
	 throws SentenceDataException{
		this.ambiguousWords = ambiguousWords;
		this.ambiguousWordsConf = ambigConf;
	}

	public List<String> getAmbiguousExplanations() {
		return ambiguousExplanations;
	}

	public void setAmbiguousExplanations(List<String> ambiguousExplanations) {
		this.ambiguousExplanations = ambiguousExplanations;
	}

	public int getPreviousSegmentIndex() {
		return previousSegmentIndex;
	}

	public void setPreviousSegmentIndex(int previousSegmentIndex) {
		this.previousSegmentIndex = previousSegmentIndex;
	}

	public int getDepWordIndex() {
		return depWordIndex;
	}

	public void setDepWordIndex(int depWordIndex) {
		this.depWordIndex = depWordIndex;
	}

	/**
	 * determines the order of how error will be addressed
	 * first address  mt errors (if there is an MT error, that means we are called after Mt)
	 * non-oov Asr errors have the lowest priority
	 * 
	 * Order of priority for sorting error
	 * 1. MT ambiguity > MT_AMBIG_CONF_THRESOLD
	 * 2. MT OOV > MT_OOV_CONF_THRESOLD
	 * 3. ASR OOV > ASR_OOV_CONF_THRESOLD
	 * 4. ASR Ambiguity > ASR_AMBIG_CONF_THRESHOLD
	 * 5. Position (if Error Confidence diff <|.1|) //if errors are similar confidence, prefer the later one
	 * 6. Confidence value of the error segment
	 * 
	 */
	@Override
	public int compareTo(Object arg0) {
		ConfigurationParameters config = ConfigurationParameters.getInstance();
		ErrorSegment otherError = (ErrorSegment) arg0;
		//MT ambiguity 
		if(this.confidenceMTAmbiguous>config.MT_AMBIG_CONF_THRESOLD && otherError.confidenceMTAmbiguous<=config.MT_AMBIG_CONF_THRESOLD)
			return -1;
		if(this.confidenceMTAmbiguous<=config.MT_AMBIG_CONF_THRESOLD && otherError.confidenceMTAmbiguous>config.MT_AMBIG_CONF_THRESOLD)
			return 1;
		
		//MT OOV
		if(this.confidenceMTOOV>config.MT_OOV_CONF_THRESHOLD && otherError.confidenceMTOOV<=config.MT_OOV_CONF_THRESHOLD)
			return -1;		
		if(this.confidenceMTOOV<= config.MT_OOV_CONF_THRESHOLD && otherError.confidenceMTOOV>config.MT_OOV_CONF_THRESHOLD)
			return 1;
		
		//ASR OOV
		if(this.confidenceAsrOOV>config.ASR_OOV_CONF_THRESHOLD && otherError.confidenceAsrOOV<=config.ASR_OOV_CONF_THRESHOLD)
			return -1;		
		if(this.confidenceAsrOOV<= config.ASR_OOV_CONF_THRESHOLD && otherError.confidenceAsrOOV>config.ASR_OOV_CONF_THRESHOLD)
			return 1;
		
		//ASR ambiguous
		if(this.confidenceAsrAmbiguous>config.ASR_AMBIG_CONF_THRESOLD && otherError.confidenceAsrAmbiguous<=config.ASR_AMBIG_CONF_THRESOLD)
			return -1;		
		if(this.confidenceAsrAmbiguous<= config.ASR_AMBIG_CONF_THRESOLD && otherError.confidenceAsrAmbiguous>config.ASR_AMBIG_CONF_THRESOLD)
			return 1;
		
		
		//if confidence scores are within .1 point, use error position
		double confdiff = this.m_confErrorSegment-otherError.m_confErrorSegment;
		if(confdiff<.1 && confdiff>-.1)
		{
			if(this.m_start > otherError.m_start)
				return -1;
			if(this.m_start < otherError.m_start)
				return 1;		    
		}

		//conf diff is the same, now use error confidence scores
		
		if(this.m_confErrorSegment>otherError.m_confErrorSegment)
			return -1;
		
		if(this.m_confErrorSegment < otherError.m_confErrorSegment)
			return 1;
		
			
		
		return 0;
	}

	public int getIndexInTheList() {
		return indexInTheList;
	}

	public void setIndexInTheList(int indexInTheList) {
		this.indexInTheList = indexInTheList;
	}

	public boolean isSkipFlag() {
		return skipFlag;
	}

	public void setSkipFlag(boolean skipFlag) {
		this.skipFlag = skipFlag;
	}




	

}
