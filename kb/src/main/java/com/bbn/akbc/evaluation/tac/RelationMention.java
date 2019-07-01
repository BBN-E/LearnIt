package com.bbn.akbc.evaluation.tac;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class RelationMention {

  @JsonProperty
  public Entity queryEntity;
  @JsonProperty
  public MentionInfo answerMention;
  @JsonProperty
  public String slot;
  @JsonProperty
  public int hopId;
  @JsonProperty
  public String judgement;
  @JsonProperty
  public boolean inSysKB;
  @JsonProperty
  public String sourcePattern;

  RelationMention(Entity queryEntity, MentionInfo answerMention, String slot,
      int hopId,
      String judgement, boolean inSysKB, String sourcePattern) {
    this.queryEntity = queryEntity;
    this.answerMention = answerMention;
    this.slot = slot;

    this.hopId = hopId;

    this.judgement = judgement;
    this.inSysKB = inSysKB;

    this.sourcePattern = sourcePattern;
  }

  @Override
  public String toString() {
    return "<reln=" + slot + ", queryEntity=" + queryEntity.id + ", answerMention=" + answerMention
        .toPrettyString() + ", judgement=" + judgement + ", inSys=" + inSysKB + ", source_pattern="
        + sourcePattern + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + queryEntity.hashCode();
    result = prime * result + answerMention.hashCode();
    result = prime * result + slot.hashCode();
    result = prime * result + hopId;

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
    RelationMention other = (RelationMention) obj;
    if (!queryEntity.equals(queryEntity)) {
      return false;
    }
    if (!answerMention.equals(other.answerMention)) {
      return false;
    }
    if (!slot.equals(slot)) {
      return false;
    }
    if (hopId != other.hopId) {
      return false;
    }
    return true;
  }
}
