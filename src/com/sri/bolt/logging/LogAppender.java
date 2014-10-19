package com.sri.bolt.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogAppender extends AppenderSkeleton {
   public LogAppender() {
      errorMessages = new ArrayList<LoggingEvent>();
   }
   @Override
   protected synchronized void append(LoggingEvent loggingEvent) {
      if (loggingEvent.getLevel() == Level.ERROR || loggingEvent.getLevel() == Level.FATAL) {
         errorMessages.add(loggingEvent);
         com.sri.bolt.ui.Util.addErrorMessage(loggingEvent.getRenderedMessage(), new Date(loggingEvent.getTimeStamp()));
      }
      com.sri.bolt.ui.Util.addDebugMessage(loggingEvent.getRenderedMessage());
   }

   @Override
   public void close() {
   }

   @Override
   public boolean requiresLayout() {
      return false;
   }

   public List<String> getErrorMessages() {
      List<String> errors = new ArrayList<String>();
      for (LoggingEvent event : errorMessages) {
         errors.add(event.getRenderedMessage());
      }

      return errors;
   }

   private List<LoggingEvent> errorMessages;
}
