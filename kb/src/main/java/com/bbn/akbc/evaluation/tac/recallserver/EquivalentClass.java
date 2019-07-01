package com.bbn.akbc.evaluation.tac.recallserver;

import com.bbn.akbc.evaluation.tac.Assessment;

import java.util.ArrayList;
import java.util.List;

public class EquivalentClass {

  public String id;
  public int hop;

  public String judgement;

  public List<Assessment> listAssessment;

  EquivalentClass(String id, int hop) {
    this.id = id;
    this.hop = hop;

    this.listAssessment = new ArrayList<Assessment>();
  }

  static EquivalentClass from(String id, int hop) {
    EquivalentClass eclass = new EquivalentClass(id, hop);
    return eclass;
  }

  public void addAssessment(Assessment a) {
    listAssessment.add(a);

    judgement = a.judgement1; // TODO: check whether all judgements are the same
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
}
