package com.bbn.akbc.neolearnit.processing.patternpruning;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.PatternScoringInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.tables.SeedScoreTable;
import com.bbn.akbc.neolearnit.storage.EfficientMapDataStore;
import com.bbn.akbc.neolearnit.storage.EfficientMultisetDataStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PatternPruningInformation implements PatternScoringInformation {

  private final Map<LearnitPattern,PatternPartialInfo> partialInfos;

  @JsonProperty
  private EfficientMapDataStore<LearnitPattern,PatternPartialInfo> partialInfos() {
    return EfficientMapDataStore.fromMap(partialInfos);
  }

  @JsonCreator
  public PatternPruningInformation(@JsonProperty("partialInfos") EfficientMapDataStore<LearnitPattern,PatternPartialInfo> partialInfos) {
    this.partialInfos = partialInfos.makeMap();
  }

  public PatternPruningInformation() {
    this.partialInfos = new HashMap<LearnitPattern,PatternPartialInfo>();
  }

  @Override
  public PatternPartialInfo getPartialInfo(LearnitPattern pattern) {
    return partialInfos.containsKey(pattern) ? partialInfos.get(pattern) : new PatternPartialInfo();
  }

  @Override
  public boolean hasPartialInfo(LearnitPattern pattern) {
    return partialInfos.containsKey(pattern);
  }

  public void recordPartialInfo(LearnitPattern pattern, PatternPartialInfo info) {
    partialInfos.put(pattern, info);
  }

  public Set<LearnitPattern> getPatterns() {
    return partialInfos.keySet();
  }

  public Set<LearnitPattern> getPatternsToScore() {
    Set<LearnitPattern> toScore = new HashSet<LearnitPattern>();
//        System.out.println("Considering " + getPatterns().size() + " patterns.");
    for (LearnitPattern p : getPatterns()) {
      if (!(p instanceof ComboPattern) || ((ComboPattern)p).isUseful(this)) {
	toScore.add(p);
      }
    }
    return toScore;
  }

  @Override
  public int getCount(LearnitPattern obj) {
    return getPartialInfo(obj).totalInstanceCount;
  }

  public static class PatternPartialInfo implements Comparable<PatternPartialInfo> {
    @JsonProperty
    public int knownInstanceCount;
    @JsonProperty
    public int totalInstanceCount;
    @JsonProperty
    public int frozenPatternInstanceCount;  // so we can catch when a pattern is a direct subset of known patterns
    @JsonProperty
    public double confidenceNumerator;
    @JsonProperty
    public double confidenceDenominator;
    @JsonProperty
    public double confidenceFromSimilarity; // for tracking purposes
    public Multiset<Integer> sources;
    @JsonProperty
    public EfficientMultisetDataStore<Integer> sources() {
      return EfficientMultisetDataStore.fromMultiset(sources);
    }
    // let's try storing unique seed scores. it is much smaller than the seed string
    @JsonProperty
    public Set<String> seedSample; // for keeping track of whether a pattern meets a minimum threshold of unique seeds

    private static StopWords stopwords;

    @JsonCreator
    public PatternPartialInfo(
	@JsonProperty("knownInstanceCount") int knownInstanceCount,
	@JsonProperty("totalInstanceCount") int totalInstanceCount,
	@JsonProperty("frozenPatternInstanceCount") int frozenPatternInstanceCount,
	@JsonProperty("confidenceNumerator") double confidenceNumerator,
	@JsonProperty("confidenceDenominator") double confidenceDenominator,
	@JsonProperty("confidenceFromSimilarity") double confidenceFromSimilarity,
	@JsonProperty("sources") EfficientMultisetDataStore<Integer> sources,
	@JsonProperty("seedSample") Set<String> seedSample) {
      this.knownInstanceCount = knownInstanceCount;
      this.totalInstanceCount = totalInstanceCount;
      this.confidenceNumerator = confidenceNumerator;
      this.confidenceDenominator = confidenceDenominator;
      this.confidenceFromSimilarity = confidenceFromSimilarity;
      this.frozenPatternInstanceCount = frozenPatternInstanceCount;
      this.sources = sources.makeMultiset();
      this.seedSample = seedSample;
    }

    public PatternPartialInfo() {
      this.knownInstanceCount = 0;
      this.totalInstanceCount = 0;
      this.confidenceNumerator = 0;
      this.confidenceDenominator = 0;
      this.frozenPatternInstanceCount = 0;
      this.confidenceFromSimilarity = 0;
      this.sources = HashMultiset.create();
      this.seedSample = new HashSet<String>();

      if (stopwords == null) stopwords = StopWords.getFromParamsWithBackoff("overlap_seed_match_filter_word_list");
    }

    public static class DoubleAndBool {
      public double value;
      public boolean bool;
      public DoubleAndBool(double value, boolean bool) {
	this.value = value;
	this.bool = bool;
      }
    }

    /*
       * set to max(confidence(seed))
       */
    public static DoubleAndBool instanceScore(InstanceIdentifier id, Mappings mappings, TargetAndScoreTables data) {
      double maxScore = 0;
      boolean fromSim = false;
      for (Seed s : mappings.getSeedsForInstance(id)) {
	double score = SeedSimilarity.getUnknownSeedScore(s.withProperText(data.getTarget()))*LearnItConfig.getDouble("pattern_pruning_similarity_penalty");
	boolean thisFromSim = SeedSimilarity.seedHasScore(s.withProperText(data.getTarget()));
	double conf = -1.0;
	if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
	  for (Seed groupSeed : SeedGroups.getGroupOrSingleton(s.getReducedForm(stopwords), s)) {
	    if (data.getSeedScores().hasScore(groupSeed)) {
	      double gConf = data.getSeedScores().getScore(groupSeed).getConfidence();
	      if (gConf > conf) conf = gConf;
	    }
	  }
	} else if (data.getSeedScores().hasScore(s)) {
	  conf = data.getSeedScores().getScore(s).getConfidence();
	}

	if (conf > score) {
	  score = conf;
	  thisFromSim = false;
	}

	if (score >= maxScore) {
	  maxScore = score;
	  fromSim = thisFromSim;
	}
      }
      return new DoubleAndBool(maxScore,fromSim);
    }

    protected static double round(double input) {
      final int DIGITS = 6;
      final int FACTOR = (int) Math.pow(10,DIGITS);
      return (double)Math.round(input*FACTOR)/FACTOR;
    }

    public static PatternPartialInfo calculateInfo(LearnitPattern pattern, Mappings mappings, TargetAndScoreTables data) {
      PatternPartialInfo result = new PatternPartialInfo();
      Multiset<Seed> capturedSeeds = mappings.getSeedsForPattern(pattern);
      boolean useSeedGroups = LearnItConfig.optionalParamTrue("use_seed_groups");
      for (Seed s : capturedSeeds.elementSet()) {
	//if it's known
	boolean knownFrozen = false;
	if (useSeedGroups) {
	  for (Seed groupSeed : SeedGroups.getGroupOrSingleton(s.getReducedForm(stopwords), s)) {
	    if (data.getSeedScores().isKnownFrozen(groupSeed)) {
	      knownFrozen = true;
	      break;
	    }
	  }
	} else
	  knownFrozen = data.getSeedScores().isKnownFrozen(s);

	if (knownFrozen) {
	  //result.knownSeeds.add(s.toString());
	  result.knownInstanceCount += capturedSeeds.count(s);
	  if (result.seedSample.size() < LearnItConfig.getInt("min_unique_seeds")) {

	    if (useSeedGroups) {
	      Collection<Seed> group = SeedGroups.getGroupOrSingleton(s.getReducedForm(stopwords), s);
	      int minInd = Integer.MAX_VALUE;
	      for (Seed groupSeed : group) {
		if (data.getSeedScores().getItemIndex(groupSeed) < minInd)
		  minInd = data.getSeedScores().getItemIndex(groupSeed);
	      }
	      result.sources.add(minInd);
	    } else
	      result.sources.add(data.getSeedScores().getItemIndex(s)); // only care to record the known sources (for tracking + size reasons)
	  }
	}

	//result.totalSeeds.add(s.toString());
	result.totalInstanceCount += capturedSeeds.count(s);
	if (result.seedSample.size() < LearnItConfig.getInt("min_unique_seeds")) {
	  if (useSeedGroups)
	    result.seedSample.add(s.getReducedForm(stopwords));
	  else
	    result.seedSample.add(s.toIDString());
	}
      }

      for (InstanceIdentifier id : mappings.getInstancesForPattern(pattern)) {

	DoubleAndBool maxScore = instanceScore(id,mappings,data);

	for (LearnitPattern p : mappings.getPatternsForInstance(id)) {
	  if (data.getPatternScores().isKnownFrozen(p)) {
	    result.frozenPatternInstanceCount += 1;
	    break;
	  }
	}

	if (maxScore.bool || maxScore.value > 0) {

	  if (maxScore.bool)
	    result.confidenceFromSimilarity += id.getConfidence()*maxScore.value;

	  result.confidenceNumerator += id.getConfidence()*maxScore.value;
	  result.confidenceDenominator += id.getConfidence();
	}
      }

            /*if (result.confidenceNumerator > 0) {
                System.out.println(result.confidenceNumerator + " / " + result.confidenceDenominator);
                System.out.println(result.confidenceFromSimilarity);
            }*/

      result.confidenceFromSimilarity = round(result.confidenceFromSimilarity);
      result.confidenceNumerator = round(result.confidenceNumerator);
      result.confidenceDenominator = round(result.confidenceDenominator);

            /*if (result.confidenceNumerator > 0) {
                System.out.println(result.confidenceNumerator + " / " + result.confidenceDenominator);
                System.out.println(result.confidenceFromSimilarity);
                System.out.println();
            }*/


      return result;
    }

    public double seedScoreSum(SeedScoreTable seedScores) {
      double sum = 0.0;
      for (Integer sind : sources.elementSet()) {
	sum += seedScores.getScore(seedScores.getItemFromIndex(sind)).getScore();
      }
      return sum;
    }

    public void mergeIn(PatternPartialInfo other) {
      this.knownInstanceCount += other.knownInstanceCount;
      this.totalInstanceCount += other.totalInstanceCount;
      this.confidenceNumerator += other.confidenceNumerator;
      this.confidenceDenominator += other.confidenceDenominator;
      this.confidenceFromSimilarity += other.confidenceFromSimilarity;
      this.frozenPatternInstanceCount += other.frozenPatternInstanceCount;
      this.sources.addAll(other.sources);
      for (String seedString : other.seedSample) {
	if (this.seedSample.size() < LearnItConfig.getInt("min_unique_seeds")) {
	  this.seedSample.add(seedString);
	} else {
	  break;
	}
      }
    }

    public double confidenceEstimate() {
      return round(this.confidenceNumerator/this.confidenceDenominator);
    }

    @Override
    public int compareTo(PatternPartialInfo o) {
      if (o.seedSample.size() != this.seedSample.size()) {
	return o.seedSample.size() - this.seedSample.size();
      }

      double c1 = this.confidenceEstimate();
      double c2 = o.confidenceEstimate();
      if (c1 == c2) {
	return o.totalInstanceCount - this.totalInstanceCount;
      } else {
	return -Double.compare(c1, c2);
      }
    }

  }

}
