package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.LabelPatternFilter;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternConstraintsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.util.GeneralUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SortExtractorsForAnnotation {
    public static List<LearnitPattern> SortingPattern(Set<LearnitPattern> patternSet,Mappings mappings){
        List<Pair<LearnitPattern,Integer>> countArr = new ArrayList<>();
        for(LearnitPattern learnitPattern:mappings.getAllPatterns().elementSet()){
            if(patternSet.contains(learnitPattern)){
                countArr.add(new Pair<>(learnitPattern,mappings.getInstancesForPattern(learnitPattern).size()));
            }
        }
        Collections.sort(countArr, new Comparator<Pair<LearnitPattern, Integer>>() {
            @Override
            public int compare(Pair<LearnitPattern, Integer> o1, Pair<LearnitPattern, Integer> o2) {
                return o1.getSecond() - o2.getSecond();
            }
        });
        return countArr.stream().filter(s->s.getSecond() > 0).map(s->s.getFirst()).collect(Collectors.toList());
    }
    public static List<Seed> SortingSeed(Set<Seed> seedSet,Mappings mappings){
        List<Pair<Seed,Integer>> countArr = new ArrayList<>();
        for(Seed seed:mappings.getAllSeeds().elementSet()){
            if(seedSet.contains(seed)){
                countArr.add(new Pair<>(seed,mappings.getInstancesForSeed(seed).size()));
            }
        }
        Collections.sort(countArr, new Comparator<Pair<Seed, Integer>>() {
            @Override
            public int compare(Pair<Seed, Integer> o1, Pair<Seed, Integer> o2) {
                return o1.getSecond() - o2.getSecond();
            }
        });
        return countArr.stream().filter(s->s.getSecond() > 0).map(s->s.getFirst()).collect(Collectors.toList());
    }
    public static void SerializerExtractor(TargetAndScoreTables targetAndScoreTables,final String outputExtractorPath,final String timeString) throws IOException{
        String strTargetPathDir = String.format("%s/%s/",outputExtractorPath, targetAndScoreTables.getTarget().getName());
        File dir = new File(strTargetPathDir);
        if(!dir.exists()) {
            dir.mkdir();
        }
        String strPathJson = strTargetPathDir + targetAndScoreTables.getTarget().getName() + "_" +
                timeString + ".json";
        System.out.println("\t serializing extractor for "+targetAndScoreTables+"...");
        targetAndScoreTables.serialize(new File(strPathJson));
        System.out.println("\t\t...done.");
    }
    public static void main(String args[]) throws IOException {
        String paramsFile = args[0];
        LearnItConfig.loadParams(new File(paramsFile));
        String mappingsPath = args[1];
        String outputExtractorPath = args[2];
        final Date date = new Date();
        final String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date)).replace("-", "");
        Mappings mappings = Mappings.deserialize(new File(mappingsPath), true);
        // Maybe filtering mappings here?
        List<MappingsFilter> filterQueueInObject = new ArrayList<>();
        filterQueueInObject.add(new FrequencyLimitFilter(1,99999999,1,99999999));
        filterQueueInObject.add(new LabelPatternFilter(mappings));
//        PatternConstraintsFilter patternConstraintsFilter = new PatternConstraintsFilter(new HashSet<>(),new HashSet<>(),true);
//        patternConstraintsFilter.set_prop_pattern_max_depth(4);
//        filterQueueInObject.add(patternConstraintsFilter);
        for(MappingsFilter mappingsFilter : filterQueueInObject){
            mappings = mappingsFilter.makeFiltered(mappings);
        }

        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
        for(Map.Entry<String, TargetAndScoreTables> extractorEntry : latestExtractors.entrySet()){
            String label = extractorEntry.getKey();
            TargetAndScoreTables currentScoreTable = extractorEntry.getValue();
            if(label.toLowerCase().compareTo("OTHER".toLowerCase()) == 0){
                SerializerExtractor(currentScoreTable,outputExtractorPath,timeString);
                continue;
            }
            Set<LearnitPattern> patternSet = new HashSet<>();
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : currentScoreTable.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    patternSet.add(pattern.getObject());
                }
            }
            List<LearnitPattern> patterns = SortingPattern(patternSet,mappings);
            TargetAndScoreTables newScoreTable = new TargetAndScoreTables(label);
            for(int i = 0;i < patterns.size();++i){
                LearnitPattern learnitPattern = patterns.get(i);
                newScoreTable.getPatternScores().addDefault(learnitPattern);
                PatternScore score = newScoreTable.getPatternScores().getScore(learnitPattern);
                score.setPrecision((double)i);
                score.setConfidence(1.0);
            }
            newScoreTable = currentScoreTable.copyWithPatternScoreTable(newScoreTable.getPatternScores());
            SerializerExtractor(newScoreTable,outputExtractorPath,timeString);
        }
    }
}
