package com.sri.bolt.audio;

import com.sri.bolt.state.ASRState;

public class ASRResult {
   public ASRResult(ASRState state, boolean successful, String errorMsg) {
      this.successful = successful;
      this.errorMsg = errorMsg;
      this.state = state;
   }

   public boolean successful;
   public String errorMsg;
   public ASRState state;
}
