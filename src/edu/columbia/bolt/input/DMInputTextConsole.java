package edu.columbia.bolt.input;

import java.io.Console;

import edu.columbia.bolt.commondata.SentenceData;

/**
 * 
 * @author sveta
 *
 * input using console
 *
 */
public class DMInputTextConsole implements DMInputWrapper {

	
	public DMInputTextConsole() {

	}

	
	//waiting for input
	public SentenceData getInput()
	{

	    Console console = System.console();
	    String  textinput = console.readLine("User: ");
	    SentenceData indata = new SentenceData(textinput);
	    return indata;
	
	}

}
