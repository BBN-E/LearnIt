package com.bbn.akbc.neolearnit.scoring.scores;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.*;

public class PatternScore extends AbstractBootstrappedScore<LearnitPattern> {

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
	private Integer seedFrequency = 0;
	@JsonProperty
	private Integer knownSeedFrequency = 0;
	@JsonProperty
	private Integer frozenPatternInstanceCount = 0;  // # of instances in common with an already frozen pattern
	@JsonProperty
	private Double tp=0D;
	@JsonProperty
	private Double fp=0D;
	@JsonProperty
	private Double fn=0D;
	@JsonProperty
	private Double tn=0D;

    @JsonProperty
    private double patternWeightedPrecision = 0D;

    @JsonProperty
    private double patternPrecision = 0D;

    @JsonProperty
    private double patternFrequency = 0D;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public Map<String,Double> scoreForFrontendRanking;

	private Multiset<Seed> sources;
	@JsonProperty
	private EfficientMultisetDataStore<Seed> sources() {
		return EfficientMultisetDataStore.fromMultiset(sources);
	}

	@JsonCreator
	private PatternScore(
			@JsonProperty("precision") double precision,
			@JsonProperty("recall") double recall,
			@JsonProperty("confidence") double confidence,
			@JsonProperty("confidenceFromSimilarity") double confidenceFromSimilarity,
			@JsonProperty("confidenceNumerator") double confidenceNumerator,
			@JsonProperty("confidenceDenominator") double confidenceDenominator,
			@JsonProperty("frequency") int frequency,
			@JsonProperty("knownFrequency") int knownFrequency,
			@JsonProperty("seedFrequency") int seedFrequency,
			@JsonProperty("knownSeedFrequency") int knownSeedFrequency,
			@JsonProperty("frozenPatternInstanceCount") int frozenPatternInstanceCount,
			@JsonProperty("tp") double tp,
			@JsonProperty("fp") double fp,
			@JsonProperty("fn") double fn,
			@JsonProperty("tn") double tn,
			@JsonProperty("frozen") boolean frozen,
			@JsonProperty("proposed") boolean proposed,
			@JsonProperty("iteration") int iteration,
			@JsonProperty("frozenIteration") int frozenIteration,
			@JsonProperty("sources") EfficientMultisetDataStore<Seed> sources) {

		super(frozen, proposed, iteration, frozenIteration);
		this.precision = precision;
		this.recall = recall;
		this.confidence = confidence;
		this.confidenceFromSimilarity = confidenceFromSimilarity;
		this.confidenceNumerator = confidenceNumerator;
		this.confidenceDenominator = confidenceDenominator;
		this.seedFrequency = seedFrequency;
		this.knownSeedFrequency = knownSeedFrequency;
		this.frequency = frequency;
		this.knownFrequency = knownFrequency;
		this.frozenPatternInstanceCount = frozenPatternInstanceCount;
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.tn = tn;
		this.sources = sources.makeMultiset();
        Random r = new Random();
        Map<String, Double> sortingKeyToValue = new HashMap<>();

		// precision
		sortingKeyToValue.put("byPrecisionAscend", this.precision);
		sortingKeyToValue.put("byPrecisionDescend", 0-this.precision);

		// recall
		sortingKeyToValue.put("byRecallAscend", this.recall);
		sortingKeyToValue.put("byRecalldescend", 0-this.recall);

		// confidence
		sortingKeyToValue.put("byConfidenceAscend", this.confidence);
		sortingKeyToValue.put("byConfidenceDescend", 0-this.confidence);

        // frequency
        sortingKeyToValue.put("byFrequencyAscend", (double)this.frequency);
        sortingKeyToValue.put("byFrequencyDescend", 0.0-this.frequency);

		this.scoreForFrontendRanking = sortingKeyToValue;
	}

	public PatternScore(int iteration) {
		super(false,true,iteration, -1);
        this.sources = HashMultiset.create();
        Random r = new Random();
        Map<String, Double> sortingKeyToValue = new HashMap<>();

        // precision
        sortingKeyToValue.put("byPrecisionAscend", this.precision);
        sortingKeyToValue.put("byPrecisionDescend", 0-this.precision);

        // recall
        sortingKeyToValue.put("byRecallAscend", this.recall);
        sortingKeyToValue.put("byRecalldescend", 0-this.recall);

        // confidence
        sortingKeyToValue.put("byConfidenceAscend", this.confidence);
        sortingKeyToValue.put("byConfidenceDescend", 0-this.confidence);

        // frequency
        sortingKeyToValue.put("byFrequencyAscend", (double)this.frequency);
        sortingKeyToValue.put("byFrequencyDescend", 0.0-this.frequency);

        this.scoreForFrontendRanking = sortingKeyToValue;
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

	public Integer getFrozenPatternInstanceCount() {
		return frozenPatternInstanceCount;
	}

	public void setFrozenPatternInstanceCount(Integer count) {
		this.frozenPatternInstanceCount = count;
	}

	public Integer getKnownFrequency() {
		return knownFrequency;
	}

	// this can be changed on a frozen value
	// we use it for averaging, which has to be updated each iter
	public void setKnownFrequency(Integer frequency) {
		this.knownFrequency = frequency;
	}

	//deprecated
	//public Integer getSeedFrequency() {
	//	return seedFrequency;
	//}

	public void setSeedFrequency(Integer frequency) {
		checkNotFrozen();
		this.seedFrequency = frequency;
	}

	public Integer getKnownSeedFrequency() {
		return knownSeedFrequency;
	}

	// this can be changed on a frozen value
	// we use it for averaging, which has to be updated each iter
	public void setKnownSeedFrequency(Integer frequency) {
		this.knownSeedFrequency = frequency;
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

	public void setSources(Multiset<Seed> sources) {
		checkNotFrozen();
		this.sources = sources;
	}

    public Double getPatternWeightedPrecision() {
        return this.patternWeightedPrecision;
    }

    public void setPatternWeightedPrecision(Set<Seed> seedsFrozen, Set<Seed> seedsMatched) {
        Set<Seed> intersection = new HashSet<Seed>(seedsFrozen);
        intersection.retainAll(seedsMatched);
        double patternWeightedPrecision = intersection.size() * 1.0 * Math.log(intersection.size() + 1) / seedsMatched.size();
        this.patternWeightedPrecision = patternWeightedPrecision;
    }

    public Double getPatternPrecision() {
        return this.patternPrecision;
    }

    public void setPatternPrecision(Set<Seed> seedsFrozen, Set<Seed> seedsMatched) {
        Set<Seed> intersection = new HashSet<Seed>(seedsFrozen);
        intersection.retainAll(seedsMatched);
        double precision = intersection.size() * 1.0 / seedsMatched.size();
        this.patternPrecision = precision;
    }

    public Double getPatternFrequency() {
        return this.patternFrequency;
    }

    public void setPatternFrequency(Set<Seed> seedsFrozen, Set<Seed> seedsMatched) {
        Set<Seed> intersection = new HashSet<Seed>(seedsFrozen);
        intersection.retainAll(seedsMatched);
        double frequency = intersection.size();
        this.patternFrequency = frequency;
    }


	/**
	 * Used for tracking purposes, do not use algorithmically if there is
	 * a better way to get the relevant information because there is
	 * no contract for updating or ensuring the correctness of this multiset
	 * @return
	 */
	public Multiset<Seed> getSources() {
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
	public int compareTo(BootstrappedScore<LearnitPattern> o) {
		double val1 = this.getConfidence();
		double val2 = o.getConfidence();

		if (o instanceof PatternScore) {

			PatternScore pso = (PatternScore)o;

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
		return "PatternScore [precision=" + precision + ", recall=" + recall
				+ ", confidence=" + confidence + ", confidenceDenominator="
				+ confidenceDenominator + ", confidenceNumerator="
				+ confidenceNumerator + ", confidenceFromSimilarity="
				+ confidenceFromSimilarity + ", frequency=" + frequency
				+ ", knownFrequency=" + knownFrequency
				+ ", knownSeedFrequency=" + knownSeedFrequency
				+ ", frozenPatternInstanceCount=" + frozenPatternInstanceCount
				+ ", tp=" + tp + ", fp=" + fp + ", fn=" + fn + ", tn=" + tn
				+ ", toString()=" + super.toString() + "]";
	}



}
