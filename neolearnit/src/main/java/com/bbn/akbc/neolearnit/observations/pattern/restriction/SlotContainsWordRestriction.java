package com.bbn.akbc.neolearnit.observations.pattern.restriction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.MentionPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.ValueMentionPatternBrandyRestrictor;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.patterns.ValueMentionPattern;
import com.bbn.serif.theories.Mention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class SlotContainsWordRestriction extends SlotRestriction
		implements MentionPatternBrandyRestrictor, ValueMentionPatternBrandyRestrictor {

	private static final Pattern ID_STRING_FORMAT = Pattern.compile("\\[slot=(\\d), word=(\\w+), lang=(\\w+)\\]");

	@JsonProperty
	private final String word;

	@JsonProperty
	private final String language;

	@JsonCreator
	public SlotContainsWordRestriction(@JsonProperty("slot") int slot,
			@JsonProperty("word") String word, @JsonProperty("language") String language) {
		super(slot);
		this.word = word;
		this.language = language;
	}

	public String getWord() {
		return word;
	}

	protected RegexPattern buildRegex() {
		RegexPattern.Builder regexPatternBuilder = new RegexPattern.Builder();
		TextPattern.Builder textBuilder = new TextPattern.Builder();
		textBuilder.withText(this.word.toString().replace("\"","&quot;"));
		regexPatternBuilder.withSubpatterns(ImmutableList.<com.bbn.serif.patterns.Pattern>of(textBuilder.build()));
		return regexPatternBuilder.build();
	}

	@Override
	public MentionPattern restrictBrandyPattern(MentionPattern pattern) {

		if (SlotRestriction.getBrandyPatternSlot(pattern) == this.slot) {
			MentionPattern.Builder builder = pattern.modifiedCopyBuilder();
			builder.withRegexPattern(buildRegex());
			return builder.build();
		} else {
			return pattern;
		}
	}

	@Override
	public ValueMentionPattern restrictBrandyPattern(ValueMentionPattern pattern) {

		if (SlotRestriction.getBrandyPatternSlot(pattern) == this.slot) {
			ValueMentionPattern.Builder builder = pattern.modifiedCopyBuilder();
			builder.withRegexPattern(buildRegex());
			return builder.build();
		} else {
			return pattern;
		}
	}


	@Override
	public boolean appliesTo(InstanceIdentifier instance, Mappings mappings) {
		for (Seed seed : mappings.getSeedsForInstance(instance)) {
			if (seed.getLanguage().equals(language) || language.equals("")) {
				for (String w : seed.getSlotHeadText(slot).toString().split(" ")) {
					if (w.equals(word))
						return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("SlotContainsWordRestriction[slot=%d, word=%s, lang=%s]",slot,word,language);
	}

	@Override
	@JsonProperty
	public String toPrettyString() {
		return toString();
	}

	@Override
	@JsonProperty
	public String toIDString() {
		return String.format("[slot=%d, word=%s, lang=%s]",slot,word,language);
	}


	public static Optional<? extends Restriction> fromIDString(String idString) {
		Matcher idMatch = ID_STRING_FORMAT.matcher(idString);
		if (idMatch.matches()) {
			int foundSlot = Integer.parseInt(idMatch.group(1));
			String foundEtype = idMatch.group(2);
			String foundLang = idMatch.group(3);
			return Optional.of(new SlotContainsWordRestriction(foundSlot, foundEtype, foundLang));
		} else {
			return Optional.<Restriction>absent();
		}
	}

	@Override
	public Set<Symbol> getLexicalItems() {
		return ImmutableSet.of(Symbol.from(word));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SlotContainsWordRestriction other = (SlotContainsWordRestriction) obj;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}


	public static class SlotContainsWordRestrictionFactory extends RestrictionFactory {

		private static final int TOP_SLOT_WORDS = 20;
		private static final int MIN_WORD_COUNT = 5;
		private static final int PATTERN_COUNT_TRIGGER = 50;

		public SlotContainsWordRestrictionFactory(TargetAndScoreTables data, Mappings mappings) {
			super(data, mappings);
		}

		protected static synchronized Collection<String> getWords(Seed seed, int slot) {
			Set<String> words = new HashSet<String>();
			for (String word : seed.getSlotHeadText(slot).toString().split(" ")) {
				try {
					if (!StopWords.getDefault().isStopWord(word)) {
						words.add(word);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
					throw new RuntimeException("Must define stopwords to use containsWord restriction!");
				}
			}
			return words;
		}

		protected Multimap<String, Seed> collectSlotWordToSeedMultimap(
				String language, int slot, InstanceIdentifier instance) {

			Multimap<String,Seed> result = HashMultimap.<String,Seed>create();

			for (Seed seed : mappings.getSeedsForInstance(instance)) {
				if (seed.getLanguage().toLowerCase().equals(language.toLowerCase())) {

					Seed properSeed = seed;
					if (data.getTarget().getSlot(slot).useBestName())
						properSeed = seed.withProperText(data.getTarget());

					for (String word : getWords(seed,slot)) {
						result.put(word, properSeed);
					}

				}
			}

			return result;
		}

		protected List<String> getBestWords(String language, int slot, Collection<InstanceIdentifier> instances) {

			final Multimap<String,Seed> wordMap = HashMultimap.<String,Seed>create();
			for (InstanceIdentifier inst : instances) {
				if (inst.getSlotMentionType(slot).isPresent() &&
						inst.getSlotMentionType(slot).get().equals(Mention.Type.DESC)) {

					wordMap.putAll(collectSlotWordToSeedMultimap(language,slot,inst));
				}
			}

			List<String> uniqueWords = Lists.newArrayList(wordMap.keySet());
			Collections.sort(uniqueWords, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					if (wordMap.get(o1).size() == wordMap.get(o2).size()) {
						return o1.compareTo(o2);
					}

					return wordMap.get(o2).size() - wordMap.get(o1).size();
				}

			});

			int min = Math.max(MIN_WORD_COUNT, (int)Math.round(instances.size()*0.1));
			int max = (int)Math.round(instances.size()*0.9);

			List<String> bestWords = new ArrayList<String>();
			for (String word : uniqueWords) {
				int count = wordMap.get(word).size();

				if (count >= min && count < max) {
					bestWords.add(word);
				}
			}
			return bestWords.subList(0, Math.min(bestWords.size(), TOP_SLOT_WORDS));
		}

		@Override
		public Set<Restriction> getRestrictions(LearnitPattern pattern) {

			Collection<InstanceIdentifier> insts = mappings.getInstancesForPattern(pattern);
			if (insts.size() < PATTERN_COUNT_TRIGGER) return new HashSet<Restriction>();  // only restrict the big ones

			Set<Restriction> toReturn = new HashSet<Restriction>();
			//get best words for each language and slot
			for (String language : LearnItConfig.getList("languages")) {
				for (int slot : ImmutableList.of(0,1)) {

					//System.out.println("best "+language+" slot "+slot+"s for "+pattern.toIDString()+" = "+words);
					List<String> bestWords = getBestWords(language,slot,insts);

					if (bestWords.size() > 0) {
						System.out.println("Restricting "+pattern.toIDString()+" with "+slot+":"+language+":"+bestWords);
					}

					for (String word : bestWords) {
						toReturn.add(new SlotContainsWordRestriction(slot, word, language.toLowerCase()));
					}
				}
			}

			return toReturn;
		}

	}


	@Override
	public boolean matchesPattern(LearnitPattern p) {
		// TODO Auto-generated method stub
		return false;
	}

}
