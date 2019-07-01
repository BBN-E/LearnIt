package com.bbn.akbc.common;

/**
 * Created by bmin on 5/12/15.
 */

import com.bbn.akbc.evaluation.tac.Query;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.resource.Resources;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// import com.bbn.akbc.neolearnit.common.InstanceIdentifier;

public class SerifHelper {

  /*
  public static Optional<String> getTextAnnotation(InstanceIdentifier inst) {
    String docId = inst.getDocid();
    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + docId + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
    }

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      if (!fileSerifXml.exists()) {
        return Optional.absent();
      }
      DocTheory dt = fromXML.loadFrom(fileSerifXml);
      SentenceTheory sentTheory = dt.sentenceTheory(inst.getSentid());

      StringBuilder sb = new StringBuilder();
//			int tokenIdx=0;
//			sentTheory.tokenSequence().size();

      for (int tokenIdx = 0; tokenIdx < sentTheory.tokenSequence().size(); tokenIdx++) {
//			while(sentTheory.tokenSequence().iterator().hasNext()) {
        Token t = sentTheory.tokenSequence().token(tokenIdx);
        if (tokenIdx == inst.getSlot0Start()) {
          sb.append("<arg1>");
        }
        if (tokenIdx == inst.getSlot1Start()) {
          sb.append("<arg2>");
        }

        sb.append(t.text() + " ");

        if (tokenIdx == inst.getSlot0End()) {
          sb.append("</arg1>");
        }
        if (tokenIdx == inst.getSlot1End()) {
          sb.append("</arg2>");
        }
      }
      return Optional.of(sb.toString().trim());
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }
  */

  public static String getTextAnnotated(TextRelationMention rm) {
                /*
                 * commented temporary
		 * TODO: fix compiling error:
		 * [ERROR] /nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/src/main/java/com/bbn/coldstart/visualization/SerifHelper.java:[82,95] cannot access com.bbn.bue.common.format.offsets.CharOffset
class file for com.bbn.bue.common.format.offsets.CharOffset not found
[ERROR] /nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/src/main/java/com/bbn/coldstart/visualization/SerifHelper.java:[88,91] cannot access com.bbn.bue.common.format.LocatedString
class file for com.bbn.bue.common.format.LocatedString not found
		 *
		String docId = rm.query.docId;
		String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
        File fileSerifXml = new File(strPathSerifXml);
        // test to see if path to serifxml exists, if not try adding .sgm
        if (!fileSerifXml.exists()) {
                strPathSerifXml = Resources.getPathSerifXml() +  docId + ".sgm.xml";
                fileSerifXml = new File(strPathSerifXml);
        }

		try {
			SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
			if(!fileSerifXml.exists()) return "File NOT found: " + strPathSerifXml;
			DocTheory dt = fromXML.loadFrom(fileSerifXml);
			for(SentenceTheory sentTheory : dt.sentenceTheories()) {
				if(sentTheory.span().size()<=0) continue;

				int sentStart = sentTheory.span().startToken().startCharOffset().value();
				int sentEnd = sentTheory.span().endToken().endCharOffset().value();

				if(sentStart == rm.spanOfSent().get().getStart() && sentEnd == rm.spanOfSent().get().getEnd()) {
					StringBuilder sb = new StringBuilder();
					sb.append(docId + ": ");
					String sentText = dt.document().originalText().get().substring(sentStart, sentEnd).text();

					for(int i=0; i<sentEnd-sentStart; i++) {
						if(i==rm.query.span.getStart()-sentStart) sb.append("<arg1>");
						if(i==rm.query.span.getEnd()+1-sentStart) sb.append("</arg1>");
						if(i==rm.answer.span.getStart()-sentStart) sb.append("<arg2>");
						if(i==rm.answer.span.getEnd()+1-sentStart) sb.append("</arg2>");

						sb.append(sentText.substring(i,i+1));
					}

					return sb.toString().replace("\n", " ");
				}
			}
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("Exception in reading: " + strPathSerifXml);
		}
		*/

    return "";
  }

  public static List<String> getTextAnnotated(Query query) {
    List<String> listText = new ArrayList<String>();

    for (TextMention mention : query.mentions) {
      listText.add(getTextAnnotated(mention));
    }

    return listText;
  }

  public static String getTextAnnotated(TextMention mention) {
                /*
                 * commented temporarily
		 * TODO: [ERROR] /nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/src/main/java/com/bbn/coldstart/visualization/SerifHelper.java:[142,95] cannot access com.bbn.bue.common.format.offsets.CharOffset
[ERROR] class file for com.bbn.bue.common.format.offsets.CharOffset not found
[ERROR] /nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/src/main/java/com/bbn/coldstart/visualization/SerifHelper.java:[150,91] cannot access com.bbn.bue.common.format.LocatedString
[ERROR] class file for com.bbn.bue.common.format.LocatedString not found
		 **/
		String strPathSerifXml = Resources.getPathSerifXml() + mention.getDocId() + ".xml";
                File f = new File(strPathSerifXml);
                // test to see if path to serifxml exists, if not try adding .sgm
                if (!f.exists()) {
                        strPathSerifXml = Resources.getPathSerifXml() +  mention.getDocId() + ".sgm.xml";
                        f = new File(strPathSerifXml);
                }
                if (!f.exists()) {
                  strPathSerifXml = Resources.getPathSerifXml() +  mention.getDocId() + ".serifxml.xml";
                  f = new File(strPathSerifXml);
                }
                if (!f.exists()) {
                  strPathSerifXml = Resources.getPathSerifXml() +  mention.getDocId() + ".mpdf.serifxml.xml";
                  f = new File(strPathSerifXml);
                }


                try {
			SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
			DocTheory dt = fromXML.loadFrom(f);
                        for(int sid=0; sid<dt.numSentences(); sid++) {
                          SentenceTheory sentTheory=dt.sentenceTheory(sid);
//			for(SentenceTheory sentTheory : dt.sentenceTheories()) {
				if(sentTheory.tokenSequence().size()<=0) continue;
				int sentStart = sentTheory.tokenSpan().startToken().startCharOffset().value();
				int sentEnd = sentTheory.tokenSpan().endToken().endCharOffset().value();

				int beg = mention.getSpan().getStart();
				int end = mention.getSpan().getEnd();

				if(sentStart<=beg && sentEnd>=end) {

					String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

					String str1 = sentText.substring(0, beg-sentStart);
					String str2 = sentText.substring(beg-sentStart, end-sentStart);
					String str3 = sentText.substring(end-sentStart);

					return str1 + "<u><b>" + str2 + "</b></u>" + str3;
				}
			}
		}catch(IOException e){
			e.printStackTrace();
			//code to handle an IOException here
			System.out.println("Exception in reading: " + strPathSerifXml);
		}
		/**/

    return "";
  }

  public static Optional<String> getTextFromSpan(TextSpan span, String docId) {
    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strPathSerifXml));
      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if (sentTheory.span().size() <= 0) {
          continue;
        }
        int sentStart = sentTheory.span().startToken().startCharOffset().value();
        int sentEnd = sentTheory.span().endToken().endCharOffset().value();

        int beg = span.getStart();
        int end = span.getEnd();

        if (sentStart <= beg && sentEnd >= end) {

          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);
          String str = sentText.substring(beg - sentStart, end - sentStart);

          return Optional.of(str);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }

  public static Optional<String> getTextAnnotated(TextSpan query, TextSpan answer,
      String docId) {

    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";

    int agent1_start = query.getStart();
    int agent1_end = query.getEnd();

    int answer_start = answer.getStart();
    int answer_end = answer.getEnd();

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strPathSerifXml));
      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if (sentTheory.span().size() <= 0) {
          continue;
        }
        int sentStart = sentTheory.span().startToken().startCharOffset().value();
        int sentEnd = sentTheory.span().endToken().endCharOffset().value();

        if (sentStart <= agent1_start && sentEnd >= agent1_end &&
            sentStart <= answer_start && sentEnd >= answer_end) {

          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

          StringBuilder sb = new StringBuilder();

          for (int i = 0; i < sentText.length(); i++) {
            if (i == agent1_start - sentStart) {
              sb.append("<a1>");
            } else if (i == agent1_end - sentStart + 1) {
              sb.append("</a1>");
            } else if (i == answer_start - sentStart) {
              sb.append("<a2>");
            } else if (i == answer_end - sentStart + 1) {
              sb.append("</a2>");
            }

            sb.append(sentText.charAt(i));
          }

          return Optional
              .of(sb.toString().replace("\t", " ").replace("\n", " ").replace("\r", " ").trim());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }
}
