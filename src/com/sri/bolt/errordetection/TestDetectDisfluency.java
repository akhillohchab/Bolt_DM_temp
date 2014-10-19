package com.sri.bolt.errordetection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import com.sri.bolt.FileIOUtil;
import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.BoolAttribute;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.SessionData.Builder;

import com.sri.interfaces.lang.Language;

/**
 * Class to do standalone test on the DetectDisfluency class
 * Testing could be run on plain text file
 * or SessionData file
 *
 */

public class TestDetectDisfluency {
    private static final String modelFileName = "models/EN.EditWord.lex4g.20130907.crf.model";
    
    public static void main(String[] args){
	String testFileName = args[0];

	try{
	    String asrOutput;
	    String ext = testFileName.substring(testFileName.lastIndexOf('.'));
	    
	    /*
	     * Test on SessionData file
	     */

	    if(ext.equals(".SessionData")) {
		File f = new File(testFileName);    
		byte[] rawData = FileIOUtil.loadFileData(f);
		if (rawData == null) {
		    System.err.println("Got null data for file: " + f);
		    System.exit(1);
		}
		
		SessionData message = SessionData.parseFrom(rawData);
		SessionData.Builder sessionDataBuilder = message.toBuilder();

		DetectDisfluency detector = new DetectDisfluency(modelFileName);
		
		SessionData updatedMessage = detector.process(message);
		
		String outFileName = testFileName + ".DetectDisfluency.SessionData";
		
		FileOutputStream output = new FileOutputStream(outFileName);
		updatedMessage.writeTo(output);
		output.close();
	    }
	    else {
		/*
		 * Test on plain text
		 */

		BufferedReader asrOutputReader = new BufferedReader(new FileReader(testFileName));

		DetectDisfluency detector = new DetectDisfluency(modelFileName);
		
		while((asrOutput = asrOutputReader.readLine()) != null) {
		    long startTime = System.currentTimeMillis();
		    HashMap<Integer, Integer> disfluencyWords = detector.process(asrOutput);
		    long endTime = System.currentTimeMillis();
		    long elapsedTime = endTime - startTime;
		    System.out.println("Elapsed time:" + elapsedTime + " msec\n");

		    String[] asrOutputWords = asrOutput.trim().split("\\s+");
		    System.out.print("ASR rescored-1best output: " + asrOutput + "\n");
		    System.out.print("DetectDisfluency output: ");
		    for (int i =0; i < asrOutputWords.length; i++){			
			System.out.print(asrOutputWords[i]);
			if(!disfluencyWords.isEmpty()
			   && disfluencyWords.containsKey(i)) {
			    if(disfluencyWords.get(i) == 0) {
				System.out.print("_FW");
			    }
			    else if(disfluencyWords.get(i) == 1) {
				System.out.print("_EW");
			    }
			}
			System.out.print(" "); 
		    }
		    System.out.println();
		    System.out.println();
		}
	    }
	}
	catch (Exception e){
	    e.printStackTrace();
	}
    }
}

