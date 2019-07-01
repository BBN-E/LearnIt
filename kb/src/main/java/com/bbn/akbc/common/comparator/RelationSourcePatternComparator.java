package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelation;
import com.bbn.akbc.common.FactConfidence;

import java.util.Comparator;

public class RelationSourcePatternComparator implements Comparator<TextRelation> {

  @Override
  public int compare(TextRelation arg0, TextRelation arg1) {
    if (arg0.isValSlot()
        && arg1.isValSlot()) { // value-slots: take the 2nd justifcation -> 1st is the filler
      double ret = 100.0 * (
          FactConfidence.getSourcePatternConf(arg1.justifications.get(1).sourcePattern.get()) -
              FactConfidence.getSourcePatternConf(arg0.justifications.get(1).sourcePattern.get()));
      return (int) ret;
    }

    double ret =
        100.0 * (FactConfidence.getSourcePatternConf(arg1.justifications.get(0).sourcePattern.get())
                     -
                     FactConfidence
                         .getSourcePatternConf(arg0.justifications.get(0).sourcePattern.get()));

    return (int) ret;
  }
}
