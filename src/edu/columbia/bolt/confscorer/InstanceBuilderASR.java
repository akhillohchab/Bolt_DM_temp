package edu.columbia.bolt.confscorer;

import java.lang.Math.*;
import java.util.Arrays;
import java.util.ArrayList;

import weka.core.*;

import edu.columbia.bolt.commondata.*;

/**
 * Builds an instances file usable by weka using a subset of features:
 * ASR confidences
 * 
 * Instances are built in the .arff file convention with a header defining
 * features and their place in the dataset.
 * 
 * minimum header:
 * \@attribute asrconfidence numeric
 * \@attribute sometag string
 * \@attribute class {correct, incorrect}
 * \@data
 * 
 * The actual data is then added in the Instance format i.e.:
 * 
 * 1.0,mytag
 * 
 * 
 * @author Philipp Salletmayr
 *
 */
 
 public class InstanceBuilderASR implements InstanceBuilderWrapper {	 
	
	 private Instances m_Data = null;
	 
	
		
	public Instances buildInstance(SentenceData input){
		
		//set up and include all possible values for the nominal POSTAG attributes (-THIS, -PREV, -NEXT)
		FastVector pos = new FastVector(80);
		for(int i=0; i < m_stanford_postags.length; i++){
			pos.addElement(m_stanford_postags[i]);
		}
		
		/*FastVector oov = new FastVector(3);
		oov.addElement("OOV");
		oov.addElement("NOOOV");
		oov.addElement("Null");
		
		Attribute oovtagprev = new Attribute ("oovtagprev", oov);//{FNC,CNT,NULL}\n";
		 attributes.addElement(oovtagprev);
		 
		 Attribute oovtagthis = new Attribute ("oovtagthis", oov);//{FNC,CNT,NULL}\n";
		 attributes.addElement(oovtagthis);
		 
		 Attribute oovtagnext = new Attribute ("oovtagnext", oov);//{FNC,CNT,NULL}\n";
		 attributes.addElement(oovtagnext);
		 
		 //Set OOV Tags	    	    
	    	    if(i > 0){
	    	    	inst.setValue(oovtagprev, input.getOovTags().get(i-1));	    	    	
	    	    }else{
	    	    	inst.setValue(oovtagprev, "Null");
	    	    }
	    	    
	    	    
    	    	inst.setValue(oovtagthis, input.getOovTags().get(i));
    	    	
	    	    
	    	    if(i < input.getWords().size()-1){	    	    	
	    	    	inst.setValue(oovtagnext, input.getOovTags().get(i+1));	    	    	
	    	    }else{
	    	    	inst.setValue(oovtagnext, "Null");
	    	    }	   
		*/
		
		//set up a Vector able to hold the maximum number of significant features as attributes (max. 22 features as of 4/26/2012)
		FastVector attributes = new FastVector(22);		 
		
		
		
		
		 Attribute logasrconfidence = new Attribute ("logasrconfidence");// numeric\n";
		 attributes.addElement(logasrconfidence);
		 
		 Attribute logasrconfidenceavg3 = new Attribute ("logasrconfidenceavg3");// numeric\n";
		 attributes.addElement(logasrconfidenceavg3);
		 
		 FastVector classValues = new FastVector(2);		//add the classification class
		 classValues.addElement("correct");
		 classValues.addElement("incorrect");
		 attributes.addElement(new Attribute("Class", classValues));
		//Attribute class {correct,incorrect}\n\n";
	     	     
	     String nameOfDataset = "UtteranceSet";
	     
	     m_Data = new Instances(nameOfDataset, attributes, 0);	// Create dataset with initial capacity of 50 words, 
	     m_Data.setClassIndex(m_Data.numAttributes() - 1);		// and set index of class.
	     
	     //fill dataset with instances
	     System.out.println("filling datasetPROSPOS");
	     for (int i = 0; i < input.getWordsCurrentUtt().size(); i++) {		
	    	 Instance inst = new Instance(22);
	    	    double avg;
	    	    
	    	    //set ASR confidence	    	    
	    	    inst.setValue(logasrconfidence, Math.log10(input.getAsrConfidence().get(i)));
	    	    
	    	    if(i > 0 && i < input.getWordsCurrentUtt().size()-1){
	    	    	//compute average over 3 asr confidence values
	    	    	avg = Math.log10(input.getAsrConfidence().get(i));
	    	    	avg = avg + Math.log10(input.getAsrConfidence().get(i-1));
	    	    	avg = avg + Math.log10(input.getAsrConfidence().get(i+1));
	    	    	avg = avg/3;
	    	    	inst.setValue(logasrconfidenceavg3, avg);
	    	    }
	    	    else if(i > 0) {
	    	    	//compute average over 2 asr confidence values for last word
	    	    	avg = Math.log10(input.getAsrConfidence().get(i));
	    	    	avg = avg + Math.log10(input.getAsrConfidence().get(i-1));
	    	    	avg = avg/2;
	    	    	inst.setValue(logasrconfidenceavg3, avg);
	    	    }
	    	    else if(i < input.getWordsCurrentUtt().size()-1){
	    	    	//compute average over 2 asr confidence values for first word
	    	    	avg = Math.log10(input.getAsrConfidence().get(i));
	    	    	avg = avg + Math.log10(input.getAsrConfidence().get(i+1));
	    	    	avg = avg/2;
	    	    	inst.setValue(logasrconfidenceavg3, avg);
	    	    }
	    	    
	    	    
				m_Data.add(inst);
			}		     
	     
	     return m_Data;		
	}	
}