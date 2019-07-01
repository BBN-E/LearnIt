package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.filters.InstanceIdentifierFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.ObservationSimilarity;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;



public class EnlargeExtractorsBySimilarity {
//    final Mappings mappings;
//    final Map<String, TargetAndScoreTables> extractors;
//    final ObservationSimilarityModule observationSimilarityModule;
//    EnlargeExtractorsBySimilarity(Mappings mappings,Map<String, TargetAndScoreTables> extractors,String dirSuffixForSimilarityMatrices) throws IOException{
//        this.mappings = mappings;
//        this.extractors = extractors;
//        this.observationSimilarityModule = ObservationSimilarityModule.create(mappings, dirSuffixForSimilarityMatrices);
//    }
    private static LearnitPattern findPattern(Mappings info, String pattern) {
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }
        return null;
    }


    public static void main(String args[]) throws IOException{
        // rm -rf /home/hqiu/Public/learnit_extractors && ./neolearnit/target/appassembler/bin/EnlargeExtractorsBySimilarity ~/ld100/learnit_working/params/learnit/runs/causeex-m11-v1.params /nfs/mercury-04/u41/learnit/CauseEx-M11-V1/source_mappings/all_event_event_pairs-1/freq_1_1/mappings.master.sjson /home/hqiu/massive/tmp/CauseExRegTest/1533926981/CauseEx/lib/learnit_patterns /home/hqiu/Public/learnit_extractors all_event_event_pairs-1
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsPath = args[1];
        String extractorsInputDirectory = args[2];
        String extractorsOutputDirectory = args[3];
        String dirSuffixForSimilarityMatrices = args[4];
//        String patternSimilarityThreshold = args[5];
        String patternSimilarityThreshold = "0";
//        String patternSimilarityCutOff = args[6];
        String patternSimilarityCutOff = "30";
//        int seedInstanceCountCutOff = Integer.parseInt(arg[7]); //20
        int seedInstanceCountCutOff = 20;
        Mappings mappings = Mappings.deserialize(new File(mappingsPath), true);
        mappings = new InstanceIdentifierFilter().makeFiltered(mappings);
//        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadExtractors(extractorsInputDirectory);
        ObservationSimilarityModule observationSimilarityModule =  ObservationSimilarityModule.create(mappings, dirSuffixForSimilarityMatrices);
        Target target = TargetFactory.makeBinaryEventEventTarget();
//        Majority code borrowed from com.bbn.akbc.neolearnit.server.handlers.addPattern and com.bbn.akbc.neolearnit.server.handlers.getSimilarPatterns
        Date date = new Date();
        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)).replace("-", "");
        for(Map.Entry<String, TargetAndScoreTables> extractorEntry : latestExtractors.entrySet()){
            String label = extractorEntry.getKey();
            if(label.toLowerCase().compareTo("OTHER".toLowerCase()) == 0)continue;
            TargetAndScoreTables currentScoreTable = extractorEntry.getValue();
            TargetAndScoreTables newScoreTable = currentScoreTable;
            Set<PatternID> patternsToFilter = new HashSet<>();
            List<Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();
            Set<InstanceIdentifier> existingInstanceIdentifier = new HashSet<>();
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : newScoreTable.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood() && mappings.getInstancesForPattern(pattern.getObject()).size() > 0) {
                        Optional<? extends ObservationSimilarity> patternSimilarity =
                                observationSimilarityModule.getPatternSimilarity(PatternID.from(pattern.getObject()));
                        patternSimilarityRows.add(patternSimilarity);
                        patternsToFilter.add(PatternID.from(pattern.getObject()));
                        for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(pattern.getObject())){
                            existingInstanceIdentifier.add(instanceIdentifier);
                        }
                }
            }
            patternsToFilter.addAll(newScoreTable.getPatternScores().getFrozen().stream().map(
                    (LearnitPattern p)->PatternID.from(p)).collect(Collectors.toSet()));
            List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarPatterns =
                    ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                            Double.parseDouble(patternSimilarityThreshold),
                            patternSimilarityCutOff.isEmpty()?Optional.absent():Optional.of(Integer.parseInt(patternSimilarityCutOff)),
                            Optional.of(patternsToFilter));
            for(com.bbn.akbc.utility.Pair<LearnItObservation,Double>learnItObservationDoublePair : similarPatterns){
//                LearnitPattern learnitPattern = (LearnitPattern) learnItObservationDoublePair.key;
                String patternStr = learnItObservationDoublePair.key.toIDString();
                LearnitPattern learnitPattern = findPattern(mappings,patternStr);
                if (learnitPattern == null) {
                    System.out.println("Cannot find pattern for string: "+patternStr);
                    continue;
//                    mappings = mappings.getAllPatternUpdatedMappings(newScoreTable);
//                    learnitPattern = findPattern(mappings,patternStr);
                }
                newScoreTable.getPatternScores().addDefault(learnitPattern);
                PatternScore score = newScoreTable.getPatternScores().getScore(learnitPattern);
                score.setPrecision(0.95);
                score.setConfidence(1.0);
//                score.freezeScore(score.getIteration());
                System.out.println("Under relationType "+label+", we're adding "+ learnitPattern.toIDString());
                int capturedByExistingPatterns = 0;
                int newCapturedInstances = 0;
                for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForPattern(learnitPattern)){
                    if(existingInstanceIdentifier.contains(instanceIdentifier)){
                        capturedByExistingPatterns++;
                    }
                    else{
                        if (newCapturedInstances < 3){
                            String orininalHtml = instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                            orininalHtml = orininalHtml.replace("<br/>","");
                            orininalHtml = orininalHtml.replace("<br />","");
                            orininalHtml = orininalHtml.replace("</span>","]");
                            orininalHtml = orininalHtml.replace("<span class=\"slot0\">","[");
                            orininalHtml = orininalHtml.replace("<span class=\"slot1\">","[");
                            System.out.println("\t" + orininalHtml);
                        }
                        newCapturedInstances++;
                        existingInstanceIdentifier.add(instanceIdentifier);
                    }
                }
                System.out.println("If we find instance using this pattern, we're expecting to get: "+ newCapturedInstances + " new instances, beyond that, "+ capturedByExistingPatterns + " has been captured by existing pattern");
                score.setConfidenceNumerator(score.getConfidenceDenominator()*score.getConfidence());
                score.setKnownFrequency((int)Math.round(score.getPrecision()*score.getFrequency()));
//
                score.calculateTPFNStats(newScoreTable.getGoodSeedPrior(), newScoreTable.getTotalInstanceDenominator());
                if (mappings.getKnownInstanceCount(newScoreTable) == 0) {
                    newScoreTable.setGoodSeedPrior(score.getConfidenceNumerator()*10,newScoreTable.getTotalInstanceDenominator());
                }
            }
            // 1. find seeds in mappings that are 1) matched by at least one pattern, 2) have a minimum frequency of FREQ_THEREHOLD
            // 2. add seeds into the extractor

            for(Seed seed : mappings.getAllSeeds().elementSet()){
                Set<InstanceIdentifier> instanceIdentifiers = new HashSet<>(mappings.getInstancesForSeed(seed));
                if(instanceIdentifiers.size() < seedInstanceCountCutOff)continue;
                Set<LearnitPattern> potentialPatterns =  new HashSet<>(mappings.getPatternsForInstances(instanceIdentifiers));
                // Case one
//                if(potentialPatterns.size() < 1)continue;
                // Case two
                LearnitPattern matchedFrozenPattern = null;
                for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : newScoreTable.getPatternScores().getObjectsWithScores()) {
                    if (pattern.getScore().isFrozen() && pattern.getScore().isGood() && mappings.getInstancesForPattern(pattern.getObject()).size() > 0) {
                        if(potentialPatterns.contains(pattern.getObject())){
                            matchedFrozenPattern = pattern.getObject();
                            break;
                        }
                    }
                }
                if(matchedFrozenPattern == null) continue;

                // Below code comes from com.bbn.akbc.neolearnit.server.handlers.freezeSeed
                if (newScoreTable.getSeedScores().isKnownFrozen(seed)) continue;
                if (!newScoreTable.getSeedScores().hasScore(seed)) {
                    newScoreTable.getSeedScores().addDefault(seed);
                }
                SeedScore sscore = newScoreTable.getSeedScores().getScore(seed);
                sscore.setScore(1000000.0); // TODO: fixme
                sscore.setConfidence(1.0);
                sscore.freezeScore(newScoreTable.getIteration());
                System.out.println("Under relationType "+label+", we're adding "+ seed.toIDString()+" because it match pattern: "+matchedFrozenPattern.toIDString());
                int capturedByExistingPatterns = 0;
                int newCapturedInstances = 0;
                for(InstanceIdentifier instanceIdentifier: mappings.getInstancesForSeed(seed)){
                    if(existingInstanceIdentifier.contains(instanceIdentifier)){
                        capturedByExistingPatterns++;
                    }
                    else{
                        if (newCapturedInstances < 3){
                            String orininalHtml = instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                            orininalHtml = orininalHtml.replace("<br/>","");
                            orininalHtml = orininalHtml.replace("<br />","");
                            orininalHtml = orininalHtml.replace("</span>","]");
                            orininalHtml = orininalHtml.replace("<span class=\"slot0\">","[");
                            orininalHtml = orininalHtml.replace("<span class=\"slot1\">","[");
                            System.out.println("\t" + orininalHtml);
                        }
                        newCapturedInstances++;
                        existingInstanceIdentifier.add(instanceIdentifier);
                    }
                }
                System.out.println("If we find instance using this seed, we're expecting to get: "+ newCapturedInstances + " new instances, beyond that, "+ capturedByExistingPatterns + " has been captured by existing pattern");
            }
            String strTargetPathDir = String.format("%s/",extractorsOutputDirectory);
            File dir = new File(strTargetPathDir);
            if(!dir.exists()) {
                dir.mkdir();
            }
            String strPathJson = strTargetPathDir + newScoreTable.getTarget().getName() + "_" +
                    timeString + ".json";
            System.out.println("\t serializing extractor for "+newScoreTable+"...");
            newScoreTable.serialize(new File(strPathJson));
            System.out.println("\t\t...done.");
        }
    }
}
