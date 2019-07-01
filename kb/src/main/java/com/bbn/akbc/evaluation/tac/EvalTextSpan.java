package com.bbn.akbc.evaluation.tac;

/**
 * Created by bmin on 11/6/15.
 */
public class EvalTextSpan {
    public String docId;
    public int charOffsetStart;
    public int charOffsetEnd;

    EvalTextSpan(String docId, int charOffsetStart, int charOffsetEnd) {
      this.docId = docId;
      this.charOffsetStart = charOffsetStart;
      this.charOffsetEnd = charOffsetEnd;
    }

    static public EvalTextSpan fromLine(String strTextSpan) {
      String docId = strTextSpan.substring(0, strTextSpan.indexOf(":"));

      String strCharOffsetPair = strTextSpan.substring(strTextSpan.indexOf(":")+1);
      int charOffsetStart = Integer.parseInt(
          strCharOffsetPair.substring(0, strCharOffsetPair.indexOf("-")));
      int charOffsetEnd = Integer.parseInt(strCharOffsetPair.substring(strCharOffsetPair.indexOf("-")+1));

      return new EvalTextSpan(docId, charOffsetStart, charOffsetEnd);
    }

  public String toString() {
    return docId + ":" + charOffsetStart + "-" + charOffsetEnd;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + docId.hashCode();
    result = prime * result + charOffsetStart;
    result = prime * result + charOffsetEnd;

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
    EvalTextSpan other = (EvalTextSpan) obj;
    if (!this.docId.equals(other.docId)) {
      return false;
    }
    if (this.charOffsetStart != other.charOffsetStart) {
      return false;
    }
    if (this.charOffsetEnd != other.charOffsetEnd) {
      return false;
    }

    return true;
  }

}
