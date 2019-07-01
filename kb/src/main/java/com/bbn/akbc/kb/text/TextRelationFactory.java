package com.bbn.akbc.kb.text;

import com.bbn.akbc.kb.common.Slot;
import com.bbn.akbc.kb.common.SlotFactory;
import com.bbn.akbc.common.Justification;

import com.google.common.base.Optional;

import java.util.ArrayList;

public class TextRelationFactory {

  public static Optional<TextRelationMention> getReverseRelationMention(TextRelationMention rm) {
    // need answer type
    if (!rm.answer.getType().isPresent()) {
      return Optional.absent();
    }

    Optional<Slot> inverseSlot = SlotFactory.getInverseSlot(rm.slot, rm.answer.getType().get());

//		if(rm.slot.toString().contains("shareholders"))
//			System.out.println();

    if (!inverseSlot.isPresent()) {
      return Optional.absent();
    } else {
      return Optional.of(new TextRelationMention(rm.answer, rm.query, inverseSlot.get()));
    }
  }

  public static Optional<TextRelation> getReverseRelation(TextRelation r) {
//		if(r.slot.toString().contains("shareholders"))
//			System.out.println();

    // no reverse for value slots
    if (r.isValSlot()) {
      return Optional.absent();
    }

//		System.out.println("getReverseRelation\toriginal\t" + r.toSimpleString());

    Optional<Slot> inverseSlot = SlotFactory.getInverseSlot(r.getSlot(), r.getAnswerType().get());
    if (!inverseSlot.isPresent()) {
      return Optional.absent();
    } else {
      // reverse the relation args and mentions
      TextRelation inverse = new TextRelation((TextEntity)r.answer(), (TextEntity)r.query(), inverseSlot.get());
      for (TextRelationMention rm : r.getMentions()) {
        TextRelationMention reverseRM = new TextRelationMention(rm.answer, rm.query, inverseSlot.get());

        if (rm.spanOfSent().isPresent()) // don't do it for multi-doc relations
        {
          reverseRM.setSpanOfSent(rm.spanOfSent().get());
        }

        reverseRM.listJustifications = new ArrayList<Justification>(rm.listJustifications);

        reverseRM.setConfidence(rm.confidence.get());
        reverseRM.setSourcePattern(rm.sourcePattern.get());
        reverseRM.isValueSlot = rm.isValueSlot;

        inverse.addMention(reverseRM);
      }

      inverse.sourcePatterns = r.sourcePatterns;
      inverse.mentionCount = r.mentionCount;

      inverse.justifications = r.justifications;

      return Optional.of(inverse);
    }
  }
}
