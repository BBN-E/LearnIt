package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.text.TextSpan;

import com.google.common.base.Optional;

public class LDCTabAnswer {

  String responseId;
  String queryId;
  String parentId;

  Slot slot;
  String docId;
  String text;

  TextSpan spanOfFiller;

  String source_pattern;

  LDCTabAnswer(String responseId, String queryId, String parentId,
      String slotName, String docId, String text,
      int filler_beg, int filler_end,
      String source_pattern) {
    this.responseId = responseId;
    this.queryId = queryId;
    this.parentId = parentId;
    slot = SlotFactory.fromStringSlotName(slotName);
    this.docId = docId;
    this.text = text;
    spanOfFiller = new TextSpan(filler_beg, filler_end);
    this.source_pattern = source_pattern;
  }

  public static Optional<LDCTabAnswer> fromLine(String line) {
    try {
      String[] fields = line.trim().split("\t");

      String responseId = fields[0];
      String queryId = fields[1];
      String parentId = fields[2];
      String slot = fields[3];
      String docId = fields[4];
      String text = fields[5];

      int filler_beg = Integer.parseInt(fields[6]);
      int filler_end = Integer.parseInt(fields[7]);

      String source_pattern = fields[10];

      return Optional.of(new LDCTabAnswer(responseId, queryId, parentId,
          slot, docId, text,
          filler_beg, filler_end,
          source_pattern));
    } catch (Exception e) {
      System.out.println("unable to parse LDCTabAnswer: " + line);
      return Optional.absent();
    }

  }

  public static Optional<LDCTabAnswer> fromLineCS2014(String line) {
    try {
      String[] fields = line.trim().split("\t");

      String responseId = "dummy response ID";
      String queryId = fields[0];
      String parentId = "dummy parent ID";
      String slot = fields[1];
      String docId = fields[7].substring(0, fields[7].indexOf(":"));
      String text = fields[6];

      String offsetPair = fields[7].substring(fields[7].indexOf(":") + 1);
      String[] items = offsetPair.split("-");

      int filler_beg = Integer.parseInt(items[0]);
      int filler_end = Integer.parseInt(items[1]);

      String source_pattern = "dummy source pattern";

      return Optional.of(new LDCTabAnswer(responseId, queryId, parentId,
          slot, docId, text,
          filler_beg, filler_end,
          source_pattern));
    } catch (Exception e) {
      System.out.println("unable to parse LDCTabAnswer: " + line);
      return Optional.absent();
    }

  }

  public String getSourcePattern() {
    return source_pattern;
  }

  public String getQueryId() {
    return queryId;
  }

  public String getDocId() {
    return docId;
  }

  public Slot getSlot() {
    return slot;
  }

  public TextSpan getSpanOfFiller() {
    return spanOfFiller;
  }

  public String toString() {
    return "[LDCTabAnswer: "
        + "responseId=" + responseId + ", "
        + "queryId=" + queryId + ", "
        + "parentId=" + parentId + ", "
        + "slot=" + slot.toString() + ", "
        + "docId=" + docId + ", "
        + "text=" + text + ", "
        + "spanOfFiller=" + spanOfFiller.toString() + ", "
        + "source_pattern=" + source_pattern
        + "]";
  }
}
