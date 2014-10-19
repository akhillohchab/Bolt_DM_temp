package com.sri.bolt.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience routine that keeps attempting to connect to remote host:port
 * after service starts.
 * @author michael.frandsen@sri.com
 *
 */
public class SocketServiceConnector {
    // Last param added. If non-null, will see if associated process has exited
    public static Socket getConnected(String name, int maxSeconds, InetSocketAddress addr, Process p) {
      logger.info("getConnected with " + name + " " + maxSeconds);

      long startTime = System.currentTimeMillis();

      // The loop is to wait for process to initialize and listen
      boolean connected = false;
      int nTries = 0;
      final int retryMillis = 1000;
      // Add 1 since our first try is ASAP with no wait
      final int maxTries = 1 + (maxSeconds * 1000) / retryMillis;
      Socket retval = null;
      while (!connected && (nTries < maxTries)) {
          nTries++;
          // Create new on each attempt or subsequent connect attempts say connection closed
          retval = new Socket();
          try {
              retval.connect(addr);
              connected = true;
          } catch (IOException e) {
              if (nTries == maxTries) {
                  // Only log on the last attempt since initial connection failures are expected
                  logger.error(name + ": connecting to " + addr + ": " + e.getMessage(), e);
              }
          }
          if (!connected && (nTries < maxTries - 1)) {
             // See if process died
             if (p != null) {
                 try {
                     int exitVal = p.exitValue();
                     // XXX If here, process exited or died so we give up
                     String m = name + " exited with " + exitVal + " while connecting to " + addr;
                     logger.info(m);
                     logger.error(m);
                     break;
                 } catch (IllegalThreadStateException e) {
                    // Do nothing - expect process to have not exited
                 }
             }
             try {
                 // We poll this often until server is ready
                 Thread.sleep(retryMillis);
             } catch (InterruptedException e1) {
                 logger.error(e1.getMessage(), e1);
             }
          }
      }
      if (!connected) {
         // XXX Should be fatal, can't connect...
         logger.error(name + ": FATAL couldn't connect after " + nTries + " attempts");
      } else {
         long elapsed = System.currentTimeMillis() - startTime;
         String dispElapsed = new DecimalFormat("#.##").format(elapsed / (10.0 * maxSeconds));
         logger.info("  " + elapsed + " ms (" + dispElapsed + "%) for getConnected with " + name);
      }

      return retval;
   }

   private static final Logger logger = LoggerFactory.getLogger(SocketServiceClient.class);
}
