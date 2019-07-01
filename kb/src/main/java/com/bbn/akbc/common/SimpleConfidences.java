package com.bbn.akbc.common;


/**
 * Created by bmin on 2/12/16.
 */
public class SimpleConfidences {
  public static double getConfidenceByProduct(String sourcePattern,
      double linkConfidenceArg1, double linkConfidenceArg2) {
    double sourcePatternConfidence = FactConfidence.getSourcePatternConfAsProbabilities(sourcePattern);
    return sourcePatternConfidence*linkConfidenceArg1*linkConfidenceArg2;
  }
}
