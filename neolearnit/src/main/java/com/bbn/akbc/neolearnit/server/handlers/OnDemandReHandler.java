package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.BBNInternalOntology;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EmptySlotConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EventMentionOnlyConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.IsMentionConstraint;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.pattern.initialization.InitializationPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.ObservationSimilarity;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposalInformation;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposer;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruner;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruner;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scorers.SimplifiedPatternScorer;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.scores.TripleScore;
import com.bbn.akbc.neolearnit.scoring.selectors.SeedAutoAcceptSelector;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.SeedScoreTable;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.akbc.neolearnit.util.InstanceIdentifierFilterForAnnotation;
import com.bbn.akbc.utility.Pair;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.bue.common.validators.ValidationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

// import SubsamplingFilter;

public class OnDemandReHandler extends SimpleJSONHandler {

    static final Map<String, String> ontologyNameToPathMap = ImmutableMap.<String, String>builder()
            .put("unaryEvent", String.format("%s/inputs/internal_ontology/unary_event_ontology_hume.yaml", LearnItConfig.get("learnit_root")))
            .put("binaryEvent", String.format("%s/inputs/internal_ontology/binary_event_ontology.yaml", LearnItConfig.get("learnit_root")))
            .put("binaryEntity", String.format("%s/inputs/internal_ontology/binary_entity_ontology.yaml", LearnItConfig.get("learnit_root")))
            .put("unaryEntity", String.format("%s/inputs/internal_ontology/unary_entity_ontology.yaml", LearnItConfig.get("learnit_root")))
            .build();



    private final Map<String,TargetAndScoreTables> extractors;
    private final Map<String,Mappings> mappingLookup;
    private final Mappings mappings;

    private final String targetPathDir;

//    private final String outputDir;

//    private final Map<String,ObservationSimilarityModule> observationSimilarityModules;
    private ObservationSimilarityModule observationSimilarityModule;
    private final String dirSuffixForSimilarityMatrices;
    private final String instanceIdentifierAnnotationFileNormalPath;
    private final String instanceIdentifierAnnotationFileOtherPath;
    private final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageNormal;
    private final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageOther;
    private static String getNowDateString() {
        Date date = new Date();
        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date))
                .replace("-", "");
        return timeString;
    }

    private Map<String, BBNInternalOntology.BBNInternalOntologyNode> ontologyMap;

    /**
     *
     * @param mappings deserialized mappings object
     * @param dirSuffixForSimilarityMatrices Directory suffix for similarity matrices; usually it will be a
     *                                       combination of the target and epoch that were used for
     *                                       running learnit sequence in order to create the similarity matrices,
     *                                       e.g. all_event_event_pairs-3
     */
    public OnDemandReHandler(Mappings mappings, String dirSuffixForSimilarityMatrices) throws Exception{
        this.ontologyMap = new HashMap<>();
        this.extractors = new HashMap<String,TargetAndScoreTables>();
        this.mappingLookup = new HashMap<String,Mappings>();
        this.mappings = mappings;
//        this.observationSimilarityModules = new HashMap<String,ObservationSimilarityModule>();
        this.dirSuffixForSimilarityMatrices = dirSuffixForSimilarityMatrices;
        //true by default
        boolean honorSlotOrderingForSeedSimilarity =
                !LearnItConfig.defined("honor_slot_ordering_for_seed_extraction") ||
                        LearnItConfig.optionalParamTrue("honor_slot_ordering_for_seed_extraction");

        //  pattern constraint-based filters
        System.out.println("Filter incomplete patterns...");
//        MappingsFilter patternConstraintsFilter =
//                new PatternConstraintsFilter(new HashSet<LearnitPattern>(),
//                        new HashSet<LearnitPattern>(), true);
        //TODO: uncomment later if necessary; this takes a lot of time to execute
//        this.mappings = patternConstraintsFilter.makeFiltered(this.mappings);

        if(dirSuffixForSimilarityMatrices!=null) {
            try{
                this.observationSimilarityModule =  ObservationSimilarityModule.create(this.mappings, dirSuffixForSimilarityMatrices);
            }
            catch (IOException e){
                this.observationSimilarityModule = null;
                System.out.println("Find similar function is down");
            }
        }
        else{
            this.observationSimilarityModule = null;
            System.out.println("Find similar function is down");
        }
        this.instanceIdentifierAnnotationFileNormalPath = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        this.instanceIdentifierAnnotationFileOtherPath = String.format("%s/inputs/relation_annotation_by_learnit_ui/%s_other.sjson", LearnItConfig.get("learnit_root"), LearnItConfig.get("corpus_name"));
        File parentDir = new File(String.format("%s/inputs/relation_annotation_by_learnit_ui", LearnItConfig.get("learnit_root")));
        if (!parentDir.exists()) {
            parentDir.mkdir();
        }
        if (new File(this.instanceIdentifierAnnotationFileNormalPath).isFile()) {
            this.inMemoryAnnotationStorageNormal = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(this.instanceIdentifierAnnotationFileNormalPath), true));
        } else {
            this.inMemoryAnnotationStorageNormal = new Annotation.InMemoryAnnotationStorage();
        }
        if (new File(this.instanceIdentifierAnnotationFileOtherPath).isFile()) {
            this.inMemoryAnnotationStorageOther = new Annotation.InMemoryAnnotationStorage(Mappings.deserialize(new File(this.instanceIdentifierAnnotationFileOtherPath), true));
        } else {
            this.inMemoryAnnotationStorageOther = new Annotation.InMemoryAnnotationStorage();
        }

            // String targetPathRel  = String.format("inputs/targets/extractors/%s/%s.json",name);
            this.targetPathDir = String.format("%s/inputs/extractors/",LearnItConfig.get("learnit_root"));

            File dir = new File(this.targetPathDir);

            if(dir.exists()) {
                for (File subDir : dir.listFiles()) {
                    if(subDir.isDirectory()) {
                        String targetName = subDir.getName(); // target name is the directory name

                        String latestFileTimestamp = GeneralUtils.getLatestExtractor(targetName, subDir).orNull();
                        TargetAndScoreTables ex = new TargetAndScoreTables(TargetFactory.fromString(targetName));
                        if (latestFileTimestamp!=null) {
                            String fileName = String.format("%s/%s_%s.json", subDir.getAbsolutePath(),
                                    targetName, latestFileTimestamp);
                            System.out.println("Loading extractor " + targetName + " from: " + fileName);
                            ex = TargetAndScoreTables.deserialize(new File(fileName));
                        }
                        extractors.put(ex.getTarget().getName(), ex);
                        TargetFilter targetFilter = new TargetFilter(ex.getTarget());
                        Mappings filteredMappings = targetFilter.makeFiltered(this.mappings);
                        mappingLookup.put(ex.getTarget().getName(), filteredMappings);

                    }
                }
            }

            /*
            if((new File(outputDirOrJsonFile)).isFile() && outputDirOrJsonFile.endsWith(".json")) {
                this.outputDir = outputDirOrJsonFile.substring(0, outputDirOrJsonFile.lastIndexOf("/"));

                File f = new File(outputDirOrJsonFile);
                if (f.toString().endsWith(".json")) {
                    TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);
                    extractors.put(ex.getTarget().getName(), ex);
                    mappingLookup.put(ex.getTarget().getName(), (new TargetFilter(ex.getTarget()).makeFiltered(this.mappings)));
                }
            }
            else {
                this.outputDir = outputDirOrJsonFile;

                File relDir = new File(outputDir);
                // load json files
                for (File f : relDir.listFiles()) {
                    if (f.toString().endsWith(".json")) {
                        TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);
                        extractors.put(ex.getTarget().getName(), ex);
                        mappingLookup.put(ex.getTarget().getName(), new TargetFilter(ex.getTarget()).makeFiltered(this.mappings));
                    }
                }
            }
            */

                /*
                // make a everything target
                TargetAndScoreTables targetAndScoreTables = new TargetAndScoreTables(TargetFactory.makeEverythingTarget());
                extractors.put(targetAndScoreTables.getTarget().getName(), targetAndScoreTables);
                mappingLookup.put(targetAndScoreTables.getTarget().getName(), new TargetFilter(targetAndScoreTables.getTarget()).makeFiltered(this.mappings));
*/

                /*
                // if no json files found, load target XML files
                for (File f : relDir.listFiles()) {
                    if (f.toString().endsWith(".target.xml")) {
                        Target target = TargetFactory.fromTargetXMLFile(f.getAbsolutePath()).get(0);
                        TargetAndScoreTables ex = new TargetAndScoreTables(target);

                        extractors.put(ex.getTarget().getName(), ex);
                        mappingLookup.put(ex.getTarget().getName(), new TargetFilter(ex.getTarget()).makeFiltered(this.mappings));
                    }
                }
                */

            // write top instances if hasn't been written
            // writeTopInstances(outputDir);
        for (String ontologyName : ontologyNameToPathMap.keySet()) {
            final File ontologyFile = new File(ontologyNameToPathMap.get(ontologyName));
            this.ontologyMap.put(ontologyName, BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile));
        }
    }

    public void writeTopInstances(String outputDir) {
        for(String targetName : mappingLookup.keySet()) {

            System.out.println("Writing out freq patterns and instances for target: " + targetName);

            String fileNameTargetFrequentPatternAndSentences = outputDir + "/" + targetName + ".patternAndSent.txt";
            File fileOutput = new File(fileNameTargetFrequentPatternAndSentences);

            if(!fileOutput.exists()) {

                Mappings mappings = mappingLookup.get(targetName);
                // for(LearnitPattern : mappings.getAllPatterns
                Iterable<Multiset.Entry<LearnitPattern>> patternsSortedByCount =
                        Multisets.copyHighestCountFirst(mappings.getAllPatterns()).entrySet();

                try {
                    Writer writer = new OutputStreamWriter(
                            new FileOutputStream(fileNameTargetFrequentPatternAndSentences), "UTF-8");
                    int MAX_NUM_PATTERNS_PRINT = 2000;
                    int MAX_NUM_INSTANCES_PER_PATTERN = 1;

                    int patterns_printed = 0;

                    for (Multiset.Entry<LearnitPattern> learnitPatternEntry : patternsSortedByCount) {
                        LearnitPattern learnitPattern = learnitPatternEntry.getElement();

                        // if(learnitPattern.getLexicalItems()
                        if(getPatternLanguage(learnitPattern).equalsIgnoreCase("chinese") &&
                                !learnitPattern.getLexicalItems().isEmpty()) {
                            int num_inst_printed = 0;

                            for (InstanceIdentifier instanceIdentifier : mappings
                                    .getInstancesForPattern(learnitPattern)) {
                                MatchInfo.LanguageMatchInfo matchInfo = instanceIdentifier
                                        .reconstructMatchInfo(extractors.get(targetName).getTarget())
                                        .getPrimaryLanguageMatch();
                                String arg1 = matchInfo.getSlot0().get().tokenSpan()
                                        .tokenizedText(matchInfo.getDocTheory()).utf16CodeUnits();
                                String arg2 = matchInfo.getSlot1().get().tokenSpan()
                                        .tokenizedText(matchInfo.getDocTheory()).utf16CodeUnits();

                                String sent = matchInfo.getSentTheory().tokenSpan()
                                        .tokenizedText(matchInfo.getDocTheory()).utf16CodeUnits();

                                writer.write(
                                        learnitPatternEntry.getCount() + "\t" + learnitPattern.toIDString()
                                                + "\t" + "<" + arg1 + ", " + arg2 + ">\t" + matchInfo.markedUpTokenString() + "\n");

                                if (++num_inst_printed > MAX_NUM_INSTANCES_PER_PATTERN)
                                    break;
                            }

                            if (++patterns_printed >= MAX_NUM_PATTERNS_PRINT)
                                break;

                            writer.write("\n");
                        }


                    }

                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getPatternLanguage(LearnitPattern p) {
        if(p instanceof PropPattern) {
            PropPattern propPattern = (PropPattern) p;
            return propPattern.getLanguage();
        }
        else if(p instanceof BetweenSlotsPattern) {
            BetweenSlotsPattern betweenSlotsPattern = (BetweenSlotsPattern) p;
            return betweenSlotsPattern.getLanguage();
        }
        else
            return "N/A";
    }


    @JettyMethod("/init/get_targets")
    public synchronized List<Target> getTargets() {
        List<Target> result = new ArrayList<Target>();
        for (String key : extractors.keySet()) {
            result.add(extractors.get(key).getTarget());
        }
        return result;
    }


    private static void calculatePatternScoreExtra(LearnitPattern learnitPattern, PatternScore patternScore, TargetAndScoreTables targetAndScoreTables, Mappings mappings) {
        Set<Seed> seedMatched = mappings.getSeedsForPattern(learnitPattern).elementSet();
        patternScore.setPatternPrecision(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
        patternScore.setPatternWeightedPrecision(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
        patternScore.setPatternFrequency(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
    }

    @JettyMethod("/init/get_extractor")
    public synchronized TargetAndScoreTables getExtractor(@JettyArg("relation") String relation) {
        return checkGetExtractor(relation);
    }

    @JettyMethod("/init/clear_unknown")
    public synchronized String clearUnknown(@JettyArg("target") String relation) {
        TargetAndScoreTables ext = checkGetExtractor(relation);
        ext.getPatternScores().removeNonFrozen();
        ext.getSeedScores().removeNonFrozen();
        ext.getTripleScores().removeNonFrozen();
        return "success";
    }

    @JettyMethod("/init/clear_all")
    public synchronized String clearAll(@JettyArg("target") String relation) {
        TargetAndScoreTables ext = checkGetExtractor(relation);
        ext.getPatternScores().clear();
        ext.getSeedScores().clear();
        ext.getTripleScores().clear();
        return "success";
    }

    @JettyMethod("/init/save_progress")
    public synchronized String saveProgress() throws Exception {
        this.inMemoryAnnotationStorageNormal.convertToMappings().serialize(new File(this.instanceIdentifierAnnotationFileNormalPath), true);
        this.inMemoryAnnotationStorageOther.convertToMappings().serialize(new File(this.instanceIdentifierAnnotationFileOtherPath), true);
        for (String ontologyName : ontologyNameToPathMap.keySet()) {
            final File ontologyFile = new File(ontologyNameToPathMap.get(ontologyName));
            BBNInternalOntology.BBNInternalOntologyNode currentOntologyNode = this.ontologyMap.get(ontologyName);
            currentOntologyNode.convertToInternalOntologyYamlFile(ontologyFile);
        }

        Map<String,List<String>> targetToErroneousPatterns = new HashMap<>();
        for (String ex : extractors.keySet()) {
            System.out.println("Saving extractor for "+ex+"...");
            TargetAndScoreTables ext = extractors.get(ex);
            System.out.println("\tclearing unfrozen artifacts...");
            Optional<List<String>> patternsInError = checkForSymmetricPatterns(ext);

            // @hqiu We truly want to save OTHER in any case.
//            if(ex.equals("OTHER"))patternsInError = Optional.absent();
            if(patternsInError.isPresent()){
                targetToErroneousPatterns.put(ext.getTarget().getName(),patternsInError.get());
                continue;
            }
            if(!ex.equals("OTHER"))clearUnknown(ex);
            ext.incrementIteration(); // set the iteration back up
            // make directory for writing extractor
            String strTargetPathDir = String.format("%s/%s/",this.targetPathDir, ext.getTarget().getName());
            File dir = new File(strTargetPathDir);
            if(!dir.exists()) {
                dir.mkdir();
            }
            String strPathJson = strTargetPathDir + ext.getTarget().getName() + "_" +
                    getNowDateString() + ".json";
            System.out.println("\t serializing extractor for "+ex+"...");
            ext.serialize(new File(strPathJson));
            System.out.println("\t\t...done.");
        }
        if(targetToErroneousPatterns.isEmpty()){
            return "Saved all extractors successfully!";
        }
        String retVal = "Following extractors could not be saved.\nPlease fix the error, and try again.\n";
        for(String target: targetToErroneousPatterns.keySet()){
            retVal+="\nTarget "+target+" have these patterns with slots interchanged: \n";
            for (String pattern : targetToErroneousPatterns.get(target)) {
                retVal += pattern+"\n";
            }
        }
        // Runtime.getRuntime().exit(0);
        return retVal;
    }

    private static synchronized Optional<List<String>> checkForSymmetricPatterns(TargetAndScoreTables ex){
        Target target = ex.getTarget();
        if (target.isSymmetric()){
            return Optional.absent();
        }
        List<String> patternsInError = new ArrayList<>();
        String targetName = target.getName();
        Set<String> slotAgnosticPatterns = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern,PatternScore> obj : ex.getPatternScores().getObjectsWithScores()) {
            if (!obj.getScore().isFrozen() || (obj.getScore().isFrozen() && !obj.getScore().isGood())){
                continue;
            }
            LearnitPattern p = obj.getObject();
            String pString = p.toIDString().replace("0", "SLOT").replace("1", "SLOT");
            if (slotAgnosticPatterns.contains(pString)) {
                System.out.println("Target: " + targetName + " has the following pattern with slots reversed: " + p.toIDString());
                patternsInError.add(p.toIDString());
            }
            slotAgnosticPatterns.add(pString);
        }
        return patternsInError.isEmpty()?Optional.absent():Optional.of(patternsInError);
    }

    private synchronized TargetAndScoreTables checkGetExtractor(String target) {
        if (!extractors.containsKey(target) || !mappingLookup.containsKey(target)) {
            throw new RuntimeException("Target "+target+" not known");
        }
        return this.extractors.get(target);
    }

    /*------------------------------------------------------------------*
     *                                                                  *
     *                       INSTANCE ROUTINES                          *
     *                                                                  *
     *------------------------------------------------------------------*/

    public Map<String,List<String>> getInstanceContexts(Set<InstanceIdentifier> insts, Target target, int amount,String relationName,boolean fromOther) throws IOException {
        Map<String,List<String>> result = new HashMap<>();

        System.out.println("We had "+insts.size()+" instances originally.");
        insts = InstanceIdentifierFilterForAnnotation.makeFiltered(insts);
        System.out.println("Now we filtered it out to "+insts.size());
        List<String> InstanceIdentifierSet = new ArrayList<>();
        List<String> HTMLStringSet = new ArrayList<>();
        List<String> AnnotationSet = new ArrayList<>();
        List<String> RelationNameSet = new ArrayList<>();
        if(fromOther){
            for(InstanceIdentifier instanceIdentifier : insts){
                Multiset<LabelPattern> labels = inMemoryAnnotationStorageOther.lookupInstanceIdentifierAnnotation(instanceIdentifier);
                // It could have multiple labels. Just use one for now
                String relationType = "UNTOUCHED";
                if (labels.size() >= 1) {
                    relationType = new ArrayList<>(labels).get(0).getLabel();
                }
                String htmlString = instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                // This filter will be enforced at frontend again
                if(InstanceIdentifierSet.size() >= amount)break;
                InstanceIdentifierSet.add(StorageUtils.getDefaultMapper().writeValueAsString(instanceIdentifier));
                HTMLStringSet.add(htmlString);
                AnnotationSet.add(FrozenState.NO_FROZEN.toString());//It's no use at all but we keep it for consistency
                RelationNameSet.add(relationType);
            }
        }
        else{
            for(InstanceIdentifier instanceIdentifier : insts){
                Multiset<LabelPattern> labels = inMemoryAnnotationStorageNormal.lookupInstanceIdentifierAnnotation(instanceIdentifier);
                FrozenState frozenState = FrozenState.NO_FROZEN;
                for (LabelPattern labelPattern : labels) {
                    if (labelPattern.getLabel().equals(relationName) && !labelPattern.getFrozenState().equals(FrozenState.NO_FROZEN)) {
                        frozenState = labelPattern.getFrozenState();
                    }
                }
                String htmlString = instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                // This filter will be enforced at frontend again
                if(InstanceIdentifierSet.size() >= amount)break;
                InstanceIdentifierSet.add(StorageUtils.getDefaultMapper().writeValueAsString(instanceIdentifier));
                HTMLStringSet.add(htmlString);
                AnnotationSet.add(frozenState.toString());
                RelationNameSet.add(relationName);//It's no use at all but we keep it for consistency
            }
        }
        result.put("InstanceIdentifierSet",InstanceIdentifierSet);
        result.put("HTMLStringSet",HTMLStringSet);
        result.put("AnnotationSet",AnnotationSet);
        result.put("RelationNameSet",RelationNameSet);
        return result;
    }

    @JettyMethod("/init/get_pattern_instances")
    public synchronized Map<String,List<String>> getPatternInstances(@JettyArg("target") String target,
                                                               @JettyArg("pattern") String patternStr, @JettyArg("amount") String amountStr,
                                                                     @JettyArg("fromOther") String fromOtherStr
                                                                     ) throws IOException {
        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int amount = Integer.parseInt(amountStr);
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = mappingLookup.get(ext.getTarget().getName());
        LearnitPattern pattern = findPattern(info,patternStr);
        if (pattern == null) {
            info = info.getAllPatternUpdatedMappings(ext);
            pattern = findPattern(info,patternStr);
        }
        if (pattern != null) {
            Set<InstanceIdentifier> insts = new HashSet<>(info.getInstancesForPattern(pattern));
            return getInstanceContexts(insts,ext.getTarget(),amount,target,fromOther);
        } else {
            return getInstanceContexts(new HashSet<>(),ext.getTarget(),amount,target,fromOther);
        }
    }

    @JettyMethod("/init/get_seed_instances")
    public synchronized Map<String,List<String>> getSeedInstances(@JettyArg("target") String target,
                                                            @JettyArg("seed") String seedStr, @JettyArg("amount") String amountStr,
                                                                  @JettyArg("fromOther") String fromOtherStr
                                                                  ) throws IOException {
        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int amount = Integer.parseInt(amountStr);
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = mappingLookup.get(ext.getTarget().getName());
        Seed seed = getSeed(seedStr,target);
        if (seed != null) {
            Set<InstanceIdentifier> insts = new HashSet<>(info.getInstancesForSeed(seed));
            return getInstanceContexts(insts,ext.getTarget(),amount,target,fromOther);
        }
        return getInstanceContexts(new HashSet<>(),ext.getTarget(),amount,target,fromOther);
    }

    /**
     *
     * @param target
     * @param tripleStr A SeedPatternSimilarity json string of the form: {"language":"<language>","slot0":"word0","slot1":"word1","pattern":"patternString"}
     * @param amountStr
     * @return
     * @throws IOException
     */
    @JettyMethod("/init/get_triple_instances")
    public synchronized Map<String,List<String>> getTripleInstances(@JettyArg("target") String target,
                                                              @JettyArg("triple") String tripleStr, @JettyArg("amount") String amountStr) throws IOException {

        int amount = Integer.parseInt(amountStr);
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = mappingLookup.get(ext.getTarget().getName());

        Set<InstanceIdentifier> insts = new HashSet<>(getTripleInstances(tripleStr,target,info));
        return getInstanceContexts(insts,ext.getTarget(),amount,target,false);
    }

    private Collection<InstanceIdentifier> getTripleInstances(String tripleStr, String targetStr, Mappings info) throws IOException{
        Target target = (new Target.Builder(targetStr)).build();
        SeedPatternPair triple = parseTripleJson(tripleStr,targetStr).orNull();
        if (triple==null){
            return Lists.newArrayList();
        }
        Seed seed = triple.seed();
        String patStr = triple.pattern().getPatternIDString();
        LearnitPattern pattern = findPattern(checkGetExtractor(targetStr), targetStr, patStr);
        Collection<InstanceIdentifier> iidsForTriples = Lists.newArrayList();
        Collection<InstanceIdentifier> iidsForSeed = info.getInstancesForSeed(seed);
        Collection<InstanceIdentifier> iidsForPattern = info.getInstancesForPattern(pattern);
        Collection<InstanceIdentifier> commonInstances = Sets.intersection(Sets.newHashSet(iidsForSeed),Sets.newHashSet(iidsForPattern));
        for (InstanceIdentifier iid : commonInstances) {
            Collection<Seed> seeds = mappings.getSeedsForInstance(iid);
            boolean instanceHasSeed = seeds.contains(seed);
            Collection<String> patternStrings = mappings.getPatternsForInstance(iid).stream().map(
                    (LearnitPattern p)->p.toIDString()).collect(Collectors.toSet());
            boolean instanceHasPattern = patternStrings.contains(patStr);
            if(instanceHasSeed && instanceHasPattern){
                iidsForTriples.add(iid);
            }
        }
        return iidsForTriples;
    }


    /*------------------------------------------------------------------*
     *                                                                  *
     *                       PATTERN ROUTINES                           *
     *                                                                  *
     *------------------------------------------------------------------*/

    boolean isBilingualOrChineseMonolingualString(String str) {
        for(int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if((c>=0x4E00 && c<=0x9FFF) || (c>=0x3400 && c<=0x4DFF) ||
                    (c>=0x20000 && c<=0x2A6DF) || (c>=0xF900 && c<=0xFAFF) ||
                    (c>=0x2F800 && c<=0x2FA1F))
                return true;
        }

        return false;
    }

    boolean isBilingualOrChineseMonolingual(LearnitPattern pattern) {
        return isBilingualOrChineseMonolingualString(pattern.toIDString());
    }

    boolean isBilingualOrChineseMonolingual(Seed seed) {
        return isBilingualOrChineseMonolingualString(seed.getSlot(0).toString()) ||
                isBilingualOrChineseMonolingualString(seed.getSlot(1).toString());
    }

    /**
     * PATTERN SEARCH
     * ---------------------------------------
     * Searches for patterns by the keywords in their lexical content
     *
     * @param target
     * @param keyword
     * @return
     */
    @JettyMethod("/init/get_patterns_by_keyword")
    public synchronized List<LearnitPattern> getPatternsByKeyword(@JettyArg("target") final String target, @JettyArg("keyword") String keyword,@JettyArg("amount") String amountStr) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Set<LearnitPattern> results = new HashSet<LearnitPattern>();

        int maximumResultLength = Integer.parseInt(amountStr);

        // search by slot0 or slot1 strings, keywords must be in the form #keyword#, e.g., #Bill Clinton#
        if(keyword.startsWith("#") && keyword.endsWith("#")) {
            keyword = keyword.substring(1, keyword.length()-1).toLowerCase();

            for (Seed seed : mappingLookup.get(target).getInstance2Seed().getAllSeeds().elementSet()) {
                String slot0text = seed.getSlot(0).toString().toLowerCase();
                String slot1text = seed.getSlot(1).toString().toLowerCase();

                if(slot0text.contains(keyword) || slot1text.contains(keyword)) {
                    for(LearnitPattern pattern : mappingLookup.get(target).getPatternsForSeed(seed)) {
                        if(!extractor.getPatternScores().isKnownFrozen(pattern) && pattern.isCompletePattern()) {
                            results.add(pattern);
                        }
                    }
                }
            }
        }
        else {

            // search by keyword on the pattern
            ImmutableSet<String> kwd = ImmutableSet.copyOf(keyword.split(" "));
            for (LearnitPattern pattern : mappingLookup.get(target).getInstance2Pattern().getAllPatterns().elementSet()) {
                if (hasKeyword(SymbolUtils.toStringSet(pattern.getLexicalItems()), kwd) &&
                        !extractor.getPatternScores().isKnownFrozen(pattern) && pattern.isCompletePattern())
                {
                    results.add(pattern);
                }
            }
        }

        List<LearnitPattern> resultList = new ArrayList<LearnitPattern>(results);



        //sort by frequency
        Collections.sort(resultList, new Comparator<LearnitPattern>() {
            @Override
            public int compare(LearnitPattern arg0, LearnitPattern arg1) {
                return mappingLookup.get(target).getInstance2Pattern().getInstances(arg1).size() -
                        mappingLookup.get(target).getInstance2Pattern().getInstances(arg0).size();
            }

        });

        List<LearnitPattern> filteredResultList = new ArrayList<>();
        for(int i = 0;i < Math.min(resultList.size(),maximumResultLength);++i){
            filteredResultList.add(resultList.get(i));
        }

        return filteredResultList;
    }

    private LearnitPattern findPattern(TargetAndScoreTables data, String target, String pattern) {
        LearnitPattern p1 = findPattern(mappingLookup.get(target), pattern);
        if (p1 == null) {
//			System.out.println("===================================");
            return findPattern(mappingLookup.get(target).getAllPatternUpdatedMappings(data), pattern);
        } else {
            return p1;
        }
    }

    private LearnitPattern findPattern(Mappings info, String pattern) {
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }
        return null;
    }

    private static void calculateSeedScoreExtra(Seed seed, SeedScore seedScore, TargetAndScoreTables targetAndScoreTables, Mappings mappings) {
        Set<LearnitPattern> patternsMatched = mappings.getPatternsForSeed(seed).elementSet();
        seedScore.setSeedFrequency(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
        seedScore.setSeedWeightedPrecision(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
        seedScore.setSeedPrecision(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
    }

    @JettyMethod("/init/add_target")
    public synchronized String addTarget(
            @JettyArg("name") String name,
            @JettyArg("description") String description,
            @JettyArg("slot0EntityTypes") String slot0EntityTypes,
            @JettyArg("slot1EntityTypes") String slot1EntityTypes,
            @JettyArg("symmetric") String symmetricStr,
            @JettyArg("isUnaryTarget") String isUnaryStr,
            @JettyArg("isEventTarget") String isEventStr
    ) {
        if (this.extractors.containsKey(name)) {
            throw new RuntimeException("You're creating a target which targetname is conflict with existing targets.");
        }
        boolean isUnary = Boolean.parseBoolean(isUnaryStr);
        boolean isEvent = Boolean.parseBoolean(isEventStr);
        boolean symmetric = Boolean.parseBoolean(symmetricStr);
        String targetPathRel = String.format("inputs/targets/json/%s.json", name);
        String targetPathFull = String.format("%s/%s", LearnItConfig.get("learnit_root"), targetPathRel);

        Target newTarget;
        ArrayList<String> slot0List = Lists.newArrayList(slot0EntityTypes.split(","));
        ArrayList<String> slot1List = Lists.newArrayList(slot1EntityTypes.split(","));
        Target.Builder newTargetBuilder;
        if (isEvent) {
            newTargetBuilder = new Target.Builder(name)
                    .setDescription(description)
                    .withTargetSlot(new TargetSlot.Builder(0, "all")
                            .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                            .build());
            newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(0, false));
            if (!isUnary) newTargetBuilder.withAddedConstraint(new EventMentionOnlyConstraint(1, false));
            else newTargetBuilder.withAddedConstraint(new EmptySlotConstraint(1));
        } else {
//            TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
//                    .withAddedConstraint(new AtomicMentionConstraint(0))
//                    .withAddedConstraint(new EntityTypeConstraint(0,slot0List))
//                    .withAddedConstraint(new MinEntityLevelConstraint("ALL",0,"DESC"))
//                    .build();
//            TargetSlot slot1;
//            if(!isUnary){
//                slot1 = new TargetSlot.Builder(1, "mention")
//                        .withAddedConstraint(new AtomicMentionConstraint(1))
//                        .withAddedConstraint(new EntityTypeConstraint(1,slot1List))
//                        .withAddedConstraint(new MinEntityLevelConstraint("ALL",1,"DESC"))
//                        .build();
//            }
//            else{
//                slot1 = new TargetSlot.Builder(1, "all")
//                        .build();
//            }
//            newTargetBuilder = new Target.Builder(name)
//                    .setDescription(description)
//                    .withTargetSlot(slot0).withTargetSlot(slot1)
//                    .withAddedConstraint(new EntityTypeConstraint(0, slot0List));
//            if (symmetric)newTargetBuilder.withAddedProperty(new TargetProperty("symmetric"));
//            if(!isUnary)newTargetBuilder.withAddedConstraint(new IsMentionConstraint(1,false));
//            else newTargetBuilder.withAddedConstraint(new EntityTypeConstraint(1, slot1List));
            newTargetBuilder = new Target.Builder(name)
                    .setDescription(description)
                    .withTargetSlot(new TargetSlot.Builder(0, "all")
                            .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                            .build());
            newTargetBuilder.withAddedConstraint(new IsMentionConstraint(0, false));
            if (!isUnary) newTargetBuilder.withAddedConstraint(new IsMentionConstraint(1, false));
            else newTargetBuilder.withAddedConstraint(new EmptySlotConstraint(1));
        }


        newTarget = newTargetBuilder.build();
        try {
            newTarget.serialize(targetPathFull);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        TargetAndScoreTables targetAndScoreTables = new TargetAndScoreTables(targetPathFull);
        extractors.put(name, targetAndScoreTables);
//        mappingLookup.put(name, new TargetFilter(newTarget).makeFiltered(mappings));
        TargetFilter targetFilter = new TargetFilter(targetAndScoreTables.getTarget());
        Mappings targetMappings = targetFilter.makeFiltered(this.mappings);
//        UnaryInstanceIdentifierFilter unaryInstanceIdentifierFilter = new UnaryInstanceIdentifierFilter();
//        targetMappings = unaryInstanceIdentifierFilter.makeFiltered(targetMappings);
        mappingLookup.put(name, targetMappings);
        return "successfully build target: " + name;
    }

    /**
     * MANUAL ACCEPT PATTERNS
     * ------------------------------------
     * Adds the given set of patterns to the extractor as known good patterns.
     *
     * @param target
     * @param patStr
     * @return
     * @throws IOException
     */

    @JettyMethod("/init/add_pattern")
    public synchronized String addPattern(@JettyArg("target") String target, @JettyArg("pattern") String patStr, @JettyArg("quality") String quality) {

        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = mappingLookup.get(target);
        System.out.println("=====info: info.getInstanceCount()=" + info.getInstanceCount() + ", info.getAllPatterns()=" + info.getAllPatterns().size());
        LearnitPattern pattern = findPattern(ext, target, patStr);
        if (pattern != null) {

            ext.getPatternScores().addDefault(pattern);
            double instanceWeight = 0.0;
            for (InstanceIdentifier id : info.getInstancesForPattern(pattern)) {
                instanceWeight += id.getConfidence();
            }

            PatternScore score = ext.getPatternScores().getScore(pattern);
            if(score.isFrozen())score.unfreeze();
            int frequency = info.getInstancesForPattern(pattern).size();

            score.setFrequency(frequency);
            score.setConfidenceDenominator(instanceWeight);
            score.setRecall(0.01);

            if (quality.equals("good")) {
                score.setPrecision(0.95);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator()*score.getConfidence());
                score.setKnownFrequency((int)Math.round(score.getPrecision()*score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
                if (info.getKnownInstanceCount(ext) == 0) {
                    ext.setGoodSeedPrior(score.getConfidenceNumerator()*10,ext.getTotalInstanceDenominator());
                }

            } else if (quality.equals("bad")) {
                score.setPrecision(0.05);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator()*score.getConfidence());
                score.setKnownFrequency((int)Math.round(score.getPrecision()*score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());

            } else {
                throw new RuntimeException("Unknown quality "+quality);
            }


            System.out.println("tp :"+score.getTP());
            System.out.println("fp :"+score.getFP());
            System.out.println("tn :"+score.getTN());
            System.out.println("fn :"+score.getFN());

            calculatePatternScoreExtra(pattern, score, ext, info);

            score.freezeScore(ext.getIteration());
            ext.getPatternScores().orderItems();
        } else {
            throw new RuntimeException("Could not add pattern "+patStr);
        }

        return "Added pattern";
    }


    /**
     * MANUAL ACCEPT TRIPLE
     * ------------------------------------
     * Adds the given triple to the extractor as a known good triple.
     *
     * @param targetStr
     * @param tripleStr A SeedPatternSimilarity json string of the form: {"language":"<language>","slot0":"word0","slot1":"word1","pattern":"patternString"}
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/add_triple")
    public synchronized String addTriple(@JettyArg("target") String targetStr, @JettyArg("triple") String tripleStr, @JettyArg("quality") String quality) throws IOException{

        TargetAndScoreTables ext = checkGetExtractor(targetStr);
        Mappings info = mappingLookup.get(targetStr);
        SeedPatternPair triple = parseTripleJson(tripleStr,targetStr).orNull();
        if (triple != null && !ext.getTripleScores().isKnownFrozen(triple)) {
            ext.getTripleScores().addDefault(triple);
            double instanceWeight = 0.0;
            Collection<InstanceIdentifier> iidsForTriples = getTripleInstances(tripleStr,targetStr,info);
            for (InstanceIdentifier iid : iidsForTriples) {
                instanceWeight += iid.getConfidence();
            }
            TripleScore score = ext.getTripleScores().getScore(triple);
            int frequency = iidsForTriples.size();

            score.setFrequency(frequency);
            score.setConfidenceDenominator(instanceWeight);
            score.setRecall(0.01);

            if (quality.equals("good")) {
                score.setPrecision(0.95);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator()*score.getConfidence());
                score.setKnownFrequency((int)Math.round(score.getPrecision()*score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
                if (info.getKnownInstanceCount(ext) == 0) {
                    ext.setGoodSeedPrior(score.getConfidenceNumerator()*10,ext.getTotalInstanceDenominator());
                }

            } else if (quality.equals("bad")) {
                score.setPrecision(0.05);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator()*score.getConfidence());
                score.setKnownFrequency((int)Math.round(score.getPrecision()*score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());

            } else {
                throw new RuntimeException("Unknown quality "+quality);
            }


            System.out.println("tp :"+score.getTP());
            System.out.println("fp :"+score.getFP());
            System.out.println("tn :"+score.getTN());
            System.out.println("fn :"+score.getFN());
            score.freezeScore(ext.getIteration());
            ext.getTripleScores().orderItems();
        } else {
            throw new RuntimeException("Could not add triple "+tripleStr);
        }

        return "Added triple";
    }

    void ChangeInstanceIdentifierAnnotation(InstanceIdentifier instanceIdentifier, String label, FrozenState frozenState) {
        List<LabelPattern> currentExistSameLabelPattern = new ArrayList<>();
        for (LabelPattern labelPattern : this.inMemoryAnnotationStorageNormal.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
            if (labelPattern.getLabel().equals(label)) {
                currentExistSameLabelPattern.add(labelPattern);
            }
        }
        for (LabelPattern labelPattern : currentExistSameLabelPattern) {
            inMemoryAnnotationStorageNormal.deleteAnnotationUnderLabelPattern(instanceIdentifier, labelPattern);
        }
        inMemoryAnnotationStorageNormal.addAnnotation(instanceIdentifier, new LabelPattern(label, frozenState));
    }
    @JettyMethod("/init/add_instance")
    public synchronized String FrozenInstance(@JettyArg("target") String target, @JettyArg("instance") String instance, @JettyArg("quality") String quality) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance,InstanceIdentifier.class);
        FrozenState frozenState;
        switch (quality){
            case "good":
                frozenState = FrozenState.FROZEN_GOOD;
                break;
            case "bad":
                frozenState = FrozenState.FROZEN_BAD;
                break;
            default:
                frozenState = FrozenState.NO_FROZEN;
        }
        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, frozenState);
        return "OK";
    }

    @JettyMethod("/init/remove_instance")
    public synchronized String UnFrozenInstance(@JettyArg("target") String target, @JettyArg("instance") String instance) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance,InstanceIdentifier.class);
        FrozenState frozenState = FrozenState.NO_FROZEN;
        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, frozenState);
        return "OK";
    }

    @JettyMethod("/init/mark_instance_from_other")
    public synchronized String MarkInstanceFromOther(@JettyArg("target") String target, @JettyArg("instance") String instance) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance,InstanceIdentifier.class);
//        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
        this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
        this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        return "OK";
    }

    @JettyMethod("/init/mark_seed_from_other")
    public synchronized String MarkSeedFromOther(@JettyArg("target") String target,@JettyArg("seed") String seedString) throws IOException{
        Seed seed = getSeed(seedString,target);
        for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForSeed(seed)){
//            ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
            this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
            this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        }
        return "OK";
    }

    @JettyMethod("/init/mark_pattern_from_other")
    public synchronized String MarkPatternFromOther(@JettyArg("target") String target, @JettyArg("pattern") String patternString) {
        LearnitPattern pattern = findPattern(mappingLookup.get("OTHER"),patternString);
        for(InstanceIdentifier instanceIdentifier:mappings.getInstancesForPattern(pattern)){
//            ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
            this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
            this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        }
        return "OK";
    }


    private Optional<SeedPatternPair> parseTripleJson(String tripleJson,String target) throws IOException{
        TargetAndScoreTables ext = checkGetExtractor(target);
        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        Map<String,String> json = mapper.readValue(tripleJson, HashMap.class);
        String language = json.get("language");
        String slot0 = json.get("slot0");
        String slot1 = json.get("slot1");
        Seed seed = Seed.from(language,slot0,slot1).withProperText(ext.getTarget());
        seed = getSeed(mapper.writeValueAsString(seed),target);
        if (seed == null){//Cannot create a triple with a seed that doesn't exist in mapping
            return Optional.absent();
        }
        String patternAsString = json.get("pattern");
        LearnitPattern learnitPattern = findPattern(mappingLookup.get(target),patternAsString);
        if (learnitPattern == null){//Cannot create a triple with a pattern that doesn't exist in mapping
            return Optional.absent();
        }
        PatternID pattern = PatternID.from(learnitPattern);
        return Optional.of(SeedPatternPair.create(seed,pattern));
    }

    /**
     * UNACCEPT A TRIPLE
     * -----------------------------------
     * Unsets the specified triple as known and good in the system
     *
     * @param target
     * @param tripleStr  A SeedPatternSimilarity json string of the form: {"language":"<language>","slot0":"word0","slot1":"word1","pattern":"patternString"}
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/remove_triple")
    public synchronized String removeTriple(@JettyArg("target") String target, @JettyArg("triple") String tripleStr) throws JsonParseException, JsonMappingException, IOException {
        TargetAndScoreTables ext = checkGetExtractor(target);
        // LearnitPattern pattern = findPattern(ext,target,patternStr);
        SeedPatternPair triple = parseTripleJson(tripleStr,target).orNull();
        if (triple != null && ext.getTripleScores().isKnownFrozen(triple)) {
            TripleScore score = ext.getTripleScores().getScore(triple);
            score.unfreeze();
            score.setConfidence(0.0);
            score.setPrecision(0.0);
            ext.getTripleScores().orderItems();
            return "success";
        } else {
            return "failure";
        }
    }

    private LearnitPattern findPattern(TargetAndScoreTables data, String pattern) {
        for (LearnitPattern p : data.getPatternScores().getFrozen()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }

        for (LearnitPattern p : data.getPatternScores().getNonFrozen()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }

        return null;
    }

    /**
     * PROPOSE PATTERNS
     * ------------------------------------------
     * Proposes a new set of patterns
     *
     * @param target
     * @return
     */
    @JettyMethod("/init/propose_patterns")
    public synchronized String addPatterns(@JettyArg("target") String target) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        addPatterns(extractor, mappingLookup.get(extractor.getTarget().getName()));
        return "success";
    }

    ////////////

    /**
     * UNACCEPT A PATTERN
     * -----------------------------------
     * Unsets the specified pattern as known and good in the system
     *
     * @param target
     * @param patternStr
     * @return
     */
    @JettyMethod("/init/remove_pattern")
    public synchronized String removePattern(@JettyArg("target") String target, @JettyArg("pattern") String patternStr) {
        TargetAndScoreTables ext = checkGetExtractor(target);
        // LearnitPattern pattern = findPattern(ext,target,patternStr);
        LearnitPattern pattern = findPattern(ext, patternStr);
        if (pattern != null && ext.getPatternScores().isKnownFrozen(pattern)) {
            PatternScore score = ext.getPatternScores().getScore(pattern);
            score.unfreeze();
            score.setConfidence(0.0);
            score.setPrecision(0.0);
            ext.getPatternScores().orderItems();
            calculatePatternScoreExtra(pattern, score, ext, this.mappingLookup.get(ext.getTarget().getName()));
            return "success";
        } else {
            throw new RuntimeException("Pattern is not here");
        }
    }

    @JettyMethod("/init/propose_patterns_new")
    public PatternScoreTable proposePatterns(@JettyArg("target") String target){
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = mappingLookup.get(ext.getTarget().getName());
        Set<Seed> seedsFrozen = ext.getSeedScores().getFrozen();
        Multiset<LearnitPattern> patternsForSeeds = info.getPatternsForSeeds(seedsFrozen);
        Set<LearnitPattern> patterns = patternsForSeeds.elementSet();

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedPatterns = 0;
        PatternScoreTable patternScoreTable = new PatternScoreTable();
        for(LearnitPattern pattern : patterns){
            if (pattern != null && !ext.getPatternScores().isKnownFrozen(pattern)) {
                numProposedPatterns++;
                patternScoreTable.addDefault(pattern);
                double instanceWeight = 0.0;
                for (InstanceIdentifier id : info.getInstancesForPattern(pattern)) {
                    instanceWeight += id.getConfidence();
                }

                PatternScore score = patternScoreTable.getScore(pattern);
                int frequency = info.getInstancesForPattern(pattern).size();

                score.setFrequency(frequency);
                score.setConfidenceDenominator(instanceWeight);
                score.setRecall(0.01);


                /*
                // testing scores for pattern sorting
                if(pattern.toIDString().contains("verb"))
                    score.setPrecision(0.9);
                else if(pattern.toIDString().contains("noun"))
                    score.setPrecision(0.8);
                else if(pattern.toIDString().trim().startsWith("{0}"))
                    score.setPrecision(0.7);
                */
                // test sorting function
                // score.setPrecision((double)frequency);
                calculatePatternScoreExtra(pattern, score, ext, info);
                //

                score.unfreeze();
                if (numProposedPatterns>=SIZE_LIMIT){
                    break;
                }
            }
        }



        // @hqiu: below code is just copied from addPatterns in order to make sure that we have the ability for restoring
        // Based on current design, you should NOT modify extractor directly!!!
        /*
        int NUM_PATTERNS_TO_PROPOSE = 100;
        PatternProposer proposer = new PatternProposer(extractor,false);
        proposer.setAmount(NUM_PATTERNS_TO_PROPOSE);
        proposer.runOnMappings(info);
        PatternPruner pruner = new PatternPruner(extractor);
        pruner.score(pruner.processMappings(info));
        extractor.getPatternScores().removeBadForHuman(true);
        extractor.getPatternScores().orderItems();
        */
        return patternScoreTable;
    }

    public void proposeFirstPatterns(String relation) {
        TargetAndScoreTables ex = checkGetExtractor(relation);
        Mappings info = mappingLookup.get(relation);
        addPatterns(ex,info);
    }

    private boolean hasKeyword(Iterable<String> words, Collection<String> keywords) {
        for (String w : words) {
            w = w.toLowerCase();
            for (String key : keywords) {
                if (key.endsWith("*")) {
                    if (w.startsWith(key.substring(0,key.length()-1))) return true;
                } else {
                    if (w.equals(key)) return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesInitializationPatternInLearnItFormat(LearnitPattern p, Collection<LearnitPattern> initPatterns) {
        for (LearnitPattern ip : initPatterns) {
            if (ip.matchesPattern(p)) return true;
        }
        return false;
    }

    private static boolean matchesInitializationPattern(LearnitPattern p, Collection<InitializationPattern> initPatterns) {
        for (InitializationPattern ip : initPatterns) {
            if (ip.matchesPattern(p)) return true;
        }
        return false;
    }

    private Collection<LearnitPattern> getFromInitial(Collection<InitializationPattern> initPatterns, Set<LearnitPattern> initPatternsInSexpFormat,
                                                      Mappings info, Target target) {
        ImmutableSet.Builder<LearnitPattern> builder = ImmutableSet.builder();
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.isProposable(target) && (matchesInitializationPattern(p,initPatterns)||initPatternsInSexpFormat.contains(p))) {
                builder.add(p);
            }
        }
        return builder.build();
    }

    public void proposeFromInitializationPatterns(String relation, Collection<InitializationPattern> initPatterns, Set<LearnitPattern> initPatternsInSexpFormat) {
        TargetAndScoreTables ex = checkGetExtractor(relation);
        Mappings info = mappingLookup.get(relation);
        Collection<LearnitPattern> fromInitial = getFromInitial(initPatterns, initPatternsInSexpFormat,
                info, ex.getTarget());
        addPatterns(ex, info, fromInitial);
    }

    public void addPatterns(TargetAndScoreTables extractor, Mappings info, Collection<LearnitPattern> initialPatterns) {
        PatternProposer proposer = new PatternProposer(extractor,false);

        PatternProposalInformation proposalInformation = proposer.processMappings(info);
        SimplifiedPatternScorer scorer = new SimplifiedPatternScorer(extractor,proposalInformation);

        Set<LearnitPattern> intersection = Sets.intersection(proposalInformation.getPatterns(), ImmutableSet.copyOf(initialPatterns));
        System.out.println("Proposing " + intersection.size() + " initial patterns");
        scorer.score(intersection, extractor.getPatternScores());
        for (LearnitPattern p : intersection) {
            extractor.getPatternScores().getScore(p).propose();
        }

        PatternPruner pruner = new PatternPruner(extractor);
        pruner.score(pruner.processMappings(info));
        extractor.getPatternScores().removeBadForHuman(false);
        extractor.getPatternScores().orderItems();
    }

    /**
     * RESCORE PATTERNS
     * --------------------------------------------
     * Rescores the patterns by the system pattern rescoring strategy
     *
     * @param target
     * @return
     */
    @JettyMethod("/init/rescore_patterns")
    public synchronized String rescorePatterns(@JettyArg("target") String target) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());
        PatternPruner pruner = new PatternPruner(extractor);
        pruner.score(pruner.processMappings(info));
        return "success";
    }

    /**
     * RESCORE TRIPLES
     * --------------------------------------------
     * Rescores the triples by the system triple rescoring strategy
     *
     * @param target
     * @return
     */
    @JettyMethod("/init/rescore_triples")
    public synchronized String rescoreTriples(@JettyArg("target") String target) {
        throw new UnsupportedOperationException("method currently not implemented");
//        TargetAndScoreTables extractor = checkGetExtractor(target);
//        Mappings info = mappingLookup.get(extractor.getTarget().getName());

//        TriplePruner pruner = new PatternPruner(extractor);
//        pruner.score(pruner.processMappings(info));
//        return "success";
    }



    /*------------------------------------------------------------------*
     *                                                                  *
     *                       SEED ROUTINES                              *
     *                                                                  *
     *------------------------------------------------------------------*/

    private Seed getSeed(String seedJson, String target) throws IOException {
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings m = mappingLookup.get(ext.getTarget().getName());

        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        Seed seed = mapper.readValue(seedJson, Seed.class).withProperText(ext.getTarget());

        for (Seed s : m.getAllSeeds().elementSet()) {
            if (seed.equals(s.withProperText(ext.getTarget())))
                return s;
        }
        return null;
    }

    private void freezeSeed(TargetAndScoreTables ext, Seed seed, String quality) {
        if (quality.equals("good")) {
            freezeSeed(ext,seed,1.0,0.9);
        } else if (quality.equals("bad")) {
            freezeSeed(ext,seed,1.0,0.1);
        } else {
            throw new RuntimeException("Unknown seed quality "+quality);
        }
    }

    public void addPatterns(TargetAndScoreTables ext, Mappings info) {
//    public void addPatterns(TargetAndScoreTables extractor, Mappings info) {
        // patterns will be first sorted by precison, then confidence in the UI

        Set<Seed> seedsFrozen = ext.getSeedScores().getFrozen();
        Multiset<LearnitPattern> patternsForSeeds = info.getPatternsForSeeds(seedsFrozen);
        Set<LearnitPattern> patterns = patternsForSeeds.elementSet();

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedPatterns = 0;
        for (LearnitPattern pattern : patterns) {

            if (pattern != null && !ext.getPatternScores().isKnownFrozen(pattern)) {
                numProposedPatterns++;
                ext.getPatternScores().addDefault(pattern);
                double instanceWeight = 0.0;
                for (InstanceIdentifier id : info.getInstancesForPattern(pattern)) {
                    instanceWeight += id.getConfidence();
                }

                PatternScore score = ext.getPatternScores().getScore(pattern);
                int frequency = info.getInstancesForPattern(pattern).size();

                score.setFrequency(frequency);
                score.setConfidenceDenominator(instanceWeight);
                score.setRecall(0.01);


                /*
                // testing scores for pattern sorting
                if(pattern.toIDString().contains("verb"))
                    score.setPrecision(0.9);
                else if(pattern.toIDString().contains("noun"))
                    score.setPrecision(0.8);
                else if(pattern.toIDString().trim().startsWith("{0}"))
                    score.setPrecision(0.7);
                */
                // test sorting function
                // score.setPrecision((double)frequency);
                calculatePatternScoreExtra(pattern, score, ext, info);
                //

                score.unfreeze();
                if (numProposedPatterns >= SIZE_LIMIT) {
                    break;
                }
            }
        }

        ext.getPatternScores().orderItems();

        /*
        int NUM_PATTERNS_TO_PROPOSE = 100;
        PatternProposer proposer = new PatternProposer(extractor,false);
        proposer.setAmount(NUM_PATTERNS_TO_PROPOSE);
        proposer.runOnMappings(info);
        PatternPruner pruner = new PatternPruner(extractor);
        pruner.score(pruner.processMappings(info));
        extractor.getPatternScores().removeBadForHuman(true);
        extractor.getPatternScores().orderItems();
        */
    }

    private void freezeSeed(TargetAndScoreTables ext, Seed seed, double confidence, double score) {
        if (ext.getSeedScores().isKnownFrozen(seed)) return;

        if (!ext.getSeedScores().hasScore(seed)) {
            ext.getSeedScores().addDefault(seed);
//            ext.getSeedScores().orderItems();
        }

        SeedScore sscore = ext.getSeedScores().getScore(seed);
        // sscore.setScore(0.9);
        sscore.setScore(1000000.0); // TODO: fixme
        sscore.setConfidence(confidence);
        sscore.freezeScore(ext.getIteration());
        calculateSeedScoreExtra(seed, sscore, ext, this.mappingLookup.get(ext.getTarget().getName()));
    }

//    /**
//     * AUTO-ACCEPT SEEDS
//     * ------------------------------
//     * Perform seed selection to get a new set of good seeds by the system's scoring
//     *
//     * @param target
//     * @param amount   the number to get
//     * @param seeds    the seeds to ban and not accept (because a human said so)
//     * @return
//     * @throws JsonParseException
//     * @throws JsonMappingException
//     * @throws IOException
//     */
//    @JettyMethod("/init/accept_seeds")
//    public synchronized String acceptSeeds(@JettyArg("target") String target,
//                                           @JettyArg("amount") String amount,
//                                           @JettyArg("bannedSeeds") String[] seeds) throws IOException {
//
//        Integer intAmount = Integer.parseInt(amount);
//        TargetAndScoreTables ext = checkGetExtractor(target);
//
//        SeedSelector selector = new SeedSelector(intAmount,true);
//        SeedScoreTable seedScoreTable = ext.getSeedScores();
//        selector.freezeScores(ext.getSeedScores());
//        for (String seedStr : seeds) {
//            if (seedStr.equals("null")) break;
//
//            Seed seed = getSeed(seedStr,target);
//            if (ext.getSeedScores().isKnownFrozen(seed)) {
//                SeedScore score = ext.getSeedScores().getScore(seed);
//                score.unfreeze();
//                score.setConfidence(0.0);
//                score.setScore(0.0);
//            }
//        }
//        ext.getSeedScores().orderItems();
//        SeedSimilarity.updateKMeans(ext);
//        return "success";
//    }

    /**
     * AUTO-ACCEPT SEEDS
     * ------------------------------
     * Perform seed selection to get a new set of good seeds by the system's scoring
     *
     * @param target
     * @param amount   the number to get
     * @param seeds    the seeds to ban and not accept (because a human said so)
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/accept_seeds")
    public synchronized String acceptSeeds(@JettyArg("target") String target,
                                           @JettyArg("amount") String amount,
                                           @JettyArg("bannedSeeds") String[] seeds) throws IOException {

        Integer intAmount = Integer.parseInt(amount);
        TargetAndScoreTables ext = checkGetExtractor(target);

        SeedAutoAcceptSelector selector = new SeedAutoAcceptSelector(5);
        selector.freezeScores(ext.getSeedScores());
        for (String seedStr : seeds) {
            if (seedStr.equals("null")) break;

            Seed seed = getSeed(seedStr,target);
            if (ext.getSeedScores().isKnownFrozen(seed)) {
                SeedScore score = ext.getSeedScores().getScore(seed);
                score.unfreeze();
                score.setConfidence(0.0);
                score.setScore(0.0);
                calculateSeedScoreExtra(seed, score, ext, this.mappingLookup.get(ext.getTarget().getName()));
            }
        }
        ext.getSeedScores().orderItems();
//        SeedSimilarity.updateKMeans(ext);
        return "success";
    }

    /**
     * AUTO-ACCEPT TRIPLES
     * ------------------------------
     * Perform triple selection to get a new set of good triples by the system's scoring
     *
     * @param target
     * @param amount   the number to get
     * @param triples    the triples to ban and not accept (because a human said so)
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @JettyMethod("/init/accept_triples")
    public synchronized String acceptTriples(@JettyArg("target") String target,
                                             @JettyArg("amount") String amount,
                                             @JettyArg("bannedTriples") String[] triples) {
        throw new UnsupportedOperationException("method currently not implemented");
//        Integer intAmount = Integer.parseInt(amount);
//        TargetAndScoreTables ext = checkGetExtractor(target);

//        TripleSelector selector = new TripleSelector(intAmount,true);
//        selector.freezeScores(ext.getTripleScores());
//        for (String tripleStr : triples) {
//            if (tripleStr.equals("null")) break;
//
//            SeedPatternPair triple = parseTripleJson(tripleStr,target);
//            if (ext.getTripleScores().isKnownFrozen(triple)) {
//                TripleScore score = ext.getTripleScores().getScore(triple);
//                score.unfreeze();
//                score.setConfidence(0.0);
//                score.setScore(0.0);
//            }
//        }
//        ext.getSeedScores().orderItems();
//        SeedSimilarity.updateKMeans(ext);
//        return "success";
    }

    /**
     * MANUAL ACCEPT SEEDS
     * ------------------------------------
     * Adds the given set of seeds to the extractor as known good seeds.
     *
     * @param target
     * @param seeds
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/add_seeds")
    public synchronized String addSeeds(@JettyArg("target") String target, @JettyArg("seeds") String[] seeds, @JettyArg("quality") String quality) throws IOException {
        int count = 0;
        TargetAndScoreTables ext = checkGetExtractor(target);
        for (String seedStr : seeds) {
            Seed seed = getSeed(seedStr,target);
            if (seed != null) {
                freezeSeed(ext,seed,quality);
                count++;
            }
        }
        ext.getSeedScores().orderItems();
        return "Added "+count+" seeds";
    }

    /**
     * UNACCEPT A SEED
     * -----------------------------------
     * Unsets the specified seed as known and good in the system
     *
     * @param target
     * @param seedStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/remove_seed")
    public synchronized String removeSeed(@JettyArg("target") String target, @JettyArg("seed") String seedStr) throws IOException {
        TargetAndScoreTables ext = checkGetExtractor(target);
        Seed seed = getSeed(seedStr,target);
        if (seed != null && ext.getSeedScores().isKnownFrozen(seed)) {
            SeedScore score = ext.getSeedScores().getScore(seed);
            score.unfreeze();
            score.setConfidence(0.0);
            score.setScore(0.0);
            ext.getSeedScores().orderItems();
            calculateSeedScoreExtra(seed, score, ext, this.mappingLookup.get(ext.getTarget().getName()));
        }
        return "success";
    }

    /**
     * PROPOSE SEEDS
     * ------------------------------------------
     * Proposes a new set of seeds
     *
     * @param target
     * @return
     */
    @JettyMethod("/init/propose_seeds")
    public synchronized String addSeeds(@JettyArg("target") String target) {
        System.out.println("Getting extractor for "+target);
        TargetAndScoreTables extractor = checkGetExtractor(target);
        System.out.println("Proposing seeds...");
        addSeeds(extractor, mappingLookup.get(extractor.getTarget().getName()));
        return "success";
    }


    @JettyMethod("/init/propose_seeds_new")
    public synchronized SeedScoreTable proposeSeeds(@JettyArg("target") String target){
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());
        SeedScoreTable seedScoreTable = new SeedScoreTable();

        Set<LearnitPattern> patternsFrozen = extractor.getPatternScores().getFrozen();
        Multiset<Seed> seedsForPatterns = info.getSeedsForPatterns(patternsFrozen);
        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;

        for(Seed seed : seedsForPatterns.elementSet()) {

            if(seed != null && !extractor.getSeedScores().isKnownFrozen(seed)) {
                numProposedSeeds++;
                seedScoreTable.addDefault(seed);
                double instanceWeight = 0.0;
                for (InstanceIdentifier id : info.getInstancesForSeed(seed)) {
                    instanceWeight += id.getConfidence();
                }

                SeedScore score = seedScoreTable.getScore(seed);
                int frequency = info.getInstancesForSeed(seed).size();

                score.setFrequency(frequency);
                score.setConfidenceDenominator(instanceWeight);
                // score.setScore(0.01);
                // score.setScore(1.0*frequency); // use frequency as score
                // score.setConfidence(1.0*frequency);
                // score.setConfidenceNumerator(1.0*frequency);
                // score.setConfidenceDenominator(1.0);

                /*
                // testing sorting function
                if(seed.toIDString().contains("bush"))
                    score.setScore(0.8);
                else if(seed.toIDString().contains("ballmer"))
                    score.setScore(0.7);
                else if(seed.toIDString().contains("白宫"))
                    score.setScore(0.9);
                    */
                // test sorting function
                // score.setScore((double)frequency);
                calculateSeedScoreExtra(seed, score, extractor, info);
                //
                score.unfreeze();
                if (numProposedSeeds>=SIZE_LIMIT){
                    break;
                }
            }
        }

        // @hqiu: below code is just copied from addSeeds in order to make sure that we have the ability for restoring
        // Based on current design, you should NOT modify extractor directly!!!

        /*
        SeedProposer proposer = new SeedProposer(extractor);
        proposer.setAmount(NUM_SEEDS_TO_PROPOSE);
        proposer.runOnMappings(info);
        SeedPruner pruner = new SeedPruner(extractor);
        SeedPruningInformation prunerInfo = pruner.processMappings(info);
        SeedScorer scorer = new SeedScorer(extractor, prunerInfo);
        scorer.score(prunerInfo.getSeeds(), extractor.getSeedScores());
        extractor.getSeedScores().removeBadForHuman(false);
        */

        return seedScoreTable;
    }

    private void addSeeds(TargetAndScoreTables extractor, Mappings info) {

        Set<LearnitPattern> patternsFrozen = extractor.getPatternScores().getFrozen();
        System.out.println("Getting seeds for all frozen patterns...");
        Multiset<Seed> seedsForPatterns = info.getSeedsForPatterns(patternsFrozen);
        System.out.println("\tfound "+seedsForPatterns.size()+" seeds...");
        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;
        for(Seed seed : seedsForPatterns.elementSet()) {

            if(seed != null && !extractor.getSeedScores().isKnownFrozen(seed)) {
                numProposedSeeds++;
                extractor.getSeedScores().addDefault(seed);
                double instanceWeight = 0.0;
                for (InstanceIdentifier id : info.getInstancesForSeed(seed)) {
                    instanceWeight += id.getConfidence();
                }

                SeedScore score = extractor.getSeedScores().getScore(seed);
                int frequency = info.getInstancesForSeed(seed).size();

                score.setFrequency(frequency);
                score.setConfidenceDenominator(instanceWeight);
                // score.setScore(0.01);
                // score.setScore(1.0*frequency); // use frequency as score
                // score.setConfidence(1.0*frequency);
                // score.setConfidenceNumerator(1.0*frequency);
                // score.setConfidenceDenominator(1.0);

                /*
                // testing sorting function
                if(seed.toIDString().contains("bush"))
                    score.setScore(0.8);
                else if(seed.toIDString().contains("ballmer"))
                    score.setScore(0.7);
                else if(seed.toIDString().contains("白宫"))
                    score.setScore(0.9);
                    */
                // test sorting function
                // score.setScore((double)frequency);
                calculateSeedScoreExtra(seed, score, extractor, info);
                //
                score.unfreeze();
                if (numProposedSeeds>=SIZE_LIMIT){
                    break;
                }
            }
        }
        extractor.getSeedScores().orderItems();
        /*
        SeedProposer proposer = new SeedProposer(extractor);
        proposer.setAmount(NUM_SEEDS_TO_PROPOSE);
        proposer.runOnMappings(info);
        SeedPruner pruner = new SeedPruner(extractor);
        SeedPruningInformation prunerInfo = pruner.processMappings(info);
        SeedScorer scorer = new SeedScorer(extractor, prunerInfo);
        scorer.score(prunerInfo.getSeeds(), extractor.getSeedScores());
        extractor.getSeedScores().removeBadForHuman(false);
        */
    }

    /**
     * RESCORE SEEDS
     * -----------------------------------------------
     * Rescores the seeds by the system seed rescoring strategy
     *
     * @param target
     * @return
     */
    @JettyMethod("/init/rescore_seeds")
    public synchronized String rescoreSeeds(@JettyArg("target") String target) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());
        SeedPruner pruner = new SeedPruner(extractor);
        pruner.score(pruner.processMappings(info));
        return "success";
    }

    /**
     * LOAD ADDITIONAL SEEDS
     * ----------------------------------------------
     * Grabs some additional seeds from seed files in a .additional directory
     *
     * @param target
     * @param strAmount
     * @return
     */
    @JettyMethod("/init/get_additional_seeds")
    public synchronized String getAdditionalSeeds(@JettyArg("target") String target, @JettyArg("amount") String strAmount) {
        return "function not supported, added 0 seeds";

        /*
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());

        int amount = Integer.parseInt(strAmount);
        List<Seed> seeds = this.additionalSeeds.get(target);

        int numTaken = 0;
        while (!seeds.isEmpty() && numTaken < amount) {
            Seed newSeed = seeds.remove(0);
            if (!extractor.getSeedScores().hasScore(newSeed)) {
                freezeSeed(extractor,newSeed,0.9,0.9);
                numTaken++;
            }
        }

        PatternProposer proposer = new PatternProposer(extractor);
        extractor.initializeSeedScores(proposer.processMappings(info));

        return "added "+numTaken+" seeds";
        */
    }

    /**
     * SEARCH SEEDS BY SLOT
     * -------------------------------------------
     * Searches for viable seeds with the given slots.
     * An empty slot denotes a wildcard.
     *
     * @param target
     * @param slot0
     * @param slot1
     * @return
     */
    @JettyMethod("/init/get_seeds_by_slots")
    public synchronized List<Seed> getSeedsBySlots(@JettyArg("target") String target, @JettyArg("slot0") String slot0, @JettyArg("slot1") String slot1,@JettyArg("amount") String amountStr) {
        int maximumResultLength = Integer.parseInt(amountStr);
        List<Seed> results = new ArrayList<Seed>();
        for (Seed seed : mappingLookup.get(target).getInstance2Seed().getAllSeeds().elementSet()) {
            if(slot1.trim().equals("NA")) {
                if(seed.getSlot(0).toString().contains(slot0))
                    results.add(seed);
            }
            else if (seed.getSlot(0).toString().contains(slot0) && seed.getSlot(1).toString()
                .contains(slot1)) {
                results.add(seed);
            }
        }

        //sort by frequency
        Collections.sort(results, new Comparator<Seed>() {

            @Override
            public int compare(Seed arg0, Seed arg1) {
                return mappings.getInstance2Seed().getInstances(arg1).size() -
                        mappings.getInstance2Seed().getInstances(arg0).size();
            }

        });

        List<Seed> filteredResultList = new ArrayList<>();
        for(int i = 0;i < Math.min(results.size(),maximumResultLength);++i){
            filteredResultList.add(results.get(i));
        }

        return filteredResultList;
    }

    /*
     * Similarity related handlers
     */

    @JettyMethod("/init/similar_seeds")
    public synchronized SeedScoreTable getSimilarSeedsNew(@JettyArg("target") String target) throws Exception{
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());
        SeedScoreTable seedScoreTable = new SeedScoreTable();

        Set<Seed> frozenGoodSeeds = new HashSet<>();
        Set<Seed> frozenSeeds = new HashSet<>();
        for(AbstractScoreTable.ObjectWithScore<Seed,SeedScore> objectWithScore: extractor.getSeedScores().getObjectsWithScores()){
            SeedScore seedScore = objectWithScore.getScore();
            if(seedScore.isFrozen() && seedScore.isGood()){
                frozenGoodSeeds.add(objectWithScore.getObject());
            }
            if(seedScore.isFrozen()){
                frozenSeeds.add(objectWithScore.getObject());
            }
        }

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;
        double threshold = 0.2;

        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;

        List<Optional<? extends ObservationSimilarity>> seedSimilarityRows = new ArrayList<>();

        for(Seed seed:frozenGoodSeeds){
            Optional<? extends ObservationSimilarity> seedSimilarity = observationSimilarityModule.getSeedSimilarity(seed);
            seedSimilarityRows.add(seedSimilarity);
        }

        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarSeeds =
                ObservationSimilarity.mergeMultipleSimilarities(seedSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(frozenSeeds));

        for(Pair<LearnItObservation,Double> seedObj : similarSeeds){
            Seed seed = (Seed)seedObj.key;
            numProposedSeeds++;
            seedScoreTable.addDefault(seed);
            SeedScore score = seedScoreTable.getScore(seed);
            int frequency = info.getInstancesForSeed(seed).size();
            score.setFrequency(frequency);
            calculateSeedScoreExtra(seed, score, extractor, info);
            score.setScore(seedObj.value);
            score.unfreeze();
        }
        return seedScoreTable;
    }


    @JettyMethod("/similarity/seeds")
    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> getSimilarSeeds(
            @JettyArg("target") String target,
            @JettyArg("language") String language,
            @JettyArg("seeds") String seedsAsJson,
            //@JettyArg("seeds") String[] seeds,
            @JettyArg("threshold") String threshold,
            @JettyArg("cutoff") String cutOff
    ) throws IOException{
//        throw new NotImplementedException();
        TargetAndScoreTables ext = checkGetExtractor(target);
        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;
        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        Map<String,List<List<String>>> json = mapper.readValue(seedsAsJson, HashMap.class);
        List<Optional<? extends ObservationSimilarity>> seedSimilarityRows = new ArrayList<>();
        System.out.println("Received "+json.get("seeds").size()+" seeds for similarity calculation...");
        Set<Seed> seedsToFilter = new HashSet<>();
        for(List<String> seedDesc : json.get("seeds")){
            String seedStr = seedDesc.get(0);
//            System.out.println("SeedStr: "+seedStr);
            Seed seed = getSeed(seedStr,target);
//            Seed seed = Seed.from(language,seedSlots.get(0),seedSlots.get(1)).withProperText(TargetFactory.fromString(target));
            Optional<? extends ObservationSimilarity> seedSimilarity = observationSimilarityModule.getSeedSimilarity(seed);
            seedSimilarityRows.add(seedSimilarity);
            seedsToFilter.add(seed);
        }
        //Filter out any seeds that are already frozen
        seedsToFilter.addAll(ext.getSeedScores().getFrozen());
        System.out.println("\nMerging similarity rows...");
        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarSeeds =
                ObservationSimilarity.mergeMultipleSimilarities(seedSimilarityRows,
                        Double.parseDouble(threshold),
                        cutOff.isEmpty()? Optional.absent(): Optional.of(Integer.parseInt(cutOff)),
                        Optional.of(seedsToFilter));
        System.out.println("Returning a list of "+similarSeeds.size()+" similar seeds...");
        return similarSeeds;
    }

    @JettyMethod("/init/similar_patterns")
    public synchronized PatternScoreTable getSimilarPatternsNew(@JettyArg("target") String target) throws Exception{
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = mappingLookup.get(extractor.getTarget().getName());
        PatternScoreTable patternScoreTable = new PatternScoreTable();

        Set<LearnitPattern> frozenGoodLearnitPatterns = new HashSet<>();
        Set<LearnitPattern> frozenLearnitPatterns = new HashSet<>();
        for(AbstractScoreTable.ObjectWithScore<LearnitPattern,PatternScore> objectWithScore: extractor.getPatternScores().getObjectsWithScores()){
            PatternScore patternScore = objectWithScore.getScore();
            if(patternScore.isFrozen() && patternScore.isGood()){
                frozenGoodLearnitPatterns.add(objectWithScore.getObject());
            }
            if(patternScore.isFrozen()){
                frozenLearnitPatterns.add(objectWithScore.getObject());
            }
        }

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return

        double threshold = 0.2;

        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;

        List<Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();

        for(LearnitPattern learnitPattern:frozenGoodLearnitPatterns){
            Optional<? extends ObservationSimilarity> learnitPatternSimilarity = observationSimilarityModule.getPatternSimilarity(PatternID.from(learnitPattern));
            patternSimilarityRows.add(learnitPatternSimilarity);
        }

        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarLearnitPatterns =
                ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(frozenLearnitPatterns));

        for(Pair<LearnItObservation,Double> patternObj : similarLearnitPatterns){
            LearnitPattern learnitPattern = findPattern(this.mappings, patternObj.key.toIDString());
            if (learnitPattern == null) {
                System.out.println("We should find something. This is a bug.");
                continue;
            }
            patternScoreTable.addDefault(learnitPattern);
            PatternScore score = patternScoreTable.getScore(learnitPattern);
            int frequency = info.getInstancesForPattern(learnitPattern).size();
            score.setFrequency(frequency);

            score.setPrecision(patternObj.value);
            calculatePatternScoreExtra(learnitPattern, score, extractor, info);
            score.unfreeze();
        }
        return patternScoreTable;
    }

    @JettyMethod("/similarity/patterns")
    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> getSimilarPatterns(
            @JettyArg("target") String target,
            @JettyArg("patterns") String patternsAsJson,
            @JettyArg("threshold") String threshold,
            @JettyArg("cutoff") String cutOff
    ) throws IOException{
//        throw new NotImplementedException();
        TargetAndScoreTables ext = checkGetExtractor(target);
        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;
        Map<String,List<String>> json = mapper.readValue(patternsAsJson, HashMap.class);
        List<com.google.common.base.Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();
        System.out.println("Received "+json.get("patterns").size()+" patterns for similarity calculation...");
        Set<PatternID> patternsToFilter = new HashSet<>();
        for(String patternStr : json.get("patterns")){
            LearnitPattern pattern = findPattern(mappingLookup.get(target),patternStr);
            if (pattern == null){
                continue;
            }
            Optional<? extends ObservationSimilarity> patternSimilarity =
                    observationSimilarityModule.getPatternSimilarity(PatternID.from(pattern));
            patternSimilarityRows.add(patternSimilarity);
            patternsToFilter.add(PatternID.from(pattern));
        }
        //Filter out any patterns that are already frozen
        patternsToFilter.addAll(ext.getPatternScores().getFrozen().stream().map(
                (LearnitPattern p)->PatternID.from(p)).collect(Collectors.toSet()));
        System.out.println("\nMerging similarity rows...");
        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarPatterns =
                ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                Double.parseDouble(threshold),
                cutOff.isEmpty()?Optional.absent():Optional.of(Integer.parseInt(cutOff)),
                Optional.of(patternsToFilter));

        System.out.println("Returning a list of "+similarPatterns.size()+" similar patterns...");
        return similarPatterns;
    }

    @JettyMethod("/similarity/triples")
    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> getSimilarSeedPatternPairs(
            @JettyArg("target") String target,
            @JettyArg("language") String language,
            @JettyArg("triples") String triplesAsJson,
            @JettyArg("threshold") String threshold,
            @JettyArg("cutoff") String cutOff
    ) {
        throw new NotImplementedException();
//        TargetAndScoreTables ext = checkGetExtractor(target);
//        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;
//        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
//        Map<String,List<Map<String,String>>> json = mapper.readValue(triplesAsJson, HashMap.class);
//        List<com.google.common.base.Optional<? extends ObservationSimilarity>> tripleSimilarityRows = new ArrayList<>();
//        System.out.println("Received "+json.get("triples").size()+" triples for similarity calculation...");
//        Set<SeedPatternPair> triplesToFilter = new HashSet<>();
//        for(Map<String,String> tripleAsMap : json.get("triples")){
//            String slot0 = tripleAsMap.get("slot0");
//            String slot1 = tripleAsMap.get("slot1");
//            // String language = tripleAsMap.get("language");
//            Seed seed = Seed.from(language,slot0,slot1).withProperText(ext.getTarget());
//            seed = getSeed(mapper.writeValueAsString(seed),target);
//            String patternAsString = tripleAsMap.get("pattern");
//            LearnitPattern pattern = findPattern(mappingLookup.get(target),patternAsString);
//            if (seed==null || pattern == null){
//                continue;
//            }
//            PatternID patternID = PatternID.from(pattern);
//            SeedPatternPair triple = SeedPatternPair.create(seed,patternID);
//            com.google.common.base.Optional<? extends ObservationSimilarity> tripleSimilarity =
//                    observationSimilarityModule.getSeedPatternPairSimilarity(triple);
//            if(tripleSimilarity.isPresent()&&!tripleSimilarity.get().similarObservations().isEmpty()){
//                System.out.println("\tGot a hit for the pair ("+slot0+", "+slot1+") "+patternAsString);
//            }
//            tripleSimilarityRows.add(tripleSimilarity);
//            triplesToFilter.add(triple);
//        }
//        //Filter out any triples that are already frozen
//        triplesToFilter.addAll(extractors.get(target).getTripleScores().getFrozen());
//        System.out.println("\nMerging similarity rows...");
//        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarTriples = ObservationSimilarity.mergeMultipleSimilarities(tripleSimilarityRows,
//                Double.parseDouble(threshold),cutOff.isEmpty()? com.google.common.base.Optional.absent(): com.google.common.base.Optional.of(Integer.parseInt(cutOff)),Optional.of(triplesToFilter));
//        System.out.println("Returning a list of "+similarTriples.size()+" similar triples...");
//        return similarTriples;
    }



    @JettyMethod("/ontology/current_tree")
    public synchronized Map<String, BBNInternalOntology.BBNInternalOntologyNode> getOntologyTree() {
        return this.ontologyMap;
    }

    @JettyMethod("/ontology/add_target")
    public synchronized String addTarget(@JettyArg("parentNodeId") String parentNodeId, @JettyArg("id") String id, @JettyArg("description") String description, @JettyArg("isUnaryTarget") String isUnaryStr, @JettyArg("isEventTarget") String isEventStr) throws Exception {
        boolean isUnary = Boolean.parseBoolean(isUnaryStr);
        boolean isEvent = Boolean.parseBoolean(isEventStr);
        if (!isEvent) {
            throw new com.bbn.bue.common.exceptions.NotImplementedException();
        }
        BBNInternalOntology.BBNInternalOntologyNode rootNode = null;
        if (isEvent) {
            if (isUnary) {
                rootNode = this.ontologyMap.get("unaryEvent");
            } else {
                rootNode = this.ontologyMap.get("binaryEvent");
            }
        } else {
            if (isUnary) {
                rootNode = this.ontologyMap.get("unaryEntity");
            } else {
                rootNode = this.ontologyMap.get("binaryEntity");
            }
        }

        Map<String, BBNInternalOntology.BBNInternalOntologyNode> nodeIdToNodeMap = rootNode.getPropToNodeMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("_id"));

        BBNInternalOntology.BBNInternalOntologyNode parent = nodeIdToNodeMap.get(parentNodeId);
        Set<String> existingNodeName = new HashSet<>();
        for (String nodeId : nodeIdToNodeMap.keySet()) {
            BBNInternalOntology.BBNInternalOntologyNode node = nodeIdToNodeMap.get(nodeId);
            existingNodeName.add(node.originalKey);
        }
        if (!org.apache.commons.lang3.StringUtils.isAlphanumeric(id.replace("-", "")) || nodeIdToNodeMap.containsKey(id.toLowerCase() + "_001") || existingNodeName.contains(id)) {
            throw new ValidationException("This target id is not acceptable");
        }
        BBNInternalOntology.BBNInternalOntologyNode newNode = new BBNInternalOntology.BBNInternalOntologyNode();
        newNode._id = id.toLowerCase()+"_001";
        newNode.originalKey = id;
        newNode._description = description;
        newNode.parent = parent;
        parent.children.add(newNode);
        return "OK";
    }

}
