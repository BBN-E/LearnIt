package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelation;

import java.util.Comparator;

public class RelationCountsComparator implements Comparator<TextRelation> {

  @Override
  public int compare(TextRelation arg0, TextRelation arg1) {

    if (!arg0.sourcePatterns.isPresent() || !arg1.sourcePatterns.isPresent()) {
      return 0;
    }

    return arg1.sourcePatterns.get().size() - arg0.sourcePatterns.get().size();
  }
}
