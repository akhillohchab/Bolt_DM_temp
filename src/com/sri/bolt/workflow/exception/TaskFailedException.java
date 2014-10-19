package com.sri.bolt.workflow.exception;

public class TaskFailedException extends Exception {
   public TaskFailedException(Exception e) {
      this.parent = e;
   }

   public Exception getParent() {
      return parent;
   }

   public void setParent(Exception parent) {
      this.parent = parent;
   }

   private Exception parent;
}
