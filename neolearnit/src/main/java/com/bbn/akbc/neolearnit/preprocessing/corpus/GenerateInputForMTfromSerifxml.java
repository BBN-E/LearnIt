package com.bbn.akbc.neolearnit.preprocessing.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import com.bbn.akbc.utility.FileUtil;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;

public class GenerateInputForMTfromSerifxml {
//	static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/1/raw_segments/";
//	static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/3/raw_segments/";
//	static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/4/raw_segments/";
//	static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/from_ys/raw_segments/";
	// static String strDirSegment = "/nfs/mercury-04/u42/bmin/d2d/corpus/hk_protest_1/raw_segments/";
	static String strDirSegment = "/nfs/mercury-04/u42/bmin/projects/relation_extraction/spanish-relations-extraction/test-corpus-10docs/segments/";

	/*
	// Example: /nfs/mercury-04/u24/mcrivaro/mr/data/d2d/serifxml//voa/output/yeman-20100120-82186362-459733.sgm.xml
	static String getDocIdFromSerifxmlPath(String strFileSerifXml) throws Exception {
		String [] items = strFileSerifXml.split("/");

		if(items[items.length-1].contains(".sgm.xml"))
			return items[items.length-3] + "_" + items[items.length-1].replace(".sgm.xml", "");
		else if(items[items.length-1].contains(".xml"))
			return items[items.length-3] + "_" + items[items.length-1].replace(".xml", "");
		else
			throw new Exception("Incorrect SERIFXML file name in: " + strFileSerifXml);
	}
	*/

	// Example: /nfs/mercury-04/u22/d2d/entityProfiles/webpages_serif/output/d139.xml
	static String getDocIdFromSerifxmlPath(String strFileSerifXml) throws Exception {
		String [] items = strFileSerifXml.split("/");

		return items[items.length-1].replace(".xml", "");
	}

	static String getSegmentPathFromSerifxmlPath(String strFileSerifXml) throws Exception {
		return strDirSegment + getDocIdFromSerifxmlPath(strFileSerifXml) + ".segment";
	}

	/* Example:
		<SEGMENT>
   			<GUID>[CBS20001021.1000.0347][CBS20001021.1000.0347][00001]</GUID>
   			<RAW_TEXT>越南湄公河地区最近因为连续豪雨造成了300多人死亡，中华民 国驻教廷大使馆20号响应教廷人道 关怀号召，透过意兴委员会 主席高德士总主教代表政府捐赠2500美元供作救助水灾难民。</RAW_TEXT>
		</SEGMENT>
	 */

	static String getSentTextFromTokenSequence(TokenSequence ts) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<ts.size(); i++)
			sb.append(ts.token(i).text() + " ");
		return sb.toString();
	}

	static void convertSerifxmlToSegmentForMT(String strFileSerifXml) throws Exception {
		try {
			String strFileSegment = getSegmentPathFromSerifxmlPath(strFileSerifXml);
			String docID = getDocIdFromSerifxmlPath(strFileSerifXml);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(strFileSegment), "UTF-8"));

			SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();

			DocTheory dt = fromXML.loadFrom(new File(strFileSerifXml));
			for(int sid=0; sid<dt.numSentences(); sid++) {
				SentenceTheory sentTheory = dt.sentenceTheory(sid);
				String sentText = getSentTextFromTokenSequence(sentTheory.tokenSequence());
				String strSentID = Util.getSentenceIDstr(sid+1); // start from 00001

				bw.write("<SEGMENT>\n");
				bw.write("   <GUID>" + "[" + docID + "]" + "[" + docID + "]" + "[" + strSentID + "]</GUID>\n");
				bw.write("   <RAW_TEXT>" + sentText + "</RAW_TEXT>\n");
				bw.write("</SEGMENT>\n");
			}

			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String [] argv) throws Exception {

//		String docidList = "/nfs/mercury-04/u24/mcrivaro/mr/data/d2d/nigeria_docids.txt";
//		String serifXmlDir = "/nfs/mercury-04/u24/mcrivaro/mr/data/d2d/serifxml/";
//		List<String> serifXmlList = Util.generateSerifXmlListForD2D(docidList, serifXmlDir);

//		List<String> serifXmlList = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/d2d/corpus/3/list_serifxmls");
//		List<String> serifXmlList = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/d2d/corpus/4/list_serifxmls");
//		List<String> serifXmlList = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/d2d/corpus/from_ys/list_serifxmls");
		// List<String> serifXmlList = FileUtil.readLinesIntoList("/nfs/mercury-04/u22/d2d/hong_kong_protest/data/hkProtest.corpus.filelist.txt");
		List<String> serifXmlList = FileUtil.readLinesIntoList("/nfs/mercury-04/u42/bmin/projects/relation_extraction/spanish-relations-extraction/test-corpus-10docs/list.10docs.spanish_gigaword.txt");

		for(String serifXml : serifXmlList) {
			System.out.println("== Doc: " + serifXml + "\n");
			convertSerifxmlToSegmentForMT(serifXml);
		}
	}
}
