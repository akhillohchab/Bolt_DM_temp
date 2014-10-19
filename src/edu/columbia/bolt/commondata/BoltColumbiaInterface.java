package edu.columbia.bolt.commondata;

import com.sri.bolt.message.BoltMessages.*;

/**
 * interface for the entry point to a Columbia Module
 * @author sstoyanchev
 *
 */
public interface BoltColumbiaInterface {
	
	
	/**
	 * process input data and generate output
	 * @param conf
	 * @param SessionData
	 * @return
	 */
	SessionData process(ConfigurationParameters conf, SessionData sessiondata) ;
	
}
