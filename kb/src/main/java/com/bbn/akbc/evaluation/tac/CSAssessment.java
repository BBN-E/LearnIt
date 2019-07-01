package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.text.TextSpan;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;


public class CSAssessment extends Assessment {

  public String responseId;
  public String queryId;
  public String parentId;
  public int hopId;
  public Slot slot;
  public String docid;
  public String text;
  public TextSpan spanOfFiller;
  public TextSpan spanOfSent;
  public String judgement1;
  public String judgement2;
  public int equivalent_class_id;
  public String equivalent_class_str; // for visualization2

  public Optional<Boolean> inSys = Optional.absent();
  public Optional<Integer> uid = Optional.absent();

  public Optional<String> equivalent_class_str_in_file = Optional.absent();
  public Optional<String> eclass_str = Optional.absent();

  public CSAssessment(String responseId, String queryId, String parentId,
      String slot, String docid, String text,
      int filler_beg, int filler_end, int sent_beg, int sent_end,
      String judgement1, String judgement2, int equivalent_class,
      String equivalent_class_str_in_file) {

    this.responseId = responseId;
    this.queryId = queryId;
    this.parentId = parentId;

    this.hopId = Integer.parseInt(queryId.substring(queryId.lastIndexOf("_") + 1));

    this.slot = SlotFactory.fromStringSlotName(slot);
    this.docid = docid;
    this.text = text;

    this.spanOfFiller = new TextSpan(filler_beg, filler_end);
    this.spanOfSent = new TextSpan(sent_beg, sent_end);

    this.judgement1 = judgement1;
    this.judgement2 = judgement2;
    this.equivalent_class_id = equivalent_class;

    this.equivalent_class_str =
        queryId + ":" + slot.toString() + ":" + hopId + ":" + equivalent_class_str_in_file;
    this.equivalent_class_str_in_file = Optional.of(equivalent_class_str_in_file);
  }

  public CSAssessment(String responseId, String queryId, String parentId,
      String slot, String docid, String text,
      int filler_beg, int filler_end, int sent_beg, int sent_end,
      String judgement1, String judgement2, int equivalent_class,
      String equivalent_class_str_in_file,
      boolean isCS2014Eval) {
    if (!isCS2014Eval) {
      System.err.println("Illgeal call on constructor for CS2014 assessment");
      System.exit(-1);
    }

    // bad coding; simply copy from another constructor & change
    this.responseId = responseId;
    this.queryId = queryId;
    this.parentId = parentId;

    this.hopId = isHop1Query(queryId) ? 1 : 0;

    this.slot = SlotFactory.fromStringSlotName(slot);
    this.docid = docid;
    this.text = text;

    this.spanOfFiller = new TextSpan(filler_beg, filler_end);
    this.spanOfSent = new TextSpan(sent_beg, sent_end);

    this.judgement1 = judgement1;
    this.judgement2 = judgement2;
    this.equivalent_class_id = equivalent_class;

    this.equivalent_class_str =
        queryId + ":" + slot.toString() + ":" + hopId + ":" + equivalent_class_str_in_file;
    this.equivalent_class_str_in_file = Optional.of(equivalent_class_str_in_file);
  }

  public Optional<String> getEquivalent_class_str_in_file() {
    return equivalent_class_str_in_file;
  }

  public boolean isHop1Query(String queryString) {
    String[] items = queryString.trim().split("_");
    if (items.length >= 4) {
      return true;
    } else {
      return false;
    }
  }

  public CSAssessment(String queryId,
      Slot slot,
      String docId,
      TextSpan spanOfFiller) {
    this.queryId = queryId;
    this.slot = slot;
    this.docid = docId;
    this.spanOfFiller = spanOfFiller;
  }

  public static CSAssessment fromLine(String sline, int equivalent_class) {

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

    String equivalent_class_str_in_file = fields[12].trim();

    CSAssessment assessment = new CSAssessment(responseId, queryId, parentId,
        slot, docid, text,
        filler_beg, filler_end, sent_beg, sent_end,
        judgement1, judgement2, equivalent_class,
        equivalent_class_str_in_file);

    return assessment;
  }

  static String normalizeQueryID(String queryID) {
/*
    String [] items = queryID.split("_");
    if(items.length==3)
      return queryID + "_00";
*/
    return queryID;
  }

  public static CSAssessment fromLine2014CS(String sline, int lineno) {
    int equivalent_class = lineno;

    String[] fields = sline.trim().split("\t");

    String responseId = fields[0]; // this has to be unique
    int idxSeperator = fields[1].substring(0, fields[1].lastIndexOf(":")).lastIndexOf(":");
    String queryId = fields[1].substring(0, idxSeperator);
    queryId = normalizeQueryID(queryId);

    String slot = fields[1].substring(idxSeperator + 1);

//		String parentId = "dummy parent ID";
    String parentId = "NIL";

    String docid = fields[4].substring(0, fields[4].indexOf(":"));
    String text = fields[3];
    // TODO: need to find whether the first span is the span of filler, when multiple docID+spans are presented
    int idxEnd = fields[4].length();
    if(fields[4].indexOf(",")!=-1)
      idxEnd = fields[4].indexOf(",");
    //

    String[] items = fields[4].substring(fields[4].indexOf(":") + 1, idxEnd).split("-");
    int filler_beg = Integer.parseInt(items[0]);
    int filler_end = Integer.parseInt(items[1]);

    int sent_beg = filler_beg;
    int sent_end = filler_end;

    String judgement1 = fields[5];
    String judgement2 = fields[6];

    String equivalent_class_str_in_file = fields[1] + "_" + fields[7].trim();

    CSAssessment assessment = new CSAssessment(responseId, queryId, parentId,
        slot, docid, text,
        filler_beg, filler_end, sent_beg, sent_end,
        judgement1, judgement2, equivalent_class,
        equivalent_class_str_in_file, true);

    return assessment;
  }

  public static CSAssessment fromLine2015CS(String sline) {
    sline = sline.trim();

    String [] fields = sline.split("\t");

    String responseId = fields[0];
    String queryIdAndSlot = fields[1];
    String provenanceOfRelation = fields[2];
    String slotFiller = fields[3];
    String provenanceOfFiller = fields[4];
    String assessmentOfFiller = fields[5];
    String assessmentOfRelationProvenance = fields[6];
    String ldcEquivalantClassOfFiller = fields[7];

    List<EvalTextSpan> textSpanRelationProvenance = new ArrayList<EvalTextSpan>();
    for(String strTextSpanRelationProvenance : provenanceOfRelation.trim().split(",")) {
      if(strTextSpanRelationProvenance.trim().isEmpty())
        textSpanRelationProvenance.add(EvalTextSpan.fromLine(strTextSpanRelationProvenance.trim()));
    }

    List<EvalTextSpan> textSpanFillerProvenance = new ArrayList<EvalTextSpan>();
    for(String strTextSpanFillerProvenance : provenanceOfFiller.trim().split(",")) {
      if(!strTextSpanFillerProvenance.trim().isEmpty())
        textSpanFillerProvenance.add(EvalTextSpan.fromLine(strTextSpanFillerProvenance.trim()));
    }

    String queryId = queryIdAndSlot.substring(0, queryIdAndSlot.indexOf(":"));
    String slot = queryIdAndSlot.substring(queryIdAndSlot.indexOf(":") + 1);

    int responseIdx = Integer.parseInt(responseId.substring(responseId.lastIndexOf("_") + 1));
    String responseIdShort = responseId.substring(0, responseId.lastIndexOf("_"));
    int hopId = Integer.parseInt(responseIdShort.substring(responseIdShort.lastIndexOf("_") + 1));
    String queryIdInResponseId = responseIdShort.substring(0, responseIdShort.lastIndexOf("_"));

    String equivalentClassId = ldcEquivalantClassOfFiller;

    System.out.println(queryIdInResponseId + "\t"
            + hopId + "\t"
            + responseIdx + "\t"
            + queryId + "\t"
            + slot + "\t"
            + assessmentOfFiller + "\t"
            + assessmentOfRelationProvenance + "\t"
            + equivalentClassId
    );

    CSAssessment assessment = new CSAssessment(responseId, queryId, "",
        slot, textSpanFillerProvenance.get(0).docId, slotFiller,
        textSpanFillerProvenance.get(0).charOffsetStart, textSpanFillerProvenance.get(0).charOffsetEnd,
        textSpanFillerProvenance.get(0).charOffsetStart, textSpanFillerProvenance.get(0).charOffsetEnd,
        assessmentOfFiller, assessmentOfRelationProvenance, 0,
        equivalentClassId, true);
    return assessment;
  }

  public static CSAssessment fromLine2016CS(String sline) {
    sline = sline.trim();

    String [] fields = sline.split("\t");

    String responseId = fields[0];
    String queryIdAndSlot = fields[1];
    String provenanceOfRelation = fields[2];
    String slotFiller = fields[3];
    String provenanceOfFiller = fields[4];
    String assessmentOfFiller = fields[5];
    String assessmentOfRelationProvenance = fields[7];
    String ldcEquivalantClassOfFiller = fields[8];

    List<EvalTextSpan> textSpanRelationProvenance = new ArrayList<EvalTextSpan>();
    for(String strTextSpanRelationProvenance : provenanceOfRelation.trim().split(",")) {
      if(strTextSpanRelationProvenance.trim().isEmpty())
        textSpanRelationProvenance.add(EvalTextSpan.fromLine(strTextSpanRelationProvenance.trim()));
    }

    List<EvalTextSpan> textSpanFillerProvenance = new ArrayList<EvalTextSpan>();
    for(String strTextSpanFillerProvenance : provenanceOfFiller.trim().split(",")) {
      if(!strTextSpanFillerProvenance.trim().isEmpty())
        textSpanFillerProvenance.add(EvalTextSpan.fromLine(strTextSpanFillerProvenance.trim()));
    }

    String queryId = queryIdAndSlot.substring(0, queryIdAndSlot.indexOf(":"));
    String slot = queryIdAndSlot.substring(queryIdAndSlot.indexOf(":") + 1);

    int responseIdx = Integer.parseInt(responseId.substring(responseId.lastIndexOf("_") + 1));
    String responseIdShort = responseId.substring(0, responseId.lastIndexOf("_"));
    int hopId = Integer.parseInt(responseIdShort.substring(responseIdShort.lastIndexOf("_") + 1));
    String queryIdInResponseId = responseIdShort.substring(0, responseIdShort.lastIndexOf("_"));

    String equivalentClassId = ldcEquivalantClassOfFiller;

    System.out.println(queryIdInResponseId + "\t"
        + hopId + "\t"
        + responseIdx + "\t"
        + queryId + "\t"
        + slot + "\t"
        + assessmentOfFiller + "\t"
        + assessmentOfRelationProvenance + "\t"
        + equivalentClassId
    );

    CSAssessment assessment = new CSAssessment(responseId, queryId, "",
        slot, textSpanFillerProvenance.get(0).docId, slotFiller,
        textSpanFillerProvenance.get(0).charOffsetStart, textSpanFillerProvenance.get(0).charOffsetEnd,
        textSpanFillerProvenance.get(0).charOffsetStart, textSpanFillerProvenance.get(0).charOffsetEnd,
        assessmentOfFiller, assessmentOfRelationProvenance, 0,
        equivalentClassId, true);
    return assessment;
  }

  public void setInSys(boolean inSys) {
    this.inSys = Optional.of(inSys);
  }

  public void setUid(int uid) {
    this.uid = Optional.of(uid);
  }

  public String getQueryId() {
    return queryId;
  }

  public String getDocId() {
    return docid;
  }

  public Slot getSlot() {
    return slot;
  }

  public TextSpan getSpanOfFiller() {
    return spanOfFiller;
  }

  public String getResponseId() {
    return responseId;
  }

  public boolean isCorrect() {
    if (this.judgement1.equals("1") || // evaluation before CS2014
        this.judgement1.equals("C")) { // CS2014
      return true;
    } else {
      return false;
    }
  }

  public String toString() {
    return "[CSAssessment: "
        + "responseId=" + responseId + ", "
        + "queryId=" + queryId + ", "
        + "parentId=" + parentId + ", "
        + "hopId=" + hopId + ", "
        + "slot=" + slot.toString() + ", "
        + "docid=" + docid + ", "
        + "spanOfFiller=" + spanOfFiller.toString() + ", "
        + "spanOfSent=" + spanOfSent.toString() + ", "
        + "judgement1=" + judgement1 + ", "
        + "judgement2=" + judgement2 + ", "
        + "equivalent_class=" + equivalent_class_id
        + "]";
  }

  public String toStringForCsAnalysis() {
    return "[ "
//        + slot.toString() + " "
//        + text.replace(" ", "_") + " "
        + equivalent_class_str_in_file.get() + " "

        + queryId + "--"
        + "h" + hopId + "--"
        + docid + ":" + spanOfFiller.getStart()+ "-" + spanOfFiller.getEnd() + " "

        + "J1=" + judgement1 + ","
        + "J2=" + judgement2 + " ]";
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.queryId.hashCode();
    result = prime * result + this.slot.hashCode();
    result = prime * result + this.docid.hashCode();
    result = prime * result + this.spanOfFiller.hashCode();

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
    CSAssessment other = (CSAssessment) obj;
    if (!queryId.equals(other.queryId)) {
      return false;
    }
    if (!slot.equals(other.slot)) {
      return false;
    }
    if (!docid.equals(other.docid)) {
      return false;
    }
    if (!this.spanOfFiller.equals(other.spanOfFiller)) {
      return false;
    }
    return true;
  }
}
