package com.bbn.akbc.kb.text;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TextSpan {
  @JsonProperty
  private int start;
  @JsonProperty
  private int end;

  @JsonCreator
  public TextSpan(
      @JsonProperty("start") int start,
      @JsonProperty("end") int end) {
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  /*
   * note: end is inclusive
   */
  public int getLength() {
    return this.end + 1 - this.start;
  }

  public String toString() {
    return "Span: " + "start=" + this.start + ", end=" + this.end;
  }

  public String toColdStart2014String() {
    return this.getStart() + "-" + this.getEnd();
  }

  public static TextSpan fromLine(String strLine) {
    String [] items = strLine.trim().split("-");
    return new TextSpan(Integer.parseInt(items[0].trim()), Integer.parseInt(items[1].trim()));
  }


  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + start;
    result = prime * result + end;

    return result;
  }

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

    TextSpan other = (TextSpan) obj;
    if (start != other.getStart()) {
      return false;
    }
    if (end != other.getEnd()) {
      return false;
    }
    return true;
  }

  public boolean overlapWith(TextSpan span) {
    if (this.getStart() <= span.getStart() &&
        this.getEnd() >= span.getStart()) {
      return true;
    }
    else if (this.getStart() >= span.getStart() &&
        this.getStart() <= span.getEnd()) {
      return true;
    } else {
      return false;
    }
  }
}
