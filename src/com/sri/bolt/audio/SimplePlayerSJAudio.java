package com.sri.bolt.audio;

import com.sri.audio.AudioReader;
import com.sri.audio.SJAudio;
import com.sri.interfaces.audio.AudioFinishedRunnable;
import com.sri.interfaces.audio.AudioProperties;
import com.sri.jsound.JSoundAudioPlayerFactory;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wrap SJAudio player into a SimplePlayerInterface
public class SimplePlayerSJAudio implements SimplePlayerInterface {
   // Can optionally pad silence at end with >= 0
   public SimplePlayerSJAudio(AudioProperties p, String filename, int numSamplesPad) {
      short[] samplesUnpadded = AudioReader.loadFileNE(new File(filename));
      //logger.info("loaded samples: " + (samplesUnpadded != null?samplesUnpadded.length:0));
      // Pad
      short[] samples = samplesUnpadded;
      if ((numSamplesPad > 0) && (samplesUnpadded != null)) {
         // End will be 0-initialized in Java
         samples = new short[samplesUnpadded.length + numSamplesPad];
         for (int i = 0; i < samplesUnpadded.length; i++) {
            samples[i] = samplesUnpadded[i];
         }
      }

      mAudio = new SJAudio(p, null, new JSoundAudioPlayerFactory());

      mSamples = samples;
   }

    public SimplePlayerSJAudio(AudioProperties p, short[] samples) {
      mSamples = samples;

      mAudio = new SJAudio(p, null, new JSoundAudioPlayerFactory());
   }

   @Override
   public boolean play(final Runnable onFinished) {
      boolean retval = false;

      Thread playThread = mAudio.playGetThread((mSamples != null)?mSamples:new short[0], onFinished);
      retval = (playThread == null)?false:true;

      return retval;
   }

   @Override
   public void waitFor() {
      mAudio.waitForIdle(-1);
   }

   @Override
   public void stopPlaying(boolean blocking) {
      mAudio.stopPlaying(blocking);
   }

   //private final String mFilename;
   private final short[] mSamples;
   private final SJAudio mAudio;

   private static final Logger logger = LoggerFactory.getLogger(SimplePlayerSJAudio.class);
}
