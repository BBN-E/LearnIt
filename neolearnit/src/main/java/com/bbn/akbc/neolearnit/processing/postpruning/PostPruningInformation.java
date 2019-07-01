package com.bbn.akbc.neolearnit.processing.postpruning;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PostPruningInformation implements PartialInformation {

	private final Multimap<LearnitPattern,String> patternMatches;
	private final Multiset<LearnitPattern> patternCounts;
	@JsonProperty
	private EfficientMultimapDataStore<LearnitPattern,String> patternMatches() {
		return EfficientMultimapDataStore.<LearnitPattern,String>fromMultimap(patternMatches);
	}
	@JsonProperty
	private EfficientMultisetDataStore<LearnitPattern> patternCounts() {
		return EfficientMultisetDataStore.<LearnitPattern>fromMultiset(patternCounts);
	}

	private PostPruningInformation(Multimap<LearnitPattern,String> patternMatches,
			Multiset<LearnitPattern> patternCounts) {
		this.patternMatches = patternMatches;
		this.patternCounts = patternCounts;
	}

	@JsonCreator
	private PostPruningInformation(
			@JsonProperty("patternMatches") EfficientMultimapDataStore<LearnitPattern,String> matches,
			@JsonProperty("patternCounts") EfficientMultisetDataStore<LearnitPattern> counts) {
		this.patternMatches = matches.makeMultimap();
		this.patternCounts = counts.makeMultiset();
	}

	public Collection<String> getMatches(LearnitPattern pattern) {
		return patternMatches.get(pattern);
	}

	public Set<LearnitPattern> getAllRecordedPatterns() {
		return patternMatches.keySet();
	}

	public Multiset<LearnitPattern> getPatternCounts() {
		return patternCounts;
	}

	public static class Builder {

		private final int instMax;
		private final Multimap<LearnitPattern,String> patternMatches;
		private final Multiset<LearnitPattern> patternCounts;

		private final static Multiset<LearnitPattern> allPatternCount = ConcurrentHashMultiset.create();

		public Builder(int max) {
			instMax = max;
			patternMatches = HashMultimap.create();
			patternCounts = HashMultiset.create();
		}

		private synchronized String getPatternStringUnderMax(Target target, LearnitPattern pattern, InstanceIdentifier id) throws IOException {
			if (allPatternCount.count(pattern) < instMax) {
				String result = MatchInfoDisplay.fromMatchInfo(id.reconstructMatchInfo(target), Optional.<Map<Symbol,Symbol>>absent()).html();
				allPatternCount.add(pattern);
				return result;
			} else {
				return "";
			}
		}

		public Builder withPatternMatch(Target target, LearnitPattern pattern, InstanceIdentifier id) throws IOException {
			patternCounts.add(pattern);
			String patString = getPatternStringUnderMax(target,pattern,id);
			if (!patString.isEmpty()) {
				patternMatches.put(pattern,patString);
			}
			return this;
		}

		private Builder withAllPatternMatches(LearnitPattern pattern, Collection<String> matches) {
			for (String match : matches) {
				patternMatches.put(pattern,match);
			}
			return this;
		}

		public Builder withInfo(PostPruningInformation info) {
			for (LearnitPattern pattern : info.getAllRecordedPatterns()) {
				withAllPatternMatches(pattern, info.getMatches(pattern));
			}
			for (LearnitPattern p : info.getPatternCounts().elementSet()) {
				// let's keep it under 1 billion or we'll crash (only because it's happened)
				if (patternCounts.count(p) + info.getPatternCounts().count(p) >= 1000000000L) {
					patternCounts.setCount(p, 1000000000);
				} else {
					patternCounts.add(p, info.getPatternCounts().count(p));
				}
			}
			return this;
		}

		public PostPruningInformation build() {
			return new PostPruningInformation(patternMatches,patternCounts);
		}
	}

}
