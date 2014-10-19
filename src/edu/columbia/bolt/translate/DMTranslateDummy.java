/**
 * 
 */
package edu.columbia.bolt.translate;

import edu.columbia.bolt.dmdata.DialogueEntry;

/**
 * @author sveta
 *
 */
public class DMTranslateDummy implements DMTranslateWrapper {

	/* (non-Javadoc)
	 * @see edu.columbia.bolt.translate.DMTranslateWrapper#produceTranslation(edu.columbia.bolt.data.DMInputData)
	 * 
	 * print translation to the console 
	 */
	public void produceTranslation(DialogueEntry indata) {
		System.out.println("Translating(" + indata.toString() + ")");		

	}

}
