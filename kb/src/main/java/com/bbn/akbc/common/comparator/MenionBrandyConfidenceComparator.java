package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.common.MentionConfidences;

import java.util.Comparator;

public class MenionBrandyConfidenceComparator implements Comparator<TextMention> {

  @Override
  public int compare(TextMention arg0, TextMention arg1) {
    return
        MentionConfidences.brandyConfidenceString2confidenceLevel.get(arg1.getBrandyConfidence().get()) -
            MentionConfidences.brandyConfidenceString2confidenceLevel.get(arg0.getBrandyConfidence().get());
  }
}
