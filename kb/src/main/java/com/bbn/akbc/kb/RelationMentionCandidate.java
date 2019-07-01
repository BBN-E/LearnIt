package com.bbn.akbc.kb;

import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.RelationMention;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.Optional;


public final class RelationMentionCandidate {
  public Mention leftMention;
  public Optional<Mention> rightMention = Optional.absent();
  public Optional<ValueMention> rightValueMention = Optional.absent();
  public double score;
  public String type;

  // relation justification span
  public int startCharOffSetInclusive;
  public int endCharOffSetInclusive;

  public RelationMentionCandidate(Mention leftMention,
      Mention rightMention,
      double score,
      String type,
      int startCharOffSetInclusive,
      int endCharOffSetInclusive) {

    this.leftMention = leftMention;
    this.rightMention = Optional.of(rightMention);
    this.score = score;
    this.type = type;
    this.startCharOffSetInclusive = startCharOffSetInclusive;
    this.endCharOffSetInclusive = endCharOffSetInclusive;
  }

  public RelationMentionCandidate(Mention leftMention,
      ValueMention rightMention,
      double score,
      String type,
      int startCharOffSetInclusive,
      int endCharOffSetInclusive) {

    this.leftMention = leftMention;
    this.rightValueMention = Optional.of(rightMention);
    this.score = score;
    this.type = type;
    this.startCharOffSetInclusive = startCharOffSetInclusive;
    this.endCharOffSetInclusive = endCharOffSetInclusive;
  }

  public static RelationMentionCandidate fromRelationMentionInSentenceTheory(RelationMention relationMention) {
    if(relationMention.timeArg().isPresent()) {
      return new RelationMentionCandidate(relationMention.leftMention(),
          relationMention.timeArg().get(),
          relationMention.score(),
          relationMention.type().asString(),
          relationMention.span().charOffsetRange().startInclusive().asInt(),
          relationMention.span().charOffsetRange().endInclusive().asInt());
    }
    else {
      return new RelationMentionCandidate(relationMention.leftMention(),
          relationMention.rightMention(),
          relationMention.score(),
          relationMention.type().asString(),
          relationMention.span().charOffsetRange().startInclusive().asInt(),
          relationMention.span().charOffsetRange().endInclusive().asInt());
    }
  }

  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("<" + leftMention.tokenSpan().originalText().content().utf16CodeUnits() + ", " + type);
    if(rightValueMention.isPresent())
      stringBuilder.append(", " + rightValueMention.get().tokenSpan().originalText().content().utf16CodeUnits());
    else
      stringBuilder.append(", " + rightMention.get().tokenSpan().originalText().content().utf16CodeUnits());
    stringBuilder.append(">");

    return stringBuilder.toString();
  }
}
