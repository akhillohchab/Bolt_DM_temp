package com.sri.bolt.audio;

public class AudioLevelValues {
   public final int LMIN;
   public final int LMAX;
   public final int LVALUE;

   public AudioLevelValues(int min, int max, int value) {
      LMIN = min;
      LMAX = max;
      LVALUE = value;
   }
}
