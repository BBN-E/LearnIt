package com.bbn.akbc.common;

import com.bbn.akbc.evaluation.tac.CSAssessment;
import com.bbn.akbc.evaluation.tac.CSInitQuery;
import com.bbn.akbc.evaluation.tac.Query;
import com.bbn.akbc.evaluation.tac.RelationFromSubmission;
import com.bbn.akbc.evaluation.tac.io.DocTheoryLoader;
import com.bbn.akbc.evaluation.tac.io.SystemKbMentionLoader;
import com.bbn.akbc.evaluation.tac.io.SystemKbRelationLoader;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.kb.text.TextSpan;
import com.bbn.akbc.resource.Resources;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTMLHelper {

  static int global_divId = 0;

  public static String getColorForJudgement(String judgement, boolean inSys) {
    String color = "";
    if (judgement.equals("-1") && inSys) {
      color = "orange";
    } else if (judgement.equals("1") && inSys) {
      color = "green";
    } else if (judgement.equals("1") && !inSys) {
      color = "lightblue";
    } else if (judgement.equals("3") && inSys) {
      color = "blue";
    } else if (judgement.equals("4") && inSys) {
      color = "turquoise";
    } else if (judgement.equals("NIL") && inSys) {
      color = "grey";
    }

    return color;
  }

  static int MAX_TEXT_CHAR_SIZE_FOR_DISPLAY = 1000;

  public static Optional<String> getCollapsableHTMLForSpans(List<TextSpan> spans, String docId,
      int divId) {
    return getCollapsableHTMLForSpans(spans, docId, divId, false);
  }

  public static Optional<String> getCollapsableHTMLForSpans(List<TextSpan> spans, String docId,
      int divId, boolean needAddDotSgmToFilePath) {
    StringBuilder sbCollapsableHTML = new StringBuilder();
    String blockId = "fulltext" + divId;
    sbCollapsableHTML.append("<TextSpan onclick=\"$('#" + blockId
        + "').toggle()\" style=\"cursor:pointer\"><font color = \"blue\">showFullText</font></TextSpan>");
    sbCollapsableHTML.append("<div id=\"" + blockId
        + "\" style=\"display: none;margin: 10px;border: 1px solid black;padding:  5px\">");

		/*
                String strPathSerifXml;
		if(needAddDotSgmToFilePath)
			strPathSerifXml = Resources.getPathSerifXml() + docId + ".sgm.xml";
		else
			strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
*/

    String strPathSerifXml = Resources.getPathSerifXml() + docId + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + docId + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
      if(!fileSerifXml.exists()) {
        strPathSerifXml = Resources.getPathSerifXml() + docId + ".serifxml";
        fileSerifXml = new File(strPathSerifXml);
      }
    }

    DocTheory dt = null;
    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
//			 fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        return Optional.absent();
      }
      dt = fromXML.loadFrom(fileSerifXml);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String docText = dt.document().originalText().content().utf16CodeUnits();

    Multiset<Integer> lefts = HashMultiset.create();
    Multiset<Integer> rights = HashMultiset.create();

    int smallest = Integer.MAX_VALUE;
    int largest = 0;
    for (TextSpan span : spans) {
      lefts.add(span.getStart());
      rights.add(span.getEnd());

      if (span.getStart() < smallest) {
        smallest = span.getStart();
      }
      if (span.getEnd() > largest) {
        largest = span.getEnd();
      }
    }

    int num_char_padding = MAX_TEXT_CHAR_SIZE_FOR_DISPLAY - (largest - smallest);
    if (num_char_padding <= 0) {
      System.err.println("error: MAX_TEXT_CHAR_SIZE_FOR_DISPLAY=" + MAX_TEXT_CHAR_SIZE_FOR_DISPLAY
          + ", which is less than span: (" + smallest + ", " + largest + ")");
    }

    smallest -= num_char_padding / 2;
    largest += num_char_padding / 2;
    if (smallest < 0) {
      smallest = 0;
    }
    if (largest > docText.length()) {
      largest = docText.length();
    }

    StringBuilder sb = new StringBuilder();
    for (int i = smallest; i < largest; i++) {
      if (lefts.contains(i)) {
        for (int j = 0; j < lefts.count(i); j++) {
          sb.append("<b>");
        }
      }
      sb.append(docText.charAt(i));
      if (rights.contains(i)) {
        for (int j = 0; j < rights.count(i); j++) {
          sb.append("</b>");
        }
      }
    }
    sbCollapsableHTML.append(sb.toString());

    sbCollapsableHTML.append("</div>");

    return Optional.of(sbCollapsableHTML.toString());
  }

  public static String getHTMLForAnnotation(Query query) {
    return getHTMLForAnnotation(query, false);
  }

  public static String getHTMLForAnnotation(Query query, boolean needAddDotSgmToFilePath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<b>Target Entity Mentions:</b><br>");
    for (TextMention mention : query.mentions) {
      Optional<String> strHTML = HTMLHelper.getHTMLForAnnotation(mention, needAddDotSgmToFilePath);
      if (strHTML.isPresent()) {
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;" + strHTML.get() + "<br>");
      } else {
        System.err.println(
            "Can't generate HTML for query " + query.id + " mention " + mention.toString());
      }
    }
    return sb.toString();
  }

  public static Optional<String> getHTMLforAnntation(TextRelationMention rm) {
    return getHTMLforAnntation(rm, false);
  }

  public static Optional<String> getHTMLforAnntation(TextRelationMention rm,
      boolean needAddDotSgmToFilePath) {
                /*
                String strPathSerifXml;
		if(needAddDotSgmToFilePath)
			strPathSerifXml = Resources.getPathSerifXml() + rm.getDocId() + ".sgm.xml";
		else
			strPathSerifXml = Resources.getPathSerifXml() + rm.getDocId() + ".xml";
		*/

    String strPathSerifXml = Resources.getPathSerifXml() + rm.getDocId() + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + rm.getDocId() + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
    }

    String strPathSerifXmlVis =
        Resources.getDirSerifXmlVisualization() + rm.getDocId() + "\\index.html";

    int sent_char_start = rm.spanOfSent().get().getStart();
    int sent_char_end = rm.spanOfSent().get().getEnd();

    int agent1_start = rm.query.getSpan().getStart();
    int agent1_end = rm.query.getSpan().getEnd();

    int answer_start = rm.answer.getSpan().getStart();
    int answer_end = rm.answer.getSpan().getEnd();

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
//			File fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        return Optional.absent();
      }

      DocTheory dt = fromXML.loadFrom(fileSerifXml);
      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if (sentTheory.span().size() <= 0) {
          continue;
        }

        int sentStart = sentTheory.span().startToken().startCharOffset().value();
        int sentEnd = sentTheory.span().endToken().endCharOffset().value();

        if (sentStart <= sent_char_start && sentEnd >= sent_char_end) {
          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

          StringBuilder sb = new StringBuilder();

          System.out.println(
              "rm: query=" + rm.query.getSpan().toString() + ", answer=" + rm.answer.getSpan().toString()
                  + ", sent=" + rm.spanOfSent().toString());

          String str_agent1, str_answer;
          if (agent1_end - sentStart + 1 <= sentText.length()) {
            str_agent1 = sentText.substring(agent1_start - sentStart, agent1_end - sentStart + 1);
          } else {
            str_agent1 = sentText.substring(agent1_start - sentStart, sentText.length());
          }
          if (answer_end - sentStart + 1 <= sentText.length()) {
            str_answer = sentText.substring(answer_start - sentStart, answer_end - sentStart + 1);
          } else {
            str_answer = sentText.substring(answer_start - sentStart, sentText.length());
          }

          for (int i = 0; i < sentText.length(); i++) {
            if (i == agent1_start - sentStart)
            // sb.append("<b>");
            {
              sb.append("<b><font color = \"red\">");
            } else if (i == agent1_end - sentStart + 1)
//							sb.append("</b>");
            {
              sb.append("</font></b>");
            } else if (i == answer_start - sentStart)
//							sb.append("<b>");
            {
              sb.append("<b><font color = \"green\">");
            } else if (i == answer_end - sentStart + 1)
//							sb.append("</b>");
            {
              sb.append("</font></b>");
            }

            sb.append(sentText.charAt(i));
          }

          String linedText = getLinkedText(strPathSerifXmlVis, "SERIFXML");
          List<TextSpan> spans = new ArrayList<TextSpan>();
          spans.add(rm.query.getSpan());
          spans.add(rm.answer.getSpan());
//					String fullText = "dummy";
          String fullText = getCollapsableHTMLForSpans(spans, rm.getDocId(), global_divId++,
              needAddDotSgmToFilePath).get();
          return Optional
              .of("<hr><b>Relation</b>:<br><strong>" + rm.slot.toString() + "(<font color=\"red\">"
                  + str_agent1 + "</font>, <font color=\"green\">" + str_answer
                  + "</font>)</strong><br>" +
                  "Is this relation justified in the following sentence?&nbsp;&nbsp;(* If the two arguments overlap, then the red mention refers to both)<br><strong>Justification Sentence: </strong>"
                  + "&nbsp;&nbsp;&nbsp;&nbsp;" + sb.toString()
                  + " " + linedText + "&nbsp;" + fullText);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }

  public static String getLinkedText(String strURL, String text) {
    return "<a href=\"" + PathConverter.convertToWindows(strURL) + "\">" + text + "</a>";
  }

  public static Optional<String> getHTMLForAnnotation(TextMention mention) {
    return getHTMLForAnnotation(mention, false);
  }

  public static Optional<String> getHTMLForAnnotation(TextMention mention,
      boolean needAddDotSgmToFilePath) {
/*
                String strPathSerifXml;
		if(needAddDotSgmToFilePath)
			strPathSerifXml = Resources.getPathSerifXml() + mention.docId + ".sgm.xml";
		else
			strPathSerifXml = Resources.getPathSerifXml() + mention.docId + ".xml";
*/
    String strPathSerifXml = Resources.getPathSerifXml() + mention.getDocId() + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + mention.getDocId() + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
      if(!fileSerifXml.exists()) {
        strPathSerifXml = Resources.getPathSerifXml() + mention.getDocId() + ".serifxml";
        fileSerifXml = new File(strPathSerifXml);
      }
    }

    System.out.println("strPathSerifXml: " + strPathSerifXml);

    String strPathSerifXmlVis =
        Resources.getDirSerifXmlVisualization() + mention.getDocId() + "\\index.html";
//		int sent_char_start = rm.spanOfSent.get().getStart();
//		int sent_char_end = rm.spanOfSent.get().getEnd();

    int mention_start = mention.getSpan().getStart();
    int mention_end = mention.getSpan().getEnd();

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
//			File fileSerifXml = new File(strPathSerifXml);
      if (!fileSerifXml.exists()) {
        return Optional.absent();
      }

      DocTheory dt = fromXML.loadFrom(fileSerifXml);
      for(int sid=0; sid<dt.numSentences(); sid++) {
        SentenceTheory sentTheory = dt.sentenceTheory(sid);
//      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if(sentTheory.tokenSequence().size()<=0) {
//        if (sentTheory.span().size() <= 0) {
          continue;
        }

        int sentStart = sentTheory.span().startToken().startCharOffset().value();
        int sentEnd = sentTheory.span().endToken().endCharOffset().value();

        if (sentStart <= mention_start && sentEnd >= mention_end) {
          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

          StringBuilder sb = new StringBuilder();

          boolean isBoldTagClosed = true;
          for (int i = 0; i < sentText.length(); i++) {
            if (i == mention_start - sentStart) {
              sb.append("<b>");
              isBoldTagClosed = false;
            }
            sb.append(sentText.charAt(i));
            if (i == mention_end - sentStart + 1) {
              sb.append("</b>");
              isBoldTagClosed = true;
            }
          }

          // close the <b> tag
          if (!isBoldTagClosed) {
            sb.append("</b>");
          }

          String linedText = getLinkedText(strPathSerifXmlVis, "SERIFXML");
          List<TextSpan> spans = new ArrayList<TextSpan>();
          spans.add(mention.getSpan());
          String fullText = getCollapsableHTMLForSpans(spans, mention.getDocId(), global_divId++,
              needAddDotSgmToFilePath).get();
//					String fullText = "dummy";
          return Optional.of("" + sb.toString() + " " + linedText + "&nbsp;" + fullText);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return Optional.absent();
  }

  public static String getHTML(CSAssessment assessment,
      TextMention queryMention,
      String eid,
      String entityHTMLCode, String eclassHTMLCode) {

    return getHTML(assessment,
        queryMention,
        eid,
        entityHTMLCode, eclassHTMLCode,
        0,
        false, "dummy");
  }

  public static String getHTML(CSAssessment assessment,
      TextMention queryMention,
      String eid,
      String entityHTMLCode, String eclassHTMLCode,
      int assessment_idx, boolean isAugmentedOutput, String querySystemEntityId) {
    StringBuilder sb = new StringBuilder();

    sb.append("<td>");
    int assessment_uid = assessment.uid.isPresent() ? assessment.uid.get() : assessment_idx;
    sb.append(assessment_uid);
    sb.append("</td>");

    sb.append("<td>");
    sb.append(assessment.responseId);
    sb.append("</td>");

    sb.append("<td>");
    System.out.println("===");
    System.out.println(eid);

    String queryPathSerifHTML = Resources.getPathSerifXmlVisualization(queryMention.getDocId());

    sb.append(
        eid + ":" + queryMention.getText().get()
            + "<br>" + "<a href=\"" + queryPathSerifHTML + "\">"
            + queryMention.getDocId() + "</a>" + HTMLHelper.getHTML(queryMention));
    sb.append("</td>");

    sb.append("<td>");
    sb.append(entityHTMLCode);
    sb.append("</td>");

    sb.append("<td>");
    sb.append(assessment.slot.toString());
    sb.append("</td>");

    sb.append("<td>");
    String assessmentPathSerifHTML = Resources.getPathSerifXmlVisualization(assessment.getDocId());
    sb.append(
        assessment.text + "<br>" + "<a href=\"" + assessmentPathSerifHTML + "\">" + assessment.docid
            + "</a>" + "(" + Integer.toString(assessment.spanOfFiller.getStart()) + "," + Integer
            .toString(assessment.spanOfFiller.getEnd()) + ")" + HTMLHelper
            .getAnnotatedSentText(assessment));
    sb.append("</td>");

    if (isAugmentedOutput) {
      String docID = assessment.docid;
      int filler_beg = assessment.spanOfFiller.getStart();
      int filler_end = assessment.spanOfFiller.getEnd();
      String relationString = getHTMLForRelations(docID, filler_beg, filler_end);
      String fillerCorefString = getHTMLForFillerCoref(docID, filler_beg, filler_end);
      String queryCorefString =
          getHTMLForQueryCoref(docID, querySystemEntityId, filler_beg, filler_end);

      sb.append("<td>");
      sb.append(queryCorefString);
      sb.append("</td>");

      sb.append("<td>");
      sb.append(fillerCorefString);
      sb.append("</td>");

      sb.append("<td>");
      sb.append(relationString);
      sb.append("</td>");
    } else {
      sb.append("<td>");
      sb.append(assessment.judgement1);
      sb.append("</td>");

      sb.append("<td>");
      sb.append(assessment.judgement2);
      sb.append("</td>");

      sb.append("<td>");
      sb.append(eclassHTMLCode);
      sb.append("</td>");
    }
    return sb.toString();
  }

  public static Optional<TextMention> getCanonicalMentionByDocID(
      List<TextMention> canonicalMentions, String docID) {
    for (TextMention m : canonicalMentions) {
      if (m.getDocId().equals(docID)) {
        return Optional.of(m);
      }
    }

    return Optional.absent();
  }

  public static Optional<Entity> getEntityByMention(DocTheory docTheory,
      int mention_begin, int mention_end) {

    TextSpan span = new TextSpan(mention_begin, mention_end);
    for (int i = 0; i < docTheory.entities().numEntities(); i++) {
      Entity e = docTheory.entities().entity(i);
      for (Mention m : e.mentions()) {
//				if(span.overlapWith(new Span(m.span().startCharOffset().value(), m.span().endCharOffset().value()))) {
        if (span.overlapWith(new TextSpan(m.atomicHead().span().startCharOffset().value(),
            m.atomicHead().span().endCharOffset().value()))) {
          return Optional.of(e);
        }
      }
    }

    return Optional.absent();
  }

  public static String getHTMLForQueryCoref(String docID, String sysEntityID,
      int filler_beg, int filler_end) {
    String ret = "NA";

    List<TextMention> canonicalMentions =
        SystemKbMentionLoader.hop0queryId2listCanonicalMention.get(sysEntityID);

    Optional<TextMention> canonicalMention = getCanonicalMentionByDocID(canonicalMentions, docID);
    Optional<DocTheory> docTheory = DocTheoryLoader.getDocTheory(docID);

    if (!canonicalMention.isPresent() || !docTheory.isPresent()) {
      return ret;
    }
    Optional<Entity> entity = getEntityByMention(docTheory.get(),
        canonicalMention.get().getSpan().getStart(), canonicalMention.get().getSpan().getEnd());
    if (!entity.isPresent()) {
      return ret;
    }

    return getBriefTextWithEntityMentionsInBold(docTheory.get(), entity.get());
  }

  public static String getHTMLForFillerCoref(String docID,
      int filler_beg, int filler_end) {
    String ret = "NA";

    Optional<DocTheory> docTheory = DocTheoryLoader.getDocTheory(docID);
    if (!docTheory.isPresent()) {
      return ret;
    }

    Optional<Entity> entity = getEntityByMention(docTheory.get(), filler_beg, filler_end);
    if (!entity.isPresent()) {
      return ret;
    }

    return getBriefTextWithEntityMentionsInBold(docTheory.get(), entity.get());
  }

  public static String getHTMLForRelations(String docID,
      int filler_beg, int filler_end) {
    String ret = "";

    Optional<DocTheory> docTheory = DocTheoryLoader.getDocTheory(docID);
    if (!docTheory.isPresent()) {
      return "NA";
    }

    for (int i = 0; i < docTheory.get().numSentences(); i++) {
      SentenceTheory sentTheory = docTheory.get().sentenceTheory(i);
      if (sentTheory.tokenSequence().isEmpty()) {
        continue;
      }

      TextSpan sentSpan = new TextSpan(sentTheory.span().startCharOffset().value(),
          sentTheory.span().endCharOffset().value());
      TextSpan fillerSpan = new TextSpan(filler_beg, filler_end);

      if (sentSpan.overlapWith(fillerSpan)) {
        if (!SystemKbRelationLoader.doc2relations.containsKey(docID)) {
          continue;
        }
        for (RelationFromSubmission relation : SystemKbRelationLoader.doc2relations.get(docID)) {
          if (relation.justifications.size() == 1) {
            TextSpan justificationSpan = new TextSpan(relation.justifications.get(0).span.getFirst(),
                relation.justifications.get(0).span.getSecond());
            if (sentSpan.overlapWith(justificationSpan)) {
              String surroundingText =
                  getSurroundingTextForRelationJustification(docTheory.get(), justificationSpan,
                      relation.slot.toString());
              ret += "- " + surroundingText + "<br>";
            }
          }
        }

        break;
      }
    }

    if (ret.endsWith("||")) {
      return ret.substring(0, ret.length() - 2);
    } else {
      return ret;
    }
  }

  public static String getSurroundingTextForRelationJustification(DocTheory docTheory,
      TextSpan justificationSpan, String relationType) {
    int SIZE_WINDOW_LEFT = 20;
    int SIZE_WINDOW_RIGHT = 20;

    int start = justificationSpan.getStart() - SIZE_WINDOW_LEFT;
    int end = justificationSpan.getEnd() + SIZE_WINDOW_RIGHT;

    start = start < 0 ? 0 : start;
    end = end >= docTheory.document().originalText().referenceBounds().endCharOffsetInclusive().asInt() ? docTheory
        .document().originalText().referenceBounds().endCharOffsetInclusive().asInt() : end;

    StringBuilder sb = new StringBuilder();
    for (int idx = start; idx < end; idx++) {
      // char c = docTheory.document().originalText().get().toString().charAt(idx);
      char c = DocTheoryLoader.getTextTagsEscaped(docTheory.document().originalText().content().utf16CodeUnits())
          .charAt(idx);

      if (idx == justificationSpan.getStart()) {
        sb.append("<b>");
      }
      sb.append(c);
      if (idx == justificationSpan.getEnd()) {
        sb.append("(" + relationType + ")");
        sb.append("</b>");
      }
    }

    return sb.toString();
  }

  public static String getBriefTextWithEntityMentionsInBold(DocTheory docTheory, Entity e) {
    StringBuilder sb = new StringBuilder();
    for (Mention m : e.mentions()) {
      sb.append("- " + getSurroundingTextForMention(docTheory, m) + "<br>");
    }

    return sb.toString();
  }

  public static String getSurroundingTextForMention(DocTheory docTheory, Mention m) {
    int SIZE_WINDOW_LEFT = 20;
    int SIZE_WINDOW_RIGHT = 20;

    int start = m.span().startCharOffset().value() - SIZE_WINDOW_LEFT;
    int end = m.span().endCharOffset().value() + SIZE_WINDOW_RIGHT;

    start = start < 0 ? 0 : start;
    end = end >= docTheory.document().originalText().referenceBounds().endCharOffsetInclusive().asInt() ? docTheory
        .document().originalText().referenceBounds().endCharOffsetInclusive().asInt() : end;

    StringBuilder sb = new StringBuilder();
    for (int idx = start; idx < end; idx++) {

      // char c = docTheory.document().originalText().get().toString().charAt(idx);
      char c = DocTheoryLoader.getTextTagsEscaped(docTheory.document().originalText().content().utf16CodeUnits())
          .charAt(idx);

      if (idx == m.span().startCharOffset().value()) {
        sb.append("<b>");
      }
      sb.append(c);
      if (idx == m.span().endCharOffset().value()) {
        sb.append("(" + docTheory.entityByMention(m).get().type().toString() + "," + m.mentionType()
            + ")");
        sb.append("</b>");
      }
    }

    return sb.toString();
  }

  public static String getHTML(CSAssessment assessment,
      CSInitQuery query, String entityHTMLCode, String eclassHTMLCode) {
    return getHTML(assessment,
        query, entityHTMLCode, eclassHTMLCode,
        0,
        false, "dummy");
  }

  public static String getHTML(CSAssessment assessment,
      CSInitQuery query, String entityHTMLCode, String eclassHTMLCode,
      int idx, boolean isAgumentedOutput, String querySystemEntityId) {
    return getHTML(assessment, query.mentions.get(0), query.id, entityHTMLCode, eclassHTMLCode,
        idx, isAgumentedOutput, querySystemEntityId);
  }

  public static String getAnnotatedSentText(CSAssessment assessment) {
    return getAnnotatedSentText(assessment, false);
  }

  public static String getAnnotatedSentText(CSAssessment assessment,
      boolean needAddDotSgmToFilePath) {
    String ret = "";
/*
		String strPathSerifXml;
		if(needAddDotSgmToFilePath)
			strPathSerifXml = Resources.getPathSerifXml() + assessment.docid + ".sgm.xml";
		else
			strPathSerifXml = Resources.getPathSerifXml() + assessment.docid + ".xml";
*/

    Optional<DocTheory> docTheory = DocTheoryLoader.getDocTheory(assessment.docid);
    if (!docTheory.isPresent()) {
      System.out.println("Error loading document: " + assessment.docid);
      return ret;
    }
    DocTheory dt = docTheory.get();

    int filler_beg = assessment.spanOfFiller.getStart();
    int filler_end = assessment.spanOfFiller.getEnd();

    for(int sid=0; sid<dt.numSentences(); sid++) {
      SentenceTheory sentTheory = dt.sentenceTheory(sid);
      if (sentTheory.tokenSequence().size() <= 0) {
        continue;
      }

      int sentStart = sentTheory.span().startToken().startCharOffset().value();
      int sentEnd = sentTheory.span().endToken().endCharOffset().value();

      if (sentStart <= filler_beg && sentEnd >= filler_end) {
        String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);
        String str1 = sentText.substring(0, filler_beg - sentStart);
        String str2 = sentText.substring(filler_beg - sentStart, filler_end - sentStart);
        String str3 = sentText.substring(filler_end - sentStart);
        ret = str1 + "<u><b>" + str2 + "</b></u>" + str3;
        break;
      }
    }

    return ret;
  }

  public static String getHTML(CSInitQuery query) {
    return getHTML(query, false);
  }

  public static String getHTML(CSInitQuery query, boolean needAddDotSgmToFilePath) {
    return getHTML(query.mentions.get(0), needAddDotSgmToFilePath);
  }

  public static String getHTML(TextMention queryMention) {
    return getHTML(queryMention, false);
  }

  public static String getHTML(TextMention queryMention, boolean needAddDotSgmToFilePath) {
    String ret = "";

    int beg = queryMention.getSpan().getStart();
    int end = queryMention.getSpan().getEnd();

    String strPathSerifXml = Resources.getPathSerifXml() + queryMention.getDocId() + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + queryMention.getDocId() + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
    }

    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + queryMention.getDocId() + ".serifxml.xml";
      fileSerifXml = new File(strPathSerifXml);
    }
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + queryMention.getDocId() + ".mpdf.serifxml.xml";
      fileSerifXml = new File(strPathSerifXml);
    }


    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strPathSerifXml));
      for(int sid=0; sid<dt.numSentences(); sid++) {
        SentenceTheory sentTheory = dt.sentenceTheory(sid);
//      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if (sentTheory.tokenSequence().size() <= 0) {
          continue;
        }
        int sentStart = sentTheory.tokenSpan().startToken().startCharOffset().value();
        int sentEnd = sentTheory.tokenSpan().endToken().endCharOffset().value();

        if (sentStart <= beg && sentEnd >= end) {

          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

          String str1 = sentText.substring(0, beg - sentStart);
          String str2 = sentText.substring(beg - sentStart, end - sentStart);
          String str3 = sentText.substring(end - sentStart);
          ret = str1 + "<u><b>" + str2 + "</b></u>" + str3;
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return ret;
  }

  public static String getTextOfMention(TextMention kbMention) {
    String ret = "NA";

    int beg = kbMention.getSpan().getStart();
    int end = kbMention.getSpan().getEnd();

    String strPathSerifXml = Resources.getPathSerifXml() + kbMention.getDocId() + ".xml";
    File fileSerifXml = new File(strPathSerifXml);
    if (!fileSerifXml.exists()) {
      strPathSerifXml = Resources.getPathSerifXml() + kbMention.getDocId() + ".sgm.xml";
      fileSerifXml = new File(strPathSerifXml);
    }

    try {
      SerifXMLLoader fromXML = SerifXMLLoader.fromStandardACETypes();
      DocTheory dt = fromXML.loadFrom(new File(strPathSerifXml));
      for(int sid=0; sid<dt.numSentences(); sid++) {
        SentenceTheory sentTheory = dt.sentenceTheory(sid);
//      for (SentenceTheory sentTheory : dt.sentenceTheories()) {
        if (sentTheory.tokenSequence().size() <= 0) {
          continue;
        }
        int sentStart = sentTheory.tokenSpan().startToken().startCharOffset().value();
        int sentEnd = sentTheory.tokenSpan().endToken().endCharOffset().value();

        if (sentStart <= beg && sentEnd >= end) {

          String sentText = dt.document().originalText().content().utf16CodeUnits().substring(sentStart, sentEnd);

//        String str1 = sentText.substring(0, beg - sentStart);
          String str2 = sentText.substring(beg - sentStart, end - sentStart);
//        String str3 = sentText.substring(end - sentStart);
//          ret = str1 + "***" + str2 + "***" + str3;
          ret = str2;
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      //code to handle an IOException here
      System.out.println("Exception in reading: " + strPathSerifXml);
    }

    return ret;
  }

  public static String getHTMLVisMainHeader() {
    StringBuilder sb = new StringBuilder();

    sb.append("<head>");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
    sb.append("<link href=\"css/jquery.dataTables.css\" rel=\"stylesheet\" type=\"text/css\" />");
    sb.append("<script type=\"text/javascript\" src=\"js/jquery.js\"></script>");
    sb.append("<script type=\"text/javascript\" src=\"js/jquery.dataTables.js\"></script>");
    sb.append("<script>");
    sb.append("$(document).ready( function () {");
    sb.append("var table = $('#tbl1').DataTable({");
    sb.append("\"aLengthMenu\": [[10, 25, 50, 100, -1], [10, 25, 50, 100, \"All\"]],");
    sb.append("\"bPaginate\": false,");
    sb.append("\"sDom\": '<\"top\"lif<\"clear\">>rt<\"bottom\"ipl<\"clear\">>'");
    sb.append("});");
    sb.append("} );");
    sb.append("</script>");
    sb.append("<title>KBP Cold Start " + Resources.getEvalYear() + "</title>");
    sb.append("</head>");

    return sb.toString();
  }
}
