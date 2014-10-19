package com.sri.bolt.errordetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.*;
import java.util.logging.*;
import java.util.regex.Pattern;
import java.io.*;
import java.util.zip.*;
import java.lang.Math;

import cc.mallet.types.*;
import cc.mallet.fst.*;
import cc.mallet.optimize.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.pipe.tsf.*;
import cc.mallet.util.*;
import junit.framework.*;

import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.Sequence;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.LineGroupIterator;

import cc.mallet.util.CommandOption;
import cc.mallet.util.MalletLogger;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.BoolAttribute;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.SessionData.Builder;


/*
 * DetectDisfluency class
 */

public class DetectDisfluency
{
    private String modelFilePath;

    private static final double DEFAULT_DISFLUENCY_CONFIDENCE = 1.0;
    private static final double DEFAULT_DISFLUENCY_ERROR_SEGMENT_CONFIDENCE = 1.0;

    private static final int ngramOrder = 4;
    private static final String ngramDelimiter = "+";

    private Pipe p = null;
    private CRF crf = null;
    private TransducerEvaluator eval = null;

    // Regexp constant.                                                                        
    private static Pattern blankline = Pattern.compile("^\\s*$");

    // FillerWord table
    private static final String[] fillerWordTable = {"ah", "eh", "hm", "huh", "hum", "uh", "um"};

    // EditWord CRF model labels
    private static final String[] editWordLabels = {"B-E+IP", "B-E", "I-E+IP", "I-E"};
    

    private static final Map<String, Integer> englishFillerTable;
    
    static {
        HashMap<String, Integer> myMap = new HashMap<String, Integer>();
	for(int i = 0; i < fillerWordTable.length; i++) {
	    myMap.put(fillerWordTable[i], 1);
        }
	
        englishFillerTable = Collections.unmodifiableMap(myMap);
    }

    private static final Map<String, Integer> ewLabelMap;
    static {
	HashMap<String, Integer> myMap = new HashMap<String, Integer>();
	for(int i = 0; i < editWordLabels.length; i++) {
	    myMap.put(editWordLabels[i], 1);
	}
	ewLabelMap = Collections.unmodifiableMap(myMap);
    }

    public DetectDisfluency(String modelFileName)
    {
	init(modelFileName);
    }

    private void init(String modelFileName)
    {
	if (modelFileName != null) 
	    this.modelFilePath = modelFileName.trim();

	try {
	    // Load the CRF model 
	    FileInputStream fis = new FileInputStream(modelFilePath);
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    crf = (CRF) ois.readObject();
	    ois.close();
	    
	    // Look up the pipe used to generate feature vectors.                                                      
	    p = crf.getInputPipe();
	    
	    // The input file does not contain tags (aka targets).                                                     
	    p.setTargetProcessing(false);

	} 
	catch (Exception e){
	    e.printStackTrace();
	}
    }
    
    public HashMap<Integer, Integer> process(String originalInput)
    {
	if(originalInput == null)
	    return null;
	
	return detectEditWord(originalInput);
    }
    
    public SessionData process(SessionData sessionData) {
	UtteranceData utt = sessionData.getUtterances(sessionData.getCurrentTurn());
	String orgInput = utt.getRescored1Best();

	System.err.println("orgInput=" + orgInput + System.getProperty("line.separator"));

	HashMap<Integer, Integer> disfluencyWords = process(orgInput);
	
	/*
	 * No error segment creation in case no disfluency was detected
	 */

	if(disfluencyWords == null || disfluencyWords.isEmpty()) 
	    return sessionData;

	/*
	 * Create a separate error segment for each disfluency word
	 */

	SessionData.Builder sessionDataBuilder = sessionData.toBuilder();
	int currentTurn = sessionDataBuilder.getCurrentTurn();
	UtteranceData.Builder uttDataBuilder = sessionDataBuilder.getUtterancesBuilder(currentTurn);

	Iterator it = disfluencyWords.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry<Integer, Integer> pairs = (Map.Entry<Integer, Integer>)it.next();
            switch (pairs.getValue()) {
	    case 0:
	    {
		ErrorSegmentAnnotation segment = createErrorSegmentFillerWord(pairs.getKey());
                uttDataBuilder.addErrorSegments(segment);
		break;
            }
	    case 1:
	    {
		ErrorSegmentAnnotation segment = createErrorSegmentEditWord(pairs.getKey());
		uttDataBuilder.addErrorSegments(segment);
		break;
	    }
	    default:
		System.err.println("ERROR: unknown disfluency type\n");
	    }
	}

	uttDataBuilder.clearDmOutput();
	sessionDataBuilder.setUtterances(currentTurn, uttDataBuilder);
	return sessionDataBuilder.build();
    }

    private static Sequence[] apply(Transducer model, Sequence input, int k)
    {
	Sequence[] answers;
	if (k == 1) {
	    answers = new Sequence[1];
	    answers[0] = model.transduce (input);
	}
	else {
	    MaxLatticeDefault lattice =
		new MaxLatticeDefault (model, input, null, 100000);
	    
	    answers = lattice.bestOutputSequences(k).toArray(new Sequence[0]);
	}

	return answers;
    }

    /*
     * Function for detecting edit words in edit disfluencies
     * including repetitions, repairs and restarts
     */
    private HashMap<Integer,Integer> detectEditWord(String originalInput) {
 
        int numEvaluations = 0;
        int iterationsBetweenEvals = 16;

	HashMap<Integer, Integer> disfluencyWords = new HashMap<Integer, Integer>();

	/*
	 * Generate features for CRF tagging
	 */
	String features = generateSentenceFeatures(originalInput, disfluencyWords);

	InstanceList testData = null;

	testData = new InstanceList(p);
	StringReader testSentence = new StringReader(features);
	testData.addThruPipe(new LineGroupIterator(testSentence, Pattern.compile("^\\s*$"), true));
	
	if (p.isTargetProcessing())
	    {
		Alphabet targets = p.getTargetAlphabet();
		StringBuffer buf = new StringBuffer("Labels:");
		for (int i = 0; i < targets.size(); i++)
		    buf.append(" ").append(targets.lookupObject(i).toString());
		System.err.println("\nHere\n");
		System.err.println(buf.toString());
	    }
	
	for (int i = 0; i < testData.size(); i++)
	    {
		Sequence input = (Sequence)testData.get(i).getData();
		
		/*
		 * Apply the transducer
		 */

		Sequence[] outputs = apply(crf, input, 1);
		int k = outputs.length;
		boolean error = false;
		for (int a = 0; a < k; a++) {
		    if (outputs[a].size() != input.size()) {
			System.err.println("Failed to decode input sequence " + i + ", answer " + a);
			error = true;
		    }
		}
		if (!error) {
		    for (int j = 0; j < input.size(); j++)
			{
			    StringBuffer buf = new StringBuffer();
			    for (int a = 0; a < k; a++) 
				{
				    buf.append(outputs[a].get(j).toString()).append(" ");
				}
			    /*
			     * If the output label belongs to editWord labels,
			     * put the index into the editWord list
			     */
			    if(ewLabelMap.containsKey((buf.toString()).trim())) {
				//System.err.println("Word"+Integer.toString(j)+" " + buf.toString() + System.getProperty("line.separator"));
				disfluencyWords.put(j, 1);
			    }
			}
		}
	    }
	
	return disfluencyWords;
    }

    private ErrorSegmentAnnotation createErrorSegmentFillerWord(int index){
	BoolAttribute.Builder isFillerWord = BoolAttribute.newBuilder();
	isFillerWord.setValue(true);
	isFillerWord.setConfidence(DEFAULT_DISFLUENCY_CONFIDENCE);
	ErrorSegmentAnnotation.Builder segment = ErrorSegmentAnnotation.newBuilder();
	segment.setErrorType(ErrorSegmentType.ERROR_SEGMENT_DF);
	segment.setConfidence(DEFAULT_DISFLUENCY_ERROR_SEGMENT_CONFIDENCE);
	segment.setStartIndex(index);
	segment.setEndIndex(index);
	segment.setIsFillerWord(isFillerWord);
	return segment.build();
    }

    private ErrorSegmentAnnotation createErrorSegmentEditWord(int index){
        BoolAttribute.Builder isEditWord = BoolAttribute.newBuilder();
        isEditWord.setValue(true);
	isEditWord.setConfidence(DEFAULT_DISFLUENCY_CONFIDENCE);
	ErrorSegmentAnnotation.Builder segment = ErrorSegmentAnnotation.newBuilder();
        segment.setErrorType(ErrorSegmentType.ERROR_SEGMENT_DF);
        segment.setConfidence(DEFAULT_DISFLUENCY_ERROR_SEGMENT_CONFIDENCE);
        segment.setStartIndex(index);
        segment.setEndIndex(index);
        segment.setIsEditWord(isEditWord);
        return segment.build();
    }

    private String generateSentenceFeatures(String originalInput, HashMap<Integer, Integer> disfluencyWords) {
	String input = "<s> " + originalInput;

	String[] inputWords = input.split("\\s+");

	String features = "";
	String ngram = "";

	for(int i = 1; i < inputWords.length; i++) 
	    {
		if(englishFillerTable.containsKey(inputWords[i])) {
		    disfluencyWords.put(i-1, 0);
		}

		String thisFeature = "";

		int bound = Math.max(0, i - ngramOrder);
        
		for(int j = i; j >= bound; j--) {
		     ngram = inputWords[j];

		    for(int k = j+1; k <= i; k++) 
			{
			    ngram += (ngramDelimiter + inputWords[k]);
			}
		    
		    thisFeature += (" " + ngram);
		}

		features += (thisFeature + "\n");
	    }

        //System.err.println("\nFeatures\n" + features);

	return features;
    }

}
