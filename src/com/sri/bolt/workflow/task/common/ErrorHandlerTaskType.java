package com.sri.bolt.workflow.task.common;

public enum ErrorHandlerTaskType {
   FORCED_ALIGNMENT("Forced Alignment Error Handler"), OOV("OOV Detector Error Handler"), CONF_SCORER("Confidence Scorer Error Handler"),
   ANSWER_MERGER("Answer Merger Error Handler"), DIALOG_MANAGER("Dialog Manager Error Handler");

   private ErrorHandlerTaskType(final String text) {
      this.text = text;
   }

   private final String text;

   @Override
   public String toString() {
      return text;
   }
}
