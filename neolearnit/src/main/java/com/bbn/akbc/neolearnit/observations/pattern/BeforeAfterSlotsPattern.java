package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.RegexPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.RegexPattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.*;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class BeforeAfterSlotsPattern extends Restriction
		implements RegexPatternBrandyRestrictor {

	@JsonProperty
	private final int slot;
	private final List<RegexableContent> content;
	@JsonProperty
	private final boolean before;
	@JsonProperty
	private final String language;

	@JsonProperty
	private ArrayList<RegexableContent> content() {
		return Lists.newArrayList(content);
	}

	public BeforeAfterSlotsPattern(String language, int slot, boolean before, List<RegexableContent> content) {
		this.language = language;
		this.slot = slot;
		this.before = before;

//		if (before)
//			this.content = Lists.reverse(content);
//		else
		this.content = content;
	}

	@JsonCreator
	private static BeforeAfterSlotsPattern from(
			@JsonProperty("language") String language,
			@JsonProperty("slot") int slot,
			@JsonProperty("before") boolean before,
			@JsonProperty("content") List<RegexableContent> content) {

		return new BeforeAfterSlotsPattern(language, slot, before, content);
	}

	@Override
	public boolean isInCanonicalSymmetryOrder() {
		return (before && slot == 0) || (!before && slot == 1);
	}

	public boolean isBeforeText() {
		return before;
	}

	public boolean isAfterText() {
		return !before;
	}


	@Override
	public RegexPattern restrictBrandyPattern(RegexPattern pattern) {
		List<Pattern> subpatterns = pattern.getSubpatterns();
		List<Pattern> beforeAfter = Lists.newArrayList();
		for (RegexableContent c : content) {
			beforeAfter.add(c.getPattern());
		}

		List<Pattern> newSubpatterns = Lists.newArrayList();
		if (before) {
			for (Pattern p : beforeAfter) newSubpatterns.add(p);
			for (Pattern p : subpatterns) newSubpatterns.add(p);
		} else {
			for (Pattern p : subpatterns) newSubpatterns.add(p);
			for (Pattern p : beforeAfter) newSubpatterns.add(p);
		}

		return pattern.modifiedCopyBuilder()
				.withSubpatterns(newSubpatterns)
				.build();

	}

	private BeforeAfterSlotsPattern getShorterForm(int numTokens) {
		List<RegexableContent> newContent = before ? content.subList(content.size() - numTokens, content.size()) : content.subList(0,numTokens);
		return new BeforeAfterSlotsPattern(language, slot, before, newContent);
	}

	public List<BeforeAfterSlotsPattern> getAllVersions() {
		List<BeforeAfterSlotsPattern> versions = new ArrayList<BeforeAfterSlotsPattern>();
		for (int i = 1; i <= content.size(); i++) {
			versions.add(getShorterForm(i));
		}
		return versions;
	}

	@Override
	public Optional<? extends LearnitPattern> getInitializationVersion() {
		if (content.size() == 0)
			return Optional.<LearnitPattern>absent();
		BeforeAfterSlotsPattern shorter = this.getShorterForm(1);
		if (shorter.getLexicalItems().isEmpty() || !Character.isLetter(shorter.getLexicalItems().iterator().next().toString().charAt(0)))
			return Optional.<LearnitPattern>absent();
		return Optional.of(shorter);
	}

	private boolean hasAppropriateSublistOf(List<RegexableContent> otherContent) {
		List<RegexableContent> sublist;
		if (this.before) {
			sublist = otherContent.subList(otherContent.size() - this.content.size(), otherContent.size());
		} else {
			sublist = otherContent.subList(0, this.content.size());
		}
		return this.content.equals(sublist);
	}

	private boolean isShorterVersionOf(BeforeAfterSlotsPattern other) {
		return this.slot == other.slot && this.before == other.before &&
				this.content.size() < other.content.size() &&
				this.hasAppropriateSublistOf(other.content);
	}

	/**
	 * The premise here is that we only store the longest before/after pattern, so to verify
	 * "{1} likes" we look for something like "{1} likes chocolate" on this instance.
	 */
	private boolean hasLongerVersionIn(Collection<LearnitPattern> patterns) {
		for (LearnitPattern pattern : patterns) {
			if (pattern instanceof BeforeAfterSlotsPattern && this.isShorterVersionOf((BeforeAfterSlotsPattern)pattern))
				return true;
		}
		return false;
	}

	@Override
	public boolean appliesTo(InstanceIdentifier instance, Mappings mappings) {
		Collection<LearnitPattern> patterns = mappings.getPatternsForInstance(instance);
		if (mappings.getAllPatterns().contains(this))
			return patterns.contains(this);
		else
			return this.hasLongerVersionIn(patterns);
	}

	@Override
	public String toString() {
		return "BeforeAfterSlotsPattern [slot=" + slot + ", content=" + content
				+ ", before=" + before + ", language=" + language + "]";
	}

	@Override
	@JsonProperty
	public String toPrettyString() {
		List<String> text = new ArrayList<String>();
		for (RegexableContent con : content) {
			text.add(con.toPrettyString());
		}
		if (before) {
			return StringUtils.SpaceJoin.apply(text) + " {"+ slot+"}";
		} else {
			return "{"+ slot+"} " + StringUtils.SpaceJoin.apply(text);
		}
	}
	@Override
	@JsonProperty
	public String toIDString() {
		return toPrettyString();
	}

	@Override
	public Set<Symbol> getLexicalItems() {
		Set<Symbol> result = new HashSet<Symbol>();
		for (RegexableContent c : this.content) {
			if (c instanceof SymbolContent) {
				result.add(((SymbolContent)c).getSymbol());
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (before ? 1231 : 1237);
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
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
		BeforeAfterSlotsPattern other = (BeforeAfterSlotsPattern) obj;
		if (before != other.before)
			return false;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (slot != other.slot)
			return false;
		return true;
	}

	public static class BeforeAfterSlotsRestrictionFactory extends RestrictionFactory {

		private final int maxRestrictions;

		public BeforeAfterSlotsRestrictionFactory(TargetAndScoreTables data, Mappings mappings) {
			super(data, mappings);
			maxRestrictions = LearnItConfig.getInt("max_before_after_restrictions_per_pattern");
		}

		@Override
		public Collection<Restriction> getRestrictions(LearnitPattern pattern) {
			final Multiset<Restriction> toReturn = HashMultiset.<Restriction>create();
			boolean useBefore = LearnItConfig.optionalParamTrue("use_before_text_for_regex");
			boolean useAfter  = LearnItConfig.optionalParamTrue("use_after_text_for_regex");

			for (InstanceIdentifier instance : mappings.getInstance2Pattern().getInstances(pattern)) {
				// if it's an instance of a known or similar seed
				for (Seed seed : mappings.getSeedsForInstance(instance)) {
					if (data.getSeedScores().isKnownFrozen(seed) || SeedSimilarity.getUnknownSeedScore(seed) > 0.3) {
						if (useBefore || useAfter) {
							for (LearnitPattern pat : mappings.getPatternsForInstance(instance)) {
								if (pat instanceof BeforeAfterSlotsPattern) {
									BeforeAfterSlotsPattern baPat = (BeforeAfterSlotsPattern)pat;
									if ((useBefore && baPat.isBeforeText()) || (useAfter && baPat.isAfterText())) {
										toReturn.addAll(((BeforeAfterSlotsPattern)pat).getAllVersions());
									}
								}
							}
						}
					}
				}
			}

			//lets only return the n most frequent
			List<Restriction> retList = Lists.newArrayList(toReturn.elementSet());
			Collections.sort(retList, new Comparator<Restriction>() {

				@Override
				public int compare(Restriction o1, Restriction o2) {
					return toReturn.count(o2) - toReturn.count(o1);
				}

			});

			return retList.subList(0, Math.min(retList.size(), maxRestrictions));
		}

	}

	@Override
	public boolean matchesPattern(LearnitPattern p) {
		// TODO Auto-generated method stub
		return false;
	}
}
