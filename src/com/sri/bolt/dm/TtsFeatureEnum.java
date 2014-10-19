package com.sri.bolt.dm;

public enum TtsFeatureEnum {
	EMPHASIZE("Emphasize"),
	SLOW_SPEED("Slow_Speed"),
	NONE("None");
	
	private String value;
	
	TtsFeatureEnum(String value) {
		this.value = value;
	}
	
	public String value(){
		return value;
	}
	
	public static TtsFeatureEnum fromString(String stringValue){
		TtsFeatureEnum returnValue = null;
		if (stringValue != null && !(stringValue.isEmpty())){
			for (TtsFeatureEnum value : TtsFeatureEnum.values()){
				if (value.value().equalsIgnoreCase(stringValue))
					return value;
			}
		}
		return returnValue;
	}
	
	public static String getTtsString(TtsFeatureEnum ttsFeature, String input){
		//if (ttsFeature == EMPHASIZE)
			//return "<emphasis level='strong'>" + input + "</emphasis>"; 
		if (ttsFeature == SLOW_SPEED)
			return "<vtml_speed value=\"90\">" + input + "</vtml_speed>";
		return input;
	}
	
	public static String getSpellingMarkup(String spelling){
		//OLD version
		//return spelling.trim().replaceAll("", ".").substring(1);
		if (spelling != null){
			//String splitWord = spelling.replaceAll("", " ");
			//return "<vtml_speed value=\"80\"><vtml_sayas interpret-as=\"ssml:characters\">" + splitWord.trim() + "</vtml_sayas></vtml_speed>";
			String splitWord = spelling.replaceAll("", "<vtml_break level=\"2\"/>");
			return splitWord;
		}
		return "";
	}
}
