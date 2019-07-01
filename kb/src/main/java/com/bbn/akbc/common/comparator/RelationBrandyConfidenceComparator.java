package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelation;
import com.bbn.akbc.common.MentionConfidences;

import java.util.Comparator;


public class RelationBrandyConfidenceComparator implements Comparator<TextRelation> {

  @Override
  public int compare(TextRelation arg0, TextRelation arg1) {
    return MentionConfidences.brandyConfidenceString2confidenceLevel
        .get(arg1.getBrancyConfidenceOfFiller().get()) -
        MentionConfidences.brandyConfidenceString2confidenceLevel
            .get(arg0.getBrancyConfidenceOfFiller().get());
  }
}
