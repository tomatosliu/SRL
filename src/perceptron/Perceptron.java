/**
 * @project Semantic Role Labeling with beam search perceptron
 * @package perceptron
 * @author Siyuan Liu
 * @data May 8, 2014
 * @copyright 2014
 * @version 1.0
 */
package perceptron;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import util.Gen;

/**
 * @CLassname Perceptron
 * @author Siyuan Liu
 */

public class Perceptron {
	/* Templates of features */
	int sumoftemplates;
	int sumofbeam;
	/* Special labels for this perceptron machine */
	ArrayList<String> labels = new ArrayList<String>();
	/* 
	 * parameters for this perceptron machine 
	 * 	the first layer is for label
	 * 	the second layer is for template
	 * 	the third layer is for feature
	*/
	ArrayList<ArrayList<ArrayList<Double>>> param = new ArrayList<ArrayList<ArrayList<Double>>>();
	//ArrayList<ArrayList<String>> paramStr = new ArrayList<ArrayList<String>>();
	ArrayList<HashMap<String, Integer>> paramStr = new ArrayList<HashMap<String, Integer>>();
	/* Increment or Decrement Unit */
	final static double var = 1;
	/* Gen */
	Gen g;
	/**
	 * 
	 * @param sumoftemplates <Integer>sum of the templates for the perceptron
	 * @param labels <ArrayList<String>>labels used in this perceptron
	 */
	public Perceptron(int sumoftemplates, ArrayList<String> labels, Gen g) {
		// Default setting:
		//	The features are denoted in String
		this.g = g;
		this.sumoftemplates = sumoftemplates;
		this.sumofbeam = sumoftemplates/3;
		this.labels.addAll(labels);
		for(int i=0; i<labels.size(); i++) {
			// For each label
			param.add(new ArrayList<ArrayList<Double>>());
			
			for(int j=0; j<sumoftemplates; j++) {
				// For each template
				param.get(i).add(new ArrayList<Double>());
			}
		}
		
		for(int i=0; i<sumoftemplates; i++)
			paramStr.add(new HashMap<String, Integer>());
	}
	
	
	/**
	 * Train the machine adjusting the parameters
	 * @param feartureLabel: Array of Strings 
	 * 		eg. PoS tagging: current__w__prefix__pre__sufix__su
	 */
	public void trainMachine(ArrayList<String> featureLabel, double precision) {
		// Train the machine iteratively
		//	until the parameter do not change a lot
		
		// Parse each 'featureLabel' to locate the param.
		//	Then call 'trainIteration'
		while(true) {
			int error = trainIteration(featureLabel);
			System.out.println(error);
			if(((double)error) / featureLabel.size() <= precision)
				break;
		}
	}
	
	
	/**
	 * Each iteration on the training data
	 */
	public int trainIteration(ArrayList<String> featureLabel) {
		// For each iteration
		//	run the training process on the whole training data
		
		//	for each training data:
		//		if "", start to perceptron training 
		//		else, parse the featherlabels
		int error = 0;
		
		/**
		 * Information for one sentence
		 * 	label: standard labels for one sencetence
		 * 	features: features of one sencetence
		 */
		ArrayList<String> label = new ArrayList<String>();
		ArrayList<ArrayList<String>> features = new ArrayList<ArrayList<String>>();
		for(int i=0; i<featureLabel.size(); i++) {
			/**
			 * Preocess one sentence
			 */
			if(featureLabel.get(i).equals("")) {
				/**
				 * The format: 
				 *  String: label1__label2__label3__...labeln, n is sum words of a sentence
				 *  Double: the score of each beam
				*/
				HashMap<ArrayList<String>, Double> beamLabelsforEachData = new HashMap<ArrayList<String>, Double>();
				for(int idxofB=0; idxofB<label.size(); idxofB++) {
					// Form previous labels set
					
					// Form the beam
					// Find Top sumofbeam labels Seqs for cur label position
					//	sumofbeam*sumlabels will be inserted into beamLabelsMid
					//	Then find the Top sumofbeam
					HashMap<ArrayList<String>, Double> beamLabelsMid = new HashMap<ArrayList<String>, Double>();
					@SuppressWarnings("rawtypes")
					Iterator iter = beamLabelsforEachData.entrySet().iterator();
					
					while (iter.hasNext()) { 
					    @SuppressWarnings("rawtypes")
						Map.Entry entry = (Map.Entry) iter.next();
					    @SuppressWarnings("unchecked")
						ArrayList<String> cur_labelsSeq = (ArrayList<String>)(entry.getKey());
					    double cur_score = (Double)(entry.getValue());
					    
					    // Form previous labels
					    String pre_label = null;
						if(beamLabelsforEachData.isEmpty()) {
							pre_label = "";
						}
						else {
							pre_label = cur_labelsSeq.get(cur_labelsSeq.size()-1);
						}
					    ArrayList<String> genlabels = new ArrayList<String>();
					    // GEN
						if(g != null)
							genlabels = g.ChunkGen(labels, pre_label);
						else
							genlabels.addAll(labels);
						// Calculate the SCOREs and pick out the maximum
						for(int l=0; l<labels.size(); l++) {
							if(!genlabels.contains(labels.get(l)))
								continue;
							double SCORE = 0;
							for(int tplate=0; tplate<sumoftemplates; tplate++) {
								SCORE += param.get(l).get(tplate).get(
										paramStr.get(tplate).get(features.get(tplate))
										);
							}
							
							// Insert into 'beamLabelsMid'
							ArrayList<String> insertedFea = new ArrayList<String>();
							insertedFea.addAll(cur_labelsSeq);
							insertedFea.add(labels.get(l));
							beamLabelsMid.put(insertedFea, cur_score+SCORE);
						}
					}
					
					// Top sumofbeam Seq of features
					List<Map.Entry<String, Integer>> infoIds = new ArrayList<Map.Entry<String, Integer>>(
							(Collection<? extends Entry<String, Integer>>) beamLabelsMid.entrySet());
					Collections.sort(infoIds, new Comparator<Map.Entry<String, Integer>>() {   
					    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {      
					        //return (o2.getValue() - o1.getValue()); 
					        return (o1.getKey()).toString().compareTo(o2.getKey());
					    }
					}); 
					
					System.out.println();
				}
				
				label = new ArrayList<String>();
				features = new ArrayList<ArrayList<String>>();
			}
			/**
			 * Preprocessing
			 */
			// Split from the argument to get features and label
			String []str = featureLabel.get(i).split("__");
			ArrayList<String> feat = new ArrayList<String>();
			for(int j=0; j<sumoftemplates; j++)
				feat.add(str[j*2+1]);
			features.add(feat);
			label.add(str[sumoftemplates*2+1]);
			
			// Store the features and get the index of them
			for(int fea=0; fea<sumoftemplates; fea++) {
				// Check if the template[fea] contains the feature
				if(!paramStr.get(fea).containsKey(features.get(features.size()-1).get(fea))) {
					// Add new features into templates
					paramStr.get(fea).put(features.get(features.size()-1).get(fea), paramStr.get(fea).size());
					for(int l=0; l<labels.size(); l++)
						param.get(l).get(fea).add((double) 0);
				}
			}
		}
		return error;
	}
	
	
	/**
	 * 
	 */	
	public String labelofInstance(String feature, String pre_label) {
		// Split from the argument to get features and label
		String []str = feature.split("__");
		ArrayList<String> features = new ArrayList<String>();
		for(int j=0; j<sumoftemplates; j++)
			features.add(str[j*2+1]);
					
		// Calculate the SCOREs and pick out the maximum
		int indexofMaxLabel = 0;
		double tmpMaxSCORE = -1111111111;
		
		ArrayList<String> genlabels = new ArrayList<String>();
		if(g != null)
			genlabels = g.ChunkGen(labels, pre_label);
		else
			genlabels.addAll(labels);
		for(int l=0; l<labels.size(); l++) {
			if(!genlabels.contains(labels.get(l)))
				continue;
			double SCORE = 0;
			for(int tplate=0; tplate<sumoftemplates; tplate++) {
				if(paramStr.get(tplate).containsKey(features.get(tplate)))
					SCORE += param.get(l).get(tplate).get(
							paramStr.get(tplate).get(features.get(tplate))
							);
			}
						
			if(SCORE > tmpMaxSCORE) {
				tmpMaxSCORE = SCORE;
				indexofMaxLabel = l;
			}
			else if(SCORE <= -1111111111)
				System.out.println("Error SCORE!");
		}
		
		return labels.get(indexofMaxLabel);
	}
	
}
