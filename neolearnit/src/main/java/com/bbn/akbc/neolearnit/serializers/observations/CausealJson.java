package com.bbn.akbc.neolearnit.serializers.observations;

import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CausealJson {
    @JsonProperty String docid;
    @JsonProperty ImmutableList<ImmutableList<Integer>> arg1_span_list;
    @JsonProperty String arg1_text;
    @JsonProperty ImmutableList<ImmutableList<Integer>> arg2_span_list;
    @JsonProperty String arg2_text;
    @JsonProperty String connective_text;
    @JsonProperty String relation_type;
    @JsonProperty String semantic_class;
    @JsonProperty ImmutableList<LearnitPattern> learnit_pattern;

    @JsonCreator
    public CausealJson(@JsonProperty String docid,
                       @JsonProperty ImmutableList<ImmutableList<Integer>> arg1_span_list,
                       @JsonProperty String arg1_text,
                       @JsonProperty ImmutableList<ImmutableList<Integer>> arg2_span_list,
                       @JsonProperty String arg2_text,
                       @JsonProperty String semantic_class,
                       @JsonProperty ImmutableList<LearnitPattern> learnit_pattern
                                ){
        this.docid = docid;
        this.arg1_span_list = arg1_span_list;
        this.arg1_text = arg1_text;
        this.arg2_span_list = arg2_span_list;
        this.arg2_text = arg2_text;
        this.connective_text = "";
        this.relation_type = "Explicit";
        this.semantic_class = semantic_class;
        this.learnit_pattern = learnit_pattern;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof CausealJson)) return false;
        CausealJson that = (CausealJson) o;
        return this.docid.equals(that.docid) &&
                this.arg1_span_list.equals(that.arg1_span_list) &&
                this.arg2_span_list.equals(that.arg2_span_list) &&
                this.relation_type.equals(that.relation_type) &&
                this.semantic_class.equals(that.semantic_class);
    }
}
