package com.bbn.akbc.neolearnit.preprocessing.corpus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Entity.RepresentativeMention;

public class FindFreqNames {
	static Writer out;

	static String getBestName(Entity e) {
		RepresentativeMention bestMention = e.representativeMention();
		String bestMentionText = bestMention.mention().atomicHead().toCasedTextString();
		return bestMentionText;
	}

	static void readNamesFromOneDoc(String strFileSerifXml) throws IOException {
		SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
		DocTheory dt = fromXML.loadFrom(new File(strFileSerifXml));

		String docId = dt.docid().toString();

		for(int i=0; i<dt.numSentences(); i++) {
			SentenceTheory sentTheory = dt.sentenceTheory(i);

			for(Mention m1 : sentTheory.mentions()) {
				Entity e1;
				if(!m1.entity(dt).isPresent()) continue;
				e1 = m1.entity(dt).get();
				String bestNameE1 = getBestName(e1);


				out.write("TEXT:\t" + docId + "\t" + bestNameE1 + "\t" + m1.toString() + "\t" + e1.type().toString() + "\t" + m1.mentionType().toString() + "\n");


				if(bestNameE1.contains("博科 圣地") || bestNameE1.contains("博科圣地")) {
					out.write("TERROR1\t" + docId + "\t" + bestNameE1 + "\t" + e1.type().toString() + "\t" + m1.mentionType().toString() + "\n");
				}

				if(bestNameE1.contains("博科 哈拉姆") || bestNameE1.contains("博科哈拉姆")) {
					out.write("TERROR2\t" + docId + "\t" + bestNameE1 + "\t" + e1.type().toString() + "\t" + m1.mentionType().toString() + "\n");
				}

				for(Mention m2 : sentTheory.mentions()) {
					Entity e2;
					if(!m2.entity(dt).isPresent()) continue;
					e2 = m2.entity(dt).get();
					String bestNameE2 = getBestName(e2);

					out.write("DBG:\t" + docId + "\t" +
							bestNameE1 + "\t" + e1.type().toString() + "\t" + m1.mentionType().toString() + "\t" +
							bestNameE2 + "\t" + e2.type().toString() + "\t" + m2.mentionType().toString() + "\n");

					if(bestNameE1.contains("尼日 利亚") || bestNameE2.contains("尼日 利亚"))
						out.write("Mention:\t" + docId + "\t" +
								bestNameE1 + "\t" + e1.type().toString() + "\t" + m1.mentionType().toString() + "\t" +
								bestNameE2 + "\t" + e2.type().toString() + "\t" + m2.mentionType().toString() + "\n");
				}
			}

		}
	}




	public static void main(String [] argv) throws IOException {
		String strFileOut = "/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/log.run_FindFreqNames";
		out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(strFileOut), "UTF-8"));

		String docidList = "/nfs/mercury-04/u24/mcrivaro/mr/data/d2d/nigeria_docids.txt";
		String serifXmlDir = "/nfs/mercury-04/u24/mcrivaro/mr/data/d2d/serifxml/";

		List<String> serifXmlList = Util.generateSerifXmlListForD2D(docidList, serifXmlDir);

		for(String serifXml : serifXmlList) {
			out.write("== Doc: " + serifXml + "\n");
			readNamesFromOneDoc(serifXml);
		}

		out.close();
	}
}
