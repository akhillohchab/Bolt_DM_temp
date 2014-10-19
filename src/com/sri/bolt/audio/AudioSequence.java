package com.sri.bolt.audio;

import com.sri.bolt.Util;
import com.sri.audio.util.AudioEnergyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import javax.sound.sampled.DataLine.Info;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioSequence {
   // Note: removed direct access to byteSequence.write() in favor of wrapping
   // in case we normalize.

   public AudioSequence() {
      byteSequence = new ByteArrayOutputStream();
      format = new AudioFormat(Util.SAMPLE_RATE, Util.BYTES_PER_SAMPLE * 8, 1, Util.SIGNED_AUDIO, Util.BIG_ENDIAN_AUDIO);
   }

   public void addFileData(File file, int startMS, int endMS) {
      try {
         AudioInputStream stream = AudioSystem.getAudioInputStream(file);

         // Sanity checking:
         if (startMS < 0) {
            startMS = 0;
         }
         if (endMS < startMS) {
            endMS = startMS;
         }

         AudioInputStream convertedBytes = AudioSystem.getAudioInputStream(format, stream);
         int startSample = (startMS * Util.SAMPLE_RATE) / 1000;
         int endSample = (endMS * Util.SAMPLE_RATE) / 1000;
         int startByte = startSample * Util.BYTES_PER_SAMPLE;
         int endByte = endSample * Util.BYTES_PER_SAMPLE;
         byte[] buffer = new byte[endByte - startByte];
         convertedBytes.skip(startByte);
         // Note that all the requested bytes might not be available in which case
         // anything else will be initialized to 0 or silence. In general, though,
         // we don't expect the offsets to be too far off but there could be cases
         // where they leak outside the actual recorded audio a bit.
         convertedBytes.read(buffer, 0, endByte - startByte);
         appendData(buffer);

      } catch (UnsupportedAudioFileException e) {
         logger.error(e.getMessage(), e);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   public void addByteDataBySeconds(byte[] bytes, double startSeconds, double endSeconds) {
      int startSample = (int) (startSeconds * Util.SAMPLE_RATE);
      int endSample = (int) (endSeconds * Util.SAMPLE_RATE);
      int startByte = startSample * Util.BYTES_PER_SAMPLE;
      int endByte = endSample * Util.BYTES_PER_SAMPLE;
      appendData(bytes, startByte, endByte - startByte);
   }

   public void addFileData(File file) {
      AudioInputStream stream;
      try {
         stream = AudioSystem.getAudioInputStream(file);
         AudioInputStream convertedBytes = AudioSystem.getAudioInputStream(format, stream);
         // 320000 is so we can hold 10 seconds of 16 kHz 16 bit data
         byte[] audioBytes = new byte[320000];
         int nread;
         while ((nread = convertedBytes.read(audioBytes)) != -1) {
            appendData(audioBytes, 0, nread);
         }
      } catch (UnsupportedAudioFileException e) {
         logger.error(e.getMessage(), e);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }

   }

   public void addByteData(byte[] data, int startFrame, int endFrame) {
      int startSample = startFrame * Util.FRAME_ADVANCE_IN_SAMPLES;
      int endSample = endFrame * Util.FRAME_ADVANCE_IN_SAMPLES;
      int startByte = startSample * Util.BYTES_PER_SAMPLE;
      int endByte = endSample * Util.BYTES_PER_SAMPLE;
      appendData(data, startByte, endByte);
   }

   public void addBuffer(int ms) {
      int numSamples = Util.SAMPLE_RATE * ms / 1000;
      int numBytes = numSamples * Util.BYTES_PER_SAMPLE;
      try {
         // OK HERE SINCE ADDING SILENCE
         byteSequence.write(new byte[numBytes]);
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   public void playAudio() {
      try {
         SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new Info(SourceDataLine.class, format));
         line.open();
         line.start();
         byte[] data = byteSequence.toByteArray();
         line.write(data, 0, data.length);
      } catch (LineUnavailableException e) {
         logger.error(e.getMessage(), e);
      }
   }

   public ByteArrayOutputStream getBytes() {
      return byteSequence;
   }

   private void appendData(byte[] buffer) {
      appendData(buffer, 0, buffer.length);
   }

   private void appendData(byte[] buffer, int off, int count) {
      if (!mNormalize) {
         byteSequence.write(buffer, off, count);
      } else {
         // Make a buffer copy since we update the data
         byte[] nbuffer = new byte[count];

         // XXX Assume little endian with true
         for (int i = 0; i < nbuffer.length; i++) {
            nbuffer[i] = buffer[i + off];
         }
         // 205 is about what TTS produces currently
         AudioEnergyUtil.setEnergy16bit(205, nbuffer, true);
         byteSequence.write(nbuffer, 0, nbuffer.length);
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(AudioSequence.class);
   private AudioFormat format;
   private ByteArrayOutputStream byteSequence;
   private boolean mNormalize = true;
}
