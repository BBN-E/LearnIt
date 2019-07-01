package com.bbn.akbc.neolearnit.server.handlers;

import java.util.Collection;

import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.postpruning.PostPruningInformation;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;

public class PostPruningHandler extends SimpleJSONHandler {

	private final TargetAndScoreTables data;
	private final PostPruningInformation info;

	public PostPruningHandler(TargetAndScoreTables data, PostPruningInformation info) {
		this.data = data;
		this.info = info;
	}

	@JettyMethod("/pruning/get_pattern_scores")
	public TargetAndScoreTables getPatternScores() {
		try {
			data.getPatternScores().reduceSize();
			data.getSeedScores().reduceSize();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@JettyMethod("/pruning/get_target_for_data")
	public Target getTargetForData() {
		return data.getTarget();
	}

	private LearnitPattern findPattern(String pattern) {
		for (LearnitPattern p : data.getPatternScores().keySet()) {
			if (p.toIDString().equals(pattern)) {
				return p;
			}
		}
		return null;
	}

	@JettyMethod("/pruning/get_pattern_matches")
	public Collection<String> getPatternMatches(@JettyArg("pattern") String pattern) {
		return info.getMatches(findPattern(pattern));
	}

	@JettyMethod("/pruning/set_frozen")
	public String setFrozen(@JettyArg("pattern") String pattern) {
		PatternScore score = data.getPatternScores().getScore(findPattern(pattern));
		score.freezeScore(score.getFrozenIteration());
		return "success";
	}

	@JettyMethod("/pruning/set_unfrozen")
	public String setUnfrozen(@JettyArg("pattern") String pattern) {
		data.getPatternScores().getScore(findPattern(pattern)).unfreeze();
		return "success";
	}

}
