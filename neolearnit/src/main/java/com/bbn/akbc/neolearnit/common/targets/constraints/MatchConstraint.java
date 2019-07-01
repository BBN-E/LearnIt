package com.bbn.akbc.neolearnit.common.targets.constraints;

import java.util.Collection;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.seed.Seed;

public interface MatchConstraint {

	public boolean valid(MatchInfo match);

	/**
	 * Used to further filters down mappings over multiple targets
	 * @param instanceId
	 * @param seeds
	 * @return
	 */
	public boolean valid(InstanceIdentifier instanceId, Collection<Seed> seeds, Target t);

	/**
	 * Whether this constraint should be off at evaluation time
	 * @return
	 */
	public boolean offForEvaluation();

}
