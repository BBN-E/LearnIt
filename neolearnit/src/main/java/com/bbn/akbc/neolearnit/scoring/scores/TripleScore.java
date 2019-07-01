package com.bbn.akbc.neolearnit.scoring.scores;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class TripleScore extends AbstractBootstrappedScore<SeedPatternPair> {

	@JsonProperty
	private Double precision = 0D;
	@JsonProperty
	private Double recall = 0D;
	@JsonProperty
	private Double confidence = 0D;
	@JsonProperty
	private Double confidenceDenominator = 0D;
	@JsonProperty
	private Double confidenceNumerator = 0D;
	@JsonProperty
	private Double confidenceFromSimilarity = 0D;
	@JsonProperty
	private Integer frequency = 0;
	@JsonProperty
	private Integer knownFrequency = 0;
	@JsonProperty
	private Integer frozenTripleInstanceCount = 0;  // # of instances in common with an already frozen triple
	@JsonProperty
	private Double tp=0D;
	@JsonProperty
	private Double fp=0D;
	@JsonProperty
	private Double fn=0D;
	@JsonProperty
	private Double tn=0D;

	private Multiset<SeedPatternPair> sources;

	private SeedScore seedScore;
	private PatternScore patternScore;

	@JsonProperty
	private EfficientMultisetDataStore<SeedPatternPair> sources() {
		return EfficientMultisetDataStore.fromMultiset(sources);
	}

	@JsonCreator
	private TripleScore(
			@JsonProperty("precision") double precision,
			@JsonProperty("recall") double recall,
			@JsonProperty("confidence") double confidence,
			@JsonProperty("confidenceFromSimilarity") double confidenceFromSimilarity,
			@JsonProperty("confidenceNumerator") double confidenceNumerator,
			@JsonProperty("confidenceDenominator") double confidenceDenominator,
			@JsonProperty("frequency") int frequency,
			@JsonProperty("knownFrequency") int knownFrequency,
			@JsonProperty("frozenTripleInstanceCount") int frozenTripleInstanceCount,
			@JsonProperty("tp") double tp,
			@JsonProperty("fp") double fp,
			@JsonProperty("fn") double fn,
			@JsonProperty("tn") double tn,
			@JsonProperty("frozen") boolean frozen,
			@JsonProperty("proposed") boolean proposed,
			@JsonProperty("iteration") int iteration,
			@JsonProperty("frozenIteration") int frozenIteration,
			@JsonProperty("seedScore") SeedScore seedScore,
			@JsonProperty("patternScore") PatternScore patternScore,
			@JsonProperty("sources") EfficientMultisetDataStore<SeedPatternPair> sources) {

		super(frozen, proposed, iteration, frozenIteration);
		this.precision = precision;
		this.recall = recall;
		this.confidence = confidence;
		this.confidenceFromSimilarity = confidenceFromSimilarity;
		this.confidenceNumerator = confidenceNumerator;
		this.confidenceDenominator = confidenceDenominator;
		this.frequency = frequency;
		this.knownFrequency = knownFrequency;
		this.frozenTripleInstanceCount = frozenTripleInstanceCount;
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.tn = tn;
		this.seedScore = seedScore;
		this.patternScore = patternScore;
		this.sources = sources.makeMultiset();
	}

	public TripleScore(int iteration) {
		super(false,true,iteration, -1);
		this.sources = HashMultiset.<SeedPatternPair>create();
	}

	public Double getPrecision() {
		return precision;
	}

	public void setPrecision(Double precision) {
		checkNotFrozen();
		this.precision = precision;
	}

	@Override
	public boolean isGood() {
		return this.precision >= 0.5;
	}

	public boolean isGood(double min_precision) {
	  return this.precision >= min_precision;
	}

  public Double getRecall() {
		return recall;
	}

	public void setRecall(Double recall) {
		checkNotFrozen();
		this.recall = recall;
	}

	@Override
	public Double getConfidence() {
		return confidence;
	}

	public void setConfidence(Double confidence) {
		checkNotFrozen();
		this.confidence = confidence;
	}

	public Double getConfidenceDenominator() {
		return confidenceDenominator;
	}

	public void setConfidenceDenominator(Double confidenceDenominator) {
		checkNotFrozen();
		this.confidenceDenominator = confidenceDenominator;
	}

	public void addToConfidenceDenominator(double value) {
		this.confidenceDenominator += value;
	}

	public Double getConfidenceNumerator() {
		return confidenceNumerator;
	}

	public void setConfidenceNumerator(Double confidenceNumerator) {
		checkNotFrozen();
		this.confidenceNumerator = confidenceNumerator;
	}

	public Double getConfidenceFromSimilarity() {
		return confidenceFromSimilarity;
	}

	public void setConfidenceFromSimilarity(Double confidenceFromSimilarity) {
		checkNotFrozen();
		this.confidenceFromSimilarity = confidenceFromSimilarity;
	}

	@Override
	public Double getScoreOrPrecision() {
		return precision;
	}

	@Override
	public Integer getFrequency() {
		return frequency;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}

	public Integer getFrozenTripleInstanceCount() {
		return frozenTripleInstanceCount;
	}

	public void setFrozenTripleInstanceCount(Integer count) {
		this.frozenTripleInstanceCount = count;
	}

	public Integer getKnownFrequency() {
		return knownFrequency;
	}

	// this can be changed on a frozen value
	// we use it for averaging, which has to be updated each iter
	public void setKnownFrequency(Integer frequency) {
		this.knownFrequency = frequency;
	}

	public SeedScore getSeedScore() {
		return this.seedScore;
	}

	public void setSeedScore(SeedScore seedScore) {
		checkNotFrozen();
		//if seedScore is frozen, setting seedScore should not be allowed
		this.seedScore.checkNotFrozen();
		this.seedScore = seedScore;
	}

	public PatternScore getPatternScore() {
		return this.patternScore;
	}

	public void setPatternScore(PatternScore patternScore) {
		checkNotFrozen();
		//if patternScore is frozen, setting patternScore should not be allowed
		this.patternScore.checkNotFrozen();
		this.patternScore = patternScore;
	}

	public void setDefaultTPFNStats() {
		tp = 99.;
		fp = 1.;
		fn = 1.;
//		tn = 899.;
		tn = 1.;
	}

	public void calculateTPFNStats(double goodSeedPrior, double totalInstanceDenominator) {
		/*tp = recall;
		fp = recall*(1.0/precision - 1.0);
		fn = (1.0 - recall);
		tn = 1.0/LearnItConfig.getDouble("good_seed_prior") - 1.0;*/

		tp = precision*confidenceDenominator;
		fp = (1.0-precision)*confidenceDenominator;

		double totalGood = goodSeedPrior*totalInstanceDenominator;

		if (totalGood == 0.0) {
			System.out.println("Couldn't estimate total good, using "+ tp*10);
			totalGood = tp*10;
		} else if (totalGood < tp*2) {
			totalGood = tp*2;
		}

		double totalBad = totalInstanceDenominator - totalGood;

		fn = totalGood-tp;
		tn = totalBad-fp;
	}

	public Double getTP() {
		return tp;
	}

	public Double getFP() {
		return fp;
	}

	public Double getFN() {
		return fn;
	}

	public Double getTN() {
		return tn;
	}

	public Double getInstanceRecall() {
		return tp/(tp+fn);
	}

	public void setSources(Multiset<SeedPatternPair> sources) {
		checkNotFrozen();
		this.sources = sources;
	}

	/**
	 * Used for tracking purposes, do not use algorithmically if there is
	 * a better way to get the relevant information because there is
	 * no contract for updating or ensuring the correctness of this multiset
	 * @return
	 */
	public Multiset<SeedPatternPair> getSources() {
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
	public int compareTo(BootstrappedScore<SeedPatternPair> o) {
		double val1 = this.getConfidence();
		double val2 = o.getConfidence();

		if (o instanceof TripleScore) {

			TripleScore pso = (TripleScore)o;

			if (val1 == val2) {
				// secondary sort is precision
				val1 = this.getPrecision();
				val2 = pso.getPrecision();
			}

			if (val1 == val2) {
				// tertiary sort is recall
				val1 = this.getKnownFrequency();
				val2 = pso.getKnownFrequency();
			}

		}

		if (val1 == val2) {
			return 0;
		} else {
			return -Double.compare(val1,val2);
		}
	}

	@Override
	public String toString() {
		return "TripleScore [precision=" + precision + ", recall=" + recall
				+ ", confidence=" + confidence + ", confidenceDenominator="
				+ confidenceDenominator + ", confidenceNumerator="
				+ confidenceNumerator + ", confidenceFromSimilarity="
				+ confidenceFromSimilarity + ", frequency=" + frequency
				+ ", knownFrequency=" + knownFrequency
				+ ", frozenTripleInstanceCount=" + frozenTripleInstanceCount
				+ ", seedScore=" + seedScore.toString()
				+ ", patternScore=" + patternScore.toString()
				+ ", tp=" + tp + ", fp=" + fp + ", fn=" + fn + ", tn=" + tn
				+ ", toString()=" + super.toString() + "]";
	}



}
