package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextMention;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.List;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class Query {

  @JsonProperty
  public String id;
  @JsonProperty
  public List<TextMention> mentions;

  @JsonProperty
  public Optional<String> textForAnnotation = Optional.absent();

  public Query(
      String id,
      List<TextMention> mentions,
      Optional<String> textForAnnotation) {
    this.id = id;
    this.mentions = mentions;
    this.textForAnnotation = textForAnnotation;
  }

  public void setTextForAnnotation(String textForAnnotation) {
    this.textForAnnotation = Optional.of(textForAnnotation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode(); // id has to be unique
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Query other = (Query) obj;
    if (!other.id.equals(this.id)) {
      return false;
    }
    return true;
  }
}
