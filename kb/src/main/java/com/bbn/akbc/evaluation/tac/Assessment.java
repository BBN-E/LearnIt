package com.bbn.akbc.evaluation.tac;

import java.util.Map;


public class Assessment {

  public String responseId;
  public String queryId;
  public String parentId;
  public int hopId;

  public String slot;
  public String docid;
  public String text;

  public int filler_beg;
  public int filler_end;

  public int sent_beg;
  public int sent_end;

  public String judgement1;
  public String judgement2;
  public String equivalent_class;

  public Assessment() {}

  public Assessment(String responseId, String queryId, String parentId,
      String slot, String docid, String text,
      int filler_beg, int filler_end, int sent_beg, int sent_end,
      String judgement1, String judgement2, String equivalent_class) {

    this.responseId = responseId;
    this.queryId = queryId;
    this.parentId = parentId;

    this.hopId = Integer.parseInt(queryId.substring(queryId.lastIndexOf("_") + 1));

    this.slot = slot;
    this.docid = docid;
    this.text = text;

    this.filler_beg = filler_beg;
    this.filler_end = filler_end;

    this.sent_beg = sent_beg;
    this.sent_end = sent_end;

    this.judgement1 = judgement1;
    this.judgement2 = judgement2;
    this.equivalent_class = equivalent_class;
  }

  public static Assessment fromLine(String sline, String equivalent_class) {
    String[] fields = sline.trim().split("\t");

    String responseId = fields[0];
    String queryId = fields[1];
    String parentId = fields[2];

    String slot = fields[3];

    String docid = fields[4];
    String text = fields[5];

    int filler_beg = Integer.parseInt(fields[6]);
    int filler_end = Integer.parseInt(fields[7]);
    int sent_beg = Integer.parseInt(fields[8]);
    int sent_end = Integer.parseInt(fields[9]);

    String judgement1 = fields[10];
    String judgement2 = fields[11];

    Assessment assessment = new Assessment(responseId, queryId, parentId,
        slot, docid, text,
        filler_beg, filler_end, sent_beg, sent_end,
        judgement1, judgement2, equivalent_class);

    return assessment;
  }

  public String getEquivalentClassStr(Map<String, String> responseID2eclassID) {
    String parentEclassID = "";
    if (!this.parentId.equals("NIL")) {
      if (!responseID2eclassID.containsKey(this.parentId)) {
        System.err.println(
            "ERROR: following parentId is not found in responseID2eclassID: " + this.parentId);
        System.exit(-1);
      }
      parentEclassID = responseID2eclassID.get(this.parentId);
    }

    String equivalentClassID = queryId + ":" + slot + ":" + parentEclassID
        + ":" + equivalent_class;
    return equivalentClassID;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + queryId.hashCode();
    result = prime * result + slot.hashCode();
    result = prime * result + docid.hashCode();
//		result = prime * result + text.hashCode();
    result = prime * result + filler_beg;
    result = prime * result + filler_end;

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
    Assessment other = (Assessment) obj;
    if (!queryId.equals(other.queryId)) {
      return false;
    }
    if (!slot.equals(other.slot)) {
      return false;
    }
    if (!docid.equals(other.docid)) {
      return false;
    }
//		if (!text.equals(other.text))
//			return false;
    if (filler_beg != other.filler_beg) {
      return false;
    }
    if (filler_end != other.filler_end) {
      return false;
    }
    return true;
  }
}
