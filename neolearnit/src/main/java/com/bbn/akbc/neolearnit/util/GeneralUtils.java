package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.DocPathResolver;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.similarity.PatternID;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.actors.ActorMention;
import com.bbn.serif.theories.actors.ActorMentions;
import com.bbn.serif.types.EntityType;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;


import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by msrivast on 5/16/18.
 */
public class GeneralUtils {

    private static Map<String, String> wordLemmaMap = new HashMap<>();
    private static Set<String> stopWordSet = new HashSet<>();

    public static void loadStopWordList() throws IOException{
        stopWordSet.clear();
        for(String line: readLinesIntoList(LearnItConfig.get("stopwords"))){
            stopWordSet.add(line);
        }
    }

    public static boolean inBlackList(String word){
        Set<String> entitiesTypeStr = new HashSet<>();
        entitiesTypeStr.add(EntityType.PER.toString());
        entitiesTypeStr.add(EntityType.ORG.toString());
        entitiesTypeStr.add(EntityType.GPE.toString());
        entitiesTypeStr.add(EntityType.LOC.toString());
        entitiesTypeStr.add(EntityType.FAC.toString());
        entitiesTypeStr.add(EntityType.WEA.toString());
        entitiesTypeStr.add(EntityType.VEH.toString());
        entitiesTypeStr.add(EntityType.TTL.toString());
        entitiesTypeStr.add(EntityType.undetermined().toString());
        entitiesTypeStr.add("OTH");
        if(entitiesTypeStr.contains(word))return true;
        else{
            return stopWordSet.contains(word.toLowerCase());
        }
    }

    public static void WriteMappingsToDirectoryWithTimestamp(Mappings mappings, File outputFolder) throws Exception {
        String timestampStr = Long.toString(System.currentTimeMillis());
        mappings.serialize(Paths.get(outputFolder.getAbsolutePath(), timestampStr + ".sjson").toFile(), true);
    }

    public static Mappings ReadMostRecentMappings(File inputFolder) throws Exception {
        Map<Long, File> timestampToFileMap = new HashMap<>();
        File[] candidates = inputFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith(".sjson")) {
                    timestampToFileMap.put(Long.parseLong(pathname.getName()), pathname);
                    return true;
                }
                return false;
            }
        });

        List<Long> sortedList = new ArrayList<>(timestampToFileMap.keySet());
        Collections.sort(sortedList);
        if (sortedList.size() > 1)
            return Mappings.deserialize(timestampToFileMap.get(sortedList.get(sortedList.size() - 1)), true);
        else return null;
    }

    public static List<String> getLatestExtractorPaths(String learnitRootDir) {
        List<String> filePaths = new ArrayList<>();
        String targetPathDir = String.format("%s/inputs/extractors/", learnitRootDir);
        File dir = new File(targetPathDir);
        if (dir.exists()) {
            for (File subDir : dir.listFiles()) {
                if (subDir.isDirectory()) {
                    String targetName = subDir.getName(); // target name is the directory name
                    String latestFileTimestamp = getLatestExtractor(targetName, subDir).orNull();
                    if (latestFileTimestamp != null) {
                        String fileName = latestFileTimestamp == null ? null : (String.format("%s/%s_%s.json", subDir.getAbsolutePath(),
                                targetName, latestFileTimestamp));
                        filePaths.add(fileName);
                    }
                }
            }
        }
        return filePaths;
    }

    public static Optional<String> getLatestExtractor(String targetName, File dir) {
        List<Long> listDates = new ArrayList<Long>();

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (fileName.startsWith(targetName) && fileName.endsWith(".json")) {
                String date = fileName.substring(targetName.length() + 1, fileName.length() - 5); // remove target from the front, and ".json" from the end
                listDates.add(Long.parseLong(date));
            }
        }
        if (listDates.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(Long.toString(Collections.max(listDates)));
    }

    public static Map<String, TargetAndScoreTables> loadLatestExtractors() throws IOException {
        String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));
        Map<String, TargetAndScoreTables> extractors = new HashMap<>();
        File dir = new File(targetPathDir);
        if (dir.exists()) {
            for (File subDir : dir.listFiles()) {
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(subDir);
                extractors.put(targetAndScoreTables.getTarget().getName(),targetAndScoreTables);
            }
        }
        return extractors;
    }

    public static Map<String, TargetAndScoreTables> loadExtractors(String directory)
            throws IOException {

        Map<String, TargetAndScoreTables> extractors = new HashMap<>();
        File dir = new File(directory);
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                continue;
            String filename = f.getName();
            if (!filename.endsWith(".json"))
                continue;
            TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);
            extractors.put(ex.getTarget().getName(), ex);
        }
        return extractors;
    }

    public static Map<String, TargetAndScoreTables> loadExtractorsFromFileList(String strFileListExtractor) throws IOException {
        Map<String, TargetAndScoreTables> ret = new HashMap<>();
        List<String> filePaths = readLinesIntoList(strFileListExtractor);
        for (String filePath : filePaths) {
            System.out.println("Load extractor: " + filePath);
            TargetAndScoreTables ex = TargetAndScoreTables.deserialize(new File(filePath));
            ret.put(ex.getTarget().getName(), ex);
        }
        return ret;
    }

    /*This method returns a printable extraction instances (with docid, token offsets, slot markups etc)
    along with a string to identify the type for the slot-fillter (event-mention, entity-mention etc.)*/
    public static Optional<Triple<String, String, String>> getInstanceStringWithSlotTypes(InstanceIdentifier instanceIdentifier,
                                                                                          Target target, boolean mustHaveValidSlotTypes) {
        MatchInfo.LanguageMatchInfo matchInfo;
        try {
            matchInfo = instanceIdentifier.reconstructMatchInfo(target).getPrimaryLanguageMatch();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.absent();
        }

        final SynNode rootNode = matchInfo.firstSpanningSentence().parse().root().get();

        //msrivast: TODO: The type for args should be determined by the target.
//        if(!(matchInfo.getSlot0().get() instanceof NodeMentionAnnotation) || !(matchInfo.getSlot1().get() instanceof NodeMentionAnnotation)) {
//            System.out.println("Exception! Arguments are not event mentions");
//            return Optional.absent();
//        }

        Spanning mention1 = matchInfo.getSlot0().get();
        Spanning mention2 = matchInfo.getSlot1().get();

        String mention1Type = getSpanningType(mention1, matchInfo.firstSpanningSentence().actorMentions());
        String mention2Type = getSpanningType(mention2, matchInfo.firstSpanningSentence().actorMentions());

        if (mustHaveValidSlotTypes && !isValidTypeCombination(mention1Type, mention2Type)) {
            return Optional.absent();
        }

        TokenSequence.Span span1 = mention1 instanceof Mention ? ((Mention) mention1).atomicHead().span() :
                (mention1 instanceof ValueMention ? mention1.span() :
                        ((EventMention) mention1).anchorNode().span());
        TokenSequence.Span span2 = mention2 instanceof Mention ? ((Mention) mention2).atomicHead().span() :
                (mention2 instanceof ValueMention ? mention2.span() :
                        ((EventMention) mention2).anchorNode().span());

        String span1Text = span1.tokenizedText().utf16CodeUnits();
        String span2Text = span2.tokenizedText().utf16CodeUnits();
        int span1Start = span1.startCharOffset().asInt();
        int span1End = span1.endCharOffset().asInt();
        int span2Start = span2.startCharOffset().asInt();
        int span2End = span2.endCharOffset().asInt();

        String modifiedMatchInfoString = markedUpTokenString(matchInfo, span1, span2);
        String instanceString = matchInfo.getDocTheory().docid().asString() + "\t" +
                span1Start + "\t" +
                span1End + "\t" +
                span2Start + "\t" +
                span2End + "\t" +
                modifiedMatchInfoString.replace("\n", " ").replace("\t", " ") + "\t" +
                span1Text.replace("\n", " ").replace("\t", " ") + "\t" +
                span2Text.replace("\n", " ").replace("\t", " ") + "\t" +
                matchInfo.markedUpTokenString().replace("\n", " ".replace("\t", " "));

        return Optional.of(new ImmutableTriple<>(instanceString, mention1Type, mention2Type));

    }

    /* msrivast: the following method can be used to control what combination of slot-types make sense
    for learnit decoding. This was added to control incorrect extractions when adding names from a list
    as mentions*/

    public static boolean isValidTypeCombination(String type1, String type2) {
//        if(type1.startsWith("EntMen")&&type2.startsWith("EntMen") ||
//                type1.startsWith("ValMen")&&type2.startsWith("ValMen") ||
//                type1.startsWith("EntMen")&&type2.startsWith("ValMen") ||
//                type1.startsWith("ValMen")&&type2.startsWith("EntMen")){
//            return false;
//        }
        //return true be default for now
        return true;
    }

    public static String getSpanningType(Spanning mention, ActorMentions actorMentions) {
        String type = "EntMen:OTH.UNDET";
        if (mention instanceof Mention) {
            ImmutableSet<ActorMention> actorMentionsForMention = actorMentions.forMention((Mention) mention);
            if (!actorMentionsForMention.isEmpty()) {
                type = actorMentions.get(0).actorName().or(Symbol.from(type)).asString();
                type = "ActMen:" + type;
            } else {
                type = ((Mention) mention).entityType().toString() + "." + ((Mention) mention).mentionType().name();
                type = "EntMen:" + type;
            }
        } else if (mention instanceof ValueMention) {
            type = ((ValueMention) mention).fullType().toString();
            type = "ValMen:" + type;
        } else if (mention instanceof EventMention) {
            type = ((EventMention) mention).type().asString();
            type = "EveMen:" + type;
        }
        return type;
    }

    public static String markedUpTokenString(MatchInfo.LanguageMatchInfo matchInfo, TokenSequence.Span span0, TokenSequence.Span span1) {
        final SynNode rootNode = matchInfo.firstSpanningSentence().parse().root().get();
        SentenceTheory sentTheory = matchInfo.firstSpanningSentence();
        StringBuilder builder = new StringBuilder();
        for (Token t : sentTheory.tokenSequence()) {
            if (t.index() == rootNode.coveringNodeFromTokenSpan(
                    span0).head().span()
                    .startIndex()) {
                builder.append("<SLOT0>");
            }
            if (t.index() == rootNode.coveringNodeFromTokenSpan(
                    span1).head().span()
                    .startIndex()) {
                builder.append("<SLOT1>");
            }
            builder.append(t.text());
            if (t.index() == rootNode.coveringNodeFromTokenSpan(
                    span1).head().span()
                    .endIndex()) {
                builder.append("</SLOT1>");
            }
            if (t.index() == rootNode.coveringNodeFromTokenSpan(
                    span0).head().span()
                    .endIndex()) {
                builder.append("</SLOT0>");
            }
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public static List<String> readLinesIntoList(String file) throws IOException {
        List<String> lines = new ArrayList();
        int nLine = 0;

        BufferedReader reader;
        String sline;
        for (reader = new BufferedReader(new FileReader(file)); (sline = reader.readLine()) != null; lines.add(sline)) {

        }

        reader.close();
        return lines;
    }

    public static void loadWordLemmaMap() throws IOException {
        synchronized (GeneralUtils.class) {
            if (!wordLemmaMap.isEmpty()) {
                return;
            }
            InputStream inputStream = GeneralUtils.class.getClassLoader().getResourceAsStream("lemma.nv");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split(" ");
                if (tokens.length != 2) {
                    continue;
                }
                wordLemmaMap.put(tokens[0].trim(), tokens[1].trim());
            }
            br.close();
        }
    }

    public static String getLemmatizedPattern(String patternString) {
        if (wordLemmaMap.isEmpty()) {
            // if wordLemmaMap has not been loaded by the caller class,
            // just return the original string as is
            return patternString;
        }
        PatternID patternID = PatternID.from(patternString);
        String normalizedPatternStr = patternID.getNormalizedString().replace("=", " = ");
        List<String> lemmatizedPatternTokens = new ArrayList<>();

        for (String token : normalizedPatternStr.split(" ")) {
            String lemmatizedToken = wordLemmaMap.getOrDefault(token, token);
            lemmatizedPatternTokens.add(lemmatizedToken);
        }
        return Joiner.on(" ").join(lemmatizedPatternTokens);
    }

    public static String getLemmatizedPattern(LearnitPattern learnitPattern) {
        return getLemmatizedPattern(learnitPattern.toIDString());
    }

    private static <T> void CombinationDFS(List<T> originalList, int currentIdx, List<T> currentStack, List<List<T>> resultStack, int remaining) {
        if (remaining == 0) {
            resultStack.add(new ArrayList<T>(currentStack));
            return;
        }
        for (int i = currentIdx; i < originalList.size(); ++i) {
            currentStack.add(originalList.get(i));
            CombinationDFS(originalList, i + 1, currentStack, resultStack, remaining - 1);
            currentStack.remove(currentStack.size() - 1);
        }
    }

    public static <T> List<List<T>> Combination(List<T> originalList, int k) {
        List<List<T>> ret = new ArrayList<>();
        if (originalList.size() < k) return ret;
        CombinationDFS(originalList, 0, new ArrayList<T>(), ret, k);
        return ret;
    }



    public static Set<DocTheory> resolvedDocTheoryFromInstanceIdentifier(Collection<InstanceIdentifier> instanceIdentifiers) throws IOException, InterruptedException, ExecutionException {

        Set<String> docPathSet = new HashSet<>();
        for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
            String docPath = new File(DocPathResolver.Monolingual.getPath(instanceIdentifier.getDocid())).getAbsolutePath();
            docPathSet.add(docPath);
        }
        return LoaderUtils.resolvedDocTheoryFromPathList(docPathSet);
    }



    public static Set<BilingualDocTheory> resolvedBiDocTheoryFromInstanceIdentifier(Collection<InstanceIdentifier> instanceIdentifiers) throws IOException, ExecutionException, InterruptedException {
        Set<String> docIds = new HashSet<>();
        for(InstanceIdentifier instanceIdentifier:instanceIdentifiers){
            docIds.add(instanceIdentifier.getDocid());
        }
        List<Map<String,String>> biEntries = new ArrayList<>();
        for(String docId:docIds){
            biEntries.add(DocPathResolver.Bilingual.getPath(docId));
        }
        return LoaderUtils.resolveBilingualDocTheoryFromBiEntries(biEntries);
    }

}
