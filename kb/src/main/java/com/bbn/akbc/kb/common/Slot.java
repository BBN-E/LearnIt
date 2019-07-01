package com.bbn.akbc.kb.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Slot extends RelationType {

  @JsonProperty
  String entityType;
  @JsonProperty
  String relation;

  @JsonProperty
  boolean isSingleValueSlot;

  @JsonCreator
  public Slot(
      @JsonProperty("entityType") String entityType,
      @JsonProperty("relation") String relation,
      @JsonProperty("isSingleValueSlot") boolean isSingleValueSlot) {
    this.entityType = entityType;
    this.relation = relation;
    this.isSingleValueSlot = isSingleValueSlot;
  }

  public String toString() {
    return this.entityType + ":" + this.relation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entityType.hashCode();
    result = prime * result + relation.hashCode();
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
    Slot other = (Slot) obj;
    if (!this.entityType.equals(other.entityType)) {
      return false;
    }
    if (!this.relation.equals(other.relation)) {
      return false;
    }
    return true;
  }
}
