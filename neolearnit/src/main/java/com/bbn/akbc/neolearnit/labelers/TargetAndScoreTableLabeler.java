package com.bbn.akbc.neolearnit.labelers;

import com.bbn.akbc.neolearnit.common.*;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilterWithCache;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TargetAndScoreTableLabeler implements MappingsLabeler {
    final List<TargetAndScoreTables> extractors;
    final boolean useGoodSeeds;
    final boolean useLemmatizedPatternAsFallback;
    final public Set<LearnitPattern> goodPatterns = new HashSet<>();
    final List<LabelTrackingObserver> labelTrackingObserverList;
    public TargetAndScoreTableLabeler(Collection<TargetAndScoreTables> extractors, boolean useGoodSeeds, boolean useLemmaizedPatternAsFallback) {
        this.extractors = new ArrayList<>(extractors);
        this.useGoodSeeds = useGoodSeeds;
        this.useLemmatizedPatternAsFallback = useLemmaizedPatternAsFallback;
        this.labelTrackingObserverList = new ArrayList<>();
    }

    public void addLabelTrackingObserver(LabelTrackingObserver labelTrackingObserver){
        this.labelTrackingObserverList.add(labelTrackingObserver);
    }

    public static Symbol processLexicalItems(Symbol originalToken){
        String originalTokenStr = originalToken.asString();
        if(originalTokenStr.equals("[0]") || originalTokenStr.equals("[1]"))return Symbol.from("");
        if(originalTokenStr.contains("<") || originalTokenStr.contains(">"))return Symbol.from("");
        originalTokenStr = originalTokenStr.replace("[0]","").replace("[1]","");
        if(originalTokenStr.startsWith("[") || originalTokenStr.length() > 3){
            originalTokenStr = originalTokenStr.substring(3);
        }
        return Symbol.from(originalTokenStr);
    }

    public static TargetAndScoreTableLabeler fromOntologyYamlAndLatestExtractors(String yamlPath, boolean useGoodSeeds, boolean useLemmaizedPattern) throws Exception {
        List<TargetAndScoreTables> relevantExtractors = new ArrayList<>();
        BBNInternalOntology.BBNInternalOntologyNode root = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(yamlPath));
        Multimap<String, BBNInternalOntology.BBNInternalOntologyNode> childrenNodeMap = root.getPropToNodeMultiMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        String targetPathDir = Domain.getExtractorsPath();
        for (String targetName : childrenNodeMap.keySet()) {
            String fileName = new File(targetPathDir+File.separator+targetName+".json").getAbsolutePath();
            if (new File(fileName).exists()) {
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(new File(fileName));
                relevantExtractors.add(targetAndScoreTables);
            }
        }
        return new TargetAndScoreTableLabeler(relevantExtractors, useGoodSeeds, useLemmaizedPattern);
    }

    public static TargetAndScoreTableLabeler fromSingleTargetAndScoreTable(String strExtractor, boolean useGoodSeeds, boolean useLemmaizedPattern) throws IOException {
        TargetAndScoreTables ex = TargetAndScoreTables.deserialize(new File(strExtractor));
        return new TargetAndScoreTableLabeler(Arrays.asList(ex), useGoodSeeds, useLemmaizedPattern);
    }

    public static TargetAndScoreTableLabeler fromExtractorList(List<String> strExtractorList, boolean useGoodSeeds, boolean useLemmaizedPattern) throws IOException {
        Map<String, TargetAndScoreTables> extractorsMap = new HashMap<>();
        for(String strExtractor : strExtractorList){
            TargetAndScoreTables ex = TargetAndScoreTables.deserialize(new File(strExtractor));
            extractorsMap.put(ex.getTarget().getName(),ex);
        }
        return new TargetAndScoreTableLabeler(extractorsMap.values(), useGoodSeeds, useLemmaizedPattern);
    }

    public static TargetAndScoreTableLabeler fromExtractorDirectoryList(List<String> extractorDirectoryList, boolean useGoodSeeds, boolean useLemmaizedPattern) throws IOException {
        Map<String,TargetAndScoreTables> extractors = new HashMap<>();
        for(String strExtractorDirectory:extractorDirectoryList){
            extractors.putAll(GeneralUtils.loadExtractors(strExtractorDirectory));
        }
        return new TargetAndScoreTableLabeler(extractors.values(), useGoodSeeds, useLemmaizedPattern);
    }
    public static TargetAndScoreTableLabeler createLabelerUsingLearnitInternalExtractors(boolean useGoodSeeds) throws Exception {
        List<TargetAndScoreTables> extractors = new ArrayList<>();
        String targetPathDir = Domain.getExtractorsPath();
        File dir = new File(targetPathDir);
        if (dir.exists()) {
            for (File subDir : dir.listFiles()) {
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(subDir);
                extractors.add(targetAndScoreTables);
            }
        }
        return new TargetAndScoreTableLabeler(extractors, useGoodSeeds, false);
    }

    public static void main(String[] args) throws Exception {
        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String targetAndScoreTableFolder = args[1];
        String autogeneratedMappingsPath = args[2];
        String outputLabeledMappingsPath = args[3];
        TargetAndScoreTableLabeler targetAndScoreTableLabeler = TargetAndScoreTableLabeler.fromExtractorDirectoryList(Lists.newArrayList(targetAndScoreTableFolder), false, false);

        Mappings original = Mappings.deserialize(new File(autogeneratedMappingsPath), true);
        Mappings labeled = targetAndScoreTableLabeler.LabelMappings(original, new Annotation.InMemoryAnnotationStorage()).convertToMappings();
        labeled.serialize(new File(outputLabeledMappingsPath), true);
    }

    @Override
    public Annotation.InMemoryAnnotationStorage LabelMappings(Mappings original, Annotation.InMemoryAnnotationStorage labeledMappings) throws Exception {
        TargetFilterWithCache targetFilterWithCache = new TargetFilterWithCache(true);
        Map<String, Set<LearnitPattern>> lemmatizedStrToLearnitPattern = null;
        if (useLemmatizedPatternAsFallback) {
            GeneralUtils.loadWordLemmaMap();
            lemmatizedStrToLearnitPattern = new HashMap<>();
            for (LearnitPattern learnitPattern : original.getAllPatterns().elementSet()) {
                String patternLemma = GeneralUtils.getLemmatizedPattern(learnitPattern);
                Set<LearnitPattern> patternSetBuf = lemmatizedStrToLearnitPattern.getOrDefault(patternLemma, new HashSet<>());
                patternSetBuf.add(learnitPattern);
                lemmatizedStrToLearnitPattern.put(patternLemma, patternSetBuf);
            }
        }

        for (TargetAndScoreTables targetAndScoreTables : this.extractors) {
            String targetName = targetAndScoreTables.getTarget().getName();
            Target extractorTarget = targetAndScoreTables.getTarget();
            targetFilterWithCache.setFocusTarget(extractorTarget);
            Mappings filtered = targetFilterWithCache.makeFiltered(original);
            Set<LearnitPattern> goodOriginalPatternSet = new HashSet<>();
            Set<LearnitPattern> goodLemmatizedPatternSet = null;
            if (useLemmatizedPatternAsFallback) {
                goodLemmatizedPatternSet = new HashSet<>();
            }
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : targetAndScoreTables.getPatternScores().getObjectsWithScores()) {
                if (pattern.getScore().isFrozen() && pattern.getScore().isGood()) {
                    LearnitPattern learnitPattern = pattern.getObject();
                    String lemmatizedPatternStr = GeneralUtils.getLemmatizedPattern(learnitPattern);
                    goodOriginalPatternSet.add(learnitPattern);
                    if (useLemmatizedPatternAsFallback) {
                        goodLemmatizedPatternSet.addAll(lemmatizedStrToLearnitPattern.getOrDefault(lemmatizedPatternStr, new HashSet<>()));
                    }

                }
            }
            if (useLemmatizedPatternAsFallback) {
                for (LearnitPattern learnitPattern : goodLemmatizedPatternSet) {
                    goodPatterns.add(learnitPattern);
                    for (InstanceIdentifier instanceIdentifier : filtered.getInstancesForPattern(learnitPattern)) {
                        LabelPattern labelPattern = new LabelPattern(targetName, Annotation.FrozenState.FROZEN_GOOD);
                        labeledMappings.addOrChangeAnnotation(instanceIdentifier, labelPattern);
                        for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                            if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                                labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,Symbol.from(learnitPattern.toPrettyString()));
                            }
                            if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.triggerText)){
                                for(Symbol symbol : learnitPattern.getLexicalItems()){
                                    Symbol processedToken = processLexicalItems(symbol);
                                    if(processedToken.asString().length()>0){
                                        labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,processedToken);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (LearnitPattern learnitPattern : goodOriginalPatternSet) {
                goodPatterns.add(learnitPattern);
                for (InstanceIdentifier instanceIdentifier : filtered.getInstancesForPattern(learnitPattern)) {
                    LabelPattern labelPattern = new LabelPattern(targetName, Annotation.FrozenState.FROZEN_GOOD);
                    labeledMappings.addOrChangeAnnotation(instanceIdentifier, labelPattern);
                    for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                        if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                            labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,Symbol.from(learnitPattern.toPrettyString()));
                        }
                        if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.triggerText)){
                            for(Symbol symbol: learnitPattern.getLexicalItems()){
                                Symbol processedToken = processLexicalItems(symbol);
                                if(processedToken.asString().length()>0){
                                    labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,processedToken);
                                }
                            }
                        }
                    }
                }
            }
            if (this.useGoodSeeds) {
                for (AbstractScoreTable.ObjectWithScore<Seed, SeedScore> seed : targetAndScoreTables.getSeedScores().getObjectsWithScores()) {
                    if (seed.getScore().isFrozen() && seed.getScore().isGood()) {
                        Seed originalSeed = seed.getObject();
                        for (InstanceIdentifier instanceIdentifier : filtered.getInstancesForSeed(originalSeed)) {
                            LabelPattern labelPattern= new LabelPattern(targetName, Annotation.FrozenState.FROZEN_GOOD);
                            labeledMappings.addOrChangeAnnotation(instanceIdentifier, labelPattern);
                            for(LabelTrackingObserver labelTrackingObserver: labelTrackingObserverList){
                                if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
                                    labelTrackingObserver.addMarking(instanceIdentifier,labelPattern,Symbol.from(originalSeed.toPrettyString()));
                                }
                                // Not implemented yet
//                                if(labelTrackingObserver.getLabelerTask().equals(LabelTrackingObserver.LabelerTask.pattern)){
//                                    for(Symbol lexicalItems : originalSeed.getSlots()){
//
//                                    }
//                                }
                                // End not implement
                            }
                        }
                    }
                }
            }
        }
        return labeledMappings;
    }

}
