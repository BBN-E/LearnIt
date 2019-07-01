package com.bbn.akbc.evaluation.tac;

import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.text.TextMention;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class CSInitQuery extends CSQuery {

  @JsonProperty
  List<Slot> slotsHop0;
  @JsonProperty
  List<Slot> slotsHop1;

  @JsonCreator
  public CSInitQuery(
      @JsonProperty("id") String id,
      @JsonProperty("mentions") List<TextMention> mentions,
      @JsonProperty("textForAnnotation") Optional<String> textForAnnotation,
      @JsonProperty("slotsHop0") List<Slot> slotsHop0,
      @JsonProperty("slotsHop1") List<Slot> slotsHop1) {

    super(id, mentions, textForAnnotation);

    this.slotsHop0 = slotsHop0;
    this.slotsHop1 = slotsHop1;
  }

  // TODO: check text consistency, get best name
  public String getText() {
    return mentions.get(0).getText().get();
  }

  // TODO: get doc list
  public String getDocId() {
    return mentions.get(0).getDocId();
  }

  public String getSlotsHop0Str() {
    StringBuilder sb = new StringBuilder();
    for (Slot slot : this.slotsHop0) {
      sb.append("," + slot.toString());
    }
    if (sb.toString().startsWith(",")) {
      return sb.toString().substring(1);
    }

    return sb.toString();
  }

  public String getSlotsHop1Str() {
    StringBuilder sb = new StringBuilder();
    for (Slot slot : this.slotsHop1) {
      sb.append("," + slot.toString());
    }
    if (sb.toString().startsWith(",")) {
      return sb.toString().substring(1);
    }

    return sb.toString();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[CSInitQuery: id=" + this.id + "\n");
    for (TextMention mention : this.mentions) {
      sb.append("[CSInitQuery: KBmention=" + mention.toString() + "\n");
    }
    for (Slot slot0 : this.slotsHop0) {
      sb.append("[CSInitQuery: slot0=" + slot0 + "\n");
    }
    for (Slot slot1 : this.slotsHop1) {
      sb.append("[CSInitQuery: slot1=" + slot1 + "\n");
    }

    return sb.toString();
  }

  public static class Builder {

    private final String id;
    private Optional<String> textForAnnotation = Optional.absent();
    private final ImmutableList.Builder<TextMention> mentionListBuilder;
    private final ImmutableList.Builder<Slot> slotsHop0Builder;
    private final ImmutableList.Builder<Slot> slotsHop1Builder;

    public Builder(String id) {
      this.id = id;
      this.mentionListBuilder = new ImmutableList.Builder<TextMention>();
      this.slotsHop0Builder = new ImmutableList.Builder<Slot>();
      this.slotsHop1Builder = new ImmutableList.Builder<Slot>();
    }

    public Builder withAddedMention(TextMention mention) {
      this.mentionListBuilder.add(mention);
      return this;
    }

    public Builder withAddedSlotHop1(Slot slot) {
      this.slotsHop1Builder.add(slot);
      return this;
    }

    public Builder withAddedSlotHop0(Slot slot) {
      this.slotsHop0Builder.add(slot);
      return this;
    }

    public Builder withAddedTextForAnnotation(String textForAnnotation) {
      this.textForAnnotation = Optional.of(textForAnnotation);
      return this;
    }

    public CSInitQuery build() {
      return new CSInitQuery(id, mentionListBuilder.build(), textForAnnotation,
          slotsHop0Builder.build(), slotsHop1Builder.build());
    }
  }

  public static CSInitQuery fromSFQuery(SFQuery slotFillerQuery) {
    CSInitQuery.Builder builder = new CSInitQuery.Builder(slotFillerQuery.id);
    // add mentions
    for (TextMention m : slotFillerQuery.mentions) {
      builder.withAddedMention(m);
    }
    // add valid slots
    ImmutableSet<Slot> slots = SlotFactory.getSFSlotValues(slotFillerQuery.getSlotsToIgnore(),
        slotFillerQuery.getEntityType());
    for (Slot slot : slots) {
      builder.withAddedSlotHop0(slot);
    }
    // add text for annotation
    if (slotFillerQuery.textForAnnotation.isPresent()) {
      builder.withAddedTextForAnnotation(slotFillerQuery.textForAnnotation.get());
    }
    return builder.build();
  }

	/*
         * TODO: write a to xml function
	 */


}
