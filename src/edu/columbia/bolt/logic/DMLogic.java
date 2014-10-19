package edu.columbia.bolt.logic;

import com.sri.bolt.message.BoltMessages.*;

import edu.columbia.bolt.commondata.ConfigurationParameters;
import edu.columbia.bolt.commondata.DialogueHistory;
import edu.columbia.bolt.commondata.MessageWrapper;
import edu.columbia.bolt.commondata.SentenceData;
import edu.columbia.bolt.commondata.BoltColumbiaInterface;
import edu.columbia.bolt.commondata.SentenceDataException;
import edu.columbia.bolt.dmdata.DialogueEntry;
import edu.columbia.bolt.logic.DMLogicWrapper;

/**
 * This class is the entry point into Columbia DM component.
 * data.getColumbiaBoltDMVersion() determines which version of DM is used.
 * Calls decide() method of DM logic,
 * sets dmEntry in data field, and returns it.
 * @author sstoyanchev
 *
 */
public class DMLogic implements BoltColumbiaInterface {
	

	/**
	 * Interface for calling from outside
	 */
	@Override
	public SessionData process(ConfigurationParameters conf, SessionData  sessiondata){
				
		//Create new wrapper to convert from BoltMessages to internally used data format
		MessageWrapper messagewrapper = new MessageWrapper();
		
		SentenceData data = new SentenceData();
		//if exception is thrown in message decoder, do not proceed
		try{
			data = process(conf, messagewrapper.decode(sessiondata));	
		}catch (SentenceDataException ex){
			ex.printStackTrace();
			return sessiondata;
		}

		return messagewrapper.encode(sessiondata, data);
	}
	
	/**
	 * process the data
	 * @param conf
	 * @param sessiondata
	 * @return
	 */
	public SentenceData process(ConfigurationParameters conf, SentenceData  data) throws SentenceDataException{
		
			DMLogicWrapper dmLogic;
			switch (conf.dmVersion) {
				case STUB:  dmLogic = new DMLogicSimple();
						 break;
				case RULEBASED:  dmLogic = new DMLogicRuleBased();
				         break;
			    default:  return null;
			    
		    }
			DialogueEntry dEntry = dmLogic.decide(data);			
			data.setDmEntry(dEntry);

			return data;
	}

	
	/**
	 * interface for internal calls
	 */
	public DMLogicWrapper getInstance(int type) {
		DMLogicWrapper dmLogic;
		switch (type ) {
			case 0:  dmLogic = new DMLogicSimple();
					 break;
			case 1:  dmLogic = new DMLogicRuleBased();
			         break;
		    default:  return null;
		}
		return dmLogic;
	}


}
