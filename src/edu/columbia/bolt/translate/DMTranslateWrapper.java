package edu.columbia.bolt.translate;

import edu.columbia.bolt.dmdata.DialogueEntry;

public interface DMTranslateWrapper {
	//passes the data to a  translation engine
	public void produceTranslation(DialogueEntry indata);
}
