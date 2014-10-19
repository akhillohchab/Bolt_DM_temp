package com.sri.bolt.audio;


import com.sri.bolt.App;
import com.sri.bolt.service.RecognizerFactory.RecognizerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioFeeder implements Runnable {
   public AudioFeeder(RecognizerType recogType) {
      this.recogType = recogType;
      samplesToSend = new LinkedBlockingQueue<byte[]>();
   }

   public void addAudio(byte[] audio) {
      samplesToSend.add(audio);
   }

   @Override
   public void run() {
      logger.info("Starting feeding for:" + recogType);
      try {
         byte[] buffer;
         while ((buffer = samplesToSend.take()).length != 0) {
            App.getApp().getServiceController().sendSamples(recogType, buffer);
         }
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      logger.info("Finished feeding for:" + recogType);
   }

   private static final Logger logger = LoggerFactory.getLogger(AudioFeeder.class);
   private RecognizerType recogType;
   private BlockingQueue<byte[]> samplesToSend;
}
