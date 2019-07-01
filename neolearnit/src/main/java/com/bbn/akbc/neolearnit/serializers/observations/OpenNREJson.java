package com.bbn.akbc.neolearnit.serializers.observations;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.label.LabelPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenNREJson {

    @JsonIgnoreProperties(ignoreUnknown = true)
    class RelationSpan{
        @JsonProperty
        final String word;
        @JsonProperty
        final String id;
        RelationSpan(String word,String id){
            this.word = word;
            this.id = id;
        }
        RelationSpan(Symbol word,Symbol id){
            this.word = word.asString();
            this.id = id.asString();
        }

        @Override
        public boolean equals(Object o){
            if(!(o instanceof  RelationSpan))return false;
            RelationSpan that = (RelationSpan)o;
            return this.word.equals(that.word);
        }
    }

    @JsonProperty
    final String sentence;
    @JsonProperty
    final RelationSpan head;
    @JsonProperty
    final RelationSpan tail;
    @JsonProperty
    final String relation;


    // This section is reserve for causal json, DO NOT CHANGE FIELD NAME!!!!!!!!
    @JsonProperty
    final String docid;
    @JsonProperty
    final ImmutableList<ImmutableList<Integer>> arg1_span_list;
    @JsonProperty
    final String arg1_text;
    @JsonProperty
    final ImmutableList<ImmutableList<Integer>> arg2_span_list;
    @JsonProperty
    final String arg2_text;
    @JsonProperty
    final String connective_text;
    @JsonProperty
    final String relation_type;
    @JsonProperty
    final String semantic_class;
    @JsonProperty
    final ImmutableList<LearnitPattern> learnit_pattern;
    // Barrier end


    @JsonProperty
    final int sentid;
    @JsonProperty
    final int slot0StartIdx;
    @JsonProperty
    final int slot0EndIdx;
    @JsonProperty
    final int slot1StartIdx;
    @JsonProperty
    final int slot1EndIdx;
    // This two may be event type or entity type
    // For more details, please refer ti InstanceIdentifier
    @JsonProperty
    final String slot0EntityType;
    @JsonProperty
    final String slot1EntityType;
    // Barrier end

    @JsonProperty
    final int sentStartCharOff;
    @JsonProperty
    final int sentEndCharOff;

    public OpenNREJson(final EventMention left, final EventMention right, LabelPattern labelPattern, InstanceIdentifier instanceIdentifier, SentenceTheory sentenceTheory) {
        this.sentence = sentenceTheory.span().tokenizedText().utf16CodeUnits();
        final String slot0IdString = getSlotIdString(left);
        final String slot0String = left.anchorNode().span().tokenizedText().utf16CodeUnits();
        final String slot1IdString = getSlotIdString(right);
        final String slot1String = right.anchorNode().span().tokenizedText().utf16CodeUnits();
        this.head = new RelationSpan(slot0String,String.format("%s|%s",left.type().asString(),slot0IdString));
        this.tail = new RelationSpan(slot1String,String.format("%s|%s",right.type().asString(),slot1IdString));
        this.relation = labelPattern.getLabel();
        this.docid = instanceIdentifier.getDocid();
        this.arg1_span_list = ImmutableList.of(ImmutableList.of(left.anchorNode().span().startCharOffset().asInt(), left.anchorNode().span().endCharOffset().asInt()));
        this.arg2_span_list = ImmutableList.of(ImmutableList.of(right.anchorNode().span().startCharOffset().asInt(), right.anchorNode().span().endCharOffset().asInt()));
        this.arg1_text = left.anchorNode().span().tokenizedText().utf16CodeUnits();
        this.arg2_text = right.anchorNode().span().tokenizedText().utf16CodeUnits();
        this.connective_text = "";
        this.relation_type = "Explicit";
        this.semantic_class = labelPattern.getLabel();
        this.learnit_pattern = ImmutableList.of();

        this.sentid = instanceIdentifier.getSentid();
        this.slot0StartIdx = instanceIdentifier.getSlot0Start();
        this.slot0EndIdx = instanceIdentifier.getSlot0End();
        this.slot1StartIdx = instanceIdentifier.getSlot1Start();
        this.slot1EndIdx = instanceIdentifier.getSlot1End();
        this.slot0EntityType = instanceIdentifier.getSlotEntityType(0);
        this.slot1EntityType = instanceIdentifier.getSlotEntityType(1);

        this.sentStartCharOff = sentenceTheory.span().startCharOffset().asInt();
        this.sentEndCharOff = sentenceTheory.span().endCharOffset().asInt();
    }

    @JsonCreator
    public OpenNREJson(@JsonProperty final String sentence, @JsonProperty final RelationSpan head,
                       @JsonProperty final RelationSpan tail, @JsonProperty final String relation,
                       @JsonProperty final String docid, @JsonProperty final ImmutableList<ImmutableList<Integer>> arg1_span_list, @JsonProperty final String arg1_text, @JsonProperty final ImmutableList<ImmutableList<Integer>> arg2_span_list, @JsonProperty final String arg2_text, @JsonProperty final String connective_text, @JsonProperty final String relation_type, @JsonProperty final String semantic_class, @JsonProperty final ImmutableList<LearnitPattern> learnit_pattern, @JsonProperty final int sentid, @JsonProperty final int slot0StartIdx, @JsonProperty final int slot0EndIdx, @JsonProperty final int slot1StartIdx, @JsonProperty final int slot1EndIdx, @JsonProperty final String slot0EntityType, @JsonProperty final String slot1EntityType, @JsonProperty int sentStartCharOff, @JsonProperty int sentEndCharOff, @JsonProperty final String rawOriginalText
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
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpenNREJson)) return false;
        OpenNREJson that = (OpenNREJson) o;
        return this.head.equals(that.head) && this.tail.equals(that.tail) && this.sentence.equals(that.sentence) && this.relation.equals(that.relation);
    }
    private String getSlotIdString(EventMention em) {
        if (em.pattern().isPresent()) 
            return em.pattern().get().asString();
        else
            return em.anchorNode().head().span().tokenizedText().utf16CodeUnits();
    }
}
