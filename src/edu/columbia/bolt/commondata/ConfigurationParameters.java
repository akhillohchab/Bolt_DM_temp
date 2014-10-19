package edu.columbia.bolt.commondata;

/**
 * configuration parameters for COLUMBIA BOLT components
 * singleton class 
 * @author sstoyanchev
 *
 */
public class ConfigurationParameters {
	
	private static ConfigurationParameters _instance;
	
	
	/**
	 * set level of verbosity for logging 
	 */
	public int m_verbouse = 1;


	public enum DMVersionType{STUB, RULEBASED};
	public enum TaggerVersionType{STANFORD, MARCELLE};
	public enum ConfScorerVersionType{ASR, ASRPROSPOS, ASRPOS}
	public enum ConfScorerStageType{OneStage, TwoStage}
	
	public DMVersionType dmVersion = DMVersionType.STUB;	
	public TaggerVersionType taggerVersion = TaggerVersionType.STANFORD;
	public ConfScorerVersionType confScorer = ConfScorerVersionType.ASRPROSPOS;
	public ConfScorerStageType confStage = ConfScorerStageType.OneStage;
	
	//Path to the model for the words classifier
	public String confScorerWordScoreWekaModelpath = "../Externals/ConfScorerWords_Columbia_V3.2_fixed.model";
	//Path to the model for the utterance classifier
	public String confScorerUttsScoreWekaModelpath = "../Externals/ConfScorerUtts_Columbia_V0.model";
	
	
	//get release version
	public String getReleaseVersion()
	{
		return "JULY10,2012";
	}
	
	private ConfigurationParameters(){}
	
	
	//set confidence thresholds
	//if confodence is below threshold, do not consider this variable
	public final double ASR_OOV_CONF_THRESHOLD = .4;
	public final double MT_OOV_CONF_THRESHOLD = .4;
	public final double ASR_AMBIG_CONF_THRESOLD = .4;
	public final double MT_AMBIG_CONF_THRESOLD = .4;
	public final double NE_CONF_THRESHOLD = .4;
	
	public synchronized static ConfigurationParameters getInstance(){
		if (_instance==null)
			_instance = new ConfigurationParameters();
		return _instance;
	}
	
	
	
	
}
