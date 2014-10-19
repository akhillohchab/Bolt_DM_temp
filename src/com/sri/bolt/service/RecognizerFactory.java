package com.sri.bolt.service;

import java.util.Properties;

public class RecognizerFactory {
   public enum RecognizerType {
      XMLRPC, SOCKET_EN, SOCKET_EN_SEC, SOCKET_EN_FA, SOCKET_IA, SOCKET_IA_SEC, SOCKET_IA_FA;

      public String prefixId() {
         switch (RecognizerType.this) {
         case SOCKET_EN:
            return "en";
         case SOCKET_EN_SEC:
            return "en_sec";
         case SOCKET_EN_FA:
            return "en_fa";
         case SOCKET_IA:
             return "ia";
         case SOCKET_IA_SEC:
             return "ia_sec";
         case SOCKET_IA_FA:
             return "ia_fa";
         default:
            return RecognizerType.this.name();
         }
      }

      public String logId() {
         return prefixId();
      }
   }

   static public DynaSpeakSocketServiceClient create(RecognizerType type, Properties props) {
      DynaSpeakSocketServiceClient retval = null;
      try {
      // For logging, want to be able to tell the difference between these recognizers
      String uniqueId = type.logId();
      DynaSpeakConfig config = new DynaSpeakConfig(uniqueId);

      String prefix = type.prefixId();
      config.endpointAddress = props.getProperty(prefix + ".DynaspeakEndpointAddr");
      config.port = props.getProperty(prefix + ".DynaspeakEndpointPort");
      config.dynaspeakPath = props.getProperty(prefix + ".DynaspeakPath");
      config.dynaspeakExe = props.getProperty(prefix + ".DynaspeakExe");
      config.initTimeoutSeconds = Integer.parseInt(props.getProperty(prefix + ".DynaspeakInitTimeoutSeconds"));
      String args = props.getProperty(prefix + ".DynaspeakArgs").trim();
      config.dynaspeakArgs = com.sri.bolt.Util.splitAndResolve(props, args);
      retval = new DynaSpeakSocketServiceClient(props, config);
      } catch (Exception e) {
         int i = 0;
      }

      return retval;
   }
}
