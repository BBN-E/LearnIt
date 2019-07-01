package com.bbn.akbc.neolearnit.relations.Features;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.BeforeAfterSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BetweenSlotsPattern;
import com.bbn.akbc.neolearnit.observations.pattern.BrandyablePattern;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.PropPattern;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.SlotContainsWordRestriction;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.Pattern;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmin on 8/4/15.
 */
public class LearnItBrandyPatternCounter {
//static Long max_num_seed_to_read = Long.MAX_VALUE;
  static Long max_num_seed_to_read = 1000000l;
  static int gid=0;

  static Multiset<Pair<String, Pattern>> counterPatterns = null;

  // Example: patternSource = "ds";
  public static void updateCounterByInstance(String slot,
      InstanceIdentifier instanceIdentifier,
      Mappings mapping,
      Target target,
      String strArg1Head, String strArg2Head) {

    if(counterPatterns==null)
      counterPatterns = HashMultiset.<Pair<String, Pattern>>create();

    // String strArg1Type = instanceIdentifier.getSlotEntityType(0);
    // String strArg2Type = instanceIdentifier.getSlotEntityType(1);

    // SlotEntityTypeRestriction arg1SlotEntityTypeRestriction = new SlotEntityTypeRestriction(0, strArg1Type);
    // SlotEntityTypeRestriction arg2SlotEntityTypeRestriction = new SlotEntityTypeRestriction(1, strArg2Type);

    SlotContainsWordRestriction arg1SlotContainsWordRestriction = new SlotContainsWordRestriction(0, strArg1Head, "english");
    SlotContainsWordRestriction arg2SlotContainsWordRestriction = new SlotContainsWordRestriction(1, strArg2Head, "english");

    List<BeforeAfterSlotsPattern> beforePatterns = new ArrayList<BeforeAfterSlotsPattern>();
    List<BeforeAfterSlotsPattern> afterPatterns = new ArrayList<BeforeAfterSlotsPattern>();
    for(LearnitPattern learnitPattern : mapping.getPatternsForInstance(instanceIdentifier)) {
      if(learnitPattern instanceof BeforeAfterSlotsPattern) {
        BeforeAfterSlotsPattern beforeAfterSlotsPattern = (BeforeAfterSlotsPattern) learnitPattern;
        for(BeforeAfterSlotsPattern variant : beforeAfterSlotsPattern.getAllVersions()) {
          System.out.println("BeforeAfterSlotsPattern:\t" + variant.isBeforeText() + "\t" + variant.toIDString());
          if (variant.isBeforeText())
            beforePatterns.add(variant);
          if (variant.isAfterText())
            afterPatterns.add(variant);
        }
      }
    }

    for(LearnitPattern learnitPattern : mapping.getPatternsForInstance(instanceIdentifier)) {
      if(learnitPattern instanceof BetweenSlotsPattern) {
        BetweenSlotsPattern betweenSlotsPattern = (BetweenSlotsPattern) learnitPattern;
        if(isLexicalContentful(betweenSlotsPattern))
          updateCounterPatterns(betweenSlotsPattern, slot, target);
        else {
          ComboPattern comboPatternWithArg1Headword = new ComboPattern(betweenSlotsPattern, arg1SlotContainsWordRestriction);
          ComboPattern comboPatternWithArg2Headword = new ComboPattern(betweenSlotsPattern, arg2SlotContainsWordRestriction);

          ComboPattern comboPatternWithBothHeadwords = new ComboPattern(comboPatternWithArg1Headword, arg2SlotContainsWordRestriction);

          updateCounterPatterns(comboPatternWithArg1Headword, slot, target);
          updateCounterPatterns(comboPatternWithArg2Headword, slot, target);
          updateCounterPatterns(comboPatternWithBothHeadwords, slot, target);

          // with before and after text patterns
          for(BeforeAfterSlotsPattern beforePattern : beforePatterns) {
            ComboPattern comboPatternWithBefore = new ComboPattern(betweenSlotsPattern, beforePattern);

            updateCounterPatterns(comboPatternWithBefore, slot, target);

            for(BeforeAfterSlotsPattern afterPattern : afterPatterns) {
              ComboPattern comboPatternWithAfter = new ComboPattern(betweenSlotsPattern, afterPattern);
              ComboPattern comboPatternWithBoth = new ComboPattern(comboPatternWithBefore, afterPattern);

              updateCounterPatterns(comboPatternWithAfter, slot, target);
              updateCounterPatterns(comboPatternWithBoth, slot, target);
            }
          }
        }
      }
      if(learnitPattern instanceof PropPattern) {
        PropPattern propPattern = (PropPattern) learnitPattern;
        if(isLexicalContentful(propPattern)) {
          updateCounterPatterns(propPattern, slot, target);
        }
        else {
          ComboPattern comboPatternWithArg1Headword =
              new ComboPattern(propPattern, arg1SlotContainsWordRestriction);
          ComboPattern comboPatternWithArg2Headword =
              new ComboPattern(propPattern, arg2SlotContainsWordRestriction);

          ComboPattern comboPatternWithBothHeadwords =
              new ComboPattern(comboPatternWithArg1Headword, arg2SlotContainsWordRestriction);

          updateCounterPatterns(comboPatternWithArg1Headword, slot, target);
          updateCounterPatterns(comboPatternWithArg2Headword, slot, target);
          updateCounterPatterns(comboPatternWithBothHeadwords, slot, target);
        }
      }
    }
  }

  public static void main(String [] argv) throws IOException {
    String learnitParamFile = argv[0];
    max_num_seed_to_read = Long.parseLong(argv[1].trim());

    LearnItConfig.loadParams(new File(learnitParamFile));

    String slot = "per_employee_or_member_of";
    String targetFile =
        "/nfs/mercury-04/u42/bmin/everything/projects/neolearnit/inputs/targets.re_decode/kbp_" + slot
            + ".target.xml";
    Target target = TargetFactory.fromTargetXMLFile(targetFile).get(0);

    String strMappingsFileForTest = "/nfs/mercury-04/u42/bmin/everything/projects/ere/data_learnit/source_mappings/everything/list000.sjson";
    Mappings mapping = Mappings.deserialize(new File(strMappingsFileForTest), true);

    int nSeedsRead=0;
    for(Seed seed : mapping.getAllSeeds().elementSet()) {
      if(++nSeedsRead>max_num_seed_to_read)
        break;

      String strArg1Head = getHeadWord(seed.getSlotHeadText(0).toString());
      String strArg2Head = getHeadWord(seed.getSlotHeadText(1).toString());

      for (InstanceIdentifier instanceIdentifier : mapping.getInstancesForSeed(seed)) {
        LearnItBrandyPatternCounter.updateCounterByInstance(slot,
            instanceIdentifier,
            mapping,
            target,
            strArg1Head, strArg2Head);
      }
    }

    // print out
    PrintWriter pw = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream("/nfs/mercury-04/u42/bmin/2del.txt", false)));
    LearnItBrandyPatternCounter.writeCountsOfPatterns(pw, "cs2014", slot);
    pw.close();
  }

  public static void writeCountsOfPatterns(PrintWriter pw,
      String patternSource, String slot) {
    if(LearnItBrandyPatternCounter.counterPatterns == null)
      return;

//    for(Pair<String, Pattern> patternPair : LearnItBrandyPatternCounter.counterPatterns.elementSet()) {

    for(Pair<String, Pattern> patternPair : Multisets.copyHighestCountFirst(LearnItBrandyPatternCounter.counterPatterns).elementSet()) {
      int count = LearnItBrandyPatternCounter.counterPatterns.count(patternPair);
      String patternIdString = patternPair.getFirst();

      Pattern pattern = patternPair.getSecond();
      pattern = addIdAndGroupForPattern(pattern, slot, patternSource, ++gid);

      pw.println("<!-- " + count + "\t" + patternIdString + "-->");
      pw.println(pattern.toString());
      pw.println();
    }
  }

  private static String getHeadWord(String text) {
    text = text.trim().replace("\r", " ").replace("\n", " ").replace("\t", " ");

    if(!text.contains(" "))
      return text;
    else {
      String[] items = text.split(" ");
      return items[items.length-1];
    }
  }

  private static void updateCounterPatterns(LearnitPattern learnitPattern, String slot, Target target) {
    counterPatterns.add(
        new Pair<String, Pattern>(learnitPattern.toIDString(),
            getBrandyPattern(learnitPattern, slot, target)));
  }

  private static Pattern getBrandyPattern(LearnitPattern learnitPattern,
      String slot, Target target) {

    if(learnitPattern instanceof BrandyablePattern) {
      BrandyablePattern brandyablePattern = (BrandyablePattern) learnitPattern;
      Pattern pattern = brandyablePattern.convertToBrandy(slot, target, new ArrayList<Restriction>());

      return pattern;
    }
    else {
      System.out.print("NOT brandyable pattern: " + learnitPattern.toIDString());
      System.exit(-1);
    }

    return null;
  }

  private static Pattern addIdAndGroupForPattern(Pattern pattern, String slot,
      String patternSource, int ID) {
    Pattern.Builder builder = pattern.modifiedCopyBuilder();
    builder.withScore(0.9f);
    builder.withScoreGroup(1);
    builder.withId(Symbol.from(slot + "_" + patternSource + "_" + ID));
    return builder.build();
  }

  private static boolean isLexicalContentful(LearnitPattern p) {
    return !p.getLexicalItemsWithContent().isEmpty();
  }
}
