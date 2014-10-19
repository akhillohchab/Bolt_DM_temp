package com.sri.bolt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileIOUtil {
    public static byte[] loadFileData(File f) throws IOException {
        int rawDataSize = (int)f.length();
        byte[] rawData = new byte[rawDataSize];
        InputStream inStream = new FileInputStream(f);
        int nread = 0;
        while (nread < rawDataSize) {
            int rd = inStream.read(rawData, nread, rawDataSize - nread);
            if (rd < 0) {
                throw new IOException("Error while reading " + f.getAbsolutePath());
            }
            nread += rd;
        }
        inStream.close();

        return rawData;
    }

    public static byte[] readFully(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        int nr;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((nr = in.read(buf)) >= 0) {
            if (nr > 0) {
                bos.write(buf, 0, nr);
            }
        }
        return bos.toByteArray();
    }

    public static void saveFileData(String filename, byte[] data) throws IOException {
        OutputStream outStream = new FileOutputStream(filename);
        if (data != null) {
            outStream.write(data);
        }
        outStream.close();
    }
}
