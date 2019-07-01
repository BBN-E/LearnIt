package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextMention;

import java.util.Comparator;

/*
 * pick a "best" canonical_mention from a list of canonical_mentions in the same document
 */

public class CanonicalMentionInfoComparator implements Comparator<TextMention> {

  @Override
  public int compare(TextMention arg0, TextMention arg1) {

    int length0 = arg0.getSpan().getLength();
    int length1 = arg1.getSpan().getLength();

    return length1 - length0;
  }
}
