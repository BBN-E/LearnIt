package com.bbn.akbc.neolearnit.serializers.observations;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenNREJson {

    @JsonProperty("arg1_span_list")
    final public List<List<Integer>> arg1_span_list;
    @JsonProperty("arg1_anchor_span_list")
    final public List<List<Integer>> arg1_anchor_span_list;

    @JsonProperty("sentence")
    final public String sentence;
    @JsonProperty("head")
    final public RelationSpan head;
    @JsonProperty("tail")
    final public RelationSpan tail;
    @JsonProperty("relation")
    final public String relation;


    // This section is reserve for causal json, DO NOT CHANGE FIELD NAME!!!!!!!!
    @JsonProperty("docid")
    final public String docid;
    @JsonProperty("arg2_span_list")
    final public List<List<Integer>> arg2_span_list;
    @JsonProperty("arg2_anchor_span_list")
    final public List<List<Integer>> arg2_anchor_span_list;
    @JsonProperty("arg1_text")
    final public String arg1_text;
    @JsonProperty("learnit_pattern")
    final public List<String> learnit_pattern;
    @JsonProperty("arg2_text")
    final public String arg2_text;
    @JsonProperty("connective_text")
    final public String connective_text;
    @JsonProperty("relation_type")
    final public String relation_type;
    @JsonProperty("semantic_class")
    final public String semantic_class;
    @JsonProperty("sentid")
    final public int sentid;
    // Barrier end
    @JsonProperty("slot0StartIdx")
    final public int slot0StartIdx;
    @JsonProperty("slot0EndIdx")
    final public int slot0EndIdx;
    @JsonProperty("slot0AnchorStartIdx")
    final public int slot0AnchorStartIdx;
    @JsonProperty("slot0AnchorEndIdx")
    final public int slot0AnchorEndIdx;
    @JsonProperty("slot1StartIdx")
    final public int slot1StartIdx;
    @JsonProperty("slot1EndIdx")
    final public int slot1EndIdx;
    @JsonProperty("slot1AnchorStartIdx")
    final public int slot1AnchorStartIdx;
    @JsonProperty("slot1AnchorEndIdx")
    final public int slot1AnchorEndIdx;
    // This two may be event type or entity type
    // For more details, please refer ti InstanceIdentifier
    @JsonProperty("slot0EntityType")
    final public String slot0EntityType;
    @JsonProperty("slot1EntityType")
    final public String slot1EntityType;
    @JsonProperty("sentStartCharOff")
    final public int sentStartCharOff;
    // Barrier end
    @JsonProperty("sentEndCharOff")
    final public int sentEndCharOff;
    @JsonCreator
    public OpenNREJson(@JsonProperty("sentence") final String sentence,
                       @JsonProperty("head") final RelationSpan head,
                       @JsonProperty("tail") final RelationSpan tail,
                       @JsonProperty("relation") final String relation,
                       @JsonProperty("docid") final String docid,
                       @JsonProperty("arg1_span_list") final List<List<Integer>> arg1_span_list,
                       @JsonProperty("arg1_text") final String arg1_text,
                       @JsonProperty("arg2_span_list") final List<List<Integer>> arg2_span_list,
                       @JsonProperty("arg2_text") final String arg2_text,
                       @JsonProperty("connective_text") final String connective_text,
                       @JsonProperty("relation_type") final String relation_type,
                       @JsonProperty("semantic_class") final String semantic_class,
                       @JsonProperty("learnit_pattern") final List<String> learnit_pattern,
                       @JsonProperty("sentid") final int sentid,
                       @JsonProperty("slot0StartIdx") final int slot0StartIdx,
                       @JsonProperty("slot0EndIdx") final int slot0EndIdx,
                       @JsonProperty("slot1StartIdx") final int slot1StartIdx,
                       @JsonProperty("slot1EndIdx") final int slot1EndIdx,
                       @JsonProperty("slot0EntityType") final String slot0EntityType,
                       @JsonProperty("slot1EntityType") final String slot1EntityType,
                       @JsonProperty("sentStartCharOff") int sentStartCharOff,
                       @JsonProperty("sentEndCharOff") int sentEndCharOff,
                       @JsonProperty("slot0AnchorStartIdx") int slot0AnchorStartIdx,
                       @JsonProperty("slot0AnchorEndIdx") int slot0AnchorEndIdx,
                       @JsonProperty("slot1AnchorStartIdx") int slot1AnchorStartIdx,
                       @JsonProperty("slot1AnchorEndIdx") int slot1AnchorEndIdx,
                       @JsonProperty("arg1_anchor_span_list")  List<List<Integer>> arg1_anchor_span_list,
                       @JsonProperty("arg2_anchor_span_list")  List<List<Integer>> arg2_anchor_span_list
    ) {
        this.sentence = sentence;
        this.head = head;
        this.tail = tail;
        this.relation = relation;
        this.docid = docid;
        this.arg1_span_list = arg1_span_list;
        this.arg1_text = arg1_text;
        this.arg2_span_list = arg2_span_list;
        this.arg2_text = arg2_text;
        this.connective_text = connective_text;
        this.relation_type = relation_type;
        this.semantic_class = semantic_class;
        this.learnit_pattern = learnit_pattern;
        this.sentid = sentid;
        this.slot0StartIdx = slot0StartIdx;
        this.slot0EndIdx = slot0EndIdx;
        this.slot1StartIdx = slot1StartIdx;
        this.slot1EndIdx = slot1EndIdx;
        this.slot0EntityType = slot0EntityType;
        this.slot1EntityType = slot1EntityType;
        this.sentStartCharOff = sentStartCharOff;
        this.sentEndCharOff = sentEndCharOff;
        this.slot0AnchorStartIdx = slot0AnchorStartIdx;
        this.slot0AnchorEndIdx = slot0AnchorEndIdx;
        this.slot1AnchorStartIdx = slot1AnchorStartIdx;
        this.slot1AnchorEndIdx = slot1AnchorEndIdx;
        this.arg1_anchor_span_list = arg1_anchor_span_list;
        this.arg2_anchor_span_list = arg2_anchor_span_list;
    }

    public static List<OpenNREJson> createOpenNREJsonFromEventMention(final EventMention leftEventMention, final EventMention rightEventMention, LabelPattern labelPattern, InstanceIdentifier instanceIdentifier, SentenceTheory sentenceTheory, List<Mappings> patternMappingsList, Set<LearnitPattern> goodPatterns) {
        List<OpenNREJson> ret = new ArrayList<>();
        String sentence = sentenceTheory.span().originalText().content().utf16CodeUnits();
        List<SynNode> leftSynNodes = InstanceIdentifier.getNode(leftEventMention, null);
        List<SynNode> rightSynNodes = InstanceIdentifier.getNode(rightEventMention, null);
        Set<String> leftEventTypes = new HashSet<>();
        Set<String> rightEventTypes = new HashSet<>();

        for (EventMention.EventType eventType : leftEventMention.eventTypes()) {
            leftEventTypes.add(eventType.eventType().asString());
        }
        if (leftEventTypes.size() < 1) {
            leftEventTypes.add(leftEventMention.type().asString());

        }
        for (EventMention.EventType eventType : rightEventMention.eventTypes()) {
            rightEventTypes.add(eventType.eventType().asString());
        }
        if (rightEventTypes.size() < 1) {
            rightEventTypes.add(rightEventMention.type().asString());
        }


        for (SynNode leftSynNode : leftSynNodes) {
            for (SynNode rightSynNode : rightSynNodes) {
                final String slot0IdString = getSlotIdString(leftEventMention, leftSynNode);
                final String slot0String = leftSynNode.span().tokenizedText().utf16CodeUnits();
                final String slot1IdString = getSlotIdString(rightEventMention, rightSynNode);
                final String slot1String = rightSynNode.span().tokenizedText().utf16CodeUnits();
                RelationSpan head = new RelationSpan(slot0String, String.format("%s|%s", leftEventTypes.stream().collect(Collectors.joining(",")), slot0IdString));
                RelationSpan tail = new RelationSpan(slot1String, String.format("%s|%s", rightEventTypes.stream().collect(Collectors.joining(",")), slot1IdString));
                String relation = labelPattern.getLabel();
                String docid = instanceIdentifier.getDocid();
                // The char off below is correct
                List<List<Integer>> arg1_span_list = ImmutableList.of(ImmutableList.of(leftEventMention.span().startCharOffset().asInt(), leftEventMention.span().endCharOffset().asInt()));
                List<List<Integer>> arg1_anchor_span_list = ImmutableList.of(ImmutableList.of(leftSynNode.span().startCharOffset().asInt(), leftSynNode.span().endCharOffset().asInt()));


                // The char off below is correct
                List<List<Integer>> arg2_span_list = ImmutableList.of(ImmutableList.of(rightEventMention.span().startCharOffset().asInt(), rightEventMention.span().endCharOffset().asInt()));
                List<List<Integer>> arg2_anchor_span_list = ImmutableList.of(ImmutableList.of(rightSynNode.span().startCharOffset().asInt(), rightSynNode.span().endCharOffset().asInt()));
                String arg1_text = leftSynNode.span().tokenizedText().utf16CodeUnits();
                String arg2_text = rightSynNode.span().tokenizedText().utf16CodeUnits();
                String connective_text = "";
                String relation_type = "Explicit";
                String semantic_class = labelPattern.getLabel();
                List<String> learnit_pattern = new ArrayList<>();
                if ((patternMappingsList != null) && (goodPatterns != null)) {
                    Set<LearnitPattern> patterns = new HashSet<>();
                    for (Mappings mappings : patternMappingsList) {
                        patterns.addAll(Sets.intersection(new HashSet<>(mappings.getPatternsForInstance(instanceIdentifier)), goodPatterns));
                    }
                    learnit_pattern.addAll(patterns.stream().map(LearnitPattern::toIDString).collect(Collectors.toSet()));
                }

                int sentid = instanceIdentifier.getSentid();
                int slot0StartIdx = instanceIdentifier.getSlot0Start();
                int slot0EndIdx = instanceIdentifier.getSlot0End();
                int slot0AnchorStartIdx = leftSynNode.span().startToken().index();
                int slot0AnchorEndIdx = leftSynNode.span().endToken().index();

                int slot1StartIdx = instanceIdentifier.getSlot1Start();
                int slot1EndIdx = instanceIdentifier.getSlot1End();
                int slot1AnchorStartIdx = rightSynNode.span().startToken().index();
                int slot1AnchorEndIdx = rightSynNode.span().endToken().index();


                String slot0EntityType = instanceIdentifier.getSlotEntityType(0);
                String slot1EntityType = instanceIdentifier.getSlotEntityType(1);

                int sentStartCharOff = sentenceTheory.span().startCharOffset().asInt();
                int sentEndCharOff = sentenceTheory.span().endCharOffset().asInt();
                ret.add(new OpenNREJson(sentence, head, tail, relation, docid, arg1_span_list, arg1_text, arg2_span_list, arg2_text, connective_text, relation_type, semantic_class, learnit_pattern, sentid, slot0StartIdx, slot0EndIdx, slot1StartIdx, slot1EndIdx, slot0EntityType, slot1EntityType, sentStartCharOff, sentEndCharOff,slot0AnchorStartIdx,slot0AnchorEndIdx,slot1AnchorStartIdx,slot1AnchorEndIdx,arg1_anchor_span_list,arg2_anchor_span_list));
            }
        }
        return ret;
    }

    private static String getSlotIdString(EventMention em, SynNode headNode) {
        if (em.pattern().isPresent())
            return em.pattern().get().asString();
        else
            return headNode.head().span().tokenizedText().utf16CodeUnits();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenNREJson)) return false;
        OpenNREJson that = (OpenNREJson) o;
        return this.head.equals(that.head) && this.tail.equals(that.tail) && this.sentence.equals(that.sentence) && this.relation.equals(that.relation);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class RelationSpan {
        @JsonProperty("word")
        final public String word;
        @JsonProperty("id")
        final public String id;

        @JsonCreator
        RelationSpan(@JsonProperty("word") String word, @JsonProperty("id") String id) {
            this.word = word;
            this.id = id;
        }

        RelationSpan(Symbol word, Symbol id) {
            this.word = word.asString();
            this.id = id.asString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RelationSpan)) return false;
            RelationSpan that = (RelationSpan) o;
            return this.word.equals(that.word);
        }
    }
}
