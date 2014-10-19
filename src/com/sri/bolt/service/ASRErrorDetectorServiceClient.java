package com.sri.bolt.service;

import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class ASRErrorDetectorServiceClient extends CPPSocketServiceClient {
   public ASRErrorDetectorServiceClient(Properties config, Language lang) {
      super(config);
      String pre = lang == Language.IRAQI_ARABIC ? "ia" : "en";
      final String globalPrefix = "ASRErrorDetection";
      String basename = "ASRErrorDetectorServiceClient";
      final String prefix = pre + "." + globalPrefix;
      name = basename + ((pre.compareTo("ia") == 0)?"-IA":"-EN");

      mEndpointAddress = config.getProperty(prefix + "Addr");
      mEndpointPath = config.getProperty(prefix + "Path");
      mEndpointExe = config.getProperty(prefix + "Exe");
      port = config.getProperty(prefix + "Port");
      // Note that we expand %var% in args
      mArgs = config.getProperty(prefix + "Args").trim();

      // Break arguments into array and resolve %var%
      mResolvedArgs = com.sri.bolt.Util.splitAndResolve(config, mArgs);

      // convert to absolute path
      mEndpointPath = new File(mEndpointPath).toURI().normalize().getPath();

      init();
   }

   @Override
   protected String getFunctionCallName() {
      return "markASRErrors";
   }

   private Language lang;
   private static final Logger logger = LoggerFactory.getLogger(ASRErrorDetectorServiceClient.class);
}
