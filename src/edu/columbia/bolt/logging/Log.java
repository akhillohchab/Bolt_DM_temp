package edu.columbia.bolt.logging;

public class Log {
	//0 - print only error messages
	//1 - print only stat messages and error mesas
	//2 - print stat messages and stack traces
	int m_verbosity = 2;
	String m_prefix;
	
	public Log(String prefix) {
		super();
		m_prefix = prefix;
	}
	
	public void setVerbose(int verb)
	{
		m_verbosity = verb;
	}

	/**
	 * prints str if m_verbosity>=level
	 * @param level
	 */
	public void print(int level, String str)
	{
		if(m_verbosity>=level)
			System.out.println("DM:" +m_prefix + ":" + level + ":"+ str);
	}

	

}
