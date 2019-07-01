package com.bbn.akbc.neolearnit.scoring.proposers;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternScoreProposer implements Proposer<LearnitPattern> {

    private final TargetAndScoreTables data;
    private final double confidenceThreshold;

    public PatternScoreProposer(TargetAndScoreTables data) {

        this.data = data;
        this.confidenceThreshold = LearnItConfig.getDouble("pattern_proposal_confidence_threshold");
    }

    /*
     * propose patterns up to {amount} by
     * - first ranked by confidence
     * - then precision
     * - then recall
     */
    @Override
    public Iterable<LearnitPattern> propose(Iterable<LearnitPattern> potentials, int amount) {

        List<ObjectWithScore<LearnitPattern,PatternScore>> toPropose = data.getPatternScores().getNonFrozenObjectsWithScores();

        //just pick the highest confidence scores
        // - first ranked by confidence
        // - then precision
        // - then recall
        Collections.sort(toPropose);

        // freeze top numToFreeze seeds;
        Set<LearnitPattern> result = new HashSet<LearnitPattern>();
        int numProposed = 0;
        int numComplexProposed = 0; //For certain relations
        Glogger.logger().debug("Proposing patterns ...");
        for (ObjectWithScore<LearnitPattern, PatternScore> scoredContext : toPropose) {
            if (scoredContext.getScore().getConfidence() >= confidenceThreshold &&
                (LearnItConfig.optionalParamTrue("score_redundant_patterns") ||
                     scoredContext.getScore().getFrozenPatternInstanceCount() < scoredContext.getScore().getFrequency()))
            {
                if (data.getTarget().useSimpleProps()) {
                    if (scoredContext.getObject() instanceof PropPattern && ((PropPattern) scoredContext.getObject()).depth() > 1) {
                        if (numComplexProposed > .2*amount) { // Bonan: another parameter: .2
                            Glogger.logger().debug(
                                "REJECTED (COMPLEX): " + scoredContext.getObject().toIDString()
                                    + " - " + scoredContext.getScore().getConfidence());
                            continue;
                        }
                        numComplexProposed++;
                    }
                }
                Glogger.logger().debug(
                    scoredContext.getObject().toIDString() + " - " + scoredContext
                        .getScore().getConfidence());
                result.add(scoredContext.getObject());
                if (scoredContext.getScore().getFrozenPatternInstanceCount() < scoredContext.getScore().getFrequency())
                    numProposed++; //Only increase count if this pattern isn't redundant

                if (numProposed >= amount) break;
            } else if (scoredContext.getScore().getConfidence() > 0) {
                Glogger.logger().debug(
                    "REJECTED: " + scoredContext.getObject().toIDString() + " - "
                        + scoredContext.getScore().getConfidence());
            }
        }
        Glogger.logger().debug("Proposed " + numProposed + " new contexts.");

        //remove contexts that are not frozen and not proposed
        for (ObjectWithScore<LearnitPattern, PatternScore> scoredContext : toPropose) {
            if (!result.contains(scoredContext.getObject())) {
                data.getPatternScores().removeItem(scoredContext.getObject());
            }
        }

        if (data.getTarget().doLexicalExpansion()) {
            Set<LearnitPattern> expandedCandidates = new HashSet<LearnitPattern>();
            for (LearnitPattern frozen : data.getPatternScores().getFrozen()) {
                //get lexically expanded versions of patterns that were frozen last iteration
                if (data.getPatternScores().getScore(frozen).getFrozenIteration() == data.getIteration() - 1) {
                    for (LearnitPattern p : frozen.getLexicallyExpandedVersions()) {
                        if (!data.getPatternScores().hasScore(p))
                            data.getPatternScores().getScoreOrDefault(p);
                        if (!data.getPatternScores().getScore(p).isFrozen()) {
                            result.add(p);
                            expandedCandidates.add(p);
                        }
                    }
                }
            }
            for (LearnitPattern p : expandedCandidates)
                Glogger.logger().debug("Proposing lexical expansions: " + p.toIDString());
        }

        return result;
    }

}
