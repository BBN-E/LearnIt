package com.bbn.akbc.kb.text;

import com.bbn.akbc.common.Justification;
import com.bbn.akbc.common.Pair;
import com.bbn.akbc.common.StringUtil;
import com.bbn.akbc.evaluation.tac.CSInitQuery;
import com.bbn.akbc.kb.common.Slot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;


// import com.bbn.annotation.questions.SelectQuestion;

/*
 * (Bonan) WARNING!!!
 *  - to temporarily remove Maven cyclic dependencies (coldstart -> annotation -> coldstart/neolearnit),
 *    commented out everything related to annotation.
 *
 *
 */
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TextRelationMention {

  @JsonProperty
  public TextMention query;
  @JsonProperty
  public TextMention answer;
  @JsonProperty
  public Slot slot;

  public List<Justification> listJustifications;

  @JsonProperty
  public Optional<Integer> hopId = Optional.absent();
  @JsonProperty
  public Optional<Boolean> inSysKB = Optional.absent();
  @JsonProperty
  public Optional<String> sourcePattern = Optional.absent();
  @JsonProperty
  public Optional<Double> confidence = Optional.absent();

  @JsonProperty
  public Optional<String> judgementOnRelation = Optional.absent();
  @JsonProperty
  public Optional<String> judgementOnQuery = Optional.absent();

  @JsonProperty
  private TextSpan spanOfSent;

  Optional<String> stringJustification = Optional.absent();

  @JsonCreator
  public TextRelationMention(
      @JsonProperty("query") TextMention query,
      @JsonProperty("answer") TextMention answer,
      @JsonProperty("slot") Slot slot,
      @JsonProperty("hopId") Optional<Integer> hopId,
      @JsonProperty("inSysKB") Optional<Boolean> inSysKB,
      @JsonProperty("sourcePattern") Optional<String> sourcePattern,
      @JsonProperty("confidence") Optional<Double> confidence,
      @JsonProperty("judgementOnRelation") Optional<String> judgementOnRelation,
      @JsonProperty("judgementOnQuery") Optional<String> judgementOnQuery,
      @JsonProperty("spanOfSent") TextSpan spanOfSent,
      @JsonProperty("isValueSlot") Optional<Boolean> isValueSlot,
//    @JsonProperty("questionForAnntation") SelectQuestion questionForAnntation,
      @JsonProperty("evalQuery") CSInitQuery evalQuery) {

    System.out.println("@JsonCreator RelationMention slot=" + slot.toString());

    this.query = query;
    this.answer = answer;
    this.slot = slot;
    this.hopId = hopId;
    this.inSysKB = inSysKB;
    this.sourcePattern = sourcePattern;
    this.confidence = confidence;
    this.judgementOnRelation = judgementOnRelation;
    this.judgementOnQuery = judgementOnQuery;
    this.spanOfSent = spanOfSent;
    this.isValueSlot = isValueSlot;
//  this.questionForAnntation = questionForAnntation;
    this.evalQuery = evalQuery;

    this.listJustifications = new ArrayList<Justification>();
  }

  public TextRelationMention(TextMention query, TextMention answer, Slot slot) {
    this.query = query;
    this.answer = answer;
    this.slot = slot;

    this.listJustifications = new ArrayList<Justification>();
  }


  public Optional<String> getStringJustification() {
    return stringJustification;
  }

  public void setStringJustification(String strJustification) {
    stringJustification = Optional.of(strJustification);
  }

  public Justification getJustificaion() {
    if(listJustifications.isEmpty()) {
      int start = query.getSpan().getStart() < answer.getSpan().getStart() ?
                  query.getSpan().getStart() : answer.getSpan().getStart();
      int end = query.getSpan().getEnd() > answer.getSpan().getEnd() ?
                query.getSpan().getEnd() : answer.getSpan().getEnd();

      listJustifications = new ArrayList<Justification>();

      Justification j = new Justification(this.getDocId(), new Pair<Integer, Integer>(start, end));
      listJustifications.add(j);
    }
    return listJustifications.get(0);
  }

  public void fillInJustificationForSingleSenteceFact() {
    int start = query.getSpan().getStart() < answer.getSpan().getStart() ?
                query.getSpan().getStart() : answer.getSpan().getStart();
    int end = query.getSpan().getEnd() > answer.getSpan().getEnd() ?
              query.getSpan().getEnd() : answer.getSpan().getEnd();

    listJustifications = new ArrayList<Justification>();

    Justification j = new Justification(this.getDocId(), new Pair<Integer, Integer>(start, end));
    j.setSourcePattern(sourcePattern.get());
    j.setQueryBrandyConfidence(query.getBrandyConfidence().get());
    j.setAnswerBrandyConfidence(answer.getBrandyConfidence().get());

    // Bonan: temporary hack
    // j.setSourcePattern("serif");
    // j.setQueryBrandyConfidence("AnyName");
    // j.setAnswerBrandyConfidence("AnyName");
    ////

    listJustifications.add(j);
  }

  public Optional<TextSpan> spanOfSent() {
    return Optional.fromNullable(spanOfSent);
  }

  public void setSpanOfSent(TextSpan spanOfSent) {
    this.spanOfSent = spanOfSent;
  }

  @JsonProperty
  public Optional<Boolean> isValueSlot = Optional.absent();
/*
  @JsonProperty
  private SelectQuestion questionForAnntation;

  public Optional<SelectQuestion> questionForAnntation() {
    return Optional.fromNullable(questionForAnntation);
  }

  public void setQuestionForAnntation(SelectQuestion questionForAnntation) {
    this.questionForAnntation = questionForAnntation;
  }
*/

  @JsonProperty
  private CSInitQuery evalQuery;

  public Optional<CSInitQuery> evalQuery() {
    return Optional.fromNullable(evalQuery);
  }

  public boolean hasEvalQuery() {
    return evalQuery != null;
  }

  public void setEvalQuery(CSInitQuery query) {
    evalQuery = query;
  }

  public Slot getSlot() {
    return slot;
  }

  public Optional<Double> getConfidence() {
    return confidence;
  }

  public TextMention getAnswer() {
    return answer;
  }

  public void setHopId(int hopId) {
    this.hopId = Optional.of(hopId);
  }

  public void setJudgement(String judgement) {
    this.judgementOnRelation = Optional.of(judgement);
  }

  public void setInSysKB(boolean inSysKB) {
    this.inSysKB = Optional.of(inSysKB);
  }

  public void setSourcePattern(String sourcePattern) {
    this.sourcePattern = Optional.of(sourcePattern);
  }

  public void setConfidence(double confidence) {
    this.confidence = Optional.of(confidence);
  }

  public void setSpan(TextSpan span) {
    this.spanOfSent = Preconditions.checkNotNull(span);
  }

  public void setIsValueSlot(boolean isValueSlot) {
    this.isValueSlot = Optional.of(isValueSlot);
  }

  public String getDocId() {
    assert (this.query.getDocId().equals(this.answer.getDocId()));

    return this.query.getDocId();
  }

  @Override
  public String toString() {
    String strHopId = hopId.isPresent() ? hopId.get().toString() : "NA";
    String strJudgement = judgementOnRelation.isPresent() ? judgementOnRelation.get() : "NA";
    String strInSysKB = inSysKB.isPresent() ? inSysKB.get().toString() : "NA";
    String strSourcePattern = sourcePattern.isPresent() ? sourcePattern.get() : "NA";
    String strConfidence = confidence.isPresent() ? confidence.get().toString() : "NA";

    return "[RelationMention: "
        + "query=" + query.toString() + ", "
        + "answer=" + answer.toString() + ", "
        + "slot=" + slot.toString() + ", "
        + "slot=" + slot.toString() + ", "
        + "hopId=" + strHopId + ", "
        + "judgement=" + strJudgement + ", "
        + "inSysKB=" + strInSysKB + ", "
        + "sourcePattern=" + strSourcePattern + ", "
        + "confidence=" + strConfidence
        + "]";
  }

  public String toStringAssessMent() {
    String strJudgementOnQuery =
        this.judgementOnQuery.isPresent() ? this.judgementOnQuery.get() : "NA";
    String strJudgementOnRelation =
        this.judgementOnRelation.isPresent() ? this.judgementOnRelation.get() : "NA";
    String strEquivalentClass = this.answer.getIdEquivalentClass().isPresent() ? Integer
        .toString(this.answer.getIdEquivalentClass().get()) : "NA";

    String strHopId = hopId.isPresent() ? hopId.get().toString() : "NA";
    String strQueryText = StringUtil
        .normalize(query.getText().isPresent() ? query.getText().get() : "NA");
    String strAnswerText = StringUtil.normalize(answer.getText().isPresent() ? answer.getText().get() : "NA");

    return strJudgementOnQuery + "\t" + strJudgementOnRelation + "\t" + strEquivalentClass + "\t"
        + slot.toString() + "\t" + this.getDocId() + "\t" + strHopId + "\t"
        + query.getSpan().getStart() + "\t" + query.getSpan().getEnd() + "\t" + strQueryText + "\t"
        + answer.getSpan().getStart() + "\t" + answer.getSpan().getEnd() + "\t" + strAnswerText;
  }

  public String toSimpleString() {
    String strQueryText = StringUtil.normalize(query.getText().isPresent() ? query.getText().get() : "NA");
    String strAnswerText = StringUtil.normalize(answer.getText().isPresent() ? answer.getText().get() : "NA");
    String strSourcePattern =
        StringUtil.normalize(this.sourcePattern.isPresent() ? this.sourcePattern.get() : "NA");

    String strQueryBrandyConfidence = query.getBrandyConfidence().isPresent()? query.getBrandyConfidence().get() : "NA";
    String strAnswerBrandyConfidence = answer.getBrandyConfidence().isPresent()? answer.getBrandyConfidence().get() : "NA";


    return slot.toString() + " " + this.sourcePattern + " "
        + "(" + query.getSpan().toString() + ") " + strQueryText + ", "
        + strQueryBrandyConfidence + ", "
        + "(" + answer.getSpan().toString() + ") " + strAnswerText + ", "
        + strAnswerBrandyConfidence + ", " + strSourcePattern + ", " + getConfidence().get();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + query.hashCode();
    result = prime * result + answer.hashCode();
    result = prime * result + slot.hashCode();

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
    TextRelationMention other = (TextRelationMention) obj;
    if (!query.equals(other.query)) {
      return false;
    }
    if (!answer.equals(other.answer)) {
      return false;
    }
    if (!slot.equals(other.slot)) {
      return false;
    }
    return true;
  }
}
