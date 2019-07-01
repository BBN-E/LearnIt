package com.bbn.akbc.kb.facts;

/**
 * Created by bmin on 9/21/15.
 */
public class FactCanonicalMention extends FactMention {

  FactCanonicalMention(String srcId,
      String mentionText,
      String docId,
      int start,
      int end) {
    super(srcId, mentionText, docId, start, end,
        1.0, 1.0, "AnyName");
  }
}
