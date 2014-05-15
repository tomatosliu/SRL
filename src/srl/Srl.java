package srl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import perceptron.Perceptron;
import util.Gen;
import util.Instance;

public class Srl {
	/* Tags used by this Srler: sum is 30*/
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
	
	/**
	 * Const
	 */
	String PredictLabel = "(V*)";
	String Predictfeat = "IsPredict";
	
	public Srl() {
		labels.add("O");
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
		// new
		featsname.add("predict-pos");
		
		//featsname.add("PredictFrequence");
		featsname.add("Distence2Predict");
		featsname.add("relativePosition");
		//featsname.add("PredictsInPath");
		featsname.add("hasPredict");
		
		// new
		featsname.add("predict-preword");
		featsname.add("predict-prepos");
		featsname.add("predict-nextword");
		featsname.add("predict-nextpos");
		featsname.add("predict-pre2word");
		featsname.add("predict-pre2pos");
		featsname.add("predict-next2word");
		featsname.add("predict-next2pos");
	}
	
	
	public ArrayList<String> getTags(String trnflname, String trnposchkflname, String trnpropsflname,
			String testflname, String testposchkflname, String testpropsflname) throws IOException {
		// Train
		Perceptron p = train(trnflname, trnposchkflname, trnpropsflname);
		// Test to get the tags
		test(p, testflname, testposchkflname, testpropsflname);
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
		for(int i=0; i<ChunklabelsIns.size(); i++) {
			if(i == 0) {
				pre_label = ChunklabelsIns.get(0);
				startposition = 0;
			}
			else if(IOBChunklabelsIns.get(i).equals("") ||
					IOBChunklabelsIns.get(i).equals("O") ||
					IOBChunklabelsIns.get(i).substring(0, 1).equals("B") ) {
				// end a chunk
				chunkinfo.add(new ChunkInfo(pre_label, startposition, i-startposition));
				startposition = i;
				pre_label = ChunklabelsIns.get(i);
			}
		}
		chunkinfo.add(new ChunkInfo(pre_label, startposition, ChunklabelsIns.size()-startposition));
		//SRL
		BufferedReader br3 = new BufferedReader(new InputStreamReader(
				new FileInputStream(trnpropsflname),"utf-8"
				));
		
		// SRL instances
		ArrayList<ArrayList<String>> SRLIns = new ArrayList<ArrayList<String>>();
		boolean []isInSRL = new boolean[30];
		for(int i=0; i<30; i++)
			isInSRL[i] = false;
		while((tmp=br3.readLine())!=null) {
			String []str = tmp.split("\t");
			ArrayList<String> srls = new ArrayList<String>();
			srls.add(str[0]);
			// Parse SRL
			for(int i=1; i<str.length; i++) {
				String []ch = {"(", ")"};

				if(str[i].contains(ch[0])) {
					if(str[i].contains(ch[1]))
						isInSRL[i] = false;
					else
						isInSRL[i] = true;
					
					String tmpstr = new String(str[i].substring(1, str[i].indexOf("*")));
					srls.add("B-"+tmpstr);
					
					if(!labels.contains("B-"+tmpstr)) {
						labels.add("B-"+tmpstr);
						labels.add("I-"+tmpstr);
					}
						
				}
				else if(str[i].contains(ch[1])) {
					isInSRL[i] = false;
					srls.add("I"+SRLIns.get(SRLIns.size()-1).get(i).substring(1));				
				}
				else if(isInSRL[i]) {
					//System.out.println(str[i]);
					srls.add("I"+SRLIns.get(SRLIns.size()-1).get(i).substring(1));
				}
				else {
					srls.add("O");
				}
			}
			SRLIns.add(srls);
		}
		
		br3.close();
		
		// Predict information: sentenceNum, predictNum - (row, Word)
		ArrayList<ArrayList<PredictInfo>> predictInfo = new ArrayList<ArrayList<PredictInfo>>();
		predictInfo.add(new ArrayList<PredictInfo>());
		for(int i=0; i<SRLIns.size(); i++) {
			if(SRLIns.get(i).get(0).equals("")) {
				// 一句了，下面设置一个句子序号的变量，对应找相应的predict
				predictInfo.add(new ArrayList<PredictInfo>());
			}
			else {
				if(!SRLIns.get(i).get(0).equals("-")) {
					// previous next
					String preWord, prePos, nextWord, nextPos;
					String pre2Word, pre2Pos, next2Word, next2Pos;
					if(i==0 || words.get(i-1).equals("")) {
						preWord = "#";
						prePos = "#";
					}
					else {
						preWord = words.get(i-1);
						prePos = PoSlabelsIns.get(i-1);
					}
					
					if(i == SRLIns.size()-1 || words.get(i+1).equals("")) {
						nextWord = "$";
						nextPos = "$";
					}
					else {
						nextWord = words.get(i+1);
						nextPos = PoSlabelsIns.get(i+1);
					}
					// previous2 next2
					if(i == 0 || words.get(i-1).equals("")) {// first word in a sentence
						pre2Word = "#";
						pre2Pos = "#";
					}
					else if(i == 1 || words.get(i-2).equals("")) {// second word in a sentence
						pre2Word = "#";
						pre2Pos = "#";
					}
					else {
						pre2Word = words.get(i-2);
						pre2Pos = PoSlabelsIns.get(i-2);
					}
					// next2 behind2-bigram
					if(i == SRLIns.size()-1 || words.get(i+1).equals("")) {
						next2Word = "$";
						next2Pos = "$";
					}
					else if(i == SRLIns.size()-2 || words.get(i+2).equals("")) {
						next2Word = "$";
						next2Pos = "$";
					}
					else {
						next2Word = words.get(i+2);
						next2Pos = PoSlabelsIns.get(i+2);
					}
					predictInfo.get(predictInfo.size()-1).add(
							new PredictInfo(SRLIns.get(i).get(0), i, 
									preWord, prePos, nextWord, nextPos,
									pre2Word, pre2Pos, next2Word, next2Pos));
				}
			}
		}
			
		// Form feature&label (unit: chunk)
		int sentenceNum = 0;
		ArrayList<ChunkInfo> senn = new ArrayList<ChunkInfo>();
		Instance instance = new Instance();
		for(int i=0; i<chunkinfo.size(); i++) {
			//selete a sentence out
			if(!chunkinfo.get(i).label.equals("")) {
				senn.add(chunkinfo.get(i));
			}
			else {
				// Insert each V-proposition into feature&label
// sen->senn				
				ArrayList<ArrayList<ChunkInfo>> sen_t = new ArrayList<ArrayList<ChunkInfo>>();
				for(int v=1; v<SRLIns.get(senn.get(0).startposition).size(); v++) {
					ArrayList<ChunkInfo> tmp_cinfo = new ArrayList<ChunkInfo>();
					for(int c=0; c<senn.size(); c++) 
						tmp_cinfo.add(new ChunkInfo(senn.get(c).label, senn.get(c).startposition, senn.get(c).length));
					sen_t.add(tmp_cinfo);
				}
				
				for(int v=1; v<SRLIns.get(senn.get(0).startposition).size(); v++) {
					// Insert each chunk into feature&label
					// 最好每个动词有自己单独的一套sen
// 修改chunk大小的同时，还要修改SRL标签内容
					ArrayList<ChunkInfo> sen = sen_t.get(v-1);
					for(int c=0; c<sen.size(); c++) {
						// 检测V
						// 训练的时候，测试某个Chunk是否存在当前的目标Predict，需要拆分Chunk
						//	这样会提高很多目标动词前后词的预测准确度
						// 测试的时候，查看一个Chunk是否存在Predict，同样拆分这个Chunk，增加feature表明这个chunk原来是包含动词的
						//if(sen.get(c).startposition == 714)
						//	System.out.println();
						if(predictInfo.get(sentenceNum).get(v-1).row >= sen.get(c).startposition
								&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																					+sen.get(c).length) {
							//System.out.println(predictInfo.get(sentenceNum).get(v-1).row);
							//System.out.println(sen.get(c).startposition);
							//System.out.println(sen.get(c).startposition+sen.get(c).length);
							//System.out.println();
							// 发现一个chunk包含的动词在chunk中间
							if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition
								&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																					+sen.get(c).length-1) {
								//增加一个chunk
								if(c != sen.size()-1) {
									sen.add(c+1, new ChunkInfo(sen.get(c).label, 
														predictInfo.get(sentenceNum).get(v-1).row+1,
														sen.get(c).startposition+sen.get(c).length 
															- predictInfo.get(sentenceNum).get(v-1).row-1));
									sen.get(c+1).hasPredict = true;
									sen.get(c+1).sumOfChunks2Predict = 0;
									sen.get(c+1).relativePosition = 0;
								}
								else {
									sen.add(new ChunkInfo(sen.get(c).label, 
											predictInfo.get(sentenceNum).get(v-1).row+1,
											sen.get(c).startposition+sen.get(c).length 
												- predictInfo.get(sentenceNum).get(v-1).row-1));
									sen.get(c+1).hasPredict = true;
									sen.get(c+1).sumOfChunks2Predict = 0;
									sen.get(c+1).relativePosition = 0;
								}
								//开始位置不变，长度减小，label不变
								sen.get(c).length = predictInfo.get(sentenceNum).get(v-1).row
										- sen.get(c).startposition;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
							}
							// 发现一个chunk包含的动词在chunk开始
							else if(predictInfo.get(sentenceNum).get(v-1).row == sen.get(c).startposition
									&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																						+sen.get(c).length-1) {
								// 长度-1，开始位置向后推移一位，label不变
								sen.get(c).length = sen.get(c).length-1;
								sen.get(c).startposition = sen.get(c).startposition+1;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
							}
							// 发现一个chunk包含的动词在chunk结束
							else if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition
									&& predictInfo.get(sentenceNum).get(v-1).row == sen.get(c).startposition
																					+sen.get(c).length-1) {
								// 长度-1，开始位置不变，label不变
								sen.get(c).length = sen.get(c).length-1;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
							}
							// 发现这个chunk和动词范围完全匹配，继续运行
							else {
								//修改feature continue
								//System.out.println();
								continue;
							}
						}
						// 不包含Predict
						else {
							sen.get(c).hasPredict = false;
							// 设置上下限
							int posi, bound;
							if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition) {
								posi = sen.get(c).startposition;
								//posi = c;
								bound = predictInfo.get(sentenceNum).get(v-1).row;
								sen.get(c).relativePosition = -1;
							}
							else {
								posi = predictInfo.get(sentenceNum).get(v-1).row;
								//posi = 
								bound = sen.get(c).startposition;
								//bound = c;
								sen.get(c).relativePosition = 1;
							}
							//String path = "";
							int distance = bound - posi;
							
// 目前还只是行数的
							sen.get(c).sumOfChunks2Predict = distance;
						}
						ArrayList<String> feats = new ArrayList<String>();
						ArrayList<String> featsname = new ArrayList<String>();
						featsname.addAll(this.featsname);
						featsname.add("label");
						
						/* Words */
						// cur-phraseWords
						String wd = "";
						for(int w=0; w<sen.get(c).length; w++)
							wd = wd + words.get(sen.get(c).startposition+w);
						feats.add(wd);
						// cur-phrasePrefix
						feats.add(words.get(sen.get(c).startposition));
						// cur-phraseSufix
						feats.add(words.get(sen.get(c).startposition + sen.get(c).length-1));
						// pre-phraseWords fore-phraseWords-bigram pre-phrasePrefix pre-phraseSufix
						wd = "";
						if(c == 0) {
							feats.add("#");
							feats.add("#" + feats.get(0));
							feats.add("#");
							feats.add("#");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c-1).length; w++)
								prewd = prewd + words.get(sen.get(c-1).startposition+w);
							feats.add(prewd);
							feats.add(prewd + feats.get(0));
							feats.add(words.get(sen.get(c-1).startposition));
							feats.add(words.get(sen.get(c-1).startposition + sen.get(c-1).length-1));
						}
						// next-phraseWords behind-phraseWords-bigram next-phrasePrefix next-phraseSufix
						wd = "";
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(feats.get(0)+"$");
							feats.add("$");
							feats.add("$");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c+1).length; w++)
								prewd = prewd + words.get(sen.get(c+1).startposition+w);
							feats.add(prewd);
							feats.add(feats.get(0)+prewd);
							feats.add(words.get(sen.get(c+1).startposition));
							feats.add(words.get(sen.get(c+1).startposition + sen.get(c+1).length-1));
						}
						
						/* PoS tag */
						// cur-phrasePoSs cur-phrasePoSPrefix cur-phrasePoSSufix
						wd = "";
						for(int w=0; w<sen.get(c).length; w++)
							wd = wd + PoSlabelsIns.get(sen.get(c).startposition+w);
						feats.add(wd);
						feats.add(PoSlabelsIns.get(sen.get(c).startposition));
						feats.add(PoSlabelsIns.get(sen.get(c).startposition + sen.get(c).length-1));
						// pre-phrasePoSs fore-phrasePoSs-bigram pre-phrasePoSPrefix pre-phrasePoSSufix
						wd = "";
						if(c == 0) {
							feats.add("#");
							feats.add("#" + feats.get(11));
							feats.add("#");
							feats.add("#");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c-1).length; w++)
								prewd = prewd + PoSlabelsIns.get(sen.get(c-1).startposition+w);
							feats.add(prewd);
							feats.add(prewd+feats.get(11));
							feats.add(PoSlabelsIns.get(sen.get(c-1).startposition));
							feats.add(PoSlabelsIns.get(sen.get(c-1).startposition + sen.get(c-1).length-1));
						}
						// next-phrasePoSs behind-phrasePoSs-bigram next-phrasePoSPrefix next-phrasePoSSufix
						wd = "";
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(feats.get(11)+"$");
							feats.add("$");
							feats.add("$");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c+1).length; w++)
								prewd = prewd + PoSlabelsIns.get(sen.get(c+1).startposition+w);
							feats.add(prewd);
							feats.add(feats.get(11)+prewd);
							feats.add(PoSlabelsIns.get(sen.get(c+1).startposition));
							feats.add(PoSlabelsIns.get(sen.get(c+1).startposition + sen.get(c+1).length-1));
						}
						
						/* Chunk */
						// cur-phraseChunk
						feats.add(sen.get(c).label);
						// pre-phraseChunk fore-phraseChunk-bigram
						if(c == 0) {
							feats.add("#");
							feats.add("#" + sen.get(c).label);
						}
						else {
							feats.add(sen.get(c-1).label);
							feats.add(sen.get(c-1).label + sen.get(c).label);
						}
						// next-phraseChunk behind-phraseChunk-bigram
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(sen.get(c).label+"$");
						}
						else {
							feats.add(sen.get(c+1).label);
							feats.add(sen.get(c).label+sen.get(c+1).label);
						}
						// PredictWord  PredictWordPoS Distence2Predict relativePosition hasPredict
						feats.add(predictInfo.get(sentenceNum).get(v-1).word);
						feats.add(PoSlabelsIns.get(predictInfo.get(sentenceNum).get(v-1).row));
						
						feats.add(String.valueOf(sen.get(c).sumOfChunks2Predict));
						feats.add(String.valueOf(sen.get(c).relativePosition));
						feats.add(String.valueOf(sen.get(c).hasPredict));
						//predict-preword predict-prepos predict-nextword predict-nextpos
						feats.add(predictInfo.get(sentenceNum).get(v-1).preWord);
						feats.add(predictInfo.get(sentenceNum).get(v-1).prePos);
						feats.add(predictInfo.get(sentenceNum).get(v-1).nextWord);
						feats.add(predictInfo.get(sentenceNum).get(v-1).nextPos);
						
						feats.add(predictInfo.get(sentenceNum).get(v-1).pre2Word);
						feats.add(predictInfo.get(sentenceNum).get(v-1).pre2Pos);
						feats.add(predictInfo.get(sentenceNum).get(v-1).next2Word);
						feats.add(predictInfo.get(sentenceNum).get(v-1).next2Pos);
						
						/* label */
						//System.out.println(sen.get(c).startposition);
						//System.out.println(v);
						String SRLabel = SRLIns.get(sen.get(c).startposition).get(v);
						feats.add(SRLabel);
						
						feaLabels.add(instance.formFeats(featsname, feats));
						
/* Remember to split V chunk phrase*/
					}
					// add a "" after each sentence
					feaLabels.add("");
				}
				senn = new ArrayList<ChunkInfo>();
				sentenceNum ++;
			}
		}
		
		/**
		 * Tarin
		 */
		Perceptron p = new Perceptron(40, labels, new Gen());
		p.trainMachine(feaLabels, 0.08);
		
		return p;
	}
	
	
	void test(Perceptron p, String testflname, String testposchkflname, String testpropsflname) throws IOException	{
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
				new FileInputStream(testflname),"utf-8"
				));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(
				new FileInputStream(testposchkflname)
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
		for(int i=0; i<ChunklabelsIns.size(); i++) {
			if(i == 0) {
				pre_label = ChunklabelsIns.get(0);
				startposition = 0;
			}
			else if(IOBChunklabelsIns.get(i).equals("") ||
					IOBChunklabelsIns.get(i).equals("O") ||
					IOBChunklabelsIns.get(i).substring(0, 1).equals("B") ) {
				// end a chunk
				chunkinfo.add(new ChunkInfo(pre_label, startposition, i-startposition));
				startposition = i;
				pre_label = ChunklabelsIns.get(i);
			}
		}
		chunkinfo.add(new ChunkInfo(pre_label, startposition, ChunklabelsIns.size()-startposition));
		//SRL
		BufferedReader br3 = new BufferedReader(new InputStreamReader(
				new FileInputStream(testpropsflname),"utf-8"
				));
		// SRL instances
		ArrayList<ArrayList<String>> SRLIns = new ArrayList<ArrayList<String>>();
		boolean []isInSRL = new boolean[30];
		for(int i=0; i<30; i++)
			isInSRL[i] = false;
		while((tmp=br3.readLine())!=null) {
			String []str = tmp.split("\t");
			ArrayList<String> srls = new ArrayList<String>();
			srls.add(str[0]);
			// Parse SRL
			for(int i=1; i<str.length; i++) {
				String []ch = {"(", ")"};

				if(str[i].contains(ch[0])) {
					if(str[i].contains(ch[1]))
						isInSRL[i] = false;
					else
						isInSRL[i] = true;
					
					String tmpstr = new String(str[i].substring(1, str[i].indexOf("*")));
					srls.add("B-"+tmpstr);
						
				}
				else if(str[i].contains(ch[1])) {
					isInSRL[i] = false;
					srls.add("I"+SRLIns.get(SRLIns.size()-1).get(i).substring(1));				
				}
				else if(isInSRL[i]) {
					//System.out.println(str[i]);
					srls.add("I"+SRLIns.get(SRLIns.size()-1).get(i).substring(1));
				}
				else {
					srls.add("O");
				}
			}
			SRLIns.add(srls);
		}
		
		br3.close();
		
		// Predict information: sentenceNum, predictNum - (row, Word)
		ArrayList<ArrayList<PredictInfo>> predictInfo = new ArrayList<ArrayList<PredictInfo>>();
		predictInfo.add(new ArrayList<PredictInfo>());
		for(int i=0; i<SRLIns.size(); i++) {
			if(SRLIns.get(i).get(0).equals("")) {
				// 一句了，下面设置一个句子序号的变量，对应找相应的predict
				predictInfo.add(new ArrayList<PredictInfo>());
			}
			else {
				if(!SRLIns.get(i).get(0).equals("-")) {
					// previous next
					String preWord, prePos, nextWord, nextPos;
					String pre2Word, pre2Pos, next2Word, next2Pos;
					if(i==0 || words.get(i-1).equals("")) {
						preWord = "#";
						prePos = "#";
					}
					else {
						preWord = words.get(i-1);
						prePos = PoSlabelsIns.get(i-1);
					}
					
					if(i == SRLIns.size()-1 || words.get(i+1).equals("")) {
						nextWord = "$";
						nextPos = "$";
					}
					else {
						nextWord = words.get(i+1);
						nextPos = PoSlabelsIns.get(i+1);
					}
					// previous2 next2
					if(i == 0 || words.get(i-1).equals("")) {// first word in a sentence
						pre2Word = "#";
						pre2Pos = "#";
					}
					else if(i == 1 || words.get(i-2).equals("")) {// second word in a sentence
						pre2Word = "#";
						pre2Pos = "#";
					}
					else {
						pre2Word = words.get(i-2);
						pre2Pos = PoSlabelsIns.get(i-2);
					}
					// next2 behind2-bigram
					if(i == SRLIns.size()-1 || words.get(i+1).equals("")) {
						next2Word = "$";
						next2Pos = "$";
					}
					else if(i == SRLIns.size()-2 || words.get(i+2).equals("")) {
						next2Word = "$";
						next2Pos = "$";
					}
					else {
						next2Word = words.get(i+2);
						next2Pos = PoSlabelsIns.get(i+2);
					}
					predictInfo.get(predictInfo.size()-1).add(
							new PredictInfo(SRLIns.get(i).get(0), i, 
									preWord, prePos, nextWord, nextPos,
									pre2Word, pre2Pos, next2Word, next2Pos));
				
				}
			}
		}
			
		// Form feature&label (unit: chunk)
		ArrayList<ArrayList<ChunkInfo>> chunks2words = new ArrayList<ArrayList<ChunkInfo>>();
		int sentenceNum = 0;
		ArrayList<ChunkInfo> senn = new ArrayList<ChunkInfo>();
		Instance instance = new Instance();
		for(int i=0; i<chunkinfo.size(); i++) {
			//selete a sentence out
			if(!chunkinfo.get(i).label.equals("")) {
				senn.add(chunkinfo.get(i));
			}
			else {
				// Insert each V-proposition into feature&label
				// 记录了二维的ChunkInfo (sentence, predictNum) - chunkinfo
				// 	与下面的tags形成对比 以空行为分割的chunk's SRL tags
// sen->senn				
				ArrayList<ArrayList<ChunkInfo>> sen_t = new ArrayList<ArrayList<ChunkInfo>>();
				for(int v=0; v<predictInfo.get(sentenceNum).size(); v++) {
					ArrayList<ChunkInfo> tmp_cinfo = new ArrayList<ChunkInfo>();
					for(int c=0; c<senn.size(); c++) 
						tmp_cinfo.add(new ChunkInfo(senn.get(c).label, senn.get(c).startposition, senn.get(c).length));
					sen_t.add(tmp_cinfo);
				}
				
				for(int v=1; v<predictInfo.get(sentenceNum).size()+1; v++) {
					// Insert each chunk into feature&label
					// 最好每个动词有自己单独的一套sen
// 修改chunk大小的同时，还要修改SRL标签内容
					ArrayList<ChunkInfo> sen = sen_t.get(v-1);
					for(int c=0; c<sen.size(); c++) {
						// 检测V
						// 训练的时候，测试某个Chunk是否存在当前的目标Predict，需要拆分Chunk
						//	这样会提高很多目标动词前后词的预测准确度
						// 测试的时候，查看一个Chunk是否存在Predict，同样拆分这个Chunk，增加feature表明这个chunk原来是包含动词的
						if(predictInfo.get(sentenceNum).get(v-1).row >= sen.get(c).startposition
								&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																					+sen.get(c).length) {
							
							// 发现一个chunk包含的动词在chunk中间
							if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition
								&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																					+sen.get(c).length-1) {
								
								
								if(c == sen.size()-1) {
									// 增加Predict Chunk
									sen.add(new ChunkInfo(Predictfeat, 
											predictInfo.get(sentenceNum).get(v-1).row, 1));
									//增加一个chunk
									sen.add(new ChunkInfo(sen.get(c).label, 
											predictInfo.get(sentenceNum).get(v-1).row+1,
											sen.get(c).startposition+sen.get(c).length 
												- predictInfo.get(sentenceNum).get(v-1).row-1));
									sen.get(c+2).hasPredict = true;
									sen.get(c+2).sumOfChunks2Predict = 0;
									sen.get(c+2).relativePosition = 0;
								}
								else {
									// 增加Predict Chunk
									sen.add(c+1, new ChunkInfo(Predictfeat, 
											predictInfo.get(sentenceNum).get(v-1).row, 1));
									//增加一个chunk
									sen.add(c+2, new ChunkInfo(sen.get(c).label, 
											predictInfo.get(sentenceNum).get(v-1).row+1,
											sen.get(c).startposition+sen.get(c).length 
												- predictInfo.get(sentenceNum).get(v-1).row-1));
									sen.get(c+2).hasPredict = true;
									sen.get(c+2).sumOfChunks2Predict = 0;
									sen.get(c+2).relativePosition = 0;
								}
								//开始位置不变，长度减小，label不变
								sen.get(c).length = predictInfo.get(sentenceNum).get(v-1).row
										- sen.get(c).startposition;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
							}
							// 发现一个chunk包含的动词在chunk开始
							else if(predictInfo.get(sentenceNum).get(v-1).row == sen.get(c).startposition
									&& predictInfo.get(sentenceNum).get(v-1).row < sen.get(c).startposition
																						+sen.get(c).length-1) {
								// 长度-1，开始位置向后推移一位，label不变
								sen.get(c).length = sen.get(c).length-1;
								sen.get(c).startposition = sen.get(c).startposition+1;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
								// 增加Predict Chunk
								sen.add(c, new ChunkInfo(Predictfeat, 
										predictInfo.get(sentenceNum).get(v-1).row, 1));
							}
							// 发现一个chunk包含的动词在chunk结束
							else if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition
									&& predictInfo.get(sentenceNum).get(v-1).row == sen.get(c).startposition
																					+sen.get(c).length-1) {
								// 长度-1，开始位置不变，label不变
								sen.get(c).length = sen.get(c).length-1;
								sen.get(c).hasPredict = true;
								sen.get(c).sumOfChunks2Predict = 0;
								sen.get(c).relativePosition = 0;
								// 增加Predict Chunk
								if(c == sen.size()-1) {
									sen.add(new ChunkInfo(Predictfeat, 
											predictInfo.get(sentenceNum).get(v-1).row, 1));
								}
								else {
									sen.add(c+1, new ChunkInfo(Predictfeat, 
											predictInfo.get(sentenceNum).get(v-1).row, 1));
								}
							}
							// 发现这个chunk和动词范围完全匹配，继续运行
							else {
								sen.get(c).label = Predictfeat;
							}
						}
						// 不包含Predict
						else {
							sen.get(c).hasPredict = false;
							// 设置上下限
							int posi, bound;
							if(predictInfo.get(sentenceNum).get(v-1).row > sen.get(c).startposition) {
								posi = sen.get(c).startposition;
								//posi = c;
								bound = predictInfo.get(sentenceNum).get(v-1).row;
								sen.get(c).relativePosition = -1;
							}
							else {
								posi = predictInfo.get(sentenceNum).get(v-1).row;
								//posi = 
								bound = sen.get(c).startposition;
								//bound = c;
								sen.get(c).relativePosition = 1;
							}
							//String path = "";
							int distance = bound - posi;
							
// 目前还只是行数的
							sen.get(c).sumOfChunks2Predict = distance;
						}
						
						// 判断是不是Predict
						if(sen.get(c).label.equals(Predictfeat)) {
							feaLabels.add(Predictfeat);
							continue;
						}
						ArrayList<String> feats = new ArrayList<String>();
						ArrayList<String> featsname = new ArrayList<String>();
						featsname.addAll(this.featsname);
						
						/* Words */
						// cur-phraseWords
						String wd = "";
						for(int w=0; w<sen.get(c).length; w++)
							wd = wd + words.get(sen.get(c).startposition+w);
						feats.add(wd);
						// cur-phrasePrefix
						feats.add(words.get(sen.get(c).startposition));
						// cur-phraseSufix
						feats.add(words.get(sen.get(c).startposition + sen.get(c).length-1));
						// pre-phraseWords fore-phraseWords-bigram pre-phrasePrefix pre-phraseSufix
						wd = "";
						if(c == 0) {
							feats.add("#");
							feats.add("#" + feats.get(0));
							feats.add("#");
							feats.add("#");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c-1).length; w++)
								prewd = prewd + words.get(sen.get(c-1).startposition+w);
							feats.add(prewd);
							feats.add(prewd + feats.get(0));
							feats.add(words.get(sen.get(c-1).startposition));
							feats.add(words.get(sen.get(c-1).startposition + sen.get(c-1).length-1));
						}
						// next-phraseWords behind-phraseWords-bigram next-phrasePrefix next-phraseSufix
						wd = "";
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(feats.get(0)+"$");
							feats.add("$");
							feats.add("$");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c+1).length; w++)
								prewd = prewd + words.get(sen.get(c+1).startposition+w);
							feats.add(prewd);
							feats.add(feats.get(0)+prewd);
							feats.add(words.get(sen.get(c+1).startposition));
							feats.add(words.get(sen.get(c+1).startposition + sen.get(c+1).length-1));
						}
						
						/* PoS tag */
						// cur-phrasePoSs cur-phrasePoSPrefix cur-phrasePoSSufix
						wd = "";
						for(int w=0; w<sen.get(c).length; w++)
							wd = wd + PoSlabelsIns.get(sen.get(c).startposition+w);
						feats.add(wd);
						feats.add(PoSlabelsIns.get(sen.get(c).startposition));
						feats.add(PoSlabelsIns.get(sen.get(c).startposition + sen.get(c).length-1));
						// pre-phrasePoSs fore-phrasePoSs-bigram pre-phrasePoSPrefix pre-phrasePoSSufix
						wd = "";
						if(c == 0) {
							feats.add("#");
							feats.add("#" + feats.get(11));
							feats.add("#");
							feats.add("#");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c-1).length; w++)
								prewd = prewd + PoSlabelsIns.get(sen.get(c-1).startposition+w);
							feats.add(prewd);
							feats.add(prewd+feats.get(11));
							feats.add(PoSlabelsIns.get(sen.get(c-1).startposition));
							feats.add(PoSlabelsIns.get(sen.get(c-1).startposition + sen.get(c-1).length-1));
						}
						// next-phrasePoSs behind-phrasePoSs-bigram next-phrasePoSPrefix next-phrasePoSSufix
						wd = "";
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(feats.get(11)+"$");
							feats.add("$");
							feats.add("$");
						}
						else {
							String prewd = "";
							for(int w=0; w<sen.get(c+1).length; w++)
								prewd = prewd + PoSlabelsIns.get(sen.get(c+1).startposition+w);
							feats.add(prewd);
							feats.add(feats.get(11)+prewd);
							feats.add(PoSlabelsIns.get(sen.get(c+1).startposition));
							feats.add(PoSlabelsIns.get(sen.get(c+1).startposition + sen.get(c+1).length-1));
						}
						
						/* Chunk */
						// cur-phraseChunk
						feats.add(sen.get(c).label);
						// pre-phraseChunk fore-phraseChunk-bigram
						if(c == 0) {
							feats.add("#");
							feats.add("#" + sen.get(c).label);
						}
						else {
							feats.add(sen.get(c-1).label);
							feats.add(sen.get(c-1).label + sen.get(c).label);
						}
						// next-phraseChunk behind-phraseChunk-bigram
						if(c == sen.size()-1) {
							feats.add("$");
							feats.add(sen.get(c).label+"$");
						}
						else {
							feats.add(sen.get(c+1).label);
							feats.add(sen.get(c).label+sen.get(c+1).label);
						}
						// PredictWord PredictWordPoS Distence2Predict relativePosition hasPredict
						feats.add(predictInfo.get(sentenceNum).get(v-1).word);
						feats.add(PoSlabelsIns.get(predictInfo.get(sentenceNum).get(v-1).row));
						
						feats.add(String.valueOf(sen.get(c).sumOfChunks2Predict));
						feats.add(String.valueOf(sen.get(c).relativePosition));
						feats.add(String.valueOf(sen.get(c).hasPredict));
						//predict-preword predict-prepos predict-nextword predict-nextpos
						feats.add(predictInfo.get(sentenceNum).get(v-1).preWord);
						feats.add(predictInfo.get(sentenceNum).get(v-1).prePos);
						feats.add(predictInfo.get(sentenceNum).get(v-1).nextWord);
						feats.add(predictInfo.get(sentenceNum).get(v-1).nextPos);
						
						feats.add(predictInfo.get(sentenceNum).get(v-1).pre2Word);
						feats.add(predictInfo.get(sentenceNum).get(v-1).pre2Pos);
						feats.add(predictInfo.get(sentenceNum).get(v-1).next2Word);
						feats.add(predictInfo.get(sentenceNum).get(v-1).next2Pos);
						
						feaLabels.add(instance.formFeats(featsname, feats));
						
/* Remember to split V chunk phrase*/
					}
					// add a "" after each sentence
					feaLabels.add("");
				}
				chunks2words.addAll(sen_t);
				senn = new ArrayList<ChunkInfo>();
				sentenceNum ++;
			}
		}
		
		/**
		 *  Get the tags and Normailiza the tags
		 */
		for(int i=0; i<feaLabels.size(); i++) {
			if(feaLabels.get(i).equals("")) {
				tags.add("");
				continue;
			}
			if(feaLabels.get(i).equals(Predictfeat)) {
				tags.add(PredictLabel);
				continue;
			}
			if(!feaLabels.get(i).equals("")) {
				if(i == 0)
					tags.add(p.labelofInstance(feaLabels.get(i), ""));
				else
					tags.add(p.labelofInstance(feaLabels.get(i), tags.get(i-1)));
				
			}
		}
		// !TEST!
		
		for(int i=0; i<tags.size(); i++) {
			if(tags.get(i).equals("") || tags.get(i).equals(PredictLabel))
				continue;
			if(tags.get(i).equals("O")) {
				tags.set(i, "*");
				continue;
			}
			String []str = new String[2];
			str[0] = tags.get(i).substring(0, 1);
			str[1] = tags.get(i).substring(2);
			if(str[0].equals("B")) {
				//System.out.println(tags.get(i+1));
				if(i != tags.size()-1 && !tags.get(i+1).equals("") &&
						tags.get(i+1).substring(0,1).equals("I") )
					tags.set(i, "(" + str[1] + "*");
				else
					tags.set(i, "(" + str[1] + "*" 
							+ ")");
			}
			else if(str[0].equals("I")) {
				if(i != tags.size()-1 && !tags.get(i+1).equals("")
						&& tags.get(i+1).substring(0,1).equals("I"))
					tags.set(i, "*");
				else
					tags.set(i, "*" + ")");
			}
			else 
				tags.set(i, "*");
		}
		for(int i=0; i<tags.size(); i++) {
			if(tags.get(i).equals(""))
				tags.remove(i);
		}
		// Transfer 'tags' into props
		// data:
		//	ArrayList<ArrayList<ChunkInfo>> chunks2words - (sentenceNum, predictNum) - chunkInfo
		//	ArrayList<String> tags
		//	上面两者都不对应没有Predict的句子
		/**
		 * 策略：
		 * 	构建只有一列信息的props，进行填充
		 * 细节：
		 * 	chunks2words是以Predict为单位形成的整个句子为单位的chunkInfo
		 * 执行：
		 * 	以predictInfo->chunks2words->tags
		 */
		ArrayList<ArrayList<String>> props = new ArrayList<ArrayList<String>>();
		for(int i=0; i<SRLIns.size(); i++) {
			props.add(new ArrayList<String>());
			props.get(i).add(SRLIns.get(i).get(0));
		}
		int idxOfchunks2words = 0;
		int idxOfTags = 0;
		for(int sent=0; sent<predictInfo.size(); sent++) {//某个句子
			for(int prdict = 0; prdict<predictInfo.get(sent).size(); prdict++) {// 某一列
				// 只是得到数据规范，开始对应tag
				//	idxOfchunks2words索引chunks2words, tags(以chunk为单位的)
				//	prdict索引列
				for(int ch=0; ch<chunks2words.get(idxOfchunks2words).size(); ch++) {
					ChunkInfo chkinf = chunks2words.get(idxOfchunks2words).get(ch);
					//prdict列，chunks2words.get(sent).get(ch).startpoint-length是行
					String []str = new String[2];
					str[0] = tags.get(idxOfTags).substring(0, 1);
					str[1] = tags.get(idxOfTags).substring(tags.get(idxOfTags).length()-1);
					int format;
					if(str[0].equals("(")) {
						if(str[1].equals(")"))
							format = 2;
						else
							format = 0;
					}
					else {
						if(str[1].equals(")"))
							format = 1;
						else
							format = 3;
					}
					/**
					 * 对于一个chunk来说，
					 * 	可能有四种形式：（， ）， （*）， *
					 * 只需要修改第一个和最后一个即可
					 */
					for(int r=chkinf.startposition; r<chkinf.startposition+chkinf.length; r++) {
						props.get(r).add("*");
					}
					if(format == 0) {
						props.get(chkinf.startposition).set(
								props.get(chkinf.startposition).size()-1, tags.get(idxOfTags));
					}
					else if(format == 1) {
						props.get(chkinf.startposition+chkinf.length-1).set(
								props.get(chkinf.startposition).size()-1, tags.get(idxOfTags));
					}
					else if(format == 2) {
						if(chkinf.length > 1) {
							props.get(chkinf.startposition).set(
								props.get(chkinf.startposition).size()-1, 
									tags.get(idxOfTags).substring(0, tags.get(idxOfTags).length()-1));
							props.get(chkinf.startposition+chkinf.length-1).set(
								props.get(chkinf.startposition).size()-1, "*)");
						}
						else {
							props.get(chkinf.startposition).set(
									props.get(chkinf.startposition).size()-1, tags.get(idxOfTags));
						}
					}
					idxOfTags ++;
				}
				// 进过句子和predict的不断叠加才得到chunks2words的index
				idxOfchunks2words ++;//句子的标号
			}
		}
		FileWriter fileWriter = new FileWriter("./data/devret.props");
		for(int i=0; i<props.size(); i++) {
			for(int j=0; j<props.get(i).size(); j++) {
				fileWriter.write(props.get(i).get(j));
				if(j != props.get(i).size()-1)
					fileWriter.write("\t");
			}
			fileWriter.write("\n");
		}
		fileWriter.close();
		return;
	}
	
	class ChunkInfo {
		String label;
		int startposition;
		int length;
		/* 0表示这个chunk中有V，>0表示到V的Chunk数目，-1为缺省值*/
		boolean hasPredict = false;
		int relativePosition;
		int sumOfChunks2Predict = -1;
		ChunkInfo(String label, int startposition, int length) {
			this.label = label;
			this.startposition = startposition;
			this.length = length;
		}
	}
	class PredictInfo {
		String word;
		int row;
		String preWord;
		String prePos;
		String nextWord;
		String nextPos;
		String pre2Word, pre2Pos, next2Word, next2Pos;
		PredictInfo(String word, int row, 
				String preWord, String prePos, String nextWord, String nextPos,
				String pre2Word, String pre2Pos, String next2Word, String next2Pos) {
			this.word = word;
			this.row = row;
			this.preWord = preWord;
			this.prePos = prePos;
			this.nextWord = nextWord;
			this.nextPos = nextPos;
			this.pre2Word = preWord;
			this.pre2Pos = prePos;
			this.next2Word = nextWord;
			this.next2Pos = nextPos;
		}
	}
}