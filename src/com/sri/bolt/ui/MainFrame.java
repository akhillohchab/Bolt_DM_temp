package com.sri.bolt.ui;

import com.sri.bolt.App;
import com.sri.bolt.RepeatingReleasedEventsFixer;
import com.sri.bolt.Util;
import com.sri.bolt.audio.Playback;
import com.sri.bolt.audio.PlaybackListener;
import com.sri.bolt.state.InteractionState;
import com.sri.bolt.state.TranslationState;
import com.sri.bolt.ui.DialogueBubblePane.Speaker;
import com.sri.interfaces.lang.Language;
import com.sri.jsound.JSoundAudioDeviceEnumerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.sri.bolt.workflow.Util.getAllWorkflowTasks;

/**
 * Small gui. Will display messages logged to the logger as well as have
 * controls for starting and stopping audio capture
 *
 * @author peter.blasco@sri.com
 */
public class MainFrame extends JFrame {
   private static final String APP_TITLE = "THUnderBOLT";

   private static final String NOT_PLAYING_OR_RECORDING_TEXT = "<html>&nbsp;</html>";
   private static final String IS_RECORDING_TEXT = "<html><font color=\"#FF0000\">Recording</font></html>";
   private static final String IS_PLAYING_TEXT = "<html><font color=\"#00FF00\">Playing</font></html>";

   private static final int STATUS_DISPLAY_TIMEOUT_MILLIS = 3000;
   private static final String STATUS_SPEAKER_CHANGED_TEXT = "<html><font color=\"#0000FF\">New speaker</font></html>";
   private static final String STATUS_NONE_TEXT = "<html>&nbsp;</html>";

   private static final String STATUS_RESET_FOR_NEW_TRIAL = "<html><font color=\"#0000FF\">Cleared state for new trial</font></html>";
   private static final String STATUS_NOTHING_TO_PLAY = "<html><font color=\"#0000FF\">Nothing to play</font></html>";

   // No change speaker button as of 6/11/2013
   private static final boolean HAVE_CHANGE_SPEAKER_BUTTON = false;

   private final JFileChooser chooser = new JFileChooser();

   public MainFrame(boolean evalMode) {
      super(APP_TITLE);

      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      System.setProperty("awt.useSystemAAFontSettings", "lcd");

      topButtonPanel = new JPanel();
      topButtonPanel.setLayout(new FlowLayout());
      bottomButtonPanel = new JPanel();
      bottomButtonPanel.setLayout(new FlowLayout());
      selectionButtonPanel = new JPanel();
      selectionButtonPanel.setLayout(new FlowLayout());
      JButton fileENButton = new JButton("Recognize English From File");
      JButton fileIAButton = new JButton("Recognize IA From File");
      JButton sessionENButton = new JButton("Recognize EN From SessionData");
      JButton sessionIAButton = new JButton("Recognize IA From SessionData");
      recordENButton = new JButton("Start EN Recording");
      recordIAButton = new JButton("Start IA Recording");
      stopButton = new JButton("Stop Recording");
      JButton startNewTrialButton = new JButton("Start new trial");
      final String PLAY_LAST_PREFIX = "";
      //final String PLAY_LAST_PREFIX = "Play Last ";
      JButton playButton = new JButton(PLAY_LAST_PREFIX + "Utterance");
      JButton playSystemButton = new JButton(PLAY_LAST_PREFIX + "System Command");
      JButton playTranslationButton = new JButton(PLAY_LAST_PREFIX + "Translation");
      //JButton abortButton = new JButton("Abort");
      JButton abortButton = null;
      JButton rollbackButton = new JButton("Rollback Last Turn");
      JButton changeSpeakerButton = null;
      if (HAVE_CHANGE_SPEAKER_BUTTON) {
         changeSpeakerButton = new JButton("Change Speaker");
      }
      final JCheckBox monitorAudioCheckBox = new JCheckBox("Monitor Audio");
      monitorAudioCheckBox.setSelected(Boolean.parseBoolean(App.getApp().getProps().getProperty("MonitorAudio", "true")));

      recordENButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            startAudioRecording(Language.ENGLISH);
         }
      });

      recordIAButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            startAudioRecording(Language.IRAQI_ARABIC);
         }
      });

      stopButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            stopAudioRecording();
         }
      });

      monitorAudioCheckBox.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
            boolean enabled = monitorAudioCheckBox.isSelected();
            // The audio properties get updated on disk so set there but copy
            // over to main properties for single source read access.
            App.getApp().getAudioProps().setProperty("MonitorAudio", Boolean.toString(enabled));
            App.getApp().saveAudioProps();
            App.getApp().copyOverAudioProps();
         }
      });

      playButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            InteractionState interaction = App.getApp().getTrial().getCurrentInteraction();
            File uttFile = null;
            if (interaction != null) {
                uttFile = interaction.getLastHumanUtterance();
            }
            if ((uttFile != null) && (uttFile.exists())) {
               logger.info("Playing last recorded user input: " + uttFile.getPath());
               Playback.playAudioFile(uttFile.getPath(), interaction.getLanguage());
            } else {
               logger.info("Playing last recorded user input; nothing to play");
               showTempStatus(STATUS_NOTHING_TO_PLAY);
            }
         }
      });

      /*
      abortButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            App.getApp().getWorkflowController().abort();

            promptForTrialId(true);
         }
      });
      */

      startNewTrialButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            promptForTrialId(true);
         }
      });

      fileENButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            recognizeFromFile(Language.ENGLISH);
         }
      });

      fileIAButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            recognizeFromFile(Language.IRAQI_ARABIC);
         }
      });

      sessionENButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            recognizeFromSessionData(Language.ENGLISH);
         }
      });

      sessionIAButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            recognizeFromSessionData(Language.IRAQI_ARABIC);
         }
      });

      playSystemButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            playLastSystemCommand();
         }
      });

      playTranslationButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            playLastTranslation();
         }
      });

      rollbackButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            App.getApp().getWorkflowController().rollbackLastTurn();
         }
      });

      if (HAVE_CHANGE_SPEAKER_BUTTON) {
         changeSpeakerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               App.getApp().getServiceController().changeSpeaker();
               showTempStatus(STATUS_SPEAKER_CHANGED_TEXT);
            }
         });
      }

      debugText = new JTextArea();
      debugText.setRows(20);
      debugText.setWrapStyleWord(true);
      debugText.setLineWrap(true);
      debugText.setFont(new Font("Monospace", Font.PLAIN, 14));
      debugText.setEditable(false);

      enChatWindow = new DialogueBubblePane(Speaker.ENGLISH);
      //chatWindow.setPreferredSize(new Dimension(400, 300));
      JScrollPane enChatScrollPane = new JScrollPane(enChatWindow, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      enChatWindow.setFocusable(false);

      iaChatWindow = new DialogueBubblePane(Speaker.ARABIC);
      //chatWindow.setPreferredSize(new Dimension(400, 300));
      JScrollPane iaChatScrollPane = new JScrollPane(iaChatWindow, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      iaChatWindow.setFocusable(false);

      selectionButtonPanel.add(monitorAudioCheckBox);
      topButtonPanel.add(startNewTrialButton);
      if (HAVE_CHANGE_SPEAKER_BUTTON) {
         topButtonPanel.add(changeSpeakerButton);
      }
      JPanel playButtonPanel = new JPanel();
      playButtonPanel.setLayout(new FlowLayout());
      JLabel playLastLabel = new JLabel("Play last: ");
      playButtonPanel.add(playLastLabel);
      playButtonPanel.add(playButton);
      playButtonPanel.add(playSystemButton);
      playButtonPanel.add(playTranslationButton);

      if (abortButton != null) {
         topButtonPanel.add(abortButton);
      }
      topButtonPanel.add(rollbackButton);

      try {
         logoPanel = new JPanel();
         logoPanel.setBackground(Color.WHITE);
         logoPanel.setLayout(new BorderLayout());

         //BufferedImage logo = ImageIO.read(new File("../resources/logo.gif"));
         //BufferedImage logo = ImageIO.read(new File("../resources/Logos.jpg"));
         BufferedImage logo = ImageIO.read(new File("../resources/left.gif"));
         ImageIcon icon = new ImageIcon(logo);
         JLabel logoLabel = new JLabel(icon);
         //logoLabel.setSize(181,128);
         logoPanel.add(logoLabel, BorderLayout.WEST);

         //JLabel titleLabel = new JLabel(APP_TITLE);
         // Set size
         //titleLabel.setFont(font);

         //BufferedImage title = ImageIO.read(new File("../resources/title2.gif"));
         BufferedImage title = ImageIO.read(new File("../resources/title3.gif"));
         ImageIcon titleIcon = new ImageIcon(title);
         JLabel titleLabel = new JLabel(titleIcon);
         logoPanel.add(titleLabel, BorderLayout.CENTER);

         BufferedImage logoRight = ImageIO.read(new File("../resources/right-v2.gif"));
         ImageIcon iconRight = new ImageIcon(logoRight);
         JLabel logoLabelRight = new JLabel(iconRight);
         logoPanel.add(logoLabelRight, BorderLayout.EAST);

         trialLabel = new JLabel("");
         trialLabel.setForeground(Color.BLACK);
         trialLabel.setHorizontalAlignment(JLabel.CENTER);
         logoPanel.add(trialLabel, BorderLayout.SOUTH);

         logoPanel.addKeyListener(new KeyEventListener());
         logoLabel.addKeyListener(new KeyEventListener());
         titleLabel.addKeyListener(new KeyEventListener());
         logoLabelRight.addKeyListener(new KeyEventListener());
         trialLabel.addKeyListener(new KeyEventListener());
      } catch (IOException e) {
         // error finding image
         logoPanel = null;
      }

      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
      buttonPanel.add(topButtonPanel);
      buttonPanel.add(playButtonPanel);
      if (!evalMode) {
         // Nothing here but some gap unless in eval mode
         buttonPanel.add(bottomButtonPanel);
      }
      buttonPanel.add(selectionButtonPanel);

      recordingOrPlayingPanel = new JPanel(new FlowLayout());
      // Will say "Recording" in red if recording
      recordingLabel = new JLabel(NOT_PLAYING_OR_RECORDING_TEXT);
      recordingLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
      playingLabel = new JLabel(NOT_PLAYING_OR_RECORDING_TEXT);
      playingLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
      recordingOrPlayingPanel.add(recordingLabel);
      recordingOrPlayingPanel.add(playingLabel);
      statusLabel = new JLabel(STATUS_NONE_TEXT);
      statusLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
      JPanel statusPanel = new JPanel(new BorderLayout());
      statusPanel.add(recordingOrPlayingPanel, BorderLayout.WEST);
      statusPanel.add(statusLabel, BorderLayout.EAST);

      JPanel buttonRecordingPanel = new JPanel(new BorderLayout());
      buttonRecordingPanel.add(statusPanel, BorderLayout.NORTH);
      buttonRecordingPanel.add(buttonPanel, BorderLayout.SOUTH);

      if (logoPanel != null) {
         add(logoPanel, BorderLayout.NORTH);
      }
      if (!evalMode) {
         JScrollPane debugScrollPane = new JScrollPane(debugText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
         JSplitPane chatSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, enChatScrollPane, iaChatScrollPane);
         textPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatSplitPane, debugScrollPane);
         textPane.setContinuousLayout(true);
         bottomButtonPanel.add(fileENButton);
         bottomButtonPanel.add(fileIAButton);
         bottomButtonPanel.add(sessionENButton);
         bottomButtonPanel.add(sessionIAButton);
         bottomButtonPanel.add(recordENButton);
         bottomButtonPanel.add(recordIAButton);
         bottomButtonPanel.add(stopButton);

         debugScrollPane.setMaximumSize(new Dimension(300, 2048));
         debugScrollPane.setPreferredSize(new Dimension(100, 300));
         enChatScrollPane.setPreferredSize(new Dimension(400, 300));
         iaChatScrollPane.setPreferredSize(new Dimension(400, 300));
         textPane.setPreferredSize(new Dimension(500, 300));
         // Doesn't enforce below as maximum width
         //textPane.setMaximumSize(new Dimension(800, 2048));
         add(textPane, BorderLayout.CENTER);

         debugScrollPane.addKeyListener(new KeyEventListener());
         chatSplitPane.addKeyListener(new KeyEventListener());
      } else {
         boolean experimental = false;
         if (experimental) {
            JScrollPane debugScrollPane = new JScrollPane(debugText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            textPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, enChatScrollPane, debugScrollPane);
            // Mainly, want to limit the height of debug window
            //scrollPane.setMaximumSize(new Dimension(100, 100));
            debugScrollPane.setPreferredSize(new Dimension(0, 0));
            textPane.setPreferredSize(new Dimension(400, 300));
            add(textPane, BorderLayout.CENTER);

            debugScrollPane.addKeyListener(new KeyEventListener());
         } else {
            JSplitPane chatSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, enChatScrollPane, iaChatScrollPane);
            enChatScrollPane.setPreferredSize(new Dimension(400, 300));
            iaChatScrollPane.setPreferredSize(new Dimension(400, 300));
            add(chatSplitPane, BorderLayout.CENTER);

            chatSplitPane.addKeyListener(new KeyEventListener());
         }
      }
      add(buttonRecordingPanel, BorderLayout.SOUTH);

      // TODO Find a better way to do this. Adding a KeyEventDispatcher to the
      // KeyboardFocusManager
      // overrides the RepeatingReleasedEventFixer. So right now just make sure
      // everything that can
      // grab focus will use this keyeventlistener
      this.addKeyListener(new KeyEventListener());
      topButtonPanel.addKeyListener(new KeyEventListener());
      bottomButtonPanel.addKeyListener(new KeyEventListener());
      selectionButtonPanel.addKeyListener(new KeyEventListener());
      fileENButton.addKeyListener(new KeyEventListener());
      fileIAButton.addKeyListener(new KeyEventListener());
      sessionENButton.addKeyListener(new KeyEventListener());
      sessionIAButton.addKeyListener(new KeyEventListener());
      recordENButton.addKeyListener(new KeyEventListener());
      recordIAButton.addKeyListener(new KeyEventListener());
      stopButton.addKeyListener(new KeyEventListener());
      startNewTrialButton.addKeyListener(new KeyEventListener());
      playButton.addKeyListener(new KeyEventListener());
      playSystemButton.addKeyListener(new KeyEventListener());
      playTranslationButton.addKeyListener(new KeyEventListener());
      if (abortButton != null) {
         abortButton.addKeyListener(new KeyEventListener());
      }
      rollbackButton.addKeyListener(new KeyEventListener());
      if (HAVE_CHANGE_SPEAKER_BUTTON) {
         changeSpeakerButton.addKeyListener(new KeyEventListener());
      }
      monitorAudioCheckBox.addKeyListener(new KeyEventListener());
      debugText.addKeyListener(new KeyEventListener());
      enChatWindow.addKeyListener(new KeyEventListener());
      enChatScrollPane.addKeyListener(new KeyEventListener());
      iaChatWindow.addKeyListener(new KeyEventListener());
      iaChatScrollPane.addKeyListener(new KeyEventListener());
      playButtonPanel.addKeyListener(new KeyEventListener());
      playLastLabel.addKeyListener(new KeyEventListener());
      buttonPanel.addKeyListener(new KeyEventListener());
      recordingOrPlayingPanel.addKeyListener(new KeyEventListener());
      recordingLabel.addKeyListener(new KeyEventListener());
      playingLabel.addKeyListener(new KeyEventListener());
      statusLabel.addKeyListener(new KeyEventListener());
      statusPanel.addKeyListener(new KeyEventListener());
      buttonRecordingPanel.addKeyListener(new KeyEventListener());

      if (textPane != null) {
         textPane.addKeyListener(new KeyEventListener());
      }

      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            CleanerUpper cleanerUpper = new CleanerUpper();
            cleanerUpper.execute();
            closingDialog.setVisible(true);
         }
      });

      loadingDlg = new JDialog(null, "INITIALIZING", ModalityType.DOCUMENT_MODAL);
      JPanel dlgText = new JPanel();
      dlgText.add(new JLabel("Initializing, please wait..."), BorderLayout.CENTER);
      loadingDlg.setUndecorated(true);
      loadingDlg.setContentPane(dlgText);
      loadingDlg.pack();
      loadingDlg.setLocationRelativeTo(null);


      closingDialog = new JDialog(null, "CLOSING", ModalityType.DOCUMENT_MODAL);
      JPanel closeDlgText = new JPanel();
      closeDlgText.add(new JLabel("Shutting down services, please wait..."), BorderLayout.CENTER);
      closingDialog.setUndecorated(true);
      closingDialog.setContentPane(closeDlgText);
      closingDialog.pack();
      closingDialog.setLocationRelativeTo(null);

      JMenuBar menuBar = new JMenuBar();
      menuBar.setBackground(Color.LIGHT_GRAY);
      JMenu settingsMenu = new JMenu("Settings");
      menuBar.add(settingsMenu);
      JMenu helpMenu = new JMenu("Help");
      menuBar.add(helpMenu);

      JMenuItem audioProperties = new JMenuItem("Audio properties");
      settingsMenu.add(audioProperties);

      JMenuItem aboutMenuItem = new JMenuItem("About");
      helpMenu.add(aboutMenuItem);

      audioProperties.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            JSoundAudioDeviceEnumerator lister = new JSoundAudioDeviceEnumerator();
            String[] playChoices = lister.getPlaybackDeviceNames(Util.SAMPLE_RATE);
            String[] recordChoices = lister.getCaptureDeviceNames(Util.SAMPLE_RATE);

            String[] noChoices = {"NONE"};
            if (playChoices == null) {
                playChoices = noChoices;
            }
            if (recordChoices == null) {
                recordChoices = noChoices;
            }

            int[] englishIndices = getActiveDevices(true, recordChoices, playChoices);
            int[] iaIndices = getActiveDevices(false, recordChoices, playChoices);
            int monitorIndex = getMonitorDevice(playChoices);

            final DualAudioSelectionPanel panel = new DualAudioSelectionPanel(
                    "English", recordChoices, englishIndices[0], playChoices, englishIndices[1],
                    "Iraqi", recordChoices, iaIndices[0], playChoices, iaIndices[1],
                    "Monitor", null, 0, playChoices, monitorIndex);

            final JDialog dialog = new JDialog(MainFrame.this, "Audio selection popup", true);
            dialog.add(panel);

            dialog.addComponentListener(new ComponentListener() {

                @Override
                public void componentResized(ComponentEvent e) {
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                }

                @Override
                public void componentShown(ComponentEvent e) {
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    saveAudioChoices(true, panel.getRecordSelection1(), panel.getPlaySelection1());
                    saveAudioChoices(false, panel.getRecordSelection2(), panel.getPlaySelection2());
                    saveAudioMonitorChoices(panel.getPlayMonitorSelection());

                    // Make just the variable audio properties persist on disk *and*
                    // copy the audio properties into our regular properties.
                    App.getApp().saveAudioProps();
                    App.getApp().copyOverAudioProps();
                }

            });

            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.show();

        }

      });

      aboutMenuItem.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            String version = App.getFullVersionString();
            String text = APP_TITLE + "\n\nVersion: " + version;

            JOptionPane.showMessageDialog(MainFrame.this, text, "About", JOptionPane.PLAIN_MESSAGE);
         }
      });

      setJMenuBar(menuBar);

      // Key listeners for menu
      menuBar.addKeyListener(new KeyEventListener());
      settingsMenu.addKeyListener(new KeyEventListener());
      audioProperties.addKeyListener(new KeyEventListener());

      Playback.addListener(new PlaybackListener() {

         @Override
         public void playStarted() {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  playingLabel.setText(IS_PLAYING_TEXT);
               }
            });
         }

         @Override
         public void playFinished() {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  // It's possible that some other player is still playing
                  // (playing to both users at once).
                  if (!Playback.isPlaying()) {
                     playingLabel.setText(NOT_PLAYING_OR_RECORDING_TEXT);
                  }
               }
            });
         }

      });

      pack();
      this.setLocationRelativeTo(null);
   }

   private void playLastSystemCommand() {
      // Was done from GUI so will apply to either user
      playLastSystemCommand(Language.UNKNOWN);
   }

   private void playLastSystemCommand(Language whichUser) {
      boolean attemptCommand = false;
      InteractionState state = App.getApp().getTrial().getCurrentInteraction();
      if (state != null) {
         switch (whichUser) {
         case ENGLISH:
             attemptCommand = state.getLanguage() == Language.ENGLISH;
             break;
         case IRAQI_ARABIC:
             attemptCommand = state.getLanguage() == Language.IRAQI_ARABIC;
             break;
         case UNKNOWN:
             attemptCommand = true;
             break;
         default:
         }
         if (!attemptCommand) {
            logger.info("Ignoring other user's request to play last command");
         } else {
            // Stop any current playback
            stopPlayback();
            File audioFile = state.getLastSystemCommand();
            if (audioFile != null && !audioFile.getName().equals("")) {
               Playback.playAudioFile(audioFile.getPath(), state.getLanguage());
               logger.info("Playing last system command");
            } else {
               logger.info("No audio file for system command");
            }
         }
      } else {
            // Call above returned false, indicating nothing to play
            logger.info("Playing last system command; nothing to play");
            showTempStatus(STATUS_NOTHING_TO_PLAY);
      }

   }

   private void playLastTranslation() {
      // Was done from GUI so will apply to either user
      playLastTranslation(Language.UNKNOWN);
   }

   private void playLastTranslation(Language whichUser) {
      boolean attemptCommand = false;
      TranslationState state = App.getApp().getTrial().getLastTranslation();
      if (state != null) {
         //play if unknown user or if the language of the speaker is the opposite of the language of the translationstate
         //The language of the translation state will be the language of the original input, not the final translation
         //So the arabic user wants to hear the translation of the english input
         switch (whichUser) {
         case ENGLISH:
            attemptCommand = state.getLanguage() == Language.IRAQI_ARABIC;
            break;
         case IRAQI_ARABIC:
            attemptCommand = state.getLanguage() == Language.ENGLISH;
            break;
         case UNKNOWN:
            attemptCommand = true;
            break;
         default:
         }
         if (!attemptCommand) {
            logger.info("Ignoring other user's request to play last translation");
         } else {
            // Stop any current playback
            stopPlayback();
            logger.info("Playing last translation");
            File audioFile = state.getTtsAudioFile();
            if (audioFile != null && !audioFile.getName().equals("")) {
               Playback.playAudioFile(audioFile.getPath(), state.getLanguage() == Language.ENGLISH ? Language.IRAQI_ARABIC : Language.ENGLISH);
            } else {
               logger.info("No audio file for translation");
            }
         }
      } else {
         // Call above returned false, indicating nothing to play
         logger.info("Playing last translation; nothing to play");
         showTempStatus(STATUS_NOTHING_TO_PLAY);
      }
   }

   private int[] getActiveDevices(boolean isEnglish, String[] recordChoices, String[] playChoices) {
       String prefix = "";
       if (!isEnglish) {
           prefix = "ia.";
       }
       String curRecord = App.getApp().getProps().getProperty(prefix + "AudioCaptureDevice", null);
       String curPlay = App.getApp().getProps().getProperty(prefix + "AudioPlaybackDevice", null);
       int recIdx = 0;
       int playIdx = 0;
       if (curRecord != null) {
           for (int i = 0; i < recordChoices.length; i++) {
               if (recordChoices[i].contains(curRecord)) {
                   recIdx = i;
                   break;
               }
           }
       }
       if (curPlay != null) {
           for (int i = 0; i < playChoices.length; i++) {
               if (playChoices[i].contains(curPlay)) {
                   playIdx = i;
                   break;
               }
           }
       }

       int[] retval = new int[2];
       retval[0] = recIdx;
       retval[1] = playIdx;

       return retval;
   }

   private int getMonitorDevice(String[] playChoices) {
       String prefix = "monitor.";
       String curPlay = App.getApp().getProps().getProperty(prefix + "AudioPlaybackDevice", null);
       int playIdx = 0;
       if (curPlay != null) {
           for (int i = 0; i < playChoices.length; i++) {
               if (playChoices[i].contains(curPlay)) {
                   playIdx = i;
                   break;
               }
           }
       }

       return playIdx;
   }

   private void saveAudioChoices(boolean isEnglish, String recordDevice, String playDevice) {
       // Be a little sneaky and grab only to first "[" in case actual
       // device number changes.
       int idx = playDevice.indexOf('[');
       if (idx >= 0) {
           playDevice = playDevice.substring(0, idx + 1);
       }
       idx = recordDevice.indexOf('[');
       if (idx >= 0) {
           recordDevice = recordDevice.substring(0, idx + 1);
       }

       String prefix = "";
       if (!isEnglish) {
           prefix = "ia.";
       }

       App.getApp().getAudioProps().setProperty(prefix + "AudioCaptureDevice", recordDevice);
       App.getApp().getAudioProps().setProperty(prefix + "AudioPlaybackDevice", playDevice);
   }

   private void saveAudioMonitorChoices(String playDevice) {
       // Be a little sneaky and grab only to first "[" in case actual
       // device number changes.
       int idx = playDevice.indexOf('[');
       if (idx >= 0) {
           playDevice = playDevice.substring(0, idx + 1);
       }

       String prefix = "monitor.";

       App.getApp().getAudioProps().setProperty(prefix + "AudioPlaybackDevice", playDevice);
   }

   public boolean promptForTrialId(boolean isReinit) {
      String trialId = Util.getUniqueTrialId();
      if (App.getApp().getProps().getProperty("PromptForTrialId", "true").equalsIgnoreCase("true")) {
         NewSessionDialog dialog = new NewSessionDialog(trialId);
         dialog.setModal(true);
         if (dialog.showDialog()) {
            trialId = dialog.getTrialId();
            App.getApp().setEvalType(dialog.getEvalType());
         } else if (!isReinit) {
            System.exit(0);
         }
      }

      if (trialId == null && !isReinit) {
         System.exit(0);
      } else if (trialId != null) {
         trialId = trialId.toUpperCase();
         File runDir = new File(App.getApp().getProps().getProperty("OutputDir") + trialId);
         if (runDir.exists()) {
            if (JOptionPane.showConfirmDialog(this, "This trial exists.  Delete files and start new trial?",
                    "Trial exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
               try {
                  Util.deleteRecursive(runDir);
               } catch (FileNotFoundException e) {
                  logger.error("unable to delete dir: " + e, e);
                  System.exit(-1);
               }
               doInit(trialId, isReinit);
            } else {
               promptForTrialId(isReinit);
            }
         } else {
            doInit(trialId, isReinit);
         }
      }

      return true;
   }

   private void doInit(String trialId, boolean isReinit) {
      if (trialLabel != null) {
         // Puts trial id at top of log section
         trialLabel.setText("Trial: " + trialId);
      }
      if (!isReinit) {
         Initializer init = new Initializer(trialId, this);
         init.execute();
         loadingDlg.setVisible(true);
      } else {
         App.getApp().setTrialId(trialId, true, false);
         enChatWindow.clear();
         iaChatWindow.clear();
         debugText.setText("");
         recording = false;
         showTempStatus(STATUS_RESET_FOR_NEW_TRIAL);
         this.requestFocus();
      }
   }

   public void addUserMessage(final String text, final Date time, final Language language) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            final DialogueBubblePane window = language == Language.ENGLISH ? enChatWindow : iaChatWindow;
            window.addLine(text, language == Language.ENGLISH ? Speaker.ENGLISH : Speaker.ARABIC);
         }
      });
   }

   public void addSystemMessage(final String text, final Date time, final Language language) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            final DialogueBubblePane window = language == Language.ENGLISH ? enChatWindow : iaChatWindow;
            window.addLine(text, Speaker.SYSTEM);
         }
      });
   }

   public void addTranslationMessage(final String text, final Date time, final Language language) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            final DialogueBubblePane window = language == Language.ENGLISH ? enChatWindow : iaChatWindow;
            window.addLine(text, language == Language.ENGLISH ? Speaker.ARABIC : Speaker.ENGLISH);
         }
      });
   }

   public void addErrorMessage(final String text, final Date time) {
      if (App.getApp().getProps().getProperty("EvalMode", "").equals("false")) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               showTempStatus(text);
            }
         });
      }
   }

   public void addDebugMessage(final String text, final Date time) {
      debugText.append(logFormat.format(time) + ": " + text + "\n");
   }

   private class KeyEventListener implements KeyListener {
      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyPressed(KeyEvent e) {
         if (keyPressed == -1) {
            int code = e.getKeyCode();
            // Set to true if it's a press we care about and
            // the default in the case statement below unsets this.
            boolean registeredPress = true;
            switch (code) {
            case KeyEvent.VK_F6:
                // English user Play Last System command
                playLastSystemCommand(Language.ENGLISH);
                break;
            case KeyEvent.VK_F7:
                // English user Play Last Translation command
                playLastTranslation(Language.ENGLISH);
                break;
            case KeyEvent.VK_F8:
                // IA user Play Last System command
                playLastSystemCommand(Language.IRAQI_ARABIC);
                break;
            case KeyEvent.VK_F9:
                // IA user Play Last Translation command
                playLastTranslation(Language.IRAQI_ARABIC);
                break;
            case KeyEvent.VK_1:
                // Device Aaron is testing (English)
            case KeyEvent.VK_F11:
               // Supermic (English)
            case KeyEvent.VK_PAGE_UP:
               // This is for Logitech R400 (English)
               if (startAudioRecording(Language.ENGLISH)) {
                  isGroup1 = true;
               }
               break;
            case KeyEvent.VK_2:
                // Device Aaron is testing (IA)
            case KeyEvent.VK_F12:
               // Supermic (IA)
            case KeyEvent.VK_PAGE_DOWN:
               // This is for Logitech R400 (IA)
               if (startAudioRecording(Language.IRAQI_ARABIC)) {
                  isGroup1 = false;
               }
               break;
            default:
               registeredPress = false;
               break;
            }
            // Don't consume unless it's a press we process
            if (registeredPress) {
                keyPressed = code;
                e.consume();
            }
         }
      }

      @Override
      public void keyReleased(KeyEvent e) {
         if (keyPressed == -1) {
            // Nothing to do if no key that we're registered for
            // is currently being held.
            return;
         } else if (e.getKeyCode() != keyPressed) {
            // We have pressed/released a key after initially held key
            return;
         }

         // If get to here, the primary key we were holding is released
         keyPressed = -1;
         e.consume();

         int code = e.getKeyCode();
         // See if key release corresponds to an htt up - aka release of key
         // for current language.
         boolean doStop = false;
         switch (code) {
         // Fall through for input language group1
         case KeyEvent.VK_1:
         case KeyEvent.VK_F11:
         case KeyEvent.VK_PAGE_UP:
            // Only attempt to process if current press is for group1
            if (isGroup1) {
               doStop = true;
            }
            break;
         // Fall through for input language group2
         case KeyEvent.VK_2:
         case KeyEvent.VK_F12:
         case KeyEvent.VK_PAGE_DOWN:
            // Only attempt to process if current press is for non-group1
            if (!isGroup1) {
               doStop = true;
            }
            break;
         }
         if (doStop) {
            stopAudioRecording();
         }
      }

      // Set true while doing HTT (hold-to-talk).
      // Note that holding the button will send many presses.
      private int keyPressed = -1;

      // Upon key press, track for which release to watch for (and
      // ignore release of other language).
      private boolean isGroup1 = true;
   }

   private void stopPlayback() {
      Playback.stopAllPlayback();
   }

   private boolean startAudioRecording(Language lang) {
      // Always called in GUI thread
      boolean retval = false;
      if (!recording) {
         // Stop any current playback
         stopPlayback();

         retval = App.getApp().getASRController().startASR(lang);
         recording = retval;
         if (retval) {
            recordingLabel.setText(IS_RECORDING_TEXT);
         }
      }

      return retval;
   }

   private boolean stopAudioRecording() {
      // Always called in GUI thread
      boolean retval = false;
      if (recording) {
         App.getApp().getASRController().stopASR();
         recording = false;
         retval = true;
         recordingLabel.setText(NOT_PLAYING_OR_RECORDING_TEXT);
      }

      return retval;
   }

   private void recognizeFromFile(Language lang) {
      int returnVal = chooser.showOpenDialog(MainFrame.this);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File chosenFile = chooser.getSelectedFile();
         App.getApp().getASRController().startASR(chosenFile, lang, false);
      }
   }

   private void recognizeFromSessionData(Language lang) {
      CustomWorkflowDialog dialog = new CustomWorkflowDialog(getAllWorkflowTasks());
      dialog.setVisible(true);
      if (dialog.isSuccess()) {
         App.getApp()
                 .getWorkflowController()
                 .startCustomWorkflow(dialog.getChosenTaskType(), dialog.getChosenSessionData(),
                         dialog.getChosenAudioFile(), lang, false);
      }

   }

   // Call on UI thread to temporarily set the text of the status
   // message which will return to empty after preset delay.
   // NOTE: If you are updating the status text quickly, a previous
   // clear status call would clear the updated status sooner than
   // expected. That is, not meant to be called again until after
   // the clear timeout expires.
   private void showTempStatus(final String msg) {
      statusLabel.setText(msg);
      Thread t = new Thread(new Runnable() {

         @Override
         public void run() {
            // Wait a bit
            try {
               Thread.sleep(STATUS_DISPLAY_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
               // Ignore
            }
            // Restore status label to empty
            statusLabel.setText(STATUS_NONE_TEXT);
         }
      });
      t.start();
   }

   private class CleanerUpper extends SwingWorker<Object, Object> {
      @Override
      protected Object doInBackground() throws Exception {
         App.getApp().cleanup();
         return null;
      }

      @Override
      protected void done() {
         closingDialog.setVisible(false);
         System.exit(0);
      }
   }

   private class Initializer extends SwingWorker<Object, Object> {
      private Initializer(String trialId, JFrame parent) {
         this.trialId = trialId;
         this.parent = parent;
      }

      @Override
      protected Object doInBackground() {
         App.getApp().setTrialId(trialId, false, false);
         return null;
      }

      @Override
      protected void done() {
         loadingDlg.setVisible(false);
         new RepeatingReleasedEventsFixer().install();
         parent.setVisible(true);
      }

      private JFrame parent;
      private String trialId;
   }

   private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);
   private JButton recordENButton;
   private JButton recordIAButton;
   private JButton stopButton;
   private JPanel topButtonPanel;
   private JPanel bottomButtonPanel;
   private JPanel selectionButtonPanel;
   private JPanel logoPanel;
   private JLabel trialLabel;
   private JSplitPane textPane;
   private JTextArea debugText;
   private JPanel recordingOrPlayingPanel;
   private JLabel recordingLabel;
   private JLabel playingLabel;
   private JLabel statusLabel;
   private DialogueBubblePane enChatWindow;
   private DialogueBubblePane iaChatWindow;
   private boolean recording;
   private static final String trialID_REG_EX = "[a-zA-Z][a-zA-Z]\\d\\d\\d-\\d\\d";
   private JDialog loadingDlg;
   private JDialog closingDialog;
   private SimpleDateFormat logFormat = new SimpleDateFormat("HH:mm:ss");
}
