package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.actors.ActorEntity;
import com.google.common.base.Optional;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.HashMultiset;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class FindLowerRankPatternGivenHighRankInstanceIdentifier {
    public static String getCanonicalName(Mention mention, DocTheory docTheory) {
        Optional<String> argCanonicalNameOptional = Optional.absent();
        Optional<Entity> entityOptional = mention.entity(docTheory);
        if (entityOptional.isPresent()) {
            ImmutableSet<ActorEntity> actorEntities = docTheory.actorEntities().forEntity(entityOptional.get());
            if (!actorEntities.isEmpty()) {
                ActorEntity actorEntity = actorEntities.iterator().next();
                argCanonicalNameOptional = Optional.of(actorEntity.actorName().asString());
            } else if (entityOptional.get().representativeName().isPresent()) {
                argCanonicalNameOptional = Optional.of(entityOptional.get().representativeName().get().mention().span().tokenizedText().utf16CodeUnits());
            }
        }
        return argCanonicalNameOptional.isPresent() ? argCanonicalNameOptional.get() : mention.span().tokenizedText().utf16CodeUnits();
    }

    public static void main(String[] args) throws Exception {
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));

//        List<String> highRankMappingsFileList = GeneralUtils.readLinesIntoList(args[1]);
//        List<String> lowRankMappingsFileList = GeneralUtils.readLinesIntoList(args[2]);

        Mappings highRankedMappings = Mappings.deserialize(new File(args[1]), true);
        Mappings lowRankedmappings = Mappings.deserialize(new File(args[2]), true);
        Annotation.InMemoryAnnotationStorage labeledMappings = new Annotation.InMemoryAnnotationStorage(highRankedMappings);

//        List<String> buf = GeneralUtils.readLinesIntoList(args[3]);
//        Set<String> requiredCanonicalNames = new HashSet<>();
//        for (String canonicalName : buf) {
//            requiredCanonicalNames.add(canonicalName.trim());
//        }

        String outputDir = args[4];
        String tripleOutputDir = outputDir+ File.separator + "triples";
        BufferedWriter tripleOutputBf = new BufferedWriter(new FileWriter(new File(tripleOutputDir)));
        String patternFreqPath = outputDir + File.separator + "pattern.freq";

        Map<String, Integer> patternToConnectedFreq = new HashMap<>();


        for (InstanceIdentifier highRankInstanceIdentifier : labeledMappings.getAllInstanceIdentifier()) {
            InstanceIdentifier myFakeLeftInstanceIdentifier = highRankInstanceIdentifier.getLowerRankInstanceIdentifierLeft();
            InstanceIdentifier myFakeRightInstanceIdentifier = highRankInstanceIdentifier.getLowerRankInstanceIdentifierRight();
            for (LabelPattern labelPattern : labeledMappings.lookupInstanceIdentifierAnnotation(highRankInstanceIdentifier)) {

                String label = labelPattern.getLabel().trim();

                for (LearnitPattern leftLowRankPattern : lowRankedmappings.getPatternsForInstance(myFakeLeftInstanceIdentifier)) {
//                    if (!isValidPattern(leftLowRankPattern))
//                        continue;

                    String leftPatString = leftLowRankPattern.toPrettyString().trim();

                    for (LearnitPattern rightLowRankPattern : lowRankedmappings.getPatternsForInstance(myFakeRightInstanceIdentifier)) {
//                        if (!isValidPattern(rightLowRankPattern))
//                            continue;


//                        boolean shouldKeepPattern = false;
//                        MatchInfo matchInfo = highRankInstanceIdentifier.reconstructMatchInfo(TargetFactory.makeBinaryEventEventTarget());
//                        for (Mention mention : matchInfo.getPrimaryLanguageMatch().getSentTheory().mentions()) {
//                            String canonicalName = getCanonicalName(mention, matchInfo.getPrimaryLanguageMatch().getDocTheory());
//                            if (requiredCanonicalNames.contains(canonicalName)) {
//                                shouldKeepPattern = true;
//                                break;
//                            }
//                        }
//                        if (!shouldKeepPattern)
//                            continue;

                        String rightPatString = rightLowRankPattern.toPrettyString().trim();

                        if (label.equals("partwhole")) {
                            partwhole_pattern.add(leftPatString);
                            partwhole_pattern.add(rightPatString);
                            partwhole_pattern_pattern.add(leftPatString + "\t" + rightPatString);
                            partwhole_pattern_total += 2;
                        } else if (label.equals("isa")) {
                            isa_pattern.add(leftPatString);
                            isa_pattern.add(rightPatString);
                            isa_pattern_pattern.add(leftPatString + "\t" + rightPatString);
                            isa_pattern_total += 2;
                        } else if (label.equals("cause")) {
                            cause_pattern.add(leftPatString);
                            cause_pattern.add(rightPatString);
                            cause_pattern_pattern.add(leftPatString + "\t" + rightPatString);
                            cause_pattern_total += 2;
                        }

                        // System.out.printf("< %s , %s , %s >\n",leftLowRankPattern.toPrettyString(),labelPattern.getLabel(),rightLowRankPattern.toPrettyString());
                    }
                }
            }
        }

        // calculate stats
        for (String p1 : partwhole_pattern.elementSet()) {
            for (String p2 : partwhole_pattern.elementSet()) {
                if (partwhole_pattern_pattern.contains(p1 + "\t" + p2)) {
                    double pmi = getPMI(p1, p2, "partwhole");
                    tripleOutputBf.write(pmi + "\t" + p1 + "\tpartwhole\t" + p2+"\n");
                    patternToConnectedFreq.put(p1,patternToConnectedFreq.getOrDefault(p1,0)+1);
                    patternToConnectedFreq.put(p2,patternToConnectedFreq.getOrDefault(p2,0)+1);
                }
            }
        }

        for (String p1 : isa_pattern.elementSet()) {
            for (String p2 : isa_pattern.elementSet()) {
                if (isa_pattern_pattern.contains(p1 + "\t" + p2)) {
                    double pmi = getPMI(p1, p2, "isa");
                    tripleOutputBf.write(pmi + "\t" + p1 + "\tisa\t" + p2+"\n");
                    patternToConnectedFreq.put(p1,patternToConnectedFreq.getOrDefault(p1,0)+1);
                    patternToConnectedFreq.put(p2,patternToConnectedFreq.getOrDefault(p2,0)+1);
                }
            }
        }

        for (String p1 : cause_pattern.elementSet()) {
            for (String p2 : cause_pattern.elementSet()) {
                if (cause_pattern_pattern.contains(p1 + "\t" + p2)) {
                    double pmi = getPMI(p1, p2, "cause");
                    tripleOutputBf.write(pmi + "\t" + p1 + "\tcause\t" + p2+"\n");
                    patternToConnectedFreq.put(p1,patternToConnectedFreq.getOrDefault(p1,0)+1);
                    patternToConnectedFreq.put(p2,patternToConnectedFreq.getOrDefault(p2,0)+1);
                }
            }
        }
        tripleOutputBf.close();
        BufferedWriter patternFreqWritter = new BufferedWriter(new FileWriter(new File(patternFreqPath)));
        for(String pattern:patternToConnectedFreq.keySet()){
            int freq = patternToConnectedFreq.get(pattern);
            patternFreqWritter.write("1\t"+freq+"\t"+pattern+"\n");
        }
        patternFreqWritter.close();
    }

    static int min_freq_patterns = 10;
    static int min_freq_pattern_pattern = 10;
    static long cause_pattern_total = 0;
    static Multiset<String> cause_pattern = HashMultiset.create();
    static Multiset<String> cause_pattern_pattern = HashMultiset.create();
    static long isa_pattern_total = 0;
    static Multiset<String> isa_pattern = HashMultiset.create();
    static Multiset<String> isa_pattern_pattern = HashMultiset.create();
    static long partwhole_pattern_total = 0;
    static Multiset<String> partwhole_pattern = HashMultiset.create();
    static Multiset<String> partwhole_pattern_pattern = HashMultiset.create();

    public static boolean isValidPattern(LearnitPattern learnitPattern) {
        String patString = learnitPattern.toPrettyString().trim();
        if (patString.equals("[0]") || patString.equals("[1]"))
            return false;

        if (!(patString.contains("GPE") || patString.contains("LOC") ||
                patString.contains("PER") || patString.contains("ORG") ||
                patString.contains("FAC") || patString.contains("VEH") || patString.contains("WEA") ||
                patString.contains("FOOD") || patString.contains("REFUGEE") || patString.contains("OTH"))) // want at least some args attached to the predicate
            return false;

        return true;
    }

    public static double getPMI(String leftPatString, String rightPatString, String label) {
        if (label.equals("cause")) {
            int count_left_right = cause_pattern_pattern.count(leftPatString + "\t" + rightPatString);
            long total = cause_pattern_total;
            int count_left = cause_pattern.count(leftPatString);
            int count_right = cause_pattern.count(rightPatString);
            if (count_left_right >= min_freq_pattern_pattern && count_left >= min_freq_patterns && count_right >= min_freq_patterns)
                return 1.0 * count_left_right * total / (count_left * count_right);
            else
                return 0.0;
        } else if (label.equals("isa")) {
            int count_left_right = isa_pattern_pattern.count(leftPatString + "\t" + rightPatString);
            long total = isa_pattern_total;
            int count_left = isa_pattern.count(leftPatString);
            int count_right = isa_pattern.count(rightPatString);
            if (count_left_right >= min_freq_pattern_pattern && count_left >= min_freq_patterns && count_right >= min_freq_patterns)
                return 1.0 * count_left_right * total / (count_left * count_right);
            else
                return 0.0;
        } else if (label.equals("partwhole")) {
            int count_left_right = partwhole_pattern_pattern.count(leftPatString + "\t" + rightPatString);
            long total = partwhole_pattern_total;
            int count_left = partwhole_pattern.count(leftPatString);
            int count_right = partwhole_pattern.count(rightPatString);
            if (count_left_right >= min_freq_pattern_pattern && count_left >= min_freq_patterns && count_right >= min_freq_patterns)
                return 1.0 * count_left_right * total / (count_left * count_right);
            else
                return 0.0;
        } else {
            System.err.println("Incorrect relation label: " + label);
            return -10000;
        }
    }
}