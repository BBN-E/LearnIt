package com.bbn.akbc.neolearnit.observations.pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.LabelPatternReturn;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternReturn;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableSet;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class BetweenSlotsPattern extends SlotPairPattern<BetweenSlotsContent<RegexableContent>> implements BrandyablePattern {

	public BetweenSlotsPattern(String language, Integer firstSlot, Integer secondSlot,
			BetweenSlotsContent<RegexableContent> content) {
		super(language, firstSlot, secondSlot, content);
	}

	@JsonCreator
	public static BetweenSlotsPattern from(
			@JsonProperty("language") String language,
			@JsonProperty("firstSlot") Integer firstSlot,
			@JsonProperty("secondSlot") Integer secondSlot,
			@JsonProperty("content") BetweenSlotsContent<RegexableContent> content) {

		return new BetweenSlotsPattern(language, firstSlot, secondSlot, content);
	}

	public static BetweenSlotsPattern from(RegexPattern brandyRegexPattern) {
		if (brandyRegexPattern.getSubpatterns().size() != 3) {
			throw new NonConvertibleException("Brandy RegexPatterns being converted into LearnIt BetweenSlotsPatterns must have 3 subpatterns");
		}
		Pattern beforePattern = brandyRegexPattern.getSubpatterns().get(0);
		Pattern betweenPattern = brandyRegexPattern.getSubpatterns().get(1);
		Pattern afterPattern = brandyRegexPattern.getSubpatterns().get(2);

		Integer firstSlot = getSlotReturn(beforePattern);
		Integer secondSlot = getSlotReturn(afterPattern);

		if (!(betweenPattern instanceof TextPattern)) {
			throw new NonConvertibleException("Brandy RegexPatterns being converted into LearnIt BetweenSlotsPatterns must have the middle pattern be a TextPattern");
		}

		TextPattern tp = (TextPattern) betweenPattern;
		BetweenSlotsContent.Builder<RegexableContent> contentBuilder = new BetweenSlotsContent.Builder<>();
		contentBuilder.withAddContent(new SymbolContent(tp.getText()));

		return from("en", firstSlot, secondSlot, contentBuilder.build());
	}

	private static Integer getSlotReturn(Pattern p) {
		String error = "When converting into a LearnIt BetweenSlotPatterns First and third subpatterns must have a return value of either 'slot1' or 'slot2'";
		PatternReturn pr = p.getPatternReturn();
		if (pr == null)
			throw new NonConvertibleException(error);
		LabelPatternReturn lpr = (LabelPatternReturn) pr;
		Symbol label = lpr.getLabel();
		if (label.equalTo(Symbol.from("slot1")))
			return 1;
		else if (label.equalTo(Symbol.from("slot2")))
			return 2;
		throw new NonConvertibleException(error);
	}

	@Override
	public Pattern convertToBrandy(String factType, Target target, Iterable<Restriction> restrictions) {
		RegexPattern.Builder regexBuilder = new RegexPattern.Builder();

		List<Pattern> subpatterns = new ArrayList<Pattern>();
		subpatterns.add(target.makeSlotBrandyPattern(factType, firstSlot, restrictions));
		for (RegexableContent c : content.getContent()) {
			subpatterns.add(c.getPattern());
		}
		subpatterns.add(target.makeSlotBrandyPattern(factType, secondSlot, restrictions));

		regexBuilder.withSubpatterns(subpatterns);

		return regexBuilder.build();
	}

	@Override
	public boolean isInCanonicalSymmetryOrder() {
		return firstSlot == 0 && secondSlot == 1;
	}

	@Override
	@JsonProperty
	public String toPrettyString() {
		return "{"+firstSlot + "} "+ content.toPrettyString() + " {"+ secondSlot+"}";
	}
	@Override
	@JsonProperty
	public String toIDString() {
		return toPrettyString();
	}

	public static Set<String> stopwords = ImmutableSet.of("have","had", "at", "in", "for", "to", "of", "a",
			"an", "the", "nor", "with", "and", "on", "that", "not", "can", "was", "its", "by", "will", "his", "would", "are", "at",
			"any", "this", "'s", "from", "he", "as", "him", "who", "no", "some", "also", "about", "which", "her", "into", "this", "up",
			"under", "two", "after", "into", "all", "those", "'", "you", "what", "him", "new",
			"their", "is", "were", "be", "has", "have", "more", "been", "more", "could", "them", "it", "will",
			"our", "while", "but", "told", "over", "``", "--", "military", "that", "say", "your", "should", "their", "great", "he", "another", "these", "I");

	@Override
	public Set<Symbol> getLexicalItems() {
		Set<Symbol> result = new HashSet<Symbol>();
		for (RegexableContent c : this.content.getContent()) {
			if (c instanceof SymbolContent) {
				Symbol symbol = ((SymbolContent)c).getSymbol();
				if(!stopwords.contains(symbol.asString())) {
					result.add(symbol);
				}
			}
		}
		return result;
	}

    private boolean matchesContent(RegexableContent s, RegexableContent r) {
    	if(r instanceof SymbolContent && s instanceof SymbolContent) {
    		return ((SymbolContent)r).getSymbol() == ((SymbolContent)s).getSymbol();
    	}

    	return false;
    }

    @Override
    public boolean matchesPattern(LearnitPattern p) {
        if(!(p instanceof BetweenSlotsPattern))
            return false;
        BetweenSlotsPattern slotsPattern = (BetweenSlotsPattern)p;
        if ((slotsPattern.getFirstSlot() != firstSlot || slotsPattern.getSecondSlot() != secondSlot))
            return false;
        if (slotsPattern.getContent().getContent().size() != this.getContent().getContent().size())
            return false;
        for (int i=0; i < this.getContent().getContent().size(); ++i) {
            if (!matchesContent(this.getContent().getContent().get(i), slotsPattern.getContent().getContent().get(i)))
                return false;
        }
        return true;
    }
}
