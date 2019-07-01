package com.bbn.akbc.evaluation.tac;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 5/4/16.
 */
public class UnAssessedEntry {
  public String slot;
  public String parentEc;
  public String thisEc;

  public EvalTextSpan filler_eval_text_span;
  public String filler_text;
  public String filler_ent_type;

  public List<EvalTextSpan> relation_provenances;

  public double relation_confidence;

  public String thisQueryId;
  public String parentQueryId;
  public int hop;

  UnAssessedEntry(String slot,
      String parentEc,
      String thisEc,
      EvalTextSpan filler_eval_text_span,
      String filler_text,
      String filler_ent_type,
      List<EvalTextSpan> relation_provenances,
      double relation_confidence,
      String thisQueryId,
      String parentQueryId,
      int hop) {

    this.slot = slot;

    this.parentEc = parentEc;
    this.thisEc = thisEc;

    this.filler_eval_text_span = filler_eval_text_span;
    this.filler_text = filler_text;
    this.filler_ent_type = filler_ent_type;

    this.relation_provenances = relation_provenances;
    this.relation_confidence = relation_confidence;

    this.thisQueryId = thisQueryId;
    this.parentQueryId = parentQueryId;
    this.hop = hop;
  }

  static UnAssessedEntry fromLine(String strLineFromCsScorerTrace) {
    strLineFromCsScorerTrace = strLineFromCsScorerTrace.trim();
    String [] itemsByTab = strLineFromCsScorerTrace.split("\t");

    String parentEc = itemsByTab[1];
    String thisEc = itemsByTab[2];

    String [] itemsBySpace = itemsByTab[3].trim().split(" ");
    String thisQueryId = itemsBySpace[0].replace("SUBMISSION:", "").trim();
    String parentQueryId = itemsBySpace[1].trim();
    int hop = Integer.parseInt(itemsBySpace[2]);

    String slot = itemsByTab[4];
    String strListOfRelationProvenances = itemsByTab[6];
    List<EvalTextSpan> relation_provenances = new ArrayList<EvalTextSpan>();
    for(String strRelationProvenance : strListOfRelationProvenances.trim().split(",")) {
      if(!strRelationProvenance.isEmpty())
        relation_provenances.add(EvalTextSpan.fromLine(strRelationProvenance));
    }
    String filler_text = itemsByTab[7];
    String filler_ent_type = itemsByTab[8];
    EvalTextSpan filler_eval_text_span = EvalTextSpan.fromLine(itemsByTab[9]);

    double relation_confidence = Double.parseDouble(itemsByTab[10]);

    return new UnAssessedEntry(slot,
        parentEc,
        thisEc,
        filler_eval_text_span,
        filler_text,
        filler_ent_type,
        relation_provenances,
        relation_confidence,
        thisQueryId,
        parentQueryId,
        hop);
  }

  // CSSF15_ENG_284245cd1f_9253aa698ad7_1_005
  // CSSF15_ENG_284245cd1f_9253aa698ad7:org:employees_or_members
  // 818287a2ab2baea1cbc92835cc9dc21e:768-828,818287a2ab2baea1cbc92835cc9dc21e:768-828,818287a2ab2baea1cbc92835cc9dc21e:791-827
  // Sun Bo
  // 818287a2ab2baea1cbc92835cc9dc21e:768-773
  // C
  // C
  // CSSF15_ENG_284245cd1f:1:1
  String toAnnotationString(int gid) {
    StringBuilder sb = new StringBuilder();

    String completeQueryId;
    if(this.parentQueryId.equals(this.thisQueryId)) {
      completeQueryId = "CSSF15_ENG_" + this.parentQueryId;
      sb.append(completeQueryId + "_0_" + gid + "\t");
    }
    else {
      completeQueryId = "CSSF15_ENG_" + this.parentQueryId + "_" + this.thisQueryId;
      sb.append(completeQueryId + "_1_" + gid + "\t");
    }

    sb.append(completeQueryId + ":" + slot + "\t");

    StringBuilder relationProvenancesBuilder = new StringBuilder();
    for(EvalTextSpan evalTextSpan : relation_provenances)
      relationProvenancesBuilder.append(evalTextSpan.toString() + ",");

    String str_relation_provenance = relationProvenancesBuilder.toString();
    if(str_relation_provenance.endsWith(","))
      str_relation_provenance = str_relation_provenance.substring(0, str_relation_provenance.length()-1);
    sb.append(str_relation_provenance + "\t");

    sb.append(filler_text + "\t");

    sb.append(filler_eval_text_span.toString() + "\t");

    sb.append("C\t");
    sb.append("C\t");
//  sb.append(thisEc);
    sb.append(thisEc + "_" + gid);

    return sb.toString();
  }
}
