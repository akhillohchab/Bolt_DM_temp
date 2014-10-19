package com.sri.bolt.dm;

public enum DmSegmentTypeEnum {

   DISPLAY_STRING("Display_String"), DISPLAY_ERROR_WORDS("Display_Error_Words"), DISPLAY_PREVIOUS("Display_Previous"), DISPLAY_PREVIOUS_SHORTER(
           "Display_Previous_Shorter"), DISPLAY_NEXT("Display_Next"), DISPLAY_HEADWORD("Display_Headword"), DISPLAY_AMBIGUOUS_WORDS_SPELLING(
           "Display_Ambiguous_Words_Spelling"), DISPLAY_AMBIGUOUS_WORDS_DESCRIPTION(
           "Display_Ambiguous_Words_Description"), DISPLAY_OPTIONS("Display_Options"),
           DISPLAY_SPELLING("Display_Spelling"), DISPLAY_SENTENCE("Display_Sentence"), PLAY_AUDIO(
           "Play_Audio"), PLAY_SPELLING("Play_Spelling"), TRANSLATE_ALL("Translate_All"), RESTART("Restart");

   private String value;

   DmSegmentTypeEnum(String value) {
      this.value = value;
   }

   public String value() {
      return value;
   }

   public static DmSegmentTypeEnum fromString(String stringValue) {
      DmSegmentTypeEnum returnValue = DISPLAY_STRING;
      if (stringValue != null && !(stringValue.isEmpty())) {
         for (DmSegmentTypeEnum value : DmSegmentTypeEnum.values()) {
            if (value.value().equalsIgnoreCase(stringValue))
               return value;
         }
      }
      return returnValue;
   }

}
