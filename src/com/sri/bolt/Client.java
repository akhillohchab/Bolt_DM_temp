package com.sri.bolt;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * This class is to test sending a protobuffer on disk
 * to a client over a socket. To the protobuffer, wraps as:
 * method(len)\n\nmethname\n\narg0[0]protobuf\n\nend\n\n
 * @author frandsen
 *
 */
public class Client {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Parse all the arg pairs
        String server = "localhost";
        int port = -1;
        String inFile = null;
        String outFile = null;
        String method = null;
        String argNameIn = "arg0";
        String argNameOut = "arg0";
        boolean printUsage = false;
        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].compareTo("-s") == 0) {
                server = args[i + 1];
            } else if (args[i].compareTo("-p") == 0) {
                port = Integer.parseInt(args[i + 1]);
            } else if (args[i].compareTo("-i") == 0) {
                inFile = args[i + 1];
            } else if (args[i].compareTo("-o") == 0) {
                outFile = args[i + 1];
            } else if (args[i].compareTo("-m") == 0) {
                method = args[i + 1];
            } else if (args[i].compareTo("-ai") == 0) {
                argNameIn = args[i + 1];
            } else if (args[i].compareTo("-ao") == 0) {
                argNameOut = args[i + 1];
            } else {
                printUsage = true;
                break;
            }
        }

        if (printUsage || (args.length < 2)) {
            System.out.println("Usage:\n" +
               " -s server (default=localhost)\n" +
               " -p port\n" +
               " -i infile (raw buffer to wrap)\n" +
               " -o outfile (wrapped return value to write)\n" +
               " -m method (method name to call)\n" +
               " -ai inputArgName (default=arg0)\n" +
               " -ao outputArgName (default=arg0)");
            System.out.println("Example: -p 8020 -m processSentence -i inproto.raw -o outproto.raw");
            System.exit(1);
        }

        File f = new File(inFile);
        if (!f.exists()) {
            System.err.println("Cannot open file for reading: " + inFile);
            System.exit(1);
        }

        byte[] rawData = FileIOUtil.loadFileData(f);
        if (rawData == null) {
            System.err.println("Got null data for file: " + inFile);
            System.exit(1);
        }

        // Assume all needed args are present; fine to throw exception
        InetSocketAddress addr = new InetSocketAddress(server, port);
        Socket s = new Socket();
        s.connect(addr);

        OutputStream os = s.getOutputStream();
        HashMap<String, byte[]> values = new HashMap<String, byte[]>();
        values.put("method", method.getBytes("UTF8"));
        values.put(argNameIn, rawData);
        com.sri.bolt.message.Util.writeMessageHash(os, values);

        HashMap<String, byte[]> retval = com.sri.bolt.message.Util.readMessageHash(s.getInputStream());
        os.close();

        // Do we write the output to file?
        if (outFile != null) {
            byte[] data = retval.get(argNameOut);
            FileIOUtil.saveFileData(outFile, data);
        }

        s.close();
    }
}
