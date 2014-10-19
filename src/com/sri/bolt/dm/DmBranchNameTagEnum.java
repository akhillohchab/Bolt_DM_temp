package com.sri.bolt.dm;

public enum DmBranchNameTagEnum {
	PERSON("Person"),
	LOCATION("Location"),
	NONE("None");
		
	private String value;
	
	DmBranchNameTagEnum(String value) {
		this.value = value;
	}
	
	public String value(){
		return value;
	}
	
	public static DmBranchNameTagEnum fromString(String stringValue){
		DmBranchNameTagEnum returnValue = null;
		if (stringValue != null && !(stringValue.isEmpty())){
			for (DmBranchNameTagEnum value : DmBranchNameTagEnum.values()){
				if (value.value().equalsIgnoreCase(stringValue))
					return value;
			}
		}
		return returnValue;
	}
}
