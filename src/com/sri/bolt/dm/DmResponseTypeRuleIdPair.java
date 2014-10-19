package com.sri.bolt.dm;

import com.sri.bolt.message.BoltMessages.DmClarificationType;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAttributeType;

public class DmResponseTypeRuleIdPair {
	private static final String DELIMETER = ";";
	private String type;
	private String ruleId;
	
	public DmResponseTypeRuleIdPair(String type, String id){
		this.type = type;
		this.ruleId = id;
	}

	public DmResponseTypeRuleIdPair(String firstAction2) {
		if (firstAction2 != null && firstAction2.indexOf(DELIMETER) != -1){
			String[] fields = firstAction2.split(DELIMETER);
			this.type = fields[0];
			this.ruleId = fields[1];
		}
	}

	public String getType() {
		return type;
	}

	public String getRuleId() {
		return ruleId;
	}
	
	public String toString(){
		return type + ":" + ruleId;
	}

	public static DmClarificationType getDmClarificationEnum(String stringValue){
		DmClarificationType returnValue = null;
		if (stringValue != null && !(stringValue.isEmpty())){
			for (DmClarificationType value : DmClarificationType.values()){
				if (value.toString().equalsIgnoreCase(stringValue))
					return value;
			}
		}
		return returnValue;
	}
	
	public static ErrorSegmentAttributeType getErrorSegmentAttributeEnum(String stringValue){
		ErrorSegmentAttributeType returnValue = null;
		if (stringValue != null && !(stringValue.isEmpty())){
			for (ErrorSegmentAttributeType value : ErrorSegmentAttributeType.values()){
				if (value.toString().equalsIgnoreCase(stringValue))
					return value;
			}
		}
		return returnValue;
	}
}
