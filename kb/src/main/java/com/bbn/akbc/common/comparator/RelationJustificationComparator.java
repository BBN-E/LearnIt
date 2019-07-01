package com.bbn.akbc.common.comparator;

import com.bbn.akbc.kb.text.TextRelation;
import com.bbn.akbc.common.FactConfidence;
import com.bbn.akbc.common.Justification;

import java.util.Collections;
import java.util.Comparator;


public class RelationJustificationComparator implements Comparator<TextRelation> {

  @Override
  public int compare(TextRelation arg0, TextRelation arg1) {
    // inferred relations are less precise
    if (arg0.isInfered() && !arg1.isInfered()) {
      return 1;
    }
    if (!arg0.isInfered() && arg1.isInfered()) {
      return -1;
    }

    Collections.sort(arg0.justifications, new JustificationSourceComparator());
    Collections.sort(arg1.justifications, new JustificationSourceComparator());

    Justification j1 = arg0.justifications.get(0);
    Justification j2 = arg1.justifications.get(0);

    double ret =
        100.0 * (FactConfidence.getSourcePatternConf(j2.sourcePattern.get()) - FactConfidence
            .getSourcePatternConf(j1.sourcePattern.get()));
    return (int) ret;
  }


/*
  @Override
  public int compare(KBRelation arg0, KBRelation arg1) {
  */
    /*
    // TODO: temporary fix to obey generator contract of comparator
    if(arg0.equals(arg1))
      return 0;
    //
    */

    /*
    // inferred relations are less precise
    if (arg0.isInfered && !arg1.isInfered) {
      return 1;
    }
    if (!arg0.isInfered && arg1.isInfered) {
      return -1;
    }

    List<Justification> justificationJointList = new ArrayList<Justification>();

    Map<Justification, Integer> justificationToArg = new HashMap<Justification, Integer>();

    System.out.println("arg0: " + arg0.isInfered + ", " + arg0.toSimpleString());
    System.out.println("arg1: " + arg1.isInfered + ", " + arg1.toSimpleString());

    for (Justification j : arg0.justifications) {
      justificationToArg.put(j, 0);
    }
    for (Justification j : arg1.justifications) {
      justificationToArg.put(j, 1);
    }

    justificationJointList.addAll(arg0.justifications);
    justificationJointList.addAll(arg1.justifications);

    // step 1: rank by source pattern
    Collections.sort(justificationJointList, new JustificationSourceComparator());
    int newSize = 3;
    if (justificationJointList.size() < 3) {
      newSize = justificationJointList.size();
    }
    justificationJointList = justificationJointList.subList(0, newSize); // pick the top 3
    // step 2: rank by brandy confidence
    Collections.sort(justificationJointList, new JustificationBrandyComparator());

    int argId = justificationToArg.get(justificationJointList.get(0));
    if (argId == 1) {
      return 1;
    } else
      return -1;
  }
*/


}
