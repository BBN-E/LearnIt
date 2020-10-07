package com.bbn.akbc.neolearnit.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenericEventDetector {

    static ImmutableSet<String> role_active_actor = ImmutableSet.of("sub", "by");
    static ImmutableSet<String> et_active_actor = ImmutableSet.of("GPE", "PER", "ORG");

    static ImmutableSet<String> role_affected_actor = ImmutableSet.of("obj", "including");
    static ImmutableSet<String> et_affected_actor = ImmutableSet.of("GPE", "PER", "ORG");

    static ImmutableSet<String> role_location = ImmutableSet.of("in", "at", "over", "<loc>", "to", "into");
    static ImmutableSet<String> et_location = ImmutableSet.of("GPE", "LOC");

    static ImmutableSet<String> role_time = ImmutableSet.of("in", "at", "over", "during", "<temp>");

    static ImmutableSet<String> role_artifact = ImmutableSet.of("by", "of", "with", "poss", "via");
    static ImmutableSet<String> et_artifact = ImmutableSet.of("VEH", "WEA");

    public static Optional<String> getExtendedEventMention(SynNode anchorNode, SentenceTheory sentenceTheory, DocTheory docTheory) {
        for (Proposition topProp : sentenceTheory.propositions()) {
            if (topProp.predHead().isPresent()) {
                SynNode predHead = topProp.predHead().get();
                if (predHead.head().span().equals(anchorNode.head().span())) {
                    Optional<String> sub = Optional.absent();
                    Optional<String> obj = Optional.absent();
                    Optional<String> iobj = Optional.absent();
                    Optional<String> poss = Optional.absent();
                    Optional<String> of = Optional.absent();
                    Optional<String> by = Optional.absent();
                    Optional<String> strFor = Optional.absent();
                    Optional<String> in = Optional.absent();
                    Optional<String> on = Optional.absent();
                    Optional<String> at = Optional.absent();
                    Optional<String> over = Optional.absent();
                    Optional<String> under = Optional.absent();
                    Optional<String> with = Optional.absent();
                    Optional<String> without = Optional.absent();
                    Optional<String> to = Optional.absent();
                    Optional<String> into = Optional.absent();
                    Optional<String> about = Optional.absent();
                    Optional<String> against = Optional.absent();
                    Optional<String> from = Optional.absent();
                    Optional<String> between = Optional.absent();
                    Optional<String> among = Optional.absent();
                    Optional<String> involving = Optional.absent();

                    for (Proposition.Argument a : topProp.args()) {
                        final Symbol role = a.role().isPresent() ? a.role().get() : Symbol.from("UNKNOWN");
                        Optional<String> text = Optional.absent();

                        if (role.asString().equals("<ref>") ||
                                role.asString().equals("<unknown>") ||
                                role.asString().equals("that") ||
                                role.asString().equals("when") ||
                                role.asString().equals("before") ||
                                role.asString().equals("during") ||
                                role.asString().equals("after") ||
                                role.asString().equals("since") ||
                                role.asString().equals("where") ||
                                role.asString().equals("while") ||
                                role.asString().equals("<temp>") ||
                                role.asString().equals("via") ||
                                role.asString().equals("including") ||
                                role.asString().equals("if") ||
                                role.asString().equals("as") ||
                                role.asString().equals("following") ||
                                role.asString().equals("out_of") ||
                                role.asString().equals("below") ||
                                role.asString().equals("<loc>") ||
                                role.asString().equals("like"))
                            continue;

                        if (a instanceof Proposition.MentionArgument) {
                            Proposition.MentionArgument ma = (Proposition.MentionArgument) a;
                            SynNode mention = ma.mention().node();
                            while (mention != null) {
                                mention = mention.head().equals(mention) ? null : mention.head();
                            }
                            if (ma.mention().entityType().name().asString().equals("OTH") ||
                                    ma.mention().entityType().name().asString().equals("UNDET"))
                                text = Optional.of(ma.mention().head().head().tokenSpan().tokenizedText(docTheory).utf16CodeUnits());
                            else
                                text = Optional.of(ma.mention().entityType().name().asString());
                        } else if (a instanceof Proposition.TextArgument) {
                            Proposition.TextArgument ta = (Proposition.TextArgument) a;
                            text = Optional.of(ta.node().tokenSpan().tokenizedText(docTheory).utf16CodeUnits());
                        } else if (a instanceof Proposition.PropositionArgument) {
                            Proposition.PropositionArgument pa = (Proposition.PropositionArgument) a;
                            //if s0 and s1 are anchorNodes of EventMentions, we may just want to match the predicate of the Proposition with them
                            Optional<SynNode> paPredHead = pa.proposition().predHead();
                            if (pa.proposition().predHead().isPresent())
                                text = Optional.of(pa.proposition().predHead().get().tokenSpan().tokenizedText(docTheory).utf16CodeUnits());
                        }

                        if (role.asString().equals("<sub>"))
                            sub = text;
                        else if (role.asString().equals("<obj>"))
                            obj = text;
                        else if (role.asString().equals("<iobj>"))
                            iobj = text;
                        else if (role.asString().equals("<poss>"))
                            poss = text;
                        else if (role.asString().equals("of"))
                            of = text;
                        else if (role.asString().equals("with"))
                            with = text;
                        else if (role.asString().equals("without"))
                            without = text;
                        else if (role.asString().equals("by"))
                            by = text;
                        else if (role.asString().equals("in"))
                            in = text;
                        else if (role.asString().equals("into"))
                            into = text;
                        else if (role.asString().equals("involving"))
                            involving = text;
                        else if (role.asString().equals("on"))
                            on = text;
                        else if (role.asString().equals("at"))
                            at = text;
                        else if (role.asString().equals("over"))
                            over = text;
                        else if (role.asString().equals("under"))
                            under = text;
                        else if (role.asString().equals("to"))
                            to = text;
                        else if (role.asString().equals("for"))
                            strFor = text;
                        else if (role.asString().equals("about"))
                            about = text;
                        else if (role.asString().equals("against"))
                            against = text;
                        else if (role.asString().equals("from"))
                            from = text;
                        else if (role.asString().equals("between"))
                            between = text;
                        else if (role.asString().equals("among"))
                            among = text;
                    }

                    String head = "[" + anchorNode.tokenSpan().tokenizedText(docTheory).utf16CodeUnits() + "]";
                    if (sub.isPresent())
                        head = sub.get() + " " + head;
                    if (poss.isPresent())
                        head += poss.get() + " 's ";
                    if (obj.isPresent())
                        head += " " + obj.get();
                    if (of.isPresent())
                        head += " of " + of.get();
                    if (from.isPresent())
                        head += " from " + from.get();
                    if (by.isPresent())
                        head += " by " + by.get();
                    if (with.isPresent())
                        head += " with " + with.get();
                    if (without.isPresent())
                        head += " without " + without.get();
                    if (strFor.isPresent())
                        head += " for " + strFor.get();
                    if (in.isPresent())
                        head += " in " + in.get();
                    if (into.isPresent())
                        head += " into " + into.get();
                    if (on.isPresent())
                        head += " on " + on.get();
                    if (at.isPresent())
                        head += " at " + at.get();
                    if (over.isPresent())
                        head += " over " + over.get();
                    if (under.isPresent())
                        head += " under " + under.get();
                    if (to.isPresent())
                        head += " to " + to.get();
                    if (iobj.isPresent())
                        head += " " + iobj.get();
                    if (about.isPresent())
                        head += " about " + about.get();
                    if (involving.isPresent())
                        head += " involving " + involving.get();
                    if (against.isPresent())
                        head += " against " + against.get();
                    if (between.isPresent())
                        head += " between " + between.get();
                    if (among.isPresent())
                        head += " among " + among.get();

                    return Optional.of(head);
                }
            }
        }

        return Optional.absent();
    }

    public static List<EventMention> getEventMentions(SynNode anchorNode, SentenceTheory sentenceTheory, DocTheory docTheory) {
        List<EventMention> eventMentions = new ArrayList<EventMention>();
        for (Proposition topProp : sentenceTheory.propositions()) {
            if (topProp.predHead().isPresent()) {
                SynNode predHead = topProp.predHead().get();
                if (predHead.head().span().equals(anchorNode.head().span())) {

                    List<EventMention.Argument> arguments = new ArrayList<EventMention.Argument>();
                    for (Proposition.Argument a : topProp.args()) {
                        final Symbol role = a.role().isPresent() ? a.role().get() : Symbol.from("UNKNOWN");
                        Optional<String> text = Optional.absent();

                        Optional<String> arg_role = Optional.absent();
                        if (role_active_actor.contains(role.asString()))
                            arg_role = Optional.of("has_active_actor");
                        else if (role_affected_actor.contains(role.asString()))
                            arg_role = Optional.of("has_affected_actor");
                        else if (role_location.contains(role.asString()))
                            arg_role = Optional.of("has_location");
                        else if (role_time.contains(role.asString()))
                            arg_role = Optional.of("has_time");
                        else if (role_artifact.contains(role.asString()))
                            arg_role = Optional.of("has_artifact");

                        if (arg_role.isPresent()) {
                            if (a instanceof Proposition.MentionArgument) {
                                Proposition.MentionArgument ma = (Proposition.MentionArgument) a;
                                SynNode mention = ma.mention().node();
                                while (mention != null) {
                                    mention = mention.head().equals(mention) ? null : mention.head();
                                }

                                String entityType = ma.mention().entityType().name().asString();
                                if ((et_active_actor.contains(entityType) && arg_role.get().equals("has_active_actor")) ||
                                        (et_affected_actor.contains(entityType) && arg_role.get().equals("has_affected_actor")) ||
                                        (et_location.contains(entityType) && arg_role.get().equals("has_location")) ||
                                        (et_artifact.contains(entityType) && arg_role.get().equals("has_artifact"))) {
                                    EventMention.MentionArgument arg = EventMention.MentionArgument.from(Symbol.from(arg_role.get()), ma.mention(), 0.7f);
                                    arguments.add(arg);
                                }
                            } else if (a instanceof Proposition.TextArgument) {
                                Proposition.TextArgument ta = (Proposition.TextArgument) a;
                                Optional<ValueMention> vmOptional = sentenceTheory.valueMentions().lookupByTokenSpan(ta.span().tokenSequence().span());
                                if (vmOptional.isPresent()) {
                                    EventMention.ValueMentionArgument arg = EventMention.ValueMentionArgument.from(Symbol.from(arg_role.get()), vmOptional.get(), 0.7f);
                                    arguments.add(arg);
                                }
                            }
                        }
                    }

                    Optional<String> extendedEventMention = getExtendedEventMention(anchorNode, sentenceTheory, docTheory);
                    Symbol eventPatternID = Symbol.from(extendedEventMention.isPresent() ? extendedEventMention.get() : "NA");

                    // // get NP
                    // Optional<SynNode> anchorNP = findNPforEventTriggers(anchorNode, sentenceTheory);
                    // if(anchorNP.isPresent())
                    //    anchorNode = anchorNP.get();

                    EventMention em = EventMention
                            .builder(Symbol.from("Event"))
                            .setAnchorNode(anchorNode)
                            .setAnchorPropFromNode(sentenceTheory)
                            .setScore(GenericEventDetector.GENERIC_EVENT_SCORE)
                            .setPatternID(eventPatternID)
                            .setArguments(arguments)
                            .build();
                    eventMentions.add(em);
                }
            }
        }
        return eventMentions;
    }


    public static int numberOfAlphabetsInString(final String text) {
        int count = 0;
        for (int cIndex = 0; cIndex < text.length(); cIndex++) {
            final char c = text.charAt(cIndex);
            if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
                count += 1;
            }
        }
        return count;
    }

    public static int numberOfDashInString(final String text) {
        int count = 0;
        for (int cIndex = 0; cIndex < text.length(); cIndex++) {
            final char c = text.charAt(cIndex);
            if (c == '-') {
                count += 1;
            }
        }
        return count;
    }

    public static boolean isInValidEventMentionString(final String text) {
        int MAX_NUM_WORDS = 5;
        if (text.split(" ").length > MAX_NUM_WORDS)
            return true;

        for (String tokenText : text.split(" ")) {
            if (tokenText.contains("'") ||
                    (numberOfAlphabetsInString(tokenText) + numberOfDashInString(tokenText)) != tokenText.length() ||
                    numberOfAlphabetsInString(tokenText) < 2)
                return true;
        }

        return false;
    }


    public DocTheory addEventMentions(final DocTheory input) {
        final DocTheory.Builder docBuilder = input.modifiedCopyBuilder();
        int eventsAdded = 0;
        for (int i = 0; i < input.numSentences(); ++i) {
            final SentenceTheory st = input.sentenceTheory(i);

            final SentenceTheory.Builder sentBuilder = st.modifiedCopyBuilder();
            final ImmutableList.Builder<EventMention> newEventMentions =
                    ImmutableList.builder();

            // keep existing event mentions
            for (EventMention eventMention : st.eventMentions()) {
                newEventMentions.add(eventMention);
            }

            Set<SynNode> addedHeads = new HashSet<>();
            // add verb or noun as mentions
            for (Proposition proposition : st.propositions().asList()) {
                if (proposition.predHead().isPresent()) {
                    String text = proposition.predHead().get().head().tokenSpan().originalText().content().utf16CodeUnits();
                    String predType = proposition.predType().name().asString();

                    if (isInValidEventMentionString(text)) {
                        //System.out.println("Skipping due to astrophe " + text);
                        //System.out.println("Skipping due to length " + text);
                        //System.out.println("Skipping due to numberOfAlphabetsInString " + text);
                        continue;
                    }

                    if (freq_not_triggers.contains(text.trim().toLowerCase()))
                        continue;
                    //String pos_and_trigger = pos_and_trigger(proposition.predHead().get().head().headPOS().asString(),
                    //        text);
                    //if(!freq_pos_and_triggers.contains(pos_and_trigger))
                    //    continue;

                    if (predType.equalsIgnoreCase("verb") || predType.equalsIgnoreCase("noun")) {
                        final SynNode currentNode = proposition.predHead().get();
                        final String headPOS = currentNode.headPOS().asString();
                        final String headText = currentNode.tokenSpan().originalText().content().utf16CodeUnits();

                        // skip anything that's not verb or noun
                        if (!headPOS.startsWith("N") && !headPOS.startsWith("V"))
                            continue;

                        // skip proper nouns
                        if (headPOS.startsWith("NNP"))
                            continue;

                        // this is a command noun, but not in the set of noun_triggers whitelist
                        if (headPOS.startsWith("NN") && !noun_triggers.contains(headText.toLowerCase())) {
                            continue;
                        }

                        addedHeads.add(currentNode);
                        List<EventMention> eventMentions = getEventMentions(currentNode, st, input);

                        newEventMentions.addAll(eventMentions);
                        eventsAdded += eventMentions.size();
                    }
                }
            }

            // add mentions
            for (Mention mention : st.mentions()) {
                String entityType = mention.entityType().name().asUnicodeFriendlyString().utf16CodeUnits();
                String mentionType = mention.mentionType().name();

                String text = mention.tokenSpan().tokenizedText(input).utf16CodeUnits();
                String headText = mention.head().tokenSpan().tokenizedText(input).utf16CodeUnits();

                if (addedHeads.contains(mention.head()))
                    continue;

                // remove invalid mention
                if (isInValidEventMentionString(text))
                    continue;

                if (!noun_triggers.contains(headText.toLowerCase()))
                    continue;

                if (entityType.equals("OTH") && mentionType.equals("DESC")) {
                    EventMention em = EventMention
                            .builder(Symbol.from("Event"))
                            .setAnchorNode(mention.head())
                            .setAnchorPropFromNode(st)
                            .setScore(GenericEventDetector.GENERIC_EVENT_SCORE)
                            .setPatternID(Symbol.from(text.replace("\n", " ").replace("\t", " ")))
                            .build();
                    newEventMentions.add(em);
                }
            }

            sentBuilder.eventMentions(new EventMentions.Builder()
                    .eventMentions(newEventMentions.build())
                    .build());
            docBuilder.replacePrimarySentenceTheory(st, sentBuilder.build());
        }

        return docBuilder.build();
    }

    private final Set<String> freq_not_triggers;
    private final Set<String> noun_triggers;

    public GenericEventDetector(String strFileNounWhiteList, String strFileBlackList) throws IOException {
        this.noun_triggers = new HashSet<>();
        List<String> lines = GeneralUtils.readLinesIntoList(strFileNounWhiteList);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            noun_triggers.add(line);
        }

        this.freq_not_triggers = new HashSet<>();

        if (strFileBlackList.trim().equals("NA"))
            return;
        String file = strFileBlackList;
        lines = GeneralUtils.readLinesIntoList(file);
        for (String line : lines) {
            freq_not_triggers.add(line.trim().toLowerCase());
        }
    }

    public static double GENERIC_EVENT_SCORE = 0.5;

    public static void main(String[] args) throws Exception{


        String paramPath = args[0];
        LearnItConfig.loadParams(new File(paramPath));
        String doclist = args[1];
        String nounWhiteList = args[2];
        String blacklist = args[3];
        String outputFolder = args[4];

        List<DocTheory> docTheoryList = new ArrayList<>();
        List<String> fileList = GeneralUtils.readLinesIntoList(doclist);
        docTheoryList.addAll(LoaderUtils.resolvedDocTheoryFromPathList(fileList));

        GenericEventDetector genericEventDetector = new GenericEventDetector(nounWhiteList,blacklist);
        List<DocTheory> resolvedDocTheories = new ArrayList<>();
        for(DocTheory docTheory:docTheoryList){
            resolvedDocTheories.add(genericEventDetector.addEventMentions(docTheory));
        }
        docTheoryList = resolvedDocTheories;
        SerifXMLWriter serifXMLWriter = SerifXMLWriter.create();
        for(DocTheory docTheory: docTheoryList){
            serifXMLWriter.saveTo(docTheory, outputFolder + File.separator + docTheory.docid().asString() + ".xml");
        }
    }
}
