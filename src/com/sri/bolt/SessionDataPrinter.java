package com.sri.bolt;

import java.io.File;
import java.io.IOException;

import com.google.protobuf.Message;
import com.sri.bolt.message.BoltMessages.SessionData;

/**
 * Simple class to read raw SessionData protobuffer from
 * disk then print it. Prints to stdout by default but
 * can specify output to file.
 * @author frandsen
 *
 */
public class SessionDataPrinter {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // Parse all the arg pairs
        String inFile = null;
        String outFile = null;
        boolean printUsage = false;
        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].compareTo("-i") == 0) {
                inFile = args[i + 1];
            } else if (args[i].compareTo("-o") == 0) {
                outFile = args[i + 1];
            } else {
                printUsage = true;
                break;
            }
        }

        if (printUsage || (args.length < 2)) {
            System.out.println("Usage:\n" +
               " -i infile (raw buffer to wrap)\n" +
               " -o outfile (write readable version to file versus stdout)\n");
            System.out.println("Example: -i inproto.raw -o outproto.raw");
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

        SessionData message = SessionData.parseFrom(rawData);
        String s = message.toString();

        // Do we write the output to file?
        if (outFile == null) {
            System.out.println(s);
        } else {
            byte[] data = s.getBytes("UTF8");
            FileIOUtil.saveFileData(outFile, data);
        }
    }
}
