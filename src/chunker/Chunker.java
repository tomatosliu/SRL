package chunker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import perceptron.Perceptron;
import util.Gen;
import util.Instance;

public class Chunker {
	/* Tags used by this Chunker: sum is 25*/
	public ArrayList<String> labels = new ArrayList<String>();
	/* PoS tags appearing in the data*/
	//public ArrayList<String> PoSlabels = new ArrayList<String>();
	//public ArrayList<String> Chunklabels = new ArrayList<String>();
	/* Features names of one word */
	public ArrayList<String> featsname = new ArrayList<String>();
	
	/**
	 * Information of test data
	 *  testInstances: eg. current__words__previous__preword__...
	 *  words: pure words eg. word
	 *  tags: results of PoS tags
	 */
	ArrayList<String> testInstances = new ArrayList<String>();
	ArrayList<String> words = new ArrayList<String>(); 
	public ArrayList<String> tags = new ArrayList<String>();
	
	public Chunker(){
		labels.add("B-NP");
		labels.add("I-NP");
		
		labels.add("B-VP");
		labels.add("I-VP");
		
		labels.add("B-QP");
		labels.add("I-QP");
		
		labels.add("B-LCP");
		labels.add("I-LCP");
		
		labels.add("B-PP");
		labels.add("I-PP");
		
		labels.add("B-ADJP");
		labels.add("I-ADJP");
		
		labels.add("B-DNP");
		labels.add("I-DNP");
		
		labels.add("B-DP");
		labels.add("I-DP");
		
		labels.add("B-ADVP");
		labels.add("I-ADVP");
		
		labels.add("B-DVP");
		labels.add("I-DVP");
		
		labels.add("B-CLP");
		labels.add("I-CLP");
		
		labels.add("B-LST");
		labels.add("I-LST");
		
		labels.add("O");
		
		/**
		 * Accumulate feature names
		 */
		
		featsname.add("current");
		featsname.add("cur-PoSTag");
		
		featsname.add("previous");
		featsname.add("fore-bigram");
		
		featsname.add("next");
		featsname.add("behind-bigram");
		
		featsname.add("pre-PoSTag");
		featsname.add("fore-biPosTag");
		featsname.add("next-PoSTag");
		featsname.add("behind-biPosTag");
		// new
		featsname.add("previous2");
		featsname.add("fore2-bigram");
		featsname.add("pre2-PoSTag");
		featsname.add("fore2-biPosTag");
		
		featsname.add("next2");
		featsname.add("behind2-bigram");
		featsname.add("next2-PoSTag");
		featsname.add("behind2-biPosTag");
	}
	
	public ArrayList<String> getTags(String trnflname, String trnposchkflname, 
			String testflname, String testposflname) throws IOException {
		// Train
		Perceptron p = train(trnflname, trnposchkflname);
		// Test to get the tags
		test(p, testflname, testposflname);
		
		return tags;
	}
	
	public Perceptron train(String trnflname, String trnposchkflname) throws IOException {
		ArrayList<String> labelsIns = new ArrayList<String>(); // 测试数据的labels实例集
		ArrayList<String> PoSlabelsIns = new ArrayList<String>();
		ArrayList<String> ChunklabelsIns = new ArrayList<String>();
		
		ArrayList<String> words = new ArrayList<String>(); //测试数据的labels实例化
		ArrayList<String> feaLabels = new ArrayList<String>(); //测试数据的feature&labels
		
		
		// Read
		@SuppressWarnings("resource")
		BufferedReader br1 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnflname),"utf-8"
				));
		@SuppressWarnings("resource")
		BufferedReader br2 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnposchkflname)
				));
		String tmp = null;
		
		boolean isIn = true;
		while((tmp = br1.readLine()) != null) {
			words.add(tmp);
			tmp = br2.readLine();
			String []str = tmp.split("\t");
			String []ch = {"(", ")"};
			if(str.length == 1) {
				labelsIns.add("");
				ChunklabelsIns.add("");
			}
			else if(str[1].contains(ch[0])) {
				if(str[1].contains(ch[1]))
					isIn = false;
				else
					isIn = true;
				
				String tmpstr = new String(str[1].substring(1, str[1].indexOf("*")));
				labelsIns.add("B-"+tmpstr);
				ChunklabelsIns.add(tmpstr);
				
				//if(!Chunklabels.contains(tmpstr))
				//	Chunklabels.add(tmpstr);
			}
			else if(str[1].contains(ch[1])) {
				isIn = false;
				ChunklabelsIns.add(ChunklabelsIns.get(ChunklabelsIns.size()-1));
				labelsIns.add("I-"+ChunklabelsIns.get(ChunklabelsIns.size()-1));				
			}
			else if(isIn) {
				ChunklabelsIns.add(ChunklabelsIns.get(ChunklabelsIns.size()-1));
				labelsIns.add("I-"+ChunklabelsIns.get(ChunklabelsIns.size()-1));
			}
			else {
				labelsIns.add("O");
				ChunklabelsIns.add("O");
			}
			
			PoSlabelsIns.add(str[0]);
			
			//if(!PoSlabels.contains(str[0]) && !str[0].equals(""))
			//	PoSlabels.add(str[0]);
		}
		
		//Chunklabels.add("*");
		
		Instance instance = new Instance();
		for(int i=0; i<words.size(); i++) {
			if(words.get(i).equals("")) {
				feaLabels.add("");
				continue;
			}
			ArrayList<String> feats = new ArrayList<String>();
			ArrayList<String> featsname = new ArrayList<String>();
			featsname.addAll(this.featsname);
			featsname.add("label");
			/**
			 * Form the features String 
			 */
			//current word and PoS tag
			feats.add(words.get(i));
			feats.add(PoSlabelsIns.get(i));
			// previous words and fore-bigram
			if(i == 0 || words.get(i-1).equals("")) {
				feats.add("#");
				feats.add("#"+words.get(i));
			}
			else {
				feats.add(words.get(i-1));
				feats.add(words.get(i-1)+words.get(i));
			}
			// next word and behind-bigram
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add(words.get(i)+"$");
			}
			else {
				feats.add(words.get(i+1));
				feats.add(words.get(i)+words.get(i+1));
			}
			
			// previous word's PoS tag and fore-biPoSTag
			if(i == 0 || PoSlabelsIns.get(i-1).equals("")) {
				feats.add("#");
				feats.add("#"+PoSlabelsIns.get(i));
			}
			else {
				feats.add(PoSlabelsIns.get(i-1));
				feats.add(PoSlabelsIns.get(i-1)+PoSlabelsIns.get(i));
			}
			
			// next word's PoS tag and behind-biPoSTag
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add(PoSlabelsIns.get(i)+"$");
			}
			else {
				feats.add(PoSlabelsIns.get(i+1));
				feats.add(PoSlabelsIns.get(i)+PoSlabelsIns.get(i+1));
			}

			// previous2 fore2-bigram pre2-PoSTag fore2-biPosTag
			if(i == 0 || words.get(i-1).equals("")) {// first word in a sentence
				feats.add("#");
				feats.add("##");
				feats.add("#");
				feats.add("##");
			}
			else if(i == 1 || words.get(i-2).equals("")) {// second word in a sentence
				feats.add("#");
				feats.add("#" + words.get(i-1));
				feats.add("#");
				feats.add("#"+PoSlabelsIns.get(i-1));
			}
			else {
				feats.add(words.get(i-2));
				feats.add(words.get(i-2) + words.get(i-1));
				feats.add(PoSlabelsIns.get(i-2));
				feats.add(PoSlabelsIns.get(i-2)+PoSlabelsIns.get(i-1));
			}
			// next2 behind2-bigram next2-PoSTag behind2-biPosTag
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add("$$");
				feats.add("$");
				feats.add("$$");
			}
			else if(i == words.size()-2 || words.get(i+2).equals("")) {
				feats.add("$");
				feats.add(words.get(i+1)+"$");
				feats.add("$");
				feats.add(PoSlabelsIns.get(i+1)+"$");
			}
			else {
				feats.add(words.get(i+2));
				feats.add(words.get(i+1)+words.get(i+2));
				feats.add(PoSlabelsIns.get(i+2));
				feats.add(PoSlabelsIns.get(i+1)+PoSlabelsIns.get(i+2));
			}
			
			
			/**
			 *  label
			 */
			feats.add(labelsIns.get(i));
			
			// Add into the list of training data
			feaLabels.add(instance.formFeats(featsname, feats));
		}	
		
		/**
		 * Tarin
		 */
		Perceptron p = new Perceptron(18, labels, new Gen());
		p.trainMachine(feaLabels, 0.01);
		
		return p;
	}
	
	
	public void test(Perceptron p, String testflname, String testposflname) throws IOException {
		ArrayList<String> ChunklabelsIns = new ArrayList<String>();
		ArrayList<String> PoSlabelsIns = new ArrayList<String>();

		// Read words
		BufferedReader br1 = new BufferedReader(new InputStreamReader(
				new FileInputStream(testflname),"utf-8"
				));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(
				new FileInputStream(testposflname)
				));
		BufferedReader br4 = new BufferedReader(new InputStreamReader(
				new FileInputStream("./data/dev.pos-chk")
				));
		String tmp = null;
		while((tmp = br1.readLine()) != null) {
			words.add(tmp);
			tmp = br2.readLine();
			PoSlabelsIns.add(tmp);
			
			tmp = br4.readLine();
			String []str = tmp.split("\t");
			if(str.length == 1)
				ChunklabelsIns.add(str[0]);
			else
				ChunklabelsIns.add(str[1]);
		}
		br1.close();
		br2.close();
		br4.close();
		// Form feature
		Instance instance = new Instance();
		for(int i=0; i<words.size(); i++) {
			if(words.get(i).equals("")) {
				testInstances.add("");
				continue;
			}
			ArrayList<String> feats = new ArrayList<String>();
			
			/**
			 * Form the features String
			 */
			//current word and PoS tag
			feats.add(words.get(i));
			feats.add(PoSlabelsIns.get(i));
			// previous words and fore-bigram
			if(i == 0 || words.get(i-1).equals("")) {
				feats.add("#");
				feats.add("#"+words.get(i));
			}
			else {
				feats.add(words.get(i-1));
				feats.add(words.get(i-1)+words.get(i));
			}
			// next word and behind-bigram
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add(words.get(i)+"$");
			}
			else {
				feats.add(words.get(i+1));
				feats.add(words.get(i)+words.get(i+1));
			}	
			// previous word's PoS tag and fore-biPoSTag
			if(i == 0 || PoSlabelsIns.get(i-1).equals("")) {
				feats.add("#");
				feats.add("#"+PoSlabelsIns.get(i));
			}
			else {
				feats.add(PoSlabelsIns.get(i-1));
				feats.add(PoSlabelsIns.get(i-1)+PoSlabelsIns.get(i));
			}
			
			// next word's PoS tag and behind-biPoSTag
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add(PoSlabelsIns.get(i)+"$");
			}
			else {
				feats.add(PoSlabelsIns.get(i+1));
				feats.add(PoSlabelsIns.get(i)+PoSlabelsIns.get(i+1));
			}
			
			// previous2 fore2-bigram pre2-PoSTag fore2-biPosTag
			if(i == 0 || words.get(i-1).equals("")) {// first word in a sentence
				feats.add("#");
				feats.add("##");
				feats.add("#");
				feats.add("##");
			}
			else if(i == 1 || words.get(i-2).equals("")) {// second word in a sentence
				feats.add("#");
				feats.add("#" + words.get(i-1));
				feats.add("#");
				feats.add("#"+PoSlabelsIns.get(i-1));
			}
			else {
				feats.add(words.get(i-2));
				feats.add(words.get(i-2) + words.get(i-1));
				feats.add(PoSlabelsIns.get(i-2));
				feats.add(PoSlabelsIns.get(i-2)+PoSlabelsIns.get(i-1));
			}
			// next2 behind2-bigram next2-PoSTag behind2-biPosTag
			if(i == words.size()-1 || words.get(i+1).equals("")) {
				feats.add("$");
				feats.add("$$");
				feats.add("$");
				feats.add("$$");
			}
			else if(i == words.size()-2 || words.get(i+2).equals("")) {
				feats.add("$");
				feats.add(words.get(i+1)+"$");
				feats.add("$");
				feats.add(PoSlabelsIns.get(i+1)+"$");
			}
			else {
				feats.add(words.get(i+2));
				feats.add(words.get(i+1)+words.get(i+2));
				feats.add(PoSlabelsIns.get(i+2));
				feats.add(PoSlabelsIns.get(i+1)+PoSlabelsIns.get(i+2));
			}
						
			testInstances.add(instance.formFeats(featsname, feats));
		}
		
		/**
		 *  Get the tags and Normailiza the tags
		 */
		for(int i=0; i<testInstances.size(); i++) {
			if(testInstances.get(i).equals("")) {
				tags.add("");
				continue;
			}
			
			if(!testInstances.get(i).equals("")) {
				if(i == 0)
					tags.add(p.labelofInstance(testInstances.get(i), ""));
				else
					tags.add(p.labelofInstance(testInstances.get(i), tags.get(i-1)));
				
			}
		}
		
		// !TEST!
		for(int i=0; i<tags.size(); i++) {
			if(tags.get(i).equals(""))
				continue;
			String []str = tags.get(i).split("-");
			if(str[0].equals("B")) {
				//System.out.println(tags.get(i+1));
				if(i != tags.size()-1 && !tags.get(i+1).equals("") &&
						tags.get(i+1).substring(0,1).equals("I") )
					tags.set(i, "(" + str[1] + "*");
				else
					tags.set(i, "(" + str[1] + "*" 
							+ str[1] + ")");
			}
			else if(str[0].equals("I")) {
				if(i != tags.size()-1 && !tags.get(i+1).equals("")
						&& tags.get(i+1).substring(0,1).equals("I"))
					tags.set(i, "*");
				else
					tags.set(i, "*" + str[1] + ")");
			}
			else 
				tags.set(i, "*");
		}
		int cor = 0;
		FileWriter fileWriter = new FileWriter("./data/dev.chk");
		for(int i=0; i<tags.size(); i++) {
			if(tags.get(i).equals(""))
				fileWriter.write("\n");
			else
				fileWriter.write(PoSlabelsIns.get(i)+"\t"+tags.get(i) + "\n");
			if(tags.get(i).equals(ChunklabelsIns.get(i)))
				cor ++;
		}
		fileWriter.close();
		System.out.println((double)cor/tags.size());
	}
}