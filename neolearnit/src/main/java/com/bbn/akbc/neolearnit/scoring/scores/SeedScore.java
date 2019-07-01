package com.bbn.akbc.neolearnit.scoring.scores;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SeedScore extends AbstractBootstrappedScore<Seed> {

	@JsonProperty
	private Double score;
	@JsonProperty
	private Double confidence; // set to confidenceNumerator/confidenceDenominator
	@JsonProperty
	private Double confidenceNumerator;
	@JsonProperty
	private Double confidenceDenominator;
	@JsonProperty
	private Integer frequency;
	@JsonProperty
	private Integer knownFrequency;

	@JsonProperty
	private double seedWeightedPrecision = 0D;

	@JsonProperty
	private double seedPrecision = 0D;

	@JsonProperty
	private double seedFrequency = 0D;


	private Multiset<LearnitPattern> sources;
	@JsonProperty
	private EfficientMultisetDataStore<LearnitPattern> sources() {
		return EfficientMultisetDataStore.fromMultiset(sources);
	}

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public Map<String,Double> scoreForFrontendRanking;

	@JsonCreator
	private SeedScore(
			@JsonProperty("score") double score,
			@JsonProperty("confidence") double confidence,
			@JsonProperty("frequency") int frequency,
			@JsonProperty("knownFrequency") int knownFrequency,
			@JsonProperty("confidenceNumerator") double confidenceNumerator,
			@JsonProperty("confidenceDenominator") double confidenceDenominator,
			@JsonProperty("frozen") boolean frozen,
			@JsonProperty("proposed") boolean proposed,
			@JsonProperty("iteration") int iteration,
			@JsonProperty("frozenIteration") int frozenIteration,
			@JsonProperty("sources") EfficientMultisetDataStore<LearnitPattern> sources) {
		super(frozen,proposed,iteration,frozenIteration);
		this.score = score;
		this.confidence = confidence;
		this.frequency = frequency;
		this.knownFrequency = knownFrequency;
		this.confidenceNumerator = confidenceNumerator;
		this.confidenceDenominator = confidenceDenominator;
		this.sources = sources.makeMultiset();
		Random r = new Random();
		Map<String, Double> sortingKeyToValue = new HashMap<>();

		// frequency

		sortingKeyToValue.put("byFrequencyAscend",(double)this.frequency);
		sortingKeyToValue.put("byFrequencyDescend",0.0-this.frequency);

		// recall
		sortingKeyToValue.put("byConfidenceAscend", this.confidence);
		sortingKeyToValue.put("byConfidenceDescend", 0-this.confidence);

		this.scoreForFrontendRanking = sortingKeyToValue;
	}

	public SeedScore(int iteration) {
		super(false,true,iteration,-1);
		this.score = 0D;
		this.frequency = 0;
		this.knownFrequency = 0;
		this.confidenceNumerator = 0D;
		this.confidenceDenominator = 1D;
		this.confidence = 0D;
		this.sources = HashMultiset.create();
		Random r = new Random();
		Map<String, Double> sortingKeyToValue = new HashMap<>();

		// frequency

		sortingKeyToValue.put("byFrequencyAscend",(double)this.frequency);
		sortingKeyToValue.put("byFrequencyDescend",0.0-this.frequency);

		// recall
		sortingKeyToValue.put("byConfidenceAscend", this.confidence);
		sortingKeyToValue.put("byConfidenceDescend", 0-this.confidence);

		this.scoreForFrontendRanking = sortingKeyToValue;
	}

    public void setIdenticalTo(SeedScore s) {
        this.score = s.score;
        this.frequency = s.frequency;
        this.knownFrequency = s.knownFrequency;
        this.confidenceNumerator = s.confidenceNumerator;
        this.confidenceDenominator = s.confidenceDenominator;
        this.confidence = s.confidence;
        this.sources = s.sources;
    }

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		checkNotFrozen();
		this.score = score;
	}

	@Override
	public boolean isGood() {
		return this.score >= 0.5;
	}

	@Override
	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		checkNotFrozen();
		this.confidence = confidence;
	}

	public Double getConfidenceNumerator() {
		return confidenceNumerator;
	}

	public void setConfidenceNumerator(Double confidenceNumerator) {
		checkNotFrozen();
		this.confidenceNumerator = confidenceNumerator;
	}

	public Double getConfidenceDenominator() {
		return confidenceDenominator;
	}

	public void setConfidenceDenominator(Double confidenceDenominator) {
		checkNotFrozen();
		this.confidenceDenominator = confidenceDenominator;
	}

	@Override
	public Double getScoreOrPrecision() {
		return score;
	}

	@Override
	public Integer getFrequency() {
		return frequency;
	}

	public void setFrequency(Integer frequency) {
		checkNotFrozen();
		this.frequency = frequency;
	}

	public Integer getKnownFrequency() {
		return knownFrequency;
	}

	public void setKnownFrequency(Integer knownFrequency) {
		checkNotFrozen();
		this.knownFrequency = knownFrequency;
	}

	public void setSources(Multiset<LearnitPattern> sources) {
		checkNotFrozen();
		this.sources = sources;
	}


	public double getSeedPrecision() {
		return this.seedPrecision;
	}

	public double getSeedWeightedPrecision() {
		return this.seedWeightedPrecision;
	}

	public double getSeedFrequency() {
		return this.seedFrequency;
	}

	public void setSeedPrecision(Set<LearnitPattern> patternsFrozen, Set<LearnitPattern> patternsMatched) {
		Set<LearnitPattern> intersection = new HashSet<LearnitPattern>(patternsFrozen);
		intersection.retainAll(patternsMatched);
		double precision = intersection.size() * 1.0 / patternsMatched.size();
		this.seedPrecision = precision;
	}

	public void setSeedWeightedPrecision(Set<LearnitPattern> patternsFrozen, Set<LearnitPattern> patternsMatched) {
		Set<LearnitPattern> intersection = new HashSet<LearnitPattern>(patternsFrozen);
		intersection.retainAll(patternsMatched);
		double weightedPrecision = intersection.size() * 1.0 * Math.log(intersection.size() + 1) / patternsMatched.size();
		this.seedWeightedPrecision = weightedPrecision;
	}

	public void setSeedFrequency(Set<LearnitPattern> patternsFrozen, Set<LearnitPattern> patternsMatched) {
		Set<LearnitPattern> intersection = new HashSet<LearnitPattern>(patternsFrozen);
		intersection.retainAll(patternsMatched);
		double frequency = intersection.size() * 1.0;
		this.seedFrequency = frequency;
	}

	/**
	 * Used for tracking purposes, do not use algorithmically if there is
	 * a better way to get the relevant information because there is
	 * no contract for updating or ensuring the correctness of this multiset
	 * @return
	 */
	public Multiset<LearnitPattern> getSources() {
		return sources;
	}

	/**
	 * call this to reduce the extractor size for tractability reasons
	 */
	public void clearSources() {
		sources.clear();
	}

	/**
	 * Boosts the confidence of this score by the specified amount
	 * this can be called after the object is frozen as a way to 'bias'
	 * the learning toward older seeds/patterns
	 * @param amount
	 */
	public void boostConfidence(double amount) {
		if (this.confidence+amount > 1) {
			this.confidence = 1.0;
		} else {
			this.confidence += amount;
		}
	}

	@Override
	public int compareTo(BootstrappedScore<Seed> o) {
		double val1 = this.getConfidence();
		double val2 = o.getConfidence();

		if (val1 == val2 && o instanceof SeedScore) {
			SeedScore ss = (SeedScore)o;

			// secondary is score
			val1 = this.getScore();
			val2 = ss.getScore();
		}

		return -Double.compare(val1,val2);


	}

	@Override
	public String toString() {
		return super.toString()
				+" [score=" + score
				+ ", confidence=" + confidence
				+ ", frequency=" + frequency
				+ ", knownFrequency=" + knownFrequency + "]";
	}

}
