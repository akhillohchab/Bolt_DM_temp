package com.sri.bolt.service;

public class DynaSpeakConfig {
   public String dynaspeakPath;
   public String dynaspeakExe;
   public String[] dynaspeakArgs;
   public String endpointAddress;
   public String port;
   public int initTimeoutSeconds;
   // Added so DynaSpeakSocketServiceClient can have unique name for each recognizer
   public final String uniqueId;

   public DynaSpeakConfig(String uniqueId) {
      this.uniqueId = uniqueId;
   }
}
