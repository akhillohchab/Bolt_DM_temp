package com.sri.bolt.service;

/**
 * A service client is the point of communication between the controller and another process.
 * @author peter.blasco@sri.com
 *
 */
public interface ServiceClient {
   public void init();

   // Called to reset any state, such as for a new trial
   public void reinit();

   public void cleanup();
}
