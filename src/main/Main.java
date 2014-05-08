package main;

import java.io.IOException;

//import chunker.Chunker;
import perceptron.Perceptron;
import postagger.PoSTagger;
//import srl.Srl;

public class Main {
	public static void main(String []args) throws IOException {
		PoSTagger p = new PoSTagger();
		p.getTags("./data/trn.wrd", "./data/trn.pos-chk", "./data/dev.wrd");
		//Chunker c = new Chunker();
		//c.getTags("./data/trn.wrd", "./data/trn.pos-chk", "./data/dev.wrd");
		//Srl s = new Srl();
		//s.getTags(null, null, "./data/trn.props");
	}
}