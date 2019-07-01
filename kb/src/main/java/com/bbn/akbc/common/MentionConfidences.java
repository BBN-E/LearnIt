package com.bbn.akbc.common;

import com.bbn.akbc.common.comparator.RelationMentionBrandyConfidenceComparator;
import com.bbn.akbc.common.comparator.RelationMentionSourcePatternComparator;
import com.bbn.akbc.kb.text.TextMention;
import com.bbn.akbc.kb.text.TextRelationMention;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MentionConfidences {

  /*
          typedef enum {
                  UNKNOWN_CONFIDENCE_,
                  ANY_NAME_,  // any name mention
                  TITLE_DESC_, // e.g. _President_ Barack Obama
                  COPULA_DESC_, // e.g. Microsoft is _a big software company_
                  APPOS_DESC_, // e.g. Microsoft, _a big software company_
                  ONLY_ONE_CANDIDATE_DESC_, //only one candidate of this type in document before desc (possibly excluding dateline ORGs)
                  PREV_SENT_DOUBLE_SUBJECT_DESC_, // adjacent sentences such as "Obama anounced .." "_The President_ was visiting..."
                  OTHER_DESC_, // any other descriptor
                  WHQ_LINK_PRON_, // Microsoft, _which_ is a big software company
                  NAME_AND_POSS_PRON_, // Bob and _his_ dog
                  DOUBLE_SUBJECT_PERSON_PRON_, // Bob said that _he_ would go shopping (two subjects, both persons, no other name preceding the pronoun)
                  ONLY_ONE_CANDIDATE_PRON_, //only one candidate of this type in document before pronoun (possibly excluding dateline ORGs)
                  PREV_SENT_DOUBLE_SUBJECT_PRON_, // adjacent sentences such as "Obama said..." "_He_ denied .."
                  OTHER_PRON_, // any other pronoun
                  NO_ENTITY_	// not co-referenct with another entity
          } EnumType;
   */
  static ImmutableSet<String> allBrandyConfidenceStrings = (new ImmutableSet.Builder<String>())
      .add("AnyName")
      .add("ApposDesc")
      .add("CopulaDesc")
      .add("DoubleSubjectPersonPron")
      .add("NULL")
      .add("NameAndPossPron")
      .add("OnlyOneCandidateDesc")
      .add("OnlyOneCandidatePron")
      .add("OtherDesc")
      .add("OtherPron")
      .add("PrevSentDoubleSubjectDesc")
      .add("PrevSentDoubleSubjectPron")
      .add("TitleDesc")
      .add("UnknownConfidence")
      .add("WhqLinkPron")

      .add("NEST") // support nested names
      .add("AmbiguousName")
      .build();

  static int MIN_CONF = 3;
  public static ImmutableMap<String, Integer> brandyConfidenceString2confidenceLevel =
      (new ImmutableMap.Builder<String, Integer>())
          .put("AnyName", 6)
          .put("NEST", 6) // support nested names

          .put("AmbiguousName", 5) // may not be good here

          .put("CopulaDesc", 4)
          .put("TitleDesc", 4)
          .put("ApposDesc", 4)
          .put("OnlyOneCandidateDesc", 4)
          .put("PrevSentDoubleSubjectDesc", 4)

          .put("DoubleSubjectPersonPron", 3)
          .put("PrevSentDoubleSubjectPron", 3)
          .put("NameAndPossPron", 3)
          .put("OnlyOneCandidatePron", 3)
          .put("WhqLinkPron", 3)

          .put("OtherDesc", 1)
          .put("OtherPron", 1)

          .put("NULL", 0)
          .put("UnknownConfidence", 0)

          .put("name", 6) // CS2017
          .put("desc", 4) // CS2017
          .put("pron", 3) // CS2017
          .put("none", 0) // CS2017
          .put("string", 0) // CS2017
          .put("NAME", 6) // CS2017
          .put("APPO", 4) // CS2017
          .put("DESC", 4) // CS2017
          .put("PRON", 3) // CS2017
          .put("PART", 2) // CS2017
          .put("NONE", 0) // CS2017
          .put("STRING", 0) // CS2017

          .build();

  public static boolean isGoodBrandyConfidences(TextMention mention) {
    String brandyConfidence = mention.getBrandyConfidence().get();

    return isGoodBrandyConfidences(brandyConfidence, MIN_CONF);
  }

  public static boolean isGoodBrandyConfidences(String brandyConfidence) {
    return isGoodBrandyConfidences(brandyConfidence, MIN_CONF);
  }

  public static boolean isGoodBrandyConfidences(String brandyConfidence, int min_conf) {
    if (allBrandyConfidenceStrings.contains(brandyConfidence)) {
      if (brandyConfidenceString2confidenceLevel.get(brandyConfidence) >= min_conf) {
        return true;
      }
    }

    return false;
  }

  public static boolean hasPronounConfidenceOrLower(TextMention mention) {
    if (allBrandyConfidenceStrings.contains(mention.getBrandyConfidence().get())) {
      if (brandyConfidenceString2confidenceLevel.get(mention.getBrandyConfidence().get()) <= 3) {
        return true;
      }
    }

    return false;
  }

  public static boolean isValidBrandyConfidences(TextMention mention) {
    if (allBrandyConfidenceStrings.contains(mention.getBrandyConfidence().get())) {
      return true;
    } else {
      return false;
    }
  }


  public static TextRelationMention getBestRelationMentionFromListOfEquivalents(
      Set<TextRelationMention> rms) {
    List<TextRelationMention> KBRelationMentions = new ArrayList<TextRelationMention>();
    KBRelationMentions.addAll(rms);

    // step 1: rank by source pattern
    Collections.sort(KBRelationMentions, new RelationMentionSourcePatternComparator());

    List<TextRelationMention>
        listKBRelationMentionNew = new ArrayList<TextRelationMention>(KBRelationMentions);
    for (int i = 0; i < KBRelationMentions.size(); i++) {
      TextRelationMention rm = KBRelationMentions.get(i);
      if (FactConfidence.getSourcePatternConf(rm.sourcePattern.get())
          >= 0.9) // always keep the most confident ones
      {
        listKBRelationMentionNew.add(rm);
      }
    }
    if (listKBRelationMentionNew.isEmpty()) {
      listKBRelationMentionNew
          .addAll(KBRelationMentions.subList(0, 5)); // otherwise keep top-5 confident one
    }

    KBRelationMentions = listKBRelationMentionNew;

    for (TextRelationMention r : KBRelationMentions) {
      System.out.println("DBG-4\t" + r.toSimpleString());
    }
    System.out.println();

    // step 2: rank by brandy confidences
    Collections.sort(KBRelationMentions, new RelationMentionBrandyConfidenceComparator());

    if (KBRelationMentions.size() > 1) {
      System.out.println("--------------------------");
      for (int i = 1; i < KBRelationMentions.size(); i++) {
        TextRelationMention rm = KBRelationMentions.get(i);
        System.out.println("remove relation mention by brandyConfidence: " + rm.toSimpleString());
      }
    }

    for (TextRelationMention r : KBRelationMentions) {
      System.out.println("DBG-5\t" + r.toSimpleString());
    }
    System.out.println();

    return KBRelationMentions.get(0);
  }
}
