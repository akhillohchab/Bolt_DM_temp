package edu.columbia.bolt.confscorer;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializedObject;

/**
 * Prosidic Scorer
 * Classifies an utterance or single word given a set of prosodic features.
 * Returns classification and confidence level.
 * 
 * @author Philipp Salletmayr (phisa@student.tugraz.at)
 *
 */
 
 public class ProsodicScorer {
	 
	 private Classifier m_classifier;
	 
	  /**
	  * 
	  * Creates a prosodic scorer with the given classifier.
	  * 
	  * @param String modelpath 
	  * Path to the classifier model file to be used by the scorer.
	  *
	  */
	 	
	 public ProsodicScorer (String modelpath) throws Exception{
		 
		//load classifier
		 this.m_classifier = (Classifier) weka.core.SerializationHelper.read(modelpath);
		     
	 }
	
	 /**
	  * Classifies the given dataset. Dataset has to be in .arff file format.
	  * 
	  * @param Instances data 
	  * Dataset containing instance to be classified
	  * 
	  * @param int instanceid
	  * Instance ID to be classified, if no ID is provided the first instance of the dataset 
	  * will be classified
	  * 
	  * @return
	  * The probabilities for the instance being correct or incorrect
	  *
	  */
	public double[] classify(Instances data, int instanceid) throws Exception{
		
		data.setClassIndex(data.numAttributes()-1);
		
		//get confidence score
		return m_classifier.distributionForInstance(data.instance(instanceid));
		
	}
	
	public double[] classify(Instances data) throws Exception{
		
		data.setClassIndex(data.numAttributes()-1);
		
		//get confidence score
		return m_classifier.distributionForInstance(data.instance(0));
		
	}
	
	
}
