package edu.columbia.bolt.confscorer;

import java.util.ArrayList;

import weka.core.*;

import edu.columbia.bolt.commondata.*;

/**
 * Builds an instances file usable by weka using a subset of significant features found for Utterances
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
 
 public class InstanceBuilderUTT implements InstanceBuilderWrapper {	 
	
	 private Instances m_Data = null;
	 
	public Instances buildInstance(SentenceData input){
		
				
		//set up a Vector able to hold the maximum number of significant features as attributes (max. 22 features as of 4/26/2012)
		FastVector attributes = new FastVector(22);		 
		
		//create all the attributes and add them to the vector
		
		 Attribute logconfidence = new Attribute ("logconfidence");// numeric\n";
		 attributes.addElement(logconfidence);
		 
		 		 
		 Attribute postagthis = new Attribute ("POSTAGTHIS"); 
		 attributes.addElement(postagthis);
		 
		 Attribute postagprev = new Attribute ("POSTAGPREV");
		 attributes.addElement(postagprev);
		 
		 Attribute postagnext = new Attribute ("POSTAGNEXT");
		 attributes.addElement(postagnext);
		 
		 Attribute hightagnext = new Attribute ("HIGHTAGNEXT");
		 attributes.addElement(hightagnext);
		 
		 
		 FastVector classValues = new FastVector(2);		//add the classification class
		 classValues.addElement("correct");
		 classValues.addElement("incorrect");
		 attributes.addElement(new Attribute("Class", classValues));
		//Attribute class {correct,incorrect}\n\n";
	     	     
	     String nameOfDataset = "UtteranceSet";
	     
	     m_Data = new Instances(nameOfDataset, attributes, 50);	// Create dataset with initial capacity of 50 words, 
	     m_Data.setClassIndex(m_Data.numAttributes() - 1);		// and set index of class.
	     
	     double confidence = 0;
	     Instance inst = new Instance(22);	     
	     //fill dataset with instances
	     for (int i = 0; i < input.getWordsCurrentUtt().size(); i++) {		
	    	    
	    	    confidence = confidence + input.getAsrConfidence().get(i);
	    	    inst.setValue(postagthis, input.getPostags().get(i));			
				
			}		     
	     
	     inst.setValue(logconfidence, confidence);
	     
	     m_Data.add(inst);
	     
	     
	     return m_Data;  
	  }
	
}