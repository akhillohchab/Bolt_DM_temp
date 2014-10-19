package com.sri.bolt.audio;

import com.sri.audio.util.AudioBufConverter;
import com.sri.audio.util.AudioEnergyUtil;
import com.sri.bolt.App;
import com.sri.bolt.Util;
import com.sri.bolt.service.RecognizerFactory.RecognizerType;
import com.sri.jsound.AudioBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import javax.sound.sampled.Line.Info;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LiveASRInputTask extends ASRInputTask {
    /*
   public LiveASRInputTask() {
      format = new AudioFormat(Util.SAMPLE_RATE, Util.BYTES_PER_SAMPLE * 8, 1, Util.SIGNED_AUDIO,
            Util.BIG_ENDIAN_AUDIO);

      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      try {
         line = (TargetDataLine) AudioSystem.getLine(info);
      } catch (LineUnavailableException e) {
         App.getApp().getLogger().debug(e.getMessage());
      }

      isRecording = new AtomicBoolean(false);
      fullData = new ByteArrayOutputStream();
   }
   */

   public LiveASRInputTask() {
      format = new AudioFormat(Util.SAMPLE_RATE, Util.BYTES_PER_SAMPLE * 8, 1, Util.SIGNED_AUDIO,
              Util.BIG_ENDIAN_AUDIO);

      Mixer superMixer = null;
      logger.debug("The following are valid input channels");
      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
      for (int i = 0; i < mixerInfo.length; i++) {
         //App.getApp().getLogger().debug(mixerInfo[i].getVendor() + " " + mixerInfo[i].getName() + " " + mixerInfo[i].getDescription());
         if (mixerInfo[i].getName().contains("AK5370")) {
           logger.debug("Found supermic mixer info: " + mixerInfo[i].getName() + " description: " + mixerInfo[i].getDescription());
            Mixer m = AudioSystem.getMixer(mixerInfo[i]);
            Info[] infos = m.getTargetLineInfo();
            if ((infos != null) && (infos.length > 0)) {
               // Found supermic!
               superMixer = m;
               break;
            } else {
               // Only log if had no target lines
               logger.debug("Number of target lines: " + (infos != null ? infos.length : 0));
            }
         }
      }

      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      line = null;
      if (superMixer != null) {

         try {
            line = (TargetDataLine) superMixer.getLine(info);
         } catch (IllegalArgumentException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         } catch (LineUnavailableException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         } catch (SecurityException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         }
      } else {
         logger.debug("Didn't find mixer for supermic, using anything");
      }
      if (line == null) {
         try {
            line = (TargetDataLine) AudioSystem.getLine(info);
         } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
         } catch (LineUnavailableException e) {
            logger.error(e.getMessage(), e);
         } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
         }
      }

      /*
      if (line != null) {
          App.getApp().getLogger().debug("Audio buffer size bytes: " + line.getBufferSize());
      }
      */

      isRecording = new AtomicBoolean(false);
      fullData = new ByteArrayOutputStream();
   }

   /**
    * This constructor takes a string that represents the mixer that the user set in the config file
    *
    * @param mixer
    */
   public LiveASRInputTask(String mixer) {
      mDeviceName = mixer;
      format = new AudioFormat(Util.SAMPLE_RATE, Util.BYTES_PER_SAMPLE * 8, 1, Util.SIGNED_AUDIO,
              Util.BIG_ENDIAN_AUDIO);
      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      //create the user mixer object and the super mixer object
      //we will try and populate both, if we find the user mixer object first then we don't care about the super mixer
      Mixer userMixer = null;
      Mixer superMixer = null;
      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
      for (int i = 0; i < mixerInfo.length; i++) {
         if (mixerInfo[i].getName().contains(mixer)) {
            logger.debug("Found user selected audio capture device mixer info: " + mixerInfo[i].getName() + " description: " + mixerInfo[i].getDescription());
            Mixer m = AudioSystem.getMixer(mixerInfo[i]);
            Info[] infos = m.getTargetLineInfo(info);
            if ((infos != null) && (infos.length > 0)) {
               // Found usermic!
               userMixer = m;
               break;
            } else {
               // Only log if had no target lines
               logger.debug("Number of target lines for user selected audio capture device:: " + (infos != null ? infos.length : 0));
            }
         }
         if (mixerInfo[i].getName().contains("AK5370")) {
            logger.debug("Found supermic mixer info: " + mixerInfo[i].getName() + " description: " + mixerInfo[i].getDescription());
            Mixer m = AudioSystem.getMixer(mixerInfo[i]);
            Info[] infos = m.getTargetLineInfo(info);
            if ((infos != null) && (infos.length > 0)) {
               // Found supermic!
               superMixer = m;
            } else {
               // Only log if had no target lines
               logger.debug("Number of target lines for supermic: " + (infos != null ? infos.length : 0));
            }
         }
      }


      line = null;
      if (userMixer != null) {
         try {
            line = (TargetDataLine) userMixer.getLine(info);
         } catch (IllegalArgumentException e) {
            logger.error("Found user mic but couldn't get line info" + e.getMessage(), e);
         } catch (LineUnavailableException e) {
            logger.error("Found user mic but couldn't get line info" + e.getMessage(), e);
         } catch (SecurityException e) {
            logger.error("Found user mic but couldn't get line info" + e.getMessage(), e);
         }
      } else if (superMixer != null) {
         try {
            line = (TargetDataLine) superMixer.getLine(info);
         } catch (IllegalArgumentException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         } catch (LineUnavailableException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         } catch (SecurityException e) {
            logger.error("Found supermic but couldn't get line info" + e.getMessage(), e);
         }
      } else {
         logger.debug("Didn't find mixer for supermic or for the user selected audio capture device, using anything");
      }
      if (line == null) {
         try {
            line = (TargetDataLine) AudioSystem.getLine(info);
         } catch (LineUnavailableException e) {
            logger.error(e.getMessage(), e);
         }
      }

      isRecording = new AtomicBoolean(false);
      fullData = new ByteArrayOutputStream();
   }

   @Override
   public TaskReturn call() {
      return startRecording();
   }

   public ByteArrayOutputStream getFullData() {
      return fullData;
   }

   private TaskReturn startRecording() {
      logger.info("Starting recording");
      TaskReturn taskReturn = new TaskReturn(true, "");
      isRecording.set(true);
      Future<?> recog1Future = null;
      Future<?> recog2Future = null;
      try {
         audioExecutor = Executors.newFixedThreadPool(secondRecognizerType == null ? 1 : 2);
         recog1AudioFeeder = new AudioFeeder(recognizerType);
         recog1Future = audioExecutor.submit(recog1AudioFeeder);
         if (secondRecognizerType != null) {
            recog2AudioFeeder = new AudioFeeder(secondRecognizerType);
            recog2Future = audioExecutor.submit(recog2AudioFeeder);
         }

         line.open(format);
         line.start();
         reader.startBuffering(line);
         App.getApp().getServiceController().startSamples(recognizerType, lmIndex);
         if (secondRecognizerType != null) {
            App.getApp().getServiceController().startSamples(secondRecognizerType, lmIndex);
         }
         while (isRecording.get()) {
            try {
               Thread.sleep(pollMillis);
            } catch (InterruptedException ie) {
               // Ignore
            }
            readData(false);
         }
      } catch (LineUnavailableException e) {
         logger.error(e.getMessage(), e);
         taskReturn = new TaskReturn(false, e.getMessage());
      } finally {
         // This duplicates the call in the stop() method but
         // shouldn't hurt and here we know it is always called
         // due to the "finally".
         line.stop();
         reader.lineStopped();
         readData(true);
         line.close();
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
         isRecording.set(false);
      }

      return taskReturn;
   }

   @Override
   public void stop() {
      state.buttonReleased();
      logger.info("Stopping recording");
      isRecording.set(false);
      // Need to call this stop so our reader can stop
      // reading samples.
      line.stop();
      // We will block on the drain() to give reader a chance
      // to see all data available.
      line.drain();
      reader.lineStopped();

      if (mAGC) {
         doAGC();
      }
   }

   private void readData(boolean sendAll) {
      try {
         int available = reader.available();
         // 100 ms blocks is sample as 1/10 of second
         int samplesIn100ms = Util.SAMPLE_RATE / 10;
         int bytesIn100ms = samplesIn100ms * Util.BYTES_PER_SAMPLE;
         if (available >= bytesIn100ms || sendAll) {
            byte buffer[] = new byte[available];
            reader.readRaw(buffer, 0, buffer.length);
            fullData.write(buffer);
            recog1AudioFeeder.addAudio(buffer);
            if (secondRecognizerType != null) {
               recog2AudioFeeder.addAudio(buffer);
            }
         }
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   // Look at last input and adjust mic level if necessary
   private void doAGC() {
      // Only called internally if AGC enabled, and after audio
      // ends.
      // First, make sure last input is of sufficient size. If so,
      // calculate energy and adjust up or down as desired.
      // Goals are to have a valid range so adjustments are infrequent,
      // but also to adjust in small enough incremements to not swap
      // between too high and too low.
      byte[] data = null;
      if (fullData != null) {
         data = fullData.toByteArray();
         double haveSecs = ((double)data.length) / (Util.SAMPLE_RATE * Util.BYTES_PER_SAMPLE);
         if (haveSecs < 0.75) {
            // Too short, ignore
            logger.info("Last audio level unchecked, no AGC since length seconds only: " + haveSecs);
            return;
         }
      }

      short[] sdata = AudioBufConverter.littleEndianToShort(data, 0, data.length);
      int level = mEnergyUtil.getEnergy(sdata, 0, sdata.length, 1, 0);

      // Input level range is 0-255. 150 is typical for a good
      // level. Don't adjust unless outside of [130, 180] range.
      int adjusted = AudioLevelUtil.autoAdjustMicLevel(level, mDeviceName);

      logger.info("Last audio level: " + level + ", adjusted level by " + adjusted);
   }

   private AudioFeeder recog1AudioFeeder;
   private AudioFeeder recog2AudioFeeder;
   private ExecutorService audioExecutor;
   private static final Logger logger = LoggerFactory.getLogger(LiveASRInputTask.class);
   private AtomicBoolean isRecording;
   private AudioFormat format;
   private ByteArrayOutputStream fullData;
   private TargetDataLine line;
   // Check for samples every 100 millis
   private static final int pollMillis = 100;
   // Wrapper around line for reading data
   private AudioBuffer reader = new AudioBuffer(pollMillis, 30 * Util.SAMPLE_RATE * Util.BYTES_PER_SAMPLE);

   private boolean mAGC = true;
   private AudioEnergyUtil mEnergyUtil = new AudioEnergyUtil(Util.BYTES_PER_SAMPLE, 255);
   private String mDeviceName = null;
}
