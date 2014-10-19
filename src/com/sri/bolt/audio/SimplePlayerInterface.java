package com.sri.bolt.audio;

public interface SimplePlayerInterface {
   // Play in background, optional onFinished callback when done
   boolean play(final Runnable onFinished);

   // Wait for current playback to finish, up to specified timeout or -1
   // for unlimited and return true if playback done.
   //boolean waitForIdle(int timeoutMillis);

   // Wait until finished
   void waitFor();

   // Stop any current playback.
   void stopPlaying(boolean blocking);
}
