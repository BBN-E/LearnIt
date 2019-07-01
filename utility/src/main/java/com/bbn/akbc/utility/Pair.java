package com.bbn.akbc.utility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Class used for serializing maps, don't know if there's a better way
 * @author mshafir
 *
 * refactored by by bmin on 5/12/15.
 *
 * @param <T>
 * @param <U>
 */
public class Pair<T,U> {
  @JsonProperty
  public T key;
  @JsonProperty
  public U value;
  @JsonCreator
  public Pair(@JsonProperty("key") T key, @JsonProperty("value") U value) {
    this.key = key;
    this.value = value;
  }
  public static <T,U> Pair<T, U> fromEntry(Map.Entry<T,U> entry) {
    return new Pair<T,U>(entry.getKey(),entry.getValue());
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
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
    Pair other = (Pair) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }
  @Override
  public String toString() {
    return String.format("(%s, %s)",key.toString(),value.toString());
  }
}
