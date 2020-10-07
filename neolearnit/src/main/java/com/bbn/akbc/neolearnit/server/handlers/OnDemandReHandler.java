package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.*;
import com.bbn.akbc.neolearnit.common.Annotation.FrozenState;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.AtomicMentionConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeOrValueConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MinEntityLevelConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.bbn.akbc.neolearnit.labelers.EERGraphLabeler;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilterWithCache;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.*;
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
import com.bbn.akbc.neolearnit.serializers.binary_event.EERGraphObserver;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.akbc.neolearnit.util.ExternalDictionary;
import com.bbn.akbc.neolearnit.util.TextNormalizer;
import com.bbn.akbc.utility.Pair;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.validators.ValidationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

// import SubsamplingFilter;

public class OnDemandReHandler extends SimpleJSONHandler {

    private final String targetPathDir;
    private final String instanceIdentifierAnnotationFileNormalPath;
    private final String instanceIdentifierAnnotationFileOtherPath;
    private final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageNormal;
    private final Annotation.InMemoryAnnotationStorage inMemoryAnnotationStorageOther;
    private final ExternalDictionary externalDictionary;
    private final Map<String, Epoch.MappingsEntryUnderEpoch> targetNameToMappingsEntrymap;
    private final Map<String, Domain.ExtractorEntry> targetNameToDomainEntryMap;
    private final Map<String, Epoch.MappingsEntryUnderEpoch> originalEpochEntries;
    private final Map<String, BBNInternalOntology.BBNInternalOntologyNode> targetNameToOntologyNode;
    private int workingEpoch;
    private Map<String, BBNInternalOntology.BBNInternalOntologyNode> ontologyMap;

    public OnDemandReHandler(String extractorDir) throws Exception {
        this.targetPathDir = extractorDir;
        this.ontologyMap = new HashMap<>();
        this.targetNameToDomainEntryMap = new HashMap<>();
        this.targetNameToMappingsEntrymap = new HashMap<>();
        this.originalEpochEntries = new HashMap<>();
        this.targetNameToOntologyNode = new HashMap<>();
        //true by default
        boolean honorSlotOrderingForSeedSimilarity =
                !LearnItConfig.defined("honor_slot_ordering_for_seed_extraction") ||
                        LearnItConfig.optionalParamTrue("honor_slot_ordering_for_seed_extraction");

        this.instanceIdentifierAnnotationFileNormalPath = Domain.getHumanLabeledMappingsMainPath();
        this.instanceIdentifierAnnotationFileOtherPath = Domain.getHumanLabeledMappingsOtherPath();
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
        File dir = new File(this.targetPathDir);

        // write top instances if hasn't been written
        // writeTopInstances(outputDir);


        for (String ontologyName : Domain.getOntologyNameToPathMap().keySet()) {
            final File ontologyFile = new File(Domain.getOntologyNameToPathMap().get(ontologyName));
            BBNInternalOntology.BBNInternalOntologyNode ontologyRoot = BBNInternalOntology.BBNInternalOntologyNode.fromInternalOntologyFile(ontologyFile);
            this.ontologyMap.put(ontologyName, ontologyRoot);
            Map<String, BBNInternalOntology.BBNInternalOntologyNode> nodeIdToNodeMap = ontologyRoot.getPropToNodeMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));
            this.targetNameToOntologyNode.putAll(nodeIdToNodeMap);
        }
        // Only load extractors that in the ontology

        for (File file : dir.listFiles()) {
            String extractorName = file.getName().replace(".json", "");
            if (this.targetNameToOntologyNode.containsKey(extractorName)) {
                TargetAndScoreTables ex = TargetAndScoreTables.deserialize(file);
                BBNInternalOntology.BBNInternalOntologyNode ontologyNode = this.targetNameToOntologyNode.get(extractorName);
                Domain.ExtractorEntry extractorEntry = new Domain.ExtractorEntry(ex, ontologyNode);
                targetNameToDomainEntryMap.put(ex.getTarget().getName(), extractorEntry);
            }
        }


        if (LearnItConfig.defined("external_dictionary_path")) {
            this.externalDictionary = new ExternalDictionary(LearnItConfig.get("external_dictionary_path"));
            try {
                this.externalDictionary.readExternalDictionary();
            } catch (Exception e) {

            }
        } else {
            this.externalDictionary = new ExternalDictionary("");
        }
        if (new File(Domain.getUIRuntimePath() + File.separator + "eer_graph.lock").exists()) {
            new File(Domain.getUIRuntimePath() + File.separator + "eer_graph.lock").delete();
        }
        new File(Domain.getUIRuntimePath()).mkdirs();
        reloadBackendData();
    }

    private static String getNowDateString() {
        Date date = new Date();
        String timeString = (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date))
                .replace("-", "");
        return timeString;
    }

    public static String getPatternLanguage(LearnitPattern p) {
        if (p instanceof PropPattern) {
            PropPattern propPattern = (PropPattern) p;
            return propPattern.getLanguage();
        } else if (p instanceof BetweenSlotsPattern) {
            BetweenSlotsPattern betweenSlotsPattern = (BetweenSlotsPattern) p;
            return betweenSlotsPattern.getLanguage();
        } else
            return "N/A";
    }

    private static synchronized Optional<List<String>> checkForSymmetricPatterns(TargetAndScoreTables ex) {
        Target target = ex.getTarget();
        if (target.isSymmetric()) {
            return Optional.absent();
        }
        List<String> patternsInError = new ArrayList<>();
        String targetName = target.getName();
        Set<String> slotAgnosticPatterns = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> obj : ex.getPatternScores().getObjectsWithScores()) {
            if (!obj.getScore().isFrozen() || (obj.getScore().isFrozen() && !obj.getScore().isGood())) {
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
        return patternsInError.isEmpty() ? Optional.absent() : Optional.of(patternsInError);
    }

    public static void DFSGetAllPropPatternOutOfComboPattern(ComboPattern root, Set<PropPattern> container) {
        for (LearnitPattern learnitPattern : root.getPatterns()) {
            if (learnitPattern instanceof ComboPattern) {
                DFSGetAllPropPatternOutOfComboPattern((ComboPattern) learnitPattern, container);
            } else if (learnitPattern instanceof PropPattern) {
                container.add((PropPattern) learnitPattern);
            }
        }
    }

    public static Set<Symbol> getRealLexicalItems(Set<Symbol> lexicalItems) {
        Set<Symbol> realLexicalItems = new HashSet<Symbol>();
        for (Symbol s : lexicalItems) {
            String str = s.toString();
            str = str.replace("[n]", "").replace("[v]", "").replace("[m]", "")
                    .replace("[0]", "").replace("[1]", "");
            if (str.endsWith("]")) {
                String entity_type = str.substring(str.indexOf("[") + 1, str.indexOf("]"));
                if (entity_type.length() > 1)
                    realLexicalItems.add(Symbol.from(entity_type));
                str = str.substring(0, str.indexOf("["));
            }
            if (!str.isEmpty()) {
                realLexicalItems.add(Symbol.from(str));
            }
        }

        return realLexicalItems;
    }

    public static Set<LearnitPattern> fullTextSearchKeyToLearnitPattern(Mappings mappings, ParsedQuery parsedQuery) {
        Set<Symbol> orWords = parsedQuery.getOrWordsSet();
        Set<Symbol> andWords = parsedQuery.getAndWordsSet();
        Set<Symbol> orPreds = parsedQuery.getOrPreds();

        System.out.println("[fullTextSearchKeyToLearnitPattern] query:\torWords: " + orWords + "\tandWords: " + andWords + "\torPreds: " + orPreds);

        Set<LearnitPattern> allPossibleSet = new HashSet<>();
        for (LearnitPattern learnitPattern : mappings.getAllPatterns().elementSet()) {
            // System.out.println("[fullTextSearchKeyToLearnitPattern]:\t" + learnitPattern.toPrettyString() + "\t" + learnitPattern.toString() + "\t" + learnitPattern.getLexicalItems() + "\t" + getRealLexicalItems(learnitPattern.getLexicalItems()));
            Set<Symbol> realLexicalItems = getRealLexicalItems(learnitPattern.getLexicalItems());
            if (Sets.intersection(TextNormalizer.normalizeASet(realLexicalItems), TextNormalizer.normalizeASet(orWords)).size() > 0) {
                allPossibleSet.add(learnitPattern);
            }
        }
        if (allPossibleSet.isEmpty() && orWords.size() < 1) {
            allPossibleSet.addAll(mappings.getAllPatterns().elementSet());
        }

        Set<LearnitPattern> allPossibleSetFilterByPred = new HashSet<>();
        if (orPreds.size() < 1) allPossibleSetFilterByPred.addAll(allPossibleSet);
        else {
            Set<PropPattern> posslblePropPattern = new HashSet<>();
//            for (LearnitPattern learnitPattern : allPossibleSet) {
//                if (learnitPattern instanceof ComboPattern) {
//                    DFSGetAllPropPatternOutOfComboPattern((ComboPattern) learnitPattern, posslblePropPattern);
//                } else if (learnitPattern instanceof PropPattern) {
//                    posslblePropPattern.add((PropPattern) learnitPattern);
//                }
//            }
//            for (PropPattern propPattern : posslblePropPattern) {
//                Set<Symbol> allPreds = new HashSet<>();
//                for (Set<Symbol> s : propPattern.getAllPredicates()) {
//                    allPreds.addAll(s);
//                }
//                Set<Symbol> allPredsNormalized = TextNormalizer.normalizeASet(allPreds);
//                for (Symbol orPred : TextNormalizer.normalizeASet(orPreds)) {
//                    if (allPredsNormalized.contains(orPred)) {
//                        allPossibleSetFilterByPred.add(propPattern);
//                    }
//                }
//            }
            for (LearnitPattern learnitPattern : allPossibleSet) {
                if (learnitPattern instanceof SerifPattern) {
                    Set<Symbol> possibleAnchors = new HashSet<>();
                    for (Symbol token : learnitPattern.getLexicalItems()) {
                        if (token.toString().endsWith("[0]") || token.toString().endsWith("[1]")) {
                            possibleAnchors.add(Symbol.from(token.toString().substring(0, token.toString().length() - 3)));
                        }
                    }
                    Set<Symbol> allPredsNormalized = TextNormalizer.normalizeASet(possibleAnchors);
                    for (Symbol orPred : TextNormalizer.normalizeASet(orPreds)) {
                        if (allPredsNormalized.contains(orPred)) {
                            allPossibleSetFilterByPred.add(learnitPattern);
                        }
                    }
                }
            }
        }


        if (andWords.size() < 1) return allPossibleSetFilterByPred;
        else {
            Set<LearnitPattern> retSet = new HashSet<>();
            for (LearnitPattern learnitPattern : allPossibleSetFilterByPred) {
                Set<Symbol> realLexicalItems = getRealLexicalItems(learnitPattern.getLexicalItems());
                if (TextNormalizer.normalizeASet(realLexicalItems).containsAll(TextNormalizer.normalizeASet(andWords))) {
                    retSet.add(learnitPattern);
                }
            }
            return retSet;
        }

    }

    public static Set<InstanceIdentifier> fullTextSearchKeyToInstanceIdentifier(Mappings mappings, ParsedQuery parsedQuery) {
        Set<Symbol> orWords = parsedQuery.getOrWordsSet();
        Set<Symbol> andWords = parsedQuery.getAndWordsSet();

        Set<InstanceIdentifier> allPossibleSet = new HashSet<>();
        for (Symbol word : orWords) {
            allPossibleSet.addAll(mappings.getInstancesForPattern(new SSUnigram(word)));

        }
        if (allPossibleSet.isEmpty() && orWords.size() < 1) {
            allPossibleSet.addAll(mappings.getPatternInstances());
        }
        for (Symbol word : andWords) {
            allPossibleSet = Sets.intersection(allPossibleSet, new HashSet<>(mappings.getInstancesForPattern(new SSUnigram(word))));
        }
        return allPossibleSet;
    }

    private static void calculatePatternScoreExtra(LearnitPattern learnitPattern, PatternScore patternScore, TargetAndScoreTables targetAndScoreTables, Mappings mappings) {
        Set<Seed> seedMatched = mappings.getSeedsForPattern(learnitPattern).elementSet();
        patternScore.setPatternPrecision(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
        patternScore.setPatternWeightedPrecision(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
        patternScore.setPatternFrequency(targetAndScoreTables.getSeedScores().getFrozen(), seedMatched);
    }

    private static void calculateSeedScoreExtra(Seed seed, SeedScore seedScore, TargetAndScoreTables targetAndScoreTables, Mappings mappings) {
        Set<LearnitPattern> patternsMatched = mappings.getPatternsForSeed(seed).elementSet();
        seedScore.setSeedFrequency(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
        seedScore.setSeedWeightedPrecision(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
        seedScore.setSeedPrecision(targetAndScoreTables.getPatternScores().getFrozen(), patternsMatched);
    }

    public static int getNumberOfPages(List<Pair<InstanceIdentifier, String>> instsWithLabel, int instancePerPage) {
        if (instsWithLabel.size() < 1 || instancePerPage < 1) return 0;
        else {
            return 1 + (instsWithLabel.size() / instancePerPage);
        }
    }

    public static List<Pair<InstanceIdentifier, String>> getPagination(List<Pair<InstanceIdentifier, String>> instsWithLabel, int instancePerPage, int currentPage) {
        List<Pair<InstanceIdentifier, String>> ret = new ArrayList<>();
        for (int currentPageIdx = currentPage * instancePerPage; currentPageIdx < Math.min((currentPage + 1) * instancePerPage, instsWithLabel.size()); ++currentPageIdx) {
            ret.add(instsWithLabel.get(currentPageIdx));
        }
        return ret;
    }

    public static void preLoadInstanceIdentifier(List<Pair<InstanceIdentifier, String>> instsWithLabel) throws IOException, ExecutionException, InterruptedException {
        Set<InstanceIdentifier> pendingSet = new HashSet<>();
        for (Pair<InstanceIdentifier, String> instanceIdentifierStringPair : instsWithLabel) {
            pendingSet.add(instanceIdentifierStringPair.key);
        }
        InstanceIdentifier.preLoadDocThoery(pendingSet);
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

    public static String getPatternKeywordInSimilarity(LearnitPattern learnitPattern) {
        if (learnitPattern instanceof SerifPattern) {
            SerifPattern serifPattern = (SerifPattern) learnitPattern;
            String highlightedToken = "";
            for (String s : serifPattern.toPrettyString().split(" ")) {
                if (s.startsWith("[v]") || s.startsWith("[n]") || s.startsWith("[m]") || s.endsWith("[0]") || s.endsWith("[1]")) {
                    highlightedToken = highlightedToken + " " + s.replace("[v]", "").replace("[n]", "").replace("[m]", "").replace("[0]", "").replace("[1]", "");
                }
            }
            if (highlightedToken.length() > 0) {
                return highlightedToken;
            } else {
                return learnitPattern.toPrettyString();
            }
        } else {
            return learnitPattern.toIDString();
        }
    }

    public void reloadBackendData() throws IOException {
        int currentEpoch = Epoch.getCurrentEpoch();
        System.out.println("Loading data at epoch " + currentEpoch);
        InstanceIdentifier.clearDocTheoryCache();
        this.targetNameToMappingsEntrymap.clear();
        this.originalEpochEntries.clear();
        int hitMappingsTimes = 0;
        for (String ontologyTargetName : this.ontologyMap.keySet()) {
            File mappingsPath = Epoch.getMappingsPath(ontologyTargetName, currentEpoch);
            String originalEpochKey = Domain.getTargetNameToOntologyRootName(Domain.getConvertedTargetName(ontologyTargetName));
            if (mappingsPath.exists()) {
                hitMappingsTimes++;
                Mappings mappings = Mappings.deserialize(mappingsPath, true);
                TargetFilterWithCache targetFilterWithCache = new TargetFilterWithCache(false);
                ObservationSimilarityModule observationSimilarityModule = ObservationSimilarityModule.create(mappings,Epoch.getSimilarityPath(ontologyTargetName, currentEpoch).toString());
                this.originalEpochEntries.put(originalEpochKey, new Epoch.MappingsEntryUnderEpoch(mappings, currentEpoch, targetFilterWithCache, observationSimilarityModule));
            } else {
                TargetFilterWithCache targetFilterWithCache = new TargetFilterWithCache(false);
                MapStorage<InstanceIdentifier, Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier, Seed>().build();
                MapStorage.Builder<InstanceIdentifier, LearnitPattern> instance2PatternTableBuilder = new HashMapStorage.Builder<>();
                Mappings emptyMappings = new Mappings(instance2Seed, instance2PatternTableBuilder.build());
                ObservationSimilarityModule observationSimilarityModule = ObservationSimilarityModule.create(emptyMappings,Epoch.getSimilarityPath(ontologyTargetName, currentEpoch).toString());
                this.originalEpochEntries.put(originalEpochKey, new Epoch.MappingsEntryUnderEpoch(emptyMappings, currentEpoch, targetFilterWithCache, observationSimilarityModule));
            }
        }
        if (hitMappingsTimes < 1) {
            System.err.println("We cannot get any mappings, have to exit");
            System.exit(1);
        }
        for (Domain.ExtractorEntry extractorEntry : this.targetNameToDomainEntryMap.values()) {
            BBNInternalOntology.BBNInternalOntologyNode rootNode = extractorEntry.getOntologyNode().getRoot();
            Epoch.MappingsEntryUnderEpoch mappingsEntryUnderEpoch = this.originalEpochEntries.get(rootNode.originalKey);
            TargetFilterWithCache targetFilterWithCache = mappingsEntryUnderEpoch.getTargetFilterWithCache();
            Mappings mappings = mappingsEntryUnderEpoch.getMappings();
            targetFilterWithCache.setFocusTarget(extractorEntry.getTargetAndScoreTables().getTarget());
            Mappings filteredMappings = targetFilterWithCache.makeFiltered(mappings);
            this.targetNameToMappingsEntrymap.put(extractorEntry.getTargetAndScoreTables().getTarget().getName(), new Epoch.MappingsEntryUnderEpoch(filteredMappings, currentEpoch, targetFilterWithCache, mappingsEntryUnderEpoch.getObservationSimilarityModule()));
        }
        Epoch.setFocusEpoch(currentEpoch);
    }


    /*------------------------------------------------------------------*
     *                                                                  *
     *                       INSTANCE ROUTINES                          *
     *                                                                  *
     *------------------------------------------------------------------*/

    @JettyMethod("/init/get_targets")
    public synchronized List<Target> getTargets() {
        List<Target> result = new ArrayList<Target>();
        for (String key : this.targetNameToDomainEntryMap.keySet()) {
            result.add(this.targetNameToDomainEntryMap.get(key).getTargetAndScoreTables().getTarget());
        }
        return result;
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
    public synchronized List<LearnitPattern> getPatternsByKeyword(@JettyArg("target") final String target, @JettyArg("keyword") String keyword, @JettyArg("amount") String amountStr, @JettyArg("fullTextSearchKey") String fullTextSearchKey, @JettyArg("sortingMethod") String sortingMethod) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Set<LearnitPattern> results = new HashSet<LearnitPattern>();

        int maximumResultLength = Integer.parseInt(amountStr);

        // search by slot0 or slot1 strings, keywords must be in the form #keyword#, e.g., #Bill Clinton#

        Set<Symbol> searchKeyInToken = new HashSet<>();
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        if (keyword.startsWith("#") && keyword.endsWith("#")) {
            keyword = keyword.substring(1, keyword.length() - 1).toLowerCase();

            for (Seed seed : mappings.getInstance2Seed().getAllSeeds().elementSet()) {
                String slot0text = seed.getSlot(0).toString().toLowerCase();
                String slot1text = seed.getSlot(1).toString().toLowerCase();

                if (slot0text.contains(keyword) || slot1text.contains(keyword)) {
                    for (LearnitPattern pattern : mappings.getPatternsForSeed(seed)) {
                        if (!extractor.getPatternScores().isKnownFrozen(pattern) && pattern.isCompletePattern() && !(pattern instanceof SSUnigram)) {
                            results.add(pattern);
                        }
                    }
                }
            }
        } else {

            // search by keyword on the pattern
//            ImmutableSet<String> kwd = ImmutableSet.copyOf(keyword.split(" "));
//            for (LearnitPattern pattern : mappingLookup.get(target).getInstance2Pattern().getAllPatterns().elementSet()) {
//                if (hasKeyword(SymbolUtils.toStringSet(pattern.getLexicalItems()), kwd) &&
//                        !extractor.getPatternScores().isKnownFrozen(pattern) && pattern.isCompletePattern() && !(pattern instanceof SSUnigram)) {
//                    results.add(pattern);
//                }
//            }
            ParsedQuery parsedQuery = new ParsedQuery(keyword);
            searchKeyInToken.addAll(parsedQuery.getOrWordsSet());
            searchKeyInToken.addAll(parsedQuery.getAndWordsSet());
            searchKeyInToken.addAll(parsedQuery.getOrPreds());
            Set<LearnitPattern> possiblePatterns = fullTextSearchKeyToLearnitPattern(mappings, parsedQuery);
            for (LearnitPattern learnitPattern : possiblePatterns) {
                if (!extractor.getPatternScores().isKnownFrozen(learnitPattern) && !(learnitPattern instanceof SSUnigram)) {
                    results.add(learnitPattern);
                }
            }

        }

        List<LearnitPattern> resultList = new ArrayList<LearnitPattern>(results);

        if (fullTextSearchKey.length() > 0) {
            List<LearnitPattern> results2 = new ArrayList<>();
            ParsedQuery parsedQuery = new ParsedQuery(fullTextSearchKey);

            Set<InstanceIdentifier> eligibleInstanceIdentifier = fullTextSearchKeyToInstanceIdentifier(mappings, parsedQuery);
            for (LearnitPattern learnitPattern : results) {
                if (Sets.intersection(new HashSet<>(mappings.getInstancesForPattern(learnitPattern)), eligibleInstanceIdentifier).size() > 0) {
                    results2.add(learnitPattern);
                }
            }
            resultList = results2;
        }


        if (sortingMethod.equals("overlap")) {
            //sort by keyword overlap
            Collections.sort(resultList, new Comparator<LearnitPattern>() {
                @Override
                public int compare(LearnitPattern o1, LearnitPattern o2) {
                    double right = (double) Sets.intersection(searchKeyInToken, o2.getLexicalItems()).size() / Math.max(o2.getLexicalItems().size(), searchKeyInToken.size()) * 1000;
                    double left = (double) Sets.intersection(searchKeyInToken, o1.getLexicalItems()).size() / Math.max(o1.getLexicalItems().size(), searchKeyInToken.size()) * 1000;
                    return (int) (right - left);
                }
            });
        } else {
//            sort by frequency
            Collections.sort(resultList, new Comparator<LearnitPattern>() {
                @Override
                public int compare(LearnitPattern arg0, LearnitPattern arg1) {
                    return mappings.getInstance2Pattern().getInstances(arg1).size() -
                            mappings.getInstance2Pattern().getInstances(arg0).size();
                }

            });
        }


        List<LearnitPattern> filteredResultList = new ArrayList<>();
        for (int i = 0; i < Math.min(resultList.size(), maximumResultLength); ++i) {
            filteredResultList.add(resultList.get(i));

        }

        return filteredResultList;
    }

    @JettyMethod("/init/save_progress")
    public synchronized String saveProgress() throws Exception {
        this.inMemoryAnnotationStorageNormal.convertToMappings().serialize(new File(this.instanceIdentifierAnnotationFileNormalPath), true);
        this.inMemoryAnnotationStorageOther.convertToMappings().serialize(new File(this.instanceIdentifierAnnotationFileOtherPath), true);
        for (String ontologyName : Domain.getOntologyNameToPathMap().keySet()) {
            final File ontologyFile = new File(Domain.getOntologyNameToPathMap().get(ontologyName));
            BBNInternalOntology.BBNInternalOntologyNode currentOntologyNode = this.ontologyMap.get(ontologyName);
            currentOntologyNode.convertToInternalOntologyYamlFile(ontologyFile);
        }

        Map<String, List<String>> targetToErroneousPatterns = new HashMap<>();
        for (String ex : this.targetNameToDomainEntryMap.keySet()) {
            System.out.println("Saving extractor for " + ex + "...");
            TargetAndScoreTables ext = this.targetNameToDomainEntryMap.get(ex).getTargetAndScoreTables();
            System.out.println("\tclearing unfrozen artifacts...");
            Optional<List<String>> patternsInError = checkForSymmetricPatterns(ext);

            // @hqiu We truly want to save OTHER in any case.
//            if(ex.equals("OTHER"))patternsInError = Optional.absent();
            if (patternsInError.isPresent()) {
                targetToErroneousPatterns.put(ext.getTarget().getName(), patternsInError.get());
                continue;
            }
            if (!ex.equals("OTHER")) clearUnknown(ex);
            ext.incrementIteration(); // set the iteration back up
            // make directory for writing extractor

            String strPathJson = this.targetPathDir + File.separator + ext.getTarget().getName() + ".json";
            System.out.println("\t serializing extractor for " + ex + "...");
            ext.serialize(new File(strPathJson));
            String strPathTargetJson = this.targetPathDir + ".target" + File.separator + ext.getTarget().getName() + ".json";
            ext.getTarget().serialize(strPathTargetJson);
            System.out.println("\t\t...done.");
        }
        if (targetToErroneousPatterns.isEmpty()) {
            return "Saved all extractors successfully!";
        }
        String retVal = "Following extractors could not be saved.\nPlease fix the error, and try again.\n";
        for (String target : targetToErroneousPatterns.keySet()) {
            retVal += "\nTarget " + target + " have these patterns with slots interchanged: \n";
            for (String pattern : targetToErroneousPatterns.get(target)) {
                retVal += pattern + "\n";
            }
        }
        // Runtime.getRuntime().exit(0);
        return retVal;
    }

//    /**
//     * @param target
//     * @param tripleStr A SeedPatternSimilarity json string of the form: {"language":"<language>","slot0":"word0","slot1":"word1","pattern":"patternString"}
//     * @param amountStr
//     * @return
//     * @throws IOException
//     */
//    @JettyMethod("/init/get_triple_instances")
//    public synchronized Map<String, List<String>> getTripleInstances(@JettyArg("target") String target,
//                                                                     @JettyArg("triple") String tripleStr, @JettyArg("amount") String amountStr) throws IOException {
//
//        int amount = Integer.parseInt(amountStr);
//        TargetAndScoreTables ext = checkGetExtractor(target);
//        Mappings info = mappingLookup.get(ext.getTarget().getName());
//
//        Set<InstanceIdentifier> insts = new HashSet<>(getTripleInstances(tripleStr, target, info));
//        return getInstanceContexts(insts, ext.getTarget(), amount, target, false);
//    }
//
//    private Collection<InstanceIdentifier> getTripleInstances(String tripleStr, String targetStr, Mappings info) throws IOException {
//        Target target = (new Target.Builder(targetStr)).build();
//        SeedPatternPair triple = parseTripleJson(tripleStr, targetStr).orNull();
//        if (triple == null) {
//            return Lists.newArrayList();
//        }
//        Seed seed = triple.seed();
//        String patStr = triple.pattern().getPatternIDString();
//        LearnitPattern pattern = findPattern(checkGetExtractor(targetStr), targetStr, patStr);
//        Collection<InstanceIdentifier> iidsForTriples = Lists.newArrayList();
//        Collection<InstanceIdentifier> iidsForSeed = info.getInstancesForSeed(seed);
//        Collection<InstanceIdentifier> iidsForPattern = info.getInstancesForPattern(pattern);
//        Collection<InstanceIdentifier> commonInstances = Sets.intersection(Sets.newHashSet(iidsForSeed), Sets.newHashSet(iidsForPattern));
//        for (InstanceIdentifier iid : commonInstances) {
//            Collection<Seed> seeds = mappings.getSeedsForInstance(iid);
//            boolean instanceHasSeed = seeds.contains(seed);
//            Collection<String> patternStrings = mappings.getPatternsForInstance(iid).stream().map(
//                    (LearnitPattern p) -> p.toIDString()).collect(Collectors.toSet());
//            boolean instanceHasPattern = patternStrings.contains(patStr);
//            if (instanceHasSeed && instanceHasPattern) {
//                iidsForTriples.add(iid);
//            }
//        }
//        return iidsForTriples;
//    }


    /*------------------------------------------------------------------*
     *                                                                  *
     *                       PATTERN ROUTINES                           *
     *                                                                  *
     *------------------------------------------------------------------*/

    private synchronized TargetAndScoreTables checkGetExtractor(String target) {
        if (!this.targetNameToDomainEntryMap.containsKey(target) || !this.targetNameToMappingsEntrymap.containsKey(target)) {
            throw new RuntimeException("Target " + target + " not known");
        }

        Mappings currentMappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        TargetAndScoreTables targetAndScoreTables = this.targetNameToDomainEntryMap.get(target).getTargetAndScoreTables();
        targetAndScoreTables.updateTargetAndScoreTableBasedOnMappings(currentMappings);

        return targetAndScoreTables;
    }

    public Map<String, List<String>> getInstanceContexts(List<Pair<InstanceIdentifier, String>> instsWithLabel, Target target, String relationName, boolean fromOther) throws IOException, ExecutionException, InterruptedException {
        Map<String, List<String>> result = new HashMap<>();
        List<String> InstanceIdentifierSet = new ArrayList<>();
        List<String> HTMLStringSet = new ArrayList<>();
        List<String> AnnotationSet = new ArrayList<>();
        List<String> RelationNameSet = new ArrayList<>();
        preLoadInstanceIdentifier(instsWithLabel);
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        if (fromOther) {
            for (Pair<InstanceIdentifier, String> insLabelPair : instsWithLabel) {
                InstanceIdentifier instanceIdentifier = insLabelPair.key;
                Multiset<LabelPattern> labels = inMemoryAnnotationStorageOther.lookupInstanceIdentifierAnnotation(instanceIdentifier);
                // It could have multiple labels. Just use one for now
                String relationType = "NA";
                FrozenState frozenState = FrozenState.NO_FROZEN;
                if (labels.size() >= 1) {
                    relationType = new ArrayList<>(labels).get(0).getLabel();
                    frozenState = new ArrayList<>(labels).get(0).getFrozenState();
                }
//                String htmlString = insLabelPair.value + instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                String htmlString = objectMapper.writeValueAsString(instanceIdentifier.reconstructMatchInfoDisplay(target));
                InstanceIdentifierSet.add(StorageUtils.getDefaultMapper().writeValueAsString(instanceIdentifier));
                HTMLStringSet.add(htmlString);
                AnnotationSet.add(frozenState.toString());//It's no use at all but we keep it for consistency
                RelationNameSet.add(relationType);
            }
        } else {
            for (Pair<InstanceIdentifier, String> insLabelPair : instsWithLabel) {
                InstanceIdentifier instanceIdentifier = insLabelPair.key;
                Multiset<LabelPattern> labels = inMemoryAnnotationStorageNormal.lookupInstanceIdentifierAnnotation(instanceIdentifier);
                FrozenState frozenState = FrozenState.NO_FROZEN;
                for (LabelPattern labelPattern : labels) {
                    if (labelPattern.getLabel().equals(relationName) && !labelPattern.getFrozenState().equals(FrozenState.NO_FROZEN)) {
                        frozenState = labelPattern.getFrozenState();
                    }
                }
//                String htmlString = insLabelPair.value + instanceIdentifier.reconstructMatchInfoDisplay(target).html();
                String htmlString = objectMapper.writeValueAsString(instanceIdentifier.reconstructMatchInfoDisplay(target));
                InstanceIdentifierSet.add(StorageUtils.getDefaultMapper().writeValueAsString(instanceIdentifier));
                HTMLStringSet.add(htmlString);
                AnnotationSet.add(frozenState.toString());
                RelationNameSet.add(relationName);//It's no use at all but we keep it for consistency
            }
        }
        result.put("InstanceIdentifierSet", InstanceIdentifierSet);
        result.put("HTMLStringSet", HTMLStringSet);
        result.put("AnnotationSet", AnnotationSet);
        result.put("RelationNameSet", RelationNameSet);
        return result;
    }

    public Map<String, List<String>> constructInstanceForDisplay(Collection<InstanceIdentifier> instanceIdentifiers, Target target, String relationName, int instancePerPage, int currentPage, boolean fromOther) throws IOException, InterruptedException, ExecutionException {
        List<Pair<InstanceIdentifier, String>> taggedInstanceIdentifier = new ArrayList<>();
        Annotation.InMemoryAnnotationStorage focusStorage = null;
        Set<InstanceIdentifier> addedInstanceIdentifier = new HashSet<>();
        if (fromOther) {
            focusStorage = this.inMemoryAnnotationStorageOther;
        } else {
            focusStorage = this.inMemoryAnnotationStorageNormal;
        }
        // Pass 1: In LabeledMapping, with FROZEN_GOOD_SENTENCE
        for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
            if (addedInstanceIdentifier.contains(instanceIdentifier)) continue;
            for (LabelPattern labelPattern : focusStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (fromOther) {
                    if (labelPattern.getFrozenState().equals(FrozenState.FROZEN_GOOD)) {
                        taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[LabeledMappings]"));
                        addedInstanceIdentifier.add(instanceIdentifier);
                        break;
                    }
                } else {
                    if (labelPattern.getLabel().equals(target.getName())) {
                        if (labelPattern.getFrozenState().equals(FrozenState.FROZEN_GOOD)) {
                            taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[LabeledMappings]"));
                            addedInstanceIdentifier.add(instanceIdentifier);
                            break;
                        }
                    }
                }

            }
        }
        // Pass 2: In LabeledMapping, with FROZEN_BAD_SENTENCE
        for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
            if (addedInstanceIdentifier.contains(instanceIdentifier)) continue;
            for (LabelPattern labelPattern : focusStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                if (fromOther) {
                    if (labelPattern.getFrozenState().equals(FrozenState.FROZEN_BAD)) {
                        taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[LabeledMappings]"));
                        addedInstanceIdentifier.add(instanceIdentifier);
                        break;
                    }
                } else {
                    if (labelPattern.getLabel().equals(target.getName())) {
                        if (labelPattern.getFrozenState().equals(FrozenState.FROZEN_BAD)) {
                            taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[LabeledMappings]"));
                            addedInstanceIdentifier.add(instanceIdentifier);
                            break;
                        }
                    }
                }

            }
        }
        // Pass 3: In LabeledMapping, with NO_FROZEN
        for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
            if (addedInstanceIdentifier.contains(instanceIdentifier)) continue;
            for (LabelPattern labelPattern : focusStorage.lookupInstanceIdentifierAnnotation(instanceIdentifier)) {
                taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[LabeledMappings]"));
                addedInstanceIdentifier.add(instanceIdentifier);
                break;
            }
        }
        // Pass 4: Other
        for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
            if (addedInstanceIdentifier.contains(instanceIdentifier)) continue;
            taggedInstanceIdentifier.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, ""));
            addedInstanceIdentifier.add(instanceIdentifier);
        }
        List<Pair<InstanceIdentifier, String>> instsWithLabel = getPagination(taggedInstanceIdentifier, instancePerPage, currentPage);
        return getInstanceContexts(instsWithLabel, target, relationName, fromOther);
    }

    @JettyMethod("/init/get_pattern_instances")
    public synchronized Map<String, List<String>> getPatternInstances(@JettyArg("target") String target,
                                                                      @JettyArg("pattern") String patternStr, @JettyArg("amount") String amountStr,
                                                                      @JettyArg("fromOther") String fromOtherStr
    ) throws IOException, InterruptedException, ExecutionException {
        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int amount = Integer.parseInt(amountStr);
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = this.targetNameToMappingsEntrymap.get(ext.getTarget().getName()).getMappings();
        LearnitPattern pattern = findPattern(info, patternStr);
        if (pattern == null) {
            info = info.getAllPatternUpdatedMappings(ext);
            pattern = findPattern(info, patternStr);
        }
        if (pattern != null) {

            Set<InstanceIdentifier> insts = new HashSet<>(info.getInstancesForPattern(pattern));
            return constructInstanceForDisplay(insts, ext.getTarget(), target, amount, 0, fromOther);
        } else {
            return constructInstanceForDisplay(new HashSet<>(), ext.getTarget(), target, amount, 0, fromOther);
        }
    }

    @JettyMethod("/init/get_seed_instances")
    public synchronized Map<String, List<String>> getSeedInstances(@JettyArg("target") String target,
                                                                   @JettyArg("seed") String seedStr, @JettyArg("amount") String amountStr,
                                                                   @JettyArg("fromOther") String fromOtherStr
    ) throws IOException, InterruptedException, ExecutionException {
        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int amount = Integer.parseInt(amountStr);
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings info = this.targetNameToMappingsEntrymap.get(ext.getTarget().getName()).getMappings();
        Seed seed = getSeed(seedStr, target);
        if (seed != null) {
            Set<InstanceIdentifier> insts = new HashSet<>(info.getInstancesForSeed(seed));
            return constructInstanceForDisplay(insts, ext.getTarget(), target, amount, 0, fromOther);
        }
        return constructInstanceForDisplay(new HashSet<>(), ext.getTarget(), target, amount, 0, fromOther);
    }

    @JettyMethod("/init/get_labeled_mapping_instances")
    public synchronized Map<String, List<String>> getInstanceFromLabeledMappings(@JettyArg("target") String targetStr, @JettyArg("fromOther") String fromOtherStr, @JettyArg("instancePerPage") String instancePerPageStr, @JettyArg("currentPage") String currentPageStr) throws IOException, ExecutionException, InterruptedException {
        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int instancePerPage = Integer.parseInt(instancePerPageStr);
        int currentPage = Integer.parseInt(currentPageStr);
        List<Pair<InstanceIdentifier, String>> instsWithLabel = new ArrayList<>();
//        Set<InstanceIdentifier> availAbleInstanceidentifier = new HashSet<>();
//        Mappings mappings = this.mappingLookup.get(targetStr);
//        availAbleInstanceidentifier.addAll(mappings.getSeedInstances());
//        availAbleInstanceidentifier.addAll(mappings.getPatternInstances());

//        InstanceComparatorBySeed instanceComparatorBySeed = new InstanceComparatorBySeed(mappings);
        Annotation.InMemoryAnnotationStorage focusLabeledMappings = null;

        if (fromOther) {
            focusLabeledMappings = inMemoryAnnotationStorageOther;
            List<InstanceIdentifier> instanceIdentifiersListInLabeledMappings = new ArrayList<>();
            for (InstanceIdentifier instanceIdentifier : focusLabeledMappings.getAllInstanceIdentifier()) {
//                if(availAbleInstanceidentifier.contains(instanceIdentifier)){
                instanceIdentifiersListInLabeledMappings.add(instanceIdentifier);
//                }
            }
//            Collections.sort(instanceIdentifiersListInLabeledMappings, instanceComparatorBySeed);
            Collections.sort(instanceIdentifiersListInLabeledMappings);
            int length = instanceIdentifiersListInLabeledMappings.size();
            int currentIdx = 0;
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiersListInLabeledMappings) {
                instsWithLabel.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[" + currentIdx + "/" + length + "] "));
                currentIdx++;
            }
        } else {
            focusLabeledMappings = inMemoryAnnotationStorageNormal;
            List<InstanceIdentifier> instanceIdentifiersListInLabeledMappings = new ArrayList<>();
            for (InstanceIdentifier instanceIdentifier : focusLabeledMappings.getAllInstanceIdentifier()) {
//                if(availAbleInstanceidentifier.contains(instanceIdentifier)){
                instanceIdentifiersListInLabeledMappings.add(instanceIdentifier);
//                }
            }
//            Collections.sort(instanceIdentifiersListInLabeledMappings, instanceComparatorBySeed);
            Collections.sort(instanceIdentifiersListInLabeledMappings);
            int length = instanceIdentifiersListInLabeledMappings.size();
            int currentIdx = 0;
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiersListInLabeledMappings) {
                instsWithLabel.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, "[" + currentIdx + "/" + length + "] "));
                currentIdx++;
            }
        }
        Target target = checkGetExtractor(targetStr).getTarget();
        List<Pair<InstanceIdentifier, String>> allInstanceWithTag = getPagination(instsWithLabel, instancePerPage, currentPage);
        Map<String, List<String>> result = new HashMap<>();
        List<String> InstanceIdentifierSet = new ArrayList<>();
        List<String> HTMLStringSet = new ArrayList<>();
        List<String> AnnotationSet = new ArrayList<>();
        List<String> RelationNameSet = new ArrayList<>();
        preLoadInstanceIdentifier(allInstanceWithTag);
        for (Pair<InstanceIdentifier, String> insLabelPair : allInstanceWithTag) {
            InstanceIdentifier instanceIdentifier = insLabelPair.key;
            Multiset<LabelPattern> labels = focusLabeledMappings.lookupInstanceIdentifierAnnotation(instanceIdentifier);
            String relationType = "NA";
            FrozenState frozenState = FrozenState.NO_FROZEN;
            if (labels.size() >= 1) {
                relationType = new ArrayList<>(labels).get(0).getLabel();
                frozenState = new ArrayList<>(labels).get(0).getFrozenState();
            }
//            String htmlString = insLabelPair.value + instanceIdentifier.reconstructMatchInfoDisplay(target).html();
            InstanceIdentifierSet.add(StorageUtils.getDefaultMapper().writeValueAsString(instanceIdentifier));
            HTMLStringSet.add(StorageUtils.getMapperWithoutTyping().writeValueAsString(instanceIdentifier.reconstructMatchInfoDisplay(target)));
            AnnotationSet.add(frozenState.toString());
            RelationNameSet.add(relationType);
        }


        result.put("InstanceIdentifierSet", InstanceIdentifierSet);
        result.put("HTMLStringSet", HTMLStringSet);
        result.put("AnnotationSet", AnnotationSet);
        result.put("RelationNameSet", RelationNameSet);
        return result;
    }

    @JettyMethod("/init/get_labeled_mapping_number_of_pages")
    public synchronized int getNumberOfPagesInInstanceFromLabeledMappings(@JettyArg("target") String targetStr, @JettyArg("fromOther") String fromOtherStr, @JettyArg("instancePerPage") String instancePerPageStr) throws IOException {
        Set<InstanceIdentifier> availAbleInstanceidentifier = new HashSet<>();
//        Mappings mappings = this.mappingLookup.get(targetStr);
//        availAbleInstanceidentifier.addAll(mappings.getSeedInstances());
//        availAbleInstanceidentifier.addAll(mappings.getPatternInstances());

        boolean fromOther = Boolean.parseBoolean(fromOtherStr);
        int instancePerPage = Integer.parseInt(instancePerPageStr);
        List<Pair<InstanceIdentifier, String>> instsWithLabel = new ArrayList<>();
        Annotation.InMemoryAnnotationStorage focusLabeledMappings = null;
        if (fromOther) {
            focusLabeledMappings = inMemoryAnnotationStorageOther;
            for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorageOther.getAllInstanceIdentifier()) {
//                if(availAbleInstanceidentifier.contains(instanceIdentifier)){
                instsWithLabel.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, ""));
//                }

            }
        } else {
            focusLabeledMappings = inMemoryAnnotationStorageNormal;
            for (InstanceIdentifier instanceIdentifier : inMemoryAnnotationStorageNormal.getAllInstanceIdentifier()) {
//                if(availAbleInstanceidentifier.contains(instanceIdentifier)){
                instsWithLabel.add(new Pair<InstanceIdentifier, String>(instanceIdentifier, ""));
//                }
            }
        }
        return getNumberOfPages(instsWithLabel, instancePerPage);
    }

    boolean isBilingualOrChineseMonolingualString(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DFF) ||
                    (c >= 0x20000 && c <= 0x2A6DF) || (c >= 0xF900 && c <= 0xFAFF) ||
                    (c >= 0x2F800 && c <= 0x2FA1F))
                return true;
        }

        return false;
    }

    @JettyMethod("/init/add_target")
    public synchronized String addTarget(
            @JettyArg("name") String name,
            @JettyArg("description") String description,
            @JettyArg("slot0EntityTypes") String slot0EntityTypes,
            @JettyArg("slot1EntityTypes") String slot1EntityTypes,
            @JettyArg("symmetric") String symmetricStr,
            @JettyArg("slot0SpanningType") String slot0SpanningType,
            @JettyArg("slot1SpanningType") String slot1SpanningType
    ) throws IOException {
        if (this.targetNameToDomainEntryMap.containsKey(name)) {
            throw new RuntimeException("You're creating a target which targetname is conflict with existing targets.");
        }

        boolean symmetric = Boolean.parseBoolean(symmetricStr);


        ArrayList<String> slot0List = Lists.newArrayList(slot0EntityTypes.split(","));
        ArrayList<String> slot1List = Lists.newArrayList(slot1EntityTypes.split(","));

        Set<InstanceIdentifier.SpanningType> possibleSlot0SpanningType = new HashSet<>();
        Set<InstanceIdentifier.SpanningType> possibleSlot1SpanningType = new HashSet<>();

        if (slot0SpanningType.contains("Empty")) {
            possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.Empty);
        } else {
            String[] spanningTypes = slot0SpanningType.split(",");
            for (String spanningType : spanningTypes) {
                possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
            }
        }

        if (slot1SpanningType.contains("Empty")) {
            possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.Empty);
        } else {
            String[] spanningTypes = slot1SpanningType.split(",");
            for (String spanningType : spanningTypes) {
                possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
            }
        }
        Target newTarget;
        if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .build();

            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention) || possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.ValueMention)) {
                List<String> allTypes = ImmutableList.of("PER", "ORG", "GPE", "LOC", "FAC", "WEA", "VEH", "PRN", "TPN");
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all").build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Mention, InstanceIdentifier.SpanningType.ValueMention)))
                        .withAddedConstraint(new AtomicMentionConstraint(1))
                        .withAddedConstraint(new EntityTypeOrValueConstraint(1, slot1List))
                        .withAddedConstraint(new MinEntityLevelConstraint("All", 1, "DESC"))
                        .build();
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                newTarget = new Target.Builder(name).
                        withTargetSlot(new TargetSlot.Builder(0, "all").build()).
                        withTargetSlot(new TargetSlot.Builder(1, "all").build())
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty))).build();
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new AtomicMentionConstraint(0))
//                        .withAddedConstraint(new EntityTypeConstraint(0, slot0List))
//                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 0, "DESC"))
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty)))
                        .build();
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
                newTarget = new Target.Builder(name)
                        .withTargetSlot(new TargetSlot.Builder(0, "all")
                                .build()).withTargetSlot(new TargetSlot.Builder(1, "all")
                                .build())
                        .withAddedConstraint(new AtomicMentionConstraint(0))
                        .withAddedConstraint(new AtomicMentionConstraint(1))
//                        .withAddedConstraint(new EntityTypeConstraint(0, slot0List))
//                        .withAddedConstraint(new EntityTypeConstraint(1, slot1List))
//                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 0, "DESC"))
//                        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 1, "DESC"))
                        .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                        .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Mention)))
                        .build();
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else {
            throw new com.bbn.bue.common.exceptions.NotImplementedException();
        }

        TargetAndScoreTables targetAndScoreTables = new TargetAndScoreTables(newTarget);
        BBNInternalOntology.BBNInternalOntologyNode bbnInternalOntologyNode = this.targetNameToOntologyNode.get(newTarget.getName());
        Domain.ExtractorEntry extractorEntry = new Domain.ExtractorEntry(targetAndScoreTables, bbnInternalOntologyNode);
        this.targetNameToDomainEntryMap.put(name, extractorEntry);
        Epoch.MappingsEntryUnderEpoch mappingsEntryUnderEpoch = this.originalEpochEntries.get(bbnInternalOntologyNode.getRoot().originalKey);
        mappingsEntryUnderEpoch.getTargetFilterWithCache().setFocusTarget(targetAndScoreTables.getTarget());
        Mappings targetMappings = mappingsEntryUnderEpoch.getTargetFilterWithCache().makeFiltered(mappingsEntryUnderEpoch.getMappings());
        Epoch.MappingsEntryUnderEpoch targetMappingsEntry = new Epoch.MappingsEntryUnderEpoch(targetMappings, workingEpoch, mappingsEntryUnderEpoch.getTargetFilterWithCache(), mappingsEntryUnderEpoch.getObservationSimilarityModule());
        this.targetNameToMappingsEntrymap.put(name, targetMappingsEntry);
        return "successfully build target: " + name;
    }

    boolean isBilingualOrChineseMonolingual(LearnitPattern pattern) {
        return isBilingualOrChineseMonolingualString(pattern.toIDString());
    }

    boolean isBilingualOrChineseMonolingual(Seed seed) {
        return isBilingualOrChineseMonolingualString(seed.getSlot(0).toString()) ||
                isBilingualOrChineseMonolingualString(seed.getSlot(1).toString());
    }

    private LearnitPattern findPattern(TargetAndScoreTables data, Mappings mappings, String pattern) {
        LearnitPattern p1 = findPattern(mappings, pattern);
        if (p1 == null) {
//			System.out.println("===================================");
            return findPattern(mappings.getAllPatternUpdatedMappings(data), pattern);
        } else {
            return p1;
        }
    }

//    @JettyMethod("/init/add_triple")
//    public synchronized String addTriple(@JettyArg("target") String targetStr, @JettyArg("triple") String tripleStr, @JettyArg("quality") String quality) throws IOException {
//
//        TargetAndScoreTables ext = checkGetExtractor(targetStr);
//        Mappings info = mappingLookup.get(targetStr);
//        SeedPatternPair triple = parseTripleJson(tripleStr, targetStr).orNull();
//        if (triple != null && !ext.getTripleScores().isKnownFrozen(triple)) {
//            ext.getTripleScores().addDefault(triple);
//            double instanceWeight = 0.0;
//            Collection<InstanceIdentifier> iidsForTriples = getTripleInstances(tripleStr, targetStr, info);
//            for (InstanceIdentifier iid : iidsForTriples) {
//                instanceWeight += iid.getConfidence();
//            }
//            TripleScore score = ext.getTripleScores().getScore(triple);
//            int frequency = iidsForTriples.size();
//
//            score.setFrequency(frequency);
//            score.setConfidenceDenominator(instanceWeight);
//            score.setRecall(0.01);
//
//            if (quality.equals("good")) {
//                score.setPrecision(0.95);
//                score.setConfidence(1.0);
//                score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
//                score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));
//
//                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
//                if (info.getKnownInstanceCount(ext) == 0) {
//                    ext.setGoodSeedPrior(score.getConfidenceNumerator() * 10, ext.getTotalInstanceDenominator());
//                }
//
//            } else if (quality.equals("bad")) {
//                score.setPrecision(0.05);
//                score.setConfidence(1.0);
//                score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
//                score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));
//
//                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
//
//            } else {
//                throw new RuntimeException("Unknown quality " + quality);
//            }
//
//
//            System.out.println("tp :" + score.getTP());
//            System.out.println("fp :" + score.getFP());
//            System.out.println("tn :" + score.getTN());
//            System.out.println("fn :" + score.getFN());
//            score.freezeScore(ext.getIteration());
//            ext.getTripleScores().orderItems();
//        } else {
//            throw new RuntimeException("Could not add triple " + tripleStr);
//        }
//
//        return "Added triple";
//    }

    private LearnitPattern findPattern(Mappings info, String pattern) {
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.toIDString().equals(pattern)) {
                return p;
            }
        }
        return null;
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
        Mappings info = this.targetNameToMappingsEntrymap.get(target).getMappings();
        System.out.println("=====info: info.getInstanceCount()=" + info.getInstanceCount() + ", info.getAllPatterns()=" + info.getAllPatterns().size());
        LearnitPattern pattern = findPattern(ext, info, patStr);
        if (pattern != null) {

            ext.getPatternScores().addDefault(pattern);
            double instanceWeight = 0.0;
            for (InstanceIdentifier id : info.getInstancesForPattern(pattern)) {
                instanceWeight += id.getConfidence();
            }

            PatternScore score = ext.getPatternScores().getScore(pattern);
            if (score.isFrozen()) score.unfreeze();
            int frequency = info.getInstancesForPattern(pattern).size();

            score.setFrequency(frequency);
            score.setConfidenceDenominator(instanceWeight);
            score.setRecall(0.01);

            if (quality.equals("good")) {
                score.setPrecision(0.95);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
                score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
                if (info.getKnownInstanceCount(ext) == 0) {
                    ext.setGoodSeedPrior(score.getConfidenceNumerator() * 10, ext.getTotalInstanceDenominator());
                }

            } else if (quality.equals("bad")) {
                score.setPrecision(0.05);
                score.setConfidence(1.0);
                score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
                score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));

                score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());

            } else {
                throw new RuntimeException("Unknown quality " + quality);
            }


            System.out.println("tp :" + score.getTP());
            System.out.println("fp :" + score.getFP());
            System.out.println("tn :" + score.getTN());
            System.out.println("fn :" + score.getFN());

            calculatePatternScoreExtra(pattern, score, ext, info);

            score.freezeScore(ext.getIteration());
            ext.getPatternScores().orderItems();
        } else {
            throw new RuntimeException("Could not add pattern " + patStr);
        }

        return "Added pattern";
    }

    void ChangeInstanceIdentifierAnnotation(InstanceIdentifier instanceIdentifier, String label, FrozenState frozenState) {
        this.inMemoryAnnotationStorageNormal.addOrChangeAnnotation(instanceIdentifier, new LabelPattern(label, frozenState));
    }

    @JettyMethod("/init/add_instance")
    public synchronized String FrozenInstance(@JettyArg("target") String target, @JettyArg("instance") String instance, @JettyArg("quality") String quality) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance, InstanceIdentifier.class);
        FrozenState frozenState;
        switch (quality) {
            case "good":
                frozenState = FrozenState.FROZEN_GOOD;
                break;
            case "bad":
                frozenState = FrozenState.FROZEN_BAD;
                break;
            default:
                frozenState = FrozenState.NO_FROZEN;
        }
        this.inMemoryAnnotationStorageNormal.deleteAnnotationUnderLabelPattern(instanceIdentifier, new LabelPattern("NA", FrozenState.FROZEN_GOOD));
        this.inMemoryAnnotationStorageNormal.deleteAnnotationUnderLabelPattern(instanceIdentifier, new LabelPattern("NA", FrozenState.FROZEN_BAD));
        this.inMemoryAnnotationStorageNormal.deleteAnnotationUnderLabelPattern(instanceIdentifier, new LabelPattern("NA", FrozenState.NO_FROZEN));
        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, frozenState);
        return "OK";
    }

    @JettyMethod("/init/remove_instance")
    public synchronized String UnFrozenInstance(@JettyArg("target") String target, @JettyArg("instance") String instance) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance, InstanceIdentifier.class);
        FrozenState frozenState = FrozenState.NO_FROZEN;
        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, frozenState);
        return "OK";
    }

    @JettyMethod("/init/mark_instance_from_other")
    public synchronized String MarkInstanceFromOther(@JettyArg("target") String target, @JettyArg("instance") String instance) throws IOException {
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance, InstanceIdentifier.class);
//        ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
        this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
        this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        return "OK";
    }

    @JettyMethod("/init/mark_instance_from_labeled_mappings")
    public synchronized String MarkInstanceFromLabeledMappings(@JettyArg("target") String target, @JettyArg("instance") String instance) throws IOException {
        // This is different from MarkInstanceFromOther due to we're marking it to normal labeledmappings
        InstanceIdentifier instanceIdentifier = StorageUtils.getDefaultMapper().readValue(instance, InstanceIdentifier.class);
        this.inMemoryAnnotationStorageNormal.deleteAllAnnotation(instanceIdentifier);
        if (target.equals("NA")) {
            this.inMemoryAnnotationStorageNormal.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        } else {
            this.inMemoryAnnotationStorageNormal.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        }
        return FrozenState.FROZEN_GOOD.toString();
    }

    @JettyMethod("/init/mark_seed_from_other")
    public synchronized String MarkSeedFromOther(@JettyArg("target") String target, @JettyArg("seed") String seedString) throws IOException {
        Seed seed = getSeed(seedString, target);
        String rootOntologyName = this.targetNameToOntologyNode.get(target).getRoot().originalKey;
        Mappings mappings = this.originalEpochEntries.get(rootOntologyName).getMappings();
        for (InstanceIdentifier instanceIdentifier : mappings.getInstancesForSeed(seed)) {
//            ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
            this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
            this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        }
        return "OK";
    }

    @JettyMethod("/init/mark_pattern_from_other")
    public synchronized String MarkPatternFromOther(@JettyArg("target") String target, @JettyArg("pattern") String patternString) {
        String rootOntologyName = this.targetNameToOntologyNode.get(target).getRoot().originalKey;
        Mappings mappings = this.originalEpochEntries.get(rootOntologyName).getMappings();
        LearnitPattern pattern = findPattern(mappings, patternString);
        for (InstanceIdentifier instanceIdentifier : mappings.getInstancesForPattern(pattern)) {
//            ChangeInstanceIdentifierAnnotation(instanceIdentifier, target, FrozenState.FROZEN_GOOD);
            this.inMemoryAnnotationStorageOther.deleteAllAnnotation(instanceIdentifier);
            this.inMemoryAnnotationStorageOther.addAnnotation(instanceIdentifier, new LabelPattern(target, FrozenState.FROZEN_GOOD));
        }
        return "OK";
    }

    private Optional<SeedPatternPair> parseTripleJson(String tripleJson, String target) throws IOException {
        TargetAndScoreTables ext = checkGetExtractor(target);
        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        Map<String, String> json = mapper.readValue(tripleJson, HashMap.class);
        String language = json.get("language");
        String slot0 = json.get("slot0");
        String slot1 = json.get("slot1");
        Seed seed = Seed.from(language, slot0, slot1).withProperText(ext.getTarget());
        seed = getSeed(mapper.writeValueAsString(seed), target);
        if (seed == null) {//Cannot create a triple with a seed that doesn't exist in mapping
            return Optional.absent();
        }
        String patternAsString = json.get("pattern");
        String rootOntologyName = this.targetNameToOntologyNode.get(target).getRoot().originalKey;
        Mappings mappings = this.originalEpochEntries.get(rootOntologyName).getMappings();
        LearnitPattern learnitPattern = findPattern(mappings, patternAsString);
        if (learnitPattern == null) {//Cannot create a triple with a pattern that doesn't exist in mapping
            return Optional.absent();
        }
        PatternID pattern = PatternID.from(learnitPattern);
        return Optional.of(SeedPatternPair.create(seed, pattern));
    }

    /**
     * UNACCEPT A TRIPLE
     * -----------------------------------
     * Unsets the specified triple as known and good in the system
     *
     * @param target
     * @param tripleStr A SeedPatternSimilarity json string of the form: {"language":"<language>","slot0":"word0","slot1":"word1","pattern":"patternString"}
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JettyMethod("/init/remove_triple")
    public synchronized String removeTriple(@JettyArg("target") String target, @JettyArg("triple") String tripleStr) throws JsonParseException, JsonMappingException, IOException {
        TargetAndScoreTables ext = checkGetExtractor(target);
        // LearnitPattern pattern = findPattern(ext,target,patternStr);
        SeedPatternPair triple = parseTripleJson(tripleStr, target).orNull();
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

    public void proposeFirstPatterns(String relation) {
        TargetAndScoreTables ex = checkGetExtractor(relation);
        String rootOntologyName = this.targetNameToOntologyNode.get(relation).getRoot().originalKey;
        Mappings mappings = this.originalEpochEntries.get(rootOntologyName).getMappings();
        addPatterns(ex, mappings);
    }

    ////////////

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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        addPatterns(extractor, mappings);
        return "success";
    }

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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        if (pattern != null && ext.getPatternScores().isKnownFrozen(pattern)) {
            PatternScore score = ext.getPatternScores().getScore(pattern);
            score.unfreeze();
            score.setConfidence(0.0);
            score.setPrecision(0.0);
            ext.getPatternScores().orderItems();
            calculatePatternScoreExtra(pattern, score, ext, mappings);
            return "success";
        } else {
            throw new RuntimeException("Pattern is not here");
        }
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
    public synchronized List<Seed> getSeedsBySlots(@JettyArg("target") String target, @JettyArg("slot0") String slot0, @JettyArg("slot1") String slot1, @JettyArg("amount") String amountStr, @JettyArg("fullTextSearchKey") String fullTextSearchKey, @JettyArg("sortingMethod") String sortingMethod) {
        int maximumResultLength = Integer.parseInt(amountStr);
        List<Seed> results = new ArrayList<Seed>();
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        for (Seed seed : mappings.getInstance2Seed().getAllSeeds().elementSet()) {
            if (slot1.trim().equals("NA")) {
                if (seed.getSlot(0).toString().contains(slot0))
                    results.add(seed);
            } else if (seed.getSlot(0).toString().contains(slot0) && seed.getSlot(1).toString()
                    .contains(slot1)) {
                results.add(seed);
            }
        }

        if (fullTextSearchKey.length() > 0) {
            List<Seed> results2 = new ArrayList<>();
            ParsedQuery parsedQuery = new ParsedQuery(fullTextSearchKey);
            Set<InstanceIdentifier> eligibleInstanceIdentifier = fullTextSearchKeyToInstanceIdentifier(mappings, parsedQuery);
            for (Seed seed : results) {
                if (Sets.intersection(new HashSet<>(mappings.getInstancesForSeed(seed)), eligibleInstanceIdentifier).size() > 0) {
                    results2.add(seed);
                }
            }
            results = results2;
        }

        List<Seed> filteredResultList = new ArrayList<>();
        for (int i = 0; i < Math.min(results.size(), maximumResultLength); ++i) {
            filteredResultList.add(results.get(i));
        }

        return filteredResultList;
    }

    private boolean hasKeyword(Iterable<String> words, Collection<String> keywords) {
        for (String w : words) {
            w = w.toLowerCase();
            for (String key : keywords) {
                if (key.endsWith("*")) {
                    if (w.startsWith(key.substring(0, key.length() - 1))) return true;
                } else {
                    if (w.equals(key)) return true;
                }
            }
        }
        return false;
    }

    private Collection<LearnitPattern> getFromInitial(Collection<InitializationPattern> initPatterns, Set<LearnitPattern> initPatternsInSexpFormat,
                                                      Mappings info, Target target) {
        ImmutableSet.Builder<LearnitPattern> builder = ImmutableSet.builder();
        for (LearnitPattern p : info.getAllPatterns().elementSet()) {
            if (p.isProposable(target) && (matchesInitializationPattern(p, initPatterns) || initPatternsInSexpFormat.contains(p))) {
                builder.add(p);
            }
        }
        return builder.build();
    }

    public void addPatterns(TargetAndScoreTables extractor, Mappings info, Collection<LearnitPattern> initialPatterns) {
        PatternProposer proposer = new PatternProposer(extractor, false);

        PatternProposalInformation proposalInformation = proposer.processMappings(info);
        SimplifiedPatternScorer scorer = new SimplifiedPatternScorer(extractor, proposalInformation);

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

    public void proposeFromInitializationPatterns(String relation, Collection<InitializationPattern> initPatterns, Set<LearnitPattern> initPatternsInSexpFormat) {
        TargetAndScoreTables ex = checkGetExtractor(relation);
        Mappings mappings = this.targetNameToMappingsEntrymap.get(relation).getMappings();
        Collection<LearnitPattern> fromInitial = getFromInitial(initPatterns, initPatternsInSexpFormat,
                mappings, ex.getTarget());
        addPatterns(ex, mappings, fromInitial);
    }

    private void freezeSeed(TargetAndScoreTables ext, Seed seed, String quality) {
        if (quality.equals("good")) {
            freezeSeed(ext, seed, 1.0, 0.9);
        } else if (quality.equals("bad")) {
            freezeSeed(ext, seed, 1.0, 0.1);
        } else {
            throw new RuntimeException("Unknown seed quality " + quality);
        }
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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        PatternPruner pruner = new PatternPruner(extractor);
        pruner.score(pruner.processMappings(mappings));
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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();

        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
        Seed seed = mapper.readValue(seedJson, Seed.class).withProperText(ext.getTarget());

        for (Seed s : mappings.getAllSeeds().elementSet()) {
            if (seed.equals(s.withProperText(ext.getTarget())))
                return s;
        }
        return null;
    }

    /**
     * AUTO-ACCEPT SEEDS
     * ------------------------------
     * Perform seed selection to get a new set of good seeds by the system's scoring
     *
     * @param target
     * @param amount the number to get
     * @param seeds  the seeds to ban and not accept (because a human said so)
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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        for (String seedStr : seeds) {
            if (seedStr.equals("null")) break;

            Seed seed = getSeed(seedStr, target);
            if (ext.getSeedScores().isKnownFrozen(seed)) {
                SeedScore score = ext.getSeedScores().getScore(seed);
                score.unfreeze();
                score.setConfidence(0.0);
                score.setScore(0.0);
                calculateSeedScoreExtra(seed, score, ext, mappings);
            }
        }
        ext.getSeedScores().orderItems();
//        SeedSimilarity.updateKMeans(ext);
        return "success";
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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(ext.getTarget().getName()).getMappings();
        SeedScore sscore = ext.getSeedScores().getScore(seed);
        // sscore.setScore(0.9);
        sscore.setScore(1000000.0); // TODO: fixme
        sscore.setConfidence(confidence);
        sscore.freezeScore(ext.getIteration());
        calculateSeedScoreExtra(seed, sscore, ext, mappings);
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
     * AUTO-ACCEPT TRIPLES
     * ------------------------------
     * Perform triple selection to get a new set of good triples by the system's scoring
     *
     * @param target
     * @param amount  the number to get
     * @param triples the triples to ban and not accept (because a human said so)
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
            Seed seed = getSeed(seedStr, target);
            if (seed != null) {
                freezeSeed(ext, seed, quality);
                count++;
            }
        }
        ext.getSeedScores().orderItems();
        return "Added " + count + " seeds";
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
        Seed seed = getSeed(seedStr, target);
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        if (seed != null && ext.getSeedScores().isKnownFrozen(seed)) {
            SeedScore score = ext.getSeedScores().getScore(seed);
            score.unfreeze();
            score.setConfidence(0.0);
            score.setScore(0.0);
            ext.getSeedScores().orderItems();
            calculateSeedScoreExtra(seed, score, ext, mappings);
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
        System.out.println("Getting extractor for " + target);
        TargetAndScoreTables extractor = checkGetExtractor(target);
        System.out.println("Proposing seeds...");
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        addSeeds(extractor, mappings);
        return "success";
    }

    @JettyMethod("/init/propose_seeds_new")
    public synchronized SeedScoreTable proposeSeeds(@JettyArg("target") String target) {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Mappings info = this.targetNameToMappingsEntrymap.get(target).getMappings();
        SeedScoreTable seedScoreTable = new SeedScoreTable();

        Set<LearnitPattern> patternsFrozen = extractor.getPatternScores().getFrozen();
        Multiset<Seed> seedsForPatterns = info.getSeedsForPatterns(patternsFrozen);
        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;

        for (Seed seed : seedsForPatterns.elementSet()) {

            if (seed != null && !extractor.getSeedScores().isKnownFrozen(seed)) {
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
                if (numProposedSeeds >= SIZE_LIMIT) {
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
        System.out.println("\tfound " + seedsForPatterns.size() + " seeds...");
        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;
        for (Seed seed : seedsForPatterns.elementSet()) {

            if (seed != null && !extractor.getSeedScores().isKnownFrozen(seed)) {
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
                if (numProposedSeeds >= SIZE_LIMIT) {
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
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        SeedPruner pruner = new SeedPruner(extractor);
        pruner.score(pruner.processMappings(mappings));
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

    @JettyMethod("/init/propose_patterns_new")
    public PatternScoreTable proposePatterns(@JettyArg("target") String target) {
        TargetAndScoreTables ext = checkGetExtractor(target);
        Mappings mappings = this.targetNameToMappingsEntrymap.get(target).getMappings();
        Set<Seed> seedsFrozen = ext.getSeedScores().getFrozen();
        Multiset<LearnitPattern> patternsForSeeds = mappings.getPatternsForSeeds(seedsFrozen);
        Set<LearnitPattern> patterns = patternsForSeeds.elementSet();

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedPatterns = 0;
        PatternScoreTable patternScoreTable = new PatternScoreTable();
        for (LearnitPattern pattern : patterns) {
            if (pattern != null && !ext.getPatternScores().isKnownFrozen(pattern) && !(pattern instanceof SSUnigram)) {
                numProposedPatterns++;
                patternScoreTable.addDefault(pattern);
                double instanceWeight = 0.0;
                for (InstanceIdentifier id : mappings.getInstancesForPattern(pattern)) {
                    instanceWeight += id.getConfidence();
                }

                PatternScore score = patternScoreTable.getScore(pattern);
                int frequency = mappings.getInstancesForPattern(pattern).size();

                score.setFrequency(frequency);
                score.setConfidenceDenominator(instanceWeight);
                score.setRecall(0.01);


                /*shouldFollowOriginalTargetFilter
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
                calculatePatternScoreExtra(pattern, score, ext, mappings);
                //

                score.unfreeze();
                if (numProposedPatterns >= SIZE_LIMIT) {
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

    @JettyMethod("/init/similar_seeds")
    public synchronized SeedScoreTable getSimilarSeedsNew(@JettyArg("target") String target) throws Exception {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Epoch.MappingsEntryUnderEpoch mappingsEntry = this.targetNameToMappingsEntrymap.get(target);
        Mappings info = mappingsEntry.getMappings();
        SeedScoreTable seedScoreTable = new SeedScoreTable();

        Set<Seed> frozenGoodSeeds = new HashSet<>();
        Set<Seed> frozenSeeds = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<Seed, SeedScore> objectWithScore : extractor.getSeedScores().getObjectsWithScores()) {
            SeedScore seedScore = objectWithScore.getScore();
            if (seedScore.isFrozen() && seedScore.isGood()) {
                frozenGoodSeeds.add(objectWithScore.getObject());
            }
            if (seedScore.isFrozen()) {
                frozenSeeds.add(objectWithScore.getObject());
            }
        }

        int SIZE_LIMIT = 50; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int numProposedSeeds = 0;
        double threshold = 0.2;

        ObservationSimilarityModule observationSimilarityModule = mappingsEntry.getObservationSimilarityModule();

        List<Optional<? extends ObservationSimilarity>> seedSimilarityRows = new ArrayList<>();

        for (Seed seed : frozenGoodSeeds) {
            Optional<? extends ObservationSimilarity> seedSimilarity = observationSimilarityModule.getSeedSimilarity(seed);
            seedSimilarityRows.add(seedSimilarity);
        }

        List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> similarSeeds =
                ObservationSimilarity.mergeMultipleSimilarities(seedSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(frozenSeeds));

        for (Pair<LearnItObservation, Double> seedObj : similarSeeds) {
            Seed seed = (Seed) seedObj.key;
            if (info.getInstancesForSeed(seed).size() < 1) continue;
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

    /*
     * Similarity related handlers
     */

    @JettyMethod("/similarity/triples")
    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> getSimilarSeedPatternPairs(
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
//
//
//    @JettyMethod("/similarity/seeds")
//    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> getSimilarSeeds(
//            @JettyArg("target") String target,
//            @JettyArg("language") String language,
//            @JettyArg("seeds") String seedsAsJson,
//            //@JettyArg("seeds") String[] seeds,
//            @JettyArg("threshold") String threshold,
//            @JettyArg("cutoff") String cutOff
//    ) throws IOException{
////        throw new NotImplementedException();
//        TargetAndScoreTables ext = checkGetExtractor(target);
//        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;
//        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
//        Map<String,List<List<String>>> json = mapper.readValue(seedsAsJson, HashMap.class);
//        List<Optional<? extends ObservationSimilarity>> seedSimilarityRows = new ArrayList<>();
//        System.out.println("Received "+json.get("seeds").size()+" seeds for similarity calculation...");
//        Set<Seed> seedsToFilter = new HashSet<>();
//        for(List<String> seedDesc : json.get("seeds")){
//            String seedStr = seedDesc.get(0);
////            System.out.println("SeedStr: "+seedStr);
//            Seed seed = getSeed(seedStr,target);
////            Seed seed = Seed.from(language,seedSlots.get(0),seedSlots.get(1)).withProperText(TargetFactory.fromString(target));
//            Optional<? extends ObservationSimilarity> seedSimilarity = observationSimilarityModule.getSeedSimilarity(seed);
//            seedSimilarityRows.add(seedSimilarity);
//            seedsToFilter.add(seed);
//        }
//        //Filter out any seeds that are already frozen
//        seedsToFilter.addAll(ext.getSeedScores().getFrozen());
//        System.out.println("\nMerging similarity rows...");
//        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarSeeds =
//                ObservationSimilarity.mergeMultipleSimilarities(seedSimilarityRows,
//                        Double.parseDouble(threshold),
//                        cutOff.isEmpty()? Optional.absent(): Optional.of(Integer.parseInt(cutOff)),
//                        Optional.of(seedsToFilter));
//        System.out.println("Returning a list of "+similarSeeds.size()+" similar seeds...");
//        return similarSeeds;
//    }

    @JettyMethod("/ontology/current_tree")
    public synchronized Map<String, BBNInternalOntology.BBNInternalOntologyNode> getOntologyTree() {
        return this.ontologyMap;
    }
//
//    @JettyMethod("/similarity/patterns")
//    public synchronized List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> getSimilarPatterns(
//            @JettyArg("target") String target,
//            @JettyArg("patterns") String patternsAsJson,
//            @JettyArg("threshold") String threshold,
//            @JettyArg("cutoff") String cutOff
//    ) throws IOException{
////        throw new NotImplementedException();
//        TargetAndScoreTables ext = checkGetExtractor(target);
//        ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
//        ObservationSimilarityModule observationSimilarityModule = this.observationSimilarityModule;
//        Map<String,List<String>> json = mapper.readValue(patternsAsJson, HashMap.class);
//        List<com.google.common.base.Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();
//        System.out.println("Received "+json.get("patterns").size()+" patterns for similarity calculation...");
//        Set<PatternID> patternsToFilter = new HashSet<>();
//        for(String patternStr : json.get("patterns")){
//            LearnitPattern pattern = findPattern(mappingLookup.get(target),patternStr);
//            if (pattern == null){
//                continue;
//            }
//            Optional<? extends ObservationSimilarity> patternSimilarity =
//                    observationSimilarityModule.getPatternSimilarity(PatternID.from(pattern));
//            patternSimilarityRows.add(patternSimilarity);
//            patternsToFilter.add(PatternID.from(pattern));
//        }
//        //Filter out any patterns that are already frozen
//        patternsToFilter.addAll(ext.getPatternScores().getFrozen().stream().map(
//                (LearnitPattern p)->PatternID.from(p)).collect(Collectors.toSet()));
//        System.out.println("\nMerging similarity rows...");
//        List<com.bbn.akbc.utility.Pair<LearnItObservation,Double>> similarPatterns =
//                ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
//                Double.parseDouble(threshold),
//                cutOff.isEmpty()?Optional.absent():Optional.of(Integer.parseInt(cutOff)),
//                Optional.of(patternsToFilter));
//
//        System.out.println("Returning a list of "+similarPatterns.size()+" similar patterns...");
//        return similarPatterns;
//    }

    @JettyMethod("/ontology/add_target")
    public synchronized String addTarget(@JettyArg("parentNodeId") String parentNodeId, @JettyArg("id") String id, @JettyArg("description") String description, @JettyArg("slot0SpanningType") String slot0SpanningType, @JettyArg("slot1SpanningType") String slot1SpanningType) throws Exception {

        Set<InstanceIdentifier.SpanningType> possibleSlot0SpanningType = new HashSet<>();
        Set<InstanceIdentifier.SpanningType> possibleSlot1SpanningType = new HashSet<>();

        if (slot0SpanningType.contains("Empty")) {
            possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.Empty);
        } else {
            String[] spanningTypes = slot0SpanningType.split(",");
            for (String spanningType : spanningTypes) {
                possibleSlot0SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
            }
        }

        if (slot1SpanningType.contains("Empty")) {
            possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.Empty);
        } else {
            String[] spanningTypes = slot1SpanningType.split(",");
            for (String spanningType : spanningTypes) {
                possibleSlot1SpanningType.add(InstanceIdentifier.SpanningType.valueOf(spanningType));
            }
        }

        BBNInternalOntology.BBNInternalOntologyNode rootNode = null;

        if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.EventMention)) {
                rootNode = this.ontologyMap.get("binaryEvent");
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention) || possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.ValueMention)) {
                rootNode = this.ontologyMap.get("binaryEventEntityOrValueMention");
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                rootNode = this.ontologyMap.get("unaryEvent");
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else if (possibleSlot0SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
            if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Empty)) {
                rootNode = this.ontologyMap.get("unaryEntity");
            } else if (possibleSlot1SpanningType.contains(InstanceIdentifier.SpanningType.Mention)) {
                rootNode = this.ontologyMap.get("binaryEntity");
            } else {
                throw new com.bbn.bue.common.exceptions.NotImplementedException();
            }
        } else {
            throw new com.bbn.bue.common.exceptions.NotImplementedException();
        }
        Map<String, BBNInternalOntology.BBNInternalOntologyNode> nodeIdToNodeMap = rootNode.getPropToNodeMap(new BBNInternalOntology.BBNInternalOntologyNode.PropStringGetter("originalKey"));

        BBNInternalOntology.BBNInternalOntologyNode parent = nodeIdToNodeMap.get(parentNodeId);
        Set<String> existingNodeName = new HashSet<>();
        for (String nodeId : nodeIdToNodeMap.keySet()) {
            BBNInternalOntology.BBNInternalOntologyNode node = nodeIdToNodeMap.get(nodeId);
            existingNodeName.add(node.originalKey);
        }
        if (!org.apache.commons.lang3.StringUtils.isAlphanumeric(id.replace("-", "").replace("_","")) || nodeIdToNodeMap.containsKey(id.toLowerCase()) || existingNodeName.contains(id)) {
            throw new ValidationException("This target id is not acceptable");
        }
        BBNInternalOntology.BBNInternalOntologyNode newNode = new BBNInternalOntology.BBNInternalOntologyNode();
        newNode.originalKey = id;
        newNode._description = description;
        newNode.parent = parent;
        parent.children.add(newNode);
        this.targetNameToOntologyNode.put(id, newNode);
        return "OK";
    }

    @JettyMethod("/init/similar_patterns")
    public synchronized PatternScoreTable getSimilarPatternsNew(@JettyArg("target") String target) throws Exception {
        TargetAndScoreTables extractor = checkGetExtractor(target);
        Epoch.MappingsEntryUnderEpoch mappingsEntryUnderEpoch = this.targetNameToMappingsEntrymap.get(target);
        Mappings info = mappingsEntryUnderEpoch.getMappings();
        PatternScoreTable patternScoreTable = new PatternScoreTable();
        long startTime = System.nanoTime();
        Set<LearnitPattern> frozenGoodLearnitPatterns = new HashSet<>();
        Set<LearnitPattern> frozenLearnitPatterns = new HashSet<>();
        for (AbstractScoreTable.ObjectWithScore<LearnitPattern, PatternScore> objectWithScore : extractor.getPatternScores().getObjectsWithScores()) {
            PatternScore patternScore = objectWithScore.getScore();
            if (patternScore.isFrozen() && patternScore.isGood()) {
                frozenGoodLearnitPatterns.add(objectWithScore.getObject());
            }
            if (patternScore.isFrozen()) {
                frozenLearnitPatterns.add(objectWithScore.getObject());
            }
        }
        System.out.println("[M0]" + (System.nanoTime() - startTime));
        startTime = System.nanoTime();
        int SIZE_LIMIT = 1000; //use a SIZE_LIMIT, otherwise sometimes the extractors are too big to return
        int RETURN_SIZE_LIMIT = 50;
        double threshold = 0.2;

        ObservationSimilarityModule observationSimilarityModule = mappingsEntryUnderEpoch.getObservationSimilarityModule();


        List<Optional<? extends ObservationSimilarity>> patternSimilarityRows = new ArrayList<>();

        for (LearnitPattern learnitPattern : frozenGoodLearnitPatterns) {
            Optional<? extends ObservationSimilarity> learnitPatternSimilarity = observationSimilarityModule.getPatternSimilarity(PatternID.from(learnitPattern));
            patternSimilarityRows.add(learnitPatternSimilarity);
        }
        System.out.println("[M1]" + (System.nanoTime() - startTime));
        startTime = System.nanoTime();
        List<com.bbn.akbc.utility.Pair<LearnItObservation, Double>> similarLearnitPatterns =
                ObservationSimilarity.mergeMultipleSimilarities(patternSimilarityRows,
                        threshold,
                        Optional.of(SIZE_LIMIT),
                        Optional.of(frozenLearnitPatterns));
        System.out.println("[M2]" + (System.nanoTime() - startTime));
        startTime = System.nanoTime();
        Set<String> existingPatternKeywords = new HashSet<>();
        int cnt = 0;
        for (Pair<LearnItObservation, Double> patternObj : similarLearnitPatterns) {
            LearnitPattern learnitPattern = findPattern(info, patternObj.key.toIDString());
            if (learnitPattern == null) {
                continue;
            }
            if (existingPatternKeywords.contains(getPatternKeywordInSimilarity(learnitPattern))) {
                continue;
            }
            existingPatternKeywords.add(getPatternKeywordInSimilarity(learnitPattern));
            patternScoreTable.addDefault(learnitPattern);
            PatternScore score = patternScoreTable.getScore(learnitPattern);
            int frequency = info.getInstancesForPattern(learnitPattern).size();
            score.setFrequency(frequency);

            score.setPrecision(patternObj.value);
//          calculatePatternScoreExtra(learnitPattern, score, extractor, info);
            score.unfreeze();
//            cnt++;
//            if(cnt > RETURN_SIZE_LIMIT)break;
            if (existingPatternKeywords.size() > RETURN_SIZE_LIMIT) {
                break;
            }
        }
        System.out.println("[M3]" + (System.nanoTime() - startTime));
        startTime = System.nanoTime();

        return patternScoreTable;
    }

    @JettyMethod("/init/dictionary_lookup")
    public synchronized Map dictionaryLookup(@JettyArg("word") String word) throws Exception {
        return this.externalDictionary.lookupDictionary(word);
    }

    @JettyMethod("/init/generate_eer_graph")
    public synchronized String generateEERGraph() throws Exception {
        String runtimeDir = Domain.getUIRuntimePath();
        new File(runtimeDir).mkdirs();
        String eerGraphGeneratorLockPath = runtimeDir + File.separator + "eer_graph.lock";
        String eerGraphPath = runtimeDir + File.separator + "eer_graph.json";
        if (new File(eerGraphGeneratorLockPath).exists()) {
            return "Background process is running. Please come back later";
        } else {
            new BufferedWriter(new FileWriter(new File(eerGraphGeneratorLockPath))).close();
//            Mappings mappings = this.originalMappings;
            MapStorage<InstanceIdentifier, Seed> instance2Seed = new HashMapStorage.Builder<InstanceIdentifier, Seed>().build();
            MapStorage.Builder<InstanceIdentifier, LearnitPattern> instance2PatternTableBuilder = new HashMapStorage.Builder<>();
            instance2PatternTableBuilder.putAll(this.originalEpochEntries.get("Event").getMappings().getInstance2Pattern().getStorage());
            instance2PatternTableBuilder.putAll(this.originalEpochEntries.get("Binary-Event").getMappings().getInstance2Pattern().getStorage());
            Mappings mappings = new Mappings(instance2Seed, instance2PatternTableBuilder.build());
            Runnable runnable = () -> {
                try {
                    EERGraphLabeler.LabelingResult labelingResult = EERGraphLabeler.generateGraph(mappings);
                    EERGraphObserver eerGraphObserver = new EERGraphObserver();
                    eerGraphObserver.serializeEERGraph(labelingResult, mappings, eerGraphPath);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    new File(eerGraphGeneratorLockPath).delete();
                }
            };
            Thread myGraphGeneratingThread = new Thread(runnable);
            myGraphGeneratingThread.start();
            return "Job scheduled.";
        }
    }

    public static class InstanceComparatorBySeed implements Comparator<InstanceIdentifier> {
        final Mappings mappings;

        public InstanceComparatorBySeed(Mappings mappings) {
            this.mappings = mappings;
        }

        @Override
        public int compare(InstanceIdentifier o1, InstanceIdentifier o2) {
            String seedFromO1Key = "";
            if (this.mappings.getSeedsForInstance(o1).iterator().hasNext()) {
                seedFromO1Key = this.mappings.getSeedsForInstance(o1).iterator().next().toIDString();
            }
            String seedFromO2Key = "";
            if (this.mappings.getSeedsForInstance(o2).iterator().hasNext()) {
                seedFromO2Key = this.mappings.getSeedsForInstance(o2).iterator().next().toIDString();
            }
            return seedFromO1Key.compareTo(seedFromO2Key);
        }
    }

    public static class ParsedQuery {
        final Set<Symbol> orWords;
        final Set<Symbol> andWords;
        final Set<Symbol> orPreds;

        public ParsedQuery(String queryStr) {
            this.orWords = new HashSet<>();
            this.andWords = new HashSet<>();
            this.orPreds = new HashSet<>();
            boolean inAndArea = false;
            boolean shouldToggleOffInAndArea = false;
            for (String currentToken : queryStr.split(" ")) {
                if (currentToken.contains("\"")) {
                    if (currentToken.chars().filter(ch -> ch == '\"').count() == 2) {
                        inAndArea = true;
                        shouldToggleOffInAndArea = true;
                    } else {
                        if (!inAndArea) {
                            inAndArea = true;
                        } else {
                            shouldToggleOffInAndArea = true;
                        }
                    }

                    currentToken = currentToken.replace("\"", "");
                }
                currentToken = currentToken.trim();
                if (!inAndArea) {
                    if (currentToken.length() > 0) {
                        if (currentToken.startsWith("*")) {
                            orPreds.add(Symbol.from(currentToken.replace("*", "")));
                        } else {
                            orWords.add(Symbol.from(currentToken));
                        }

                    }

                } else {
                    if (currentToken.length() > 0) {
                        if (currentToken.startsWith("*")) {
                            orPreds.add(Symbol.from(currentToken.replace("*", "")));
                        } else {
                            andWords.add(Symbol.from(currentToken));
                        }
                    }
                }
                if (shouldToggleOffInAndArea) {
                    shouldToggleOffInAndArea = false;
                    inAndArea = false;
                }
            }
        }

        public Set<Symbol> getOrWordsSet() {
            return ImmutableSet.copyOf(this.orWords);
        }

        public Set<Symbol> getAndWordsSet() {
            return ImmutableSet.copyOf(this.andWords);
        }

        public Set<Symbol> getOrPreds() {
            return ImmutableSet.copyOf(this.orPreds);
        }
    }
}
