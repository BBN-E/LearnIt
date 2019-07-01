package com.bbn.akbc.evaluation.tac;


import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.HTMLHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by bmin on 11/8/15.
 */
public class AssessmentTextPrinter {
  public static void write(String fileTextOutput,
      List<CSAssessment> assessments,
      Map<String, String> queryToEntityInSysOut,
      Map<String, List<TextMention>> id2listCanonicalMention,
      Map<String, CSInitQuery> id2queryEntities,
      Map<String, TextMention> hop1Query2Mention) throws IOException {

    PrintWriter pw = new PrintWriter(new FileWriter(fileTextOutput));

    StringBuilder sb = new StringBuilder();

    for (int idx = 0; idx < assessments.size(); idx++) {
      CSAssessment assessment = assessments.get(idx);

      writeAssessment(queryToEntityInSysOut,
          id2listCanonicalMention,
          id2queryEntities,
          hop1Query2Mention,
          assessment,
          pw);
    }

    pw.close();
  }

  static int MAX_CANONICAL_MENTION_PRINT=3;
  static String getEntityCanonicalNames(String queryId,
      Map<String, List<TextMention>> id2listCanonicalMention,
      Map<String, TextMention> hop1Query2Mention) {
    StringBuilder sb = new StringBuilder();

    if(hop1Query2Mention.containsKey(queryId)) {
      sb.append(HTMLHelper.getTextOfMention(hop1Query2Mention.get(queryId)) + "|||");
    }
    else if(id2listCanonicalMention.containsKey(queryId)){
      sb.append("|||");

      List<TextMention> canonicalMentions =
          new ArrayList<TextMention>(id2listCanonicalMention.get(queryId));
      if(canonicalMentions.size() > MAX_CANONICAL_MENTION_PRINT)
        canonicalMentions = canonicalMentions.subList(0, MAX_CANONICAL_MENTION_PRINT);
      for (TextMention kbMention : canonicalMentions) {
        sb.append("\t" + kbMention.getText().get());
      }
    }
    return sb.toString().replace("\t", "|");
  }

  static void writeAssessment(Map<String, String> queryToEntityInSysOut,
      Map<String, List<TextMention>> id2listCanonicalMention,
      Map<String, CSInitQuery> id2queryEntities,
      Map<String, TextMention> hop1Query2Mention,

      CSAssessment assessment,
      PrintWriter pw) throws IOException {

    StringBuilder sb = new StringBuilder();

    sb.append(assessment.responseId + "\t");

    String entityNameText = getEntityCanonicalNames(assessment.queryId,
        id2listCanonicalMention, hop1Query2Mention);
    sb.append(assessment.queryId + ":" + entityNameText + "\t");

    sb.append(assessment.slot.toString() + "\t");

    String textOfFiller = assessment.text + "|" +
        HTMLHelper.getTextOfMention(new TextMention(assessment.docid,
            assessment.spanOfFiller.getStart(), assessment.spanOfFiller.getEnd()));
    sb.append(textOfFiller + "\t");


    String docID = assessment.docid;
    int filler_beg = assessment.spanOfFiller.getStart();
    int filler_end = assessment.spanOfFiller.getEnd();
    String relationString = HTMLHelper.getHTMLForRelations(docID, filler_beg, filler_end);
    String fillerCorefString = HTMLHelper.getHTMLForFillerCoref(docID, filler_beg, filler_end);

    sb.append(fillerCorefString + "\t");
    sb.append(relationString + "\t");
    sb.append(assessment.judgement1 + "\t");
    sb.append(assessment.judgement2 + "\t");

    sb.append(assessment.equivalent_class_str);

    pw.println(sb.toString());
  }
}
