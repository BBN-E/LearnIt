package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.mappings.callables.PatternInstanceMatchMappingExtractor;
import com.bbn.akbc.neolearnit.mappings.callables.PatternMatchMappingExtractor;
import com.bbn.akbc.neolearnit.mappings.callables.SeedInstanceMatchMappingExtractor;
import com.bbn.akbc.neolearnit.mappings.callables.SeedMatchMappingExtractor;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultisetDataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EvaluationHandler extends SimpleJSONHandler {

	private final Map<String,EvalReportMappings> evalMaps;

	public EvaluationHandler() {
		this.evalMaps = new HashMap<String,EvalReportMappings>();
	}

	public EvalReportMappings getMappings(String path) throws IOException {
		if (!evalMaps.containsKey(path)) {
			System.out.println("Loading report "+path);
			String filename = LearnItConfig.get("archive_dir")+"/reports/"+path;
			EvalReportMappings mappings = StorageUtils.deserialize(new File(filename), EvalReportMappings.class, true);
			//this.evalMaps.clear();
			this.evalMaps.put(path, mappings);
			System.out.println("Loaded "+path);
		}

		SeedSimilarity.load(evalMaps.get(path).getExtractor());
		return evalMaps.get(path);
	}

	@JettyMethod("/evaluation/refresh_cache")
	public String refreshCache() {
		SeedSimilarity.clear();
		this.evalMaps.clear();
		return "success";
	}

	@JettyMethod("/evaluation/get_extractor")
	public TargetAndScoreTables getExtractor(@JettyArg("path") String path) {
		try {
			TargetAndScoreTables extractor = getMappings(path).getExtractor();
			extractor.getPatternScores().reduceSize();
			extractor.getSeedScores().reduceSize();
			return extractor;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_answer_map")
	public MapStorage<InstanceIdentifier, EvalAnswer> getAnswerMap(@JettyArg("path") String path) {
		try {
			return getMappings(path).getInstance2Answer().getStorage();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_pattern_map")
	public MapStorage<InstanceIdentifier, LearnitPattern> getPatternMap(@JettyArg("path") String path) {
		try {
			EvalReportMappings mappings = getMappings(path);
			MapStorage.Builder<InstanceIdentifier, LearnitPattern> builder = mappings.getInstance2Pattern().getStorage().newBuilder();
			for (LearnitPattern pattern : getMappings(path).getAllPatterns().elementSet()) {
				if (pattern.isCompletePattern()) {
					for (InstanceIdentifier id : mappings.getInstance2Pattern().getInstances(pattern)) {
						builder.put(id, pattern);
					}
				}
			}

			return builder.build();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_all_patterns")
	public Set<LearnitPattern> getAllPatterns(@JettyArg("path") String path) {
		try {
			return getMappings(path).getInstance2Pattern().getAllPatterns().elementSet();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_instance_info")
	public MatchInfoDisplay getInstanceInfo(@JettyArg("path") String path, @JettyArg("instance") String instanceJSON) {
		try {
			InstanceIdentifier id = this.loadObject(instanceJSON, InstanceIdentifier.class);
			return getMappings(path).getInstance2MatchInfo().getMatchInfoDisplay(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_mapping_files")
	public Collection<String> getMappingsFilelists(@JettyArg("path") String path) {
		try {
            return Lists.newArrayList((new File(getMappingDir(path))).list());

		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}

    private String getMappingDir(String path) throws IOException {
		EvalReportMappings mappings = getMappings(path);
		return LearnItConfig.get("learnit_root")+"/expts/"+path.substring(0,path.indexOf('/'))+"/"+
				mappings.getExtractor().getRelationPathName()+"/mappings";
	}

	@JettyMethod("/evaluation/get_seeds_from_mappings")
	public EfficientMultisetDataStore<Seed> getSeedsFromMappings(@JettyArg("path") String path,
			@JettyArg("file") String file, @JettyArg("object") String pattern) {

		try {
			EvalReportMappings mappings = getMappings(path);
			System.out.println("Loading seeds from "+file);
			File f = new File(getMappingDir(path)+"/"+file);
			SeedMatchMappingExtractor seedExtractor = new SeedMatchMappingExtractor(f, mappings.getExtractor(), findPattern(path,pattern));
			return EfficientMultisetDataStore.fromMultiset(seedExtractor.call());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_patterns_from_mappings")
	public EfficientMultisetDataStore<LearnitPattern> getPatternsFromMappings(@JettyArg("path") String path,
			@JettyArg("file") String file, @JettyArg("object") String seedJson) {

		try {
			EvalReportMappings mappings = getMappings(path);
			System.out.println("Loading patterns from "+file);
			File f = new File(getMappingDir(path)+"/"+file);
			PatternMatchMappingExtractor patternExtractor = new PatternMatchMappingExtractor(f, mappings.getExtractor(), findSeed(path,seedJson));
			return EfficientMultisetDataStore.fromMultiset(patternExtractor.call());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_pattern_instances_from_mappings")
	public Collection<String> getPatternInstancesFromMappings(@JettyArg("path") String path,
			@JettyArg("file") String file, @JettyArg("object") String obj) {

		try {
			EvalReportMappings mappings = getMappings(path);
			System.out.println("Loading instances from "+file);
			File f = new File(getMappingDir(path)+"/"+file);
			PatternInstanceMatchMappingExtractor patternExtractor = new PatternInstanceMatchMappingExtractor(
					f, mappings.getExtractor(), findPattern(path,obj));
			return patternExtractor.call().elementSet();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_seed_instances_from_mappings")
	public Collection<String> getSeedInstancesFromMappings(@JettyArg("path") String path,
			@JettyArg("file") String file, @JettyArg("object") String obj) {

		try {
			EvalReportMappings mappings = getMappings(path);
			System.out.println("Loading instances from "+file);
			File f = new File(getMappingDir(path)+"/"+file);
			SeedInstanceMatchMappingExtractor seedExtractor = new SeedInstanceMatchMappingExtractor(
					f, mappings.getExtractor(), findSeed(path,obj));
			return seedExtractor.call().elementSet();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/evaluation/get_seed_score")
	public Double getSeedScore(@JettyArg("path") String path, @JettyArg("slot0") String slot0, @JettyArg("slot1") String slot1) {

        double score;
        if (LearnItConfig.optionalParamTrue("use_seed_groups")) {
            Seed seed = Seed.from("English", slot0, slot1);

            if (!SeedSimilarity.seedHasScore(seed)) return -1.0;
            score = SeedSimilarity.getUnknownSeedScore(seed);
            System.out.println("Score for " + seed.toSimpleString() + " = " + score);
        } else {
            String seed = slot0+"\t"+slot1;

            if (!SeedSimilarity.seedHasScore(seed)) return -1.0;
            score = SeedSimilarity.getUnknownSeedScore(seed);
            System.out.println("Score for " + seed + " = " + score);
        }
        return score;
	}

	public Seed findSeed(String path, String seedJson) throws IOException {
		EvalReportMappings mappings = getMappings(path);
		ObjectMapper mapper = StorageUtils.getMapperWithoutTyping();
		Seed seed = mapper.readValue(seedJson, Seed.class).withProperText(mappings.getExtractor().getTarget());
		return seed;
	}

	public LearnitPattern findPattern(String path, String patternId) throws IOException {
		EvalReportMappings mappings = getMappings(path);
		for (LearnitPattern pattern : mappings.getAllPatterns().elementSet()) {
			if (pattern.toIDString().equals(patternId)) return pattern;
		}
		return null;
	}


}
