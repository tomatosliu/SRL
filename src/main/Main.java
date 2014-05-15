package main;

import java.io.IOException;

import chunker.Chunker;
import perceptron.Perceptron;
import postagger.PoSTagger;
import srl.Srl;

public class Main {
	public static void main(String []args) throws IOException {
		//PoSTagger p = new PoSTagger();
		//p.getTags("./data/trn.wrd", "./data/trn.pos-chk", "./data/dev.wrd");
		//Chunker c = new Chunker();
		//c.getTags("./data/trn.wrd", "./data/trn.pos-chk", "./data/dev.wrd", "./data/dev.pos");
		Srl s = new Srl();
		s.getTags("./data/trn.wrd", "./data/trn.pos-chk", "./data/trn.props",
				"./data/dev.wrd", "./data/dev.chk", "./data/dev.props");
		//boolean b = true;
		//System.out.println(String.valueOf(b));
		/*
		int sum = 10;
		for(int i=0; i<sum; i++) {
			System.out.println(i);
			if(i == 9)
				sum = 20;
		}
		*/
	}
}