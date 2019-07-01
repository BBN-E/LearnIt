package com.bbn.akbc.neolearnit.processing.patternpruning;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternMatchFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruningInformation.PatternPartialInfo;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scorers.PatternScorer;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.selectors.PatternSelector;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PatternPruner extends AbstractStage<PatternPruningInformation> {

  protected int numberToAccept;
  private static final double LEXICAL_EXPANSION_PENALTY = .8;

  public PatternPruner(TargetAndScoreTables data) {
    super(data);
    this.numberToAccept = LearnItConfig.getInt("max_num_patterns_to_freeze");
  }

  @Override
  public Mappings applyGeneralMappingsFilter(Mappings mappings) {
    return mappings;
  }

  @Override
  public Mappings applyStageMappingsFilter(Mappings mappings) {
    return new PatternMatchFilter(data.getPatternScores().keySet()).makeFiltered(mappings);
  }

  /*
   * apply restrictions (each is a combo_pattern(original_pattern, restriction)
   */
  @Override
  public PatternPruningInformation processFilteredMappings(Mappings mappings) {
//		try {
//			SeedSimilarity.load(data);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

    Mappings mappingsToUse;
    if (LearnItConfig.optionalParamTrue("restrict_patterns_at_pruning")) {
      Glogger.logger().debug("Restricting patterns...");

      Collection<ComboPattern> restrictionPatterns = mappings.getRestrictedPatternVariants(data);

      if (restrictionPatterns.isEmpty()) {
	mappingsToUse = mappings;
      } else {
	Glogger.logger().debug("Applying " + restrictionPatterns.size() + " restrictions...");
	mappingsToUse = mappings.getUpdatedMappingsWithComboPatterns(restrictionPatterns);
      }
    } else {
      mappingsToUse = mappings;
    }

    // we do this a little later so we can take advantage of head text during restrictions
    mappingsToUse = new NormalizeSeedsFilter(data.getTarget()).makeFiltered(mappingsToUse);

    Glogger.logger().debug("Calculating info for "+mappingsToUse.getAllPatterns().elementSet().size()+" patterns");

    boolean stopWordRelation = data.getTarget().allowStopwordPatterns();
    PatternPruningInformation result = new PatternPruningInformation();
    for (LearnitPattern p : mappingsToUse.getAllPatterns().elementSet()) {
      if (p.isCompletePattern() && !data.getPatternScores().isKnownFrozen(p) && (stopWordRelation || !p.getLexicalItemsWithContent().isEmpty())) {
	result.recordPartialInfo(p, PatternPartialInfo.calculateInfo(p,mappingsToUse,data));
      }
      else
	Glogger.logger().debug("not include: " + p.toIDString());
    }

    Glogger.logger().debug("Got " + result.getPatterns().size() + " patterns.");
    return result;
  }

  @Override
  public PatternPruningInformation reduceInformation(Collection<PatternPruningInformation> inputs) {

    PatternPruningInformation result = new PatternPruningInformation();
    for (PatternPruningInformation input : inputs) {
//            System.out.println("Reducing from " + input.getPatterns().size() + " patterns");
      for (LearnitPattern pattern : input.getPatterns()) {
	if (result.hasPartialInfo(pattern))
	  result.getPartialInfo(pattern).mergeIn(input.getPartialInfo(pattern));
	else
	  result.recordPartialInfo(pattern, input.getPartialInfo(pattern));
      }
    }
//        System.out.println("Reduced to " + result.getPatterns().size() + " patterns");
    return result;
  }

  public void score(PatternPruningInformation input) {
    PatternScorer scorer = new PatternScorer(data, input);
    scorer.score(input.getPatternsToScore(), data.getPatternScores());
  }

  public Set<LearnitPattern> select(int amount) {
    PatternSelector selector = new PatternSelector(data,amount);
    return selector.freezeScores(data.getPatternScores());
  }

  private void acceptFromLexicalExpansion() {
    Map<LearnitPattern,LearnitPattern> allExpansionPatterns= new HashMap<LearnitPattern,LearnitPattern>();
    for (LearnitPattern frozen : data.getPatternScores().getFrozen()) {
      if (data.getPatternScores().getScore(frozen).getFrozenIteration() == data.getIteration()-1) {
	for (LearnitPattern expanded : frozen.getLexicallyExpandedVersions()) {
	  if (!allExpansionPatterns.containsKey(expanded) || //Only keeping the most confident generating pattern
	      data.getPatternScores().getScore(frozen).getConfidence() >
		  data.getPatternScores().getScore(allExpansionPatterns.get(expanded)).getConfidence())
	  {
	    allExpansionPatterns.put(expanded, frozen);
	  }
	}
      }
    }

    Set<LearnitPattern> nonfrozen = ImmutableSet.copyOf(data.getPatternScores().getNonFrozen());
    for (LearnitPattern scored : nonfrozen) {
      if (allExpansionPatterns.containsKey(scored)) {
	PatternScore expandedScore = data.getPatternScores().getScore(scored);
	PatternScore sourceScore = data.getPatternScores().getScore(allExpansionPatterns.get(scored));
	expandedScore.setConfidence(Math.max(expandedScore.getConfidence(), sourceScore.getConfidence() * LEXICAL_EXPANSION_PENALTY));
	expandedScore.setPrecision(Math.max(expandedScore.getPrecision(), sourceScore.getPrecision() * LEXICAL_EXPANSION_PENALTY));
	expandedScore.freezeScore(data.getIteration());
	Glogger.logger().debug("Accepting lexical expansion pattern " + scored.toIDString());
      }
    }
  }

  @Override
  public void runStage(PatternPruningInformation input) {
    score(input);
    data.getPatternScores().removeProposed();

    if (data.getTarget().doLexicalExpansion()) {
      acceptFromLexicalExpansion();
    }

    select(numberToAccept);

//		System.out.println(data.getPatternScores().toFrozenString());

  }

  @Override
  public Class<PatternPruningInformation> getInfoClass() {
    return PatternPruningInformation.class;
  }

}
