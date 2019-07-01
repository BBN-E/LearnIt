package com.bbn.akbc.evaluation.tac;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EquivalentClass {

  public String id;

  public String judgement;
  public int hop;

  public String responseID;


  public List<CSAssessment> listAssessment;

  public boolean inSys = false;

  public EquivalentClass(String id, int hop) {
    this.id = id;
    this.hop = hop;
    listAssessment = new ArrayList<CSAssessment>();
  }

  public EquivalentClass(String slineScorerLog) {
    String[] fields = slineScorerLog.trim().split(" ");
    hop = Integer.parseInt(fields[1]);
    judgement = fields[2];
    responseID = fields[4];
    id = fields[5].trim();

    listAssessment = new ArrayList<CSAssessment>();
  }

  public EquivalentClass(int hop, String judgement, String expandedQueryId, String responseId) {
    this.hop = hop;
    this.judgement = judgement;
    this.responseID = responseId;
    this.id = expandedQueryId + "_" + responseId;

    listAssessment = new ArrayList<CSAssessment>();
  }

  public void addAssessment(CSAssessment a) {
    listAssessment.add(a);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();
    result = prime * result + hop;
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
    EquivalentClass other = (EquivalentClass) obj;
    if (!id.equals(other.id)) {
      return false;
    }
    if (hop != other.hop) {
      return false;
    }
    return true;
  }

  public static String getEquivalentClassStr(CSAssessment assessment,
      Map<String, String> responseID2eclassID) {
    //	if(!responseID2eclassID.containsKey(assessment.parentId))

    //	responseID2eclassID.get(assessment.parentId) + ":" +

    // TODO Auto-generated method stub
    throw new RuntimeException("Not implemented yet");
  }
}
