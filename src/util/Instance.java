package util;

import java.util.ArrayList;

public class Instance {
	public String formFeats(ArrayList<String> featsname, ArrayList<String> feats) {
		String featsStr = "";
		for(int i=0; i<featsname.size(); i++) {
			if(i == 0)
				featsStr += featsname.get(i) + "__" + feats.get(i);
			else
				featsStr += "__" + featsname.get(i) + "__" + feats.get(i);
		}
		
		return featsStr;
	}
	
	public String formFeats(String fs, ArrayList<String> featsname, ArrayList<String> feats) {
		String str = fs;
		if(!featsname.isEmpty())
			str += "__" + formFeats(featsname, feats);
		return str;
	}
}