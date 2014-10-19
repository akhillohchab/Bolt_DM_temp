package com.sri.bolt.state;

import com.sri.bolt.App;
import com.sri.interfaces.lang.BuckwalterUnicodeConverter;
import com.sri.interfaces.lang.Language;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Util {
   public static String getFileName(String trialId, String turnType, Date time) {
      StringBuilder builder = new StringBuilder(App.getApp().getRunDir().getPath() + "/" + trialId + "-");
      if (!turnType.equals("")) {
         builder.append(turnType + "-");
      }
      builder.append(new Long(time.getTime() / 1000));

      return builder.toString();
   }

   /**
    * Will format a time so that it represents the number of seconds past a start time in the format
    * SSSS.hh
    *
    * @param startTime The beginning of
    * @param time
    * @return
    */
   public static String getTimeForDisplay(Date startTime, Date time) {
      Date timeDiff = new Date(time.getTime() - startTime.getTime());

      CALENDAR.setTime(timeDiff);
      int seconds = CALENDAR.get(Calendar.SECOND) + (CALENDAR.get(Calendar.MINUTE) * 60);
      int hundreds = CALENDAR.get(Calendar.MILLISECOND) / 10;


      String timeStr = String.valueOf(seconds);
      while (timeStr.length() < NUM_SECOND_DIGITS) {
         timeStr = "0" + timeStr;
      }

      timeStr += ".";
      if (hundreds < 10) {
         timeStr += "0";
      }
      timeStr += String.valueOf(hundreds);

      return timeStr;
   }

   public static void indent(XMLStreamWriter writer, int tabLevel) throws XMLStreamException {
      for (int count = 0; count < tabLevel; ++count) {
         writer.writeCharacters("\t");
      }
   }

   public static String getStringForDisplay(String str, Language lang) {
      if (lang == Language.ENGLISH) {
         return str;
      } else {
         return BuckwalterUnicodeConverter.buckwalterToUnicode(str, lang);
      }
   }

   private static final Calendar CALENDAR = new GregorianCalendar();
   private static final int NUM_SECOND_DIGITS = 5;
}
