package com.bbn.akbc.evaluation.tac;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Relation {

  @JsonProperty
  public Entity queryEntity;
  @JsonProperty
  public Entity answerEntity;
  @JsonProperty
  public String slot;
  @JsonProperty
  public Set<RelationMention> relationMentions;
  @JsonProperty
  public String judgement;

  Relation(Entity queryEntity, Entity answerEntity,
      String slot) {

    Preconditions.checkNotNull(queryEntity);
    Preconditions.checkNotNull(answerEntity);
    this.queryEntity = queryEntity;
    this.answerEntity = answerEntity;
    this.slot = slot;

    relationMentions = new HashSet<RelationMention>();
  }

  public void addMention(RelationMention relationMention) {
    relationMentions.add(relationMention);

    judgement = relationMention.judgement; // TODO: check consisitency
  }

  public Set<RelationMention> getMentions() {
    return relationMentions;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<id: " + slot + "(" + queryEntity.id + ", " + answerEntity.id + ")>\n");
    for (RelationMention rm : relationMentions) {
      sb.append("\t<RelationMention: " + rm.toString() + ">\n");
    }

    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + queryEntity.hashCode();
    result = prime * result + answerEntity.hashCode();
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
    Relation other = (Relation) obj;
    if (!queryEntity.equals(other.queryEntity)) {
      return false;
    }
    if (!answerEntity.equals(other.answerEntity)) {
      return false;
    }
    if (!slot.equals(other.slot)) {
      return false;
    }
    return true;
  }
}
