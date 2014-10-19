package com.sri.bolt.ui;


import com.sri.bolt.App;
import com.sri.interfaces.lang.Language;

import java.util.Date;

public class Util {
   public static void addUserMessage(final String text, final Language language) {
      if (App.getApp().getMainFrame() != null) {
         App.getApp().getMainFrame().addUserMessage(text, new Date(), language);
      }
   }

   public static void addSystemMessage(final String text, final Language language) {
      if (App.getApp().getMainFrame() != null) {
         App.getApp().getMainFrame().addSystemMessage(text, new Date(), language);
      }
   }

   public static void addTranslationMessage(final String text, final Language language) {
      if (App.getApp().getMainFrame() != null) {
         App.getApp().getMainFrame().addTranslationMessage(text, new Date(), language);
      }
   }


   public static void addErrorMessage(final String text, final Date time) {
      if (App.getApp().getMainFrame() != null) {
         App.getApp().getMainFrame().addErrorMessage(text, time);
      }
   }

   public static void addDebugMessage(final String text) {
      if (App.getApp().getMainFrame() != null) {
         App.getApp().getMainFrame().addDebugMessage(text, new Date());
      }
   }
}
