package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay.LanguageMatchInfoDisplay;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.types.EntityType;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GenerateExamples {

    public static void main(String[] args) throws Exception {
        final String paramsFile = args[0];

        LearnItConfig.loadParams(new File(paramsFile));                             // load params file

        //final int distanceThreshold = LearnItConfig.getInt("distanceThreshold");

        final String inputList = LearnItConfig.get("engChiAlignmentFile");
        final String outputFile = LearnItConfig.get("outputFile");
        //final String patternOutputFile = LearnItConfig.get("idOutputFile");

        final Optional<ImmutableSet<Symbol>> targetEntities;         // if we want to limit relation mentions to specific entities in slot0 or slot1
        if(LearnItConfig.defined("targetEntityList")) {
          final String targetEntityList = LearnItConfig.get("targetEntityList");
          targetEntities = Optional.of(loadTargetEntities(targetEntityList));
        }
        else {
          targetEntities = Optional.absent();
        }

        //final ImmutableSet<String> entityTypesConstaint = FluentIterable.from(LearnItConfig.getList("entityTypes", ",")).toSet();


        final Collection<BilingualDocTheory> docs = LoaderUtils.loadRegularBilingualFileList( new File(inputList) );    // loads bilingual documents

        // first, let's gather all relation mentions where at least one slot involves a target entity, or satisfy certain entity type constraints
        final ImmutableList.Builder<MatchInfo> targetRelationMentions = ImmutableList.builder();
        for (BilingualDocTheory doc : docs) {
            final ImmutableList<MatchInfo> relationMentions = gatherRelationMentions(doc);  // let's just gather Mention and omit ValueMention
            final ImmutableList<MatchInfo> constraintResult = filterSourceEntityTypeConstraints(filterBothSourceSlotsPresent(relationMentions));

            final ImmutableList<MatchInfo> result;
            if(targetEntities.isPresent()) {
              result = getMentionsInvolvingTargetEntities(constraintResult, targetEntities.get());
            }
            else {
              result = constraintResult;
            }

            System.out.println(doc.getSourceDoc().docid() + " " + result.size() + " candidates out of " + relationMentions.size() + " possible");
            targetRelationMentions.addAll( result );
        }
        final ImmutableList<MatchInfo> candidates = targetRelationMentions.build();
        System.out.println("TOTAL candidates.size()=" + candidates.size());


        int bothTargetSlotsPresentCount = 0;
        int oneTargetSlotsPresentCount = 0;
        for(final MatchInfo eg : candidates) {
          if(bothTargetSlotsPresent(eg)) {
            bothTargetSlotsPresentCount += 1;
          }
          if(oneTargetSlotsPresent(eg)) {
            oneTargetSlotsPresentCount += 1;
          }
        }
        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
        writer.write(bothTargetSlotsPresentCount + " " + oneTargetSlotsPresentCount + " " + candidates.size() + "\n");
        writer.close();

        /*
        final ImmutableList.Builder<String> mentionOutputStrings = ImmutableList.builder();
        final ImmutableList.Builder<String> patternOutputStrings = ImmutableList.builder();
        for(final MatchInfo eg : candidates) {
          final Optional<String> egString = printRelationMention(eg, "NA", distanceThreshold);
          if(egString.isPresent()) {
            mentionOutputStrings.add(egString.get());
            patternOutputStrings.add(eg.uniqueIdentifier());
          }
        }


        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
        for(final String s : mentionOutputStrings.build()) {
            writer.write(s+"\n");
        }
        writer.close();

        PrintWriter patternWriter = new PrintWriter(patternOutputFile, "UTF-8");
        for(final String s : patternOutputStrings.build()) {
            patternWriter.write(s+"\n");
        }
        patternWriter.close();
        */
    }

    private static Optional<Span> getHeadSpan(final SynNode rootNode, final Optional<Spanning> inSpan) {
      if(inSpan.isPresent()) {
        return Optional.of(rootNode.coveringNodeFromTokenSpan(inSpan.get().span()).head().span());
      }
      else {
        return Optional.<Span>absent();
      }
    }

    private static Optional<String> printRelationMention(final MatchInfo eg, final String relationName, final int distanceThreshold) {
        final LanguageMatchInfo chiInfo = eg.getLanguageMatch("chinese");
        final Symbol docId = chiInfo.getDocTheory().docid();
        final int sentenceNumber = chiInfo.getSentTheory().sentenceNumber();
        final SynNode rootChiNode = chiInfo.getSentTheory().parse().root().get();

        final String chiMarkedUpTokenString = chiInfo.markedUpTokenString();

        final Optional<Span> chiSlot0Span = getHeadSpan(rootChiNode, chiInfo.getSlot0());
        final Optional<Span> chiSlot1Span = getHeadSpan(rootChiNode, chiInfo.getSlot1());
        final String chiSlot0HeadString = chiSlot0Span.isPresent()? chiSlot0Span.get().originalText().content().utf16CodeUnits() : "NULL";
        final String chiSlot1HeadString = chiSlot1Span.isPresent()? chiSlot1Span.get().originalText().content().utf16CodeUnits() : "NULL";



        final LanguageMatchInfo engInfo = eg.getLanguageMatch("english");
        final SynNode rootEngNode = engInfo.getSentTheory().parse().root().get();

        final String engMarkedUpTokenString = engInfo.markedUpTokenString();

        final Optional<Span> engSlot0Span = getHeadSpan(rootEngNode, engInfo.getSlot0());
        final Optional<Span> engSlot1Span = getHeadSpan(rootEngNode, engInfo.getSlot1());
        final String engSlot0HeadString = engSlot0Span.isPresent()? engSlot0Span.get().originalText().content().utf16CodeUnits() : "NULL";
        final String engSlot1HeadString = engSlot1Span.isPresent()? engSlot1Span.get().originalText().content().utf16CodeUnits() : "NULL";



        final String chiSlot0EntityType = getEntityType(chiInfo.getSlot0());
        final String chiSlot1EntityType = getEntityType(chiInfo.getSlot1());
        final String engSlot0EntityType = getEntityType(engInfo.getSlot0());
        final String engSlot1EntityType = getEntityType(engInfo.getSlot1());

        final String chiSlot0MentionLevel = getMentionLevel(chiInfo.getSlot0());
        final String chiSlot1MentionLevel = getMentionLevel(chiInfo.getSlot1());
        final String engSlot0MentionLevel = getMentionLevel(engInfo.getSlot0());
        final String engSlot1MentionLevel = getMentionLevel(engInfo.getSlot1());

        final int numTokensInBetween = getNumberOfTokensInBetween(chiSlot0Span, chiSlot1Span);
        if(numTokensInBetween > distanceThreshold) {
          return Optional.absent();
        }

        final int chiSlot0StartIndex = chiSlot0Span.isPresent()? chiSlot0Span.get().startIndex() : -1;
        final int chiSlot0EndIndex = chiSlot0Span.isPresent()? chiSlot0Span.get().endIndex() : -1;
        final int chiSlot1StartIndex = chiSlot1Span.isPresent()? chiSlot1Span.get().startIndex() : -1;
        final int chiSlot1EndIndex = chiSlot1Span.isPresent()? chiSlot1Span.get().endIndex() : -1;

        StringBuffer s = new StringBuffer("<relation_mention>\n");
        s.append(eg.uniqueIdentifier());
        s.append("\n");

        s.append(relationName);
        s.append("\t");
        s.append(docId);
        s.append("\t");
        s.append(sentenceNumber);
        s.append("\n");

        s.append(chiSlot0HeadString);
        s.append("\t");
        s.append(chiSlot1HeadString);
        s.append("\t");
        s.append(chiSlot0EntityType);
        s.append("\t");
        s.append(chiSlot1EntityType);
        s.append("\t");
        s.append(chiSlot0MentionLevel);
        s.append("\t");
        s.append(chiSlot1MentionLevel);
        s.append("\t");
        s.append(numTokensInBetween);
        s.append("\t");
        s.append(chiSlot0StartIndex);
        s.append("\t");
        s.append(chiSlot0EndIndex);
        s.append("\t");
        s.append(chiSlot1StartIndex);
        s.append("\t");
        s.append(chiSlot1EndIndex);
        s.append("\n");

        s.append(engSlot0HeadString);
        s.append("\t");
        s.append(engSlot1HeadString);
        s.append("\t");
        s.append(engSlot0EntityType);
        s.append("\t");
        s.append(engSlot1EntityType);
        s.append("\t");
        s.append(engSlot0MentionLevel);
        s.append("\t");
        s.append(engSlot1MentionLevel);
        s.append("\n");

        s.append(chiMarkedUpTokenString);
        s.append("\n");
        s.append(engMarkedUpTokenString);
        s.append("\n");

        s.append("</relation_mention>");

        return Optional.of(s.toString());
    }

    private static int getNumberOfTokensInBetween(final Optional<Span> span1, final Optional<Span> span2) {
        if(span1.isPresent() && span2.isPresent()) {
            final int span1Start = span1.get().startIndex();
            final int span1End = span1.get().endIndex();
            final int span2Start = span2.get().startIndex();
            final int span2End = span2.get().endIndex();
            if(span1Start < span2Start) {
                if(span1End < span2Start) {
                    return (span2Start - span1End - 1);
                }
                else {      // overlap
                    return -1;
                }
            }
            else if(span2Start < span1Start) {
                if(span2End < span1Start) {
                    return (span1Start - span2End - 1);
                }
                else {      // overlap
                    return -1;
                }
            }
            else {          // both spans start at the same token
                return -1;  // overlap
            }
        }
        else {
            return -1;
        }
    }

    private static String getSpanText(final Optional<Spanning> span) {
        if(span.isPresent()) {
            return span.get().span().originalText().content().utf16CodeUnits();
        }
        else {
            return "NULL";
        }
    }

    private static String getEntityType(final Optional<Spanning> span) {
        if(span.isPresent()) {
            final Spanning spanning = span.get();
            if(spanning instanceof Mention) {
                return ((Mention)spanning).entityType().name().toString();
            }
            else {
                return ((ValueMention)spanning).fullType().name().toString();
            }
        }
        else {
            return "NULL";
        }
    }

    private static String getMentionLevel(final Optional<Spanning> span) {
        if(span.isPresent()) {
            final Spanning spanning = span.get();
            if(spanning instanceof Mention) {
                return ((Mention)spanning).mentionType().name();
            }
            else {
                return "VALUE";
            }
        }
        else {
            return "NULL";
        }
    }


    private static ImmutableList<MatchInfo> getMentionsInvolvingTargetEntities(final ImmutableList<MatchInfo> relationMentions, final ImmutableSet<Symbol> targetEntities) {
        final ImmutableList.Builder<MatchInfo> ret = ImmutableList.builder();

        for(final MatchInfo mention : relationMentions) {
            final LanguageMatchInfo langMention = mention.getLanguageMatch("chinese");
            final Seed chiSeed = Seed.from(langMention, true);

            // if either slot is one of our target entities, we will accept this relation mention
            final Symbol slot0Trimmed = Symbol.from(chiSeed.getSlot(0).toString().replaceAll("\\s", ""));
            final Symbol slot1Trimmed = Symbol.from(chiSeed.getSlot(1).toString().replaceAll("\\s", ""));
            if(targetEntities.contains(slot0Trimmed) || targetEntities.contains(slot1Trimmed)) {
              ret.add(mention);
            }
        }

        return ret.build();
    }

    private static boolean matches(EntityType entityType, String typeString) {
        return entityType.name().asString().toUpperCase().startsWith(typeString.toUpperCase());
    }

    private static ImmutableList<MatchInfo> filterSourceEntityTypeConstraints(final ImmutableList<MatchInfo> relationMentions) {
      final ImmutableList.Builder<MatchInfo> ret = ImmutableList.builder();

      for(final MatchInfo mention : relationMentions) {
          final LanguageMatchInfo langMention = mention.getLanguageMatch("chinese");
          final Spanning slot0 = langMention.getSlot0().get();
          final Spanning slot1 = langMention.getSlot1().get();

          if((slot0 instanceof Mention) && (slot1 instanceof Mention)) {
            final EntityType e0 = ((Mention)slot0).entityType();
            final EntityType e1 = ((Mention)slot1).entityType();
            if((matches(e0, "PER") || matches(e0, "ORG") || matches(e0, "GPE") || matches(e0, "LOC")) &&
                (matches(e1, "PER") || matches(e1, "ORG") || matches(e1, "GPE") || matches(e1, "LOC"))) {
              ret.add(mention);
            }
          }
      }

      return ret.build();
  }

    private static ImmutableList<MatchInfo> filterBothSourceSlotsPresent(final ImmutableList<MatchInfo> relationMentions) {
      final ImmutableList.Builder<MatchInfo> ret = ImmutableList.builder();

      for(final MatchInfo mention : relationMentions) {
          final LanguageMatchInfo langMention = mention.getLanguageMatch("chinese");
          if(langMention.getSlot0().isPresent() && langMention.getSlot1().isPresent()) {
            ret.add(mention);
          }
      }

      return ret.build();
    }

    private static boolean bothTargetSlotsPresent(final MatchInfo relationMention) {
      final LanguageMatchInfo langMention = relationMention.getLanguageMatch("english");
      if(langMention.getSlot0().isPresent() && langMention.getSlot1().isPresent()) {
        return true;
      }
      else {
        return false;
      }
    }

    private static boolean oneTargetSlotsPresent(final MatchInfo relationMention) {
      final LanguageMatchInfo langMention = relationMention.getLanguageMatch("english");
      if(langMention.getSlot0().isPresent() || langMention.getSlot1().isPresent()) {
        return true;
      }
      else {
        return false;
      }
    }

    private static ImmutableList<MatchInfo> gatherRelationMentions(final BilingualDocTheory bidoc) {
        final ImmutableList.Builder<MatchInfo> ret = ImmutableList.builder();

        final DocTheory sourceDoc = bidoc.getSourceDoc();
        for (final SentenceTheory sourceSt : sourceDoc.sentenceTheories()) {    // for each source sentence
            // final Iterable<Spanning> sourceAllSpans = Iterables.concat(sourceSt.mentions(), sourceSt.valueMentions());
            List<Spanning> sourceAllSpans = new ArrayList<Spanning>();
            for(Mention m : sourceSt.mentions().asList())
                sourceAllSpans.add(m);
            for(ValueMention m : sourceSt.valueMentions().asList())
                sourceAllSpans.add(m);

            for (Spanning sourceSlot0 : sourceAllSpans) {
                for (Spanning sourceSlot1 : sourceAllSpans) {
                    if((sourceSlot0 != sourceSlot1) && ((sourceSlot0 instanceof Mention) || (sourceSlot1 instanceof Mention))) {    // prevent both slots valueMention
                        // create a bilingual MatchInfo
                        final MatchInfo match = MatchInfo.from(null, bidoc, sourceSt, sourceSlot0, sourceSlot1);
                        ret.add(match);
                    }
                }
            }

        }

        return ret.build();
    }

    private static ImmutableList<TargetAndScoreTables> loadExtractors() throws IOException {
        final ImmutableList.Builder<TargetAndScoreTables> ret = ImmutableList.builder();

        final Double threshold = LearnItConfig.getDouble("extractor.threshold");
        //final File[] extractorFiles = new File(LearnItConfig.get("extractor.directory")).listFiles();
        final File extractorFile = new File(LearnItConfig.get("extractor.filename"));

        //for(final File file : extractorFiles) {
        //    if(file.getName().endsWith("json")) {
                System.out.println("Loading extractor: " + extractorFile.getAbsolutePath());
                TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(extractorFile);
                extractor.setConfidenceThreshold(threshold);
                System.out.println(extractor.getTarget().toString());
                ret.add(extractor);
        //    }
        //}

        return ret.build();
    }



    private static void checkSeedStatus(final Set<MatchInfoDisplay> matches) {
        int totalCount = 0;
        int chiSeedCount = 0;
        int engSeedCount = 0;

        for(final MatchInfoDisplay match : matches) {
            final LanguageMatchInfoDisplay engMatch = match.getLanguageMatchInfoDisplay("english");
            final LanguageMatchInfoDisplay chiMatch = match.getLanguageMatchInfoDisplay("chinese");

            final Optional<Seed> chiSeed = chiMatch.getSeed();
            final Optional<Seed> engSeed = engMatch.getSeed();

            String chiSlot0 = "NULL";
            String chiSlot1 = "NULL";
            String engSlot0 = "NULL";
            String engSlot1 = "NULL";

            if(chiSeed.isPresent()) {
                chiSeedCount += 1;
                chiSlot0 = chiSeed.get().getSlot(0).toString();
                chiSlot1 = chiSeed.get().getSlot(1).toString();
            }
            if(engSeed.isPresent()) {
                engSeedCount += 1;
                engSlot0 = engSeed.get().getSlot(0).toString();
                engSlot1 = engSeed.get().getSlot(1).toString();
            }
            totalCount += 1;

            String engCanonicalSlot0 = "NULL";
            String engCanonicalSlot1 = "NULL";
            final Optional<Seed> engCanonicalSeed = engMatch.getCanonicalSeed();
            if(engCanonicalSeed.isPresent()) {
                engCanonicalSlot0 = engCanonicalSeed.get().getSlot(0).toString();
                engCanonicalSlot1 = engCanonicalSeed.get().getSlot(1).toString();
            }

            System.out.println("SEED-TRANSLATION\t" + chiSlot0 + "\t" + engSlot0 + "\t" + engCanonicalSlot0);
            System.out.println("SEED-TRANSLATION\t" + chiSlot1 + "\t" + engSlot1 + "\t" + engCanonicalSlot1);
        }

        System.out.println("Seed Counts : " + totalCount + " " + chiSeedCount + " " + engSeedCount);
    }

    private static Multimap<MatchInfo, LearnitPattern> filterNoCorefNameInSameSentence(final Multimap<MatchInfo, LearnitPattern> examples) {
        final ImmutableMultimap.Builder<MatchInfo, LearnitPattern> ret = ImmutableMultimap.builder();

        for(final MatchInfo match : examples.keySet()) {
            final LanguageMatchInfo langMatch = match.getLanguageMatch("chinese");
            final SentenceTheory st = langMatch.getSentTheory();
            final int sentenceNumber = st.sentenceNumber();

            final Optional<Spanning> slot0Span = langMatch.getSlot0();
            final Optional<Spanning> slot1Span = langMatch.getSlot1();

            boolean slot0Satisfy = false;
            boolean slot1Satisfy = false;
            final List<Entity> entities = langMatch.getDocTheory().entities().asList();
            for(final Entity entity : entities) {
                if(entity.hasNameMention()) {
                    boolean hasNameMentionInSameSentence = false;
                    boolean corefWithSlot0 = false;
                    boolean corefWithSlot1 = false;
                    // check whether this entity coref with slot0 or slot1
                    for(final Mention mention : entity.mentions()) {
                        if(mention.sentenceNumber() == sentenceNumber) {
                            if(mention.isName()) {
                                hasNameMentionInSameSentence = true;
                            }
                            final int startIndex = mention.head().span().startIndex();
                            final int endIndex = mention.head().span().endIndex();
                            if(slot0Span.isPresent() && (slot0Span.get().span().startIndex()==startIndex) && (slot0Span.get().span().endIndex()==endIndex)) {
                                corefWithSlot0 = true;
                            }
                            if(slot1Span.isPresent() && (slot1Span.get().span().startIndex()==startIndex) && (slot1Span.get().span().endIndex()==endIndex)) {
                                corefWithSlot1 = true;
                            }
                        }
                    }
                    if(hasNameMentionInSameSentence && corefWithSlot0) {
                        slot0Satisfy = true;
                    }
                    if(hasNameMentionInSameSentence && corefWithSlot1) {
                        slot1Satisfy = true;
                    }
                }
            }

            if(slot0Span.isPresent() && slot1Span.isPresent() && slot0Satisfy && slot1Satisfy) {
                ret.putAll(match, examples.get(match));
            }
            else if(slot0Span.isPresent() && slot0Satisfy) {
                ret.putAll(match, examples.get(match));
            }
            else if(slot1Span.isPresent() && slot1Satisfy) {
                ret.putAll(match, examples.get(match));
            }
        }

        return ret.build();
    }


    private static Multimap<MatchInfoDisplay, LearnitPattern> setTransliterationFlagInDisplay(
            final Multimap<MatchInfoDisplay, LearnitPattern> displays, Multimap<Symbol, Symbol> transliterations) {
        final ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern> ret = new ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern>();

        for(final MatchInfoDisplay infoDisplay : displays.keySet()) {
            final Collection<LearnitPattern> patterns = displays.get(infoDisplay);

            final Seed engSeed = infoDisplay.getLanguageMatchInfoDisplay("english").seed();
            final Seed chiSeed = infoDisplay.getLanguageMatchInfoDisplay("chinese").seed();

            int isValidTransliteration = 1;
            if(engSeed!=null && chiSeed!=null) {
                final Symbol engS0 = engSeed.getSlot(0);
                final Symbol engS1 = engSeed.getSlot(1);
                final Symbol chiS0 = chiSeed.getSlot(0);
                final Symbol chiS1 = chiSeed.getSlot(1);

                if(!transliterations.containsEntry(chiS0, engS0) || !transliterations.containsEntry(chiS1, engS1)) {
                    isValidTransliteration = 0;
                }
            }

            final MatchInfoDisplay newInfoDisplay = infoDisplay.copyWithTransliteration(isValidTransliteration);
            ret.putAll(newInfoDisplay, patterns);
        }

        return ret.build();
    }

    private static Multimap<Symbol, Symbol> loadTransliterations(final CharSource in) throws IOException {
        final ImmutableMultimap.Builder<Symbol, Symbol> ret = new ImmutableMultimap.Builder<Symbol, Symbol>();

        for(final String line : in.readLines()) {
            final String[] tokens = line.split("\t");
            final String chi = tokens[0];
            final String eng = tokens[1];
            ret.put(Symbol.from(chi), Symbol.from(eng));
        }

        return ret.build();
    }

    private static Optional<Map<Symbol, Symbol>> loadNameTranslations() throws IOException {
        if(LearnItConfig.defined("chiEngNameMappingFile")) {
            return Optional.of(loadChiEngNameMapping(Files.asCharSource(LearnItConfig.getFile("chiEngNameMappingFile"), Charsets.UTF_8)));
        }
        else {
            return Optional.absent();
        }
    }

    private static Map<Symbol, Symbol> loadChiEngNameMapping(final CharSource in) throws IOException {
        final ImmutableMap.Builder<Symbol, Symbol> ret = new ImmutableMap.Builder<Symbol, Symbol>();

        for(final String line : in.readLines()) {
            final String[] tokens = line.split("\t");
            final String chi = tokens[0];
            final String eng = tokens[1];
            //final String eng = tokens[1].substring(0, tokens[1].lastIndexOf(":"));
            ret.put(Symbol.from(chi), Symbol.from(eng));
        }

        return ret.build();
    }

    private static ImmutableSet<Symbol> loadTargetEntities(final String filename) throws IOException {
        final ImmutableSet.Builder<Symbol> ret = ImmutableSet.builder();

        final CharSource in = Files.asCharSource(new File(filename), Charsets.UTF_8);
        for(final String line : in.readLines()) {
            ret.add(Symbol.from(line));
        }

        return ret.build();
    }


}
