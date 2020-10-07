package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.ValidTypeConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.serif.patterns.Pattern.Builder;
import com.bbn.serif.patterns.ValueMentionPattern;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.types.ValueType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// import com.bbn.serif.types.SerifTypes;

public class ValueTypeConstraint extends ValidTypeConstraint {

//	private final Set<String> valueTypes;

	@JsonProperty
	private Set<String> valueTypes() {
		return new HashSet<String>(validTypes);
	}


	@JsonCreator
	public ValueTypeConstraint(
			@JsonProperty("slot") int slot,
			@JsonProperty("valueTypes") Iterable<String> valueTypes) {
		super(slot,valueTypes);
	}

	@Override
	public boolean valid(Spanning mention) {

		if (mention instanceof ValueMention) {
			Optional<ValueMention> m = Optional.of((ValueMention)mention);
            // special case: if valueTypes contains "all" then every value type is valid
            return m.isPresent() &&
                    (validTypes.contains(m.get().fullType().toString()) ||
                            validTypes.contains("all"));
        } else {
			return false;
		}
	}

//	@Override
//	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
//		return validTypes.contains(instanceId.getSlotEntityType(slot));
//	}

/*
	public static ValueType getTypeFromString(String valueType) {
		SerifTypes serifTypes;
		try {
			serifTypes = SerifTypes.fromParameters(LearnItConfig.params());
		} catch (IOException e) {
			throw new RuntimeException("ValueTypeConstraint: Failed to load types");
		}
		return serifTypes.valueTypes().fromSymbol(Symbol.from(valueType));
	}
*/

	public static ValueType getTypeFromString(String valueType) {
		return ValueType.parseDottedPair(valueType);
		/*
//                SerifTypes serifTypes;
                try {
//                        serifTypes = SerifTypes.fromParameters(LearnItConfig.params());

			 return ValueType.loadValueTypesFrom(LearnItConfig.params()).fromSymbol(Symbol.from(valueType)).get();
		} catch (IOException e) {
                        throw new RuntimeException("ValueTypeConstraint: Failed to load types");
                }
//                return serifTypes.valueTypes().fromSymbol(Symbol.from(valueType));
		*/
        }

	@Override
	public Builder setBrandyConstraints(Builder builder) {
		if (builder instanceof ValueMentionPattern.Builder) {
			ValueMentionPattern.Builder mbuilder = (ValueMentionPattern.Builder)builder;
			List<ValueType> types = new ArrayList<ValueType>();
			for (String type : validTypes) {
				types.add(getTypeFromString(type));
			}

			mbuilder.withValueTypes(types);
			return mbuilder;

		} else {
			throw new RuntimeException("ValueTypeConstraint: Invalid builder "+builder);
		}
	}

	@Override
	public String toString() {
		return "ValueTypeConstraint [valueTypes=" + validTypes + ", slot="
				+ slot + "]";
	}

    @Override
    public boolean valid(InstanceIdentifier instanceId, Seed seed) {
        if (slot == 0) {
            return instanceId.getSlot0SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention) && (validTypes.contains("all") || validTypes.contains(instanceId.getSlotEntityType(0)));
        } else if (slot == 1) {
            return instanceId.getSlot1SpanningType().equals(InstanceIdentifier.SpanningType.ValueMention) && (validTypes.contains("all") || validTypes.contains(instanceId.getSlotEntityType(1)));
        } else {
            throw new NotImplementedException();
        }
    }
}
