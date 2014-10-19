package edu.columbia.bolt.dm;

import edu.columbia.bolt.commondata.DialogueHistory;
import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.dmdata.DialogueEntry.DMDecision;
import edu.columbia.bolt.input.DMInputTextConsole;
import edu.columbia.bolt.input.DMInputWrapper;
import edu.columbia.bolt.logic.DMLogic;
import edu.columbia.bolt.logic.DMLogicWrapper;
import edu.columbia.bolt.output.DMOutputTextConsole;
import edu.columbia.bolt.output.DMOutputWrapper;
import edu.columbia.bolt.translate.DMTranslateDummy;
import edu.columbia.bolt.translate.DMTranslateWrapper;



/**
 *  DMLoop is a singleton, can be instantiated only once
 * @author sveta
 *
 */
public class DMLoop {
	
	static DMInputWrapper inputWrapper;
	static DMLogicWrapper logicWrapper;
	static DMOutputWrapper outputWrapper;
	static DMTranslateWrapper translateWrapper;
	static DialogueHistory dialogueHistory;

	
	 private static DMLoop _instance;
	 
	 /** constructor determines which instance of each object to create
	  * TODO: create other constructors to implement different functionalitiess
	  */
	 private DMLoop(){
		 inputWrapper = new DMInputTextConsole();
		 outputWrapper = new DMOutputTextConsole();
		 translateWrapper = new DMTranslateDummy();
		 logicWrapper = new DMLogic().getInstance(0);
		 dialogueHistory = new DialogueHistory();
		 
		 runDMLoop();
		 
	 };
     
	 public static synchronized DMLoop getInstance() {
         if (_instance == null) {
                 _instance = new DMLoop();
         }
         return _instance;
      }
	

	
	//loop waiting on notification from input object
	private void runDMLoop()
	{	
	    System.out.println("Starting DMLoop");
	    
	    
		while(true)
		{	
			try{
			//wait for the input
			SentenceData indata = inputWrapper.getInput();
			DialogueEntry newEntry = logicWrapper.decide(indata);
			indata.setDmEntry(newEntry);
			dialogueHistory.add(indata);
			if(newEntry.getDmDecision().equals(DMDecision.TRANSLATE))
				translateWrapper.produceTranslation(newEntry);
			else
				outputWrapper.produceOutput(newEntry);
					 		
			 if(indata.isQuit())
				    break;
			}
			catch (SentenceDataException ex)
			{
				ex.printStackTrace();
			}
		}
		System.out.println("DMLoop: Exiting");
		
	}
		
	
	public static void main(String[] args)
	{
		
		DMLoop loop = DMLoop.getInstance();
	}

}
