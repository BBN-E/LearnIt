package com.bbn.akbc.neolearnit.observers.instance;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observations.feature.AbstractRelationFeature;
import com.bbn.akbc.neolearnit.observations.feature.EntityTypeOfArg;
import com.bbn.akbc.neolearnit.observations.feature.MentionLevelOfArg;
import com.bbn.akbc.neolearnit.observations.feature.NumOfTokensInBetween;
import com.bbn.akbc.neolearnit.observations.feature.TokenSlideWindow;
import com.bbn.akbc.neolearnit.observations.feature.WordAroundArg;
import com.bbn.akbc.neolearnit.observations.feature.WordInArg;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 6/6/15.
 */
public class CommonRelationFeatureObserver extends AbstractInstanceIdObserver<AbstractRelationFeature> {

  public CommonRelationFeatureObserver(
      Recorder<InstanceIdentifier, AbstractRelationFeature> recorder) {
    super(recorder);
  }

  @Override
  public void observe(MatchInfo matchInfo) {
    if(LearnItConfig.optionalParamTrue("bilingual")) {
      System.err.println("bilingual not supported yet");
      System.exit(-1);
    }
//  System.out.println("================================================================================");
//  System.out.println("[matchInfo] " + matchInfo.toString());

    InstanceIdentifier instanceIdentifier = InstanceIdentifier.from(matchInfo);

    String arg1entType = instanceIdentifier.getSlotEntityType(0).substring(0, 3).toUpperCase();
    String arg2entType = instanceIdentifier.getSlotEntityType(0).substring(0, 3).toUpperCase();
    Optional<Mention.Type> arg1mentionLevel = instanceIdentifier.getSlotMentionType(0);
    Optional<Mention.Type> arg2mentionLevel = instanceIdentifier.getSlotMentionType(1);

    // entity type features
    this.record(matchInfo, new EntityTypeOfArg(arg1entType, 1));
    this.record(matchInfo, new EntityTypeOfArg(arg2entType, 2));
    //

    // mention level features
    if(arg1mentionLevel.isPresent())
      this.record(matchInfo, new MentionLevelOfArg(arg1mentionLevel.get(), 1));
    if(arg2mentionLevel.isPresent())
      this.record(matchInfo, new MentionLevelOfArg(arg2mentionLevel.get(), 2));
    //

    MatchInfo.LanguageMatchInfo languageMatchInfo = matchInfo.getLanguageMatch(
        matchInfo.getAllLanguages().iterator().next());
    SentenceTheory sentenceTheory = languageMatchInfo.getSentTheory();

    int nTokensInSent = languageMatchInfo.getSentTheory().tokenSequence().size();

    int arg1tokenIdxStart = languageMatchInfo.getSlot0().get().span().startIndex();
    int arg1tokenIdxEnd = languageMatchInfo.getSlot0().get().span().endIndex();
    int arg1headIdx = arg1tokenIdxEnd; // by default, use last token as head
    if (languageMatchInfo.getSlot0().get() instanceof Mention) {
      Mention m0 = (Mention)languageMatchInfo.getSlot0().get();
//    if (m0.mentionType() == Mention.Type.DESC  && m0.atomicHead().parent().isPresent())
//      arg1headIdx = m0.atomicHead().parent().get().span().endTokenIndexInclusive();
//    else
        arg1headIdx = m0.atomicHead().span().endTokenIndexInclusive();
    }

    int arg2tokenIdxStart = languageMatchInfo.getSlot1().get().span().startIndex();
    int arg2tokenIdxEnd = languageMatchInfo.getSlot1().get().span().endIndex();
    int arg2headIdx = arg2tokenIdxEnd;
    if (languageMatchInfo.getSlot1().get() instanceof Mention) {
      Mention m1 = (Mention)languageMatchInfo.getSlot1().get();
//    if (m1.mentionType() == Mention.Type.DESC  && m1.atomicHead().parent().isPresent())
//        arg2headIdx = m1.atomicHead().parent().get().span().endTokenIndexInclusive();
//      else
        arg2headIdx = m1.atomicHead().span().endTokenIndexInclusive();
    }

    // number of tokens in between arguments
    int numOfTokensInBetweenArgs = getNumOfTokensInBetweenArguments(arg1tokenIdxStart, arg1tokenIdxEnd,
        arg2tokenIdxStart, arg2tokenIdxEnd);
    this.record(matchInfo, new NumOfTokensInBetween(numOfTokensInBetweenArgs));
    //

    for(int idx=0; idx<sentenceTheory.tokenSequence().size(); idx++) {

      // slide window
      List<String> words = new ArrayList<String>();
      String tokenTextPrevious = "SENT_START";
      Optional<String> tokenTextPreviousOptional = getTokenStringByIdx(sentenceTheory, idx - 1);
      if(tokenTextPreviousOptional.isPresent())
        tokenTextPrevious = tokenTextPreviousOptional.get();
      words.add(tokenTextPrevious);

      String tokenText = getTokenStringByIdx(sentenceTheory, idx).get();
      words.add(tokenText);

      String tokenTextNext = "SENT_END";
      Optional<String> tokenTextNextOptional = getTokenStringByIdx(sentenceTheory, idx+1);
      if(tokenTextNextOptional.isPresent())
        tokenTextNext = tokenTextNextOptional.get();
      words.add(tokenTextNext);

      int distanceToHeadOfArg1 = idx-arg1headIdx;
      int distanceToHeadOfArg2 = idx-arg2headIdx;

      int distantceToArg1 = 0, distantceToArg2 = 0;
      if(idx<=arg1tokenIdxStart) distantceToArg1 = idx-arg1tokenIdxStart;
      else if(idx>=arg1tokenIdxEnd) distantceToArg1 = idx-arg1tokenIdxEnd;
      else distantceToArg1=0;
      if(idx<=arg2tokenIdxStart) distantceToArg2 = idx-arg2tokenIdxStart;
      else if(idx>=arg2tokenIdxEnd) distantceToArg2 = idx-arg2tokenIdxEnd;
      else distantceToArg2=0;


      this.record(matchInfo,
          new TokenSlideWindow(words, distanceToHeadOfArg1, distanceToHeadOfArg2));
      //

      // words
      boolean isInArg=false;
      if(idx>=arg1tokenIdxStart && idx<=arg1tokenIdxEnd) {// words in arg1
        this.record(matchInfo, new WordInArg(tokenText, 1, idx == arg1headIdx));
        isInArg = true;
      }
      if(idx>=arg2tokenIdxStart && idx<=arg2tokenIdxEnd) {// words in arg2
        this.record(matchInfo, new WordInArg(tokenText, 2, idx == arg2headIdx));
        isInArg = true;
      }
      if(!isInArg) { // outside of args
        this.record(matchInfo, new WordAroundArg(tokenText, distantceToArg1, distantceToArg2));
      }
      //
    }

//  System.out.println("================================================================================");

  }


  @Override
  protected void record(MatchInfo instance, AbstractRelationFeature relationFeature) {
    this.recorder.record(InstanceIdentifier.from(instance), relationFeature);


    // for debugging
    if(relationFeature instanceof EntityTypeOfArg) {
      EntityTypeOfArg entityTypeOfArg = (EntityTypeOfArg) relationFeature;

//    System.out.print("[record] entityTypeOfArg: " + entityTypeOfArg.toIDString() + "\n");
    }
    else if(relationFeature instanceof MentionLevelOfArg) {
      MentionLevelOfArg mentionLevelOfArg = (MentionLevelOfArg) relationFeature;

//    System.out.print("[record] mentionLevelOfArg: " + mentionLevelOfArg.toIDString() + "\n");
    }
    else if(relationFeature instanceof NumOfTokensInBetween) {
      NumOfTokensInBetween numOfTokensInBetween = (NumOfTokensInBetween) relationFeature;

//    System.out.print("[record] NumOfTokensInBetween: " + numOfTokensInBetween.toIDString() + "\n");
    }
    else if(relationFeature instanceof TokenSlideWindow) {
      TokenSlideWindow tokenSlideWindow = (TokenSlideWindow) relationFeature;

//    System.out.print("[record] tokenSlideWindow: " + tokenSlideWindow.toIDString() + "\n");
    }
    else if(relationFeature instanceof WordAroundArg) {
      WordAroundArg wordAroundArg = (WordAroundArg) relationFeature;

//    System.out.print("[record] wordAroundArg: " + wordAroundArg.toIDString() + "\n");
    }
    else if(relationFeature instanceof WordInArg) {
      WordInArg wordInArg = (WordInArg) relationFeature;

//    System.out.print("[record] WordInArg: " + wordInArg.toIDString() + "\n");
    }

  }


  public int getNumOfTokensInBetweenArguments(int arg1tokenIdxStart, int arg1tokenIdxEnd,
      int arg2tokenIdxStart, int arg2tokenIdxEnd) {
    if(arg1tokenIdxStart<=arg2tokenIdxStart) {
      if(arg1tokenIdxEnd>=arg2tokenIdxStart)
        return 0;
      else
        return arg2tokenIdxStart-arg1tokenIdxEnd;
    }
    else {
      if(arg2tokenIdxEnd>=arg1tokenIdxStart)
        return 0;
      else
        return arg1tokenIdxStart-arg2tokenIdxEnd;
    }
  }

  public Optional<String> getTokenStringByIdx(SentenceTheory sentTheory, int idx) {
    if(idx<0||idx>=sentTheory.tokenSequence().size())
      return Optional.absent();
    else
      return Optional.of(sentTheory.tokenSequence().token(idx).tokenizedText().utf16CodeUnits().trim().toLowerCase());
  }


}
