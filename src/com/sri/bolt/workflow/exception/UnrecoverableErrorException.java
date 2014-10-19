package com.sri.bolt.workflow.exception;

public class UnrecoverableErrorException extends Exception {
   public UnrecoverableErrorException(String err) {
      super(err);
   }

   public UnrecoverableErrorException() {
      super();
   }
}
