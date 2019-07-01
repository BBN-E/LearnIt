package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.AtomicMentionConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.EntityTypeConstraint;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.MinEntityLevelConstraint;
import com.bbn.akbc.neolearnit.common.targets.properties.TargetProperty;
import com.bbn.akbc.neolearnit.exec.Initialize;
import com.bbn.akbc.neolearnit.mappings.filters.BilingualOrChineseOnlyFilter;
// import SubsamplingFilter;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.initialization.InitializationPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposalInformation;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposer;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruner;
import com.bbn.akbc.neolearnit.processing.seedproposal.SeedProposer;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruner;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruningInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scorers.SeedScorer;
import com.bbn.akbc.neolearnit.scoring.scorers.SimplifiedPatternScorer;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.selectors.SeedSelector;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class InitializationHandler extends SimpleJSONHandler {

  private final Map<String, TargetAndScoreTables> extractors;
  private final Map<String, Mappings> mappingLookup;
  private final Map<String, List<Seed>> additionalSeeds;
  private Mappings mappings;
  private final String path;
  private final int iteration;

  private final boolean isPatternWritingServer;
  private String outputDir;

  private static final int NUM_PATTERNS_TO_PROPOSE = 400;
  private static final int NUM_SEEDS_TO_PROPOSE = 400;

  private final boolean isBilingualOrChineseOnly = false;

  public InitializationHandler(Mappings mappings, String outputDirOrJsonFile,
      double subsampling_ratio) {

    this.extractors = new HashMap<String, TargetAndScoreTables>();
    this.mappingLookup = new HashMap<String, Mappings>();
//		this.mappings = mappings;

    if (isBilingualOrChineseOnly) {
      this.mappings = (new BilingualOrChineseOnlyFilter()).makeFiltered(mappings);
    } else {
      this.mappings = mappings;
    }

/*
		if(isBilingualOrChineseOnly)
			this.mappings = (new SubsamplingFilter(subsampling_ratio)).makeFiltered((new BilingualOrChineseOnlyFilter()).makeFiltered(mappings));
		else
			this.mappings = mappings;
*/


/*
		if(isBilingualOrChineseOnly)
			this.mappings = (new SubsamplingFilter(subsampling_ratio)).makeFiltered((new BilingualOrChineseOnlyFilter()).makeFiltered(mappings));
		else
			this.mappings = mappings;
*/

    this.additionalSeeds = new HashMap<String, List<Seed>>();
    this.isPatternWritingServer = true;

    this.path = "/dummy/path/this/is/a/bad/fix";
    this.iteration = 0;

    try {
      if ((new File(outputDirOrJsonFile)).isFile() && outputDirOrJsonFile.endsWith(".json")) {
        this.outputDir = outputDirOrJsonFile.substring(0, outputDirOrJsonFile.lastIndexOf("/"));

        File f = new File(outputDirOrJsonFile);
        if (f.toString().endsWith(".json")) {
          TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);

          ex = getExtractorFitToMappings(ex, this.mappings);

          extractors.put(ex.getTarget().getName(), ex);

					/*
					if(isBilingualOrChineseOnly)
//						this.mappings = (new SubsamplingFilter(ex, subsampling_ratio)).makeFiltered((new BilingualOrChineseOnlyFilter()).makeFiltered(mappings));
						this.mappings = (new BilingualOrChineseOnlyFilter()).makeFiltered(mappings);
					else
						this.mappings = mappings;
						*/

//					mappingLookup.put(ex.getTarget().getName(), (new SubsamplingFilter(ex, subsampling_ratio)).makeFiltered(new TargetFilter(ex.getTarget()).makeFiltered(mappings)));
          // mappingLookup.put(ex.getTarget().getName(), (new BilingualOrChineseOnlyFilter()).makeFiltered(new TargetFilter(ex.getTarget()).makeFiltered(mappings)));

          mappingLookup.put(ex.getTarget().getName(),
              (new TargetFilter(ex.getTarget()).makeFiltered(this.mappings)));
          // mappingLookup.put(ex.getTarget().getName(), this.mappings);

        }

      } else {
        this.outputDir = outputDirOrJsonFile;

				/*
				//
				if(isBilingualOrChineseOnly)
					this.mappings = (new BilingualOrChineseOnlyFilter()).makeFiltered(mappings);
				else
					this.mappings = mappings;
				//
				 */

        File relDir = new File(outputDir);
        for (File f : relDir.listFiles()) {
          if (f.toString().endsWith(".json")) {
            TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);
            extractors.put(ex.getTarget().getName(), ex);

						/*
						if(isBilingualOrChineseOnly)
//							mappingLookup.put(ex.getTarget().getName(), (new SubsamplingFilter(ex, subsampling_ratio)).makeFiltered((new BilingualOrChineseOnlyFilter()).makeFiltered(new TargetFilter(ex.getTarget()).makeFiltered(mappings))));
							mappingLookup.put(ex.getTarget().getName(), (new BilingualOrChineseOnlyFilter()).makeFiltered(new TargetFilter(ex.getTarget()).makeFiltered(mappings)));
						else
							mappingLookup.put(ex.getTarget().getName(), new TargetFilter(ex.getTarget()).makeFiltered(mappings));
							*/
            mappingLookup.put(ex.getTarget().getName(),
                new TargetFilter(ex.getTarget()).makeFiltered(this.mappings));
          }
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private TargetAndScoreTables getExtractorFitToMappings(TargetAndScoreTables extractor,
      Mappings mappings) {
    Set<LearnitPattern> patternsToRemove = new HashSet<LearnitPattern>();
    Set<Seed> SeedToRemove = new HashSet<Seed>();

    for (LearnitPattern pattern : extractor.getPatternScores().keySet()) {
      if (!mappings.getAllPatterns().contains(pattern)) {
        patternsToRemove.add(pattern);
      }
    }

    for (Seed seed : extractor.getSeedScores().keySet()) {
      if (!mappings.getAllSeeds().contains(seed)) {
        SeedToRemove.add(seed);
      }
    }

    for (LearnitPattern pattern : patternsToRemove) {
      extractor.getPatternScores().removeItem(pattern);
    }
    for (Seed seed : SeedToRemove) {
      extractor.getSeedScores().removeItem(seed);
    }

    return extractor;
  }

  public InitializationHandler(Mappings mappings, TargetAndScoreTables data, String path,
      int iteration) {
    this.extractors = new HashMap<String, TargetAndScoreTables>();
    this.mappingLookup = new HashMap<String, Mappings>();
    this.mappings = mappings;
    this.extractors.put(data.getTarget().getName(), data);
    this.mappingLookup.put(data.getTarget().getName(), mappings);
    this.path = path;
    this.iteration = iteration;
    this.additionalSeeds = new HashMap<String, List<Seed>>();

    this.isPatternWritingServer = false;

    // let's load the additional seeds
    String additionalSeedsPath =
        LearnItConfig.get("learnit_root") + "/inputs/seeds/" + data.getExperimentName()
            + ".additional/";
    String addFilename = data.getTarget().getName() + ".seeds.xml";

    // hate to do this specific case, but everyone insisted on different KBP naming
    if (data.getExperimentName().equals("kbp-tac")) {
      addFilename = "kbp_" + addFilename;
    }

    File additionalSeedsFile = new File(additionalSeedsPath, addFilename);
    if (additionalSeedsFile.exists()) {
      additionalSeeds.put(data.getTarget().getName(), Initialize.loadSeedsXML(additionalSeedsFile));
    } else {
      additionalSeeds.put(data.getTarget().getName(), new ArrayList<Seed>());
    }
    // decrement the iteration
    data.setIteration(iteration);
    data.getSeedScores().orderItems();
    data.getPatternScores().orderItems();

    double totalConf = 0.0;
    for (InstanceIdentifier id : mappings.getInstance2Seed().getAllInstances()) {
      totalConf += id.getConfidence();
    }
    data.setTotalInstanceDenominator(totalConf);

    try {
      SeedSimilarity.runKMeans(data);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getRelation() {
    return extractors.keySet().iterator().next();
  }

	/*public InitializationHandler loadData() throws JsonParseException, JsonMappingException, IOException {
		File relDir = new File(relationPath);
		for (File f : relDir.listFiles()) {
			if (f.toString().endsWith(".json")) {
				TargetAndScoreTables ex = TargetAndScoreTables.deserialize(f);
				extractors.put(ex.getTarget().getName(), ex);
				mappingLookup.put(ex.getTarget().getName(), new TargetFilter(ex.getTarget()).makeFiltered(mappings));
			}
		}
		return this;
	}*/

  @JettyMethod("/init/get_targets")
  public synchronized List<Target> getTargets() {
    List<Target> result = new ArrayList<Target>();
    for (String key : extractors.keySet()) {
      result.add(extractors.get(key).getTarget());
    }
    return result;
  }

  @JettyMethod("/init/add_target")
  public synchronized String addTarget(
      @JettyArg("name") String name,
      @JettyArg("description") String description,
      @JettyArg("slot0Types") String slot0Types,
      @JettyArg("slot1Types") String slot1Types,
      @JettyArg("symmetric") String symmetricStr) {

    String targetPathRel = String.format("inputs/targets/json/%s.json", name);
    String targetPathFull =
        String.format("%s/%s", LearnItConfig.get("learnit_root"), targetPathRel);

    Target newTarget;
//		if (!new File(targetPathFull).exists()) {
    ArrayList<String> slot0List = Lists.newArrayList(slot0Types.split(","));
    ArrayList<String> slot1List = Lists.newArrayList(slot1Types.split(","));

    TargetSlot slot0 = new TargetSlot.Builder(0, "mention")
        .withAddedConstraint(new AtomicMentionConstraint(0))
        .withAddedConstraint(new EntityTypeConstraint(0, slot0List))
        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 0, "DESC"))
        .build();
    TargetSlot slot1 = new TargetSlot.Builder(1, "mention")
        .withAddedConstraint(new AtomicMentionConstraint(1))
        .withAddedConstraint(new EntityTypeConstraint(1, slot1List))
        .withAddedConstraint(new MinEntityLevelConstraint("ALL", 1, "DESC"))
        .build();

    boolean symmetric = Boolean.parseBoolean(symmetricStr);

    Target.Builder newTargetBuilder = new Target.Builder(name)
        .setDescription(description)
        .withTargetSlot(slot0).withTargetSlot(slot1)
        .withAddedConstraint(new EntityTypeConstraint(0, slot0List))
        .withAddedConstraint(new EntityTypeConstraint(1, slot1List));

    if (symmetric) {
      newTargetBuilder.withAddedProperty(new TargetProperty("symmetric"));
    }

    newTarget = newTargetBuilder.build();

    try {
      newTarget.serialize(targetPathFull);

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
/*
		} else {
			try {
				newTarget = StorageUtils.deserialize(new File(targetPathFull), Target.class, false);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}
		}
		*/

    extractors.put(name, new TargetAndScoreTables(targetPathRel));
    mappingLookup.put(name, new TargetFilter(newTarget).makeFiltered(mappings));
    return "successfully build target: " + newTarget.toString();
  }

  @JettyMethod("/init/get_extractor")
  public synchronized TargetAndScoreTables getExtractor(@JettyArg("relation") String relation) {
    return checkGetExtractor(relation);
  }

  @JettyMethod("/init/clear_unknown")
  public synchronized String clearUnknown(@JettyArg("target") String relation) throws IOException {
    TargetAndScoreTables ext = checkGetExtractor(relation);
    ext.getPatternScores().removeNonFrozen();
    ext.getSeedScores().removeNonFrozen();
    return "success";
  }

  @JettyMethod("/init/clear_all")
  public synchronized String clearAll(@JettyArg("target") String relation) throws IOException {
    TargetAndScoreTables ext = checkGetExtractor(relation);
    ext.getPatternScores().clear();
    ext.getSeedScores().clear();
    return "success";
  }

  @JettyMethod("/init/shutdown")
  public synchronized String shutdown() throws IOException {
    for (String ex : extractors.keySet()) {
      TargetAndScoreTables ext = extractors.get(ex);
      clearUnknown(ex);
      ext.incrementIteration(); // set the iteration back up

      if (!this.isPatternWritingServer)
//				ext.serialize(new File(path));
      {
        ext.serialize(new File(this.outputDir + "/" + ext.getTarget().getName()
            + ".json.new"));
      } else {
        String strDirJson = this.outputDir;
        String strDirXml = this.outputDir;

        String strPathJson = strDirJson + "/" + ext.getTarget().getName() + ".json";
        String strPathTargetXml =
            strDirXml + "/" + ext.getTarget().getName() + ".target.xml";

        ext.getTarget().writeToXmlFile(new File(strPathTargetXml));
        ext.serialize(new File(strPathJson));
      }
    }
    Runtime.getRuntime().exit(0);
    return "success";
  }

  private synchronized TargetAndScoreTables checkGetExtractor(String target) {
    if (!extractors.containsKey(target) || !mappingLookup.containsKey(target)) {
      throw new RuntimeException("Target " + target + " not known");
    }
    return this.extractors.get(target);
  }

	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       INSTANCE ROUTINES                          *
	 *                                                                  *
	 *------------------------------------------------------------------*/

  public List<String> getInstanceContexts(Collection<InstanceIdentifier> insts, Target target,
      int amount) throws IOException {
    List<String> result = new ArrayList<String>();
    List<InstanceIdentifier> instList = new ArrayList<InstanceIdentifier>(insts);
    Collections.shuffle(instList, new Random());
    Iterator<InstanceIdentifier> instsIter = instList.iterator();
    for (int i = 0; i < amount && i < insts.size(); i++) {
      result.add(instsIter.next().reconstructMatchInfoDisplay(target).html());
    }
    return result;
  }

  @JettyMethod("/init/get_pattern_instances")
  public synchronized List<String> getPatternInstances(@JettyArg("target") String target,
      @JettyArg("pattern") String patternStr, @JettyArg("amount") String amountStr)
      throws IOException {

    int amount = Integer.parseInt(amountStr);
    TargetAndScoreTables ext = checkGetExtractor(target);
    Mappings info = mappingLookup.get(ext.getTarget().getName());
    LearnitPattern pattern = findPattern(info, patternStr);
    if (pattern == null) {
      info = info.getAllPatternUpdatedMappings(ext);
      pattern = findPattern(info, patternStr);
    }

    if (pattern != null) {
      Collection<InstanceIdentifier> insts = info.getInstancesForPattern(pattern);
      return getInstanceContexts(insts, ext.getTarget(), amount);
    } else {
      return new ArrayList<String>();
    }
  }

  @JettyMethod("/init/get_seed_instances")
  public synchronized List<String> getSeedInstances(@JettyArg("target") String target,
      @JettyArg("seed") String seedStr, @JettyArg("amount") String amountStr) throws IOException {

    int amount = Integer.parseInt(amountStr);
    TargetAndScoreTables ext = checkGetExtractor(target);
    Mappings info = mappingLookup.get(ext.getTarget().getName());
    Seed seed = getSeed(seedStr, target);
    if (seed != null) {
      Collection<InstanceIdentifier> insts = info.getInstancesForSeed(seed);
      return getInstanceContexts(insts, ext.getTarget(), amount);
    }
    return new ArrayList<String>();
  }


	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       PATTERN ROUTINES                           *
	 *                                                                  *
	 *------------------------------------------------------------------*/

  boolean isBilingualOrChineseMonolingualString(String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DFF) ||
          (c >= 0x20000 && c <= 0x2A6DF) || (c >= 0xF900 && c <= 0xFAFF) ||
          (c >= 0x2F800 && c <= 0x2FA1F)) {
        return true;
      }
    }

    return false;
  }

  boolean isBilingualOrChineseMonolingual(LearnitPattern pattern) {
    return isBilingualOrChineseMonolingualString(pattern.toIDString());
  }

  boolean isBilingualOrChineseMonolingual(Seed seed) {
    return isBilingualOrChineseMonolingualString(seed.getSlot(0).toString()) ||
        isBilingualOrChineseMonolingualString(seed.getSlot(1).toString());
  }

  /**
   * PATTERN SEARCH
   * ---------------------------------------
   * Searches for patterns by the keywords in their lexical content
   */
  @JettyMethod("/init/get_patterns_by_keyword")
  public synchronized List<LearnitPattern> getPatternsByKeyword(
      @JettyArg("target") final String target, @JettyArg("keyword") String keyword) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    Set<LearnitPattern> results = new HashSet<LearnitPattern>();

    // search by slot0 or slot1 strings, keywords must be in the form #keyword#, e.g., #Bill Clinton#
    if (keyword.startsWith("#") && keyword.endsWith("#")) {
      keyword = keyword.substring(1, keyword.length() - 1).toLowerCase();
			/*
			Iterator<InstanceIdentifier> it = mappingLookup.get(target).getInstance2Pattern().getAllInstances().iterator();
			while(it.hasNext()) {
				InstanceIdentifier instId = it.next();
				instId.
			}
			*/

      for (Seed seed : mappingLookup.get(target).getInstance2Seed().getAllSeeds().elementSet()) {
        String slot0text = seed.getSlot(0).toString().toLowerCase();
        String slot1text = seed.getSlot(1).toString().toLowerCase();

        if (slot0text.contains(keyword) || slot1text.contains(keyword)) {
          for (LearnitPattern pattern : mappingLookup.get(target).getPatternsForSeed(seed)) {
            if (!extractor.getPatternScores().isKnownFrozen(pattern) && pattern
                .isCompletePattern()) {
              results.add(pattern);
            }
          }
        }
      }
    } else {
      // search by keyword on the pattern
      String kwd = keyword;
      for (LearnitPattern pattern : mappingLookup.get(target).getInstance2Pattern().getAllPatterns()
          .elementSet()) {
        if (hasKeyword(SymbolUtils.toStringSet(pattern.getLexicalItems()), ImmutableSet.of(kwd)) &&
            !extractor.getPatternScores().isKnownFrozen(pattern) && pattern.isCompletePattern()) {
          results.add(pattern);
        }
      }
    }

    List<LearnitPattern> resultList = new ArrayList<LearnitPattern>(results);

    //sort by frequency
    Collections.sort(resultList, new Comparator<LearnitPattern>() {

      @Override
      public int compare(LearnitPattern arg0, LearnitPattern arg1) {
        return mappingLookup.get(target).getInstance2Pattern().getInstances(arg1).size() -
            mappingLookup.get(target).getInstance2Pattern().getInstances(arg0).size();
      }

    });

    return resultList;
  }

  private LearnitPattern findPattern(TargetAndScoreTables data, String target, String pattern) {
    LearnitPattern p1 = findPattern(mappingLookup.get(target), pattern);
    if (p1 == null) {
//			System.out.println("===================================");
      return findPattern(mappingLookup.get(target).getAllPatternUpdatedMappings(data), pattern);
    } else {
      return p1;
    }
  }

  private LearnitPattern findPattern(Mappings info, String pattern) {
    try {
      PrintStream out = new PrintStream(System.out, true, "UTF-8");

      for (LearnitPattern p : info.getAllPatterns().elementSet()) {
//				out.println("== pattern: " + p.toIDString());

        if (p.toIDString().equals(pattern)) {
          return p;
        }
      }

    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return null;
  }

  /**
   * MANUAL ACCEPT PATTERNS
   * ------------------------------------
   * Adds the given set of patterns to the extractor as known good patterns.
   */
  @JettyMethod("/init/add_pattern")
  public synchronized String addPattern(@JettyArg("target") String target,
      @JettyArg("pattern") String patStr, @JettyArg("quality") String quality) {

    TargetAndScoreTables ext = checkGetExtractor(target);
    Mappings info = mappingLookup.get(target);
    System.out.println(
        "=====info: info.getInstanceCount()=" + info.getInstanceCount() + ", info.getAllPatterns()="
            + info.getAllPatterns().size());
    LearnitPattern pattern = findPattern(ext, target, patStr);
    if (pattern != null && !ext.getPatternScores().isKnownFrozen(pattern)) {
      ext.getPatternScores().addDefault(pattern);
      double instanceWeight = 0.0;
      for (InstanceIdentifier id : info.getInstancesForPattern(pattern)) {
        instanceWeight += id.getConfidence();
      }

      PatternScore score = ext.getPatternScores().getScore(pattern);
      int frequency = info.getInstancesForPattern(pattern).size();

      score.setFrequency(frequency);
      score.setConfidenceDenominator(instanceWeight);
      score.setRecall(0.01);

      if (quality.equals("good")) {
        score.setPrecision(0.95);
        score.setConfidence(1.0);
        score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
        score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));

        score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());
        if (info.getKnownInstanceCount(ext) == 0) {
          ext.setGoodSeedPrior(score.getConfidenceNumerator() * 10,
              ext.getTotalInstanceDenominator());
        }

      } else if (quality.equals("bad")) {
        score.setPrecision(0.05);
        score.setConfidence(1.0);
        score.setConfidenceNumerator(score.getConfidenceDenominator() * score.getConfidence());
        score.setKnownFrequency((int) Math.round(score.getPrecision() * score.getFrequency()));

        score.calculateTPFNStats(ext.getGoodSeedPrior(), ext.getTotalInstanceDenominator());

      } else {
        throw new RuntimeException("Unknown quality " + quality);
      }

      System.out.println("tp :" + score.getTP());
      System.out.println("fp :" + score.getFP());
      System.out.println("tn :" + score.getTN());
      System.out.println("fn :" + score.getFN());
      score.freezeScore(ext.getIteration());
      ext.getPatternScores().orderItems();
    } else {
      throw new RuntimeException("Could not add pattern " + patStr);
    }

    return "Added pattern";
  }

  /**
   * UNACCEPT A PATTERN
   * -----------------------------------
   * Unsets the specified pattern as known and good in the system
   */
  @JettyMethod("/init/remove_pattern")
  public synchronized String removePattern(@JettyArg("target") String target,
      @JettyArg("pattern") String patternStr)
      throws JsonParseException, JsonMappingException, IOException {
    TargetAndScoreTables ext = checkGetExtractor(target);
    LearnitPattern pattern = findPattern(ext, target, patternStr);
    if (pattern != null && ext.getPatternScores().isKnownFrozen(pattern)) {
      PatternScore score = ext.getPatternScores().getScore(pattern);
      score.unfreeze();
      score.setConfidence(0.0);
      score.setPrecision(0.0);
      ext.getPatternScores().orderItems();
      return "success";
    } else {
      return "failure";
    }
  }

  /**
   * PROPOSE PATTERNS
   * ------------------------------------------
   * Proposes a new set of patterns
   */
  @JettyMethod("/init/propose_patterns")
  public synchronized String addPatterns(@JettyArg("target") String target) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    addPatterns(extractor, mappingLookup.get(extractor.getTarget().getName()));
    return "success";
  }

  public void addPatterns(TargetAndScoreTables extractor, Mappings info) {
    PatternProposer proposer = new PatternProposer(extractor, false);
    proposer.setAmount(NUM_PATTERNS_TO_PROPOSE);
    proposer.runOnMappings(info);
    PatternPruner pruner = new PatternPruner(extractor);
    pruner.score(pruner.processMappings(info));
    extractor.getPatternScores().removeBadForHuman(true);
    extractor.getPatternScores().orderItems();
  }

  public void proposeFirstPatterns(String relation) {
    TargetAndScoreTables ex = checkGetExtractor(relation);
    Mappings info = mappingLookup.get(relation);
    addPatterns(ex, info);
  }

  private boolean hasKeyword(Iterable<String> words, Collection<String> keywords) {
    for (String w : words) {
      w = w.toLowerCase();
      for (String key : keywords) {
        if (key.endsWith("*")) {
          if (w.startsWith(key.substring(0, key.length() - 1))) {
            return true;
          }
        } else {
          if (w.equals(key)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean matchesInitializationPatternInLearnItFormat(LearnitPattern p,
      Collection<LearnitPattern> initPatterns) {
    for (LearnitPattern ip : initPatterns) {
      if (ip.matchesPattern(p)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesInitializationPattern(LearnitPattern p,
      Collection<InitializationPattern> initPatterns) {
    for (InitializationPattern ip : initPatterns) {
      if (ip.matchesPattern(p)) {
        return true;
      }
    }
    return false;
  }

  private Collection<LearnitPattern> getFromInitial(Collection<InitializationPattern> initPatterns,
      Set<LearnitPattern> initPatternsInSexpFormat,
      Mappings info, Target target) {
    ImmutableSet.Builder<LearnitPattern> builder = ImmutableSet.builder();
    for (LearnitPattern p : info.getAllPatterns().elementSet()) {
      if (p.isProposable(target) && (matchesInitializationPattern(p, initPatterns)
                                         || initPatternsInSexpFormat.contains(p))) {
        builder.add(p);
      }
    }
    return builder.build();
  }

  public void proposeFromInitializationPatterns(String relation,
      Collection<InitializationPattern> initPatterns,
      Set<LearnitPattern> initPatternsInSexpFormat) {
    TargetAndScoreTables ex = checkGetExtractor(relation);
    Mappings info = mappingLookup.get(relation);
    Collection<LearnitPattern> fromInitial = getFromInitial(initPatterns, initPatternsInSexpFormat,
        info, ex.getTarget());
    addPatterns(ex, info, fromInitial);
  }

  public void addPatterns(TargetAndScoreTables extractor, Mappings info,
      Collection<LearnitPattern> initialPatterns) {
    PatternProposer proposer = new PatternProposer(extractor, false);

    PatternProposalInformation proposalInformation = proposer.processMappings(info);
    SimplifiedPatternScorer scorer = new SimplifiedPatternScorer(extractor, proposalInformation);

    Set<LearnitPattern> intersection =
        Sets.intersection(proposalInformation.getPatterns(), ImmutableSet.copyOf(initialPatterns));
    System.out.println("Proposing " + intersection.size() + " initial patterns");
    scorer.score(intersection, extractor.getPatternScores());
    for (LearnitPattern p : intersection) {
      extractor.getPatternScores().getScore(p).propose();
    }

    PatternPruner pruner = new PatternPruner(extractor);
    pruner.score(pruner.processMappings(info));
    extractor.getPatternScores().removeBadForHuman(false);
    extractor.getPatternScores().orderItems();
  }

  /**
   * RESCORE PATTERNS
   * --------------------------------------------
   * Rescores the seeds by the system seed rescoring strategy
   */
  @JettyMethod("/init/rescore_patterns")
  public synchronized String rescorePatterns(@JettyArg("target") String target) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    Mappings info = mappingLookup.get(extractor.getTarget().getName());
    PatternPruner pruner = new PatternPruner(extractor);
    pruner.score(pruner.processMappings(info));
    return "success";
  }



	/*------------------------------------------------------------------*
	 *                                                                  *
	 *                       SEED ROUTINES                              *
	 *                                                                  *
	 *------------------------------------------------------------------*/

  private Seed getSeed(String seedJson, String target) throws IOException {
    TargetAndScoreTables ext = checkGetExtractor(target);
    Mappings m = mappingLookup.get(ext.getTarget().getName());

    ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
    Seed seed = mapper.readValue(seedJson, Seed.class).withProperText(ext.getTarget());

    for (Seed s : m.getAllSeeds().elementSet()) {
      if (seed.equals(s.withProperText(ext.getTarget()))) {
        return s;
      }
    }
    return null;
  }

  private void freezeSeed(TargetAndScoreTables ext, Seed seed, String quality) {
    if (quality.equals("good")) {
      freezeSeed(ext, seed, 1.0, 0.9);
    } else if (quality.equals("bad")) {
      freezeSeed(ext, seed, 1.0, 0.1);
    } else {
      throw new RuntimeException("Unknown seed quality " + quality);
    }
  }

  private void freezeSeed(TargetAndScoreTables ext, Seed seed, double confidence, double score) {
    if (ext.getSeedScores().isKnownFrozen(seed)) {
      return;
    }

    if (!ext.getSeedScores().hasScore(seed)) {
      ext.getSeedScores().addDefault(seed);
      ext.getSeedScores().orderItems();
    }

    SeedScore sscore = ext.getSeedScores().getScore(seed);
    sscore.setScore(0.9);
    sscore.setConfidence(confidence);
    sscore.freezeScore(ext.getIteration());
  }

  /**
   * AUTO-ACCEPT SEEDS
   * ------------------------------
   * Perform seed selection to get a new set of good seeds by the system's scoring
   *
   * @param amount the number to get
   * @param seeds  the seeds to ban and not accept (because a human said so)
   */
  @JettyMethod("/init/accept_seeds")
  public synchronized String acceptSeeds(@JettyArg("target") String target,
      @JettyArg("amount") String amount,
      @JettyArg("bannedSeeds") String[] seeds) throws IOException {

    Integer intAmount = Integer.parseInt(amount);
    TargetAndScoreTables ext = checkGetExtractor(target);
    SeedSelector selector = new SeedSelector(intAmount, true);
    selector.freezeScores(ext.getSeedScores());
    for (String seedStr : seeds) {
      if (seedStr.equals("null")) {
        break;
      }

      Seed seed = getSeed(seedStr, target);
      if (ext.getSeedScores().isKnownFrozen(seed)) {
        SeedScore score = ext.getSeedScores().getScore(seed);
        score.unfreeze();
        score.setConfidence(0.0);
        score.setScore(0.0);
      }
    }
    ext.getSeedScores().orderItems();
    SeedSimilarity.updateKMeans(ext);
    return "success";
  }

  /**
   * MANUAL ACCEPT SEEDS
   * ------------------------------------
   * Adds the given set of seeds to the extractor as known good seeds.
   */
  @JettyMethod("/init/add_seeds")
  public synchronized String addSeeds(@JettyArg("target") String target,
      @JettyArg("seeds") String[] seeds, @JettyArg("quality") String quality) throws IOException {
    int count = 0;
    TargetAndScoreTables ext = checkGetExtractor(target);
    for (String seedStr : seeds) {
      Seed seed = getSeed(seedStr, target);
      if (seed != null) {
        freezeSeed(ext, seed, quality);
        count++;
      }
    }
    ext.getSeedScores().orderItems();
    return "Added " + count + " seeds";
  }

  /**
   * UNACCEPT A SEED
   * -----------------------------------
   * Unsets the specified seed as known and good in the system
   */
  @JettyMethod("/init/remove_seed")
  public synchronized String removeSeed(@JettyArg("target") String target,
      @JettyArg("seed") String seedStr) throws IOException {
    TargetAndScoreTables ext = checkGetExtractor(target);
    Seed seed = getSeed(seedStr, target);
    if (seed != null && ext.getSeedScores().isKnownFrozen(seed)) {
      SeedScore score = ext.getSeedScores().getScore(seed);
      score.unfreeze();
      score.setConfidence(0.0);
      score.setScore(0.0);
      ext.getSeedScores().orderItems();
    }
    return "success";
  }

  /**
   * PROPOSE SEEDS
   * ------------------------------------------
   * Proposes a new set of seeds
   */
  @JettyMethod("/init/propose_seeds")
  public synchronized String addSeeds(@JettyArg("target") String target) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    addSeeds(extractor, mappingLookup.get(extractor.getTarget().getName()));
    return "success";
  }

  public void addSeeds(TargetAndScoreTables extractor, Mappings info) {
    SeedProposer proposer = new SeedProposer(extractor);
    proposer.setAmount(NUM_SEEDS_TO_PROPOSE);
    proposer.runOnMappings(info);
    SeedPruner pruner = new SeedPruner(extractor);
    SeedPruningInformation prunerInfo = pruner.processMappings(info);
    SeedScorer scorer = new SeedScorer(extractor, prunerInfo);
    scorer.score(prunerInfo.getSeeds(), extractor.getSeedScores());
    extractor.getSeedScores().removeBadForHuman(false);
  }

  /**
   * RESCORE SEEDS
   * -----------------------------------------------
   * Rescores the seeds by the system seed rescoring strategy
   */
  @JettyMethod("/init/rescore_seeds")
  public synchronized String rescoreSeeds(@JettyArg("target") String target) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    Mappings info = mappingLookup.get(extractor.getTarget().getName());
    SeedPruner pruner = new SeedPruner(extractor);
    pruner.score(pruner.processMappings(info));
    return "success";
  }

  /**
   * LOAD ADDITIONAL SEEDS
   * ----------------------------------------------
   * Grabs some additional seeds from seed files in a .additional directory
   */
  @JettyMethod("/init/get_additional_seeds")
  public synchronized String getAdditionalSeeds(@JettyArg("target") String target,
      @JettyArg("amount") String strAmount) {
    TargetAndScoreTables extractor = checkGetExtractor(target);
    Mappings info = mappingLookup.get(extractor.getTarget().getName());

    int amount = Integer.parseInt(strAmount);
    List<Seed> seeds = this.additionalSeeds.get(target);

    int numTaken = 0;
    while (!seeds.isEmpty() && numTaken < amount) {
      Seed newSeed = seeds.remove(0);
      if (!extractor.getSeedScores().hasScore(newSeed)) {
        freezeSeed(extractor, newSeed, 0.9, 0.9);
        numTaken++;
      }
    }

    PatternProposer proposer = new PatternProposer(extractor);
    extractor.initializeSeedScores(proposer.processMappings(info));

    return "added " + numTaken + " seeds";

  }

  /**
   * SEARCH SEEDS BY SLOT
   * -------------------------------------------
   * Searches for viable seeds with the given slots.
   * An empty slot denotes a wildcard.
   */
  @JettyMethod("/init/get_seeds_by_slots")
  public synchronized List<Seed> getSeedsBySlots(@JettyArg("target") String target,
      @JettyArg("slot0") String slot0, @JettyArg("slot1") String slot1) {

    List<Seed> results = new ArrayList<Seed>();
    for (Seed seed : mappingLookup.get(target).getInstance2Seed().getAllSeeds().elementSet()) {
      if (seed.getSlot(0).toString().contains(slot0) && seed.getSlot(1).toString()
          .contains(slot1)) {
        results.add(seed);
      }
    }

    //sort by frequency
    Collections.sort(results, new Comparator<Seed>() {

      @Override
      public int compare(Seed arg0, Seed arg1) {
        return mappings.getInstance2Seed().getInstances(arg1).size() -
            mappings.getInstance2Seed().getInstances(arg0).size();
      }

    });

    return results;
  }

}
