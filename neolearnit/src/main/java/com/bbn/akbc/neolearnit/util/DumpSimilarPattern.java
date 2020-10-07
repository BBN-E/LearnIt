package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.observations.similarity.ObservationSimilarity;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.utility.Pair;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.PropPattern;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.*;

public class DumpSimilarPattern {

    public static void main(String[] args) throws Exception{
        String paramsPath = args[0];
        LearnItConfig.loadParams(new File(paramsPath));
        String mappingsPath = args[1];
        String dirSuffixForSimilarityMatrices = args[2];
        Mappings autopopulatedMappings = Mappings.deserialize(new File(mappingsPath),true);
        ObservationSimilarityModule observationSimilarityModule = ObservationSimilarityModule.create(autopopulatedMappings, dirSuffixForSimilarityMatrices);
        Map<String,SerifPattern> idStringToPattern = new HashMap<>();
        for(LearnitPattern learnitPattern: autopopulatedMappings.getAllPatterns().elementSet()){
            if(learnitPattern instanceof SerifPattern){
                SerifPattern serifPattern = (SerifPattern)learnitPattern;
                idStringToPattern.put(serifPattern.toIDString(),serifPattern);
            }
        }
        for(LearnitPattern learnitPattern:autopopulatedMappings.getAllPatterns().elementSet()){
            if(learnitPattern instanceof SerifPattern){
                SerifPattern srcSerifPattern = (SerifPattern) learnitPattern;
                List<Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();
                Optional<? extends ObservationSimilarity> learnitPatternSimilarityOptional = observationSimilarityModule.getPatternSimilarity(PatternID.from(learnitPattern));
                patternSimilarityRows.add(learnitPatternSimilarityOptional);
                List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> similarLearnitPatterns =
                        ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                                0.0,
                                Optional.of(100),
                                Optional.absent());
                for(Pair<LearnItObservation, Double> similarityPatternScorePair: similarLearnitPatterns){
                    if(idStringToPattern.containsKey(similarityPatternScorePair.key.toIDString())){
                        SerifPattern dstPattern = idStringToPattern.get(similarityPatternScorePair.key.toIDString());
                        PropPattern srcPropPattern = (PropPattern)srcSerifPattern.getPattern();
                        PropPattern dstPropPattern = (PropPattern)dstPattern.getPattern();
                        Set<Symbol> srcPred = srcPropPattern.getPredicates();
                        Set<Symbol> dstPred = dstPropPattern.getPredicates();
                        if(Sets.intersection(srcPred,dstPred).size()< Math.min(srcPred.size(),dstPred.size())){
                            System.out.println(similarityPatternScorePair.value+"\t"+learnitPattern.toIDString()+"\t"+similarityPatternScorePair.key.toIDString());
                        }
                    }
                }
            }
        }
    }
}
