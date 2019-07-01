package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextEntity;

import java.util.Comparator;

/**
 * Created by bmin on 3/14/16.
 */
public class EntityNumberOfMentionsComparator implements Comparator<TextEntity> {
  @Override
  public int compare(TextEntity arg0, TextEntity arg1) {
    double ret = arg0.getMentions().size() - arg1.getMentions().size();
    return (int) ret;
  }
}
