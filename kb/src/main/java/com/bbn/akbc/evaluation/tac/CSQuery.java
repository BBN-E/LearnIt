package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextMention;
import com.google.common.base.Optional;

import java.util.List;

public class CSQuery extends Query {

  public CSQuery(String id, List<TextMention> mentions,
      Optional<String> textForAnnotation) {
    super(id, mentions, textForAnnotation);
  }
}
