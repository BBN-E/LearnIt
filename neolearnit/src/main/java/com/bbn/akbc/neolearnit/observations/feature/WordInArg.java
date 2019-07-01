package com.bbn.akbc.neolearnit.observations.feature;

/**
 * Created by bmin on 6/6/15.
 * - include HeadWordOfArg & WordsInArg
 */

public class WordInArg extends AbstractWordRelationFeature {
  private int slot;
  private boolean isHeadWord;

  public WordInArg(String word,
      int slot,
      boolean isHeadWord) {

    super(word);
    this.slot = slot;
    this.isHeadWord = isHeadWord;
  }

  public boolean getIsHeadWord() {
    return isHeadWord;
  }

  public int getSlot() {
    return slot;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + getWord().hashCode();
    result = prime * result + slot;
    if(isHeadWord)
      result = prime * result + 1;
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
    WordInArg other = (WordInArg) obj;
    if (!getWord().equals(other.getWord()))
      return false;
    if (slot != other.slot)
      return false;
    if (isHeadWord != other.isHeadWord)
      return false;

    return true;
  }
}
