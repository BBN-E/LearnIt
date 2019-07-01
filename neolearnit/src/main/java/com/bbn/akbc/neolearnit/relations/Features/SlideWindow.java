package com.bbn.akbc.neolearnit.relations.Features;

public class SlideWindow {

  String tokenPrevious;
  String currentToken;
  String tokenNext;

  int distanceToHeadOfSlot0;
  int distanceToHeadOfSlot1;

  SlideWindow(String tokenPrevious,
      String currentToken,
      String tokenNext,
      int distanceToHeadOfSlot0,
      int distanceToHeadOfSlot1) {
    this.tokenPrevious = tokenPrevious;
    this.currentToken = currentToken;
    this.tokenNext = tokenNext;

    this.distanceToHeadOfSlot0 = distanceToHeadOfSlot0;
    this.distanceToHeadOfSlot1 = distanceToHeadOfSlot1;
  }

  public String toString() {
    return "[" + tokenPrevious + "|" + currentToken + "|" + tokenNext + "|" + distanceToHeadOfSlot0
        + "|" + distanceToHeadOfSlot1 + "]";
  }
}
