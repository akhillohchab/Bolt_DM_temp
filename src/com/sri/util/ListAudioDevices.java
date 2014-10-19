package com.sri.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class ListAudioDevices {
   public static void main(String[] args) {
      int rate = 16000;
      int bits = 16;
      int channels = 1;
      boolean isSigned = true;
      boolean isBigEndian = false;

      // This is the format we want to record in
      AudioFormat format = new AudioFormat(16000, bits, channels, isSigned, isBigEndian);
      DataLine.Info recFormatInfo = new DataLine.Info(TargetDataLine.class, format);
      DataLine.Info playFormatInfo = new DataLine.Info(SourceDataLine.class, format);

      System.out.println("Desired format: " + format);
      System.out.println();

      // NOTE:
      // Can find Supermic mixer by searching for "AK5370" as substring in the "name" of the Mixer.Info

      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
      System.out.println("Total mixers found: " + mixerInfo.length);
      System.out.println();

      System.out.println("RECORDING");
      for (int i = 0; i < mixerInfo.length; i++) {
         System.out.println();
         Mixer m = AudioSystem.getMixer(mixerInfo[i]);
         // Ignore unless supports recording appropriate format
         Line.Info[] recordingInfo = m.getTargetLineInfo(recFormatInfo);
         System.out.println(" mixerInfo[" + i + "]: " + mixerInfo[i].getName());
         if ((recordingInfo == null) || (recordingInfo.length == 0)) {
            System.out.println("  IGNORE -- recording at desired format not supported");
            continue;
         }
         System.out.println("  description: " + mixerInfo[i].getDescription());
         TargetDataLine line = null;
         try {
            // Note that this shouldn't fail because we've already filtered our recordingInfo
            // array to those that match recFormatInfo.
            line = (TargetDataLine) m.getLine(recFormatInfo);
            System.out.println("  YES - record - supports desired format: '" + mixerInfo[i].getName() + "'");
         } catch (IllegalArgumentException e) {
            System.out.println("  (IllegalArgumentException) NO - record - does not support desired format: " + recFormatInfo);
            System.out.println("  getting line: " + e);
         } catch (LineUnavailableException e) {
            System.out.println("  (LineUnavailableException) NO - record - does not support desired format: " + recFormatInfo);
            System.out.println("  getting line: " + e);
         } catch (SecurityException e) {
            System.out.println("  (SecurityException) NO - record - does not support desired format: " + recFormatInfo);
            System.out.println("  getting line: " + e);
         }
      }

      System.out.println();
      System.out.println("PLAYBACK");
      for (int i = 0; i < mixerInfo.length; i++) {
         Mixer m = AudioSystem.getMixer(mixerInfo[i]);
         // Ignore unless supports playing appropriate format
         Line.Info[] playingInfo = m.getSourceLineInfo(playFormatInfo);
         // More silent for playback - ignore printing if playback at desired format not supported
         if ((playingInfo == null) || (playingInfo.length == 0)) {
            //System.out.println("  IGNORE -- playing at desired format not supported");
            continue;
         }
         System.out.println();
         System.out.println(" mixerInfo[" + i + "]: " + mixerInfo[i].getName());
         System.out.println("  description: " + mixerInfo[i].getDescription());
         SourceDataLine line = null;
         try {
            // Note that this shouldn't fail because we've already filtered our playingInfo
            // array to those that match playFormatInfo.
            line = (SourceDataLine) m.getLine(playFormatInfo);
            System.out.println("  YES - play - supports desired format: '" + mixerInfo[i].getName() + "'");
         } catch (IllegalArgumentException e) {
            System.out.println("  (IllegalArgumentException) NO - play - does not support desired format: " + playFormatInfo);
            System.out.println("  getting line: " + e);
         } catch (LineUnavailableException e) {
            System.out.println("  (LineUnavailableException) NO - play - does not support desired format: " + playFormatInfo);
            System.out.println("  getting line: " + e);
         } catch (SecurityException e) {
            System.out.println("  (SecurityException) NO - play - does not support desired format: " + playFormatInfo);
            System.out.println("  getting line: " + e);
         }
      }

      // This will quickly get us to supported target lines, but no mixer info or name information to tell what card it goes with
      System.out.println();
      System.out.println("RECORDING SIMPLE");
      Line.Info[] recordingInfo = AudioSystem.getTargetLineInfo(recFormatInfo);
      System.out.println("Total lines supporting recording at desired format: " + ((recordingInfo != null)?recordingInfo.length:0));
      System.out.println();
      if (recordingInfo != null) {
         for (int i = 0; i < recordingInfo.length; i++) {
            System.out.println("recordingInfo[" + i + "]: " + recordingInfo[i]);
         }
      }

      System.out.println();
      System.out.println("PLAYBACK SIMPLE");
      Line.Info[] playingInfo = AudioSystem.getSourceLineInfo(playFormatInfo);
      System.out.println("Total lines supporting playback at desired format: " + ((playingInfo != null)?playingInfo.length:0));
      System.out.println();
      if (playingInfo != null) {
         for (int i = 0; i < playingInfo.length; i++) {
            System.out.println("playingInfo[" + i + "]: " + playingInfo[i]);
         }
      }
   }
}
