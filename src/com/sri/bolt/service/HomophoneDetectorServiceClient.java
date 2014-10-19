package com.sri.bolt.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sri.bolt.homophone.HomophoneDetector;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.interfaces.lang.Language;

public class HomophoneDetectorServiceClient implements SessionDataServiceClient {
   public HomophoneDetectorServiceClient(Properties props, Language language) {
      this.language = language;
      if (language == Language.ENGLISH) {
         filePath = props.getProperty("en.HomophoneList");
      } else {
         filePath = props.getProperty("ia.HomophoneList");
      }
      init();
   }

   @Override
   public void init() {
      detector =  new HomophoneDetector(filePath, language);
   }

   @Override
   public void reinit() {
   }
   
   @Override
   public SessionData checkInput(SessionData data) {
      //TODO add error checking
      return data;
   }

   @Override
   public SessionData process(SessionData data) {
      SessionData checkedData = checkInput(data);
      return detector.processHomophones(checkedData);
   }

   @Override
   public void cleanup() {
      // TODO Auto-generated method stub
      
   }

   private Language language;
   private HomophoneDetector detector;
   private String filePath;
}
