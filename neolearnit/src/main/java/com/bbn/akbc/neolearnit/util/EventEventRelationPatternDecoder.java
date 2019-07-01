package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.FileUtil;
import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.bbn.akbc.neolearnit.util.GeneralUtils.*;


public class EventEventRelationPatternDecoder {

    static boolean changePatternTokensToLemma = true;


    static boolean useOnlyPropPatterns = false;
    static int MIN_FREQ_EVENT_PAIRS = 0;
    static boolean USE_TRIPLE_WHITE_LIST = false;
    static Set<Triple<String, String, String>> triples_relation_event1_event2 = new HashSet<Triple<String, String, String>>();

    static JSONArray all_objects = new JSONArray();

    private static final Multimap<String, String> pattern2relation = HashMultimap.create();
    private static final Multimap<String, String> lemmatizedPattern2relation = HashMultimap
        .create();
    private static final Multimap<String,String> relation2instances = HashMultimap.create();
    private static final Multimap<String,String> instance2patterns = HashMultimap.create();

    private static final Target target = TargetFactory.makeBinaryEventEventTarget();

    private static final Multimap<String,Pair<String,String>> instanceToSlotPairTypes = HashMultimap.create();


    private static void loadPatternsUsingTimeStamps() throws IOException {
        ArrayList<Pair<String,String>> patternIDStringAndLabels =
                new ArrayList<>();
        Map<String, TargetAndScoreTables> latestExtractors = GeneralUtils.loadLatestExtractors();
        for (Map.Entry<String, TargetAndScoreTables> entry : latestExtractors.entrySet()) {
            String label = entry.getKey();
            TargetAndScoreTables ex = latestExtractors.get(label);
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : ex
                .getPatternScores().getObjectsWithScores()) {
                String patternIDString = pattern.getObject().toIDString().trim();
                if (pattern.getScore().isGood()) {
                    patternIDStringAndLabels.add(new Pair(patternIDString,label));
                }
            }
        }
        updatePattern2Relation(patternIDStringAndLabels);
    }

    private static void loadPatterns(String extractorDirectory) throws IOException {
        ArrayList<Pair<String,String>> patternIDStringAndLabels =
            new ArrayList<>();
        Map<String, TargetAndScoreTables> extractors =
            GeneralUtils.loadExtractors(extractorDirectory);
        for (Map.Entry<String, TargetAndScoreTables> entry : extractors.entrySet()) {
            String label = entry.getKey();
            TargetAndScoreTables ex = extractors.get(label);
            for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> pattern : ex
                .getPatternScores().getObjectsWithScores()) {
                String patternIDString = pattern.getObject().toIDString().trim();
                if (pattern.getScore().isGood()) {
                    patternIDStringAndLabels.add(new Pair(patternIDString,label));
                }
            }
        }
        updatePattern2Relation(patternIDStringAndLabels);
    }

    private static void updatePattern2Relation(List<Pair<String,String>> patternsAndLabels){
        for(Pair<String,String> patternAndLabel : patternsAndLabels) {
            String patternIDString = patternAndLabel.getFirst();
            String label = patternAndLabel.getSecond();
            String lemmatizedPattern = getLemmatizedPattern(patternIDString);
            pattern2relation.put(patternIDString, label);
            lemmatizedPattern2relation.put(lemmatizedPattern, label);
        }
    }

    static Set<Triple<String, String, String>> load_triples(String strFile, int min_freq_triples)
        throws IOException{
        Set<Triple<String, String, String>> triples_relation_event1_event2 = new HashSet<Triple<String, String, String>>();
        List<String> lines = readLinesIntoList(strFile);
        for(String line : lines) {
            line = line.trim();
            if(line.isEmpty())
                continue;

            int idx_space = line.indexOf(" ");
            int freq = Integer.parseInt(line.substring(0, idx_space));

            if(freq>=min_freq_triples) {
                line = line.substring(idx_space + 1);
                String[] items = line.split("\t");
                Triple<String, String, String> triple = Triple.of(items[0], items[1], items[2]);
                // System.out.println("in_list: " +  triple.toString());
                triples_relation_event1_event2.add(triple);
            }
        }

        return triples_relation_event1_event2;
    }

    static void updateDecodingMaps(Mappings mappings) {
        Multimap<String, InstanceIdentifier> reln2matchedInst = HashMultimap.create();
        Multimap<InstanceIdentifier, LearnitPattern> inst2pattern = HashMultimap.create();

        try {
            System.out.println("=== Number of patterns in mappings: "+ mappings.getAllPatterns().elementSet().size());
            int numHits = 0;
            for (LearnitPattern p : mappings.getAllPatterns().elementSet()) {
                if(useOnlyPropPatterns)
                    if(!(p instanceof PropPattern))
                        continue;

                String patternString = p.toIDString();
                String lemmatizedPattern = getLemmatizedPattern(patternString);

                if(!pattern2relation.containsKey(patternString) &&
                    lemmatizedPattern2relation.containsKey(lemmatizedPattern)){
                    System.out.println("=== Found a hit with lemmatized pattern for: "
                        + ""+patternString+"\t lemmatizedPattern: "+lemmatizedPattern);
                }

                if(pattern2relation.containsKey(patternString) ||
                        lemmatizedPattern2relation.containsKey(lemmatizedPattern)) {
                    numHits++;

                    Collection<String> relationsForPattern = pattern2relation.containsKey
                        (patternString)? pattern2relation.get(patternString)
                                       :lemmatizedPattern2relation.get(lemmatizedPattern);

                    Collection<InstanceIdentifier> instances = mappings.getInstancesForPattern(p);

                    for(String reln : relationsForPattern) {
                        reln2matchedInst.putAll(reln, instances);
                    }

                    for(InstanceIdentifier instanceIdentifier : instances)
                        inst2pattern.put(instanceIdentifier, p);
                }
            }

            System.out.println("=== Number of patterns decoded from mappings: "+numHits);

        } catch (Exception e) {
            e.printStackTrace();
        }


        for(String reln : reln2matchedInst.keySet()) {

            Set<InstanceIdentifier> uniqueIIDsForRelation = Sets.newHashSet(reln2matchedInst.get
                (reln));

            for (InstanceIdentifier instanceIdentifier : uniqueIIDsForRelation) {
                Optional<Triple<String,String,String>> triple = getInstanceStringWithSlotTypes(
                    instanceIdentifier,target,true);
                //Slot types in the triple (the middle and right elements) can be used to filter out
                //noisy slot types particularly when adding names from a list as mentions (e.g. for
                // world-modelers domain adaptation)

                if(triple.isPresent()) {
                    String instString = triple.get().getLeft();
//                    instanceToSlotPairTypes.put(instString,new Pair(triple.get().getMiddle(),
//                            triple.get().getRight()));
                    relation2instances.put(reln,instString);

                    for(LearnitPattern learnItPattern :  inst2pattern.get(instanceIdentifier)) {
                        instance2patterns.put(instString,learnItPattern.toIDString().trim());
                    }
                }
            }
        }
    }

    static JSONObject instanceToJson(String reln, String instanceString, String pattern) {
        JSONObject obj = new JSONObject();
        String[] items = instanceString.split("\t");

        String docid = items[0];
        int arg1_start = Integer.parseInt(items[1]);
        int arg1_end = Integer.parseInt(items[2]);
        int arg2_start = Integer.parseInt(items[3]);
        int arg2_end = Integer.parseInt(items[4]);

        JSONArray arg1_span_lists = new JSONArray();
        JSONArray arg1_spans = new JSONArray();

        arg1_spans.add(arg1_start);
        arg1_spans.add(arg1_end);

        arg1_span_lists.add(arg1_spans);
        obj.put("arg1_span_list", arg1_span_lists);

        obj.put("arg1_text", items[6]);

        JSONArray arg2_span_lists = new JSONArray();
        JSONArray arg2_spans = new JSONArray();
        arg2_spans.add(arg2_start);
        arg2_spans.add(arg2_end);
        arg2_span_lists.add(arg2_spans);
        obj.put("arg2_span_list", arg2_span_lists);

        obj.put("arg2_text", items[7]);

        obj.put("connective_text", "");
        obj.put("docid", docid);
        obj.put("relation_type", "Explicit");
        obj.put("semantic_class", reln);

        JSONArray patterns = new JSONArray();
        patterns.add(pattern);
        obj.put("learnit_pattern", patterns);

        return obj;
    }

    static void writeJson(String strOutputRelationJson) {
        try (FileWriter file = new FileWriter(strOutputRelationJson)) {
            file.write((new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()).toJson(all_objects));
            System.out.println("Successfully Copied JSON Object to File: "+strOutputRelationJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        try{
            trueMain(args);
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void trueMain(String [] argv) throws IOException {
        String paramFile = argv[0];
        String strJsonFileList = argv[1]; // could also be a single json file
        String strOutputRelationJson = argv[2];
        useOnlyPropPatterns = argv[3].trim().toLowerCase().equals("props"); // "props" or "all"
        MIN_FREQ_EVENT_PAIRS = Integer.parseInt(argv[4]);
        USE_TRIPLE_WHITE_LIST = argv[5].trim().toLowerCase().equals("use_triple_whitelist"); // "use_triple_whitelist" or "na"
        if (USE_TRIPLE_WHITE_LIST){
                String strFileTripleRelationEventPairs = argv[6];
                triples_relation_event1_event2 = load_triples(strFileTripleRelationEventPairs, MIN_FREQ_EVENT_PAIRS);
        }
        String extractorDirectory = argv[7];
        // Very bad practice AGAIN
        boolean activateSeedDecoder = false;
        for(String arg : argv){
            activateSeedDecoder |= arg.trim().toLowerCase().equals("decode_seed");
        }
        System.out.println("=== Loading params...");
        LearnItConfig.loadParams(new File(paramFile));

        loadPatterns(extractorDirectory);

        if(changePatternTokensToLemma){
            System.out.println("=== Loading word-lemma map...");
            loadWordLemmaMap();
            System.out.println("=== \tdone loading word-lemma map.");
        }

        List<String> jsonFiles = new ArrayList<>();
        if (!strJsonFileList.endsWith("json")) {
             jsonFiles = FileUtil.readLinesIntoList(new File(strJsonFileList));
        }else{
            jsonFiles.add(strJsonFileList);
        }

        for(String jsonFile : jsonFiles){
            System.out.println("=== Process json file: " + jsonFile);
            Mappings mappings = Mappings.deserialize(new File(jsonFile), true);
            updateDecodingMaps(mappings);
            if(activateSeedDecoder){
                decodeSeeds(extractorDirectory,mappings);
            }
        }
        decodePatterns(relation2instances,instance2patterns);
        writeJson(strOutputRelationJson);
    }

    static void decodeSeeds(String extractorDirectory,Mappings mappings) throws IOException{
        Map<String,TargetAndScoreTables> extractors = GeneralUtils.loadExtractors(extractorDirectory);
        for(Map.Entry<String,TargetAndScoreTables> relationTable : extractors.entrySet()){
            String relationName = relationTable.getKey();
            TargetAndScoreTables targetAndScoreTables = relationTable.getValue();
            Set<Seed> seedsInExtractor = new HashSet<>();
            for (AbstractScoreTable.ObjectWithScore<Seed, SeedScore> seed : targetAndScoreTables
                    .getSeedScores().getObjectsWithScores()){
                if(seed.getScore().isGood() && seed.getScore().isFrozen()){
                    seedsInExtractor.add(seed.getObject());
                }
            }
            for(Seed seed : seedsInExtractor){
                for(InstanceIdentifier instanceIdentifier : mappings.getInstancesForSeed(seed)){
                    Optional<Triple<String,String,String>> triple = getInstanceStringWithSlotTypes(
                            instanceIdentifier,target,true);
                    all_objects.add(instanceToJson(relationName,triple.get().getLeft(),"[Seed]"+seed.toPrettyString()));
                }
            }
        }
    }

    static void decodePatterns(Multimap<String,String> label2Instance,
                       Multimap<String,String> instance2Pattern) {
        Multimap<String,String> relnAndInstanceToPatterns = HashMultimap.create();

        for(String reln : label2Instance.keySet()) {
            for (String instString : label2Instance.get(reln)) {
                    String [] items = instString.trim().split("\t");
                    Triple<String, String, String> relation_event1_event2 = Triple.of(reln, items[6], items[7]);
                    if(USE_TRIPLE_WHITE_LIST) {
                        if(!triples_relation_event1_event2.contains(relation_event1_event2))
                            continue;
                    }
                    for(String pattern :  instance2Pattern.get(instString)) {
                        relnAndInstanceToPatterns.put(reln+"\t"+instString,pattern);
                    }
            }
        }
        for(String relnAndInstance : relnAndInstanceToPatterns.keySet()){
            StringBuilder patternStrings = new StringBuilder();
            for(String patternString : relnAndInstanceToPatterns.get(relnAndInstance)) {
                patternStrings.append(patternString + " | ");
            }
            String instanceString = relnAndInstance.substring(relnAndInstance.indexOf("\t")+1);
//            String typeString = "slot_types=[";
//            for(Pair<String,String> slotTypes : instanceToSlotPairTypes.get(instanceString)){
//                typeString+="("+slotTypes.getFirst()+","+slotTypes.getSecond()+"),";
//            }
//            typeString=typeString.substring(0,typeString.length()-1);
//            typeString+="]";
//            System.out.println(relnAndInstance+"\t"+patternStrings.toString()+"\t"+typeString);
            System.out.println(relnAndInstance+"\t"+patternStrings.toString());
            String reln = relnAndInstance.substring(0,relnAndInstance.indexOf("\t"));
            all_objects.add(instanceToJson(reln,instanceString,patternStrings.toString()));
        }


        System.out.println("=== Total number of relations found: "+all_objects.size());
    }



}

