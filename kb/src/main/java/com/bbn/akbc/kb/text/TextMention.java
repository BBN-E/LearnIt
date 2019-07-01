package com.bbn.akbc.kb.text;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TextMention extends TextRelationArgument {
  @JsonProperty
  private String docId;
  @JsonProperty
  private TextSpan span;
  @JsonProperty
  private Optional<String> text = Optional.absent();
  @JsonProperty
  private Optional<String> type = Optional.absent();
  @JsonProperty
  private Optional<String> subType = Optional.absent();
  @JsonProperty
  private Optional<Integer> anchorOffset = Optional.absent();
  @JsonProperty
  private Optional<String> eid = Optional.absent();
  @JsonProperty
  private Optional<Double> confidence = Optional.absent(); // pronoun always has confidence=0
  @JsonProperty
  private Optional<Double> linkConfidence = Optional.absent();
  @JsonProperty
  private Optional<String> brandyConfidence = Optional.absent();

  // for cs2016
  @JsonProperty
  private Optional<Boolean> isPlural = Optional.absent();
  //

  /*
   * To be deleted
   */
  @JsonProperty
  private Optional<String> strHTMLForAnnotation = Optional.absent();
  @JsonProperty
  private Optional<Integer> idEquivalentClass = Optional.absent();
  //

  @JsonCreator
  public TextMention(
      @JsonProperty("docId") String docId,
      @JsonProperty("span") TextSpan span,
      @JsonProperty("text") Optional<String> text,
      @JsonProperty("type") Optional<String> type,
      @JsonProperty("subType") Optional<String> subType,
      @JsonProperty("anchorOffset") Optional<Integer> anchorOffset,
      @JsonProperty("eid") Optional<String> eid,
      @JsonProperty("strHTMLForAnnotation") Optional<String> strHTMLForAnnotation,
      @JsonProperty("idEquivalentClass") Optional<Integer> idEquivalentClass,

      @JsonProperty("confidence") Optional<Double> confidence,
      @JsonProperty("linkConfidence") Optional<Double> linkConfidence,
      @JsonProperty("brandyConfidence") Optional<String> brandyConfidence) {

    this.docId = docId;
    this.span = span;
    this.text = text;
    this.type = type;
    this.subType = subType;
    this.anchorOffset = anchorOffset;
    this.eid = eid;
    this.strHTMLForAnnotation = strHTMLForAnnotation;
    this.idEquivalentClass = idEquivalentClass;

    this.confidence = confidence;
    this.linkConfidence = linkConfidence;
    this.brandyConfidence = brandyConfidence;
  }

  public TextMention(TextEntity e) {
    eid = Optional.of(e.getId());
  }

  public TextMention(String docId,
      int start, int end) {
    this.docId = docId;
    this.span = new TextSpan(start, end);
  }

  public TextMention(String docId,
      int start, int end,
      String text) {
    this(docId, start, end);
    this.text = Optional.of(text);
  }

  public TextMention(String docId,
      int start, int end,
      String text,
      String type, String subtype) {
    this(docId, start, end, text);
    this.type = Optional.of(type);
    this.subType = Optional.of(subtype);
  }

  public TextMention(String docId,
      int start, int end,
      String type, String subtype) {
    this(docId, start, end);
    this.type = Optional.of(type);
    this.subType = Optional.of(subtype);
  }

  public TextMention getDeepCopy() {
    TextMention m =
        new TextMention(this.docId, this.span.getStart(), this.span.getEnd(), this.text.get());

    m.setEntityId(this.eid.get());
    m.setConfidence(this.confidence.isPresent() ? this.confidence.get() : 1.0);
    m.setLinkConfidence(this.linkConfidence.isPresent() ? this.linkConfidence.get() : 1.0);
    m.setBrandyConfidence(this.brandyConfidence.isPresent() ? this.brandyConfidence.get() : "NA");

    return m;
  }

  public Optional<String> getType() {
    return type;
  }

  public Optional<String> getSubType() {
    return subType;
  }

  public Optional<String> getBrandyConfidence() {
    return brandyConfidence;
  }

  public Optional<Integer> getIdEquivalentClass() {
    return idEquivalentClass;
  }

  public void setIdEquivalentClass(int eclassId) {
    idEquivalentClass = Optional.of(eclassId);
  }

  public void setText(String text) {
    this.text = Optional.of(text);
  }

  public void setAnchorOffset(int anchorOffset) {
    this.anchorOffset = Optional.of(anchorOffset);
  }

  public void setType(String type) {
    this.type = Optional.of(type);
  }

  public void setSubType(String subType) {
    this.subType = Optional.of(subType);
  }

  public void setIsPlural(boolean isPlural) {this.isPlural = Optional.of(isPlural); }

  public Optional<Boolean> getIsPlural() { return isPlural; }

  public void setEntityId(String eid) {
    this.eid = Optional.of(eid);
  }

  public Optional<String> getEntityId() {
    return eid;
  }

  public void setConfidence(double confidence) {
    this.confidence = Optional.of(confidence);
  }

  public Optional<Double> getConfidence() {
    return confidence;
  }

  public TextSpan getSpan() {
    return span;
  }

  public Optional<String> getText() {
    return text;
  }

  public String getDocId() {
    return docId;
  }

  public void setLinkConfidence(double linkConfidence) {
    this.linkConfidence = Optional.of(linkConfidence);
  }

  public Optional<Double> getLinkConfidence() {
    return linkConfidence;
  }

  public void setBrandyConfidence(String brandyConfidence) {
    this.brandyConfidence = Optional.of(brandyConfidence);
  }

  public void setStrHTMLForAnnotation(String strHTML) {
    strHTMLForAnnotation = Optional.of(strHTML);
  }

  @Override
  public String toString() {
    String strText = text.isPresent() ? text.get() : "NA";
    String strBrandyConfidence =
        this.brandyConfidence.isPresent() ? this.brandyConfidence.get() : "NA";
    return "TextMention: "
        + "text=" + strText + ", "
        + "docId=" + docId + ", "
        + "span=" + "[" + span.toString() + "]" + ", "
        + "bconf=" + strBrandyConfidence + ", "
        + "type=" + type;
  }

  public String toSimpleString() {
    String strText = text.isPresent() ? text.get() : "NA";
    String strBrandyConfidence =
        this.brandyConfidence.isPresent() ? this.brandyConfidence.get() : "NA";
    return "TextMention: "
        + "text=" + strText
        + ", docId=" + docId
        + ", span=" + span.toString()
        + ", type=" + type;
  }

  public String toStringAssessMent() {

    String strType = type.isPresent() ? type.get() : "NA";
    String strSubtype = type.isPresent() ? type.get() : "NA";
    String strAnchorOffset = anchorOffset.isPresent() ? anchorOffset.get().toString() : "NA";
    String strEid = eid.isPresent() ? eid.get() : "NA";
    String strText = text.isPresent() ? text.get() : "NA";

    return docId + "\t" + span.getStart() + "\t" + span.getEnd() + "\t" + strType + "\t"
        + strSubtype + "\t" + strAnchorOffset + "\t" + strEid + "\t" + strText;
  }

  public String toColdStartString() {
    return "\"" + this.text.get() + "\"\t" + this.docId + "\t" + this.span.getStart() + "\t"
        + this.span.getEnd();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + docId.hashCode();
    result = prime * result + span.hashCode();

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
    TextMention other = (TextMention) obj;
    if (!this.docId.equals(other.docId)) {
      return false;
    }
    if (!this.span.equals(other.span)) {
      return false;
    }
    return true;
  }
}
