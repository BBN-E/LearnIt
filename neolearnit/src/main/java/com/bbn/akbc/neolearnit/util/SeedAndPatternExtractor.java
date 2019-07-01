package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.utility.FileUtil;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts seeds and patterns from a given mapping file.
 * The main purpose of extracting seeds (slot-pairs) and patterns by this utility
 * is to use them later in the pipeline, for things like similarity-matrix generation etc.
 * This similarity-matrix generation part of LearnIt for seeds is currently agnostic of the order of slots, one can make
 * use of the parameter "honor_slot_ordering_for_seed_extraction" appropriately.
 */
public class SeedAndPatternExtractor {

    public static void main(String [] args) throws IOException {
        String params = args[0];
        String strListJsonFiles = args[1];
        String seedOutputFile = args[2];
        String patternOutputFile = args[3];
        String targetName = args[4];

        LearnItConfig.loadParams(new File(params));

        //true by default
        boolean honorSlotOrderingForSeedExtraction =
                !LearnItConfig.defined("honor_slot_ordering_for_seed_extraction") ||
                        LearnItConfig.optionalParamTrue("honor_slot_ordering_for_seed_extraction");
        //false by default
        boolean normalizeSeedsByTargetDef = LearnItConfig.optionalParamTrue("normalize_seeds_by_target_def");

        Multiset<Seed> seeds =  HashMultiset.create();
        Multiset<String> patterns = HashMultiset.create();

        PrintWriter seedOutput = new PrintWriter(new OutputStreamWriter(new FileOutputStream(seedOutputFile)));
        PrintWriter patternOutput = new PrintWriter(new OutputStreamWriter(new FileOutputStream(patternOutputFile)));

        List<String> listJsonFiles = FileUtil.readLinesIntoList(strListJsonFiles);
        List<String> listOutputJsonFiles = new ArrayList<>();

        for (String strJsonFile : listJsonFiles) {
            System.out.println("Load mapping file " + strJsonFile);

            File mappingsJsonFile =  new File(strJsonFile);
            Mappings mappings = Mappings.deserialize(mappingsJsonFile, true);

            if(normalizeSeedsByTargetDef){
                mappings = (new NormalizeSeedsFilter(new Target.Builder(targetName).build())).makeFiltered(mappings);
            }

            System.out.println("Extracting seeds and patterns...");
            seeds.addAll(extractSeedsFromMappings(mappings,honorSlotOrderingForSeedExtraction));
            patterns.addAll(extractPatternIDsFromMappings(mappings).stream().map(
                    (PatternID patternID)->patternID.getPatternIDString()).collect(Collectors.toList()));
        }

        System.out.println("Saving seeds...");
        for(Seed seed : Multisets.copyHighestCountFirst(seeds).elementSet()){
            seedOutput.println(seed.getSlot(0)+"\t"+seed.getSlot(1)+"\t"+seeds.count(seed));
        }
        seedOutput.close();

        System.out.println("Saving patterns...");
        for(String pattern : Multisets.copyHighestCountFirst(patterns).elementSet()){
            patternOutput.println(pattern+"\t"+patterns.count(pattern));
        }
        patternOutput.close();

        if (!listOutputJsonFiles.isEmpty()){
            File inputListFile = new File(strListJsonFiles);
            String outputDirectory = LearnItConfig.get("updated_mappings_lists_dir")+File.separator+targetName;
            new File(outputDirectory).mkdirs();
            FileUtil.writeToFile(listOutputJsonFiles, "\n", outputDirectory + File.separator + inputListFile.getName());
        }

        System.out.println("Finished extracting seeds and patterns!");
    }

    public static Multiset<Seed> extractSeedsFromMappings(Mappings mappings, boolean honorSlotOrdering) {
        Multiset<Seed> allSeeds = HashMultiset.create();
        Stream<Seed> seedsInMapping = mappings.getAllSeeds().stream();
        if(!honorSlotOrdering){
            seedsInMapping = seedsInMapping.map((Seed seed)->seed.makeSymmetric());
        }
        allSeeds.addAll(seedsInMapping.collect(Collectors.toList()));
        return allSeeds;
    }

    public static Multiset<PatternID> extractPatternIDsFromMappings(Mappings mappings) {
        Multiset<PatternID> allPatterns = HashMultiset.create();
        allPatterns.addAll(
                mappings.getAllPatterns().stream().filter(
                        (LearnitPattern learnitPattern) -> learnitPattern.isCompletePattern()).map((LearnitPattern learnitPattern) ->
                        PatternID.from(learnitPattern)).collect(Collectors.toList()));
        return allPatterns;
    }
}
