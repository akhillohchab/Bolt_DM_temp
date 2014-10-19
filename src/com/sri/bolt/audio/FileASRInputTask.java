package com.sri.bolt.audio;

import com.sri.bolt.App;
import com.sri.bolt.TimeKeeper;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class FileASRInputTask extends ASRInputTask {
   public FileASRInputTask(File audioFile) {
      this.audioFile = audioFile;
      audioBuffer = new ByteArrayOutputStream();
   }

   @Override
   public void stop() {
      //here do nothing
   }

   @Override
   public TaskReturn call() {
      try {
         audioExecutor = Executors.newFixedThreadPool(secondRecognizerType == null ? 1 : 2);
         recog1AudioFeeder = new AudioFeeder(recognizerType);

         Future<?> recog1Future;
         Future<?> recog2Future = null;
         recog1Future = audioExecutor.submit(recog1AudioFeeder);
         if (secondRecognizerType != null) {
            recog2AudioFeeder = new AudioFeeder(secondRecognizerType);
            recog2Future = audioExecutor.submit(recog2AudioFeeder);
         }
         App.getApp().getServiceController().startSamples(recognizerType, lmIndex);
         if (secondRecognizerType != null) {
            App.getApp().getServiceController().startSamples(secondRecognizerType, lmIndex);
         }
         AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
         int numBytes = 1024 * stream.getFormat().getFrameSize();
         byte[] audioBytes = new byte[numBytes];
         int numBytesRead = 0;
         int totalBytes = 0;
         // Try to read numBytes bytes from the file.
         while ((numBytesRead = stream.read(audioBytes)) != -1) {
            totalBytes += numBytesRead;
            recog1AudioFeeder.addAudio(audioBytes);
            if (secondRecognizerType != null) {
               recog2AudioFeeder.addAudio(audioBytes);
            }
            audioBuffer.write(audioBytes);
            //allocate new array so that audio doesn't get overwritten in other threads
            audioBytes = new byte[numBytes];
         }
         recog1AudioFeeder.addAudio(new byte[]{});
         if (secondRecognizerType != null) {
            recog2AudioFeeder.addAudio(new byte[]{});
         }

         boolean cleanTermination = false;
         try {
            recog1Future.get();
            if (secondRecognizerType != null) {
               recog2Future.get();
            }
            audioExecutor.shutdown();
            cleanTermination = audioExecutor.awaitTermination(5, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            logger.error("Clean shutdown of audio executor failed", e);
         } catch (ExecutionException e) {
            logger.error("Clean shutdown of audio executor failed", e);
         }
         if (!cleanTermination) {
            logger.error("Clean shutdown of audio executor failed");
         }

         this.state.buttonReleased();
         long time = (long)(((double)totalBytes / 32000.0) * 1000.0);
         TimeKeeper timeKeeper = App.getApp().getTimeKeeper();
         timeKeeper.addTime("ASR File Length " + (language == Language.IRAQI_ARABIC ? "IA" : "EN"), time);
      } catch (UnsupportedAudioFileException e) {
         logger.error("File ASR failed with exception:" + e, e);
         return new TaskReturn(false, e.getMessage());
      } catch (IOException e) {
         logger.error("File ASR failed with exception:" + e, e);
         return new TaskReturn(false, e.getMessage());
      }

      return new TaskReturn(true, "");
   }

   public ByteArrayOutputStream getFullData() {
      return audioBuffer;
   }

   private static final Logger logger = LoggerFactory.getLogger(FileASRInputTask.class);
   public ByteArrayOutputStream audioBuffer;
   private File audioFile;
   private AudioFeeder recog1AudioFeeder;
   private AudioFeeder recog2AudioFeeder;
   private ExecutorService audioExecutor;
}
