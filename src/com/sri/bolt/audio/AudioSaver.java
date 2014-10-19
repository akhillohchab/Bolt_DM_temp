package com.sri.bolt.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class AudioSaver {
   private static final int HEADER_SIZE = 44;

   public static File writeAudioFile(String fileName, ByteArrayOutputStream rawAudioData, boolean overwrite) {
      try {
         String newFileName = new String(fileName);
         int suffixCount = 1;
         File file;
         if (overwrite) {
            newFileName = fileName;
            file = new File(newFileName + ".wav");
         } else {
            while (!((file = new File(newFileName + ".wav")).createNewFile())) {
               newFileName = new String(fileName + "-" + String.valueOf(suffixCount++));
            }
         }

         if (rawAudioData == null) {
            logger.error("writeAudioFile given null rawAudioData");
            return null;
         }
         BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newFileName + ".wav"));
         outputStream.write(createWavHeader(rawAudioData.size(), 16000, 1, 2));
         outputStream.write(rawAudioData.toByteArray());
         outputStream.close();
         outputStream.flush();

         return file;
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
         return null;
      }
   }

   /**
    * This method creates the proper wave file header for the specified parameters.
    * <p/>
    * Example: To create a wave file header for a raw file, sampled at 16KHz, mono, and 16 bits.<br>
    * dataSize = rawFile.length<br>
    * samplingRate = 16000<br>
    * numChannels = 1<br>
    * bytesPerSampleSlice = 2<br>
    *
    * @param dataSize            The number of bytes for the raw sound data
    * @param samplingRate        The number of sample slices per second.
    * @param numChannels         The number of seperate audio channels. A value of "1" means mono, a value of "2" means stereo, etc.
    * @param bytesPerSampleSlice The number of bytes used for each sample slice
    * @return Returns the proper wave file header as a byte[] of length 44
    */
   public static byte[] createWavHeader(int dataSize, int samplingRate, int numChannels, int bytesPerSampleSlice) {
      byte header[] = {
              // ["RIFF"              ]  [total_len - 8       ]
              0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,

              // ["WAVE"              ]  ["fmt "              ]
              0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20,

              // [fmt len             ]  [Codec   ]  [# Chan  ]
              0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,

              // [sampling rate       ]  [Avg. Bytes / Second ]
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

              // [B / smpl]  [b / smpl]  ["data"              ]
              0x02, 0x00, 0x10, 0x00, 0x64, 0x61, 0x74, 0x61,

              // [data_len            ]
              0x00, 0x00, 0x00, 0x00
      };

      if (dataSize < 0) {
         throw new IllegalArgumentException("invalid file size : " + dataSize);
      }
      if (samplingRate <= 0) {
         throw new IllegalArgumentException("invalid sampling rate : " + samplingRate);
      }
      if (numChannels <= 0) {
         throw new IllegalArgumentException("invalid num Channels : " + numChannels);
      }
      if (bytesPerSampleSlice <= 0) {
         throw new IllegalArgumentException("invalid bytes per sample slice : " + bytesPerSampleSlice);
      }

      // dataSize must be a multiple of bytesPerSampleSlice
      // and bytesPerSampleSlice must be a multiple of numChannels.
      if (dataSize % bytesPerSampleSlice != 0) {
         throw new IllegalArgumentException("dataSize must be a multiple of bytesPerSampleSlice");
      }
      if (bytesPerSampleSlice % numChannels != 0) {
         throw new IllegalArgumentException("bytesPerSampleSlice must be a multiple of numChannels");
      }

      // Need byte of padding if data size not multiple of 2
      // (Can only happen if numChannels=1 and bytesPerSampleSlice=1)
      int padding = 0;
      if (dataSize % 2 != 0) {
         // 1 byte of padding
         padding = 1;
      }

      // Total length of file less first 8 bytes
      int riff_len = dataSize + padding + HEADER_SIZE - 8;

      // Total length of audio data; padding not included here.
      int data_len = dataSize;

      int riff_len_ptr = 4;
      int num_channels_ptr = 22;
      int sampling_rate_ptr = 24;
      int avg_bytes_per_second_ptr = 28;
      int bytes_per_sample_ptr = 32;
      int sig_bits_per_sample_ptr = 34;
      int data_len_ptr = 40;

      header[riff_len_ptr + 0] = (byte) ((riff_len) & 0xFF);
      header[riff_len_ptr + 1] = (byte) ((riff_len >> 8) & 0xFF);
      header[riff_len_ptr + 2] = (byte) ((riff_len >> 16) & 0xFF);
      header[riff_len_ptr + 3] = (byte) ((riff_len >> 24) & 0xFF);

      header[data_len_ptr + 0] = (byte) ((data_len) & 0xFF);
      header[data_len_ptr + 1] = (byte) ((data_len >> 8) & 0xFF);
      header[data_len_ptr + 2] = (byte) ((data_len >> 16) & 0xFF);
      header[data_len_ptr + 3] = (byte) ((data_len >> 24) & 0xFF);

      header[num_channels_ptr + 0] = (byte) ((numChannels) & 0xFF);
      header[num_channels_ptr + 1] = (byte) ((numChannels >> 8) & 0xFF);

      header[sampling_rate_ptr + 0] = (byte) ((samplingRate) & 0xFF);
      header[sampling_rate_ptr + 1] = (byte) ((samplingRate >> 8) & 0xFF);
      header[sampling_rate_ptr + 2] = (byte) ((samplingRate >> 16) & 0xFF);
      header[sampling_rate_ptr + 3] = (byte) ((samplingRate >> 24) & 0xFF);

      int abps = samplingRate * bytesPerSampleSlice;
      header[avg_bytes_per_second_ptr + 0] = (byte) ((abps) & 0xFF);
      header[avg_bytes_per_second_ptr + 1] = (byte) ((abps >> 8) & 0xFF);
      header[avg_bytes_per_second_ptr + 2] = (byte) ((abps >> 16) & 0xFF);
      header[avg_bytes_per_second_ptr + 3] = (byte) ((abps >> 24) & 0xFF);

      header[bytes_per_sample_ptr + 0] = (byte) ((bytesPerSampleSlice) & 0xFF);
      header[bytes_per_sample_ptr + 1] = (byte) ((bytesPerSampleSlice >> 8) & 0xFF);

      int sig_bits_per_sample = 8 * bytesPerSampleSlice / numChannels;
      header[sig_bits_per_sample_ptr + 0] = (byte) ((sig_bits_per_sample) & 0xFF);
      header[sig_bits_per_sample_ptr + 1] = (byte) ((sig_bits_per_sample >> 8) & 0xFF);

      return header;
   }

   private static final Logger logger = LoggerFactory.getLogger(AudioSaver.class);

}
