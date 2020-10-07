package com.bbn.akbc.neolearnit.serializers.observations;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SharableEventAnnotation {
    @JsonProperty("doc_uuid")
    final String doc_UUID;
    @JsonProperty("sent_offsets")
    final List<List<Integer>> sent_offsets;
    @JsonProperty("trigger_offsets")
    final List<List<Integer>> trigger_offsets;
    @JsonProperty("original_sentence")
    final String original_sentence;
    @JsonProperty("original_trigger")
    final String original_trigger;

    @JsonProperty("event_type")
    final String event_type;

    @JsonProperty("positive")
    final boolean positive;

    @JsonCreator
    public SharableEventAnnotation(@JsonProperty("doc_uuid")
                                           String doc_UUID,
                                   @JsonProperty("sent_offsets")
                                           List<List<Integer>> sent_offsets,
                                   @JsonProperty("trigger_offsets")
                                           List<List<Integer>> trigger_offsets,
                                   @JsonProperty("original_sentence")
                                           String original_sentence,
                                   @JsonProperty("original_trigger")
                                           String original_trigger,
                                   @JsonProperty("event_type")
                                           String event_type,
                                   @JsonProperty("positive")
                                           boolean positive
    ) {
        this.doc_UUID = doc_UUID;
        this.sent_offsets = sent_offsets;
        this.trigger_offsets = trigger_offsets;
        this.original_sentence = original_sentence;
        this.original_trigger = original_trigger;
        this.event_type = event_type;
        this.positive = positive;
    }

}
