package com.bbn.akbc.neolearnit.common;

import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
import com.bbn.bue.common.exceptions.NotImplementedException;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class InstanceIdentifier implements Comparable<InstanceIdentifier> {


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
                slot0Start, slot0End, slot0SpanningType, intToMType(slot0MentionType), slot0EntityType, isSlot0BestNameTypeNameb,
                slot1Start, slot1End, slot1SpanningType, intToMType(slot1MentionType), slot1EntityType, isSlot1BestNameTypeNameb);
    }

    public InstanceIdentifier(String docid, int sentid,
                              int slot0Start, int slot0End, SpanningType slot0SpanningType, Optional<Mention.Type> slot0MentionType,
                              String slot0EntityType, boolean isSlot0BestNameTypeName,
                              int slot1Start, int slot1End, SpanningType slot1SpanningType, Optional<Mention.Type> slot1MentionType,
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

    public static String entityType(Spanning span, boolean isUnary) {
        if (isUnary) {
            return span.getClass().getSimpleName();
        } else {
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
        if (!match.getSlot1().isPresent()) {
            return new InstanceIdentifier(
                    match.getDocTheory().docid().toString(), match.getSentTheory().index(),
                    span0.startToken().index(), span0.endToken().index(), SpanningType.valueOf(spanning0.getClass().getSimpleName()), s0Type, entityType(spanning0, true), isSlot0BestNameTypeName,
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
                span0.startToken().index(), span0.endToken().index(), SpanningType.valueOf(spanning0.getClass().getSimpleName()), s0Type, entityType(spanning0, false), isSlot0BestNameTypeName,
                span1.startToken().index(), span1.endToken().index(), SpanningType.valueOf(spanning1.getClass().getSimpleName()), s1Type, entityType(spanning1, false), isSlot1BestNameTypeName);
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

    // Ordering here is important, we should try align to most specific
    // concept first (in case there is an event and an mention with the same span)

    public static List<Spanning> getSpannings(SentenceTheory st, int start, int end, SpanningType spanningType) {
        List<Spanning> ret = new ArrayList<>();
        if (st == null) return ret;
        switch (spanningType) {
            case EventMention:
                for (EventMention eventMention : st.eventMentions()) {
                    if (eventMention.span().startToken().index() == start && eventMention.span().endToken().index() == end) {
                        ret.add(eventMention);
                    }
                }
                break;
            case Mention:
                for (Mention m : st.mentions()) {
                    if (m.span().startToken().index() == start && m.span().endToken().index() == end) {
                        ret.add(m);
                    }
                }
                break;
            case ValueMention:
                for (ValueMention vm : st.valueMentions()) {
                    if (vm.span().startToken().index() == start && vm.span().endToken().index() == end) {
                        ret.add(vm);
                    }
                }
                break;
            case Empty:
                break;
            default:
                throw new NotImplementedException();
        }
        return ret;
    }

    public static Optional<Spanning> getSpanning(SentenceTheory st, int start, int end, SpanningType spanningType) {
        if (st == null) return Optional.absent();
        switch (spanningType) {
            case EventMention:
                for (EventMention eventMention : st.eventMentions()) {
                    if (eventMention.span().startToken().index() == start && eventMention.span().endToken().index() == end) {
                        return Optional.of(eventMention);
                    }
                }
                break;
            case Mention:
                for (Mention m : st.mentions()) {
                    if (m.span().startToken().index() == start && m.span().endToken().index() == end) {
                        return Optional.of(m);
                    }
                }
                break;
            case ValueMention:
                for (ValueMention vm : st.valueMentions()) {
                    if (vm.span().startToken().index() == start && vm.span().endToken().index() == end) {
                        return Optional.of(vm);
                    }
                }
                break;
            case Empty:
                return Optional.absent();
            default:
                throw new NotImplementedException();
        }
        return Optional.absent();
    }

    public static List<SynNode> getNode(Spanning span, DocTheory dt) {
        if (span instanceof Mention) {
            return ImmutableList.of(((Mention) span).node());
        } else if (span instanceof ValueMention) {
            Optional<SynNode> synNodeOptional = ValueMention.node(dt, ((ValueMention) span));
            if (synNodeOptional.isPresent())
                return ImmutableList.of(synNodeOptional.get());
            else
                return ImmutableList.of();
        } else if (span instanceof SynNode) {
            return ImmutableList.of(((SynNode) span));
        } else if (span instanceof EventMention) {
            Set<SynNode> possibleLeftSynNodes = new HashSet<>();
            EventMention em = (EventMention) span;
            if (em.anchorNode() != null) possibleLeftSynNodes.add(em.anchorNode());
            for (EventMention.Anchor anchor : em.anchors()) {
                if (anchor.anchorNode() != null) possibleLeftSynNodes.add(anchor.anchorNode());
            }
            return ImmutableList.copyOf(possibleLeftSynNodes);
        } else if (span instanceof SentenceTheory) {
            final SentenceTheory sentence = (SentenceTheory) span;

            if (!sentence.parse().isAbsent())
                return ImmutableList.of(sentence.parse().root().get());
            else
                return ImmutableList.of();
        } else {
            throw new RuntimeException("Unhandled span type: " + span);
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

    public boolean isSlotValueMention(int slot) {
        if (slot == 0) {
            return this.getSlot0SpanningType().equals(SpanningType.ValueMention);
        } else {
            return this.getSlot1SpanningType().equals(SpanningType.ValueMention);
        }
    }

    // must be called after calling reconstructMatchInfo()
    public static Optional<DocTheory> getDocTheoryFromDocID(String docID) {
        if (cachedDocTheories.containsKey(docID))
            return Optional.of(cachedDocTheories.get(docID));
        else
            return Optional.absent();
    }

    public static Optional<BilingualDocTheory> getBiDocTheoryFromDocId(String docid) {
        if (cachedBiDocTheories.containsKey(docid)) {
            return Optional.of(cachedBiDocTheories.get(docid));
        } else
            return Optional.absent();
    }

    public static void putDocTheory(DocTheory docTheory) {
        cachedDocTheories.put(docTheory.docid().asString(), docTheory);
    }

    public static void putBiDocTheory(BilingualDocTheory bilingualDocTheory) {
        cachedBiDocTheories.put(bilingualDocTheory.getSourceDoc().docid().asString(), bilingualDocTheory);
    }

    public static void preLoadDocThoery(Collection<InstanceIdentifier> instanceIdentifiers) throws IOException, InterruptedException, ExecutionException {
        // TODO: Restore its functionality for bilingual also
        if (LearnItConfig.optionalParamTrue("bilingual")) {
            Set<InstanceIdentifier> pendingSet = new HashSet<>();
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                String docId = instanceIdentifier.getDocid();
                if (!cachedBiDocTheories.containsKey(docId)) {
                    pendingSet.add(instanceIdentifier);
                }
            }
            Set<BilingualDocTheory> result = GeneralUtils.resolvedBiDocTheoryFromInstanceIdentifier(pendingSet);
            for (BilingualDocTheory bilingualDocTheory : result) {
                putBiDocTheory(bilingualDocTheory);
            }
        } else {
            Set<InstanceIdentifier> pendingSet = new HashSet<>();
            for (InstanceIdentifier instanceIdentifier : instanceIdentifiers) {
                String docId = instanceIdentifier.getDocid();
                if (!cachedDocTheories.containsKey(docId)) {
                    pendingSet.add(instanceIdentifier);
                }
            }
            Set<DocTheory> result = GeneralUtils.resolvedDocTheoryFromInstanceIdentifier(pendingSet);
            for (DocTheory docTheory : result) {
                putDocTheory(docTheory);
            }
        }

    }

    public static void clearDocTheoryCache() {
        cachedDocTheories.clear();
        cachedBiDocTheories.clear();
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
    public MatchInfo reconstructMatchInfo(Target target) throws IOException {
        if (LearnItConfig.optionalParamTrue("bilingual")) {
            if (!cachedBiDocTheories.containsKey(docid)) {
                Map<String, String> biEntries = DocPathResolver.Bilingual.getPath(docid);
                BilingualDocTheory newDT = BilingualDocTheory.fromTabularPathLists(docid, biEntries);
                cachedBiDocTheories.put(docid, newDT);
            }
            BilingualDocTheory dt = cachedBiDocTheories.get(docid);
            SentenceTheory st = dt.getSourceDoc().sentenceTheory(sentid);
            Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0SpanningType);
            Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1SpanningType);
            if (slot1.isPresent()) {
                return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
            } else {
                return MatchInfo.from(target, dt, st, slot0.get());
            }
        } else {
            if (!cachedDocTheories.containsKey(docid)) {
                String docPath = DocPathResolver.Monolingual.getPath(docid);
                SerifXMLLoader serifXMLLoader = SerifXMLLoader.builder().build();
                DocTheory newDT = serifXMLLoader.loadFrom(new File(docPath));
                cachedDocTheories.put(docid, newDT);
            }
            DocTheory dt = cachedDocTheories.get(docid);
            SentenceTheory st = dt.sentenceTheory(sentid);
            Optional<Spanning> slot0 = getSpanning(st, slot0Start, slot0End, slot0SpanningType);
            Optional<Spanning> slot1 = getSpanning(st, slot1Start, slot1End, slot1SpanningType);
            if (slot1.isPresent()) {
                return MatchInfo.from(target, dt, st, slot0.get(), slot1.get());
            } else {
                return MatchInfo.from(target, dt, st, slot0.get());
            }
        }
    }


    public MatchInfoDisplay reconstructMatchInfoDisplay(Target target) throws IOException {
        return MatchInfoDisplay.fromMatchInfo(reconstructMatchInfo(target), Optional.absent());
    }

    public InstanceIdentifier reversed() {
        return new InstanceIdentifier(docid, sentid,
                slot1Start, slot1End, slot1SpanningType, slot1MentionType, slot1EntityType, isSlotBestNameTypeName.get(1),
                slot0Start, slot0End, slot0SpanningType, slot0MentionType, slot0EntityType, isSlotBestNameTypeName.get(0));
    }

    public InstanceIdentifier getCopy() {
        return new InstanceIdentifier(docid, sentid,
                slot0Start, slot0End, slot0SpanningType, slot0MentionType, slot0EntityType, isSlotBestNameTypeName.get(0),
                slot1Start, slot1End, slot1SpanningType, slot1MentionType, slot1EntityType, isSlotBestNameTypeName.get(1));
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
        int result = 0;
        result = 31 * result + docid.hashCode();
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

    @Override
    public int compareTo(InstanceIdentifier that) {
        return Integer.compare(this.hashCode(), that.hashCode());
    }

    public enum SpanningType {
        EventMention,
        Mention,
        ValueMention,
        Empty,
        Unknown
    }

    public InstanceIdentifier getLowerRankInstanceIdentifierLeft() {
        return new InstanceIdentifier(
                this.getDocid(),
                this.getSentid(),
                this.getSlot0Start(),
                this.getSlot0End(),
                this.getSlot0SpanningType(),
                this.getSlotMentionType(0),
                this.getSlot0SpanningType().name(),
                this.isSlotBestNameTypeName(0),
                -1,
                -1,
                InstanceIdentifier.SpanningType.Empty,
                Optional.absent(),
                "NA",
                false
        );
    }

    public InstanceIdentifier getLowerRankInstanceIdentifierRight() {
        return new InstanceIdentifier(
                this.getDocid(),
                this.getSentid(),
                this.getSlot1Start(),
                this.getSlot1End(),
                this.getSlot1SpanningType(),
                this.getSlotMentionType(1),
                this.getSlot1SpanningType().name(),
                this.isSlotBestNameTypeName(1),
                -1,
                -1,
                InstanceIdentifier.SpanningType.Empty,
                Optional.absent(),
                "NA",
                false
        );
    }

    public boolean isUnaryInstanceIdentifier() {
        return this.getSlot1SpanningType().equals(SpanningType.Empty);
    }
}
