package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.util.PathConverter;
import com.bbn.akbc.neolearnit.common.util.SourceListsReader;
import com.bbn.bue.common.parameters.exceptions.MissingRequiredParameter;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.*;
import com.bbn.serif.theories.TokenSequence.Span;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class InstanceIdentifier {


    private static Map<String, DocTheory> cachedDocTheories = new ConcurrentHashMap<String, DocTheory>();
    private static Map<String, BilingualDocTheory> cachedBiDocTheories = new ConcurrentHashMap<String, BilingualDocTheory>();
    @JsonProperty("docid")
    private final String docid;
    @JsonProperty("sentid")
    private final int sentid;
    @JsonProperty("slot0Start")
    private final int slot0Start; // these are token indices
    @JsonProperty("slot0End")
    private final int slot0End;
    @JsonProperty("slot0SpanningType")
    private final SpanningType slot0SpanningType;
    @JsonProperty("slot1Start")
    private final int slot1Start;
    @JsonProperty("slot1End")
    private final int slot1End;
    @JsonProperty("slot1SpanningType")
    private final SpanningType slot1SpanningType;
    @JsonProperty("s0EType")
    private final String slot0EntityType;
    @JsonProperty("s1EType")
    private final String slot1EntityType;
    @JsonProperty("s0MType")
    private int slot0MentionType() {
        return mTypeToInt(slot0MentionType);
    }
    @JsonProperty("s1MType")
    private int slot1MentionType() {
        return mTypeToInt(slot1MentionType);
    }
    @JsonProperty("s0BestName")
    private int isSlot0BestNameTypeName() {
        return isSlotBestNameTypeName.get(0) ? 1 : 0;
    }
    @JsonProperty("s1BestName")
    private int isSlot1BestNameTypeName() {
        return isSlotBestNameTypeName.get(1) ? 1 : 0;
    }

    private final Optional<Mention.Type> slot0MentionType;
    private final Optional<Mention.Type> slot1MentionType;
    private final List<Boolean> isSlotBestNameTypeName;

    @JsonCreator
    private static InstanceIdentifier from(
            @JsonProperty("docid") String docid,
            @JsonProperty("sentid") int sentid,
            @JsonProperty("slot0Start") int slot0Start,
            @JsonProperty("slot0End") int slot0End,
            @JsonProperty("s0MType") int slot0MentionType,
            @JsonProperty("s0EType") String slot0EntityType,
            @JsonProperty("slot1Start") int slot1Start,
            @JsonProperty("slot1End") int slot1End,
            @JsonProperty("s1MType") int slot1MentionType,
            @JsonProperty("s1EType") String slot1EntityType,
            @JsonProperty("s0BestName") int isSlot0BestNameTypeName,
            @JsonProperty("s1BestName") int isSlot1BestNameTypeName,
            @JsonProperty("slot0SpanningType") SpanningType slot0SpanningType,
            @JsonProperty("slot1SpanningType") SpanningType slot1SpanningType
            ) {
        boolean isSlot0BestNameTypeNameb = (isSlot0BestNameTypeName == 1);
        boolean isSlot1BestNameTypeNameb = (isSlot1BestNameTypeName == 1);
        return new InstanceIdentifier(docid, sentid,
                slot0Start, slot0End,slot0SpanningType, intToMType(slot0MentionType), slot0EntityType, isSlot0BestNameTypeNameb,
                slot1Start, slot1End,slot1SpanningType, intToMType(slot1MentionType), slot1EntityType, isSlot1BestNameTypeNameb);
    }

    public InstanceIdentifier(String docid, int sentid,
                              int slot0Start, int slot0End, SpanningType slot0SpanningType,Optional<Mention.Type> slot0MentionType,
                              String slot0EntityType, boolean isSlot0BestNameTypeName,
                              int slot1Start, int slot1End,SpanningType slot1SpanningType ,Optional<Mention.Type> slot1MentionType,
                              String slot1EntityType, boolean isSlot1BestNameTypeName) {
        this.docid = docid;
        this.sentid = sentid;
        this.slot0Start = slot0Start;
        this.slot0End = slot0End;
        this.slot1Start = slot1Start;
        this.slot1End = slot1End;
        this.slot0MentionType = slot0MentionType;
        this.slot1MentionType = slot1MentionType;
        this.slot0EntityType = slot0EntityType;
        this.slot1EntityType = slot1EntityType;
        this.isSlotBestNameTypeName = ImmutableList.of(isSlot0BestNameTypeName, isSlot1BestNameTypeName);
        this.slot0SpanningType = slot0SpanningType;
        this.slot1SpanningType = slot1SpanningType;
    }

    private static int mTypeToInt(Optional<Mention.Type> slotType) {
        if (slotType.isPresent()) {
            Mention.Type mtype = slotType.get();
            if (mtype == Mention.Type.NAME) {
                return 0;
            } else if (mtype == Mention.Type.DESC) {
                return 1;
            } else if (mtype == Mention.Type.PRON) {
                return 2;
            } else if (mtype == Mention.Type.APPO) {
                return 3;
            } else {
                return 4;
            }
        } else {
            return 5;
        }
    }

    private static Optional<Mention.Type> intToMType(int slotType) {
        if (slotType == 0) return Optional.of(Mention.Type.NAME);
        if (slotType == 1) return Optional.of(Mention.Type.DESC);
        if (slotType == 2) return Optional.of(Mention.Type.PRON);
        if (slotType == 3) return Optional.of(Mention.Type.APPO);
        if (slotType == 4) return Optional.of(Mention.Type.NONE);
        if (slotType == 5) return Optional.absent();
        throw new RuntimeException("Unknown Mention Type key read from JSON!");
    }

    public static double getConfidence(Optional<Mention.Type> mentionType) {
        if (mentionType.isPresent()) {
            Mention.Type mtype = mentionType.get();
            if (mtype == Mention.Type.NAME) return 1.0;
            if (mtype == Mention.Type.DESC) return 0.8;
            else if (mtype == Mention.Type.PRON) return 0.1;
            else if (mtype == Mention.Type.APPO) return 0.1;
            else return 0.1;
        }
        return 1.0;
    }

    public static InstanceIdentifier from(MatchInfo match) {
        LanguageMatchInfo primary = match.getPrimaryLanguageMatch();
        return InstanceIdentifier.from(primary, match.getTarget().isSymmetric());
    }

    public static String entityType(Spanning span) {
        if (span instanceof Mention) {
            return ((Mention) span).entityType().toString();
        } else if (span instanceof ValueMention) {
            return ((ValueMention) span).fullType().toString();
        } else if (span instanceof SynNode) {
            return "SynNode";
        } else if (span instanceof EventMention) {// augment for event
            return ((EventMention) span).type().asString();
        } else {
            throw new RuntimeException("Don't know how to build instance out of " + span.getClass());
        }
    }

    public static InstanceIdentifier from(LanguageMatchInfo match, boolean symmetric) {
        Spanning spanning0 = match.getSlot0().get();
        Span span0 = match.getSlot0().get().span();

        Optional<Mention.Type> s0Type;
        boolean isSlot0BestNameTypeName;
        if (spanning0 instanceof Mention) {
            s0Type = Optional.of(((Mention) spanning0).mentionType());

            Optional<Entity> e0 = ((Mention) spanning0).entity(match.getDocTheory());
            if (e0.isPresent())
                isSlot0BestNameTypeName = e0.get().representativeName().isPresent();
            else
                isSlot0BestNameTypeName = ((Mention) spanning0).mentionType() == Mention.Type.NAME;
        } else {
            s0Type = Optional.absent();
            isSlot0BestNameTypeName = false;
        }
        // hack to get unary mention
        if(!match.getSlot1().isPresent()) {
            return new InstanceIdentifier(
                match.getDocTheory().docid().toString(), match.getSentTheory().index(),
                span0.startToken().index(), span0.endToken().index(), SpanningType.valueOf(spanning0.getClass().getSimpleName()),s0Type, entityType(spanning0), isSlot0BestNameTypeName,
                    -1, -1, SpanningType.Empty, Optional.absent(), "NA", false);
        }

        Spanning spanning1 = match.getSlot1().get();
        Span span1 = match.getSlot1().get().span();

        if (symmetric) {
            if ((span0.startToken().index() > span1.startToken().index()) ||
                    (span0.startToken().index() == span1.startToken().index() &&
                            span0.endToken().index() > span1.endToken().index())) {
                Span temp = span1;
                span1 = span0;
                span0 = temp;

                Spanning tempSpanning = spanning1;
                spanning1 = spanning0;
                spanning0 = tempSpanning;
            }
        }


        Optional<Mention.Type> s1Type;
        boolean isSlot1BestNameTypeName;


        if (spanning1 instanceof Mention) {
            s1Type = Optional.of(((Mention) spanning1).mentionType());

            Optional<Entity> e1 = ((Mention) spanning1).entity(match.getDocTheory());
            if (e1.isPresent())
                isSlot1BestNameTypeName = e1.get().representativeName().isPresent();
            else
                isSlot1BestNameTypeName = ((Mention) spanning1).mentionType() == Mention.Type.NAME;
        } else {
            s1Type = Optional.absent();
            isSlot1BestNameTypeName = false;
        }
        return new InstanceIdentifier(
                match.getDocTheory().docid().toString(), match.getSentTheory().index(),
                span0.startToken().index(), span0.endToken().index(),SpanningType.valueOf(spanning0.getClass().getSimpleName()), s0Type, entityType(spanning0), isSlot0BestNameTypeName,
                span1.startToken().index(), span1.endToken().index(),SpanningType.valueOf(spanning1.getClass().getSimpleName()), s1Type, entityType(spanning1), isSlot1BestNameTypeName);
    }

    public String getDocid() {
        return docid;
    }

    public int getSentid() {
        return sentid;
    }

    public int getSlot0Start() {
        return slot0Start;
    }

    public int getSlot0End() {
        return slot0End;
    }

    public int getSlot1Start() {
        return slot1Start;
    }

    public int getSlot1End() {
        return slot1End;
    }

    public SpanningType getSlot0SpanningType() {
        return this.slot0SpanningType;
    }

    public SpanningType getSlot1SpanningType() {
        return this.slot1SpanningType;
    }

    // Ordering here is importatant, we should try align to most specific
    // concept first (in case there is an event and an mention with the same span)

    public String getSlotEntityType(int slot) {
        if (slot == 0) {
            return slot0EntityType;
        } else {
            return slot1EntityType;
        }
    }

    public boolean isSlotMention(int slot) {
        if (slot == 0) {
            return slot0MentionType.isPresent();
        } else {
            return slot1MentionType.isPresent();
        }
    }

    public Optional<Mention.Type> getSlotMentionType(int slot) {
        if (slot == 0) {
            return slot0MentionType;
        } else {
            return slot1MentionType;
        }
    }

    public boolean isSlotName(int slot) {
        if (slot == 0) {
            return slot0MentionType.isPresent() && slot0MentionType.get() == Mention.Type.NAME;
        } else {
            return slot1MentionType.isPresent() && slot1MentionType.get() == Mention.Type.NAME;
        }
    }

    public boolean isSlotBestNameTypeName(int slot) {
        return isSlotBestNameTypeName.get(slot);
    }

    public double getConfidence() {
        return (getConfidence(slot0MentionType) + getConfidence(slot1MentionType)) / 2.0;
    }

    // Ordering here is important, we should try align to most specific
    // concept first (in case there is an event and an mention with the same span)
    public static Optional<Spanning> getSpanning(SentenceTheory st, int start, int end, String slotType) {
        for (EventMention vm : st.eventMentions()) {
            if (vm.span().startToken().index() == start && vm.span().endToken().index() == end) {
                if (slotType.equals(entityType(vm))) {
                    return Optional.of(vm);
                }
            }
        }
        for (Mention m : st.mentions()) {
            if (m.span().startToken().index() == start && m.span().endToken().index() == end) {
                return Optional.of(m);
            }
        }
        for (ValueMention vm : st.valueMentions()) {
            if (vm.span().startToken().index() == start && vm.span().endToken().index() == end) {
                return Optional.of(vm);
            }
        }
        return Optional.absent();
    }

    // must be called after calling reconstructMatchInfo()
    public Optional<DocTheory> getDocTheoryFromDocID(String docID) {
        if (cachedDocTheories.containsKey(docID))
            return Optional.of(cachedDocTheories.get(docID));
        else
            return Optional.absent();
    }

    /**
     * Reconstructs the original match that generated this instanceID
     * by fetching the source document. Using path conversions to
     * figure out the source serifxml file.
     * <p>
     * TODO: Not bilingual ready yet!
     *
     * @param target
     * @return
     * @throws IOException
     */
    public MatchInfo reconstructMatchInfo(Target target) {

//		System.out.println("Reconstructing match info for "+this.toShortString()+"...");
        try {
            // special handling for using 2016 chn cs mappings under bilingual gigaword parama
            if ((new File("/nfs/mercury-04/u10/resources/KBP/CS/2016/corpus_chinese_mini/serifxmls/" // by default, 2016cs chinese mini corpus
                    + docid + ".xml")).exists()) {
                File docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2016/corpus_chinese_mini/serifxmls/"
                                + docid + ".xml");

                SerifXMLLoader loader = SerifXMLLoader.createFrom(
                        LearnItConfig.params());

                DocTheory newDT = loader.loadFrom(docFile);
                cachedDocTheories.put(docid, newDT);

                DocTheory dt = cachedDocTheories.get(docid);
                SentenceTheory st = dt.sentenceTheory(sentid);
                Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0EntityType);
                Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1EntityType);

                if(slot1.isPresent()){
                    return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
                }
                else{
                    return MatchInfo.from(target,dt,st,slot0.get());
                }

            }
            ////


            if (LearnItConfig.optionalParamTrue("bilingual")) {

                if (!cachedBiDocTheories.containsKey(docid)) {
                    List<File> docFiles = PathConverter.getFiles(docid);
                    String lang1 = LearnItConfig.getList("languages").get(0);
                    String lang2 = LearnItConfig.getList("languages").get(1);

                    System.out.println("PathConverter.getAlignmentPath(docid): " + PathConverter
                            .getAlignmentPath(docid));
                    BilingualDocTheory newDT = BilingualDocTheory.fromPaths(
                            lang1, docFiles.get(0).toString(),
                            lang2, docFiles.get(1).toString(),
                            PathConverter.getAlignmentPath(docid));

                    System.out.println("lang1: " + lang1);
                    System.out.println("lang2: " + lang2);
                    System.out.println("docFiles.get(0).toString(): " + docFiles.get(0).toString());
                    System.out.println("docFiles.get(1).toString(): " + docFiles.get(1).toString());
                    System.out.println("PathConverter.getAlignmentPath(docid): " + PathConverter.getAlignmentPath(docid));

                    cachedBiDocTheories.put(docid, newDT);
                }

                BilingualDocTheory dt = cachedBiDocTheories.get(docid);
                SentenceTheory st = dt.getSourceDoc().sentenceTheory(sentid);
                Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0EntityType);
                Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1EntityType);

                if(slot1.isPresent()){
                    return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
                }
                else{
                    return MatchInfo.from(target,dt,st,slot0.get());
                }

            } else {

                if (!cachedDocTheories.containsKey(docid)) {

                    System.out.println("== try loading serifxml for " + docid);
                    File docFile = getDocFileUsingCorpusName().orNull();
                     if(docFile==null){
                        try {
                            docFile = PathConverter.getFile(docid);
                        } catch (Exception e) {
                            //if everything fails, try reading path to the serifxml from source_lists param
                            docFile = new File(SourceListsReader.getFullPath(docid));
                        }
                     }
                    System.out.println("docid:\t" + docid + "\tpath:\t" + docFile.getAbsolutePath());

                    SerifXMLLoader loader = null;
                    boolean loadSerifXMLWithSloppyOffsets =
                            LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false);
                    if (loadSerifXMLWithSloppyOffsets){
                        loader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();
                     }else {
                        // loader = SerifXMLLoader.createFrom(
                        //    LearnItConfig.params());
                        loader = SerifXMLLoader.builderWithDynamicTypes().allowSloppyOffsets().build();
                    }
                    DocTheory newDT = loader.loadFrom(docFile);
                    cachedDocTheories.put(docid, newDT);
                }

                DocTheory dt = cachedDocTheories.get(docid);
                SentenceTheory st = dt.sentenceTheory(sentid);
                Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0EntityType);
                Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1EntityType);

                if(slot1.isPresent()){
                    return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
                }
                else{
                    return MatchInfo.from(target,dt,st,slot0.get());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // TODO: bad fix
    }

    public MatchInfo ReConstructMatchInfoWithDocTheory(Target target,DocTheory newDT){
        cachedDocTheories.put(docid, newDT);
        DocTheory dt = cachedDocTheories.get(docid);
        SentenceTheory st = dt.sentenceTheory(sentid);
        Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0EntityType);
        Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1EntityType);

        if(slot1.isPresent()){
            return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
        }
        else{
            return MatchInfo.from(target,dt,st,slot0.get());
        }
    }

    public Optional<File> getDocFileUsingCorpusName(){
        File docFile = null;
        String corpusName = null;
        try{
            corpusName = LearnItConfig.get("corpus_name");
        }catch (MissingRequiredParameter e){
            return Optional.absent();
        }
        if (corpusName.equals("coldstart_cs2016_chinese_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/CS/2016/corpus_chinese_mini/serifxmls/"
                            + docid + ".xml");
        } else if (corpusName.equals("coldstart_cs2015")) {
            docFile = new File(
                    "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2015CS_trail.v1.no_indoc_split.statRE/serifxml/"
                            + docid + ".xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/runjobs2/expts/experiments/2015CS_trail.v1.no_indoc_split.statRE/serifxml/"
                                + docid + ".sgm.xml");
        } else if (corpusName.equals("coldstart_cs2015_mini")) {
            docFile = new File("/nfs/mercury-04/u42/bmin/runjobs2/expts/scripts/2015CS_test_for_2016.bbn2.v1.no_indoc_split.megaDefender.v12.serif20160706_mgCoref.new_actorDB.e2e.with_empty_namelist.df_bolt_parser/serifxml/"
                    + docid + ".mpdf.serifxml.xml");
            if (!docFile.exists()) {
                docFile = new File("/nfs/mercury-04/u42/bmin/runjobs2/expts/scripts/2015CS_test_for_2016.bbn2.v1.no_indoc_split.megaDefender.v12.serif20160706_mgCoref.new_actorDB.e2e.with_empty_namelist.df_bolt_parser/serifxml/"
                        + docid + ".serifxml.xml");
            }
        } else if (corpusName.equals("coldstart_cs2014_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                            + docid + ".xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".sgm.xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u10/resources/KBP/CS/2014/corpus/mini_corpus/data_for_learnit/serifxml/"
                                + docid + ".sgm.xml");
        } else if (corpusName.equals("coldstart_sf2014_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/SF/2014/mini_corpus/data_for_learnit/serifxml/"
                            + docid + ".sgm.xml");
        } else if (corpusName.equals("coldstart_cs2013_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/CS/2013/corpus/mini_corpus/data_for_learnit/serifxml/"
                            + docid + ".xml");
        }else if (corpusName.equals("coldstart_sf2013_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/SF/2013/mini_corpus/data_for_learnit/serifxml/"
                            + docid + ".sgm.xml");
        } else if (corpusName.equals("coldstart_sf2012_mini")) {
            docFile = new File(
                    "/nfs/mercury-04/u10/resources/KBP/SF/2012/mini_corpus/data_for_learnit/serifxml/"
                            + docid + ".sgm.xml");
        } else if (corpusName.equals("ere_LDC2015E29_and_LDC2015E68")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                            + docid + ".xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                                + docid + ".mpdf.xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u41/learnit/ERE/serifxml/"
                                + docid + ".cmp.txt.xml");
        } else if (corpusName.equals("ere_event_test_LDC2015E29_and_LDC2015E68")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/ERE.withVerbsAsEventMentions/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("ere_LDC2015E29_DEFT_Rich_ERE_English_Training_Annotation_V1")) {
            docFile = new File(
                    "/nfs/mercury-04/u42/bmin/everything/projects/ere/data_learnit/serifxml/"
                            + docid + ".xml");
            if (!docFile.exists())
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/everything/projects/ere/data_learnit/serifxml/"
                                + docid + ".mpdf.xml");
        } else if (corpusName.equals("pdtb_v2")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/pdtb_v2/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("pdtb_v2.withVerbsAsEventMentions")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/pdtb_v2.withVerbsAsEventMentions/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("red.eventFromAnnotation")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/RED.eventFromAnnotation/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("wm_starter")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/WM_starter/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("causeex-m5")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/CauseEx-M5/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("causeex-m5-pos")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/CauseEx-M5.add_verbs_and_nouns/serifxml/"
                            + docid + ".xml");
        } else if (corpusName.equals("wm_m6")) {
            docFile = new File(
                    "/nfs/mercury-04/u41/learnit/WM_m6/serifxml/"
                            + docid + ".serifxml");
        } else if (LearnItConfig.get("corpus_name")
                .equals("causeex-m9")) {
            docFile = new File(
                    "/nfs/mercury-04/u42/bmin/projects/CauseEx/M9_assessment/relations/learnit/causeex_m9.add_verbs_nouns/serifxml/"
                            + docid + ".serifxml");
            if(!docFile.exists()) {
                docFile = new File(
                        "/nfs/mercury-04/u42/bmin/projects/CauseEx/M9_assessment/relations/learnit/causeex_m9.add_verbs_nouns/serifxml/"
                                + docid + ".xml");
            }
        }
        return Optional.fromNullable(docFile);
    }

    public MatchInfoDisplay reconstructMatchInfoDisplay(Target target) {
        return MatchInfoDisplay.fromMatchInfo(reconstructMatchInfo(target), Optional.absent());
    }

    public InstanceIdentifier reversed() {
        return new InstanceIdentifier(docid, sentid,
                slot1Start, slot1End,slot1SpanningType, slot1MentionType, slot1EntityType, isSlotBestNameTypeName.get(1),
                slot0Start, slot0End,slot0SpanningType ,slot0MentionType, slot0EntityType, isSlotBestNameTypeName.get(0));
    }

    public InstanceIdentifier getCopy() {
        return new InstanceIdentifier(docid, sentid,
                slot0Start, slot0End,slot0SpanningType, slot0MentionType, slot0EntityType, isSlotBestNameTypeName.get(0),
                slot1Start, slot1End,slot1SpanningType, slot1MentionType, slot1EntityType, isSlotBestNameTypeName.get(1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceIdentifier that = (InstanceIdentifier) o;

        if (sentid != that.sentid) return false;
        if (slot0Start != that.slot0Start) return false;
        if (slot0End != that.slot0End) return false;
        if (slot1Start != that.slot1Start) return false;
        if (slot1End != that.slot1End) return false;
        if (!docid.equals(that.docid)) return false;
        if (!slot0EntityType.equals(that.slot0EntityType)) return false;
        if (!slot1EntityType.equals(that.slot1EntityType)) return false;
        if (!slot0MentionType.equals(that.slot0MentionType)) return false;
        if (!slot1MentionType.equals(that.slot1MentionType)) return false;
        if (!slot0SpanningType.equals(that.slot0SpanningType)) return false;
        if (!slot1SpanningType.equals(that.slot1SpanningType)) return false;
        return isSlotBestNameTypeName.equals(that.isSlotBestNameTypeName);
    }

    @Override
    public int hashCode() {
        int result = docid.hashCode();
        result = 31 * result + sentid;
        result = 31 * result + slot0Start;
        result = 31 * result + slot0End;
        result = 31 * result + slot1Start;
        result = 31 * result + slot1End;
        result = 31 * result + slot0EntityType.hashCode();
        result = 31 * result + slot1EntityType.hashCode();
        result = 31 * result + slot0MentionType.hashCode();
        result = 31 * result + slot1MentionType.hashCode();
        result = 31 * result + isSlotBestNameTypeName.hashCode();
        result = 31 * result + slot0SpanningType.hashCode();
        result = 31 * result + slot1SpanningType.hashCode();
        return result;
    }

    @Override
    public String toString() {
//        return "InstanceIdentifier [docid=" + docid + ", sentid=" + sentid
//                + ", slot0Start=" + slot0Start + ", slot0End=" + slot0End
//                + ", slot1Start=" + slot1Start + ", slot1End=" + slot1End
//                + ", slot0MentionType=" + slot0MentionType
//                + ", slot1MentionType=" + slot1MentionType
//                + ", slot0EntityType=" + slot0EntityType + ", slot1EntityType="
//                + slot1EntityType + "]";
        return "InstanceIdentifier [docid=" + docid + ", sentid=" + sentid
                + ", slot0Start=" + slot0Start + ", slot0End=" + slot0End
                + ", slot0SpanningType=" + slot0SpanningType + ", slot1SpanningType=" + slot1SpanningType
                + ", s0MType=" + this.slot0MentionType() + ", s0EType=" + slot0EntityType
                + ", slot1Start=" + slot1Start + ", slot1End=" + slot1End
                + ", s1MType=" + this.slot1MentionType() + ", s1EType="
                + slot1EntityType + ", s0BestName=" + this.isSlot0BestNameTypeName() + ", s1BestName=" + this.isSlot1BestNameTypeName() + "]";
    }

    public String toShortString() {
        return "InstanceIdentifier [docid="
                + docid + ", sentid=" + sentid + ", slot0Start=" + slot0Start
                + ", slot0End=" + slot0End + ", slot1Start=" + slot1Start
                + ", slot1End=" + slot1End + "]";
    }

    public enum SpanningType {
        EventMention,
        Mention,
        ValueMention,
        Empty,
        Unknown
    }
}
