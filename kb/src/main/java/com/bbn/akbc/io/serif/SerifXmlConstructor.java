package com.bbn.akbc.io.serif;

import com.bbn.akbc.common.Pair;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.languages.EnglishSerifLanguage;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Document;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.MentionConfidence;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.Names;
import com.bbn.serif.theories.NestedName;
import com.bbn.serif.theories.Region;
import com.bbn.serif.theories.Sentence;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bmin on 4/4/16.
 */
public class SerifXmlConstructor {

  static SerifXMLWriter serifxmlWriter = SerifXMLWriter.create();

  static List<LocatedString> splitByDelimiter(LocatedString originalText, String delimiter) {
    List<LocatedString> results = new ArrayList<LocatedString>();

    int length = originalText.referenceBounds().endCharOffsetInclusive().asInt() -
        originalText.referenceBounds().endCharOffsetInclusive().asInt();

    int offset = 0;
    while(offset<=length) {
      int offset_start = offset;
      int offset_end = originalText.content().utf16CodeUnits().indexOf(delimiter, offset_start);
      if(offset_end<0)
        offset_end = length-1;
      else
        offset_end=offset_end-1;

      CharOffset charOffset_start =
          CharOffset.asCharOffset(offset_start + originalText.referenceBounds().startCharOffsetInclusive().asInt());
      CharOffset charOffset_end =
          CharOffset.asCharOffset(offset_end + originalText.referenceBounds().startCharOffsetInclusive().asInt());

      OffsetRange<CharOffset> offsetRange = OffsetRange.charOffsetRange(charOffset_start.asInt(), charOffset_end.asInt());
      LocatedString substring = originalText.contentLocatedSubstringByReferenceOffsets(offsetRange);

      results.add(substring);

      offset = offset_end + 2;
    }

    return results;
  }

  static List<LocatedString> tagByString(LocatedString originalText, String textString) {
    List<LocatedString> results = new ArrayList<LocatedString>();

    int offset = 0;
    while(true) {
//      int offset_start = offset;
      int offset_start = originalText.content().utf16CodeUnits().indexOf(textString, offset);
      int offset_end = offset_start + textString.length()-1;

      if(offset_start<0)
        break;

      CharOffset charOffset_start =
          CharOffset.asCharOffset(offset_start + originalText.referenceBounds().startCharOffsetInclusive().asInt());
      CharOffset charOffset_end =
          CharOffset.asCharOffset(offset_end + originalText.referenceBounds().startCharOffsetInclusive().asInt());

      OffsetRange<CharOffset> offsetRange = OffsetRange.charOffsetRange(charOffset_start.asInt(), charOffset_end.asInt());
      LocatedString substring = originalText.contentLocatedSubstringByReferenceOffsets(offsetRange);

      results.add(substring);

      offset = offset_end;
    }

    return results;
  }

  /* start resources */
  static ImmutableMap<String, EntityType> namesString2Type = ImmutableMap.of(
      "Hu Jintao", EntityType.PER,
      "the Unites States", EntityType.GPE,
      "the United Nations", EntityType.ORG);
  static ImmutableMap<String, EntityType> nestedNameString2Type = ImmutableMap.of(
      "States", EntityType.GPE);
  static ImmutableMap<String, ValueType> valueString2Type = ImmutableMap.of(
      "Friday", ValueType.parseDottedPair(Symbol.from("TIMEX2").asString()));

  /* end resources */

  static Pair<SynNode, List<Mention>> generateParseWithAllNnWithConstraints(TokenSequence tokenSequence, Names nameTheory, ValueMentions valueMentionTheory) {
    List<Mention> mentions = new ArrayList<Mention>();

    Map<Integer, SynNode> startTokenIdx2synNode = new HashMap<Integer, SynNode>();

    SynNode.NonterminalBuilder rootBuilder = SynNode.nonterminalBuilder(Symbol.from("S"));

    // enforce names
    for(Name name : nameTheory.asList()) {
      SynNode.NonterminalBuilder nameRootBuilder = SynNode.nonterminalBuilder(Symbol.from("NP"));

      for(int tokenIdx = name.tokenSpan().startTokenIndexInclusive(); tokenIdx<=name.tokenSpan().endTokenIndexInclusive(); tokenIdx++) {
        LocatedString tokenText = tokenSequence.token(tokenIdx).originalText();

        SynNode.NonterminalBuilder synNodePreTerminalBuilder =
            SynNode.nonterminalBuilder(Symbol.from("NN"));
        synNodePreTerminalBuilder.appendHead(
            SynNode.terminalBuilder(Symbol.from(tokenText.content().utf16CodeUnits()))
                .tokenIndex(tokenIdx).build(tokenSequence));

        System.out.println("name tokenIdx="+tokenIdx);

        if(tokenIdx==name.tokenSpan().startTokenIndexInclusive())
          nameRootBuilder.appendHead(synNodePreTerminalBuilder.build(tokenSequence));
        else
          nameRootBuilder.appendNonHead(synNodePreTerminalBuilder.build(tokenSequence));
      }

//          SynNode headNode = parse.nodeForToken(headToken);
      SynNode nameRootSynNode = nameRootBuilder.build(tokenSequence);

      Mention mention = nameRootSynNode.setMention(Mention.typeForSymbol(Symbol.from("name")),
            name.type(), EntitySubtype.undetermined(),
            new Mention.MetonymyInfo(name.type(), name.type()),
            1.0, 1.0, getId("mention"));
      mentions.add(mention);

//      rootBuilder.appendNonHead();
      startTokenIdx2synNode.put(name.tokenSpan().startTokenIndexInclusive(), nameRootSynNode);
    }
    // enforce value mentions
    for(ValueMention valueMention : valueMentionTheory.asList()) {
      SynNode.NonterminalBuilder valueRootBuilder = SynNode.nonterminalBuilder(Symbol.from("NP"));

      for(int tokenIdx = valueMention.tokenSpan().startTokenIndexInclusive(); tokenIdx<=valueMention.tokenSpan().endTokenIndexInclusive(); tokenIdx++) {
        LocatedString tokenText = tokenSequence.token(tokenIdx).originalText();

        SynNode.NonterminalBuilder synNodePreTerminalBuilder =
            SynNode.nonterminalBuilder(Symbol.from("NN"));
        synNodePreTerminalBuilder.appendHead(
            SynNode.terminalBuilder(Symbol.from(tokenText.content().utf16CodeUnits()))
                .tokenIndex(tokenIdx).build(tokenSequence));

        System.out.println("value tokenIdx="+tokenIdx);

        if(tokenIdx==valueMention.tokenSpan().startTokenIndexInclusive())
          valueRootBuilder.appendHead(synNodePreTerminalBuilder.build(tokenSequence));
        else
          valueRootBuilder.appendNonHead(synNodePreTerminalBuilder.build(tokenSequence));
      }

//      rootBuilder.appendNonHead();

      startTokenIdx2synNode.put(valueMention.tokenSpan().startTokenIndexInclusive(), valueRootBuilder.build(tokenSequence));

    }

    // add other nodes
    boolean isFirstToken = true;
    for(int tokenIdx=0; tokenIdx<tokenSequence.size(); tokenIdx++) {
      boolean shouldSkip=false;
      for(Name name : nameTheory.asList()) {
        if(tokenIdx>=name.tokenSpan().startTokenIndexInclusive() &&
            tokenIdx<=name.tokenSpan().endTokenIndexInclusive()) {
          shouldSkip=true;
          break;
        }
      }
      for(ValueMention valueMention : valueMentionTheory.asList()) {
        if(tokenIdx>=valueMention.tokenSpan().startTokenIndexInclusive() &&
            tokenIdx<=valueMention.tokenSpan().endTokenIndexInclusive()) {
          shouldSkip=true;
          break;
        }
      }
      if(shouldSkip)
        continue;

      System.out.println("token tokenIdx="+tokenIdx);

      LocatedString tokenText = tokenSequence.token(tokenIdx).originalText();

      SynNode.NonterminalBuilder synNodePreTerminalBuilder =
          SynNode.nonterminalBuilder(Symbol.from("NN"));
      synNodePreTerminalBuilder.appendHead(
          SynNode.terminalBuilder(Symbol.from(tokenText.content().utf16CodeUnits()))
              .tokenIndex(tokenIdx).build(tokenSequence));

      startTokenIdx2synNode.put(tokenIdx, synNodePreTerminalBuilder.build(tokenSequence));

      /*
      if(isFirstToken) {
        rootBuilder.appendHead(synNodeTerminal);
        isFirstToken = false;
      } else
        rootBuilder.appendNonHead(synNodeTerminal);
        */
    }

    boolean isHead = true;
    for(int tokenIdx=0; tokenIdx<tokenSequence.size(); tokenIdx++) {
      if(!startTokenIdx2synNode.containsKey(tokenIdx))
        continue;
      SynNode synNode = startTokenIdx2synNode.get(tokenIdx);

      if(isHead) {
        rootBuilder.appendHead(synNode);
        isHead = false;
      } else
        rootBuilder.appendNonHead(synNode);
    }

    return new Pair<SynNode, List<Mention>>(rootBuilder.build(tokenSequence), mentions);
  }


  static Multiset<String> counterForTypes = HashMultiset.create();
  public static Symbol getId(String type) {
    counterForTypes.add(type);
    return Symbol.from(type + "-" + Integer.toString(counterForTypes.count(type)));
  }
  public static void main(String [] argv) {

    // test entity
    List<Mention> testMentions = new ArrayList<Mention>();
    Map<Mention, MentionConfidence> testMentionConfidences = new HashMap<Mention, MentionConfidence>();

    // original text
//  LocatedString originalText =
//      LocatedString.fromStringStartingAtZero("Chinese president Hu Jintao visited the Unites States on Friday");
    LocatedString originalText =
        LocatedString.fromReferenceString("Chinese president Hu Jintao visited the Unites States on Friday");

    List<Region> regions = new ArrayList<Region>();
    Region region = Region.fromTagAndContent(Symbol.from("text"), originalText).build();
    regions.add(region);

    Document doc = Document.withNameAndLanguage(Symbol.from("TestNewsDoc"),
        EnglishSerifLanguage.getInstance())
        .withRegions(regions)
        .withOriginalText(originalText)
        .withSourceType(Symbol.from("sgm"))
        .build();

    // doc theory
    DocTheory.Builder docTheoryBuilder = DocTheory.builderForDocument(doc);

    List<LocatedString> sentenceLocatedStrings = splitByDelimiter(originalText, ".");
    for(int sentenceIdx=0; sentenceIdx<sentenceLocatedStrings.size(); sentenceIdx++) {
      System.out.println("Process sentence " + sentenceIdx);
      LocatedString sentenceLocatedString = sentenceLocatedStrings.get(sentenceIdx);

      Sentence sentence = Sentence.forSentenceInDocument(doc, sentenceIdx)
          .withContent(sentenceLocatedString)
          .withRegion(region).build();

      System.out.println("sentence: " + sentence.locatedString().content().utf16CodeUnits());

      // token sequence
      TokenSequence.FromTokenDataBuilder tokenSequenceBuilder = TokenSequence.withOriginalText(sentenceIdx,
          sentenceLocatedString);
      List<LocatedString> tokenLocatedStrings = splitByDelimiter(sentenceLocatedString, " ");

      CharOffset end_charoffset = sentenceLocatedString.referenceBounds().endCharOffsetInclusive();
      System.out.println("end_charoffset=" + end_charoffset.value());
      OffsetGroup offsetGroup = sentenceLocatedString.endReferenceOffsetsForContentOffset(end_charoffset);

      /*
      for(int i=0; i<63; i++) {
        System.out.println("i="+i);
        offsetGroup = sentenceLocatedString.offsetGroupForCharOffset(CharOffset.asCharOffset(i));
      }
      */

      for(LocatedString tokenLocatedString : tokenLocatedStrings) {
        System.out.println("tokenLocatedString: " + tokenLocatedString.toString());

        OffsetRange<CharOffset> offsetRange = OffsetRange.charOffsetRange(
            tokenLocatedString.referenceBounds().startCharOffsetInclusive().asInt(),
            tokenLocatedString.referenceBounds().endCharOffsetInclusive().asInt());

        tokenSequenceBuilder.addToken(Symbol.from(tokenLocatedString.content().utf16CodeUnits()),
            offsetRange);
      }
      TokenSequence tokenSequence = tokenSequenceBuilder.build();

      System.out.println("tokenSequence: " + tokenSequence.toString());

      // sentence
      SentenceTheory.Builder sentenceTheoryBuilder = SentenceTheory.createForTokenSequence(sentence, tokenSequence);

      // names and nested names
      List<Name> names = new ArrayList<Name>();
      List<NestedName> nestedNames = new ArrayList<NestedName>();
      for(String nameStringInDictionary : namesString2Type.keySet()) {
        EntityType entityType = namesString2Type.get(nameStringInDictionary);
        for (LocatedString nameLocatedString : tagByString(sentenceLocatedString, nameStringInDictionary)) {
          System.out.println("nameLocatedString=" + nameLocatedString.toString());
          Optional<TokenSequence.Span> tokenSpanOptional = tokenSequence.spanFromCharacterOffsets(
              OffsetRange.charOffsetRange(
              nameLocatedString.referenceBounds().startCharOffsetInclusive().asInt(),
                  nameLocatedString.referenceBounds().endCharOffsetInclusive().asInt()));
          if(tokenSpanOptional.isPresent()) {
            Name name = Name.builder(tokenSpanOptional.get(), entityType).build();
            names.add(name);
            System.out.println("name: " + name.toString());

            // find nested names
            for(String nestedNameStringInDictionary : nestedNameString2Type.keySet()) {
              EntityType nestedEntityType = nestedNameString2Type.get(nestedNameStringInDictionary);
              for (LocatedString nestedNameLocatedString : tagByString(nameLocatedString, nestedNameStringInDictionary)) {
                System.out.println("nestedName: " + nestedNameLocatedString.toString());
                Optional<TokenSequence.Span> nestedTokenSpanOptional = tokenSequence.spanFromCharacterOffsets(OffsetRange.charOffsetRange(
                    nestedNameLocatedString.referenceBounds().startCharOffsetInclusive().asInt(),
                    nestedNameLocatedString.referenceBounds().endCharOffsetInclusive().asInt()));
                if(nestedTokenSpanOptional.isPresent()) {
                  NestedName nestedName = NestedName.builder(nestedTokenSpanOptional.get(), nestedEntityType, name).build();
                  nestedNames.add(nestedName);
                  System.out.println("nestedName: " + nestedName.toString());
                }
              }
            }
          }
        }
      }
      Names nameTheory = Names.createFrom(names, tokenSequence, 1.0);

      /*
      NestedNames nestedNameTheory = NestedNames.builder(
          ImmutableList.<NestedName>copyOf(nestedNames), tokenSequence, nameTheory).build();
      sentenceTheoryBuilder.withNameTheory(nameTheory);
      sentenceTheoryBuilder.withNestedNameTheory(nestedNameTheory);
      */

      //

      /*
      // value
      List<ValueMention> valueMentions = new ArrayList<ValueMention>();
      for(String valueStringInDictionary : valueString2Type.keySet()) {
        ValueType valueType = valueString2Type.get(valueStringInDictionary);
        for(LocatedString valueLocatedString : tagByString(sentenceLocatedString, valueStringInDictionary)) {
          Optional<TokenSequence.Span> valueSpanOptional = tokenSequence.spanFromCharacterOffsets(OffsetRange.charOffsetRange(
              valueLocatedString.startCharOffset().value(), valueLocatedString.endCharOffset().value()));
          if(valueSpanOptional.isPresent()) {
            ValueMention valueMention = ValueMention.builder(valueType, valueSpanOptional.get()).build();
            valueMentions.add(valueMention);
            System.out.println("valueMention: " + valueMention.toString());
          }
        }
      }
      ValueMentions valueMentionTheory = ValueMentions.create(valueMentions);
      sentenceTheoryBuilder.withValueMentions(valueMentionTheory);

      System.out.println("tokenSequence="+tokenSequence.toString());
      for(Name name : nameTheory.asList())
        System.out.println("nameINTheory="+name.toString());
      for(ValueMention valueMention : valueMentions)
        System.out.println("valueMentionTheory="+valueMention.toString());


      // parse
      Pair<SynNode, List<Mention>> rootSynNodeAndMentions =
          generateParseWithAllNnWithConstraints(tokenSequence, nameTheory, valueMentionTheory);
      */

      /*

      // mention
      List<Mention> mentions = new ArrayList<Mention>();
      for(Name name : nameTheory.asList()) {
        // Token headToken = tokenSequence.token(name.tokenSpan().startTokenIndexInclusive());

        Optional<SynNode> headNodeOptional = rootSynNode.nodeByTokenSpan(name.span());

        if(headNodeOptional.isPresent()) {
//          SynNode headNode = parse.nodeForToken(headToken);
          Mention mention = headNodeOptional.get().setMention(Mention.typeForSymbol(Symbol.from("name")),
              name.type(), EntitySubtype.UNDET,
              new Mention.MetonymyInfo(name.type(), name.type()),
              1.0, 1.0, getId("mention"));
          mentions.add(mention);
        }
      }
      */

      /*
      SynNode rootSynNode = rootSynNodeAndMentions.key;
      List<Mention> mentions = rootSynNodeAndMentions.value;

      Mentions mentionSet = Mentions.of(mentions, 1.0f, 1.0f);
      sentenceTheoryBuilder.withMentionSet(mentionSet);

      // seal and attach parse
      Parse parse = Parse.create(tokenSequence, rootSynNode, 1.0f);
      sentenceTheoryBuilder.withParse(parse);

      // metonymy
      // No interface in jserif

      // entity
      // TODO!!! - no interfaces in jserif

      // event
      List<EventMention> eventMentions = new ArrayList<EventMention>();
      Optional<Mention> mentionArg1Optional = Optional.absent();
      Optional<Mention> mentionArg2Optional = Optional.absent();
      Optional<ValueMention> valueMentionOptional = Optional.absent();
      for(Mention mention : mentionSet.asList()) {
        if(mention.tokenSpan().originalText().get().text().contains("Hu Jintao"))
          mentionArg1Optional = Optional.of(mention);
        if(mention.tokenSpan().originalText().get().text().contains("the Unites States"))
          mentionArg2Optional = Optional.of(mention);
      }
      for(ValueMention valueMention : valueMentionTheory.asList()) {
        if(valueMention.tokenSpan().originalText().get().text().contains("Friday"))
          valueMentionOptional = Optional.of(valueMention);
      }
      Optional<SynNode> anchorNodeOptional = Optional.absent();
      for(int tokenIdx=0; tokenIdx<tokenSequence.size(); tokenIdx++) {
        SynNode synNode = parse.nodeForToken(tokenSequence.token(tokenIdx));
        if(synNode.tokenSpan().originalText().get().text().contains("visit"))
          anchorNodeOptional = Optional.of(synNode);
      }
      if(anchorNodeOptional.isPresent() && mentionArg1Optional.isPresent()
          && mentionArg2Optional.isPresent()) {
        EventMention.Builder eventMentionBuilder = EventMention.builder(Symbol.from("event-VISIT"));
        List<EventMention.Argument> eventArguments = new ArrayList<EventMention.Argument>();
        eventMentionBuilder.setAnchorNode(anchorNodeOptional.get());
        EventMention.Argument eventArg1 = EventMention.MentionArgument.from(
            Symbol.from("visit-arg1"), mentionArg1Optional.get(), 1.0f);
        eventArguments.add(eventArg1);
        EventMention.Argument eventArg2 = EventMention.MentionArgument.from(
            Symbol.from("visit-arg2"), mentionArg2Optional.get(), 1.0f);
        eventArguments.add(eventArg2);
        eventMentionBuilder.setArguments(eventArguments);
        eventMentions.add(eventMentionBuilder.build());
        EventMentions eventMentionTheory = EventMentions.create(eventMentions);
        sentenceTheoryBuilder.withEventMentions(eventMentionTheory);
      }

      // test mentions
      testMentions.add(mentionArg1Optional.get());
      testMentionConfidences.put(mentionArg1Optional.get(), MentionConfidence.AnyName);
      //

      // relation
      List<RelationMention> relationMentions = new ArrayList<RelationMention>();
      RelationMention relationMention = new RelationMention(
          Symbol.from("relation-VISIT"), Symbol.from("relation-VISIT-raw"),
          mentionArg1Optional.get(), mentionArg2Optional.get(), valueMentionOptional.get(),
          Symbol.from("TIME-role-past"), Tense.PAST, Modality.ASSERTED, 1.0, null);
      relationMentions.add(relationMention);
      RelationMentions relationMentionTheory = new RelationMentions(relationMentions);
      sentenceTheoryBuilder.withRelationMentions(relationMentionTheory);

      // prop
      List<Proposition> propositions = new ArrayList<Proposition>();

      List<Proposition.ArgumentBuilder> argBuilders = new ArrayList<Proposition.ArgumentBuilder>();
      Proposition.ArgumentBuilder propArg1Builder = new Proposition.MentionArgumentBuilder(
          Proposition.Argument.SUB_ROLE, mentionArg1Optional.get());
      Proposition.ArgumentBuilder propArg2Builder = new Proposition.MentionArgumentBuilder(
          Proposition.Argument.OBJ_ROLE, mentionArg2Optional.get());
      argBuilders.add(propArg1Builder);
      argBuilders.add(propArg2Builder);

      List<Proposition.Status> statuses = new ArrayList<Proposition.Status>();
      statuses.add(Proposition.Status.DEFAULT);

      Proposition proposition = new Proposition(
          Proposition.PredicateType.VERB,
          anchorNodeOptional.get(), null, null, null, null,
          argBuilders, statuses);

      Propositions propTheory = new Propositions(propositions);
      sentenceTheoryBuilder.withPropositionSet(propTheory);
      */

      // doc-entities

      // sentenceTheoryBuilder.withPOSSequence();

      SentenceTheory sentenceTheory = sentenceTheoryBuilder.build();

      docTheoryBuilder.addSentenceTheory(sentenceTheory);
    }

    /*
    List<Entity> entities = new ArrayList<Entity>();
    Entity.create(testMentions, EntityType.createWithName(Symbol.from("PER")),
        EntitySubtype.UNDET, 1234, false, testMentionConfidences, null);
    Entities entitySet = Entities.create(entities, "1.0");
    docTheoryBuilder.setEntitySet(entitySet);
    */

    DocTheory docTheory = docTheoryBuilder.build();
    try {
      serifxmlWriter.saveTo(docTheory, new File("./testSerifXml.xml"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
