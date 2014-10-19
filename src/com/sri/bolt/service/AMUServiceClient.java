package com.sri.bolt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class AMUServiceClient extends CPPSocketServiceClient {
   public AMUServiceClient(Properties config, String pre) {
      super(config);
      final String globalPrefix = "AMU";
      String basename = "AMUServiceClient";
      final String prefix = pre + "." + globalPrefix;
      name = basename + ((pre.compareTo("ia") == 0)?"-IA":"-EN");

      mEndpointAddress = config.getProperty(globalPrefix + "EndpointAddr");
      mEndpointPath = config.getProperty(globalPrefix + "Path");
      mEndpointExe = config.getProperty(globalPrefix + "Exe");
      mEndpointInitTimeoutSeconds = Integer.parseInt(config.getProperty(globalPrefix + "InitTimeoutSeconds"));
      mEndpointConfig = config.getProperty(prefix + "Config");
      port = config.getProperty(prefix + "EndpointPort");
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
      return "processSentence";  //To change body of implemented methods use File | Settings | File Templates.
   }

   private static final Logger logger = LoggerFactory.getLogger(AMUServiceClient.class);
}
