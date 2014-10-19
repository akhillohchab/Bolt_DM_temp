package com.sri.bolt.dm;

import java.util.ArrayList;import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import com.sri.bolt.App;
import com.sri.bolt.message.BoltMessages.DmClarificationType;

public class DmResponseTable {
    private static final Random randomNumberGenerator = new Random();
    
	
	private static final String DM_ACTION_TAG = "dm_action";
	private static final String DM_ACTION_NAME_TAG = "name";
	private static final String DM_ACTION_RESPONSE_TAG = "response";
	private static final String DM_ACTION_RULEID_TAG = "rule_id";
	private static final String DM_ACTION_TARGET_ATTR_TAG = "target_attr";
	private static final String DM_ACTION_SEGMENT_TAG = "segment";
	private static final String DM_ACTION_SEGMENT_TEXT_TAG = "text";
	private static final String DM_ACTION_SEGMENT_ACTION_TAG = "action";
	private static final String DM_ACTION_SEGMENT_TTS_TAG = "tts_feature";
	private static final String DM_ACTION_SEGMENT_PREFIX_TAG = "prefix";
	private static final String DM_ACTION_SEGMENT_SUFFIX_TAG = "suffix";
	private static final String DM_ACTION_SEGMENT_CONNECTOR_TAG = "connector";
	
	private Document doc;
    private Map<String, Map<String, List<DmResponse>>> responseTable;
    private boolean randomize;
    
	public DmResponseTable(String xmlfile, Language language, boolean randomize){
		this.randomize = randomize; 
        responseTable = new HashMap<String, Map<String, List<DmResponse>>>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlfile);
            parseDocument(language);
        } 
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void parseDocument(Language language) throws Exception {
        Element docEle = doc.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName(DM_ACTION_TAG);
        if (nl != null && nl.getLength() > 0) {
            // for each dm_action
            for (int i = 0; i < nl.getLength(); i++) {
                Map<String, List<DmResponse>> responseMap = new HashMap<String, List<DmResponse>>();
            	Element dmActionElement = (Element) nl.item(i);
                String dmActionName = getTagValue(dmActionElement, DM_ACTION_NAME_TAG);
                DmClarificationType dmActionType = DmResponseTypeRuleIdPair.getDmClarificationEnum(dmActionName);
                if (!dmActionName.equals(DialogManager.DM_ACTION_TRANSLATE) && 
                		!dmActionName.equals(DialogManager.DM_ACTION_PREAMBLE_SORRY) &&
                     !dmActionName.equals(DialogManager.DM_ACTION_RESTART) &&
                     !dmActionName.equals(DialogManager.DM_ACTION_MOVE_ON) &&
                     dmActionType == null)
                	throw new Exception("Undefined dm action: " + dmActionName);
                NodeList responseList= dmActionElement.getElementsByTagName(DM_ACTION_RESPONSE_TAG);
                if (responseList != null && responseList.getLength() > 0){
                	for (int j= 0 ; j < responseList.getLength(); j++){
                        DmResponse singleResponse = new DmResponse();
                		Element responseElement = (Element) responseList.item(j);
                		String targetAttr = getTagValue(responseElement, DM_ACTION_TARGET_ATTR_TAG);
                		singleResponse.setTargetedAttribute(DmResponseTypeRuleIdPair.getErrorSegmentAttributeEnum(targetAttr));
                		String ruleId = getTagValue(responseElement, DM_ACTION_RULEID_TAG);
                		if (ruleId == null)
                			throw new Exception("Missing rule id: " + dmActionName);
                		List<DmResponse> responses = responseMap.get(ruleId);
                		if (responses == null){
                			responses = new ArrayList<DmResponse>();
                			responseMap.put(ruleId, responses);
                		}
                        NodeList segmentList = responseElement.getElementsByTagName(DM_ACTION_SEGMENT_TAG);
                        if (segmentList != null && segmentList.getLength() > 0){
                        	for (int k=0; k < segmentList.getLength(); k++){
                        		Element segmentElement = (Element) segmentList.item(k);
	                        	String text = getTagValue(segmentElement, DM_ACTION_SEGMENT_TEXT_TAG);
	                        	String action = getTagValue(segmentElement, DM_ACTION_SEGMENT_ACTION_TAG);
	                        	String ttsFeature = getTagValue(segmentElement, DM_ACTION_SEGMENT_TTS_TAG);
	                        	String prefix = getTagValue(segmentElement, DM_ACTION_SEGMENT_PREFIX_TAG);
	                        	String suffix = getTagValue(segmentElement, DM_ACTION_SEGMENT_SUFFIX_TAG);
	                        	String connector = getTagValue(segmentElement, DM_ACTION_SEGMENT_CONNECTOR_TAG);
	                        	DmSegment dmSegment = new DmSegment(text, action, ttsFeature, prefix, suffix, connector, language);
	                        	singleResponse.addSegment(dmSegment);
                        	}
                            responses.add(singleResponse);
                        }
                	}
                	if (!responseMap.isEmpty())
                		responseTable.put(dmActionName,responseMap);
                }
            }
        }
    }


	private String getTagValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }
				
	public DmResponse getResponse(DmResponseTypeRuleIdPair pair){
		return getResponse(pair.getType(), pair.getRuleId());
	}
	
	public DmResponse getResponse(String dmAction, String ruleId){
		Map<String, List<DmResponse>> responseMap =  responseTable.get(dmAction);
		if (responseMap != null && !responseMap.isEmpty()){
			List<DmResponse> responses = responseMap.get(ruleId);
			if (responses != null && !responses.isEmpty()){
				int responseIndex = 0;
				if (randomize)
					responseIndex = randomNumberGenerator.nextInt(responses.size());
				return responses.get(responseIndex);
			}
		}
		return null;
	}
	
	public void printAllResponses(){
		List<String> dmActions = new ArrayList<String>(responseTable.keySet());
		Collections.sort(dmActions);
		for (String type : dmActions){
			logger.info(type);
			Map<String, List<DmResponse>> responseMap =  responseTable.get(type);
			List<String> ruleIds = new ArrayList<String>(responseMap.keySet());
			Collections.sort(ruleIds);
			for (String ruleId : ruleIds){
				logger.info("  " + ruleId);
				List<DmResponse> responses = responseMap.get(ruleId);
				for (DmResponse response : responses){
					logger.info("    " + response);
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			DmResponseTable dmResponseParser = new DmResponseTable(args[0], args[1] == "arabic" ? Language.IRAQI_ARABIC :Language.ENGLISH, true);
			dmResponseParser.printAllResponses();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

   private static final Logger logger = LoggerFactory.getLogger(DmResponseTable.class);
}
