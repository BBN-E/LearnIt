package com.bbn.akbc.evaluation.tac;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Entity {

  @JsonProperty
  public String id;
  @JsonProperty
  public Set<MentionInfo> mentions;

  Entity(String id) {
    this.id = id;
    mentions = new HashSet<MentionInfo>();
  }

  public void addMention(MentionInfo mention) {
    mentions.add(mention);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<id: " + id + ">");
    for (MentionInfo mention : mentions) {
      sb.append("\t<mention: " + mention.toPrettyString() + ">\n");
    }

    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();

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
    Entity other = (Entity) obj;
    if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }
}
