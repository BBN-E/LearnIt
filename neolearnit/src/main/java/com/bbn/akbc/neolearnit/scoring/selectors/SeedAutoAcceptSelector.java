package com.bbn.akbc.neolearnit.scoring.selectors;

import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.SeedScoreTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeedAutoAcceptSelector implements Selector<Seed,SeedScore,SeedScoreTable> {
    final int numToFreeze;
    public SeedAutoAcceptSelector(int numToFreeze){
        this.numToFreeze = numToFreeze;
    }
    @Override
    public Set<Seed> freezeScores(SeedScoreTable scores) {
        List<AbstractScoreTable.ObjectWithScore<Seed, SeedScore>> tofreeze = scores.getNonFrozenObjectsWithScores();
        Collections.sort(tofreeze);
        Set<Seed> changedSeed = new HashSet<>();
        for(int i = 0;i < Math.min(tofreeze.size(),this.numToFreeze);++i){
            AbstractScoreTable.ObjectWithScore<Seed, SeedScore> scoredSeed = tofreeze.get(i);
            SeedScore score = scoredSeed.getScore();
            if(!score.isGood())break;
            score.freezeScore(score.getIteration());
            changedSeed.add(scoredSeed.getObject());
        }
        return changedSeed;
    }
}
