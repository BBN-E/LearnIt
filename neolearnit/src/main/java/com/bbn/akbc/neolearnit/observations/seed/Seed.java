package com.bbn.akbc.neolearnit.observations.seed;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.Entity.RepresentativeMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Seed extends LearnItObservation {

	private final List<Slot> slots;
	@JsonProperty
	private Slot slot0() {
		return slots.get(0);
	}
	@JsonProperty
	private Slot slot1() {
		if(slots.size()<=1)
			return new Slot(Symbol.from("")); // hack to support unary relations
		else
			return slots.get(1);
	}
	@JsonProperty
	private final String language;

	@JsonCreator
	private Seed(@JsonProperty("language") String language,
			@JsonProperty("slot0") Slot slot0,
			@JsonProperty("slot1") Slot slot1) {
		this(language.toLowerCase(), ImmutableList.of(slot0,slot1));
	}

	private Seed(String language, List<Slot> slots) {
		if (language.equals("")) {
			this.language = LearnItConfig.getList("languages").get(0).toLowerCase();
		} else {
			this.language = language;
		}
		this.slots = slots;
	}

	public List<String> getSlotTokens() {
		List<String> result = new ArrayList<String>();
		for (String slot : slots.get(0).getText().toString().split(" ")) {
			result.add(slot);
		}
		for (String slot : slots.get(1).getText().toString().split(" ")) {
			result.add(slot);
		}
		return result;
	}

	private static Optional<String> tryGetBestName(DocTheory dt, Mention m) {
		Optional<Entity> e = m.entity(dt);
		if (e.isPresent()) {
			RepresentativeMention repMention = e.get().representativeMention();
			return Optional.of(repMention.span().tokenizedText().utf16CodeUnits());
		}
		return Optional.absent();
	}

	private static Optional<String> tryGetNormalizedDate(ValueMention m) {
		Optional<Value> v = m.documentValue();
		if (v.isPresent() && v.get().isSpecificDate()) {
			Optional<Symbol> timexVal = v.get().timexVal();
			if (timexVal.isPresent())
				return Optional.of(timexVal.get().toString());
		}
		return Optional.absent();
	}

	public static Seed from(String language, String slot0, String slot1) {
		return new Seed(language,new Slot(Symbol.from(slot0)), new Slot(Symbol.from(slot1)));
	}

	public static synchronized Seed from(LanguageMatchInfo match, boolean symmetric) {
		// GET Best Names (TODO: this should be configurable)
		List<String> slot0Fillers = new ArrayList<String>();
		List<String> slot1Fillers = new ArrayList<String>();

        boolean descParent = LearnItConfig.optionalParamTrue("use_parent_for_desc");

		//REMEMBER TO LOWER CASE TEXT
		if(match.getSlot0().isPresent()) {

			if (match.getSlot0().get() instanceof Mention) {
				Mention m0 = (Mention) match.getSlot0().get();
				String headText;
				if (m0.mentionType() == Mention.Type.DESC && descParent && m0
						.atomicHead().parent().isPresent())
					headText = m0.atomicHead().parent().get().span()
							.tokenizedText().utf16CodeUnits()
							.toLowerCase();
				else
					headText = m0.atomicHead().span().tokenizedText()
							.utf16CodeUnits().toLowerCase();
				//Get best name if available. Add it if it's different than the head text (to save space)
				Optional<String> bestName0 =
						tryGetBestName(match.getDocTheory(), m0);
				if (bestName0.isPresent() && !bestName0.get().toLowerCase()
						.equals(headText))
					slot0Fillers.add(bestName0.get().toLowerCase());
				slot0Fillers.add(headText);
			} else if (match.getSlot0().get() instanceof ValueMention) {
				//"Best name" for dates is the normalized YYYY-MM-DD form
				Optional<String> normDate0 = tryGetNormalizedDate(
						(ValueMention) match.getSlot0().get());
				if (normDate0.isPresent()) {
					slot0Fillers.add(normDate0.get().toLowerCase());
				}
				slot0Fillers.add(match.getSlot0().get().span().tokenizedText()
						.utf16CodeUnits().toLowerCase());
			} else if (match.getSlot0().get() instanceof EventMention) {
				EventMention eventMention = (EventMention) match.getSlot0().get();

				// slot0Fillers.add(eventMention.anchorNode().span().originalText().content().utf16CodeUnits());
				// ATTN: use patternID

				// TODO: Change below back
//				if (eventMention.pattern().isPresent())
//					slot0Fillers.add(eventMention.pattern().get().asString());
//				else
//					slot0Fillers.add(eventMention.anchorNode().span()
//							.originalText().content().utf16CodeUnits());
				slot0Fillers.add(eventMention.anchorNode().head().span().tokenizedText().utf16CodeUnits());

			/*
			if(eventMention.anchorNode().span().originalText().content().isPresent())
				slot0Fillers.add(eventMention.anchorNode().span().originalText().content().utf16CodeUnits());
			else
				slot0Fillers.add("No text for event mention");
				*/
			} else {
				System.err.print("Incorrect slot0 type");
				System.exit(-1);
			}
		}

		if(match.getSlot1().isPresent()) {
			if (match.getSlot1().get() instanceof Mention) {
				Mention m1 = (Mention) match.getSlot1().get();
				String headText;
				if (m1.mentionType() == Mention.Type.DESC && descParent && m1
						.atomicHead().parent().isPresent())
					headText = m1.atomicHead().parent().get().span()
							.tokenizedText().utf16CodeUnits()
							.toLowerCase();
				else
					headText = m1.atomicHead().span().tokenizedText()
							.utf16CodeUnits().toLowerCase();
				//Get best name if available. Add it if it's different than the head text (to save space)
				Optional<String> bestName1 =
						tryGetBestName(match.getDocTheory(), m1);
				if (bestName1.isPresent() && !bestName1.get().toLowerCase()
						.equals(headText))
					slot1Fillers.add(bestName1.get().toLowerCase());
				slot1Fillers.add(headText);
			} else if (match.getSlot1().get() instanceof ValueMention) {
				//"Best name" for dates is the normalized YYYY-MM-DD form
				Optional<String> normDate1 = tryGetNormalizedDate(
						(ValueMention) match.getSlot1().get());
				if (normDate1.isPresent()) {
					slot1Fillers.add(normDate1.get().toLowerCase());
				}
				slot1Fillers.add(match.getSlot1().get().span().tokenizedText()
						.utf16CodeUnits().toLowerCase());
			} else if (match.getSlot1().get() instanceof SynNode) {
				slot1Fillers.add("Context");
			} else if (match.getSlot1().get() instanceof EventMention) {
				EventMention eventMention = (EventMention) match.getSlot1().get();

				// slot1Fillers.add(eventMention.anchorNode().span().originalText().content().utf16CodeUnits());
				// ATTN: use patternID

				//TODO: Change below back
//				if (eventMention.pattern().isPresent())
//					slot1Fillers.add(eventMention.pattern().get().asString());
//				else
//					slot1Fillers.add(eventMention.anchorNode().span()
//							.originalText().content().utf16CodeUnits());

				slot1Fillers.add(eventMention.anchorNode().head().span().tokenizedText().utf16CodeUnits());
			/*
			if(eventMention.anchorNode().span().originalText().isPresent())
				slot1Fillers.add(eventMention.anchorNode().span().originalText().get().text());
			else
				slot1Fillers.add("No text for event mention");
				*/
			} else {
				System.err.print("Incorrect slot1 type");
				System.exit(-1);
			}
		}

		if(slot1Fillers.isEmpty())
			return new Seed(match.getLanguage(), ImmutableList.of(new Slot(slot0Fillers)));
		else
			return new Seed(match.getLanguage(), ImmutableList.of(new Slot(slot0Fillers), new Slot(slot1Fillers)));
	}

	public static Seed from(Element seed) {
		String slot0 = XMLUtils.requiredAttribute(seed, "slot0").toLowerCase();
		String slot1 = XMLUtils.requiredAttribute(seed, "slot1").toLowerCase();
		String language = XMLUtils.defaultStringAttribute(seed, "language",
				LearnItConfig.getList("languages").get(0)).toLowerCase();

		return new Seed(language, ImmutableList.of(new Slot(Symbol.from(slot0)), new Slot(Symbol.from(slot1))));
	}

	public List<Symbol> getSlots() {
		return ImmutableList.of(getSlot(0),getSlot(1));
	}

	public Symbol getSlot(int slot) {
		return slot == 0 ? slots.get(0).getText() : slots.get(1).getText();
	}

	public Symbol getSlotHeadText(int slot) {
		return slot == 0 ? slots.get(0).getHeadText() : slots.get(1).getHeadText();
	}

    public String getReducedForm(StopWords stopwords) {
        List<String> clean = Lists.newArrayList();
        for (String slot : getStringSlots()) {
            if (slot.matches(".*\\d.*")) return this.toSimpleString(); //Don't mess with numbers (probably values); they don't make sense here
            List<String> cleanSlot = Lists.newArrayList();
            for (String token : slot.toLowerCase().split(" ")) {
                if (!stopwords.isStopWord(token) && !token.endsWith(".")) cleanSlot.add(token);
            }
            if (cleanSlot.size() == 0)
                return this.toSimpleString();
            else
                clean.add(StringUtils.SpaceJoin.apply(cleanSlot));
        }

        return clean.isEmpty() ? this.toSimpleString() : Joiner.on(", ").join(clean);
    }

	public String getLanguage() {
		return language;
	}

	public List<String> getStringSlots() {
		List<String> result = new ArrayList<String>();
		for (Slot slot : slots) {
			result.add(slot.getText().toString());
		}
		return result;
	}

	public String toSimpleString() {
		if (slots.size() != 2)
			throw new RuntimeException("Seed with wrong number of slots: "+slots.size());

		return StringUtils.join(slots, "\t");
	}

	public Seed makeSymmetric() {
		List<Slot> slotsForSorting = new ArrayList<Slot>(slots);
		Collections.sort(slotsForSorting, new Comparator<Slot>() {

			@Override
			public int compare(Slot arg0, Slot arg1) {
				return arg0.getText().toString().compareTo(arg1.getText().toString());
			}

		});
		return new Seed(language, slotsForSorting);
	}

    public Seed reversed() {
        return new Seed(language, ImmutableList.of(slots.get(1), slots.get(0)));
    }

	public Seed withProperText(Target target) {
		return this.withProperText(target.getSlot(0).useBestName(), target.getSlot(1).useBestName());
	}

	public Seed withProperText(boolean slot0UseBestName, boolean slot1UseBestName) {
		return new Seed(language, ImmutableList.of(Slot.from(slots.get(0), slot0UseBestName),
								  				   Slot.from(slots.get(1), slot1UseBestName)));
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
		return language+":("+StringUtils.CommaJoin.apply(slots)+")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((slots == null) ? 0 : slots.hashCode());
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
		Seed other = (Seed) obj;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (slots == null) {
			if (other.slots != null)
				return false;
		} else if (!slots.equals(other.slots))
			return false;
		return true;
	}

}
