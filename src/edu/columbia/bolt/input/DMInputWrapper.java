package edu.columbia.bolt.input;

import edu.columbia.bolt.commondata.SentenceData;

/**
 * Interface describing input to a dialogue manager
 * Notifies dialogue manager when input is ready
 * @author sveta
 *
 */
public interface DMInputWrapper {
	
	public SentenceData getInput();

}
