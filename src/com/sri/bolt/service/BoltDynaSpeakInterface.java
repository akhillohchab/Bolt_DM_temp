package com.sri.bolt.service;

import com.sri.recognizer.message.RecognizerMessages.CombinedRecognizerResult;


public interface BoltDynaSpeakInterface {

   CombinedRecognizerResult endSamples();

   void cleanup();

   boolean startSamples();

   boolean sendSamples(byte[] audio);

   // Name of grammar rule.
   boolean setGrammar(String rule);

   // "Wrapper" since takes simple sentence and converts to JSGF grammar.
   boolean extendGrammarWrapper(String rescoredOneBest);

   boolean setRescoringLMIndex(int index);

   boolean resetMllr();
}
