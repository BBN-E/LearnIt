package com.bbn.akbc.neolearnit.observers.instance;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.google.inject.Inject;

public class SeedObserver extends AbstractInstanceIdObserver<Seed> {

	@Inject
	public SeedObserver(InstanceToSeedMapping.Builder recorder) {
		super(recorder);
	}

	@Override
	public void observe(MatchInfo match) {
		for (String language : match.getAvailableLanguages()) {
			this.record(match, Seed.from(match.getLanguageMatch(language), match.getTarget().isSymmetric()));

			/* for debug only */
//			Seed seed = Seed.from(match.getLanguageMatch(language), match.getTarget().isSymmetric());
//			System.out.println("observed seed: " + seed.toIDString());
			//
		}
	}

}
