package com.bbn.akbc.common;

import com.google.common.base.Optional;

public class Justification {

  public String docId;
  public Pair<Integer, Integer> span;

  public Optional<String> textForVis = Optional.absent();

  public Optional<String> sourcePattern = Optional.absent();
  public Optional<String> queryBrandyConfidence = Optional.absent();
  public Optional<String> answerBrandyConfidence = Optional.absent();


  public Justification(String docId, Pair<Integer, Integer> span) {
    this.docId = docId;
    this.span = span;
  }

  public static Justification fromString(String sline) {
    return fromString(sline, false);
  }
  public static Justification fromString(String sline, boolean adjust_offsets_for_adept) {
    String[] tmpVar1 = sline.split(":");

    String docId = tmpVar1[0];

    String[] tmpVar2 = tmpVar1[1].split("-");

    int start = Integer.parseInt(tmpVar2[0]);
    int end = Integer.parseInt(tmpVar2[1]);

    if(adjust_offsets_for_adept)
      return new Justification(docId, new Pair<Integer, Integer>(start, end+1));
    else
      return new Justification(docId, new Pair<Integer, Integer>(start, end));
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(docId.toString() + ", " + span.toString());
    if (sourcePattern.isPresent()) {
      sb.append(", " + sourcePattern.get());
    }
    if (queryBrandyConfidence.isPresent()) {
      sb.append(", " + queryBrandyConfidence.get());
    }
    if (answerBrandyConfidence.isPresent()) {
      sb.append(", " + answerBrandyConfidence.get());
    }
    return sb.toString();
  }

  public void setTextForVis(String str) {
    textForVis = Optional.of(str);
  }

  public void setQueryBrandyConfidence(String conf) {
    queryBrandyConfidence = Optional.of(conf);
  }

  public void setAnswerBrandyConfidence(String conf) {
    answerBrandyConfidence = Optional.of(conf);
  }

  public void setSourcePattern(String source) {
    sourcePattern = Optional.of(source);
  }

  public String toSimpleString() {
    return "[J= " + docId + ", " + span.toString() + ", " +
        sourcePattern.get() + ", " + queryBrandyConfidence.get() + ", " + answerBrandyConfidence
        .get() + " ]";
  }

  public boolean covers(Justification justification) {
    if(this.docId.equals(justification.docId)) {
      if(this.span.getFirst()<=justification.span.getFirst()
          && this.span.getSecond()>=justification.span.getSecond())
        return true;
    }

    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + docId.hashCode();
    result = prime * result + span.getFirst();
    result = prime * result + span.getSecond();

    if (this.sourcePattern.isPresent()) {
      result = prime * result + this.sourcePattern.get().hashCode();
    }

    if (this.queryBrandyConfidence.isPresent()) {
      result = prime * result + this.queryBrandyConfidence.get().hashCode();
    }

    if (this.answerBrandyConfidence.isPresent()) {
      result = prime * result + this.answerBrandyConfidence.get().hashCode();
    }

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
    Justification other = (Justification) obj;
    if (!docId.equals(other.docId)) {
      return false;
    }
    if (!span.equals(other.span)) {
      return false;
    }

    if (this.sourcePattern.isPresent() && other.sourcePattern.isPresent()) {
      if (!this.sourcePattern.get().equals(other.sourcePattern.get())) {
        return false;
      }
    }

    if (this.queryBrandyConfidence.isPresent() && other.queryBrandyConfidence.isPresent()) {
      if (!this.queryBrandyConfidence.get().equals(other.queryBrandyConfidence.get())) {
        return false;
      }
    }

    if (this.answerBrandyConfidence.isPresent() && other.answerBrandyConfidence.isPresent()) {
      if (!this.answerBrandyConfidence.get().equals(other.answerBrandyConfidence.get())) {
        return false;
      }
    }

    return true;
  }
}
