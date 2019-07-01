package com.bbn.akbc.neolearnit.relations.utility;


import com.bbn.akbc.common.Pair;

import java.util.Comparator;

/**
 * Created by bmin on 6/24/15.
 */
public class ConfidenceComparator implements Comparator<Pair<Long, Double>> {
  @Override
  public int compare(Pair<Long, Double> p1, Pair<Long, Double> p2) {
    if(p1.getSecond()>p2.getSecond())
      return 1;
    else if(p1.getSecond()<p2.getSecond())
      return -1;
    else
      return 0;
  }

}
