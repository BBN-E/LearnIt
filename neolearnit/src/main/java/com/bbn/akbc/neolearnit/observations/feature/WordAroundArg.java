package com.bbn.akbc.neolearnit.observations.feature;

/**
 * Created by bmin on 6/6/15.
 */
public class WordAroundArg extends AbstractWordRelationFeature {

  private int distanceToArg1;
  private int distanceToArg2;


  public WordAroundArg(String word,
      int distanceToArg1,
      int distanceToArg2) {

    super(word);
    this.distanceToArg1 = distanceToArg1;
    this.distanceToArg2 = distanceToArg2;
  }

  public int getDistanceToArg1() {
    return distanceToArg1;
  }

  public int getDistanceToArg2() {
    return distanceToArg2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getWord().hashCode();
    result = prime * result + distanceToArg1;
    result = prime * result + distanceToArg2;

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
    WordAroundArg other = (WordAroundArg) obj;
    if (!getWord().equals(other.getWord()))
      return false;
    if (distanceToArg1 != other.distanceToArg1)
      return false;
    if (distanceToArg2 != other.distanceToArg2)
      return false;

    return true;
  }
}
