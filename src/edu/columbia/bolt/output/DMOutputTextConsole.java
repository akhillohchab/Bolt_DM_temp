package edu.columbia.bolt.output;


import edu.columbia.bolt.dmdata.DialogueEntry;

public class DMOutputTextConsole implements DMOutputWrapper{	
		
	public void  produceOutput(DialogueEntry outData)
	{
		System.out.println("System:" + outData.toString());
	}
	
    
}
