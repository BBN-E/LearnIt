package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.common.FactConfidence;

import java.util.Comparator;

public class RelationMentionSourcePatternComparator implements Comparator<TextRelationMention> {

  @Override
  public int compare(TextRelationMention arg0, TextRelationMention arg1) {

    double ret =
        100.0 * (FactConfidence.getSourcePatternConf(arg1.sourcePattern.get()) - FactConfidence
            .getSourcePatternConf(arg0.sourcePattern.get()));
    return (int) ret;
  }
}
