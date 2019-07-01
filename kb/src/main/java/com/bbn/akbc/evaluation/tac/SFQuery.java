package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.common.Slot;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class SFQuery extends Query {

  List<Slot> slotsToIgnore;
  String entityType;

  public SFQuery(String id, ImmutableList<TextMention> mentions,
      Optional<String> textForAnnotation,
      ImmutableList<Slot> slotsToIgnore,
      String entityType) {
    super(id, mentions, textForAnnotation);
    this.slotsToIgnore = slotsToIgnore;
    this.entityType = entityType;
  }

  public static class Builder {

    private final String id;
    private String entityType;
    private Optional<String> textForAnnotation;
    private final ImmutableList.Builder<TextMention> mentionListBuilder;
    private final ImmutableList.Builder<Slot> slotsToIgnoreBuilder;

    public Builder(String id) {
      this.id = id;
      this.mentionListBuilder = new ImmutableList.Builder<TextMention>();
      this.slotsToIgnoreBuilder = new ImmutableList.Builder<Slot>();
    }

    public Builder withEntityType(String entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withAddedMention(TextMention mention) {
      this.mentionListBuilder.add(mention);
      return this;
    }

    public Builder withAddedSlotToIgnore(Slot slot) {
      this.slotsToIgnoreBuilder.add(slot);
      return this;
    }

    public Builder withAddedTextForAnnotation(String textForAnnotation) {
      this.textForAnnotation = Optional.of(textForAnnotation);
      return this;
    }

    public SFQuery build() {
      return new SFQuery(id, mentionListBuilder.build(),
          textForAnnotation,
          slotsToIgnoreBuilder.build(),
          entityType);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[SFQuery: id=" + this.id + "\n");
    for (TextMention mention : this.mentions) {
      sb.append("[SFQuery: KBmention=" + mention.toString() + "\n");
    }
    for (Slot slot : this.slotsToIgnore) {
      sb.append("[SFQuery: slotsToIgnore=" + slot + "\n");
    }

    return sb.toString();
  }

  public List<Slot> getSlotsToIgnore() {
    return this.slotsToIgnore;
  }

  public String getEntityType() {
    return this.entityType;
  }
}
