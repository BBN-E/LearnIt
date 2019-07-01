package com.bbn.akbc.kb.facts;

import com.bbn.akbc.common.format.Normalization;

/**
 * Created by bmin on 9/18/15.
 */
public class FactMention {
  String srcId;
  String mentionText;
  String docId;
  int start;
  int end;
  double confidence;
  double linkConfidence;
  String brandyConfidence;

  FactMention(String srcId,
      String mentionText,
      String docId,
      int start,
      int end,
      double confidence,
      double linkConfidence,
      String brandyConfidence) {

    this.srcId = srcId;
    this.mentionText = mentionText;
    this.docId = docId;
    this.start = start;
    this.end = end;
    this.confidence = confidence;
    this.linkConfidence = linkConfidence;
    this.brandyConfidence = brandyConfidence;
  }

  public FactMention fromLine(String sline) {
    String[] fields = sline.trim().split("\t");

    String srcId = fields[0];
    srcId = Normalization.convertIDtoOnlyHaveAsciiCharacter(srcId); // normalize for submission

    String predicate = fields[1];

    String mentionText = fields[2];

    String docId = fields[3];
    int start = Integer.parseInt(fields[4]);
    int end = Integer.parseInt(fields[5]);

    confidence = Double.parseDouble(fields[6]);
    linkConfidence = Double.parseDouble(fields[7]);
    brandyConfidence = fields[8].trim();

    return new FactMention(srcId, mentionText, docId, start, end,
        confidence, linkConfidence, brandyConfidence);
  }
}
