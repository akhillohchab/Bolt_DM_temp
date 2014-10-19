package com.sri.bolt.service;

import java.util.Properties;

import com.sri.bolt.dm.DialogManager;
import com.sri.bolt.message.BoltMessages.SessionData;

import com.sri.interfaces.lang.Language;
import edu.columbia.bolt.commondata.ConfigurationParameters;
import edu.columbia.bolt.logic.DMLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogManagerServiceClient implements SessionDataServiceClient {
	private static final String SRI_DM = "SRI-DM";
	private static final String CU_DM = "CU-DM";
	private String dmType;
	
	private ConfigurationParameters confParams;
	private DMLogic cuDialogManager;
	
	private DialogManager sriDialogManager;
    private String dmResponseFile;
    private String dmStrategyFile;
    private String dmPriorities;
    private boolean dmRandomizeResponse = false;
	private static final int MAX_RETRIES = 2;

   private Language language;

	public DialogManagerServiceClient(Properties props, Language lang) {
		dmType = props.getProperty("DialogManager");
      this.language = lang;
		if (dmType.equals(CU_DM)){
			confParams = ConfigurationParameters.getInstance();
			confParams.dmVersion = ConfigurationParameters.DMVersionType.RULEBASED;
		}
		else if (dmType.equals(SRI_DM)){
			dmResponseFile = props.getProperty(lang == Language.ENGLISH ? "en.DmResponseFile" : "ia.DmResponseFile");
			dmStrategyFile = props.getProperty(lang == Language.ENGLISH ? "en.DmStrategyFile" : "ia.DmStrategyFile");
			dmPriorities = props.getProperty(lang == Language.ENGLISH ? "en.DmPriorities" : "ia.DmPriorities");
			dmRandomizeResponse = Boolean.parseBoolean(props.getProperty("DmRandomizeResponses"));
		}
		init();
	}

	@Override
	public void init() {
		if (dmType.equals(CU_DM)){
			cuDialogManager = new DMLogic();
			sriDialogManager = null;
		}
		else if (dmType.equals(SRI_DM)){
			cuDialogManager = null;
			sriDialogManager = new DialogManager(dmResponseFile, dmStrategyFile, dmPriorities, language, dmRandomizeResponse);
		}
	}

	@Override
    public void reinit() {
    }

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
	}

	@Override
	public SessionData checkInput(SessionData data) {
		// TODO add error checking
		return data;
	}

	@Override
	public SessionData process(SessionData data) {
		SessionData checkedData = checkInput(data);
		int numTry = 0;
		while (numTry < MAX_RETRIES) {
			try {
				if (dmType.equals(CU_DM))
					return cuDialogManager.process(confParams, checkedData);
				else if (dmType.equals(SRI_DM))
					return sriDialogManager.process(checkedData);
			} catch (Throwable t) {
				logger.error("Dialog Manager process failed: " + t.getMessage() + ". Retrying", t);
				init();
				numTry++;
			}
		}
		return null;
	}

   private static final Logger logger = LoggerFactory.getLogger(DialogManagerServiceClient.class);
}
