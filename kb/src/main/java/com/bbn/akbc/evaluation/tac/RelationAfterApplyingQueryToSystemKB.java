package com.bbn.akbc.evaluation.tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bmin on 11/7/15.
 */
public class RelationAfterApplyingQueryToSystemKB {
  public String queryId;
  public String slot;
  public List<EvalTextSpan> relationProvenances;
  public List<EvalTextSpan> fillerProvenances;
  public double conf;

  RelationAfterApplyingQueryToSystemKB(String queryId,
      String slot,
      List<EvalTextSpan> relationProvenances,
      List<EvalTextSpan> fillerProvenances,
      double conf) {

    this.queryId = queryId;
    this.slot = slot;
    this.relationProvenances = relationProvenances;
    this.fillerProvenances = fillerProvenances;
    this.conf = conf;
  }

  public static RelationAfterApplyingQueryToSystemKB fromLine(String sline) {
    String [] fields = sline.trim().split("\t");

    String queryId = fields[0];
    String slot = fields[1];

    List<EvalTextSpan> relationProvenances = new ArrayList<EvalTextSpan>();
    for(String provenance : fields[3].split(","))
      relationProvenances.add(EvalTextSpan.fromLine(provenance));

    List<EvalTextSpan> fillerProvenances = new ArrayList<EvalTextSpan>();
    for(String provenance : fields[6].split(","))
      fillerProvenances.add(EvalTextSpan.fromLine(provenance));

    double conf = Double.parseDouble(fields[7]);

    return new RelationAfterApplyingQueryToSystemKB(queryId,
        slot,
        relationProvenances,
        fillerProvenances,
        conf);

  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("queryId=" + queryId + ", ");
    sb.append("slot=" + slot + ", ");
    sb.append("conf=" + conf + ", ");
    for(EvalTextSpan textSpan : fillerProvenances)
      sb.append("EvalTextSpan=" + textSpan.toString() + ", ");

    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + queryId.hashCode();
    result = prime * result + slot.hashCode();
    for(EvalTextSpan textSpan : fillerProvenances) {
      result = prime * result + textSpan.docId.hashCode();
      result = prime * result + textSpan.charOffsetStart;
      result = prime * result + textSpan.charOffsetEnd;
    }
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
    RelationAfterApplyingQueryToSystemKB other = (RelationAfterApplyingQueryToSystemKB) obj;
    if (!this.queryId.equals(other.queryId)) {
      return false;
    }
    if (this.slot != other.slot) {
      return false;
    }

    Collections.sort(this.fillerProvenances, Collections.reverseOrder());
    Collections.sort(other.fillerProvenances, Collections.reverseOrder());
    if(this.fillerProvenances.size() != other.fillerProvenances.size())
      return false;
    for(int i=0; i<this.fillerProvenances.size(); i++) {
      if(!this.fillerProvenances.get(i).equals(other.fillerProvenances.get(i)))
        return false;
    }

    return true;
  }
}
