package com.sri.bolt.service;

public interface TTSServiceClient extends ServiceClient {
   public void textToSpeechFile(String text, String filePath);
}
