package com.sri.bolt.audio;

public class TaskReturn {
   public TaskReturn(boolean success, String resultText) {
      this.success = success;
      this.resultText = resultText;
   }

   public final boolean success;
   public final String resultText;
}
