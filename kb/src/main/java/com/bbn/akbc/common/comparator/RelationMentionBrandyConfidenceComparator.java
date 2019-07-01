package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.common.MentionConfidences;

import java.util.Comparator;

public class RelationMentionBrandyConfidenceComparator implements Comparator<TextRelationMention> {

  @Override
  public int compare(TextRelationMention arg0, TextRelationMention arg1) {
    System.out.println("arg0.query.getBrandyConfidence().get(): " + arg0.query.getBrandyConfidence().get());
    System.out.println("arg1.query.getBrandyConfidence().get(): " + arg1.query.getBrandyConfidence().get());

    System.out.println("arg0.answer.getBrandyConfidence().get(): " + arg0.answer.getBrandyConfidence().get());
    System.out.println("arg1.answer.getBrandyConfidence().get(): " + arg1.answer.getBrandyConfidence().get());

    return (MentionConfidences.brandyConfidenceString2confidenceLevel
                .get(arg1.query.getBrandyConfidence().get()) -
                MentionConfidences.brandyConfidenceString2confidenceLevel
                    .get(arg0.query.getBrandyConfidence().get())) +
        (MentionConfidences.brandyConfidenceString2confidenceLevel
             .get(arg1.answer.getBrandyConfidence().get()) -
             MentionConfidences.brandyConfidenceString2confidenceLevel
                 .get(arg0.answer.getBrandyConfidence().get()));
  }
}
