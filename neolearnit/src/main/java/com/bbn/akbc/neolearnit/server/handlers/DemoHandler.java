package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Map;

public class DemoHandler extends SimpleJSONHandler {

	private final Multimap<MatchInfoDisplay, LearnitPattern> displayMap;
	private final Map<Target, EfficientMapDataStore<LearnitPattern,Double>> patternScoreTables;

	public DemoHandler(Multimap<MatchInfoDisplay, LearnitPattern> displayMap, Map<Target,EfficientMapDataStore<LearnitPattern,PatternScore>> patternScoreTables) {
		this.displayMap = displayMap;
        this.patternScoreTables = new HashMap<Target, EfficientMapDataStore<LearnitPattern, Double>>();
		for (Target t : patternScoreTables.keySet()) {
            Map<LearnitPattern,Double> confMap = new HashMap<LearnitPattern, Double>();
            Map<LearnitPattern,PatternScore> scoreMap = patternScoreTables.get(t).makeMap();
            for (LearnitPattern p : scoreMap.keySet()) {
                confMap.put(p,scoreMap.get(p).getConfidence());
            }
            this.patternScoreTables.put(t,EfficientMapDataStore.fromMap(confMap));
        }
	}

	@JettyMethod("/demo/load_multimap")
	public EfficientMultimapDataStore<MatchInfoDisplay, LearnitPattern> loadMultimap() {
		return EfficientMultimapDataStore.fromMultimap(displayMap);
	}

//	@JettyMethod("/demo/load_pattern_scores")
//	public EfficientMapDataStore<Target, EfficientMapDataStore<LearnitPattern,PatternScore>> loadPatternScores() {
//		return EfficientMapDataStore.fromMap(patternScoreTables);
//	}

    @JettyMethod("/demo/load_pattern_scores")
    public EfficientMapDataStore<Target, EfficientMapDataStore<LearnitPattern,Double>> loadPatternScores() {
        return EfficientMapDataStore.fromMap(patternScoreTables);
    }
}
