package com.bbn.akbc.neolearnit.mappings.groups;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToAnswerMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToCommonRelationFeatureMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the collection of mappings available for evaluation.
 *
 * @author mshafir
 *
 */

public class EvalReportMappings {

	@JsonProperty
	private final TargetAndScoreTables extractor;

	private final InstanceToAnswerMapping instance2Answer;
	private final InstanceToPatternMapping instance2Pattern;
	@JsonProperty
	private final InstanceToMatchInfoDisplayMap instance2MatchInfo;

//	Optional<InstanceToLexFeatureSetMapping> lexFeatureSetMapping;
  	Optional<InstanceToCommonRelationFeatureMapping> instanceToCommonRelationFeature;

	@JsonProperty
	private MapStorage<InstanceIdentifier, EvalAnswer> answerInstanceMap() {
		return instance2Answer.getStorage();
	}

	@JsonProperty
	private MapStorage<InstanceIdentifier, LearnitPattern> patternInstanceMap() {
		return instance2Pattern.getStorage();
	}

	@JsonCreator
	private EvalReportMappings(
			@JsonProperty("extractor") TargetAndScoreTables data,
			@JsonProperty("answerInstanceMap") MapStorage<InstanceIdentifier, EvalAnswer> answer2Instance,
			@JsonProperty("patternInstanceMap") MapStorage<InstanceIdentifier, LearnitPattern> pattern2Instance,
			@JsonProperty("instance2MatchInfo") InstanceToMatchInfoDisplayMap instance2MatchInfo) {
		this.extractor = data;
		this.instance2Answer = new InstanceToAnswerMapping(answer2Instance);
		this.instance2Pattern =  new InstanceToPatternMapping(pattern2Instance);
		this.instance2MatchInfo = instance2MatchInfo;
	}

	public EvalReportMappings(TargetAndScoreTables data,
			InstanceToAnswerMapping instance2Answer,
			InstanceToPatternMapping instance2Pattern,
			InstanceToMatchInfoDisplayMap instance2Match) {
		this.extractor = data;
		this.instance2Answer = instance2Answer;
		this.instance2Pattern = instance2Pattern;
		this.instance2MatchInfo = instance2Match;
	}

	public EvalReportMappings(TargetAndScoreTables data,
			Mappings mappings,
			InstanceToAnswerMapping instance2Answer,
			InstanceToMatchInfoDisplayMap instance2Match) {
		this.extractor = data;
		this.instance2Answer = instance2Answer;
		this.instance2Pattern = mappings.getInstance2Pattern();
		this.instance2MatchInfo = instance2Match;
	}

	public EvalReportMappings(TargetAndScoreTables data,
			Mappings mappings,
			InstanceToAnswerMapping instance2Answer,
			InstanceToMatchInfoDisplayMap instance2Match,
	//		InstanceToLexFeatureSetMapping instance2lex
	    InstanceToCommonRelationFeatureMapping instanceToCommonRelationFeature
	) {
		this.extractor = data;
		this.instance2Answer = instance2Answer;
		this.instance2Pattern = mappings.getInstance2Pattern();
		this.instance2MatchInfo = instance2Match;

//		this.lexFeatureSetMapping = Optional.of(instance2lex);
	  	this.instanceToCommonRelationFeature = Optional.of(instanceToCommonRelationFeature);
	}

	public InstanceToAnswerMapping getInstance2Answer() {
		return instance2Answer;
	}

  /*
	public InstanceToLexFeatureSetMapping getInstance2lexFeatureSet() {
		return lexFeatureSetMapping.get();
	}
*/
  	public InstanceToCommonRelationFeatureMapping getInstanceToCommonRelationFeature() {
	  return instanceToCommonRelationFeature.get();
	}

	public InstanceToPatternMapping getInstance2Pattern() {
		return instance2Pattern;
	}

	public InstanceToMatchInfoDisplayMap getInstance2MatchInfo() {
		return instance2MatchInfo;
	}

	public Multiset<LearnitPattern> getAllPatterns() {
		return instance2Pattern.getAllPatterns();
	}

	public boolean hasCorrectAnswer(LearnitPattern pattern) {
		for (InstanceIdentifier id : instance2Pattern.getInstances(pattern)) {
			if (instance2Answer.getAnswer(id).isCorrect())
				return true;
		}
		return false;
	}

	public Set<LearnitPattern> getPatternsWithACorrectAnswer() {
		Set<LearnitPattern> result = new HashSet<LearnitPattern>();
		for (LearnitPattern pattern : getAllPatterns()) {
			if (hasCorrectAnswer(pattern))
				result.add(pattern);
		}
		return result;
	}

	public TargetAndScoreTables getExtractor() {
		return extractor;
	}

	public void serialize(File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		StorageUtils.getDefaultMapper().writeValue(stream, this);
		stream.close();
	}

	public static EvalReportMappings deserialize(File file) throws IOException {
		return StorageUtils.getDefaultMapper().readValue(file, EvalReportMappings.class);
	}

}
