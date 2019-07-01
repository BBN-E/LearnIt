package com.bbn.akbc.neolearnit.common.matchinfo;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.util.PathConverter;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DocQueryDisplay {

	@JsonProperty
	private final Target target;
    @JsonProperty
    private final String docid;
    @JsonProperty
    private final String query;
    @JsonProperty
    private final int slot;
//    @JsonProperty
//    private final List<String> html;
//
//    @JsonProperty
//    private EfficientMultimapDataStore<Integer,String> sentenceEntityMap() {
//        return EfficientMultimapDataStore.fromMultimap(sentenceEntityMap);
//    }
    @JsonProperty
    private final List<SentenceWindow> sentWindows;

    private final Multimap<Integer,Integer> queryStartMap = HashMultimap.create();
    private final Multimap<Integer,Integer> queryEndMap = HashMultimap.create();

    private final Multimap<Integer,String> sentenceEntityMap = HashMultimap.create();

    private final Map<Integer,List<String>> sentenceTokensMap = new HashMap<Integer, List<String>>();

    private static Map<String,DocTheory> cachedDocTheories = new ConcurrentHashMap<String,DocTheory>();

    public static final Set<String> FP_PRONOUNS = ImmutableSet.of("we","us","our");

	public DocQueryDisplay(Target target,String docid,Map<InstanceIdentifier,Seed> instances,String query,int slot) throws IOException {
		this.target = target;
        this.docid = docid;
		this.query = query;
        this.slot = slot;

        for (InstanceIdentifier id : instances.keySet()) {
            if (slot==0) {
                queryStartMap.put(id.getSentid(),id.getSlot0Start());
                queryEndMap.put(id.getSentid(),id.getSlot0End());
            }
            else {
                queryStartMap.put(id.getSentid(),id.getSlot1Start());
                queryEndMap.put(id.getSentid(),id.getSlot1End());
            }

            Optional<Mention.Type> mentionType = id.getSlotMentionType(1-slot);
            if (!mentionType.isPresent() || mentionType.get() == Mention.Type.NAME || mentionType.get() == Mention.Type.DESC)
                sentenceEntityMap.put(id.getSentid(),instances.get(id).getSlotHeadText(1-slot).toString());
            else if (mentionType.get() == Mention.Type.PRON) {
                if (FP_PRONOUNS.contains(instances.get(id).getSlotHeadText(1-slot).toString()))
                    sentenceEntityMap.put(id.getSentid(),instances.get(id).getStringSlots().get(1-slot));
            }
        }

        constructDocInfo();
        this.sentWindows = buildSentenceWindows();
	}

    @JsonCreator
    public DocQueryDisplay(@JsonProperty("target") Target target, @JsonProperty("docid") String docid,
                           @JsonProperty("query") String query, @JsonProperty("slot") int slot,
                           @JsonProperty("sentWindows") List<SentenceWindow> sentWindows) throws IOException {
        this.target = target;
        this.docid = docid;
        this.query = query;
        this.slot = slot;
        this.sentWindows = sentWindows;
    }

    public void constructDocInfo() throws IOException {
        if (!cachedDocTheories.containsKey(docid)) {
            File docFile = PathConverter.getFile(docid);
            SerifXMLLoader loader = SerifXMLLoader.createFrom(LearnItConfig.params());
            DocTheory newDT = loader.loadFrom(docFile);
            cachedDocTheories.put(docid, newDT);
        }

        DocTheory dt = cachedDocTheories.get(docid);
        for (SentenceTheory sent : dt.nonEmptySentenceTheories()) {
            List<String> tokens = new ArrayList<String>();
            for (Token t : sent.tokenSequence()) {
                tokens.add(sanitize(t.tokenizedText().utf16CodeUnits()));
            }
            sentenceTokensMap.put(sent.index(), tokens);
        }
    }

	public static String sanitize(String input) {
		return input.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;").replace("\n", "<br />").replace("\t", "&nbsp;&nbsp;&nbsp;")
				.replace("@-@", "-").replace("-LRB-","&#40;").replace("-RRB-","&#41;").replace("numbercommontoken", "&#35;");
	}

	public Target getTarget() {
		return target;
	}

    private List<List<Integer>> getConsecutiveSentences(List<Integer> sortedSents) {
        List<List<Integer>> toReturn = new ArrayList<List<Integer>>();
        List<Integer> current = new ArrayList<Integer>();
        int last = sortedSents.get(0);
        current.add(last);
        if (sortedSents.size() == 1) {
            toReturn.add(current);
            return toReturn;
        }
        for (int sent : sortedSents.subList(1,sortedSents.size()-1)) {
            if (sent == last+1) current.add(sent);
            else {
                toReturn.add(current);
                current = new ArrayList<Integer>();
                current.add(sent);
            }
            last = sent;
        }
        if (!current.isEmpty()) toReturn.add(current);
        return toReturn;
    }

    public List<SentenceWindow> buildSentenceWindows() {
        List<Integer> querySents = new ArrayList<Integer>(queryStartMap.keySet());
        Collections.sort(querySents);

        //FIND THE RIGHT SPANS
        List<List<Integer>> windows = getConsecutiveSentences(querySents);
        String span = slot==0 ? "<span class=\"slot0\">" : "<span class=\"slot1\">";

        List<SentenceWindow> toReturn = new ArrayList<SentenceWindow>();

        for (List<Integer> window : windows) {
            StringBuilder builder = new StringBuilder();
            //i = sentence number
            for (int i = Math.max(window.get(0)-3, 0); i <= Math.min(window.get(window.size()-1)+3, sentenceTokensMap.size()-1); i++) {
                if (!sentenceTokensMap.containsKey(i)) continue;
                List<String> tokens = sentenceTokensMap.get(i);
                //j = token index w/in sentence
                for (int j = 0; j < tokens.size(); j++) {

                    if (queryStartMap.get(i).contains(j))
                        builder.append(span);

                    builder.append(tokens.get(j));

                    if (queryEndMap.get(i).contains(j))
                        builder.append("</span>");

                    builder.append(" ");
                }
            }
            String html = builder.toString();

            Set<String> entities = new HashSet<String>();
            for (int i = window.get(0); i <= window.get(window.size()-1); i++) {
                entities.addAll(sentenceEntityMap.get(i));
            }

            toReturn.add(new SentenceWindow(html,entities));
        }
        return toReturn;
    }

    public static class SentenceWindow {
        @JsonProperty
        private final String html;
        @JsonProperty
        private final Set<String> entities;

        @JsonCreator
        public SentenceWindow(@JsonProperty("html") String html, @JsonProperty("entities") Set<String> entities) {
            this.html = html;
            this.entities = entities;
        }
    }

}
