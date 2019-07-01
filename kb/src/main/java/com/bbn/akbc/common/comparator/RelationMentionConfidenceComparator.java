package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelationMention;

import java.util.Comparator;

/**
 * Created by bmin on 7/26/17.
 */
public final class RelationMentionConfidenceComparator implements Comparator<TextRelationMention> {

  @Override
  public int compare(TextRelationMention arg0, TextRelationMention arg1) {

    if(arg1.getConfidence().get() > arg0.getConfidence().get())
      return 1;
    else if(arg1.getConfidence().get() < arg0.getConfidence().get())
      return -1;
    else
      return 0;
  }
}
