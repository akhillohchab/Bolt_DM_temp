package edu.columbia.bolt.commondata;

import java.util.List;
import java.util.ArrayList;


/**
 * Container holding and managing the prosodic features
 * 
 * @author Philipp Salletmayr
 *
 */
public class Prosodics {
	
	//maximum pitch features for each word
	List<Double> f0max;
	//minimum pitch features for each word
	List<Double> f0min;
	//mean pitch features for each word
	List<Double> f0mean;
	//standard deviation of pitch features for each word
	List<Double> f0stdev;
	//maximum energy features for each word
	List<Double> engmax;
	//minimum energy features for each word
	List<Double> engmin;
	//mean energy features for each word
	List<Double> engmean;
	//standard deviation of energy features for each word
	List<Double> engstdev;
	//Ratio of voiced frames to total frames
	List<Double> vcd2tot;
	
	public Prosodics(){
		f0max = new ArrayList<Double>();
		f0min = new ArrayList<Double>();
		f0mean = new ArrayList<Double>();
		f0stdev = new ArrayList<Double>();
		engmax = new ArrayList<Double>();
		engmin = new ArrayList<Double>();
		engmean = new ArrayList<Double>();
		engstdev = new ArrayList<Double>();
		vcd2tot = new ArrayList<Double>();
	}
	
	 /**
	  * Overloaded constructor to allow for construction with both Lists (for words) and single values (for utterance) 
	  *
	  */
	public void setProsodicsForWords(List<Double> f0max, List<Double> f0min, List<Double> f0mean, List<Double> f0stdev,
			 List<Double> engmax, List<Double> engmin, List<Double> engmean, List<Double> engstdev, List<Double> vcd2tot) {
				
			this.f0max = f0max;
			this.f0min = f0min;
			this.f0mean = f0mean;
			this.f0stdev = f0stdev;
			this.engmax = engmax;
			this.engmin = engmin;
			this.engmean = engmean;
			this.engstdev = engstdev;
			this.vcd2tot = vcd2tot;
		}
	
	public void setProsodicsForUtterance(Double f0max, Double f0min, Double f0mean, Double f0stdev,
			Double engmax, Double engmin, Double engmean, Double engstdev, Double vcd2tot) {
				
			this.f0max.add(f0max);
			this.f0min.add(f0min);
			this.f0mean.add(f0mean);
			this.f0stdev.add(f0stdev);
			this.engmax.add(engmax);
			this.engmin.add(engmin);
			this.engmean.add(engmean);
			this.engstdev.add(engstdev);
			this.vcd2tot.add(vcd2tot);
		}

	public List<Double> getF0max() {
			return f0max;
		}

	public List<Double> getF0min() {
			return f0min;
	}

	public List<Double> getF0mean() {
			return f0mean;
	}
	
	public List<Double> getF0stdev() {
		return f0stdev;
}

	public List<Double> getEngmax() {
			return engmax;
	}

	public List<Double> getEngmin() {
			return engmin;
	}

	public List<Double> getEngmean() {
			return engmean;
	}

	public List<Double> getEngstdev() {
			return engstdev;
	}
	
	public List<Double> getVcd2tot() {
		return vcd2tot;
	}

}
