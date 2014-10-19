package edu.columbia.bolt.commondata;

import java.util.List;

import com.sri.bolt.message.BoltMessages.UtteranceData;


/**
 * Exception to be thrown when size of an input feature vector does not match
 * @author sstoyanchev
 *
 */
public class SentenceDataException extends Exception {

	public SentenceDataException(int sizeold, int sizenew)
	{
	   	super("Size of the input list is wrong " + sizenew + " should be " + sizeold);
	}
	
	public SentenceDataException(SentenceData sd)
	{
	   	super("Unknown condition in SentenceData " + sd.toString());
	}

	public SentenceDataException(UtteranceData uttData, String str )
	{
	   	super("Processing Proto Message: " + str);
	}

	public SentenceDataException(List<ErrorSegment> errorSegment, String str )
	{
	   	super("In Error segment list size : " + errorSegment.size() + " " + str);
	}

	public SentenceDataException( String str )
	{
	   	super(str);
	}

	
}
