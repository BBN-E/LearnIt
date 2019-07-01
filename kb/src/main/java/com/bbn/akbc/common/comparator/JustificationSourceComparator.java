package com.bbn.akbc.common.comparator;

import com.bbn.akbc.common.FactConfidence;
import com.bbn.akbc.common.Justification;

import java.util.Comparator;

public class JustificationSourceComparator implements Comparator<Justification> {

  @Override
  public int compare(Justification arg0, Justification arg1) {

    double ret =
        100.0 * (FactConfidence.getSourcePatternConf(arg1.sourcePattern.get()) - FactConfidence
            .getSourcePatternConf(arg0.sourcePattern.get()));
    return (int) ret;
  }
}
