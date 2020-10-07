package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.utility.FileUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CompareExtractors {

    public static TargetAndScoreTables loadExtractor(String strFile) throws IOException {

        System.out.println("load extractor: " + strFile);

        TargetAndScoreTables extractor =
                TargetAndScoreTables.deserialize(new File(strFile));

        return extractor;
    }

    private static Map<String, String> createTargetNameToExFileMap(List<String> extractors) {
        Map<String, String> retVal = new LinkedHashMap<>();
        for (String extractor : extractors) {
            String name = extractor.substring(extractor.lastIndexOf(File.separator) + 1);
            name = name.substring(0, name.indexOf("_"));
            retVal.put(name, extractor);
        }
        return retVal;
    }

    private static List<String> compareExtractors(TargetAndScoreTables ex1, TargetAndScoreTables ex2) {
        return compareExtractors(ex1, ex2, true);
    }

    private static List<String> compareExtractors(TargetAndScoreTables ex1, TargetAndScoreTables ex2, boolean patternsOnly) {
        if (!patternsOnly) {
            throw new UnsupportedOperationException();
        }
        List<String> output = new ArrayList<>();
        output.add("TargetName: " + ex1.getTarget().getName());
        output.add("Patterns in 1: " + ex1.getPatternScores().getFrozen().size() + ", good patterns: " +
                ex1.getPatternScores().getFrozen().stream().filter((LearnitPattern pattern) -> ex1.getPatternScores().getScore(pattern).isGood()).count());
        output.add("Patterns in 2: " + ex2.getPatternScores().getFrozen().size() + ", good patterns: " +
                ex2.getPatternScores().getFrozen().stream().filter((LearnitPattern pattern) -> ex2.getPatternScores().getScore(pattern).isGood()).count());
        return output;
    }

    public static void main(String[] args) throws IOException {

        String params = args[0];
        String strExtractorList1 = args[1];

        LearnItConfig.loadParams(new File(params));

        List<String> extractorList1 = FileUtil.readLinesIntoList(strExtractorList1);
        Map<String, String> targetNameToExFile1 = createTargetNameToExFileMap(extractorList1);

        Map<String, TargetAndScoreTables> latestExtractors = loadLatestExtractors();
        List<String> output = new ArrayList<>();
        for (String targetName : targetNameToExFile1.keySet()) {
            TargetAndScoreTables ex1 = loadExtractor(targetNameToExFile1.get(targetName));
            TargetAndScoreTables ex2 = latestExtractors.get(targetName);
            output.addAll(compareExtractors(ex1, ex2));
            output.add("\n");
        }
        System.out.println("\n\n");
        System.out.println(Joiner.on("\n").join(output));
    }

    private static Map<String, TargetAndScoreTables> loadLatestExtractors() throws IOException {
        String targetPathDir = String.format("%s/inputs/extractors/", LearnItConfig.get("learnit_root"));
        Map<String, TargetAndScoreTables> extractors = new HashMap<>();
        File dir = new File(targetPathDir);
        if (dir.exists()) {
            for (File subDir : dir.listFiles()) {
                TargetAndScoreTables targetAndScoreTables = TargetAndScoreTables.deserialize(subDir);
                extractors.put(targetAndScoreTables.getTarget().getName(), targetAndScoreTables);
            }
        }
        return extractors;
    }

    private static Optional<String> getLatestExtractor(String targetName, File dir) {
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
}
