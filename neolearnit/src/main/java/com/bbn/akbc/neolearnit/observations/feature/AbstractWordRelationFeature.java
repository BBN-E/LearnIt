package com.bbn.akbc.neolearnit.observations.feature;

import com.bbn.bue.common.symbols.Symbol;

/**
 * Created by bmin on 6/6/15.
 */
public abstract class AbstractWordRelationFeature extends AbstractRelationFeature {

  private Symbol word;

  public AbstractWordRelationFeature(String word) {
    this.word = Symbol.from(word);
  }

  public String getWord() {
    return word.toString();
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
    return word.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + word.hashCode();
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
    AbstractWordRelationFeature other = (AbstractWordRelationFeature) obj;
    if (!word.equals(other.word))
      return false;
    return true;
  }

}
