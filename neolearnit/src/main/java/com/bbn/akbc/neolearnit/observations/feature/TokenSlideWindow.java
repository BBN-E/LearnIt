package com.bbn.akbc.neolearnit.observations.feature;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 6/6/15.
 */
public class TokenSlideWindow extends AbstractRelationFeature {
  static int windowSize = 3;

  public static int getWindowSize() {
    return windowSize;
  }


  private List<Symbol> words; // ordered token list
  private int distanceToHeadOfArg1;
  private int distanceToHeadOfArg2;

  public TokenSlideWindow(List<String> wordStrings,
      int distanceToHeadOfArg1,
      int distanceToHeadOfArg2) {

    words = new ArrayList<Symbol>();
    for(String wordString : wordStrings)
      words.add(Symbol.from(wordString));

    this.distanceToHeadOfArg1 = distanceToHeadOfArg1;
    this.distanceToHeadOfArg2 = distanceToHeadOfArg2;
  }

  public List<String> getWords() {
    List<String> wordStrings = new ArrayList<String>();
    for(Symbol word : words)
      wordStrings.add(word.toString());
    return wordStrings;
  }

  public int getDistanceToHeadOfArg1() {
    return distanceToHeadOfArg1;
  }

  public int getDistanceToHeadOfArg2() {
    return distanceToHeadOfArg2;
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
    return windowSize + ":" + distanceToHeadOfArg1 + ":" + distanceToHeadOfArg2 + ":" + StringUtils.join(words, ",");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    for(Symbol word : words)
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
    TokenSlideWindow other = (TokenSlideWindow) obj;
    for(int i=0; i<words.size(); i++) {
      if (!words.get(i).equals(other.words.get(i)))
        return false;
    }
    if(distanceToHeadOfArg1 != other.distanceToHeadOfArg1)
      return false;
    if(distanceToHeadOfArg2 != other.distanceToHeadOfArg2)
      return false;

    return true;
  }
}
