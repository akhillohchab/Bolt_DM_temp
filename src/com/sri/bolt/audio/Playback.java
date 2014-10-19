package com.sri.bolt.audio;

import com.sri.audio.AudioReader;
import com.sri.bolt.App;
import com.sri.interfaces.audio.AudioFinishedRunnable;
import com.sri.interfaces.audio.AudioProperties;
import com.sri.interfaces.lang.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

import com.sri.interfaces.log.DLog;
import com.sri.interfaces.log.DLogInterface;

public class Playback {
   // Experimental way to cause some idle time after playback
   public static final int IDLE_MILLIS_AFTER_PLAYBACK = 0;

   public static void playAudioFile(String filename, Language lang) {
      playAudioFile(filename, lang, null, false, true);
   }

   public static void playAudioFile(String filename, Language lang, boolean waitForQuiet) {
      playAudioFile(filename, lang, null, false, waitForQuiet);
   }

   private static void playAudioFile(String filename, Language lang, Runnable onFinished, boolean blocking, boolean waitForQuiet) {
      if (App.getApp().playAudio()) {
         playAudioFileWithJava(filename, lang, onFinished, blocking, waitForQuiet);
      }
   }

   public static String doTTS(String text, Language lang) {
      String fileName = com.sri.bolt.Util.reserveUniqueFileName(App.getApp().getRunDir().getAbsolutePath() + "/tts", ".wav");
      App.getApp().getServiceController().textToSpeech(text, fileName, lang);
      return fileName;
   }

   /* unused
   // Stop any existing playback and play right away
   public static void playTTSTextPreempt(String text, Language lang) {
      stopAllPlayback(true);
      playTTSText(text, lang);
   }
   */

   public static void playTTSText(String text, Language lang) {
      playTTSText(text, lang, null, false, true);
   }

   private static void playTTSText(String text, Language lang, Runnable onFinished, boolean blocking, boolean waitForQuiet) {
      if (App.getApp().playAudio()) {
         String filename = doTTS(text, lang);
         playAudioFile(filename, lang, onFinished, blocking, waitForQuiet);
      }
   }

   // Assumes file is 16kHz .wav
   // waitForQuiet means to not attempt to start playback until any currently playing audio finished
   private static void playAudioFileWithJava(final String filename, final Language lang, final Runnable onFinished, final boolean blocking, final boolean waitForQuiet) {
      // Assign first id to who got to this call first
      long id;
      synchronized (gLivePlayers) {
         id = mNextPlayId++;
      }
      final long myId = id;

       /*
      boolean useThread = false;
      if (!blocking && waitForQuiet && isPlaying()) {
         useThread = true;
      }
       */
      Runnable r = new Runnable() {
         public void run() {
            playAudioFileWithJavaMain(filename, lang, onFinished, blocking, waitForQuiet, myId);
         }
      };
      /*
      if (useThread) {
         Thread t = new Thread(r);
         t.start();
         // If blocking, don't use thread so no blocking case here
         // to wait for thread to finish.
      } else {
         r.run();
      }
      */

      // Always run in thread
      Thread t = new Thread(r);
      t.start();
      if (blocking) {
         try {
            t.join();
         } catch (InterruptedException e) {
            // Ignore
         }
      }
   }

   private static AudioFinishedRunnable createOnPlaybackFinishedRunnable(final SimplePlayerInterface mainAudio, final SimplePlayerInterface otherAudio, final Runnable onFinished) {
      AudioFinishedRunnable r = new AudioFinishedRunnable() {
         @Override
         public void run() {
            if (!getSucceeded()) {
               logger.warn("Playback finished with error condition");
            }

            // Remove first so any queries in calls below about
            // running audio processes will not include this.
            boolean fullyFinished = true;
            synchronized (gLivePlayers) {
               gLivePlayers.remove(mainAudio);

               if ((otherAudio != null) && (gLivePlayers.contains(otherAudio))) {
                  // If playing on two audio devices on once, make sure both finished.
                  fullyFinished = false;
                  gLivePlayers.notifyAll();
               }
            }
            if (fullyFinished) {
               // Next id available for playback
               ++mReadyPlayId;

               // Can wait to force some idle time between playback
               if (IDLE_MILLIS_AFTER_PLAYBACK > 0) {
                  try {
                     Thread.sleep(IDLE_MILLIS_AFTER_PLAYBACK);
                  } catch (InterruptedException e) {
                     // Ignore
                  }
               }
               synchronized (gLivePlayers) {
                  gLivePlayers.notifyAll();
               }

               // Only run "onFinished" once; depending on if monitoring audio
               // and which audio player finishes last.
               if (onFinished != null) {
                  onFinished.run();
               }
            }
            for (PlaybackListener l: gListeners) {
               l.playFinished();
            }
         }

      };
      return r;
   }

   private static void playAudioFileWithJavaMain(String filename, Language lang, final Runnable onFinished, boolean blocking, boolean waitForQuiet, long myId) {
      // Ultimately, all the play calls get to here

      if (waitForQuiet) {
         long start = System.currentTimeMillis();
         waitForPlayFinished(myId);
         long millis = System.currentTimeMillis() - start;
         logger.info("id=" + myId + " waited to be quiet for playback, took " + millis + " millis");
      }

      logger.info("Playing for lang: " + lang);

      final int numChannels = 1;
      AudioProperties p = new AudioProperties(com.sri.bolt.Util.SAMPLE_RATE, numChannels);
      // Can specify output device - use null since don't have to specify
      if (lang == Language.IRAQI_ARABIC) {
         p.mixerPlaybackName = App.getApp().getProps().getProperty("ia.AudioPlaybackDevice", null);
      } else {
         p.mixerPlaybackName = App.getApp().getProps().getProperty("AudioPlaybackDevice", null);
      }

      // See if user also wants to monitor playback, usually on main computer speakers,
      // but also only do this additional playback if different from main playback device.
      boolean playMonitor = Boolean.parseBoolean(App.getApp().getProps().getProperty("MonitorAudio", "true"));
      AudioProperties pMonitor = new AudioProperties(com.sri.bolt.Util.SAMPLE_RATE, numChannels);
      pMonitor.mixerPlaybackName = App.getApp().getProps().getProperty("monitor.AudioPlaybackDevice", null);

      // If supposed to monitor playback on system speaker, make sure it's a not simply the
      // same device.
      if (playMonitor &&
          (pMonitor.mixerPlaybackName != null) &&
          (p.mixerPlaybackName != null) &&
          (pMonitor.mixerPlaybackName.compareTo(p.mixerPlaybackName) == 0)) {
         playMonitor = false;
      }

      logger.info("main playback: '" + p.mixerPlaybackName + "' monitor playback: '" + pMonitor.mixerPlaybackName + "' play monitor: " + playMonitor);

      // True for external sox playback, false for playback through Java API
      final boolean soxPlayback = false;

      //final SJAudio audio = new SJAudio(p, null, new JSoundAudioPlayerFactory());
      //final SJAudio audioMonitor = playMonitor?new SJAudio(pMonitor, null, new JSoundAudioPlayerFactory()):null;
      final SimplePlayerInterface audio = soxPlayback?new SimplePlayer(filename, p.mixerPlaybackName):new SimplePlayerSJAudio(p, filename, 0);
      final SimplePlayerInterface audioMonitor = playMonitor?(soxPlayback?new SimplePlayer(filename, pMonitor.mixerPlaybackName):new SimplePlayerSJAudio(pMonitor, filename, 0)):null;

      // If monitoring audio, notify when 2nd player finishes - not sure what order
      // will finish.
      final AudioFinishedRunnable onFinishedWrapper = createOnPlaybackFinishedRunnable(audio, audioMonitor, onFinished);
      // Below, we reverse the arguments since finished happens when audioMonitor finished
      // *but* also want to make sure main audio is finished.
      final Runnable onFinishedMonitor = (audioMonitor != null)?createOnPlaybackFinishedRunnable(audioMonitor, audio, onFinished):null;

      boolean played = false;
      if (filename != null) {
         for (PlaybackListener l: gListeners) {
             l.playStarted();
         }

         // Add *before* we start playback to avoid race condition
         // where a quick finish could happen before we add
         synchronized (gLivePlayers) {
            gLivePlayers.add(audio);
            if (playMonitor) {
               gLivePlayers.add(audioMonitor);
            }
         }

         played = audio.play(onFinishedWrapper);
         boolean playedMonitor = false;
         if (playMonitor) {
            playedMonitor = audioMonitor.play(onFinishedMonitor);
            if (!playedMonitor) {
               // Error... - call onFinished manually
               if (onFinishedMonitor != null) {
                  onFinishedMonitor.run();
               }
            }
         }
         if (played) {
            // If playback started and we should block, wait for audio playback to finish
            if (blocking) {
               audio.waitFor();
            }
         }
      }

      if (!played) {
         // Error... - call onFinished manually
         // (this also updates play counters)
         if (onFinishedWrapper != null) {
            onFinishedWrapper.run();
         }
      }
   }

   public static boolean isPlaying() {
      synchronized (gLivePlayers) {
         return (gLivePlayers.size() > 0);
      }
   }

   private static void waitForPlayFinished() {
      synchronized (gLivePlayers) {
         while (gLivePlayers.size() > 0) {
            // Releases lock while waiting; when any removed
            // calls notifyAll.
            try {
               // XXX Had been found to hang here in one case where audio
               // playback hung in "write" call.
               gLivePlayers.wait();
            } catch (InterruptedException ie) {
               // Ignore
            }
         }
      }
   }

   // Similar to waitForPlayFinished but also makes sure there
   // isn't known queued playback ready to start.
   private static void waitForPlayAndQueuedFinished() {
      while (true) {
         waitForPlayFinished();
         synchronized (gLivePlayers) {
            if (mReadyPlayId == mNextPlayId) {
               break;
            }
         }
      }
   }

   private static void waitForPlayFinished(long id) {
      while (true) {
         waitForPlayFinished();
         // Use >= since play call that doesn't have to wait
         // could have been inserted and caused this to bump.
         // We have to wait for it but it doesn't have to
         // wait for us.
         synchronized (gLivePlayers) {
            if (mReadyPlayId >= id) {
               break;
            }
         }
      }
   }

   public static void stopAllPlayback() {
      stopAllPlayback(false);
   }

   private static void stopAllPlayback(boolean blocking) {
      synchronized (gLivePlayers) {
         for (SimplePlayerInterface audio: gLivePlayers) {
            // Passing false for non-blocking call, knowing that
            // playback should stop soon.
            audio.stopPlaying(false);
         }
      }

      if (blocking) {
         waitForPlayFinished();
      }
   }

   public static void playAudioResource(int resource, Language lang) {
      playAudioResource(resource, lang, null, false);
   }

   public static void playAudioResource(int resource, Language lang, Runnable onFinished, boolean blocking) {
      String filename = App.getApp().getAudioFilenameResource(resource);
      if ((filename == null) || (filename.length() == 0)) {
         return;
      }
      playAudioFile(filename, lang, onFinished, blocking, true);
   }

   public static boolean needsSequentialPlaybackForDifferentLanguages() {
      boolean playMonitor = Boolean.parseBoolean(App.getApp().getProps().getProperty("MonitorAudio", "true"));

      // Note that null at top means to use default device
      String out1 = App.getApp().getProps().getProperty("AudioPlaybackDevice", null);
      String out2 = App.getApp().getProps().getProperty("ia.AudioPlaybackDevice", null);
      if (playMonitor) {
         // Both EN and IA will get played through the monitor device so they shouldn't be allowed
         // to be played at same time and overlap
         return true;
      } else {
         // No playback monitoring
         if ((out1 == null) && (out2 == null)) {
            // Both are using default device
            return true;
         } else if ((out1 != null) && (out2 != null) && (out1.compareTo(out2) != 0)) {
            // Both set and are different so overlapping is allowed
            return false;
         } else {
            // One is set and one is using default so they *could* be the same;
            // don't risk playing outputs on top of each other.
            return true;
         }
      }
   }

   public static void addListener(PlaybackListener l) {
      gListeners.add(l);
   }

   private static final Logger logger = LoggerFactory.getLogger(Playback.class);
   private static ArrayList<PlaybackListener> gListeners = new ArrayList<PlaybackListener>();
   private static ArrayList<SimplePlayerInterface> gLivePlayers = new ArrayList<SimplePlayerInterface>();

   private static long mNextPlayId = 0;
   private static long mReadyPlayId = 0;

   /* Uncomment for audio debugging
   static {
      // Default is to log to stdout
      //DLog.setLogger(null);
      DLog.setLogger(new DLogInterface() {
         public void ll(String prefix, String comp, String msg) {
            System.out.println(prefix + comp + "::" + msg);
         }
         public void w(String comp, String msg) {
            ll("W/", comp, msg);
         }
         public void v(String comp, String msg) {
            ll("V/", comp, msg);
         }
         public void e(String comp, String msg) {
            ll("E/", comp, msg);
            throw new RuntimeException("Making RuntimeException for error");
         }
         public void i(String comp, String msg) {
            ll("I/", comp, msg);
         }
         public void d(String comp, String msg) {
            ll("D/", comp, msg);
         }
      });
   }
   */
}
