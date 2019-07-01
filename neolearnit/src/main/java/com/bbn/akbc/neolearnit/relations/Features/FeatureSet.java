package com.bbn.akbc.neolearnit.relations.Features;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.feature.AbstractRelationFeature;
import com.bbn.akbc.neolearnit.observations.feature.EntityTypeOfArg;
import com.bbn.akbc.neolearnit.observations.feature.MentionLevelOfArg;
import com.bbn.akbc.neolearnit.observations.feature.NumOfTokensInBetween;
import com.bbn.akbc.neolearnit.observations.feature.TokenSlideWindow;
import com.bbn.akbc.neolearnit.observations.feature.WordAroundArg;
import com.bbn.akbc.neolearnit.observations.feature.WordInArg;
import com.bbn.akbc.neolearnit.observations.pattern.BeforeAfterSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.relations.utility.Utility;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.learning.features.Feature;
import com.bbn.bue.learning.features.SymbolFeature;
import com.bbn.bue.learning.features.WeightedFeatureList;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FeatureSet {

  static int INIT_FEATURE_LIST_SIZE = 2;

  public static final Symbol BETWEEN_SLOTS_PATTERN = Symbol.from("BETWEEN_SLOTS_PATTERN");
  public static final Symbol BEFORE_SLOTS_PATTERN = Symbol.from("BEFORE_SLOTS_PATTERN");
  public static final Symbol AFTER_SLOTS_PATTERN = Symbol.from("AFTER_SLOTS_PATTERN");
  public static final Symbol PROP_PATTERN = Symbol.from("PROP_PATTERN");

  public static final Symbol ENTITY_TYPE_ARG1 = Symbol.from("ENTITY_TYPE_ARG1");
  public static final Symbol ENTITY_TYPE_ARG2 = Symbol.from("ENTITY_TYPE_ARG2");
  public static final Symbol ENTITY_TYPE_ARGS_COMBINE = Symbol.from("ENTITY_TYPE_ARGS_COMBINE");

  public static final Symbol MENTION_LEVEL_ARG1 = Symbol.from("MENTION_LEVEL_ARG1");
  public static final Symbol MENTION_LEVEL_ARG2 = Symbol.from("MENTION_LEVEL_ARG2");
  public static final Symbol MENTION_LEVEL_ARGS_COMBINE = Symbol.from("MENTION_LEVEL_ARGS_COMBINE");

  public static final Symbol TOKEN_IN_BETWEEN = Symbol.from("TOKEN_IN_BETWEEN");
  public static final Symbol NUM_TOKENS_IN_BETWEEN = Symbol.from("NUM_TOKENS_IN_BETWEEN");

  public static final Symbol WORD_BEFORE_SLOT = Symbol.from("WORD_BEFORE_SLOT");
  public static final Symbol WORD_AFTER_SLOT = Symbol.from("WORD_AFTER_SLOT");

  public static final Symbol WORD_ARG1 = Symbol.from("WORD_ARG1");
  public static final Symbol WORD_ARG2 = Symbol.from("WORD_ARG2");
  public static final Symbol HEAD_WORD_ARG1 = Symbol.from("HEAD_WORD_ARG1");
  public static final Symbol HEAD_WORD_ARG2 = Symbol.from("HEAD_WORD_ARG2");
  public static final Symbol HEAD_WORD_ARGS_COMBINE = Symbol.from("HEAD_WORD_ARGS_COMBINE");

  // Arg1
  Optional<String> headWordArg1 = Optional.absent();
  Optional<String> tokenLeftOfArg1 = Optional.absent();
  Optional<String> tokenRightOfArg1 = Optional.absent();
  Optional<String> arg1entType = Optional.absent();
  Optional<String> arg1mentionLevel = Optional.absent();

  // Arg2
  Optional<String> headWordArg2 = Optional.absent();
  Optional<String> tokenLeftOfArg2 = Optional.absent();
  Optional<String> tokenRightOfArg2 = Optional.absent();
  Optional<String> arg2entType = Optional.absent();
  Optional<String> arg2mentionLevel = Optional.absent();

  List<SlideWindow> listSlideWindows = new ArrayList<SlideWindow>();

  Optional<Integer> numTokensInBetween = Optional.absent();


  FeatureSet(
      Optional<String> headWordArg1,
      Optional<String> headWordArg2,
      Optional<String> tokenLeftOfArg1,
      Optional<String> tokenRightOfArg1,
      Optional<String> tokenLeftOfArg2,
      Optional<String> tokenRightOfArg2,
      List<SlideWindow> listSlideWindows,

      Optional<String> arg1entType,
      Optional<String> arg2entType,
      Optional<String> arg1mentionLevel,
      Optional<String> arg2mentionLevel,
      Optional<Integer> numTokensInBetween) {

    this.headWordArg1 = headWordArg1;
    this.headWordArg2 = headWordArg2;
    this.tokenLeftOfArg1 = tokenLeftOfArg1;
    this.tokenRightOfArg1 = tokenRightOfArg1;
    this.tokenLeftOfArg2 = tokenLeftOfArg2;
    this.tokenRightOfArg2 = tokenRightOfArg2;
    this.listSlideWindows = listSlideWindows;

    this.arg1entType = arg1entType;
    this.arg2entType = arg2entType;
    this.arg1mentionLevel = arg1mentionLevel;
    this.arg2mentionLevel = arg2mentionLevel;
    this.numTokensInBetween = numTokensInBetween;
  }



  // TODO get all interesting (including combo) features for relations
  public static Optional<FeatureSet> fromCommonRelationFeatureMappings(
      Collection<AbstractRelationFeature> commonRelationFeatures) {
    Optional<String> headWordSlot0 = Optional.absent();
    Optional<String> headWordSlot1 = Optional.absent();
    Optional<String> tokenLeftOfSlot0 = Optional.absent();
    Optional<String> tokenRightOfSlot0 = Optional.absent();
    Optional<String> tokenLeftOfSlot1 = Optional.absent();
    Optional<String> tokenRightOfSlot1 = Optional.absent();
    List<SlideWindow> listSlideWindows = new ArrayList<SlideWindow>();

    Optional<String> arg1entType = Optional.absent();
    Optional<String> arg2entType = Optional.absent();
    Optional<String> arg1mentionLevel = Optional.absent();
    Optional<String> arg2mentionLevel = Optional.absent();
    Optional<Integer> numTokensInBetween = Optional.absent();

//	  StringBuilder stringBuilderDebug = new StringBuilder();

    for (AbstractRelationFeature relationFeature : commonRelationFeatures) {
      if (relationFeature instanceof EntityTypeOfArg) {
        EntityTypeOfArg entityTypeOfArg = (EntityTypeOfArg) relationFeature;

        if (entityTypeOfArg.getSlot() == 1) {
          arg1entType = Optional.of(entityTypeOfArg.getEntityType());
        }
        if (entityTypeOfArg.getSlot() == 2) {
          arg2entType = Optional.of(entityTypeOfArg.getEntityType());
        }

//	      stringBuilderDebug.append("entityTypeOfArg: " + entityTypeOfArg.toIDString() + "\n");
      } else if (relationFeature instanceof MentionLevelOfArg) {
        MentionLevelOfArg mentionLevelOfArg = (MentionLevelOfArg) relationFeature;

        if (mentionLevelOfArg.getSlot() == 1) {
          arg1mentionLevel = Optional.of(mentionLevelOfArg.getMentionType().name());
        }
        if (mentionLevelOfArg.getSlot() == 2) {
          arg2mentionLevel = Optional.of(mentionLevelOfArg.getMentionType().name());
        }

//	      stringBuilderDebug.append("mentionLevelOfArg: " + mentionLevelOfArg.toIDString() + "\n");
      } else if (relationFeature instanceof NumOfTokensInBetween) {
        NumOfTokensInBetween numOfTokensInBetween = (NumOfTokensInBetween) relationFeature;
        numTokensInBetween = Optional.of(numOfTokensInBetween.getNumOfTokensInBetween());

//	      stringBuilderDebug.append("NumOfTokensInBetween: " + numOfTokensInBetween.toIDString() + "\n");
      } else if (relationFeature instanceof TokenSlideWindow) {
        TokenSlideWindow tokenSlideWindow = (TokenSlideWindow) relationFeature;

        listSlideWindows.add(new SlideWindow(tokenSlideWindow.getWords().get(0),
            tokenSlideWindow.getWords().get(1),
            tokenSlideWindow.getWords().get(2),
            tokenSlideWindow.getDistanceToHeadOfArg1(),
            tokenSlideWindow.getDistanceToHeadOfArg2()));

//	      stringBuilderDebug.append("tokenSlideWindow: " + tokenSlideWindow.toIDString() + "\n");
      } else if (relationFeature instanceof WordAroundArg) {
        WordAroundArg wordAroundArg = (WordAroundArg) relationFeature;

        if (wordAroundArg.getDistanceToArg1() == -1) {
          tokenLeftOfSlot0 = Optional.of(wordAroundArg.getWord());
        }
        if (wordAroundArg.getDistanceToArg1() == 1) {
          tokenRightOfSlot0 = Optional.of(wordAroundArg.getWord());
        }
        if (wordAroundArg.getDistanceToArg2() == -1) {
          tokenLeftOfSlot1 = Optional.of(wordAroundArg.getWord());
        }
        if (wordAroundArg.getDistanceToArg2() == 1) {
          tokenRightOfSlot1 = Optional.of(wordAroundArg.getWord());
        }

//	      stringBuilderDebug.append("wordAroundArg: " + wordAroundArg.toIDString() + "\n");
      } else if (relationFeature instanceof WordInArg) {
        WordInArg wordInArg = (WordInArg) relationFeature;

        if (wordInArg.getIsHeadWord()) {
          if (wordInArg.getSlot() == 1) {
            headWordSlot0 = Optional.of(wordInArg.getWord());
          }
          if (wordInArg.getSlot() == 2) {
            headWordSlot1 = Optional.of(wordInArg.getWord());
          }
        }

//	      stringBuilderDebug.append("wordAroundArg: " + wordInArg.toIDString() + "\n");
      }

//	    System.out.println(stringBuilderDebug.toString());
//	    System.out.println("================================================================");

    }

//	  if(headWordArg1.isPresent() && headWordArg2.isPresent())
    return Optional.of(new FeatureSet(
        headWordSlot0,
        headWordSlot1,
        tokenLeftOfSlot0,
        tokenRightOfSlot0,
        tokenLeftOfSlot1,
        tokenRightOfSlot1,
        listSlideWindows,
        arg1entType,
        arg2entType,
        arg1mentionLevel,
        arg2mentionLevel,
        numTokensInBetween)
    );
/*
	  else
	    return Optional.absent();
	    */
  }

  public static Optional<FeatureSet> fromRelationMention(Mention arg1, Mention arg2,
      SentenceTheory sentenceTheory) {

    Optional<String> headWordSlot0 = Optional.absent();
    Optional<String> headWordSlot1 = Optional.absent();
    Optional<String> tokenLeftOfSlot0 = Optional.absent();
    Optional<String> tokenRightOfSlot0 = Optional.absent();
    Optional<String> tokenLeftOfSlot1 = Optional.absent();
    Optional<String> tokenRightOfSlot1 = Optional.absent();
    List<SlideWindow> listSlideWindows = new ArrayList<SlideWindow>();

    Optional<String> arg1entType = Optional.absent();
    Optional<String> arg2entType = Optional.absent();
    Optional<String> arg1mentionLevel = Optional.absent();
    Optional<String> arg2mentionLevel = Optional.absent();
    Optional<Integer> numTokensInBetween = Optional.absent();

    arg1entType = Optional.of(arg1.entityType().name().toString());
    arg2entType = Optional.of(arg2.entityType().name().toString());

    arg1mentionLevel = Optional.of(arg1.mentionType().name().toString());
    arg2mentionLevel = Optional.of(arg2.mentionType().name().toString());

    int startTokenIdxArg1 = arg1.tokenSpan().startTokenIndexInclusive();
    int endTokenIdxArg1 = arg1.tokenSpan().endTokenIndexInclusive();
    int headIdxArg1 = arg1.atomicHead().span().endTokenIndexInclusive();

    int startTokenIdxArg2 = arg2.tokenSpan().startTokenIndexInclusive();
    int endTokenIdxArg2 = arg2.tokenSpan().endTokenIndexInclusive();
    int headIdxArg2 = arg2.atomicHead().span().endTokenIndexInclusive();

    numTokensInBetween = Optional.of(getNumOfTokensInBetweenArguments(
        startTokenIdxArg1, endTokenIdxArg1,
        startTokenIdxArg2, endTokenIdxArg2));

    for (int tokenIdx = 0; tokenIdx < sentenceTheory.tokenSequence().size(); tokenIdx++) {

      Optional<String> tokenString = Utility.getTokenStringByIdx(sentenceTheory, tokenIdx);

      if (tokenIdx == headIdxArg1) {
        headWordSlot0 = tokenString;
      }
      if (tokenIdx == headIdxArg2) {
        headWordSlot1 = tokenString;
      }

      if (tokenIdx == startTokenIdxArg1 - 1) // TODO: check arg order and fix
      {
        tokenLeftOfSlot0 = tokenString;
      }
      if (tokenIdx == endTokenIdxArg1 + 1) {
        tokenRightOfSlot0 = tokenString;
      }

      if (tokenIdx == startTokenIdxArg2 - 1) // TODO: check arg order and fix
      {
        tokenLeftOfSlot1 = tokenString;
      }
      if (tokenIdx == endTokenIdxArg2 + 1) {
        tokenRightOfSlot1 = tokenString;
      }

      // slide window
      List<String> words = new ArrayList<String>();
      String tokenTextPrevious = "SENT_START";
      Optional<String> tokenTextPreviousOptional =
          Utility.getTokenStringByIdx(sentenceTheory, tokenIdx - 1);
      if (tokenTextPreviousOptional.isPresent()) {
        tokenTextPrevious = tokenTextPreviousOptional.get();
      }
      words.add(tokenTextPrevious);

      words.add(tokenString.get());

      String tokenTextNext = "SENT_END";
      Optional<String> tokenTextNextOptional = Utility.getTokenStringByIdx(sentenceTheory, tokenIdx + 1);
      if (tokenTextNextOptional.isPresent()) {
        tokenTextNext = tokenTextNextOptional.get();
      }
      words.add(tokenTextNext);

      int distanceToHeadOfArg1 = tokenIdx - headIdxArg1;
      int distanceToHeadOfArg2 = tokenIdx - headIdxArg2;

      listSlideWindows.add(new SlideWindow(tokenTextPrevious, tokenString.get(), tokenTextNext,
          distanceToHeadOfArg1, distanceToHeadOfArg2));
      //
    }

    return Optional.of(new FeatureSet(
        headWordSlot0,
        headWordSlot1,
        tokenLeftOfSlot0,
        tokenRightOfSlot0,
        tokenLeftOfSlot1,
        tokenRightOfSlot1,
        listSlideWindows,
        arg1entType,
        arg2entType,
        arg1mentionLevel,
        arg2mentionLevel,
        numTokensInBetween)
    );
  }

  public static Optional<FeatureSet> fromMatchInfo(MatchInfo langIndependentMatch) {
    if (LearnItConfig.optionalParamTrue("bilingual")) {
      System.err.println("bilingual not supported yet");
      System.exit(-1);
    }

    LanguageMatchInfo match = langIndependentMatch.getLanguageMatch(langIndependentMatch.getAllLanguages().iterator().next());

    int nTokensInSent = match.getSentTheory().tokenSequence().size();

    int idxTokenLeftOfSlot0 = match.getSlot0().get().span().startIndex() - 1;
    int idxTokenRightOfSlot0 = match.getSlot0().get().span().endIndex() + 1;

    int idxTokenLeftOfSlot1 = match.getSlot1().get().span().startIndex() - 1;
    int idxTokenRightOfSlot1 = match.getSlot1().get().span().endIndex() + 1;

    Optional<String> tokenLeftOfArg1 = Utility.getTokenStringByIdx(match.getSentTheory(), idxTokenLeftOfSlot0);
    Optional<String> tokenRightOfArg1 = Utility.getTokenStringByIdx(match.getSentTheory(), idxTokenRightOfSlot0);
    Optional<String> tokenLeftOfArg2 = Utility.getTokenStringByIdx(match.getSentTheory(), idxTokenLeftOfSlot1);
    Optional<String> tokenRightOfArg2 = Utility.getTokenStringByIdx(match.getSentTheory(), idxTokenRightOfSlot1);

    Optional<String> headWordArg1 = Optional.absent();
    Optional<String> headWordArg2 = Optional.absent();

    List<SlideWindow> listSlideWindows = new ArrayList<SlideWindow>();

    //REMEMBER TO LOWER CASE TEXT
    if (match.getSlot0().get() instanceof Mention) {
      Mention m0 = (Mention) match.getSlot0().get();
      String headText;
      if (m0.mentionType() == Mention.Type.DESC && m0.atomicHead().parent().isPresent()) {
        headText = m0.atomicHead().parent().get().span().tokenizedText().utf16CodeUnits().toLowerCase();
      } else {
        headText = m0.atomicHead().span().tokenizedText().utf16CodeUnits().toLowerCase();
      }

      headWordArg1 = Optional.of(Utility.getSingleTokenHeadText(headText));
    }
    if (match.getSlot1().get() instanceof Mention) {
      Mention m1 = (Mention) match.getSlot1().get();
      String headText;
      if (m1.mentionType() == Mention.Type.DESC && m1.atomicHead().parent().isPresent()) {
        headText = m1.atomicHead().parent().get().span().tokenizedText().utf16CodeUnits().toLowerCase();
      } else {
        headText = m1.atomicHead().span().tokenizedText().utf16CodeUnits().toLowerCase();
      }

      headWordArg2 = Optional.of(Utility.getSingleTokenHeadText(headText));
    }

    // windows
    for (int i = 0; i < nTokensInSent; i++) {
      String tokenTextPrevious = "SENT_START";
      if (i > 0) {
        tokenTextPrevious = match.getSentTheory().tokenSequence().token(i - 1).tokenizedText().utf16CodeUnits()
            .toLowerCase();
      }

      String tokenText = match.getSentTheory().tokenSequence().token(i).tokenizedText().utf16CodeUnits().toLowerCase();

      String tokenTextNext = "SENT_END";
      if (i < nTokensInSent - 1) {
        tokenTextNext = match.getSentTheory().tokenSequence().token(i + 1).tokenizedText().utf16CodeUnits().toLowerCase();
      }

      listSlideWindows.add(new SlideWindow(tokenTextPrevious, tokenText, tokenTextNext,
          i - match.getSlot0().get().span().endIndex(),
          i - match.getSlot1().get().span().endIndex()));
    }

    return Optional.of(new FeatureSet(
        headWordArg1,
        headWordArg2,
        tokenLeftOfArg1,
        tokenRightOfArg1,
        tokenLeftOfArg2,
        tokenRightOfArg2,
        listSlideWindows,
        Optional.<String>absent(),
        Optional.<String>absent(),
        Optional.<String>absent(),
        Optional.<String>absent(),
        Optional.<Integer>absent()));
  }

  public static List<String> fromSeedToCoarseFeatureSet(InstanceIdentifier insID,
      Mappings mapping,
      Target target,
      Optional<String> OptionalLabelToWrite,
      Optional<String> sourceName) {
    List<String> listStrings = null;
    try {
      listStrings = fromSeedToCoarseFeatureSet(insID, mapping, target,
          Optional.<Name>absent(), Optional.<Name>absent(),
          OptionalLabelToWrite,
          sourceName);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return listStrings;
  }


  public static List<String> fromSeedToCoarseFeatureSet(InstanceIdentifier insID,
      Mappings mapping,
      Target target,
      Optional<Name> arg1nameHypothesis, Optional<Name> arg2nameHypothesis,
      Optional<String> OptionalLabelToWrite,
      Optional<String> sourceName) throws IOException {

    WeightedFeatureList outputFeatures = WeightedFeatureList.createWithCapacity(
        INIT_FEATURE_LIST_SIZE);

    // entity type features
    String arg1entType;
    if(!arg1nameHypothesis.isPresent())
      arg1entType = insID.getSlotEntityType(0).substring(0, 3).toUpperCase();
    else
      arg1entType = arg1nameHypothesis.get().type().toString().substring(0, 3).toUpperCase();

    String arg2entType;
    if(!arg2nameHypothesis.isPresent())
      arg2entType = insID.getSlotEntityType(1).substring(0, 3).toUpperCase();
    else
      arg2entType = arg2nameHypothesis.get().type().toString().substring(0, 3).toUpperCase();

    String entityTypeCombined = arg1entType + "-" + arg2entType;

    // get head words
    String headWordArg1="NA", headWordArg2="NA";
    MatchInfo matchInfo = insID.reconstructMatchInfo(target);
    String primaryLanguage = LearnItConfig.getList("languages").get(0);
    Spanning spanning0 = matchInfo.getLanguageMatch(primaryLanguage).getSlot0().get();
    Spanning spanning1 = matchInfo.getLanguageMatch(primaryLanguage).getSlot1().get();

    Optional<DocTheory> docTheoryOptional = insID.getDocTheoryFromDocID(insID.getDocid());
    if (!docTheoryOptional.isPresent())
      return new ArrayList<String>(); // return empty feature set
    DocTheory dt = docTheoryOptional.get();

    String arg1AtomicHeadText = null;
    if (arg1nameHypothesis.isPresent())
      if (arg1nameHypothesis.get().tokenSpan().isSingleToken())
        arg1AtomicHeadText = arg1nameHypothesis.get().tokenSpan().tokenizedText(dt).utf16CodeUnits().toLowerCase();
    if (arg1AtomicHeadText == null)
      arg1AtomicHeadText = Utility.getAtomicHeadText(spanning0, dt).toLowerCase(); // only use lower case

    String arg2AtomicHeadText = null;
    if (arg2nameHypothesis.isPresent())
      if (arg2nameHypothesis.get().tokenSpan().isSingleToken())
        arg2AtomicHeadText = arg2nameHypothesis.get().tokenSpan().tokenizedText(dt).utf16CodeUnits().toLowerCase();
    if (arg2AtomicHeadText == null)
      arg2AtomicHeadText = Utility.getAtomicHeadText(spanning1, dt).toLowerCase(); // only use lower case

    String[] arg1AtomicHeadTokens = arg1AtomicHeadText.trim().split(" ");
    String[] arg2AtomicHeadTokens = arg2AtomicHeadText.trim().split(" ");

    if (arg1AtomicHeadTokens.length != 0) {
      // take last token as head
      headWordArg1 = arg1AtomicHeadTokens[arg1AtomicHeadTokens.length - 1];
    }
    if (arg2AtomicHeadTokens.length != 0) {
      // take last token as head
      headWordArg2 = arg2AtomicHeadTokens[arg2AtomicHeadTokens.length - 1];
    }
    if(headWordArg1.equals("NA") || headWordArg2.equals("NA"))
      return new ArrayList<String>(); // return empty feature set
    ////

    for (LearnitPattern pattern : mapping.getPatternsForInstance(insID)) {
      /*
       * change pattern based on nameHypothesis: actually no need to do this since LearnitPattern doesn't have entity types in it.
      pattern = changeArgumentType(pattern, arg1nameHypothesis, arg2nameHypothesis); // TODO: implement change argument types on learnitPatterns
      */
      if(pattern instanceof BetweenSlotsPattern) {
        if(Utility
            .isLexicalContentful(pattern)) {
          outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
              Symbol.from(entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
        else {
          /*
          if(!isStopword(headWordArg1))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
              Symbol.from(headWordArg1 + "_NA" + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          if(!isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
              Symbol.from("NA" + "_" + headWordArg2 + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
                  */
          if(!Utility
              .isStopword(headWordArg1) && Utility
              .isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
                Symbol.from(headWordArg1 + "_" + headWordArg2 + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
      }
      else if(pattern instanceof PropPattern) {
        if(Utility
            .isLexicalContentful(pattern)) {
          outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from(entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
        else {
          /*
          if(!isStopword(headWordArg1))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from(headWordArg1 + "_NA" + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          if(!isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from("NA" + "_" + headWordArg2 + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
                  */
          if(!Utility
              .isStopword(headWordArg1) && Utility
              .isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
                Symbol.from(headWordArg1 + "_" + headWordArg2 + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
      }
    }


    // pattern counter
    if(OptionalLabelToWrite.isPresent()) {
      if(!OptionalLabelToWrite.get().equals("NA")) {
        LearnItBrandyPatternCounter.updateCounterByInstance(
            target.getName(),
            insID,
            mapping,
            target,
            headWordArg1, headWordArg2);
      }
    }


    List<String> features = new ArrayList<String>();
    Iterator<WeightedFeatureList.Cursor> it = outputFeatures.iterator();
    while(it.hasNext()) {
      WeightedFeatureList.Cursor c = it.next();

      Feature feature = c.feature();
      double weight = c.weight();

      //////////////////////////////////////////////////////
      // Here we set all features to upper case !!!!
      //////////////////////////////////////////////////////
      features.add(feature.toString().toUpperCase());
//    features.add(arg1entType + "-" + arg2entType + "-" +feature.toString());
    }

    return features;
  }

  public static List<String> fromSeedToCoarseFeatureSet(Seed seed,
      Mappings mapping,
      Target target,
      Optional<String> optionalLabelToWrite,
      Optional<String> sourceName) throws IOException {

    WeightedFeatureList outputFeatures = WeightedFeatureList.createWithCapacity(
        INIT_FEATURE_LIST_SIZE);

    Optional<Pair<String, String>> likelyEntityTypesForSeed = Utility
        .getLikelyEntityTypesForSeed(seed, mapping);

    // entity type features
    String arg1entType = "NA"; // = insID.getSlotEntityType(0).substring(0, 3).toUpperCase();
    String arg2entType = "NA"; // = insID.getSlotEntityType(1).substring(0, 3).toUpperCase();

    if(likelyEntityTypesForSeed.isPresent()) {
      arg1entType = likelyEntityTypesForSeed.get().getFirst();
      arg2entType = likelyEntityTypesForSeed.get().getSecond();
    }

    String entityTypeCombined = arg1entType + "-" + arg2entType;

    String headWordArg1 = seed.getSlotHeadText(0).toString().toUpperCase();
    String headWordArg2 = seed.getSlotHeadText(1).toString().toUpperCase();

    for (LearnitPattern pattern : mapping.getPatternsForSeed(seed)) {
      if(pattern instanceof BetweenSlotsPattern) {
        if(Utility
            .isLexicalContentful(pattern)) {
          outputFeatures.add(SymbolFeature.of(BETWEEN_SLOTS_PATTERN,
              Symbol.from(entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
        else {
          /*
          if(!isStopword(headWordArg1))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
                Symbol.from(headWordArg1 + "_NA" + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          if(!isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
                Symbol.from("NA" + "_" + headWordArg2 + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          */
          if(!Utility
              .isStopword(headWordArg1) && !Utility
              .isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
                Symbol.from(headWordArg1 + "_" + headWordArg2 + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
      }
      else if(pattern instanceof PropPattern)
        if(Utility
            .isLexicalContentful(pattern)) {
          outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from(entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
        else {
          /*
          if(!isStopword(headWordArg1))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from(headWordArg1 + "_NA" + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          if(!isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
              Symbol.from("NA" + "_" + headWordArg2 + "-" +
                  entityTypeCombined + "-" + pattern.toIDString())), 1.0);
          */
          if(!Utility
              .isStopword(headWordArg1) && !Utility
              .isStopword(headWordArg2))
            outputFeatures.add(new SymbolFeature(PROP_PATTERN,
                Symbol.from(headWordArg1 + "_" + headWordArg2 + "-" +
                    entityTypeCombined + "-" + pattern.toIDString())), 1.0);
        }
    }

    // count patterns
    Collection<InstanceIdentifier> seedInstances = mapping.getInstancesForSeed(seed);
    for(InstanceIdentifier instanceIdentifier : seedInstances) {
      // pattern counter
      if(optionalLabelToWrite.isPresent()) {
        if(!optionalLabelToWrite.get().equals("NA")) {
          LearnItBrandyPatternCounter.updateCounterByInstance(
              target.getName(),
              instanceIdentifier,
              mapping,
              target,
              headWordArg1, headWordArg2);
        }
      }
    }
    //

    List<String> features = new ArrayList<String>();
    Iterator<WeightedFeatureList.Cursor> it = outputFeatures.iterator();
    while(it.hasNext()) {
      WeightedFeatureList.Cursor c = it.next();

      Feature feature = c.feature();
      double weight = c.weight();

      //////////////////////////////////////////////////////
      // Here we set all features to upper case !!!!
      //////////////////////////////////////////////////////
      features.add(feature.toString().toUpperCase());
//    features.add(arg1entType + "-" + arg2entType + "-" +feature.toString());
    }

    return features;
  }



  public static List<String> fromInstIdToRichFeatures(InstanceIdentifier insID,
      Mappings mapping,
      Target target) {
    List<String> listStrings = null;
    try {
      listStrings = fromInstIdToRichFeatures(insID,
          mapping,
          target,
          Optional.<Name>absent(), Optional.<Name>absent());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return listStrings;
  }

  public static List<String> fromInstIdToRichFeatures(InstanceIdentifier insID,
      Mappings mapping,
      Target target,
      Optional<Name> arg1nameHypothesis, Optional<Name> arg2nameHypothesis) throws IOException {
    WeightedFeatureList outputFeatures = WeightedFeatureList.createWithCapacity(
        INIT_FEATURE_LIST_SIZE);

    try {
      for (LearnitPattern pattern : mapping.getPatternsForInstance(insID)) {

        /*
         * change pattern based on nameHypothesis: actually no need to do this since LearnitPattern doesn't have entity types in it.
        pattern = changeArgumentType(pattern, arg1nameHypothesis,
            arg2nameHypothesis); // TODO: implement change argument types on learnitPatterns
        //
        */

        if (pattern instanceof BetweenSlotsPattern) {
          if(Utility
              .isLexicalContentful(pattern)) {
            outputFeatures.add(new SymbolFeature(BETWEEN_SLOTS_PATTERN,
                Symbol.from(Utility.getPatternString(pattern))), 1.0);
          }
          BetweenSlotsPattern p = (BetweenSlotsPattern) pattern;
          if(p.getLexicalItems().size()<=5) {
            for (Symbol lexicalItem : p.getLexicalItems()) {
              if (!Utility
                  .isStopword(lexicalItem.toString()))
                outputFeatures.add(new SymbolFeature(TOKEN_IN_BETWEEN, lexicalItem), 1.0);
            }
          }
          outputFeatures.add(new SymbolFeature(NUM_TOKENS_IN_BETWEEN,
              Symbol.from(Integer.toString(p.getLexicalItems().size()))), 1.0);
        } else if (pattern instanceof PropPattern)
          if(Utility
              .isLexicalContentful(pattern)) {
            outputFeatures
                .add(new SymbolFeature(PROP_PATTERN, Symbol.from(Utility.getPatternString(pattern))), 1.0);
          }
          else if (pattern instanceof BeforeAfterSlotsPattern) {
            /*
          BeforeAfterSlotsPattern p = (BeforeAfterSlotsPattern) pattern;
          if (p.isBeforeText()) {
            for(BeforeAfterSlotsPattern pVariant : p.getAllVersions()) {
              if(isLexicalContentful(pVariant)) {
                outputFeatures
                    .add(new SymbolFeature(BEFORE_SLOTS_PATTERN, Symbol.from(getPatternString(
                        pVariant))), 1.0);
              }
            }
            for (Symbol lexicalItem : p.getLexicalItems()) {
              if (!isStopword(lexicalItem.toString())) {
                outputFeatures.add(new SymbolFeature(WORD_BEFORE_SLOT, lexicalItem), 1.0);
              }
            }
          } else {
            for(BeforeAfterSlotsPattern pVariant : p.getAllVersions()) {
              if(isLexicalContentful(pVariant)) {
                outputFeatures
                    .add(new SymbolFeature(AFTER_SLOTS_PATTERN, Symbol.from(getPatternString(
                        pVariant))), 1.0);
              }
            }
            for (Symbol lexicalItem : p.getLexicalItems()) {
              if(!isStopword(lexicalItem.toString())) {
                outputFeatures.add(new SymbolFeature(WORD_AFTER_SLOT, lexicalItem), 1.0);
              }
            }
          }
                      */
          } else {
            System.err.println(
                "Error pattern type: " + pattern.getClass() + "\t" + Utility.getPatternString(pattern));
          }
      }

      // entity type features
      String arg1entType;
      if (!arg1nameHypothesis.isPresent())
        arg1entType = insID.getSlotEntityType(0).substring(0, 3).toUpperCase();
      else
        arg1entType = arg1nameHypothesis.get().type().toString().substring(0, 3).toUpperCase();

      String arg2entType;
      if (!arg2nameHypothesis.isPresent())
        arg2entType = insID.getSlotEntityType(1).substring(0, 3).toUpperCase();
      else
        arg2entType = arg2nameHypothesis.get().type().toString().substring(0, 3).toUpperCase();

//    outputFeatures.add(new SymbolFeature(ENTITY_TYPE_ARG1, Symbol.from(arg1entType)), 1.0);
//    outputFeatures.add(new SymbolFeature(ENTITY_TYPE_ARG2, Symbol.from(arg2entType)), 1.0);
      outputFeatures.add(
          new SymbolFeature(ENTITY_TYPE_ARGS_COMBINE, Symbol.from(arg1entType + "-" + arg2entType)),
          1.0);

      /*
      // mention level features
      Optional<Mention.Type> arg1mentionLevel = insID.getSlotMentionType(0);
      Optional<Mention.Type> arg2mentionLevel = insID.getSlotMentionType(1);
      if (arg1mentionLevel.isPresent())
        outputFeatures.add(
            new SymbolFeature(MENTION_LEVEL_ARG1, Symbol.from(arg1mentionLevel.get().toString())),
            1.0);
      if (arg2mentionLevel.isPresent())
        outputFeatures.add(
            new SymbolFeature(MENTION_LEVEL_ARG2, Symbol.from(arg2mentionLevel.get().toString())),
            1.0);
      if (arg1mentionLevel.isPresent() && arg2mentionLevel.isPresent())
        outputFeatures.add(new SymbolFeature(MENTION_LEVEL_ARGS_COMBINE, Symbol
                .from(arg1mentionLevel.get().toString() + "-" + arg2mentionLevel.get().toString())),
            1.0);
      */

      // head word features
      MatchInfo matchInfo = insID.reconstructMatchInfo(target);
      String primaryLanguage = LearnItConfig.getList("languages").get(0);
      Spanning spanning0 = matchInfo.getLanguageMatch(primaryLanguage).getSlot0().get();
      Spanning spanning1 = matchInfo.getLanguageMatch(primaryLanguage).getSlot1().get();

      // DocTheory dt = ereLabeler.getDocTheory(insID.getDocid());
      Optional<DocTheory> docTheoryOptional = insID.getDocTheoryFromDocID(insID.getDocid());
      if (!docTheoryOptional.isPresent())
        return new ArrayList<String>(); // return empty feature set
      DocTheory dt = docTheoryOptional.get();

      String arg1AtomicHeadText = null;
      if (arg1nameHypothesis.isPresent())
        if (arg1nameHypothesis.get().tokenSpan().isSingleToken())
          arg1AtomicHeadText = arg1nameHypothesis.get().tokenSpan().tokenizedText(dt).utf16CodeUnits().toLowerCase();
      if (arg1AtomicHeadText == null)
        arg1AtomicHeadText = Utility.getAtomicHeadText(spanning0, dt).toLowerCase(); // only use lower case

      String arg2AtomicHeadText = null;
      if (arg2nameHypothesis.isPresent())
        if (arg2nameHypothesis.get().tokenSpan().isSingleToken())
          arg2AtomicHeadText = arg2nameHypothesis.get().tokenSpan().tokenizedText(dt).utf16CodeUnits().toLowerCase();
      if (arg2AtomicHeadText == null)
        arg2AtomicHeadText = Utility.getAtomicHeadText(spanning1, dt).toLowerCase(); // only use lower case

      String[] arg1AtomicHeadTokens = arg1AtomicHeadText.trim().split(" ");
      String[] arg2AtomicHeadTokens = arg2AtomicHeadText.trim().split(" ");

      /*
      if (arg1AtomicHeadTokens.length != 0) {
        for (int i = 0; i < arg1AtomicHeadTokens.length - 1; i++) {
          if(!isStopword(arg1AtomicHeadTokens[i])) {
            outputFeatures
                .add(new SymbolFeature(WORD_ARG1, Symbol.from(arg1AtomicHeadTokens[i])), 1.0);
          }
        }
        // take last token as head
        String headArg1 = arg1AtomicHeadTokens[arg1AtomicHeadTokens.length - 1];
        if(!isStopword(headArg1))
          outputFeatures.add(new SymbolFeature(HEAD_WORD_ARG1, Symbol.from(headArg1)), 1.0);
      }
      if (arg2AtomicHeadTokens.length != 0) {
        for (int i = 0; i < arg2AtomicHeadTokens.length - 1; i++) {
          if(!isStopword(arg2AtomicHeadTokens[i])) {
            outputFeatures
                .add(new SymbolFeature(WORD_ARG2, Symbol.from(arg2AtomicHeadTokens[i])), 1.0);
          }
        }
        // take last token as head
        String headArg2 = arg2AtomicHeadTokens[arg2AtomicHeadTokens.length - 1];
        if(!isStopword(headArg2))
          outputFeatures.add(new SymbolFeature(HEAD_WORD_ARG2, Symbol.from(headArg2)), 1.0);
      }
      */
      if (arg1AtomicHeadTokens.length != 0 && arg2AtomicHeadTokens.length != 0) {
        // take last token as head
        String headArg1 = arg1AtomicHeadTokens[arg1AtomicHeadTokens.length - 1];
        String headArg2 = arg2AtomicHeadTokens[arg2AtomicHeadTokens.length - 1];
        if(!Utility
            .isStopword(headArg1) && !Utility
            .isStopword(headArg2)) {
          outputFeatures
              .add(
                  new SymbolFeature(HEAD_WORD_ARGS_COMBINE, Symbol.from(headArg1 + "-" + headArg2)),
                  1.0);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    List<String> features = new ArrayList<String>();
    Iterator<WeightedFeatureList.Cursor> it = outputFeatures.iterator();
    while(it.hasNext()) {
      WeightedFeatureList.Cursor c = it.next();

      Feature feature = c.feature();
      double weight = c.weight();

      //////////////////////////////////////////////////////
      // Here we set all features to lower case !!!!
      //////////////////////////////////////////////////////
      features.add(feature.toString().toUpperCase());
//    features.add(arg1entType + "-" + arg2entType + "-" +feature.toString());
    }

    return features;
  }


  public static int getNumOfTokensInBetweenArguments(int arg1tokenIdxStart, int arg1tokenIdxEnd,
      int arg2tokenIdxStart, int arg2tokenIdxEnd) {
    if (arg1tokenIdxStart <= arg2tokenIdxStart) {
      if (arg1tokenIdxEnd >= arg2tokenIdxStart) {
        return 0;
      } else {
        return arg2tokenIdxStart - arg1tokenIdxEnd;
      }
    } else {
      if (arg2tokenIdxEnd >= arg1tokenIdxStart) {
        return 0;
      } else {
        return arg1tokenIdxStart - arg2tokenIdxEnd;
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("headWordArg1=" + Utility.getOptionalStr(headWordArg1) + " ");
    sb.append("headWordArg2=" + Utility.getOptionalStr(headWordArg2) + " ");

    sb.append("tokenLeftOfArg1=" + Utility.getOptionalStr(tokenLeftOfArg1) + " ");
    sb.append("tokenRightOfArg1=" + Utility.getOptionalStr(tokenRightOfArg1) + " ");
    sb.append("tokenLeftOfArg2=" + Utility.getOptionalStr(tokenLeftOfArg2) + " ");
    sb.append("tokenRightOfArg2=" + Utility.getOptionalStr(tokenRightOfArg2) + " ");

    sb.append("arg1entType=" + Utility.getOptionalStr(arg1entType) + " ");
    sb.append("arg2entType=" + Utility.getOptionalStr(arg2entType) + " ");
    sb.append("arg1mentionLevel=" + Utility.getOptionalStr(arg1mentionLevel) + " ");
    sb.append("arg2mentionLevel=" + Utility.getOptionalStr(arg2mentionLevel) + " ");
    sb.append("numTokensInBetween=" + Utility.getOptionalInt(numTokensInBetween) + " ");

    for (SlideWindow w : listSlideWindows) {
      sb.append("window=" + w.toString() + " ");
    }

    return sb.toString().trim();
  }
}
