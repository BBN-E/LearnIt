package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.text.TextEntity;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelation;
import com.bbn.akbc.kb.text.TextRelationMention;
import com.bbn.akbc.common.Justification;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelationFromSubmission {

  public String srcId;
  public Slot slot;
  public String dstId;

  public List<Justification> justifications;

  public double conf;

  public boolean isValueSlot = false;

  public RelationFromSubmission(String sline) {
    this(sline, false);
  }

  public RelationFromSubmission(String sline, boolean adjust_offsets_for_adept) {
    String[] tmpVars1 = sline.split("\t");

    srcId = tmpVars1[0];
    slot = SlotFactory.fromStringSlotName(tmpVars1[1]);
    dstId = tmpVars1[2];

    if (dstId.startsWith("\"") && dstId.endsWith("\"")) {
      isValueSlot = true;
    }

    justifications = new ArrayList<Justification>();

    String[] tmpVars2 = tmpVars1[3].split(",|;");
    /*
    for (String strJustification : tmpVars2) {
      Justification newJustification = Justification.fromString(strJustification, adjust_offsets_for_adept);

      boolean alreadyExistOrCovered = false;
      for (Justification justification : justifications) {
        if (justification.covers(newJustification)) {
          alreadyExistOrCovered = true;
        } else if (newJustification.covers(justification)) {
          justifications.remove(justification);
          break;
        }
      }

      if(!alreadyExistOrCovered)
        justifications.add(newJustification);
    }
    */

    /*
    // skip first provenance which is the filler span
    int startIdx=0;
    if(isValueSlot)
      startIdx=1;
    //
    */

    for (int i=0; i<tmpVars2.length; i++) {
      String strJustication = tmpVars2[i];
      justifications.add(Justification.fromString(strJustication, adjust_offsets_for_adept));
    }

    conf = Double.parseDouble(tmpVars1[4]);
  }

  /**
   * Note: use relation confidencd as relation mention confidence
   * @param eid2entities
   * @return
   */
  public TextRelation toKBPRelation(Map<String, TextEntity> eid2entities) {
    TextRelation kbpRelation = null;

    TextEntity query = eid2entities.get(srcId);

    List<Justification> justificationsAsRelationMention = justifications;

    // entity slot
    if(!isValueSlot) {
      TextEntity answer = eid2entities.get(dstId);
      kbpRelation = new TextRelation(query, answer, slot);
    }
    // value slot
    else {
      // Justification valueJustification = justifications.get(justifications.size()-1);
      System.out.println("REMOVE first justification for value-slot relation");
      Justification valueJustification = justifications.get(0);

      TextMention value = new TextMention(valueJustification.docId,
          valueJustification.span.getFirst(), valueJustification.span.getSecond());

      value.setText(dstId.substring(1, dstId.length()-1));

      kbpRelation = new TextRelation(query, value, slot);

      // justificationsAsRelationMention = justificationsAsRelationMention.subList(0, justificationsAsRelationMention.size()-1);
      justificationsAsRelationMention = justificationsAsRelationMention.subList(1, justificationsAsRelationMention.size());
    }

    // update relation map
    if(isValueSlot)
      kbpRelation.setToValueSlot();

    kbpRelation.mentionCount = Optional.of(justificationsAsRelationMention.size());
    for(Justification justification : justificationsAsRelationMention) {
      /*
       * Note: Here span(query)=span(answer)=span(sentence)
       */
      TextMention queryMention = new TextMention(justification.docId, justification.span.getFirst(), justification.span.getSecond());
      TextMention answerMention = new TextMention(justification.docId, justification.span.getFirst(), justification.span.getSecond());
      TextRelationMention
          kbpRelationMention = new TextRelationMention(queryMention, answerMention, slot);

      // use relation confidencd as relation mention confidence
      kbpRelationMention.setConfidence(this.conf);

      kbpRelation.addMention(kbpRelationMention);
    }

    kbpRelation.setConfidence(this.conf);

    return kbpRelation;
  }

  public String getSignature() {
    return srcId + "-" + slot.toString() + "-" + dstId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(srcId + "-" + slot.toString() + "-" + dstId + "\t");
    sb.append(conf + "\t");
    for(Justification justification : justifications)
      sb.append(justification.toString() + "\t");

    return sb.toString();
  }
}
