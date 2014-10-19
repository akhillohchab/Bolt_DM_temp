package com.sri.bolt.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.Message;
import com.sri.bolt.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SocketServiceClient implements ServiceClient {
   public SocketServiceClient(Properties props) {
      executor = Executors.newSingleThreadExecutor();
      enableComponentLogging = Boolean.parseBoolean(props.getProperty("EnableComponentLogging", "false"));
      killEndpointPath = props.getProperty("KillEndpointPath");
      killEndpointExe = props.getProperty("KillEndpointExe");
      portAvailablePath = props.getProperty("PortAvailablePath");
      portAvailableExe = props.getProperty("PortAvailableExe");
   }

   @Override
   public void reinit() {
      // Default assumes no state saved; override if need to clear state
   }

   protected byte[] makeServiceCall(String methodName, String args, boolean retry) {
      return makeServiceCall(methodName, args.toString().getBytes(charset), retry);
   }

   protected byte[] makeServiceCall(String methodName, boolean retry) {
      return makeServiceCall(methodName, new byte[0], retry);
   }

   protected byte[] makeServiceCall(String methodName, Message message, boolean retry) {
      return makeServiceCall(methodName, message.toByteArray(), retry);
   }

   protected byte[] makeServiceCall(String methodName, byte[] args, boolean retry) {
      // If has died, be sure has started first
      if (!endpointProcessAlive()) {
         logger.debug("Starting process that's not alive");
         init();
      }

      if (retry) {
         int numAttempts = 0;
         while (numAttempts < maxAttempts) {
            String maybeRetry = "No retries left";
            if (numAttempts + 1 < maxAttempts) {
               // If error, don't say will retry if won't
               maybeRetry = "Attempting to retry, maxAttempts=" + maxAttempts;
            }
            doPreCallWork(numAttempts);
            SocketServiceCall call = new SocketServiceCall(socket, methodName, args);
            Future<byte[]> result = executor.submit(call);
            byte[] returnVal;
            int timeoutSecs = getTimeout();
            try {
               returnVal = result.get(timeoutSecs, TimeUnit.SECONDS);
               return returnVal;
            } catch (InterruptedException e) {
               logger.error("Caught Exception during " + methodName + ". " + maybeRetry + ": " + e.getMessage(), e);
            } catch (ExecutionException e) {
               logger.error("Caught Exception during " + methodName + ". " + maybeRetry + ": " + e.getMessage(), e);
            } catch (TimeoutException e) {
               logger.error(methodName + " timed out (timeout=" + timeoutSecs + "s). " + maybeRetry, e);
               // need to try to cancel task
               result.cancel(true);
            }

            // If we're here, the call was not successful

            // Close socket client is not referencing server.
            try {
               socket.close();
            } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

            // Kill the process we started.
            endpoint.destroy();

            // If we can't kill the endpoint, we need to return null.
            // Note: currently only returns true.
            // The destroy() call above should be adequate if we started
            // the process but process listening at port could have been
            // started externally.
            if (!killEndpoint(port)) {
               return null;
            }

            // Wait til process is dead for sure before we try to reinit.
            try {
               endpoint.waitFor();
            } catch (InterruptedException e) {
               logger.error("Interrupted waiting for process to end: " + e.getMessage(), e);
            }

            ++numAttempts;
            if (numAttempts < maxAttempts) {
               init();
            }
         }
      } else {
         doPreCallWork(0);
         SocketServiceCall call = new SocketServiceCall(socket, methodName, args);
         try {
            return call.call();
         } catch (Exception e) {
            // TODO Hiding this since it will always pop up when we call
            // endProcessing on UW
            // Need to find a better way of handling that case
            // App.getApp().getLogger().debug(methodName + " failed");
         }
      }

      return null;
   }

   // Override this method if you need to do work before a service call, like
   // connect to a socket if you don't have a constant connection
   protected void doPreCallWork(int numAttempts) {
   }

   /**
    * See if the process we started is running.
    * @return true if running, false if not.
    */
   protected boolean endpointProcessAlive() {
      boolean retval = false;
      if (endpoint != null) {
         try {
            int exitVal = endpoint.exitValue();
         } catch (IllegalThreadStateException e) {
            // Process is still running.
            // This is generally what we expect.
            retval = true;
         }
      }
      return retval;
   }

   protected boolean killEndpoint(String port) {
      return killByPort(port, "./" + killEndpointExe, killEndpointPath);
   }

   public boolean killEndpoint() {
      return killEndpoint(port);
   }

   protected int getTimeout() {
      return TIMEOUT;
   }

    /*
   protected void closeStreams(Process p) {
      InputStream processOut = p.getInputStream();
      if (processOut != null) {
          try {
             processOut.close();
          } catch (IOException e) {
             // Ignore
          }
      }
      InputStream osErr = p.getErrorStream();
      if (osErr != null) {
          try {
             osErr.close();
          } catch (IOException e) {
             // Ignore
          }
      }
   }
    */

   public Process startAndHandleLogging(ProcessBuilder pb, String basename) throws IOException {
      return startAndHandleLogging(pb, basename, enableComponentLogging, null);
   }

   public static Process startAndHandleLogging(ProcessBuilder pb, final String basename, boolean writeLogFile, final Runnable optionalFinishedRunnable) throws IOException {
      OutputStream os = null;
      OutputStream errOs = null;
      if (writeLogFile) {
         try {
            os = openOutputLog(basename + "Output");
            errOs = openOutputLog(basename + "Error");
         } catch (Exception e) {
            // Ignore - leave as null
            os = null;
         }
      }
      boolean errorRedirected = false;
      /*
       * No longer redirect so we can also log to logging facility
       * and tell stderr from stdout
      if ((os == null) || (errOs == null)) {
         // Not saving the output so just join them together and read so they don't fill up
         // We differentiate stderr and stdout logs below so we don't redirect
         // both streams to stdout here.
         pb.redirectErrorStream(true);
         errorRedirected = true;
      }
      */

      Process nfRetval = null;
      try {
         nfRetval = pb.start();
      } catch (Exception e) {
         // Will be null
      }
      final Process retval = nfRetval;

      if (retval == null) {
         logger.error("Process null calling start for basename " + basename);
      }

      // Note: if opened, below call should close streams when finished.
      // The calls below will read the process stdout and stderr (if not already
      // redirected to stdout) and pass along to the specified output stream
      // if non-null *or* just ignore the output if last param is null.
      if (retval != null) {
         readOutputStream(retval, basename, os);
         if (!errorRedirected) {
            readErrorStream(retval, basename, errOs);
         }
      }

      if (optionalFinishedRunnable != null) {
         Runnable onFinished = new Runnable() {
            public void run() {
               if (retval != null) {
                  try {
                     int exitValue = retval.waitFor();
                     logger.info("Process for " + basename + " exited with value " + exitValue);
                  } catch (InterruptedException e) {
                     // Ignore
                     logger.warn("InterruptedExcepton for " + basename);
                  }
               }
               optionalFinishedRunnable.run();
            }
         };
         Thread t = new Thread(onFinished);
         t.start();
      }

      return retval;
   }

   public static boolean killByPort(String port, String killExe, String killPath) {
      ProcessBuilder pb = new ProcessBuilder("./" + killExe, port);
      pb.directory(new File(killPath));
      Process proc;
      try {
         proc = pb.start();
         proc.waitFor();
      } catch (IOException e) {
         logger.error("Kill endpoint failed: " + e.getMessage(), e);
      } catch (InterruptedException e) {
         logger.error("Kill endpoint failed: " + e.getMessage(), e);
      }

      return true;
   }

   private static OutputStream openOutputLog(String basename) throws IOException {
      long time = System.currentTimeMillis();
      // Organize by putting in "components-{TS}" subdirectory, making directory if needed
      String logDir = App.getApp().getComponentLoggingOutputDir().getAbsolutePath();
      File f = new File(logDir);
      if (!f.isDirectory()) {
         f.mkdirs();
      }
      String filename = logDir + "/" + basename + "-" + dateFormat.format(time) + ".log";
      FileOutputStream fos = new FileOutputStream(filename);
      return fos;
   }

   private static void readOutputStream(Process p, String descr, OutputStream os) {
      InputStream processOut = p.getInputStream();
      readStream(p, descr, processOut, "stdout", os);
   }

   private static void readErrorStream(Process p, String descr, OutputStream os) {
      InputStream processErr = p.getErrorStream();
      readStream(p, descr, processErr, "stderr", os);
   }

   // May assume stderr was redirected to stdout so only
   // have to read one. If os is non-null, write output
   // to stream.
   // As of 9/30, split into lines and log to log facility:
   //  stdout -> logger.debug
   //  stderr -> logger.error
   private static void readStream(final Process p, final String descr, final InputStream processStream, final String streamName, final OutputStream os) {
      // Prefix for logging:
      final String logPrefix = descr + "-" + streamName + ":";
      if (processStream == null) {
         logger.error(logPrefix + "stream null; can't read");
      } else {
         Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
               boolean isErrorStream = false;
               if ((streamName != null) && (streamName.compareTo("stderr") == 0)) {
                  isErrorStream = true;
               }

               // Read line at a time
               BufferedReader br = new BufferedReader(new InputStreamReader(processStream));

               String line;
               boolean ended = false;
               while (!ended) {
                  try {
                     line = br.readLine();
                  } catch (IOException e) {
                     line = null;
                  }
                  if (line == null) {
                     ended = true;
                     break;
                  }
                  // Newline has been removed - which is what we desire
                  // for log facility.
                  if (isErrorStream) {
                     // Currently, both use "debug" but only so we don't
                     // fill up the logs with too many distracting messages.
                     //logger.error(logPrefix + line);
                     logger.debug(logPrefix + line);
                  } else {
                     logger.debug(logPrefix + line);
                  }
                  if (LOG_PROCESS_OUTPUT_TO_CONSOLE) {
                      System.out.println(logPrefix + line);
                  }
                  if (os != null) {
                     byte[] buf = line.getBytes();
                     if ((buf != null) && (buf.length > 0)) {
                        try {
                           os.write(buf, 0, buf.length);
                           // Add a newline
                           os.write('\n');
                           os.flush();
                        } catch (Exception e) {
                           // Ignore
                        }
                     }
                  }
               }

               String exitValueString = "HASN'T EXITED";
               try {
                   exitValueString = Integer.toString(p.exitValue());
               } catch (IllegalThreadStateException e) {
                   // Ignore - hasn't exited yet
               }

               // Wait for process to finish
               String msg = "End of stream reading output stream " + streamName + " for " + descr +
                            ", exit value: " + exitValueString;
               logger.warn(msg);
               if (os != null) {
                  String logfileMsg = "\n---------------------------------------------------\n" + msg + "\n";
                  try {
                     os.write(logfileMsg.getBytes());
                  } catch (IOException e) {
                     // Ignore
                  }
               }

               // May or may not be an error depending on if we killed process;
               // Made this only of type debug since previous message
               // will be more informative.
               logger.debug("Done reading output stream " + streamName + " for process " + descr);
               if (os != null) {
                   // Close any OutputStream when done
                   try {
                       os.close();
                   } catch (Exception e) {
                       // Ignore
                   }
               }

               // Wait for process to end
               while (true) {
                   try {
                       // Don't need to keep reading if process exited
                       int exitVal = p.exitValue();
                       // May or may not be an error depending on if we killed process
                       logger.warn("End of process with exit value " + exitVal + " reading output stream " + streamName + " for " + descr);
                       break;
                   } catch (IllegalThreadStateException e) {
                      // Process still running - wait before checking again
                      try {
                         Thread.sleep(1000);
                      } catch (InterruptedException e1) {
                         // TODO Auto-generated catch block
                         e1.printStackTrace();
                      }
                   }
                }
             }
          });
          t.start();
      }
   }

   private static final Logger logger = LoggerFactory.getLogger(SocketServiceClient.class);
   protected Process endpoint;
   protected boolean enableComponentLogging;
   protected String killEndpointPath;
   protected String killEndpointExe;
   protected String portAvailablePath;
   protected String portAvailableExe;
   protected Socket socket;
   protected String port;
   private ExecutorService executor;
   protected int maxAttempts = 2;
   private static final int TIMEOUT = 30;
   private static final Charset charset = Charset.forName("UTF8");

   private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");

   // If enabled, all output streams read for socket clients will be written to stdout.
   // With EnableComponentLogging=true, this isn't as necessary but can be helpful to
   // watch the stream more in realtime. However, there is a lag in reading the streams
   // and they are not necessarily grabbed at good boundaries so this isn't necessarily
   // clean output.
   private static final boolean LOG_PROCESS_OUTPUT_TO_CONSOLE = false;
}
