package com.sri.bolt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.sri.bolt.listener.FileSystemLoggingListener;
import com.sri.bolt.logging.LogAppender;
import com.sri.bolt.audio.ASRController;
import com.sri.bolt.service.ServiceController;
import com.sri.bolt.state.ASRState;
import com.sri.bolt.state.TrialState;
import com.sri.bolt.workflow.WorkflowController;
import com.sri.bolt.workflow.task.WorkflowTaskType;
import com.sri.bolt.ui.MainFrame;
import com.sri.interfaces.lang.Language;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.log4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Singleton class that holds the main components of the controller.
 *
 * @author peter.blasco@sri.com
 */
public class App {
   public static final int AUDIO_RESOURCE_PROCESSING = 0;
   public static final int AUDIO_RESOURCE_ABORT = 1;
   public static final int AUDIO_RESOURCE_ABORTED = 2;
   public static final int AUDIO_RESOURCE_TRANSLATION_COMPLETE = 3;
   public static final int AUDIO_RESOURCE_TEST_ENGLISH_PLAYBACK = 4;
   public static final int AUDIO_RESOURCE_PTT_PRESSED = 5;

   public static App getApp() {
      return AppHolder.INSTANCE;
   }

   public String getPlayerCommand() {
      return getApp().playerCommand;
   }

   public String getAudioFilenameResource(int resid) {
      String retval = null;
      switch (resid) {
         case AUDIO_RESOURCE_PROCESSING:
            retval = properties.getProperty("ProcessingAudio", null);
            break;
         case AUDIO_RESOURCE_ABORT:
            retval = properties.getProperty("AbortAudio", null);
            break;
         case AUDIO_RESOURCE_ABORTED:
            retval = properties.getProperty("AbortedAudio", null);
            break;
         case AUDIO_RESOURCE_TRANSLATION_COMPLETE:
            retval = properties.getProperty("AudioTranslationComplete", null);
            break;
         case AUDIO_RESOURCE_TEST_ENGLISH_PLAYBACK:
            retval = properties.getProperty("AudioTestEnglishOutput", "../../../res/test_english_output.wav");
            break;
         case AUDIO_RESOURCE_PTT_PRESSED:
            retval = properties.getProperty("PTTPressed", null);
            break;
      }
      return retval;
   }

   public ServiceController getServiceController() {
      return serviceController;
   }

   public WorkflowController getWorkflowController() {
      return workflowController;
   }

   public ASRController getASRController() {
      return asrController;
   }

   public MainFrame getMainFrame() {
      return mainFrame;
   }

   public File getOutputDir() {
      return new File(properties.getProperty("OutputDir"));
   }

   private String getComponentLoggingOutputDirBasename() {
      String s = "components-" + gStartTimeString;
      return s;
   }

   public File getComponentLoggingOutputDir() {
      return new File(properties.getProperty("OutputDir") + "/" + getComponentLoggingOutputDirBasename());
   }

   public File getRunDir() {
      return runDir;
   }

   public Properties getProps() {
      return properties;
   }

   public Properties getAudioProps() {
      return audioProperties;
   }

   public boolean saveAudioProps() {
      boolean retval = false;
      try {
         audioProperties.store(new FileOutputStream(AUDIO_PROPERTIES_FILENAME), "Auto-generated from audio config");
         retval = true;
      } catch (FileNotFoundException e) {
         String msg = "Error saving audio properties, file not found: " + e;
         logger.error(msg, e);
      } catch (IOException e) {
         String msg = "Error saving audio properties, IOException: " + e;
         logger.error(msg, e);
      }

      return retval;
   }

   public void copyOverAudioProps() {
      // Copy from audio settings only to main settings.
      // We keep these separate since we save the audio properties.
      properties.putAll(audioProperties);
   }

   public TrialState getTrial() {
      return trial;
   }

   /* Unused
   public void abort() {
      asrController.abort();
      workflowController.abort();
   }
   */

   public boolean setTrialId(String trialId, boolean isReinit, boolean overwrite) {
      trialId = trialId.toUpperCase();
      runDir = new File(properties.getProperty("OutputDir") + trialId);
      if (overwrite && runDir.exists()) {
         try {
            Util.deleteRecursive(runDir);
         } catch (FileNotFoundException e) {
            logger.error("Could not erase directory");
         }
      }
      if (runDir.mkdirs()) {
         startTrial(trialId, isReinit);
         return true;
      } else {
         logger.error("Could not create directory: " + runDir.getPath());
         return false;
      }
   }

   public void startTrial(String trialId, boolean isReinit) {
      isProcessing = new AtomicBoolean(false);
      if (isReinit) {
         // Tell service controller to reset any components that keep state.
         // We do this first both so gets tacked on to last trial and since
         // can't log after close logger below.
         getServiceController().initNewTrial();

         //App.getApp().getServiceController().restartUW();
         workflowController.cleanup();
         org.apache.log4j.Logger.getRootLogger().removeAppender(logAppender);
         org.apache.log4j.Logger.getRootLogger().removeAppender(fileAppender);
      }
      final String logDir = runDir.getAbsolutePath();
      configureLogging(logDir);

      trial = new TrialState(trialId);


      workflowController = new WorkflowController();
      asrController = new ASRController();

      if (App.getApp().getProps().getProperty("FullLogging", "true").equals("true")) {
         // See if log binary session data
         String sessionDataLogDir = properties.getProperty("SessionDataLogDir", null);
         FileSystemLoggingListener listener = null;
         if (sessionDataLogDir != null) {
            // Check for "special" value
            if (sessionDataLogDir.compareTo("USE_TRIAL_DIR") == 0) {
               listener = new FileSystemLoggingListener(logDir);
            } else {
               listener = new FileSystemLoggingListener(sessionDataLogDir);
            }
         }
         workflowController.addListener(listener);
      }

      if (!isReinit) {
         CamelContext camel = (CamelContext) context.getBean("camel");
         try {
            camel.start();
         } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }

         serviceController = new ServiceController();
         try {
            serviceController.init(properties);
         } catch (Exception e) {
            logger.error("Exception with serviceController.init: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
         }
      }

      // Have a fallback default if unset
      playerCommand = properties.getProperty("Player", "/usr/bin/play");
   }

   public void init(List<String> propPaths) {
      isProcessing = new AtomicBoolean(false);
      properties = new Properties();
      for (String propPath : propPaths) {
         Properties newProps = new Properties();
         try {
            newProps.load(new FileReader(propPath));
            properties.putAll(newProps);
         } catch (FileNotFoundException e) {
            logger.error("Couldn't load props: " + e);
         } catch (IOException e) {
            logger.error("Couldn't load props: " + e);
         }
      }

      // Default to anything present in main properties
      audioProperties = new Properties();
      String acd = properties.getProperty("AudioCaptureDevice", null);
      if (acd != null) {
         audioProperties.setProperty("AudioCaptureDevice", acd);
      }
      String apd = properties.getProperty("AudioPlaybackDevice", null);
      if (apd != null) {
         audioProperties.setProperty("AudioPlaybackDevice", apd);
      }
      String acdIa = properties.getProperty("ia.AudioCaptureDevice", null);
      if (acdIa != null) {
         audioProperties.setProperty("ia.AudioCaptureDevice", acdIa);
      }
      String apdIa = properties.getProperty("ia.AudioPlaybackDevice", null);
      if (apdIa != null) {
         audioProperties.setProperty("ia.AudioPlaybackDevice", apdIa);
      }
      String apdMonitor = properties.getProperty("monitor.AudioPlaybackDevice", null);
      if (apdMonitor != null) {
         audioProperties.setProperty("monitor.AudioPlaybackDevice", apdMonitor);
      }

      Properties ap = new Properties();
      try {
         ap.load(new FileReader(AUDIO_PROPERTIES_FILENAME));
         // Override with any settings we loaded
         audioProperties.putAll(ap);
      } catch (FileNotFoundException e) {
         // Ignore
      } catch (IOException e) {
         // Ignore
      }
      // Copy settings over to regular properties
      properties.putAll(audioProperties);

      context = new FileSystemXmlApplicationContext(properties.getProperty("WorkflowPath"));
      timeKeeper = new TimeKeeper();

      if (properties.getProperty("Mode").equalsIgnoreCase("BatchListAudio")) {
         // setTrialId(properties.getProperty("BatchTrialId"), true);
         runBatchListAudioMode();
      } else if (properties.getProperty("Mode").equalsIgnoreCase("BatchFolderAudio")) {
         //right now do nothing, test runner will run this
      } else if (properties.getProperty("Mode").equalsIgnoreCase("BatchSessionData")) {
         runBatchSessionMode();
      } else {
         mainFrame = new MainFrame(properties.getProperty("EvalMode", "false").equals("true"));
         javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               mainFrame.promptForTrialId(false);
            }
         });
      }
   }

   private void configureLogging(final String logDir) {
      logAppender = new LogAppender();
      logAppender.setName("LogAppender");
      logAppender.setLayout(new EnhancedPatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSSSSS} : %-5p [%t] %c{1} - %m%n"));
      logAppender.activateOptions();
      org.apache.log4j.Logger.getRootLogger().addAppender(logAppender);

      fileAppender = new FileAppender();
      fileAppender.setFile(logDir + "/trialLog.log");
      fileAppender.setName("FileAppender");
      fileAppender.setLayout(new EnhancedPatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSSSSS} : %-5p [%t] %c{1} - %m%n"));
      fileAppender.setAppend(true);
      fileAppender.activateOptions();
      org.apache.log4j.Logger.getRootLogger().addAppender(fileAppender);

      boolean hasGUI = !((properties.getProperty("Mode").equalsIgnoreCase("BatchListAudio")
              || properties.getProperty("Mode").equalsIgnoreCase("BatchFolderAudio")
              ||properties.getProperty("Mode").equalsIgnoreCase("BatchSessionData")));


      ConsoleAppender consoleAppender = (ConsoleAppender)org.apache.log4j.Logger.getRootLogger().getAppender("console");
      if (hasGUI) {
         consoleAppender.setThreshold(Level.ALL);
      } else {
         consoleAppender.setThreshold(Level.ERROR);
      }

      logger.info("Starting logger. Component logging in: " + getComponentLoggingOutputDirBasename());
      /*
      Java 7 required; not in Java 6
      try {
         // Note, output directory might not exist yet.
         Files.createSymbolicLink("components", getComponentLoggingOutputDir());
      } catch (IOException e1) {
         // Ignore; don't worry if soft-link fails
      } catch (UnsupportedOperationException e2) {
         // Ignore; don't worry if soft-link fails
      }
      */
   }

   private void runBatchListAudioMode() {
      logger.info("Running batch audio mode");
      playAudio = false;
      runBatchList(getFilesForBatchList(properties.getProperty("BatchListEnglish", "")), Language.ENGLISH);
      runBatchList(getFilesForBatchList(properties.getProperty("BatchListArabic", "")), Language.IRAQI_ARABIC);
      logger.info("Batch mode finished");
      cleanup();
      System.exit(0);
   }

   private void runBatchList(List<String> fileNames, Language lang) {
      for (String fileName : fileNames) {
         File audioFile = new File(fileName);
         if (audioFile.getName().toLowerCase().endsWith(".wav")) {
            // String trialName = trialIdPrefix + "-" + count;
            String trialName = audioFile.getName();
            int extIndex = trialName.lastIndexOf(".");
            if (extIndex != -1)
               trialName = trialName.substring(0, extIndex);
            // if we have a servicecontroller, then it's a reinit
            if (setTrialId(trialName, serviceController != null, true)) {
               logger.info("Starting trial: " + trialName);
               try {
                  ASRState state = asrController.startASR(audioFile, lang, true);
                  if (state != null) {
                     workflowController.processASRState(state, true);
                  }
               } catch (Exception e) {
                  logger.error("Exception caught while batch processing:" + e.toString());
               }
            }
         }

      }
   }

   private List<String> getFilesForBatchList(String audioFileName) {
      List<String> fileNames = new ArrayList<String>();
      File audioFileList = new File(audioFileName);
      if (audioFileList.exists()) {

         try {
            BufferedReader br = new BufferedReader(new FileReader(audioFileList));
            String line;
            while ((line = br.readLine()) != null) {
               fileNames.add(line);
            }
         } catch (Exception e) {
            logger.error("Error reading the file: " + audioFileList);
         }
      }

      return fileNames;
   }

   //Used for system tests
   public void runFile(File audioFile, Language lang) {
      playAudio = false;
      ASRState state = asrController.startASR(audioFile, lang, true);
      workflowController.processASRState(state, true);
   }

   private void runBatchSessionMode() {
      logger.info("Running batch session mode");
      playAudio = false;
      File audioFileList = new File(properties.getProperty("BatchList"));
      List<String> fileLines = new ArrayList<String>();

      try {
         BufferedReader br = new BufferedReader(new FileReader(audioFileList));
         String line;
         while ((line = br.readLine()) != null) {
            fileLines.add(line);
         }
      } catch (Exception e) {
         logger.error("Error reading the file: " + audioFileList);
      }

      for (String fileLine : fileLines) {
         String[] sessionAudioPair = fileLine.split(" ");
         if (sessionAudioPair.length == 2) {
            File sessionFile = new File(sessionAudioPair[0]);
            File audioFile = new File(sessionAudioPair[1]);
            if (sessionFile.getName().toLowerCase().endsWith(".sessiondata")
                    && audioFile.getName().toLowerCase().endsWith(".wav")) {
               String trialName = sessionFile.getName();
               int extIndex = trialName.lastIndexOf(".");
               if (extIndex != -1)
                  trialName = trialName.substring(0, extIndex);
               // if we have a servicecontroller, then it's a reinit
               if (setTrialId(trialName, serviceController != null, false)) {
                  logger.info("Starting trial: " + trialName);
                  String workflowStartStr = properties.getProperty("BatchSessionWorkflowStart", "");
                  WorkflowTaskType workflowStart = com.sri.bolt.workflow.Util.getTypeForConfigName(workflowStartStr);
                  if (workflowStart != null) {
                     workflowController.startCustomWorkflow(workflowStart, sessionFile, audioFile, Language.ENGLISH, true);
                  }
               }
            }
         }

      }

      logger.info("Batch mode finished");
      cleanup();
      System.exit(0);
   }

   /**
    * This function is used to set the TrialId for the stress Test
    *
    * @param trialId
    * @param isReinit
    */
   public void setTrialIdForTest(String trialId, boolean isReinit) {
      trialId = trialId.toUpperCase();
      runDir = new File(properties.getProperty("OutputDir") + trialId);
      if (runDir.exists()) {
         try {
            delete(runDir);
         } catch (IOException e) {
            logger.error("Error could not remove old directory");
            System.exit(0);
         }
      }
      if (runDir.mkdirs()) {
         final String logDir = runDir.getAbsolutePath();

         if (!isReinit) {
            serviceController = new ServiceController();
            serviceController.init(properties);
         }

         trial = new TrialState(trialId);

         workflowController = new WorkflowController();
      } else {
         logger.error("Could not create directory: " + runDir.getPath());
         System.exit(-1);
      }
      playerCommand = properties.getProperty("Player", "/usr/bin/play");
   }

   public static void delete(File file) throws IOException {

      if (file.isDirectory()) {
         //directory is empty, then delete it
         if (file.list().length == 0) {
            file.delete();

         } else {
            //list all the directory contents
            String files[] = file.list();
            for (String temp : files) {
               //construct the file structure
               File fileDelete = new File(file, temp);
               //recursive delete
               delete(fileDelete);
            }
            //check the directory again, if empty then delete it
            if (file.list().length == 0) {
               file.delete();
            }
         }

      } else {
         //if file, then delete it
         file.delete();
      }
   }

   public void initTest(String propPath) {
      properties = new Properties();
      try {
         properties.load(new FileReader(propPath));
      } catch (FileNotFoundException e) {
         logger.error("Couldn't load props: " + e);
      } catch (IOException e) {
         logger.error("Couldn't load props: " + e);
      }
   }

   /**
    * This function is used to start a new trial It will check if we are in
    * batch mode or not. If not it will prompt for a new Trial Id
    */
   public void promptForNewTrial() {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            mainFrame.promptForTrialId(true);
         }
      });
   }

   public void cleanup() {
      if (serviceController != null) {
         serviceController.cleanup();
      }
      if (workflowController != null) {
         workflowController.cleanup();
      }
      if (asrController != null) {
         asrController.cleanup();
      }
      if (trial != null) {
         trial.writeTrialSummaries();
      }
      if (timeKeeper != null) {
         timeKeeper.writeOut();
      }

      try {
         CamelContext context = getCamelContext();
         ShutdownStrategy strategy = context.getShutdownStrategy();
         strategy.setTimeout(2);
         strategy.setTimeUnit(TimeUnit.SECONDS);
         context.stop();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public List<String> getErrorMessages() {
      return logAppender.getErrorMessages();
   }

   public boolean playAudio() {
      return playAudio;
   }

   public TimeKeeper getTimeKeeper() {
      return timeKeeper;
   }

   public AtomicBoolean getIsProcessing() {
      return isProcessing;
   }

   public CamelContext getCamelContext() {
      return (CamelContext) context.getBean("camel");
   }

   public EvalType getEvalType() {
      return evalType;
   }

   public void setEvalType(EvalType evalType) {
      this.evalType = evalType;
   }

   private static class AppHolder {
      private static final App INSTANCE = new App();
   }

   private App() {
      //PropertyConfigurator.configure("../resources/log4j.properties");
//      Properties props = new Properties();
    //  props.load(getClass().getResourceAsStream("/Users/Akhil/workspace/BOLT_DM_proj/src/log4j.properties"));
  //    PropertyConfigurator.configure(props);
	   PropertyConfigurator.configure("/Users/Akhil/workspace/BOLT_DM_proj/src/log4j.properties");
	   //    
        
   }

   private Properties properties;
   private Properties audioProperties;
   private ServiceController serviceController;
   private WorkflowController workflowController;

   private TimeKeeper timeKeeper;
   private ASRController asrController;
   private MainFrame mainFrame;
   private File runDir;

   private TrialState trial;

   private String playerCommand;

   private boolean playAudio = true;

   private ApplicationContext context;
   private AtomicBoolean isProcessing;

   private EvalType evalType = EvalType.WITH_CLARIFICATION;

   private LogAppender logAppender;
   private FileAppender fileAppender;

   private static final Logger logger = LoggerFactory.getLogger(App.class);

   private static final long gStartTime = System.currentTimeMillis();
   private static final SimpleDateFormat gDateFormat = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
   private static final String gStartTimeString = gDateFormat.format(gStartTime);

   private static final String AUDIO_PROPERTIES_FILENAME = "../resources/audio.ini";

   // Checked in
   private static final String VERSION_FILENAME = "../resources/version.txt";
   // Not checked in; updated by scripts; use if present
   private static final String SUBVERSION_FILENAME = "../resources/subversion.txt";

   // Static initialization from above files
   private static final String VERSION_STRING;
   private static final String SUBVERSION_STRING;
   private static final String FULL_VERSION_STRING;

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (UnsupportedLookAndFeelException e) {
         // TODO Auto-generated catch block
      } catch (ClassNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (InstantiationException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (IllegalAccessException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      App app = App.getApp();
      if (args.length == 0) {
         List<String> defaultProp = new ArrayList<String>();
         defaultProp.add("../resources/config.ini");
         app.init(defaultProp);
      } else {
         List<String> props = new ArrayList<String>();
         for (int count = 0; count < args.length; ++count) {
            props.add(args[count]);
         }

         app.init(props);
      }
   }

   public static String getShortVersionString() {
      return VERSION_STRING;
   }

   public static String getFullVersionString() {
      return FULL_VERSION_STRING;
   }

   static {
      File fVersion = new File(VERSION_FILENAME);
      String tmp = null;

      try {
         tmp = new String(FileIOUtil.loadFileData(fVersion));
      } catch (IOException e) {
         // Ignore; tmp will stay null
      }

      if (tmp == null) {
         tmp = "UNKNOWN";
      }
      VERSION_STRING = tmp;

      tmp = null;
      File fSubversion = new File(SUBVERSION_FILENAME);
      try {
         tmp = new String(FileIOUtil.loadFileData(fSubversion));
      } catch (IOException e) {
         // Ignore; tmp will stay null
      }

      if (tmp == null) {
         tmp = "";
      }
      SUBVERSION_STRING = tmp;

      String fullVersion = VERSION_STRING;
      if (SUBVERSION_STRING.length() > 0) {
         fullVersion += "\n" + SUBVERSION_STRING;
      }

      FULL_VERSION_STRING = fullVersion;

      logger.info("Version: " + fullVersion.trim().replaceAll("\n+", "; "));
   }
}
