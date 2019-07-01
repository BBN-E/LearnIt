package com.bbn.akbc.common;

public class Triple {

  public String first;
  public String second;
  public String third;

  public Triple(String first, String second, String third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + first.hashCode();
    result = prime * result + second.hashCode();
    result = prime * result + third.hashCode();

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Triple other = (Triple) obj;
    if (!first.equals(other.first)) {
      return false;
    }
    if (!second.equals(other.second)) {
      return false;
    }
    if (!third.equals(other.third)) {
      return false;
    }

    return true;
  }

  public String toString() {
    return "<" + this.first + ", " + this.second + ", " + this.third + ">";
  }
}
