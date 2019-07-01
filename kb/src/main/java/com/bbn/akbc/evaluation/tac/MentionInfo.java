package com.bbn.akbc.evaluation.tac;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE)
public class MentionInfo {

  @JsonProperty
  public String text;
  @JsonProperty
  public String docId;
  @JsonProperty
  public int start;
  @JsonProperty
  public int end;

  public MentionInfo(String text, String docId, int start, int end) {
    this.text = text;
    this.docId = docId;
    this.start = start;
    this.end = end;
  }

  public String toPrettyString() {
    return "<text=" + text + ", docID=" + docId + ", offset=(" + start + "," + end + ")>";
  }

  @Override
  public String toString() {
    return "\"" + text + "\"\t" + docId + "\t" + start + "\t" + end;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + docId.hashCode();
    result = prime * result + start;
    result = prime * result + end;

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
    MentionInfo other = (MentionInfo) obj;
    if (!docId.equals(other.docId)) {
      return false;
    }
    if (start != other.start) {
      return false;
    }
    if (end != other.end) {
      return false;
    }
    return true;
  }
}
