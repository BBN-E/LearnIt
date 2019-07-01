package com.bbn.akbc.neolearnit.scoring;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.modules.DecodingExtractionModule;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.SeedPatternPair;
import com.bbn.akbc.neolearnit.processing.InitializingStats;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.processing.Stage;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposalInformation;
import com.bbn.akbc.neolearnit.processing.patternproposal.PatternProposer;
import com.bbn.akbc.neolearnit.processing.patternpruning.PatternPruner;
import com.bbn.akbc.neolearnit.processing.postpruning.PostPruner;
import com.bbn.akbc.neolearnit.processing.preprocessing.Preprocessor;
import com.bbn.akbc.neolearnit.processing.seedproposal.SeedProposer;
import com.bbn.akbc.neolearnit.processing.seedpruning.SeedPruner;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.scores.SeedScore;
import com.bbn.akbc.neolearnit.scoring.tables.AbstractScoreTable.ObjectWithScore;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.SeedScoreTable;
import com.bbn.akbc.neolearnit.scoring.tables.TripleScoreTable;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.serif.theories.DocTheory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetAndScoreTables {
	@JsonProperty
	private final String experimentName;
	@JsonProperty
	private final Date startDate;
	@JsonProperty
	private final String targetPath;
	@JsonProperty
	private final SeedScoreTable seedScores;
	@JsonProperty
	private final PatternScoreTable patternScores;
	@JsonProperty
	private final TripleScoreTable tripleScores;
	@JsonProperty
	private int iteration;
	@JsonProperty
	private String stage;
	@JsonProperty
	private double totalConfidenceDenominator; // the sum of instance confidences across all instances in the corpus (calculated at pattern proposal)
	@JsonProperty
	private double goodSeedPrior;
	@JsonProperty
	private Map<String,String> params = null;


//	public static ImmutableList<String> sortingParameterName = ImmutableList.of("byPrecisionAscend","byPrecisionDescend","byRecallAscend","byRecalldescend","byConfidenceAscend","byConfidenceDescend");

//	@JsonProperty("sortingParameterName")
//	private ImmutableList<String> sortingParameterName2 = sortingParameterName;

	private final Target target;

	@JsonCreator
	private TargetAndScoreTables(
			@JsonProperty("startDate") Date startDate,
			@JsonProperty("targetPath") String targetPath,
			@JsonProperty("seedScores") SeedScoreTable seedScores,
			@JsonProperty("patternScores") PatternScoreTable patternScores,
			@JsonProperty("tripleScores") TripleScoreTable tripleScores,
			@JsonProperty("iteration") int iteration,
			@JsonProperty("goodSeedPrior") double goodSeedPrior,
			@JsonProperty("totalConfidenceDenominator") double totalConfidenceDenominator,
			@JsonProperty("stage") String stage,
			@JsonProperty("params") Map<String,String> params) {

        this.experimentName = LearnItConfig.get("learnit_expt_suffix");
        this.startDate = startDate;
        //This is all that needs to be done to facilitate old JSON:
		this.targetPath = targetPath.endsWith(".json") ? targetPath.substring(targetPath.lastIndexOf("/")+1, targetPath.length()-5) : targetPath;
		System.out.println("targetPath: " + targetPath);
		this.seedScores = seedScores!=null?seedScores:new SeedScoreTable();
		this.patternScores = patternScores!=null?patternScores:new PatternScoreTable();
		this.tripleScores = tripleScores!=null?tripleScores:new TripleScoreTable();
		this.iteration = iteration;
		this.goodSeedPrior = goodSeedPrior;
		this.stage = stage;
		this.totalConfidenceDenominator = totalConfidenceDenominator;
		if (params == null) {
			this.params = new HashMap<String,String>(LearnItConfig.allParams());
		}

		try {
			this.target = TargetFactory.fromString(this.targetPath);//StorageUtils.deserialize(new File(LearnItConfig.get("learnit_root")+"/inputs/targets/json",this.targetPath+".json"), Target.class, false);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public TargetAndScoreTables copyWithPatternScoreTable(final PatternScoreTable newPatternScores) {
		return new TargetAndScoreTables(startDate, targetPath, seedScores, newPatternScores, tripleScores, iteration, goodSeedPrior, totalConfidenceDenominator, stage, params);
	}

	public TargetAndScoreTables copyWithPatternAndSeedScoreTable(final PatternScoreTable newPatternScores, final SeedScoreTable newSeedScores) {
		return new TargetAndScoreTables(startDate, targetPath, newSeedScores, newPatternScores, tripleScores, iteration, goodSeedPrior, totalConfidenceDenominator, stage, params);
	}

	public TargetAndScoreTables(String targetPath) {
		this.experimentName = LearnItConfig.get("learnit_expt_suffix");
		this.startDate = new Date();
		this.targetPath = targetPath.endsWith(".json") ? targetPath.substring(targetPath.lastIndexOf("/"), targetPath.length()-5) : targetPath;
		this.seedScores = new SeedScoreTable();
		this.patternScores = new PatternScoreTable();
		this.tripleScores = new TripleScoreTable();
		this.iteration = 0;
		this.goodSeedPrior = 0.001D;
		this.totalConfidenceDenominator = 1000;
		this.stage = "initial";
		this.params = new HashMap<String,String>(LearnItConfig.allParams());

		try {
			System.out.println(this.targetPath);
			this.target = TargetFactory.fromString(this.targetPath);//StorageUtils.deserialize(new File(LearnItConfig.get("learnit_root")+"/inputs/targets/json",this.targetPath+".json"), Target.class, false);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	public TargetAndScoreTables(Target target) {
		this.experimentName = LearnItConfig.get("learnit_expt_suffix");
		this.startDate = new Date();
		this.targetPath = target.getName();
		this.seedScores = new SeedScoreTable();
		this.patternScores = new PatternScoreTable();
		this.tripleScores = new TripleScoreTable();
		this.iteration = 0;
		this.stage = "initial";
		this.target = target;
		this.goodSeedPrior = 0.001D;
		this.params = new HashMap<String,String>(LearnItConfig.allParams());
	}

	public String getRelationPathName() {
		return this.targetPath;
	}

	public TargetAndScoreTables(TargetAndScoreTables oldTable, String targetPath) {
		this(oldTable.startDate, targetPath, oldTable.seedScores, oldTable.patternScores, oldTable.tripleScores, oldTable.iteration, oldTable.goodSeedPrior, oldTable.totalConfidenceDenominator, oldTable.stage, null);
	}

	public TargetAndScoreTables withNewStartDate() {
		return new TargetAndScoreTables(new Date(), targetPath,seedScores,patternScores,tripleScores,iteration,goodSeedPrior,totalConfidenceDenominator,stage, null);
	}

	public void setGoodSeedPrior(double goodInstanceEstimate, double totalInstanceDenominator) {
		double value = goodInstanceEstimate/totalInstanceDenominator;
	  Glogger.logger().debug("Setting good seed prior to " + value);
		if (value == 0) {
		  Glogger.logger().error(
			    "ERROR: should not have a good seed prior of 0, ignoring...");
			return;
		}
		this.totalConfidenceDenominator = totalInstanceDenominator;
		this.goodSeedPrior = value;
		// when the gsp changes, we want to recalculate all TPFN stats
		for (ObjectWithScore<LearnitPattern,PatternScore> scored : patternScores.getFrozenObjectsWithScores()) {
//			scored.getScore().setConfidenceDenominator(goodInstanceEstimate);
			scored.getScore().calculateTPFNStats(this.goodSeedPrior, totalInstanceDenominator);
		}
	}

	public double getGoodSeedPrior() {
		return this.goodSeedPrior;
	}

	public void setTotalInstanceDenominator(double value) {
		this.totalConfidenceDenominator = value;
	}

	public double getTotalInstanceDenominator() {
		return this.totalConfidenceDenominator;
	}

	public void setConfidenceThreshold(double threshold) {
		patternScores.setConfidenceThreshold(threshold);
		seedScores.setConfidenceThreshold(threshold);
	}

  	/*
  	 * initial seed gets a score of 0.9, confidence of 1.0
  	 */
	public void setInitialSeeds(Set<Seed> initialSeeds) {

		//populate the initial seeds
		for (Seed s : initialSeeds) {
			seedScores.addDefault(s);
			seedScores.getScore(s).setScore(0.9);
			seedScores.getScore(s).setConfidence(1.0);
			seedScores.getScore(s).freezeScore(0);
		}
	}

  	/*
  	 * initial pattern gets a recall of 0.9? This would affect TPFN calculation
  	 */
	public void setInitialPatterns(Set<LearnitPattern> initialPatterns) {
		// populate the initial patterns
		for(LearnitPattern p : initialPatterns) {
			this.patternScores.addDefault(p);
			this.patternScores.getScore(p).setConfidence(1.0);
			this.patternScores.getScore(p).setPrecision(1.0);
			this.patternScores.getScore(p).setRecall(0.9);
//			this.patternScores.getScore(p).setConfidenceDenominator(1.0);
			this.patternScores.getScore(p).freezeScore(0);
		}
	}

	/*
  	 * copy initial values from those for pattern
  	 */
	public void setInitialTriples(Set<SeedPatternPair> initialTriples) {
		// populate the initial patterns
		for(SeedPatternPair p : initialTriples) {
			this.tripleScores.addDefault(p);
			this.tripleScores.getScore(p).setConfidence(1.0);
			this.tripleScores.getScore(p).setPrecision(1.0);
			this.tripleScores.getScore(p).setRecall(0.9);
//			this.patternScores.getScore(p).setConfidenceDenominator(1.0);
			this.tripleScores.getScore(p).freezeScore(0);
		}
	}

	public String getExperimentName() {
		return experimentName;
	}

	public void updateStage(String stage) {
		this.stage = stage;
	}

	public void incrementStage() {
		if (stage.equals("initializing-stats")) {
			stage = "seed-proposer";
		} else if (stage.equals("pattern-proposer")) {
			stage = "pattern-pruner";
		} else if (stage.equals("pattern-pruner")) {
			stage = "seed-proposer";
		} else if (stage.equals("post-pruner")) {
			stage = "seed-proposer";
		} else if (stage.equals("seed-proposer")) {
			stage = "seed-pruner";
		} else if (stage.equals("seed-pruner")) {
			stage = "pattern-proposer";
			incrementIteration();
		} else if (stage.equals("preprocessing")) {
			stage = "pattern-proposer";
		} else {
			throw new RuntimeException("Unknown stage in incrementStage "+stage);
		}
	}

	public void incrementIteration() {
		this.iteration++;
		seedScores.incrementIteration();
		patternScores.incrementIteration();
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iter) {
		this.iteration = iter;
		seedScores.setIteration(iter);
		patternScores.setIteration(iter);
	}

	public String getStageName() {
		return stage;
	}

	public Target getTarget() {
		return target;
	}

	public SeedScoreTable getSeedScores() {
		return seedScores;
	}

	public PatternScoreTable getPatternScores() {
		return patternScores;
	}

	public TripleScoreTable getTripleScores() {
		return tripleScores;
	}

	//TODO: Figure out if adding a stage for Triples is required
	public Stage<? extends PartialInformation> getStage() {
		if (stage.equals("initializing-stats")) {
			return new InitializingStats(this);
		}
		if (stage.equals("seed-proposer")) {
			return new SeedProposer(this);

		} else if (stage.equals("seed-pruner")) {
			return new SeedPruner(this);

		} else if (stage.equals("pattern-proposer")) {
			return new PatternProposer(this);

		} else if (stage.equals("pattern-pruner")) {
			return new PatternPruner(this);

		} else if (stage.equals("post-pruner")) {
			return new PostPruner(this);

		} else if (stage.equals("preprocessing")) {
			return new Preprocessor(this);

		} else {
			throw new RuntimeException("Unknown stage in getStage "+stage);
		}
	}

	public Set<Seed> initializeSeedScores(PatternProposalInformation info) {

		List<ObjectWithScore<Seed, SeedScore>> toscore = getSeedScores().getFrozenObjectsWithScores();
		Set<Seed> result = new HashSet<Seed>();

		//get frequencies of seeds
		for (ObjectWithScore<Seed, SeedScore> scoredSeed : toscore) {
			scoredSeed.getScore().unfreeze();
			scoredSeed.getScore().setFrequency(info.getSeedCount(scoredSeed.getObject()));
			scoredSeed.getScore().freezeScore(0);
			result.add(scoredSeed.getObject());
		}

		return result;
	}

	public Multimap<MatchInfo,LearnitPattern> extractRelations(DocTheory document) {
		return DecodingExtractionModule.extractRelations(document, this);
	}

	public Multimap<MatchInfo,LearnitPattern> extractRelations(BilingualDocTheory document) {
		return DecodingExtractionModule.extractRelations(document, this);
	}

	public String formattedStartDate() {
		SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
		return ft.format(startDate);
	}

	public void archive() throws IOException {
		File path = new File(LearnItConfig.get("archive_dir")+"/extractors/"+this.getExperimentName()+"/"+
					this.getTarget().getName()+"/"+formattedStartDate()+"_extractor.json");
		new File(path.getParent()).mkdirs();
		serialize(path);
	}

	public void serialize(File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		StorageUtils.getDefaultMapper().writeValue(out, this);
		out.close();
	}

	public static TargetAndScoreTables deserialize(File file) throws IOException {
		InputStream in = new FileInputStream(file);
        ObjectMapper mapper = StorageUtils.getDefaultMapper();
		mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS,false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TargetAndScoreTables result = mapper.readValue(in, TargetAndScoreTables.class);
		in.close();
		return result;
	}

}
