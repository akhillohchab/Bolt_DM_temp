package com.sri.bolt;

public class InvalidComponentResponse extends Exception {
   public InvalidComponentResponse(String component) {
      super();
      this.component = component;
   }
   
   public String component;
}
