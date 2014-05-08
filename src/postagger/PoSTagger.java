package postagger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import perceptron.Perceptron;
import util.Instance;

public class PoSTagger {
	/**
	 * Arguments of this PoS tagger
	 * 	labels: set of labels, eg. NN, V, ...
	 *  featsname: defined names of features, eg. current, previous
	 */
	ArrayList<String> labels = new ArrayList<String>();
	ArrayList<String> featsname = new ArrayList<String>();;
	
	/**
	 * Information of test data
	 *  testInstances: eg. current__words__previous__preword__...
	 *  words: pure words eg. word
	 *  tags: results of PoS tags
	 */
	ArrayList<String> testInstances = new ArrayList<String>();
	ArrayList<String> words = new ArrayList<String>(); 
	ArrayList<String> tags = new ArrayList<String>();  
	
	
	public PoSTagger() {
		featsname.add("current");
		
		featsname.add("previous");
		featsname.add("fore-bigram");
		
		featsname.add("next");
		featsname.add("behind-bigram");
		
		featsname.add("prefix");
		featsname.add("sufix");
		featsname.add("length");
	}
	
	/**
	 * 
	 * @param trnflname training words data filename
	 * @param trnposchkflname training PoS Tags and Chunks filename
	 * @param testflname testing words data filename
	 * @return the tages in accordance to the test data
	 * @throws IOException
	 */
	public ArrayList<String> getTags(String trnflname, String trnposchkflname, 
			String testflname) throws IOException {
		// Train
		Perceptron p = train(trnflname, trnposchkflname);
		// Test to get the tags
		test(p, testflname);
		
		return tags;
	}
	
	
	/**
	 * 
	 * @param trnflname
	 * @param trnposchkflname
	 * @return a trained Perceptron Machine
	 * @throws IOException
	 */
	public Perceptron train(String trnflname, String trnposchkflname) throws IOException {
		ArrayList<String> labelsIns = new ArrayList<String>(); // 测试数据的labels实例集
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
		
		while((tmp = br1.readLine()) != null) {
			words.add(tmp);
			tmp = br2.readLine();
			String []str = tmp.split("	");
			labelsIns.add(str[0]);
			
			if(!labels.contains(str[0]) && !str[0].equals(""))
				labels.add(str[0]);
		}
		
		// Form feature&label
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
			
			//current word
			feats.add(words.get(i));
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
			// prefix
			char []ch = words.get(i).toCharArray();
			feats.add(String.valueOf(ch[0]));
			// sufix			
			feats.add(String.valueOf(ch[ch.length-1]));
			// length
			feats.add(String.valueOf(ch.length));
			
			
			// label
			feats.add(labelsIns.get(i));
			
			// Add into the list of training data
			feaLabels.add(instance.formFeats(featsname, feats));
		}		
		Perceptron p = new Perceptron(8, labels, null);
		
		p.trainMachine(feaLabels, 0.02);
		
		return p;
	}
	
	
	/**
	 * 
	 * @param p perceptron train from the training data
	 * @param testflname test data filename
	 * @throws IOException
	 */
	public void test(Perceptron p, String testflname) throws IOException {
		ArrayList<String> labelsIns = new ArrayList<String>(); // 测试数据的labels实例集(标准答案): NN, NR...
		
		// Read words
		BufferedReader br1 = new BufferedReader(new InputStreamReader(
				new FileInputStream(testflname),"utf-8"
				));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(
				new FileInputStream("./data/dev.pos-chk")
				));
		String tmp = null;
		while((tmp = br1.readLine()) != null) {
			words.add(tmp);
			tmp = br2.readLine();
			String []str = tmp.split("	");
			labelsIns.add(str[0]);
		}
		
		// Form feature
		Instance instance = new Instance();
		for(int i=0; i<words.size(); i++) {
			if(words.get(i).equals("")) {
				testInstances.add("");
				continue;
			}
			ArrayList<String> feats = new ArrayList<String>();
			// current word
			feats.add(words.get(i));
			/// previous words and fore-bigram
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
			// prefix
			char[] ch = words.get(i).toCharArray();
			feats.add(String.valueOf(ch[0]));
			// sufix			
			feats.add(String.valueOf(ch[ch.length-1]));
			// length
			feats.add(String.valueOf(ch.length));
			
			testInstances.add(instance.formFeats(featsname, feats));
		}
		
		// Get the tags and Normailiza the tags
		//int idxOfdata = 0;
		for(int i=0; i<testInstances.size(); i++) {// Notice the process of adding the tags with ""
			if(testInstances.get(i).equals("")) {
				tags.add("");
			//	idxOfdata ++;
				continue;
			}
			if(!testInstances.get(i).equals(""))
				tags.add(p.labelofInstance(testInstances.get(i), ""));
			//idxOfdata ++;
		}
		//for(int i=0; i<labelsIns.size(); i++)
		//	if(labelsIns.get(i).equals(""))
		//		labelsIns.remove(i);
		
		// !TEST!
		int cor = 0;
		FileWriter fileWriter = new FileWriter("./data/dev.pos");
		for(int i=0; i<tags.size(); i++) {
			fileWriter.write(tags.get(i)+"\n");
			//System.out.println(tags.get(i));
			fileWriter.flush();
			if(tags.get(i).equals(labelsIns.get(i)))
				cor ++;
		}
		br1.close();
		br2.close();
		fileWriter.close();
		System.out.println((double)cor/testInstances.size());
	}
}