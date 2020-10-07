package com.bbn.akbc.neolearnit.SerifAnnotationDataTemplate;

import com.bbn.akbc.neolearnit.common.Annotation;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class SerifAnnotationData {
    @JsonProperty("event_mentions")
    public final List<NodeMentionAnnotation> eventMentionAnnotations;
    @JsonProperty("event_event_relation_mentions")
    public final List<EdgeMentionAnnotation> eventEventRelationMentionAnnotations;
    @JsonProperty("event_event_argument_relation_mentions")
    public final List<EdgeMentionAnnotation> eventEventArgumentRelationMentionAnnotations;
    @JsonProperty("doc_id")
    public final String docId;

    @JsonCreator
    public SerifAnnotationData(@JsonProperty("doc_id") String docId, @JsonProperty("event_mentions") List<NodeMentionAnnotation> eventMentionAnnotations, @JsonProperty("event_event_relation_mentions") List<EdgeMentionAnnotation> eventEventRelationMentionAnnotations, @JsonProperty("event_event_argument_relation_mentions") List<EdgeMentionAnnotation> eventEventArgumentRelationMentionAnnotations) {
        this.docId = docId;
        this.eventMentionAnnotations = eventMentionAnnotations;
        this.eventEventRelationMentionAnnotations = eventEventRelationMentionAnnotations;
        this.eventEventArgumentRelationMentionAnnotations = eventEventArgumentRelationMentionAnnotations;
    }

    public SerifAnnotationData(@JsonProperty("doc_id") String docId) {
        this.docId = docId;
        this.eventMentionAnnotations = new ArrayList<>();
        this.eventEventRelationMentionAnnotations = new ArrayList<>();
        this.eventEventArgumentRelationMentionAnnotations = new ArrayList<>();
    }

    public static NodeMentionAnnotation getNodeMentionAnnotation(List<NodeMentionAnnotation> nodeMentionAnnotationList, Span span) {
        NodeMentionAnnotation ret = null;
        for (NodeMentionAnnotation nodeMentionAnnotation : nodeMentionAnnotationList) {
            if (nodeMentionAnnotation.span.equals(span)) {
                ret = nodeMentionAnnotation;
                break;
            }
        }
        return ret;
    }

    public static class AnnotationEntry{
        @JsonProperty("source")
        public final String source;
        @JsonProperty("markings")
        public final List<Marking> markings;
        public AnnotationEntry(String source){
            this.source = source;
            this.markings = new ArrayList<>();
        }
        @JsonCreator
        public AnnotationEntry(@JsonProperty("source") String source, @JsonProperty("markings") List<Marking> markings) {
            this.source = source;
            this.markings = markings;
        }
    }

    public static class Span{
        @JsonProperty("start_char_off")
        final int startCharOff;
        @JsonProperty("end_char_off")
        final int endCharOff;
        @JsonProperty("sent_start_char_off")
        final int sentStartCharOff;
        @JsonProperty("sent_end_char_off")
        final int sentEndCharOff;

        @JsonCreator
        public Span(@JsonProperty("start_char_off") int startCharOff,@JsonProperty("end_char_off") int endCharOff,@JsonProperty("sent_start_char_off") int sentStartCharOff, @JsonProperty("sent_end_char_off") int sentEndCharOff){
            this.startCharOff = startCharOff;
            this.endCharOff = endCharOff;
            this.sentStartCharOff = sentStartCharOff;
            this.sentEndCharOff = sentEndCharOff;
        }

        @Override
        public boolean equals(Object o){
            if(!(o instanceof Span))return false;
            Span that = (Span)o;
            return that.startCharOff == this.startCharOff && that.endCharOff == this.endCharOff && that.sentStartCharOff == this.sentStartCharOff && that.sentEndCharOff == this.sentEndCharOff;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int ret = this.startCharOff;
            ret = ret * prime + this.endCharOff;
            ret = ret * prime + this.sentStartCharOff;
            ret = ret * prime + this.sentEndCharOff;
            return ret;
        }
    }

    public static class NodeMentionAnnotation {
        @JsonProperty("annotation_entries")
        public final List<AnnotationEntry> annotationEntries;

        @JsonProperty("span")
        final Span span;


        @JsonCreator
        public NodeMentionAnnotation(@JsonProperty("span") Span span, @JsonProperty("annotation_entries") List<AnnotationEntry> annotationEntries) {
            this.span = span;
            this.annotationEntries = annotationEntries;

        }

        public NodeMentionAnnotation(Span span) {
            this.span = span;
            this.annotationEntries = new ArrayList<>();

        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeMentionAnnotation)) return false;
            NodeMentionAnnotation that = (NodeMentionAnnotation) o;
            return that.span.equals(this.span);
        }

    }

    public static class EdgeMentionAnnotation {
        @JsonProperty("annotation_entries")
        public final List<AnnotationEntry> annotationEntries;

        @JsonProperty("left_span")
        final Span leftSpan;

        @JsonProperty("right_span")
        final Span rightSpan;

        @JsonCreator
        public EdgeMentionAnnotation(@JsonProperty("annotation_entries") List<AnnotationEntry> annotationEntries, @JsonProperty("left_span") Span leftSpan, @JsonProperty("right_span") Span rightSpan) {
            this.annotationEntries = annotationEntries;
            this.leftSpan = leftSpan;
            this.rightSpan = rightSpan;
        }

        public EdgeMentionAnnotation(Span leftSpan, Span rightSpan) {
            this.annotationEntries = new ArrayList<>();
            this.leftSpan = leftSpan;
            this.rightSpan = rightSpan;
        }
    }

    public static NodeMentionAnnotation getOrCreateNodeMentionAnnotation(List<NodeMentionAnnotation> nodeMentionAnnotationList, Span span) {
        NodeMentionAnnotation ret = getNodeMentionAnnotation(nodeMentionAnnotationList, span);
        if (ret != null) {
            return ret;
        } else {
            ret = new NodeMentionAnnotation(span);
            nodeMentionAnnotationList.add(ret);
            return ret;
        }
    }

    public static EdgeMentionAnnotation getEdgeMentionAnnotation(List<EdgeMentionAnnotation> edgeMentionAnnotationList, Span left, Span right) {
        EdgeMentionAnnotation ret = null;
        for (EdgeMentionAnnotation edgeMentionAnnotation : edgeMentionAnnotationList) {
            if (edgeMentionAnnotation.leftSpan.equals(left) && edgeMentionAnnotation.rightSpan.equals(right)) {
                ret = edgeMentionAnnotation;
                break;
            }
        }
        return ret;
    }

    public static EdgeMentionAnnotation getOrCreateEdgeMentionAnnotation(List<EdgeMentionAnnotation> edgeMentionAnnotationList, Span left, Span right) {
        EdgeMentionAnnotation ret = getEdgeMentionAnnotation(edgeMentionAnnotationList, left, right);
        if (ret != null) {
            return ret;
        } else {
            ret = new EdgeMentionAnnotation(left, right);
            edgeMentionAnnotationList.add(ret);
            return ret;
        }
    }

    public static SerifAnnotationData readSerifAnnotationData(File path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(path, SerifAnnotationData.class);
    }

    public static AnnotationEntry getAnnotationEntry(List<AnnotationEntry> annotationEntries, String source) {
        AnnotationEntry ret = null;
        for (AnnotationEntry annotationEntry : annotationEntries) {
            if (annotationEntry.source.equals(source)) {
                ret = annotationEntry;
                break;
            }
        }
        return ret;
    }

    public static AnnotationEntry getOrCreateAnnotationEntry(List<AnnotationEntry> annotationEntries, String source) {
        AnnotationEntry ret = getAnnotationEntry(annotationEntries, source);
        if (ret != null) {
            return ret;
        } else {
            ret = new AnnotationEntry(source);
            annotationEntries.add(ret);
            return ret;
        }
    }

    public NodeMentionAnnotation getOrCreateEventMentionAnnotation(Span span) {
        return getOrCreateNodeMentionAnnotation(this.eventMentionAnnotations, span);
    }

    public EdgeMentionAnnotation getOrCreateEventEventRelationMentionAnnotation(Span left, Span right) {
        return getOrCreateEdgeMentionAnnotation(this.eventEventRelationMentionAnnotations, left, right);
    }

    public EdgeMentionAnnotation getOrCreateEventEventArgumentRelationMentionAnnotation(Span left, Span right) {
        return getOrCreateEdgeMentionAnnotation(this.eventEventArgumentRelationMentionAnnotations, left, right);
    }

    public void writeSerifAnnotationData(File path) throws IOException {
        ObjectMapper objectMapper = StorageUtils.getMapperWithoutTyping();
        objectMapper.writeValue(path, this);
    }

    public static class Marking {
        @JsonProperty("label")
        public final String label;
        @JsonProperty("status")
        public final Annotation.FrozenState frozenState;

        @JsonCreator
        public Marking(@JsonProperty("label") String label, @JsonProperty("status") Annotation.FrozenState frozenState) {
            this.label = label;
            this.frozenState = frozenState;
        }
    }

}
