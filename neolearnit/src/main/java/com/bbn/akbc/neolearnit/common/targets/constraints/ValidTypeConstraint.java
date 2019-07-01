package com.bbn.akbc.neolearnit.common.targets.constraints;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public abstract class ValidTypeConstraint extends AbstractSlotMatchConstraint {

	protected final Set<String> validTypes;

	@JsonProperty
	private Set<String> validTypes() {
		return new HashSet<String>(validTypes);
	}

	protected ValidTypeConstraint(
		@JsonProperty("slot") int slot,
		@JsonProperty("validTypes") Iterable<String> validTypes)
	{
		super(slot);
		this.validTypes = ImmutableSet.copyOf(validTypes);
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return validTypes.contains(instanceId.getSlotEntityType(slot));
	}

	public Set<String> getValidTypeSet() {
		return validTypes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((validTypes == null) ? 0 : validTypes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValidTypeConstraint other = (ValidTypeConstraint) obj;
		if (validTypes == null) {
			if (other.validTypes != null)
				return false;
		} else if (!validTypes.equals(other.validTypes))
			return false;
		return true;
	}

}
