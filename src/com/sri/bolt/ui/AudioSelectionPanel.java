package com.sri.bolt.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.sri.audio.AudioReader;
import com.sri.audio.SJAudio;
import com.sri.bolt.App;
import com.sri.bolt.audio.AudioLevelUtil;
import com.sri.bolt.audio.AudioLevelValues;
import com.sri.interfaces.audio.AudioProperties;
import com.sri.jsound.JSoundAudioCaptureFactory;
import com.sri.jsound.JSoundAudioPlayerFactory;
import com.sri.interfaces.audio.VUMeterListener;

public class AudioSelectionPanel extends JPanel {
   private LevelChangeListener mListener;

   private boolean mShowRecord;
   private boolean mShowPlay;

   private JComboBox mComboRecord;
   private JComboBox mComboPlay;
   private JButton mTestRecord;
   private JButton mTestPlay;
   private JProgressBar mRecLevel;
   private JProgressBar mPlayLevel;
   private JSlider mRecSet;
   private JSlider mPlaySet;

   private String mPlaySelection;
   private String mRecordSelection;

   private String mSettingsPrefix = "";

   private static final int HGAP_OUTER = 5;
   private static final int VGAP_OUTER = 0;

   private static final int HGAP = 0;
   private static final int VGAP = 5;

   public AudioSelectionPanel(String prefix, String[] recordChoices, int recIdx, String[] playChoices, int playIdx) {
      super(((recordChoices != null) && (playChoices != null))?new GridLayout(0, 2, HGAP_OUTER, VGAP_OUTER):new GridLayout(0, 1, HGAP_OUTER, VGAP_OUTER));

      // This is a hack in case couldn't find any devices.
      // Or, check that recIdx and playIdx not out-of-bounds.
      String[] noChoices = {"None"};

      mShowRecord = true;
      mShowPlay = true;

      JPanel recPanel = null;
      JPanel playPanel = null;

      if (recordChoices == null) {
         mShowRecord = false;
      } else {
         recPanel = new JPanel(new GridLayout(0, 1, HGAP, VGAP));
         this.add(recPanel);
      }

      if (playChoices == null) {
         mShowPlay = false;
      } else {
         playPanel = new JPanel(new GridLayout(0, 1, HGAP, VGAP));
         this.add(playPanel);
      }

      if ((recordChoices == null) || (recordChoices.length == 0)) {
         recordChoices = noChoices;
      }

      if ((playChoices == null) || (playChoices.length == 0)) {
         playChoices = noChoices;
      }

      mRecordSelection = recordChoices[recIdx];
      mPlaySelection = playChoices[playIdx];

      if (prefix.startsWith("Iraqi")) {
         mSettingsPrefix = "ia.";
      } else if (prefix.startsWith("Monitor")) {
         mSettingsPrefix = "monitor.";
      }

      JLabel recordLabel = new JLabel(prefix + " Recording");
      JLabel playLabel = new JLabel(prefix + " Playback");

      mComboRecord = new JComboBox(recordChoices);
      mComboPlay = new JComboBox(playChoices);

      mComboRecord.setSelectedIndex(recIdx);
      mComboPlay.setSelectedIndex(playIdx);

      mTestRecord = new JButton("Test");
      mTestPlay = new JButton("Test");

      mRecLevel = new JProgressBar(0, 100);
      mPlayLevel = new JProgressBar(0, 100);

      mRecSet = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
      mPlaySet = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);

      if (mShowRecord) {
         recPanel.add(recordLabel);
      }
      if (mShowPlay) {
         playPanel.add(playLabel);
      }
      if (mShowRecord) {
         recPanel.add(mComboRecord);
      }
      if (mShowPlay) {
         playPanel.add(mComboPlay);
      }
      if (mShowRecord) {
         recPanel.add(mTestRecord);
      }
      if (mShowPlay) {
         playPanel.add(mTestPlay);
      }
      if (mShowRecord) {
         mRecLevel.setString("Activity Meter");
         mRecLevel.setStringPainted(true);
         recPanel.add(mRecLevel);
      }
      if (mShowPlay) {
         mPlayLevel.setString("Activity Meter");
         mPlayLevel.setStringPainted(true);
         playPanel.add(mPlayLevel);
      }
      if (mShowRecord) {
         recPanel.add(new JLabel("Set Mic Level"));
      }
      if (mShowPlay) {
         playPanel.add(new JLabel("Set Play Level"));
      }
      if (mShowRecord) {
         recPanel.add(mRecSet);
         // Should only happen upon release
         mRecSet.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               if (mRecSet.getValueIsAdjusting()) {
                  // Wait until user releases to process event; else
                  // update many many times.
                  return;
               }
               int level = mRecSet.getValue();
               AudioLevelUtil.setMicLevel(getRecordSelection(), level);
               if (mListener != null) {
                  mListener.checkLevels();
               }
            }
         });
      }
      if (mShowPlay) {
         playPanel.add(mPlaySet);
         // Should only happen upon release
         mPlaySet.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               if (mPlaySet.getValueIsAdjusting()) {
                  // Wait until user releases to process event; else
                  // update many many times.
                  return;
               }
               int level = mPlaySet.getValue();
               AudioLevelUtil.setPlayLevel(getPlaySelection(), level);
               if (mListener != null) {
                  mListener.checkLevels();
               }
            }
         });
      }

      // If not showing, could instead use setVisible() of false
      // but should be best to just not add.

      mComboRecord.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            Object newItem = mComboRecord.getSelectedItem();
            if (newItem != null) {
               mRecordSelection = newItem.toString();
               App.getApp().getAudioProps().setProperty(mSettingsPrefix + "AudioCaptureDevice", mRecordSelection);
               App.getApp().copyOverAudioProps();
            }
            updateLevels();
         }

      });

      mComboPlay.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            Object newItem = mComboPlay.getSelectedItem();
            if (newItem != null) {
               mPlaySelection = newItem.toString();

               App.getApp().getAudioProps().setProperty(mSettingsPrefix + "AudioPlaybackDevice", mPlaySelection);
               App.getApp().copyOverAudioProps();

               // Play a test sound; no need to call updateLevels() since doTest() does that
               doTest(false, null);
            }
         }

      });

      mTestRecord.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            doTest(true, null);
         }

      });

      mTestPlay.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            doTest(false, null);
         }

      });

      updateLevels();
   }

   public String getPlaySelection() {
      return mPlaySelection;
   }

   public String getRecordSelection() {
      return mRecordSelection;
   }

   public void setListener(LevelChangeListener l) {
      mListener = l;
   }

   // Set current values for record and play
   public void updateLevels() {
      if (mShowRecord) {
         AudioLevelValues mval = AudioLevelUtil.getMicValues(getRecordSelection());
         if (mval != null) {
            mRecSet.setMinimum(mval.LMIN);
            mRecSet.setMaximum(mval.LMAX);
            mRecSet.setValue(mval.LVALUE);
            mRecSet.setEnabled(true);
         } else {
            mRecSet.setEnabled(false);
         }
      }

      if (mShowPlay) {
         AudioLevelValues pval = AudioLevelUtil.getPlayValues(getPlaySelection());
         if (pval != null) {
            mPlaySet.setMinimum(pval.LMIN);
            mPlaySet.setMaximum(pval.LMAX);
            mPlaySet.setValue(pval.LVALUE);
            mPlaySet.setEnabled(true);
         } else {
            mPlaySet.setEnabled(false);
         }
      }
   }

   // Expected range for "in" is 0-255
   private static int scaleVUMeter(double in, int min, int max) {
       int range = max - min + 1;

       int val = min + ((int)(in * range / 255.0));
       if (val < min) {
          val = min;
       } else if (val > max) {
          val = max;
       }

       return val;
   }

   private void doTest(boolean forRecord, short[] playSamples) {
      // Take this opportunity to make sure levels are correct
      if (mListener != null) {
         // Update everything if there's a listener
         mListener.checkLevels();
      } else {
         updateLevels();
      }

      final int numChannels = 1;
      AudioProperties p = new AudioProperties(com.sri.bolt.Util.SAMPLE_RATE, numChannels);
      // Can specify output device - use null since don't have to specify
      p.mixerPlaybackName = App.getApp().getProps().getProperty(mSettingsPrefix + "AudioPlaybackDevice", null);
      p.mixerCaptureName = App.getApp().getProps().getProperty(mSettingsPrefix + "AudioCaptureDevice", null);

      final SJAudio audio = new SJAudio(p, new JSoundAudioCaptureFactory(), new JSoundAudioPlayerFactory());

      if (forRecord) {
         // We are in the GUI thread
         mTestRecord.setText("Recording 5");

         audio.setRecordVUMeterListener(new VUMeterListener() {

            @Override
            public void energyLevel(final double[] channelEnergy, long sampleOffset, int numSamples) {
               SwingUtilities.invokeLater(new Runnable() {

                  @Override
                  public void run() {
                     int val = scaleVUMeter(channelEnergy[0], 0, 100);
                     mRecLevel.setValue(val);
                  }

               });
            }

         });
         audio.startRecording();

         // Stop in 5 seconds
         Runnable r = new Runnable() {

            @Override
            public void run() {
               for (int i = 0; i < 5; i++) {
                  try {
                     Thread.sleep(1000);
                  } catch (InterruptedException e) {
                  }
                  final int r = 4 - i;
                  SwingUtilities.invokeLater(new Runnable() {

                     @Override
                     public void run() {
                        mTestRecord.setText("Recording " + (r));
                     }

                  });
               }
               audio.stopRecording(true);
               SwingUtilities.invokeLater(new Runnable() {

                  @Override
                  public void run() {
                     mTestRecord.setText("Test");
                     mRecLevel.setValue(0);

                     // Now test playback
                     short[] samples = audio.getData();
                     doTest(false, samples);
                  }

               });
            }

         };
         // We record for 5 seconds.
         new Thread(r).start();
      } else {
         // Test playback

         // We are in the GUI thread
         mTestPlay.setText("Playing");
         mPlayLevel.setValue(50);

         audio.setPlayVUMeterListener(new VUMeterListener() {

             @Override
             public void energyLevel(final double[] channelEnergy, long sampleOffset, int numSamples) {
                //App.getLog4jLogger().info("energy " + channelEnergy[0] + " offset " + sampleOffset + " numSamples " + numSamples);
                SwingUtilities.invokeLater(new Runnable() {

                   @Override
                   public void run() {
                      int val = scaleVUMeter(channelEnergy[0], 0, 100);
                      mPlayLevel.setValue(val);
                   }

                });
             }

          });

         // Use a fallback if specified samples was null
         if (playSamples == null) {
             String filename = App.getApp().getAudioFilenameResource(App.AUDIO_RESOURCE_TEST_ENGLISH_PLAYBACK);
             playSamples = AudioReader.loadFileNE(new File(filename));
             // playSamples could still be null if filename couldn't be loaded; but that's
             // at least partially handled by not playing anything.
         }

         final Runnable actualWork = new Runnable() {

            @Override
            public void run() {
               mTestPlay.setText("Test");
               mPlayLevel.setValue(0);
            }

         };
         final Runnable onFinished = new Runnable() {

            @Override
            public void run() {
               SwingUtilities.invokeLater(actualWork);
            }

         };
         boolean played = false;
         if (playSamples != null) {
            played = audio.play(playSamples, onFinished);
         }
         if (!played) {
            // Didn't play so didn't actually call the callback
            actualWork.run();
         }
      }
   }
}
