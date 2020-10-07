package com.bbn.akbc.neolearnit.mappings.filters;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.storage.MemberStorageFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.SerifPattern;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.*;

public class BadSerifPatternFilter implements MappingsFilter {

    public static boolean isBadUnaryEventSerifPattern(SerifPattern serifPattern) {
        String prettyString = serifPattern.toPrettyString();
        if (prettyString.contains("[1]")) {
            return true;
        }
        if(prettyString.replace("[0]","").replace(" ","").length()<1){
            return true;
        }

        if (prettyString.contains("said[0]") || prettyString.contains("told[0]") || prettyString.contains("says[0]") || prettyString.contains("reported[0]") || prettyString.contains("reporting[0]") || prettyString.contains("report[0]")) {
            return true;
        }

        if (prettyString.contains("<temp>")) {
            return true;
        }

//        if(serifPattern.getLexicalItems().size()<2)return true;
        String[] tokens = prettyString.split(" ");
        for (String token : tokens) {
            if (token.equals("[OTH]")) {
                return true;
            }
            String escapedToken = token;
            if (token.contains("[")) {
                escapedToken = escapedToken.substring(0, escapedToken.indexOf("["));
                if (escapedToken.equals("he") || escapedToken.equals("she")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBadBinaryEntityEntityPattern(SerifPattern serifPattern){
        String prettyString = serifPattern.toPrettyString();

        return false;
    }

    public static boolean isBadBinaryEventEventSerifPattern(SerifPattern serifPattern) {
        String prettyString = serifPattern.toPrettyString();
        String[] tokens = prettyString.split(" ");
//        for(String token:tokens){
//            if(token.equals("[0]") || token.equals("[1]")){
//                return true;
//            }
//            String escapedToken = token.replace("[0]","").replace("[1]","");
//            if(escapedToken.startsWith("[") && token.endsWith("]")){
//                return true;
//            }
//        }
        Set<String> blacklistedConnectingWord = Sets.newHashSet(
                " ",
                "<obj>",
                "<sub>",
                "to", "and", "the", "of", ",", "in", "that", "with", "on", "is", "while", "for", "when", "a", "by", "has", "at", "will", "which", "it"
        );

        Set<String> stopWordSet = Sets.newHashSet(
                ",", "-", ":", ".",";","↗","/",
                "and", "or","but",
                "the", "a", "an",
                "on", "at", "to", "by", "of", "for", "in",  "with", "on", "from","without","up","over","until","so",
                "is", "are", "was", "were", "been", "be", "'s",
                "must", "might","may",
                "while", "when", "which", "that","how","where",
                "it", "he", "she", "they","their","them","his","her","its",
                "has", "have", "had",
                "will", "would", "as", "although","can","could","through","though",
                "-lrb-","-rrb-",
                "continue","into","also",
                "expect","expected"
        );
        if(prettyString.replace("[0]","").replace("[1]","").replace(" ","").length()<1) {
            return true;
        }

        Set<Symbol> notStopWord = new HashSet<>();
        for(Symbol symbol:serifPattern.getLexicalItems()){
            if(symbol.asString().startsWith("[") && symbol.asString().length() > 2){
                symbol = Symbol.from(symbol.asString().substring(3));
            }
            if(!stopWordSet.contains(symbol.asString().toLowerCase()) && symbol.asString().length()>0){
                notStopWord.add(symbol);
//                System.out.println("AAA "+serifPattern.toPrettyString() + " "+ symbol.asString());
            }
        }

        if(notStopWord.size() <1)return true;

        int slot0Idx = -1;
        int slot1Idx = -1;
        for (int i = 0; i < tokens.length; ++i) {
            if (tokens[i].contains("[0]")) slot0Idx = i;
            if (tokens[i].contains("[1]")) slot1Idx = i;
        }

        if (slot0Idx != -1 && slot1Idx != -1) {
            int minIdx = Math.min(slot0Idx, slot1Idx);
            int maxIdx = Math.max(slot0Idx, slot1Idx);
            if ((maxIdx - minIdx) == 1) return true;
            Set<String> subTokens = new HashSet<>();
            for (int i = minIdx + 1; i < maxIdx; ++i) {
                subTokens.add(tokens[i]);
            }
            if (subTokens.size() < 1) return true;
            if (subTokens.size() == 1) {
                if (Sets.intersection(blacklistedConnectingWord, subTokens).size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum FilteringMode {
        unary_entity,
        unary_event,
        binary_event_event
    }

    FilteringMode filteringMode;

    public BadSerifPatternFilter(FilteringMode filteringMode) {
        this.filteringMode = filteringMode;
    }

    @Override
    public Mappings makeFiltered(Mappings input) {
        Set<LearnitPattern> goodPatterns = new HashSet<>();
        for (LearnitPattern learnitPattern : input.getAllPatterns().elementSet()) {
            if (!(learnitPattern instanceof SerifPattern)) {
                goodPatterns.add(learnitPattern);
            } else {
                boolean isBadPattern;
                switch (filteringMode) {
                    case unary_entity:
                        throw new NotImplementedException();
//                        break;
                    case binary_event_event:
                        isBadPattern = isBadBinaryEventEventSerifPattern((SerifPattern) learnitPattern);
                        if (isBadPattern) {
                            System.out.println("Throwing\t" + learnitPattern.toPrettyString());
                        } else {
                            goodPatterns.add(learnitPattern);
                        }
                        break;
                    case unary_event:
                        isBadPattern = isBadUnaryEventSerifPattern((SerifPattern) learnitPattern);
                        if (isBadPattern) {
                            System.out.println("Throwing\t" + learnitPattern.toPrettyString());
                        } else {
                            goodPatterns.add(learnitPattern);
                        }
                        break;
                }
            }
        }
        MapStorage.Builder<InstanceIdentifier, LearnitPattern> instance2Pattern = input.getInstance2Pattern().getStorage().newBuilder();
        for(LearnitPattern learnitPattern: goodPatterns){
            System.out.println("Keeping\t"+learnitPattern.toPrettyString());
            for(InstanceIdentifier instanceIdentifier: input.getInstancesForPattern(learnitPattern)){
                instance2Pattern.put(instanceIdentifier,learnitPattern);
            }
        }
        InstanceToPatternMapping newPatternMapping = new InstanceToPatternMapping(instance2Pattern.build());

//        final Mappings relevantMapping = new RelevantInstancesFilter().makeFiltered(new Mappings(input.getInstance2Seed(), newPatternMapping));
        return new Mappings(input.getInstance2Seed(), newPatternMapping);
    }

    public static void main(String[] args) throws Exception {
        LearnItConfig.loadParams(new File(args[0]));
        FilteringMode filteringMode = FilteringMode.valueOf(args[1]);
        String mappingsPath = args[2];
        Mappings originalMappings = Mappings.deserialize(new File(mappingsPath), true);
        BadSerifPatternFilter badSerifPatternFilter = new BadSerifPatternFilter(filteringMode);
        Mappings filteredMappings = badSerifPatternFilter.makeFiltered(originalMappings);
        filteredMappings.serialize(new File(args[3]), true);
    }

}
