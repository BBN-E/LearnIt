package com.bbn.akbc.neolearnit.relations.utility;

import com.bbn.akbc.common.Pair;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.resources.StopWords;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.utility.FileUtil;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by bmin on 11/21/16.
 */
public class Utility {
  public static String getOptionalStr(Optional<String> str) {
    return str.isPresent() ? str.get() : "N/A";
  }

  public static int getOptionalInt(Optional<Integer> integerOptional) {
    return integerOptional.isPresent() ? integerOptional.get()
                                       : 0; // if not exisit, take 0; TODO: FIX
  }

  // simple heuristic to take the last token
  public static String getSingleTokenHeadText(String headText) {
    String[] items = headText.trim().split(" ");
    return items[items.length - 1];
  }

  public static String sanitize(String input) {
//		return input.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;").replace("\n", "<br />").replace("\t", "&nbsp;&nbsp;&nbsp;")
//				.replace("@-@", "-").replace("-LRB-","&#40;").replace("-RRB-","&#41;").replace("numbercommontoken", "&#35;");
    return input;
  }

  public static Optional<String> getTokenStringByIdx(SentenceTheory sentTheory, int idx) {
    if (idx < 0 || idx >= sentTheory.tokenSequence().size()) {
      return Optional.absent();
    } else {
      return Optional.of(sentTheory.tokenSequence().token(idx).tokenizedText().utf16CodeUnits().toLowerCase());
    }
  }

  public static String list2string(List<String> listStr, String delimiter) {
    return list2string(listStr, delimiter, "");
  }

  public static String list2string(List<String> listStr, String delimiter, String prefix) {
    StringBuilder sb = new StringBuilder();
    for(String str : listStr) {
      sb.append(prefix + str + delimiter);
    }

    String ret = sb.toString();
    if(!ret.isEmpty())
      ret = ret.substring(0, ret.length()-delimiter.length());

    return ret;
  }

  public static String list2string(Set<String> listStr, String delimiter) {
    return list2string(listStr, delimiter, "");
  }

  public static String list2string(Set<String> listStr, String delimiter, String prefix) {
    List<String> setStr = new ArrayList<String>();
    setStr.addAll(listStr);
    return list2string(setStr, delimiter, prefix);
  }

  public static Pair<String, String> getEntityPairString(Seed seed) {
    List<String> args = seed.getStringSlots();
    if(args.size()!=2) {
//			System.err.println("err reading arguments: " + args.toString());
    }

    String arg1 = args.get(0).trim().toLowerCase();
    String arg2 = args.get(1).trim().toLowerCase();
//		System.out.println("2\tSeed: " + "<" + arg1 + ", " + arg2 + ">");

    Pair<String, String>
        pair = new Pair<String, String>(arg1, arg2);

    return pair;
  }

  public static Optional<Pair<String, String>> getLikelyEntityTypesForSeed(Seed seed, Mappings mappings) {
    int max_inst_to_try = 5;

    List<String> arg1entTypes = new ArrayList<String>();
    List<String> arg2entTypes = new ArrayList<String>();

    int numTried=0;
    for(InstanceIdentifier instanceIdentifier : mappings.getInstancesForSeed(seed)) {
      String arg1entType = instanceIdentifier.getSlotEntityType(0).substring(0, 3).toUpperCase();
      String arg2entType = instanceIdentifier.getSlotEntityType(1).substring(0, 3).toUpperCase();

      arg1entTypes.add(arg1entType);
      arg2entTypes.add(arg2entType);

      if(++numTried>max_inst_to_try)
        break;
    }

    Collections.shuffle(arg1entTypes);
    Collections.shuffle(arg2entTypes);

    if(!arg1entTypes.isEmpty() && !arg2entTypes.isEmpty())
      return Optional.of(new Pair<String, String> (arg1entTypes.get(0), arg2entTypes.get(0)));
    else
      return Optional.absent();
  }

  public static boolean isLexicalContentful(LearnitPattern p) {
    return !p.getLexicalItemsWithContent().isEmpty();
  }

  public static boolean isStopword(String word) {
    StopWords stopwords = StopWords.getDefault();
    if (stopwords.isStopWord(Symbol.from(word.toLowerCase()))) {
      return true;
    } else {
      return false;
    }
  }

  public static String normalizeFeatureString(String feature) {
    return feature.replace("\t", " ")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace(" ", "_");
  }

  public static String getTextForSpan(DocTheory docTheory,
      int sentenceID, int tokenIdxStart, int tokenIdxEnd) {
    SentenceTheory sentenceTheory = docTheory.sentenceTheory(sentenceID);

    StringBuilder sb = new StringBuilder();
    for(int idx=tokenIdxStart; idx<=tokenIdxEnd; idx++)
      sb.append(sentenceTheory.tokenSequence().token(idx).tokenizedText() + " ");

    return sb.toString().trim();
  }

  public static boolean passSampling(double subsampling_ratio, Random randomGenerator) {
    int randomInt = randomGenerator.nextInt(1000000);
    if((double)randomInt/1000000 > subsampling_ratio)
      return false;
    return true;
  }

  public static String getAtomicHeadText(Spanning spanning, DocTheory dt) {
    String atomicHeadText = "";
    if(spanning instanceof Mention) {
      Mention m = (Mention) spanning;
      atomicHeadText = m.atomicHead().tokenSpan().tokenizedText(dt).utf16CodeUnits();
    }
    else
      atomicHeadText = spanning.tokenSpan().tokenizedText(dt).utf16CodeUnits();

    return atomicHeadText;
  }

  public static Map<String, Map<String, Double>> load_negative_sampling_ratio() throws IOException {
    Map<String, Map<String, Double>> ret = new HashMap<String, Map<String, Double>>();

    String resourceName = "/nfs/mercury-04/u42/bmin/repositories/git/local/akbc/neolearnit/src/main/resources/tac_negative_sampling_ratio.txt";
    List<String> lines = FileUtil.readLinesIntoList(resourceName);
//    ImmutableList<String> lines = Resources
//        .asCharSource(Resources.getResource(resourceName), Charsets.UTF_8).readLines();


    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      String[] items = line.split(" ");

      String corpusName = items[0];
      String slot = items[1];
      double negative_sampling_ration = Double.parseDouble(items[2]);

      if (!ret.containsKey(corpusName)) {
        ret.put(corpusName, new HashMap<String, Double>());
      }
      ret.get(corpusName).put(slot, negative_sampling_ration);
    }

    return ret;
  }

  public static String getPatternString(LearnitPattern learnitPattern) {
    return learnitPattern.toIDString();
  }
}
