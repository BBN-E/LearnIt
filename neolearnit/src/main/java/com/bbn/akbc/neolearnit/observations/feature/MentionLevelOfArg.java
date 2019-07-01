package com.bbn.akbc.neolearnit.observations.feature;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;

/**
 * Created by bmin on 6/6/15.
 */
public class MentionLevelOfArg extends AbstractRelationFeature {

  private int slot;
  private Symbol mentionType;

  public MentionLevelOfArg(Mention.Type mentionType,
      int slot) {
    this.mentionType = Symbol.from(mentionType.toString());
    this.slot = slot;
  }

  public Mention.Type getMentionType() {
    return Mention.typeForSymbol(Symbol.from(mentionType.toString().toLowerCase()));
  }

  public int getSlot() {
    return slot;
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
    return slot + ":" + mentionType.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + mentionType.hashCode();
    result = prime * result + slot;
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
    MentionLevelOfArg other = (MentionLevelOfArg) obj;
    if (!mentionType.equals(other.getMentionType()))
      return false;
    if (slot != other.getSlot())
      return false;

    return true;
  }
}
