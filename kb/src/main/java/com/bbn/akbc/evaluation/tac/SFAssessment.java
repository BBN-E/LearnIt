package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextSpan;

import java.util.ArrayList;

public class SFAssessment extends Assessment {

  public String query;
  public String slot;
  public String docId;
  public String text;
  public ArrayList<TextSpan> span;
  public int evalYear = 2000;

  public SFAssessment() {
  }

  // only deal with correct judgements
  public boolean fromLine2012(String strLine2012) {
    evalYear = 2012;

    String[] items = strLine2012.trim().split("\t");
    this.docId = items[2];

    String judgement = items[3];
    if (!judgement.equals("1")) {
      return false;
    }

    String[] columns = items[1].split(":");
    this.query = columns[0];
    this.slot = columns[1] + ":" + columns[2];

    this.text = items[5];

    String[] offsets = items[7].split(":");

    this.span = new ArrayList<TextSpan>();
    this.span.add(new TextSpan(Integer.parseInt(offsets[0]), Integer.parseInt(offsets[1])));

    return true;
  }

  // only deal with correct judgements
  public boolean fromLine2013(String strLine2013) {
    evalYear = 2013;

    String[] items = strLine2013.trim().split("\t");
    this.docId = items[2];

    String judgement = items[10];
    if (!judgement.equals("C") && !judgement.equals("R")) {
      return false;
    }

    String[] columns = items[1].split(":");
    this.query = columns[0];
    this.slot = columns[1] + ":" + columns[2];

    this.text = items[3];

    this.span = new ArrayList<TextSpan>();

    for (String offsetPair : items[4].split(",")) {
      String[] offsets = offsetPair.split("-");
      this.span.add(new TextSpan(Integer.parseInt(offsets[0]), Integer.parseInt(offsets[1])));
    }

    return true;
  }
}
