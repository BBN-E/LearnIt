package com.bbn.akbc.neolearnit.preprocessing.corpus;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.bbn.akbc.utility.FileUtil;

public class Util {
	static List<String> generateSerifXmlListForD2D(String docidList, String serifXmlDir) {
		List<String> serifXmlList = new ArrayList<String>();

		List<String> lines = FileUtil.readLinesIntoList(docidList);
		for(String line : lines) {
			String dirName = line.substring(0, line.indexOf("_"));
			String fileName = line.substring(line.indexOf("_")+1, line.lastIndexOf("."));

			String serifXmlPath = serifXmlDir + dirName + "/output/" + fileName + ".sgm.xml";

			serifXmlList.add(serifXmlPath);
		}

		return serifXmlList;
	}

	static String getSentenceIDstr(int sid) {
		DecimalFormat formatter = new DecimalFormat("#00000");
		return formatter.format(sid);
	}
}
