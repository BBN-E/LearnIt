package com.bbn.akbc.neolearnit.observations.feature;

/**
 * Created by bmin on 6/6/15.
 */
public class NumOfTokensInBetween extends AbstractRelationFeature {

  private int numOfTokensInBetween;

  public NumOfTokensInBetween(int numOfTokensInBetween) {
    this.numOfTokensInBetween = numOfTokensInBetween;
  }

  public int getNumOfTokensInBetween() {
    return numOfTokensInBetween;
  }

  @Override
  public String toPrettyString() {
    return toString();
  }

  @Override
  public String toIDString() {
    return toString();
  }

  @Override
  public String toString() {
    return Integer.toString(numOfTokensInBetween);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + numOfTokensInBetween;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NumOfTokensInBetween other = (NumOfTokensInBetween) obj;
    if (numOfTokensInBetween != other.getNumOfTokensInBetween())
      return false;

    return true;
  }
}
