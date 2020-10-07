package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.ValidTypeConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.types.EntityType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// import com.bbn.serif.types.SerifTypes;

public class EntityTypeConstraint extends ValidTypeConstraint {

//	private final Set<String> entityTypes;

	@JsonProperty
	public Set<String> entityTypes() {
		return new HashSet<String>(validTypes);
	}

	@JsonCreator
	public EntityTypeConstraint(
			@JsonProperty("slot") int slot,
			@JsonProperty("entityTypes") Iterable<String> entityTypes) {
		super(slot,entityTypes);
	}

	public EntityTypeConstraint(int slot, String entityType) {
		super(slot,ImmutableSet.of(entityType));
	}

	@Override
	public boolean valid(Spanning mention) {
		if (mention instanceof Mention) {
			Optional<Mention> m = Optional.of((Mention)mention);
			while (m.isPresent()) {
				if (validTypes.contains(m.get().entityType().toString())) {
					return true;
				}
				m = m.get().child();
			}
			return false;
		} else {
			return false;
		}
	}

//	@Override
//	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
//		return validTypes.contains(instanceId.getSlotEntityType(slot));
//	}

/*
	public static EntityType getTypeFromString(String entityType) {
		SerifTypes serifTypes;
		try {
			serifTypes = SerifTypes.fromParameters(LearnItConfig.params());
		} catch (IOException e) {
			throw new RuntimeException("EntityTypeConstraint: Failed to load types");
		}
		return serifTypes.entityTypes().fromSymbol(Symbol.from(entityType));
	}
*/
	public static EntityType getTypeFromString(String entityType) {
		try {
//			return EntityType.loadEntityTypesFrom(LearnItConfig.params()).fromSymbol(
//					Symbol.from(entityType)).get();
			return EntityType.of(entityType);
		} catch (Exception e) {
			throw new RuntimeException("EntityTypeConstraint: Failed to load types");
		}
        }
	@Override
	public Builder setBrandyConstraints(Builder builder) {
		if (builder instanceof MentionPattern.Builder) {
			MentionPattern.Builder mbuilder = (MentionPattern.Builder)builder;
			List<EntityType> types = new ArrayList<EntityType>();
			for (String type : validTypes) {
				types.add(getTypeFromString(type));
			}

			mbuilder.withAceTypes(types);
			return mbuilder;

		} else {
			throw new RuntimeException("EntityTypeConstraint: Invalid builder "+builder);
		}
	}

	@Override
	public String toString() {
		return "EntityTypeConstraint [entityTypes=" + validTypes + ", slot="
				+ slot + "]";
	}

	public String getValidTypesSpaceDelimited() {
		StringBuilder sb = new StringBuilder();
		for(String validType : validTypes)
			sb.append(validType + " ");
		return sb.toString().trim();
	}

    @Override
    public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        if (slot == 0) {
            return instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || validTypes.contains(instanceId.getSlotEntityType(slot));
        } else if (slot == 1) {
            return instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.Mention) || validTypes.contains(instanceId.getSlotEntityType(slot));
        } else {
            throw new NotImplementedException();
        }
    }
}
