package com.bbn.akbc.common.comparator;

import com.bbn.akbc.common.Justification;
import com.bbn.akbc.common.MentionConfidences;

import java.util.Comparator;

public class JustificationBrandyComparator implements Comparator<Justification> {

  @Override
  public int compare(Justification arg0, Justification arg1) {
    if(!MentionConfidences.brandyConfidenceString2confidenceLevel.containsKey(arg0.queryBrandyConfidence.get())
        && !MentionConfidences.brandyConfidenceString2confidenceLevel.containsKey(arg1.queryBrandyConfidence.get()))
      return 0;
    if(!MentionConfidences.brandyConfidenceString2confidenceLevel.containsKey(arg0.queryBrandyConfidence.get()))
      return -1;
    else if(!MentionConfidences.brandyConfidenceString2confidenceLevel.containsKey(arg1.queryBrandyConfidence.get()))
      return 1;


    return (MentionConfidences.brandyConfidenceString2confidenceLevel
                .get(arg1.queryBrandyConfidence.get()) -
                MentionConfidences.brandyConfidenceString2confidenceLevel
                    .get(arg0.queryBrandyConfidence.get())) +
        (MentionConfidences.brandyConfidenceString2confidenceLevel
             .get(arg1.answerBrandyConfidence.get()) -
             MentionConfidences.brandyConfidenceString2confidenceLevel
                 .get(arg0.answerBrandyConfidence.get()));
  }
}
