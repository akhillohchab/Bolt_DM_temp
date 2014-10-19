package edu.columbia.bolt.dmdata;

import edu.columbia.bolt.commondata.SentenceDataException;

/**
 * output for the user
 * this  may contain intonation parameters for TTS
 * or translation marking for the translation string
 * @author sveta
 *
 */
public class DMOutputDataSegment {
	
	public enum DataType{STRING, ERRORINDEX};
	DataType type;
	//string for TTS
	String m_outputString;
	//index of the error in the original utterance
	int m_indexOfError = -1;

	/**
	 * default type is string
	 * @param type
	 * @param outputString
	 * @throws SentenceDataException
	 */
	public DMOutputDataSegment(String outputString) 
	{
		super();		
		this.m_outputString = outputString;
		type = DataType.STRING;
	
	}

	
	/**
	 * this constructor is called for PLAY_TTS,TRANSLATE types
	 * @param type
	 * @param outputString
	 * @throws SentenceDataException
	 */
	public DMOutputDataSegment(String outputString, DataType type) 
	{
		super();		
		this.m_outputString = outputString;
		this.type = type;
	
	}
	

	/**
	 * this constructor is called for PLAY_AUDIO,TRANSLITERATE types
	 * @param type
	 * @param outputString
	 * @throws SentenceDataException
	 */
	public DMOutputDataSegment( int errorIndex) 
	{
		super();		
		this.m_indexOfError = errorIndex;	
		type = DataType.ERRORINDEX;
	}
	
	


	public String toString()
	{
		if(type==DataType.STRING)
			return m_outputString;
		else
			return "<PLAY ERROR " + this.m_indexOfError + ">";
		
	}


	public DataType getType() {
		return type;
	}


	public void setType(DataType type) {
		this.type = type;
	}


	public String getM_outputString() {
		return m_outputString;
	}


	public void setM_outputString(String m_outputString) {
		this.m_outputString = m_outputString;
	}





}
