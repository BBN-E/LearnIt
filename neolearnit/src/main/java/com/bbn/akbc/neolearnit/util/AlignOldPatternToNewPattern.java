package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;

import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.LabelPatternReturn;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.patterns.PatternReturn;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.types.EntityType;


public class AlignOldPatternToNewPattern {

    private static final Pattern nounCombinationPattern1 =
        Pattern.compile("CombinationPattern\\[\\[Headword=(.*?), POSTag=(NN|NNS|NNP)]]\\[\\[(\\S*)]]");
    private static final Pattern nounCombinationPattern2 =
        Pattern.compile("CombinationPattern\\[\\[(\\S*)]]\\[\\[Headword=(.*?), POSTag=(NN|NNS|NNP)]]");
    private static final Pattern premodPattern =
        Pattern.compile("(\\S+?),");
    private static final Pattern verbHeadWordPattern =
        Pattern.compile("^\\[Headword=(.*?), POSTag=(VBN|VBG|VB|VBD|VBZ|VBP)]");
    private static final Pattern nounHeadWordPattern =
        Pattern.compile("^\\[Headword=(.*?), POSTag=(NN|NNS|NNP)]");
    private static final Pattern propPattern =
        Pattern.compile("^\\s*(= )?(modifier|verb|noun|copula|comp|set): \\[(.*)]");
    private static final Pattern binaryPropPattern =
        Pattern.compile(".*?(modifier|verb|noun|copula|comp|set): \\[(.*)]");
    private static final Pattern roleArgumentPatternUnary =
        Pattern.compile("^\\s*(.*?) (.*)");
    public static final Pattern entityTypePattern =
        Pattern.compile("PER|ORG|GPE|FAC|LOC|WEA|VEH|ART");
    private static final Pattern roleArgumentPatternBinary =
        Pattern.compile("^\\s*(.*?) (PER|ORG|GPE|FAC|LOC|WEA|VEH|ART|REFUGEE)? ?= (.*?)\\s*$");
    private static final Pattern inBetweenSlotsPattern =
        Pattern.compile("^\\{(\\d)} (.*) \\{(\\d)}$");


    private static final PatternReturn slot0PatternReturn =
        new LabelPatternReturn(Symbol.from("slot0"));
    private static final PatternReturn slot1PatternReturn =
        new LabelPatternReturn(Symbol.from("slot1"));


    public static Set<String> getNodeNamesFromOntology(String ontologyFilePath) throws Exception {
        BBNInternalOntology.BBNInternalOntologyNode ontologyNode = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(new File(ontologyFilePath));
        Map<String, BBNInternalOntology.BBNInternalOntologyNode> nodeIdToNodeMap = ontologyNode.getPropToNodeMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
        return new HashSet<>(nodeIdToNodeMap.keySet());
    }

    public static void addPatternToTable(LearnitPattern learnitPattern,PatternScoreTable patternScoreTable,boolean isGood){
        PatternScore patternScore = patternScoreTable.getScoreOrDefault(learnitPattern);
        if (patternScore.isFrozen()) patternScore.unfreeze();
        patternScore.setPrecision(isGood?0.95:0.05);
        patternScore.setConfidence(1.0);
        patternScore.freezeScore(0);
    }

    public static List<com.bbn.serif.patterns.Pattern> hqiuHandler(LearnitPattern learnitPattern) throws IOException {
        String learnitPatternPrettyStr = learnitPattern.toPrettyString();
        List<com.bbn.serif.patterns.Pattern> ret = new ArrayList<>();
        if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBN]][[rains,received]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("rains")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBD]][[received,heavy,rain]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("rains")));
            mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString("heavy"));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBD]][[received,aboveaverage,rain]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("rains")));
            mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString("aboveaverage"));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBD]][[received,above-average,rain,during]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("rains")));
            mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString("above-average"));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBD]][[received,rainfall]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("rainfall")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[REFUGEE,have,arrived]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "arrived".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=arriving, POSTag=VBG]][[REFUGEE,arriving]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "arriving".toLowerCase();
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[sought,refuge,in]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "sought".toLowerCase();
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("refuge")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[influx,of,returnees]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "influx".toLowerCase();
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("of")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("returnees")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=sought, POSTag=VBN]][[sought,refuge]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "sought".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("refuge")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=arrived, POSTag=VBD]][[REFUGEE,arrived]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "arrived".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[REFUGEE,influx]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "influx".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[REFUGEE,have,arrived,in]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "arrived".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[REFUGEE,arriving]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "arriving".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<sub>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.of("REFUGEE")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=access, POSTag=VB]][[access,education]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "access".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("education")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=providing, POSTag=VBG]][[providing,access,to,education]]")){
            PropPattern.Builder ppPBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String ppredicateHead = "providing".toLowerCase();
            ppPBuilder.withPredicates(getSymbolSetFromString(ppredicateHead));
            ppPBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder aapBuilder = new ArgumentPattern.Builder();
            aapBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));


            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "access".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("to")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("education")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            aapBuilder.withPattern(ppBuilder.build());
            ppPBuilder.withArgs(Lists.newArrayList(aapBuilder.build()));
            ret.add(ppPBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("[influx,of,refugees,fleeing]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "influx".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("of")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("refugees")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=received, POSTag=VBN]][[funding,received]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "received".toLowerCase();
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("funding")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=providing, POSTag=VBG]][[providing,food]]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "providing".toLowerCase();
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("<obj>")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withHeadwords(Sets.newHashSet(Symbol.from("food")));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[[Headword=influx, POSTag=VBD]][verb: [[0]]\n" +
                "   to GPE = 1 ]")){
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = "influx".toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
            apBuilder.withRoles(Lists.newArrayList(Symbol.from("to")));
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            mpBuilder.withAceTypes(Lists.newArrayList(EntityType.GPE));
            apBuilder.withPattern(mpBuilder.build());
            ppBuilder.withArgs(Lists.newArrayList(apBuilder.build()));
            ret.add(ppBuilder.build());
        }
        else if(learnitPatternPrettyStr.contains("CombinationPattern[Type: TIMEX2.TIME][verb: [[0]]\n" +
                "   in = 1 ]")){
            com.bbn.serif.patterns.Pattern pattern = SerifPattern.construcPatternFromSexpStr("(vprop (return slot0) (args (argument (role in) (value (return slot1) (type TIMEX2.TIME)))))");
            ret.add(pattern);
        }
        return ret;
    }

    public static void unaryExtractorHandler(File file,String outputRoot) throws IOException {
        String extractorName = file.getName().replace(".json","");
        TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(file);
        PatternScoreTable patternScoreTable = new PatternScoreTable();
        for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry: targetAndScoreTables.getPatternScores().getObjectsWithScores()){
            if(patternScoreTableEntry.getScore().isFrozen()){
                LearnitPattern srcLearnitPattern = patternScoreTableEntry.getObject();
                if(!(srcLearnitPattern instanceof SerifPattern)){
                    String learnitPrettyString = srcLearnitPattern.toPrettyString();
                    System.out.println("--------------");
                    System.out.println(learnitPrettyString);
                    System.out.println("\n");
                    List<com.bbn.serif.patterns.Pattern> patterns = convertUnaryPattern(learnitPrettyString);
                    if (patterns.size() == 0) {
                        patterns = hqiuHandler(srcLearnitPattern);
                        if(patterns.size() == 0){
                            System.out.println("\nCOULD NOT CONVERT");
                            addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                        }
                        else{
                            for(com.bbn.serif.patterns.Pattern p:patterns){
                                System.out.println("HQIU handler "+ p.toString());
                                addPatternToTable(new SerifPattern(p),patternScoreTable,patternScoreTableEntry.getScore().isGood());
                            }
                        }
                    }
                    else
                       for (com.bbn.serif.patterns.Pattern p : patterns){
                           System.out.println(p.toString());
                           addPatternToTable(new SerifPattern(p),patternScoreTable,patternScoreTableEntry.getScore().isGood());
                       }

                }
                else{
                    addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                }
            }
            else{
                addPatternToTable(patternScoreTableEntry.getObject(),patternScoreTable,false);
            }
        }
        TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(patternScoreTable);
        newTargetAndScoreTables.serialize(new File(outputRoot+File.separator+extractorName+".json"));
    }

    public static void binaryExtractorHandler(File file,String outputRoot) throws IOException {
        String extractorName = file.getName().replace(".json","");
        TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(file);
        PatternScoreTable patternScoreTable = new PatternScoreTable();
        for(AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> patternScoreTableEntry: targetAndScoreTables.getPatternScores().getObjectsWithScores()){
            if(patternScoreTableEntry.getScore().isFrozen()){
                LearnitPattern srcLearnitPattern = patternScoreTableEntry.getObject();
                if(!(srcLearnitPattern instanceof SerifPattern)){

                    // Only handle old patterns
                    String learnitPrettyString = srcLearnitPattern.toPrettyString();
                    System.out.println("--------------");
                    System.out.println(learnitPrettyString);
                    System.out.println("\n");

                    List<com.bbn.serif.patterns.Pattern> patterns = convertBinaryPattern(learnitPrettyString);
                    if (patterns.size() == 0){
                        patterns = hqiuHandler(srcLearnitPattern);
                        if(patterns.size() == 0){
                            System.out.println("\nCOULD NOT CONVERT");
                            addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                        }
                        else{
                            for(com.bbn.serif.patterns.Pattern p:patterns){
                                System.out.println("HQIU handler "+ p.toString());
                                addPatternToTable(new SerifPattern(p),patternScoreTable,patternScoreTableEntry.getScore().isGood());
                            }
                        }
                    }

                    else
                        for (com.bbn.serif.patterns.Pattern p : patterns) {
                            System.out.println(p.toString());
                            addPatternToTable(new SerifPattern(p), patternScoreTable, patternScoreTableEntry.getScore().isGood());
                        }
                }
                else{
                    addPatternToTable(srcLearnitPattern,patternScoreTable,patternScoreTableEntry.getScore().isGood());
                }
            }
            else{
                addPatternToTable(patternScoreTableEntry.getObject(),patternScoreTable,false);
            }
        }
        TargetAndScoreTables newTargetAndScoreTables = targetAndScoreTables.copyWithPatternScoreTable(patternScoreTable);
        newTargetAndScoreTables.serialize(new File(outputRoot+File.separator+extractorName+".json"));
    }

    /* verb: [encourage]
          <obj> = 1
          <sub> = 0
   */
    private static List<PropPattern> getPropPatterns(
        String s, int line, int indent, PatternReturn topLevelReturn)
    {

        String[] lines = s.split("\n");

        Matcher m = binaryPropPattern.matcher(lines[line]);
        if (!m.find()) {
            System.out.println("Couldn't match prop: " + s);
            System.exit(1);
        }
        String predTypeString = m.group(1);
        String predicateOrSlot = m.group(2);
        Proposition.PredicateType predType = getPredicateType(predTypeString);

        PropPattern.Builder ppBuilder = new PropPattern.Builder(predType);
        if (topLevelReturn != null)
            ppBuilder.withPatternReturn(topLevelReturn);

        if (predicateOrSlot.equals("[0]"))
            ppBuilder.withPatternReturn(slot0PatternReturn);
        else if (predType != Proposition.PredicateType.SET) // patterns based on SETs don't have predicates
            ppBuilder.withPredicates(getSymbolSetFromString(predicateOrSlot));

        // For each argument, create a set of possible ArgumentPattern objects.
        List<Set<ArgumentPattern>> listOfSets = new ArrayList<>();
        for (int i = line + 1; i < lines.length; i++) {

            String argumentLine = lines[i];

            // If we hit another prop at the same level, we can stop looking for arguments
            if (hasIndentOf(argumentLine, indent)) break;

            if (hasIndentOf(argumentLine, indent + 3)) {
                Set<ArgumentPattern> argumentPatternSet = new HashSet<>();

                Matcher m2 = roleArgumentPatternBinary.matcher(argumentLine);
                if (m2.find()) {

                    String role = m2.group(1);
                    String entityTypeString = m2.group(2);
                    String argumentString = m2.group(3);

                    ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
                    apBuilder.withRoles(getSymbolListFromString(role));

                    Matcher m3 = binaryPropPattern.matcher(argumentString);
                    if (!m3.find()) {
                        // Could be mention pattern
                        MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
                        if (entityTypeString != null)
                            mpBuilder.withAceTypes(getEntityTypeListFromString(entityTypeString));
                        if (argumentString.equals("0")) {
                            mpBuilder.withPatternReturn(slot0PatternReturn);
                        } else if (argumentString.equals("1")) {
                            mpBuilder.withPatternReturn(slot1PatternReturn);
                        } else {
                            mpBuilder.withHeadwords(getSymbolSetFromString(argumentString));
                        }
                        apBuilder.withPattern(mpBuilder.build());
                        argumentPatternSet.add(apBuilder.build());

                        if ((argumentString.equals("0") || argumentString.equals("1")) &&
                             entityTypeString == null)
                        {
                            // could be prop pattern
                            PropPattern.Builder ppBuilder2 =
                                new PropPattern.Builder(Proposition.PredicateType.VERB);
                            if (argumentString.equals("0")) {
                                ppBuilder2.withPatternReturn(slot0PatternReturn);
                            } else if (argumentString.equals("1")) {
                                ppBuilder2.withPatternReturn(slot1PatternReturn);
                            }
                            apBuilder.withPattern(ppBuilder2.build());
                            argumentPatternSet.add(apBuilder.build());

                            PropPattern.Builder ppBuilder3 =
                                new PropPattern.Builder(Proposition.PredicateType.NOUN);
                            if (argumentString.equals("0")) {
                                ppBuilder3.withPatternReturn(slot0PatternReturn);
                            } else if (argumentString.equals("1")) {
                                ppBuilder3.withPatternReturn(slot1PatternReturn);
                            }
                            apBuilder.withPattern(ppBuilder3.build());
                            argumentPatternSet.add(apBuilder.build());
                        }
                    } else {
                        // PropPattern
                        List<PropPattern> possiblePatterns =
                            getPropPatterns(s, i, indent+3, null);

                        for (PropPattern possiblePattern : possiblePatterns) {
                            apBuilder.withPattern(possiblePattern);
                            argumentPatternSet.add(apBuilder.build());
                        }
                    }
                }

                listOfSets.add(argumentPatternSet);
            }
        }

        // Take all combinations of possible argument patterns
        Set<List<ArgumentPattern>> crossProduct = Sets.cartesianProduct(listOfSets);

        List<PropPattern> results = new ArrayList<>();
        for (List<ArgumentPattern> args : crossProduct) {
            ppBuilder.withArgs(args);
            results.add(ppBuilder.build());
        }

        return results;
    }

    private static boolean hasIndentOf(String line, int indent) {
        int i;
        for (i = 0; i < indent; i++) {
            if (line.charAt(i) != ' ') return false;
        }
        if (line.charAt(i) == ' ') return false;
        return true;
    }

    private static List<com.bbn.serif.patterns.Pattern>
    convertBinaryPattern(String learnitPrettyString) {
        List<com.bbn.serif.patterns.Pattern> results = new ArrayList<>();
        Matcher m = propPattern.matcher(learnitPrettyString);

        if (m.find()) {
            PatternReturn patternReturn = null;
            if (!learnitPrettyString.contains("0"))
                patternReturn = slot0PatternReturn;
            else if (!learnitPrettyString.contains("1"))
                patternReturn = slot1PatternReturn;

            results.addAll(getPropPatterns(learnitPrettyString, 0, 0, patternReturn));
            return results;
        }

        m = inBetweenSlotsPattern.matcher(learnitPrettyString);
        if (m.find()) {
            String firstSlotString = m.group(1);
            String inBetweenString = m.group(2).toLowerCase();
            String secondSlotString = m.group(3);

            PatternReturn firstPatternReturn = null;
            PatternReturn secondPatternReturn = null;

            if (firstSlotString.equals("0"))
                firstPatternReturn = slot0PatternReturn;
            if (firstSlotString.equals("1"))
                firstPatternReturn = slot1PatternReturn;

            if (secondSlotString.equals("0"))
                secondPatternReturn = slot0PatternReturn;
            if (secondSlotString.equals("1"))
                secondPatternReturn = slot1PatternReturn;

            if (firstPatternReturn == secondPatternReturn ||
                firstPatternReturn == null ||
                secondPatternReturn == null) {
                System.out.println("Bad pattern returns in inBetweenSlotsPattern");
                System.exit(1);
            }

            results.add(makeRegexPattern(
                makeTextPattern(firstPatternReturn, null),
                makeTextPattern(null, inBetweenString),
                makeTextPattern(secondPatternReturn, null)));
        }

        return results;
    }

    private static com.bbn.serif.patterns.Pattern makeRegexPattern(
        com.bbn.serif.patterns.Pattern p1,
        com.bbn.serif.patterns.Pattern p2,
        com.bbn.serif.patterns.Pattern p3)
    {
        RegexPattern.Builder rpBuilder = new RegexPattern.Builder();

        List<com.bbn.serif.patterns.Pattern> subpatterns = new ArrayList<>();
        subpatterns.add(p1);
        subpatterns.add(p2);
        subpatterns.add(p3);
        rpBuilder.withSubpatterns(subpatterns);

        return rpBuilder.build();
    }

    private static com.bbn.serif.patterns.Pattern makeMentionPattern(PatternReturn pr) {
        MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
        mpBuilder.withPatternReturn(pr);
        return mpBuilder.build();
    }

    private static com.bbn.serif.patterns.Pattern makeTextPattern(PatternReturn pr, String text) {
        TextPattern.Builder tpBuilder = new TextPattern.Builder();
        if (pr != null)
            tpBuilder.withPatternReturn(pr);
        if (text != null)
            tpBuilder.withText(text);
        return tpBuilder.build();
    }

    private static List<com.bbn.serif.patterns.Pattern>
    convertUnaryPattern(String learnitPrettyString) {
        List<com.bbn.serif.patterns.Pattern> results = new ArrayList<>();
        Matcher m = nounCombinationPattern1.matcher(learnitPrettyString);
        if (m.find()) {
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            String headWord = m.group(1).toLowerCase();
            String premods = m.group(3).toLowerCase();
            String premodString = getPremodString(premods, headWord);
            mpBuilder.withHeadwords(getSymbolSetFromString(headWord));
            mpBuilder.withPatternReturn(slot0PatternReturn);
            if (premodString.length() > 0)
                mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString(premodString));

            results.add(mpBuilder.build());
            return results;
        }

        m = nounCombinationPattern2.matcher(learnitPrettyString);
        if (m.find()) {
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            String headWord = m.group(2).toLowerCase();
            String premods = m.group(1).toLowerCase();
            String premodString = getPremodString(premods, headWord);
            mpBuilder.withHeadwords(getSymbolSetFromString(headWord));
            mpBuilder.withPatternReturn(slot0PatternReturn);
            if (premodString.length() > 0)
                mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString(premodString));

            results.add(mpBuilder.build());
            return results;
        }

        m = nounHeadWordPattern.matcher(learnitPrettyString);
        if (m.find()) {
            MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
            String headWord = m.group(1).toLowerCase();
            mpBuilder.withHeadwords(getSymbolSetFromString(headWord));
            mpBuilder.withPatternReturn(slot0PatternReturn);
            results.add(mpBuilder.build());
            return results;
        }
        m = verbHeadWordPattern.matcher(learnitPrettyString);
        if (m.find()) {
            PropPattern.Builder ppBuilder = new PropPattern.Builder(Proposition.PredicateType.VERB);
            String predicateHead = m.group(1).toLowerCase();
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            ppBuilder.withPatternReturn(slot0PatternReturn);

            results.add(ppBuilder.build());
            return results;
        }

        m = propPattern.matcher(learnitPrettyString);
        if (m.find()) {
            Proposition.PredicateType predicateType = getPredicateType(m.group(2));
            String predicateHead = m.group(3).toLowerCase();
            PropPattern.Builder ppBuilder = new PropPattern.Builder(predicateType);
            ppBuilder.withPatternReturn(slot0PatternReturn);
            ppBuilder.withPredicates(getSymbolSetFromString(predicateHead));
            String[] lines = learnitPrettyString.split("\n");
            // arguments
            List<ArgumentPattern> argumentPatterns = new ArrayList<>();
            Set<Symbol> seenHeadwords = new HashSet<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.length() == 0) continue;
                if (line.startsWith("trigger")) continue;
                ArgumentPattern.Builder apBuilder = new ArgumentPattern.Builder();
                Matcher am = roleArgumentPatternUnary.matcher(line);
                if (am.find()) {
                    String role = am.group(1);
                    String argument = am.group(2);
                    Matcher m2 = propPattern.matcher(argument);
                    if (m2.find()) {
                        // ignore this argument, it appears that when this happens, we're just repeating the
                        // top level prop
                        continue;
                    }
                    apBuilder.withRoles(getSymbolListFromString(role));
                    apBuilder.withPattern(getPatternFromArgumentString(argument));
                    ArgumentPattern ap = apBuilder.build();
                    MentionPattern mp = (MentionPattern) ap.getPattern();
                    if (mp.getHeadwords().size() > 0) {
                        Symbol headword = (Symbol) mp.getHeadwords().toArray()[0];
                        if (!seenHeadwords.contains(headword))
                            argumentPatterns.add(ap);
                        seenHeadwords.add(headword);
                    } else {
                        argumentPatterns.add(ap);
                    }

                } else {
                    System.out.println("Could not parse argument line: " + line);
                    System.exit(1);
                }
            }
            ppBuilder.withArgs(argumentPatterns);
            results.add(ppBuilder.build());
            return results;
        }

        return results;
    }

    private static com.bbn.serif.patterns.Pattern getPatternFromArgumentString(String argumentString) {
        MentionPattern.Builder mpBuilder = new MentionPattern.Builder();
        String possibleEntityType = argumentString.substring(0, 3);
        Matcher m = entityTypePattern.matcher(possibleEntityType);
        if (m.find()) {
            mpBuilder.withAceTypes(getEntityTypeListFromString(possibleEntityType));
            return mpBuilder.build();
        }
        // list of words here, turn into headword and premods
        argumentString = argumentString.toLowerCase();
        if (argumentString.endsWith(" =")) {
            argumentString = argumentString.substring(0, argumentString.length() - 2);
        } else {
            System.out.println("Unexpected argument string: " + argumentString);
            System.exit(1);
        }

        int lastSpace = argumentString.lastIndexOf(" ");
        String headword = argumentString;
        String premods = "";
        if (lastSpace != -1) {
            // multiple words
            headword = argumentString.substring(lastSpace+1);
            premods = argumentString.substring(0, lastSpace);
        }
        mpBuilder.withHeadwords(getSymbolSetFromString(headword));
        if (premods.length() > 0) {
            mpBuilder.withRegexPattern(PatternGenerator.getRegexPatternFromString(premods));
        }
        return mpBuilder.build();
    }

    private static List<EntityType> getEntityTypeListFromString(String etString) {
        EntityType et = EntityType.of(etString);
        List<EntityType> results = new ArrayList<>();
        results.add(et);
        return results;
    }

    private static Proposition.PredicateType getPredicateType(String s) {
        Proposition.PredicateType predicateType = null;
        if (s.equals("noun"))
            predicateType = Proposition.PredicateType.NOUN;
        else if (s.equals("verb"))
            predicateType = Proposition.PredicateType.VERB;
        else if (s.equals("set"))
            predicateType = Proposition.PredicateType.SET;
        else if (s.equals("copula"))
            predicateType = Proposition.PredicateType.VERB;
        else if (s.equals("modifier"))
            predicateType = Proposition.PredicateType.MODIFIER;
        else if (s.equals("comp"))
            predicateType = Proposition.PredicateType.COMP;
        else {
            System.out.println("Unknown predicate type");
            System.exit(1);
        }
        return predicateType;
    }

    private static List<Symbol> getSymbolListFromString(String s) {
        List<Symbol> results = new ArrayList<>();
        results.add(Symbol.from(s));
        return results;
    }

    private static Set<Symbol> getSymbolSetFromString(String s) {
        Set<Symbol> results = new HashSet<>();
        results.add(Symbol.from(s));
        return results;
    }

    private static String getPremodString(String premods, String headword) {
        StringBuilder premodStringBuilder = new StringBuilder();

        Matcher m = premodPattern.matcher(premods + ",");
        while (m.find()) {
            String word = m.group(1);
            if (!word.equals(headword)) {
                if (premodStringBuilder.length() != 0)
                    premodStringBuilder.append(" ");
                premodStringBuilder.append(word);
            }
        }
        return premodStringBuilder.toString();
    }

    public static void main(String[] args)throws Exception{
        LearnItConfig.loadParams(new File(args[0]));
        String inputRoot = args[1];
        String outputRoot = args[2];
        Set<String> unaryExtractorNameSet = new HashSet<>();
        Set<String> binaryExtractorNameSet = new HashSet<>();
        unaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/unary_event_ontology_hume.yaml", LearnItConfig.get("learnit_root"))));
        unaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/wm/hume/internal_ontology/unary_event_ontology_hume.yaml", LearnItConfig.get("learnit_root"))));
        binaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/binary_event_ontology.yaml", LearnItConfig.get("learnit_root"))));
        binaryExtractorNameSet.addAll(getNodeNamesFromOntology(String.format("%s/inputs/ontologies/cx/bae_icm/internal_ontology/binary_event_entity_or_value_mention.yaml", LearnItConfig.get("learnit_root"))));

//        File dir = new File(String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root")));
        File dir = new File(inputRoot);
        Map<String, TargetAndScoreTables> e = new HashMap<>();

        System.out.println("UNARY:\n\n");
        for (File file : dir.listFiles()) {
            String extractorName = file.getName().replace(".json", "");
            if (unaryExtractorNameSet.contains(extractorName)) {
                unaryExtractorHandler(file,outputRoot);

            }
        }

        System.out.println("\n\nBINARY:\n\n");
        for (File file : dir.listFiles()) {
            String extractorName = file.getName().replace(".json", "");
            if(binaryExtractorNameSet.contains(extractorName)){
                binaryExtractorHandler(file,outputRoot);
            }
        }
    }

}
