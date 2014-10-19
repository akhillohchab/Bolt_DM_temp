package com.sri.bolt.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;

public class SocketServiceCall implements Callable<byte[]> {
   public SocketServiceCall(Socket socket, String methodName, byte[] args) {
      this.args = args;
      this.methodName = methodName;
      this.socket = socket;
   }
   
   @Override
   public byte[] call() throws Exception {
      return makeServiceCall(socket, methodName, args);
   }
   
   private static byte[] makeServiceCall(Socket socket, String methodName, byte[] args) throws IOException {
      OutputStream stream = socket.getOutputStream();
      InputStream inputStream = socket.getInputStream();

      com.sri.bolt.message.Util.writeMessage(stream, methodName, args);
      return com.sri.bolt.message.Util.readMessage(inputStream);
   }
   
   private byte[] args;
   private String methodName;
   private Socket socket;
}
