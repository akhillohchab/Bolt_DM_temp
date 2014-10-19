package com.sri.bolt.dm;

public enum DmBranchPosTagEnum {
	JJ("JJ"),
	NN("NN"),
	NNP("NNP");
		
	private String value;
	
	DmBranchPosTagEnum(String value) {
		this.value = value;
	}
	
	public String value(){
		return value;
	}
	
	public static DmBranchPosTagEnum fromString(String stringValue){
		DmBranchPosTagEnum returnValue = null;
		if (stringValue != null && !(stringValue.isEmpty())){
			for (DmBranchPosTagEnum value : DmBranchPosTagEnum.values()){
				if (value.value().equalsIgnoreCase(stringValue))
					return value;
			}
		}
		return returnValue;
	}
}
