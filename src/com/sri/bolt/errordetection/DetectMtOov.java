package com.sri.bolt.errordetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sri.bolt.message.BoltMessages;
import com.sri.bolt.message.BoltMessages.BoolAttribute;
import com.sri.bolt.message.BoltMessages.ErrorSegmentAnnotation;
import com.sri.bolt.message.BoltMessages.ErrorSegmentType;
import com.sri.bolt.message.BoltMessages.SessionData;
import com.sri.bolt.message.BoltMessages.UtteranceData;
import com.sri.bolt.message.BoltMessages.SessionData.Builder;

public class DetectMtOov {
	private static final String OOV_TAG_PREFIX = "__UNKNOWN:";
	private static Set<String> skipWords;
	
	private static final String ALIGNMENT_PAIR_DELIMETER = ",";
	private static final String ALIGNMENT_WORD_DELIMETER = "-";

	private static final double DEFAULT_MT_ERROR_SEGMENT_CONFIDENCE = 1.0;
	private static final double DEFAULT_OOV_CONFIDENCE = 1.0;
	
	private String originalInput;
	private String preprocessedInput;
	private String translation;
	private String alignment;
	private Map<Integer, Set<Integer>> srcIndexMap;
	private Map<Integer, List<Integer>> tgt2SrcAlignmentMap;
	
	static{
		skipWords = new HashSet<String>();
		skipWords.add("uh");
		skipWords.add("um");
		skipWords.add("uh-huh");
	}
	
	public DetectMtOov(){
		init(null, null, null, null);
	}
	
	public DetectMtOov(String originalInput, String preprocessedInput, String translation, String alignment){
		init(originalInput, preprocessedInput, translation, alignment);
	}
	
	private void init(String originalInput, String preprocessedInput, String translation, String alignment){
		if (originalInput != null)
			this.originalInput = originalInput.trim();
		
		if (preprocessedInput != null)
			this.preprocessedInput = preprocessedInput.trim();

		this.srcIndexMap = new HashMap<Integer, Set<Integer>>();
		if (originalInput != null && preprocessedInput != null)
			this.srcIndexMap = ModifiedEditDistance.getSecondStringToFirstStringMapping(originalInput, preprocessedInput);

		if (translation != null)
			this.translation = translation.trim();

		this.tgt2SrcAlignmentMap = new HashMap<Integer, List<Integer>>();
		if (alignment != null){
			this.alignment = alignment.trim();
			processAlignmentString();
		}
	}
		
	private void processAlignmentString(){
		if (alignment!= null && !(alignment.isEmpty())){
			String[] pairs = alignment.trim().split(ALIGNMENT_PAIR_DELIMETER);
			for (String pair : pairs){
				String[] indices = pair.split(ALIGNMENT_WORD_DELIMETER);
				int srcIndex = Integer.parseInt(indices[0]);
				int tgtIndex = Integer.parseInt(indices[1]);
				List<Integer> alist= tgt2SrcAlignmentMap.get(tgtIndex);
				if (alist == null){
					alist = new ArrayList<Integer>();
					tgt2SrcAlignmentMap.put(tgtIndex, alist);
				}
				alist.add(srcIndex);
			}
		}
	}
	
	boolean oovWordExists(){
		String[] words = translation.split("\\s+");
		for (String word : words){
			if (word.startsWith(OOV_TAG_PREFIX)){
				String srcWord = word.substring(OOV_TAG_PREFIX.length());
				if (!(skipWords.contains(srcWord)))
					return true;
			}
		}
		return false;
	}
	
	public List<Integer> process(){
		if (originalInput == null || preprocessedInput == null || translation == null)
			return null;
		return getOovIndices();
	}
	
	public SessionData process(SessionData sessionData){
		UtteranceData utt = sessionData.getUtterances(sessionData.getCurrentTurn());
		String orgInput = utt.getMtData().getOriginalInput();
		String mtInput = utt.getMtData().getPreprocessedInput();
		String trans = null;
		if (utt.getMtData().getOriginalTranslationsCount() > 0)
			trans = utt.getMtData().getOriginalTranslations(0);

		String align = null;
		if (utt.getMtData().getAlignmentsCount() > 0)
			align = utt.getMtData().getAlignments(0);
		
		init(orgInput, mtInput, trans, align);
		
		// create a separate error segment for each oov word
		List<Integer> oovIndices = process();
		if (oovIndices == null || oovIndices.isEmpty())
			return sessionData;
		
		SessionData.Builder sessionDataBuilder = sessionData.toBuilder();
		int currentTurn = sessionDataBuilder.getCurrentTurn();
		UtteranceData.Builder uttDataBuilder = sessionDataBuilder.getUtterancesBuilder(currentTurn);
		
		for (int srcIndex : oovIndices){
			ErrorSegmentAnnotation segment = createErrorSegment(srcIndex);
			uttDataBuilder.addErrorSegments(segment);
		}
		uttDataBuilder.clearDmOutput();
		uttDataBuilder.clearMtData();
		sessionDataBuilder.setUtterances(currentTurn, uttDataBuilder);
		return sessionDataBuilder.build();		
	}
	
	private List<Integer> getOovIndices(){
		if (!oovWordExists())
			return null;
		List<Integer> oovIndices = new ArrayList<Integer>();
		String[] translationWords = translation.split("\\s+");
		String[] mtInputWords = preprocessedInput.split("\\s+");
		for (int trnsIndex=0; trnsIndex < translationWords.length; trnsIndex++){
			String trnsWord = translationWords[trnsIndex];
			if (trnsWord.startsWith(OOV_TAG_PREFIX)){
				String srcWord = trnsWord.substring(OOV_TAG_PREFIX.length());
				if (skipWords.contains(srcWord))
					continue;	
				int srcIndex = -1;
				List<Integer> alist = tgt2SrcAlignmentMap.get(trnsIndex);
				if (alist != null && !(alist.isEmpty())){
					// the list can have only one item for OOV words
					srcIndex = alist.get(0);
				}
				else{
					srcIndex = findFirstOccurrence(mtInputWords, srcWord);
				}
				if (srcIndex != -1){
					Set<Integer> mappedIndices = srcIndexMap.get(srcIndex);
					if (mappedIndices != null){
						for (int newIndex : mappedIndices){
							oovIndices.add(newIndex);
						}
					}					
				}
			}
		}
		return oovIndices;
	}
	
	private int findFirstOccurrence(String[] words, String term){
		for (int i=0; i < words.length; i++){
			if (words[i].equals(term))
				return i;
		}
		return -1;
	}
	
	private ErrorSegmentAnnotation createErrorSegment(int index){
		BoolAttribute.Builder isOov = BoolAttribute.newBuilder();
		isOov.setValue(true);
		isOov.setConfidence(DEFAULT_OOV_CONFIDENCE);
		ErrorSegmentAnnotation.Builder segment = ErrorSegmentAnnotation.newBuilder();
		segment.setErrorType(ErrorSegmentType.ERROR_SEGMENT_MT);
		segment.setConfidence(DEFAULT_MT_ERROR_SEGMENT_CONFIDENCE);
		segment.setStartIndex(index);
		segment.setEndIndex(index);
		segment.setIsMtOov(isOov);
		// may need to add other fields later
		return segment.build();
	}
	
}
