package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.evaluation.tac.RelationMention;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 1/7/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationMentionInfo {
    @JsonProperty
    private final List<String> tokens;
    @JsonProperty
    private final List<Integer> distancesToArg1;
    @JsonProperty
    private final List<Integer> distancesToArg2;
    @JsonProperty
    private final String label;

    @JsonProperty
    private final int arg1index;
    @JsonProperty
    private final String arg1entityType;
    @JsonProperty
    private final String arg1mentionLevel;

    @JsonProperty
    private final int arg2index;
    @JsonProperty
    private final String arg2entityType;
    @JsonProperty
    private final String arg2mentionLevel;


    @JsonProperty
    private final List<String> entityTypeLabels;
    @JsonProperty
    private final List<String> mentionLevelLabels;

    @JsonProperty
    private final String docId;
    @JsonProperty
    private final int sentId;

    @JsonProperty
    private String markedSentenceString;
    @JsonProperty
    private Pair<Integer, Integer> arg1span;
    @JsonProperty
    private Pair<Integer, Integer> arg2span;


    @JsonCreator
    public RelationMentionInfo(
            @JsonProperty("tokens") List<String> tokens,
            @JsonProperty("distancesToArg1") List<Integer> distancesToArg1,
            @JsonProperty("distancesToArg2") List<Integer> distancesToArg2,
            @JsonProperty("label") String label,

            @JsonProperty("arg1index") int arg1index,
            @JsonProperty("arg1entityType") String arg1entityType,
            @JsonProperty("arg1mentionLevel") String arg1mentionLevel,
            @JsonProperty("arg2index") int arg2index,
            @JsonProperty("arg2entityType") String arg2entityType,
            @JsonProperty("arg2mentionLevel") String arg2mentionLevel,

            @JsonProperty("entityTypeLabels") List<String> entityTypeLabels,
            @JsonProperty("mentionLevelLabels") List<String> mentionLevelLabels,
            @JsonProperty("docId") String docId,
            @JsonProperty("sentId") int sentId,
            @JsonProperty("markedSentenceString") String markedSentenceString,
            @JsonProperty("arg1span") Pair<Integer, Integer> arg1span,
            @JsonProperty("arg2span") Pair<Integer, Integer> arg2span
    ) {
        this.tokens = tokens;
        this.distancesToArg1 = distancesToArg1;
        this.distancesToArg2 = distancesToArg2;
        this.label = label;

        this.arg1index = arg1index;
        this.arg1entityType = arg1entityType;
        this.arg1mentionLevel = arg1mentionLevel;
        this.arg2index = arg2index;
        this.arg2entityType = arg2entityType;
        this.arg2mentionLevel = arg2mentionLevel;

        this.entityTypeLabels = entityTypeLabels;
        this.mentionLevelLabels = mentionLevelLabels;
        this.markedSentenceString = markedSentenceString;
        this.arg1span = arg1span;
        this.arg2span = arg2span;
        this.docId = docId;
        this.sentId = sentId;
    }

    public static class SerializationFactory{
        public static class StringSerializer{
            RelationMentionInfo relationMentionInfo;
            StringSerializer(RelationMentionInfo relationMentionInfo){
                this.relationMentionInfo = relationMentionInfo = relationMentionInfo;
            }
            public String getSentenceInOneLine(){
                StringBuilder sb = new StringBuilder();
                for(String token : relationMentionInfo.tokens)
                    sb.append(token.replace("\n", "").replace("\t", "").replace(" ", "").trim() + " ");
                return sb.toString().trim();
            }
            public String getLabel(){
                return relationMentionInfo.label;
            }
            public String distanceToArg1InOneLine(){
                StringBuilder sb = new StringBuilder();
                for(int dis : relationMentionInfo.distancesToArg1)
                    sb.append(dis + " ");

                return sb.toString().trim();
            }
            public String distanceToArg2InOneLine(){
                StringBuilder sb = new StringBuilder();
                for(int dis : relationMentionInfo.distancesToArg2)
                    sb.append(dis + " ");

                return sb.toString().trim();
            }
            public String entityTypeLabelsCoveringArgSpanInOneLine(){
                StringBuilder sb = new StringBuilder();
                for(String s : relationMentionInfo.entityTypeLabels){
                    sb.append(s + " ");
                }
                return sb.toString().trim();
            }
            public String mentionLevelLabelsCoveringArgSpanInOneLine(){
                StringBuilder sb = new StringBuilder();
                for(String s : relationMentionInfo.mentionLevelLabels){
                    sb.append(s + " ");
                }
                return sb.toString().trim();
            }
        }
        public static class JSONStringSerializer{
            RelationMentionInfo relationMentionInfo;
            JSONStringSerializer(RelationMentionInfo relationMentionInfo){
                this.relationMentionInfo = relationMentionInfo;
            }
            public String getJSON(){
                ObjectMapper mapper = new ObjectMapper();
                try{
                    return mapper.writeValueAsString(relationMentionInfo);
                }
                catch (JsonProcessingException j){
                    j.printStackTrace();
                    return null;
                }
            }
        }
        public static class AmtCsvSerializer{
            RelationMentionInfo relationMentionInfo;
            AmtCsvSerializer(RelationMentionInfo relationMentionInfo){
                this.relationMentionInfo = relationMentionInfo;
            }
            public String getCsvRow() throws IOException{
                // Need further investigation. For Overlapping, appear before or some similar stuff.
                StringBuilder sb = new StringBuilder();
                CSVPrinter csvPrinter;
                csvPrinter = new CSVPrinter(sb, CSVFormat.DEFAULT);
                StringBuilder htmlMarkedSentenceBuilder = new StringBuilder();
                for (int i = 0; i < this.relationMentionInfo.tokens.size(); i++) {
                    final String currentToken = this.relationMentionInfo.tokens.get(i);
                    if (this.relationMentionInfo.arg1span.getFirst().equals(i) ||this.relationMentionInfo.arg2span.getFirst().equals(i)) {
                        if(this.relationMentionInfo.arg1span.getFirst().equals(this.relationMentionInfo.arg2span.getFirst())){
                            htmlMarkedSentenceBuilder.append("<span style=\\\"color:##FF0000;\\\"><b><span style=\\\"color:##FF0000;\\\"><b> " + currentToken);
                        }
                        else{
                            htmlMarkedSentenceBuilder.append("<span style=\\\"color:##FF0000;\\\"><b> " + currentToken);
                        }

                    }
                    else if (this.relationMentionInfo.arg1span.getSecond().equals(i) || this.relationMentionInfo.arg2span.getSecond().equals(i)){
                        if(this.relationMentionInfo.arg1span.getSecond().equals(this.relationMentionInfo.arg2span.getSecond())){
                            htmlMarkedSentenceBuilder.append(currentToken + "</b></span></b></span> ");
                        }
                        else{
                            htmlMarkedSentenceBuilder.append(currentToken + "</b></span> ");
                        }
                    }
                    else{
                        htmlMarkedSentenceBuilder.append(currentToken + " ");
                    }
                }
                csvPrinter.printRecord(
                        relationMentionInfo.docId,
                        relationMentionInfo.sentId,
                        relationMentionInfo.arg1span.getFirst(),
                        relationMentionInfo.arg1span.getSecond(),
                        relationMentionInfo.arg2span.getFirst(),
                        relationMentionInfo.arg2span.getSecond(),
                        relationMentionInfo.tokens.get(relationMentionInfo.distancesToArg1.indexOf(0)),
                        relationMentionInfo.tokens.get(relationMentionInfo.distancesToArg2.indexOf(0)),
                        htmlMarkedSentenceBuilder.toString()
                );
                return sb.toString();
            }
        }
    }




    public static class Builder {

        List<String> tokens;
        List<Integer> distancesToArg1;
        List<Integer> distancesToArg2;
        String label;

        int arg1index;
        String arg1entityType;
        String arg1mentionLevel;

        int arg2index;
        String arg2entityType;
        String arg2mentionLevel;

        List<String> entityTypeLabels;
        List<String> mentionLevelLabels;
        Pair<Integer, Integer> arg1span;
        Pair<Integer, Integer> arg2span;
        String markedSentenceString;
        final String defaultEntityTypeLabels;
        final String defaultmentionLevelLabels;
        MatchInfo.LanguageMatchInfo languageMatchInfo;
        enum MentionOrganization{
            LAST_TOKEN,
            HEAD_WORD
        }
        MentionOrganization mentionOrganization;
        public Builder(MatchInfo.LanguageMatchInfo languageMatchInfo) {
            this.tokens = new ArrayList<String>();
            SentenceTheory sentenceTheory = languageMatchInfo.getSentTheory();
            for(Token token: sentenceTheory.tokenSequence()){
                tokens.add(token.originalText().content().utf16CodeUnits());
            }
            this.defaultEntityTypeLabels = "NA";
            this.defaultmentionLevelLabels = "NA";
            this.languageMatchInfo = languageMatchInfo;
            mentionOrganization = MentionOrganization.HEAD_WORD;
            markedSentenceString = "";
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        // arg1 info
        public Builder withArg1index(int arg1index) {
            this.arg1index = arg1index;
            return this;
        }

        public Builder withArg1entityType(String arg1entityType) {
            this.arg1entityType = arg1entityType;
            return this;
        }

        public Builder withArg1mentionLevel(String arg1mentionLevel) {
            this.arg1mentionLevel = arg1mentionLevel;
            return this;
        }
        //

        // arg2 info
        public Builder withArg2index(int arg2index) {
            this.arg2index = arg2index;
            return this;
        }

        public Builder withArg2entityType(String arg2entityType) {
            this.arg2entityType = arg2entityType;
            return this;
        }

        public Builder withArg2mentionLevel(String arg2mentionLevel) {
            this.arg2mentionLevel = arg2mentionLevel;
            return this;
        }
        //
        public Builder withArg1Span(int start,int end){
            this.arg1span = new Pair<>(start,end);
            return this;
        }
        public Builder withArg2Span(int start,int end){
            this.arg2span = new Pair<>(start,end);
            return this;
        }
        public Builder withmarkedSentenceString(String markedSentenceString){
            this.markedSentenceString = markedSentenceString;
            return this;
        }
        public Builder withMentionOrganizationHeadWord(){
            this.mentionOrganization = MentionOrganization.HEAD_WORD;
            return this;
        }
        public Builder withMentionOrganizationLastToken(){
            this.mentionOrganization = MentionOrganization.LAST_TOKEN;
            return this;
        }
        public RelationMentionInfo build() {
            entityTypeLabels = new ArrayList<String>();
            mentionLevelLabels = new ArrayList<String>();
            distancesToArg1 = new ArrayList<Integer>();
            distancesToArg2 = new ArrayList<Integer>();
            EventMention arg1 = (EventMention) languageMatchInfo.firstSpanning();
            EventMention arg2 = (EventMention) languageMatchInfo.secondSpanning();
            Token arg1Head = this.languageMatchInfo.getSentTheory().tokenSequence().token(arg1.anchorNode().head().tokenSpan().endTokenIndexInclusive());
            Token arg2Head = this.languageMatchInfo.getSentTheory().tokenSequence().token(arg2.anchorNode().head().tokenSpan().endTokenIndexInclusive());
            for (int i = 0; i < tokens.size(); i++) {
                // arg1
                if (i >= arg1span.getFirst() && i <= arg1span.getSecond()) {
                    //entityTypeLabels.add("B-" + arg1entityType);
                    entityTypeLabels.add(arg1entityType);
                    mentionLevelLabels.add(arg1mentionLevel);
                } else if (i >= arg2span.getFirst() && i <= arg2span.getSecond()) {
                    //entityTypeLabels.add("B-" + arg2entityType);
                    entityTypeLabels.add(arg2entityType);
                    mentionLevelLabels.add(arg2mentionLevel);
                } else {
                    entityTypeLabels.add(defaultEntityTypeLabels);
                    mentionLevelLabels.add(defaultmentionLevelLabels);
                }
                if(this.mentionOrganization == MentionOrganization.HEAD_WORD){
                    distancesToArg1.add(i - arg1Head.index());
                    distancesToArg2.add(i - arg2Head.index());
                }
                else{
                    assert this.mentionOrganization == MentionOrganization.LAST_TOKEN;
                    distancesToArg1.add(i-this.languageMatchInfo.getSentTheory().tokenSequence().token(arg1index).index());
                    distancesToArg2.add(i-this.languageMatchInfo.getSentTheory().tokenSequence().token(arg2index).index());
                }
            }
            return new RelationMentionInfo(
                    this.tokens,
                    this.distancesToArg1,
                    this.distancesToArg2,
                    this.label,

                    this.arg1index,
                    this.arg1entityType,
                    this.arg1mentionLevel,
                    this.arg2index,
                    this.arg2entityType,
                    this.arg2mentionLevel,

                    this.entityTypeLabels,
                    this.mentionLevelLabels,
                    this.languageMatchInfo.getDocTheory().docid().asString(),
                    this.languageMatchInfo.getSentTheory().sentenceNumber(),
                    this.markedSentenceString,
                    this.arg1span,
                    this.arg2span
                    );
        }
    }
}
