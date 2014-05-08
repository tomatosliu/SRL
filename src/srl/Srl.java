package srl;

import java.io.BufferedReader;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;

import perceptron.Perceptron;
import util.Gen;

public class Srl {
	/* Tags used by this Srler: sum is 25*/
	public ArrayList<String> labels = new ArrayList<String>();
	
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
	ArrayList<String> tags = new ArrayList<String>();
	
	public Srl() {
		/**
		 * Accumulate feature names
		 */
		featsname.add("cur-phraseWords");
		featsname.add("cur-phrasePrefix");
		featsname.add("cur-phraseSufix");
		featsname.add("pre-phraseWords");
		featsname.add("fore-phraseWords-bigram");
		featsname.add("pre-phrasePrefix");
		featsname.add("pre-phraseSufix");
		featsname.add("next-phraseWords");
		featsname.add("behind-phraseWords-bigram");
		featsname.add("next-phrasePrefix");
		featsname.add("next-phraseSufix");
		
		featsname.add("cur-phrasePoSs");
		featsname.add("cur-phrasePoSPrefix");
		featsname.add("cur-phrasePoSSufix");
		featsname.add("pre-phrasePoSs");
		featsname.add("fore-phrasePoSs-bigram");
		featsname.add("pre-phrasePoSPrefix");
		featsname.add("pre-phrasePoSSufix");
		featsname.add("next-phrasePoSs");
		featsname.add("behind-phrasePoSs-bigram");
		featsname.add("next-phrasePoSPrefix");
		featsname.add("next-phrasePoSSufix");
				
		featsname.add("cur-phraseChunk");
		featsname.add("pre-phraseChunk");
		featsname.add("fore-phraseChunk-bigram");
		featsname.add("next-phraseChunk");
		featsname.add("behind-phraseChunk-bigram");
	
		featsname.add("PredictWord");
		featsname.add("PredictFrequence");
		featsname.add("Distence2Predict");
		featsname.add("PredictsInPath");
	}
	
	
	public ArrayList<String> getTags(String trnflname, String trnposchkflname, String trnpropsflname,
			String testflname) throws IOException {
		// Train
		Perceptron p = train(trnflname, trnposchkflname, trnpropsflname);
		// Test to get the tags
		test(p, testflname);
		return tags;
	}
	
	
	Perceptron train(String trnflname, String trnposchkflname, String trnpropsflname) throws IOException {
		/* IOB-Chunk label */
		ArrayList<String> IOBChunklabelsIns = new ArrayList<String>();
		/* PoS tag */
		ArrayList<String> PoSlabelsIns = new ArrayList<String>();
		/* Non-IOB-Chunk label */
		ArrayList<String> ChunklabelsIns = new ArrayList<String>();
		/* Exact words */
		ArrayList<String> words = new ArrayList<String>(); 
		
		/* feature&label Strings to Perceptron */
		ArrayList<String> feaLabels = new ArrayList<String>();
		
		// Read
		BufferedReader br1 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnflname),"utf-8"
				));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnposchkflname)
				));
		String tmp = null;
		
		boolean isIn = true;
		while((tmp = br1.readLine()) != null) {
			// words
			words.add(tmp);
			tmp = br2.readLine();
			// PoS and Chunk
			String []str = tmp.split("\t");
			String []ch = {"(", ")"};
			if(str.length == 1) {
				IOBChunklabelsIns.add("");
				ChunklabelsIns.add("");
			}
			else if(str[1].contains(ch[0])) {
				if(str[1].contains(ch[1]))
					isIn = false;
				else
					isIn = true;
				
				String tmpstr = new String(str[1].substring(1, str[1].indexOf("*")));
				IOBChunklabelsIns.add("B-"+tmpstr);
				ChunklabelsIns.add(tmpstr);
				
				//if(!Chunklabels.contains(tmpstr))
				//	Chunklabels.add(tmpstr);
			}
			else if(str[1].contains(ch[1])) {
				isIn = false;
				ChunklabelsIns.add(ChunklabelsIns.get(ChunklabelsIns.size()-1));
				IOBChunklabelsIns.add("I-"+ChunklabelsIns.get(ChunklabelsIns.size()-1));				
			}
			else if(isIn) {
				ChunklabelsIns.add(ChunklabelsIns.get(ChunklabelsIns.size()-1));
				IOBChunklabelsIns.add("I-"+ChunklabelsIns.get(ChunklabelsIns.size()-1));
			}
			else {
				IOBChunklabelsIns.add("O");
				ChunklabelsIns.add("O");
			}
			
			PoSlabelsIns.add(str[0]);	
		}
		br1.close();
		br2.close();
		
		// Chunk info
		ArrayList<ChunkInfo> chunkinfo = new ArrayList<ChunkInfo>();
		String pre_label = ChunklabelsIns.get(0);
		int startposition = 0;
		//int endposition = 0;
		for(int i=0; i<ChunklabelsIns.size(); i++) {
			if(ChunklabelsIns.get(i).equals("O") || 
					!ChunklabelsIns.get(i).equals(pre_label)) {
				// end a chunk
				chunkinfo.add(new ChunkInfo(pre_label, startposition, i-startposition));
				startposition = i;
				pre_label = ChunklabelsIns.get(i);
			}
		}
		//SRL
		BufferedReader br3 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnpropsflname),"utf-8"
				));
		
		
		br3.close();
		/**
		 * Tarin
		 */
		Perceptron p = new Perceptron(10, labels, new Gen());
		p.trainMachine(feaLabels, 0.02);
		
		return p;
	}
	
	
	void test(Perceptron p, String testflname) {
		
	}
	
	class ChunkInfo {
		String label;
		int startposition;
		int length;
		
		ChunkInfo(String label, int startposition, int length) {
			this.label = label;
			this.startposition = startposition;
			this.length = length;
		}
	}
}