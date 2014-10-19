package com.sri.bolt.service;

import com.sri.bolt.message.BoltMessages.SessionData;

public interface SessionDataServiceClient extends ServiceClient {
   public SessionData checkInput(SessionData data);

   public SessionData process(SessionData data);
}
