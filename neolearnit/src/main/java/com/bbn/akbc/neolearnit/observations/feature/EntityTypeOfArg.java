package com.bbn.akbc.neolearnit.observations.feature;

import com.bbn.bue.common.symbols.Symbol;

/**
 * Created by bmin on 6/6/15.
 */
public class EntityTypeOfArg extends AbstractRelationFeature {

  private int slot;
  private Symbol entityType;

  public EntityTypeOfArg(String entityType,
      int slot) {
    this.entityType = Symbol.from(entityType);
    this.slot = slot;
  }

  public String getEntityType() {
    return entityType.toString();
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
    return slot + ":" + entityType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entityType.hashCode();
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
    EntityTypeOfArg other = (EntityTypeOfArg) obj;
    if (!entityType.equals(other.getEntityType()))
      return false;
    if (slot != other.getSlot())
      return false;

    return true;
  }
}
