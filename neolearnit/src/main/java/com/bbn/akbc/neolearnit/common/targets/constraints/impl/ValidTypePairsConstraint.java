package com.bbn.akbc.neolearnit.common.targets.constraints.impl;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.targets.constraints.SlotPairMatchConstraint;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.utility.Pair;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.ValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

public class ValidTypePairsConstraint extends SlotPairMatchConstraint {

	private final Set<Pair<Symbol, Symbol>> validPairs;

	private static final Symbol WILDCARD = Symbol.from("*");

	@JsonProperty
	private Set<Pair<String, String>> validPairs() {
		Set<Pair<String, String>> serial = new HashSet<Pair<String,String>>();
		for (Pair<Symbol, Symbol> pair : validPairs) {
			serial.add(new Pair<String, String>(pair.key.toString(), pair.value.toString()));
		}
		return serial;
	}

	private Set<Pair<Symbol, Symbol>> toSymbolPairs(Set<Pair<String, String>> stringPairs) {
		Set<Pair<Symbol, Symbol>> symbolPairs = new HashSet<Pair<Symbol,Symbol>>();
		for (Pair<String, String> pair : stringPairs) {
			symbolPairs.add(new Pair<Symbol, Symbol>(Symbol.from(pair.key), Symbol.from(pair.value)));
		}
		return symbolPairs;
	}

	@JsonCreator
	private ValidTypePairsConstraint(@JsonProperty("validPairs") Set<Pair<String, String>> validPairs) {
		this.validPairs = toSymbolPairs(validPairs);
	}

	public static final class Builder {

		private final ImmutableSet.Builder<Pair<String, String>> validPairs;

		public Builder() {
			validPairs = ImmutableSet.builder();
		}

		public Builder withAddedPair(Pair<String, String> pair) {
			validPairs.add(new Pair<String, String>(pair.key, pair.value));
			return this;
		}

		public ValidTypePairsConstraint build() {
			return new ValidTypePairsConstraint(validPairs.build());
		}
	}

	@Override
	public boolean offForEvaluation() {
		return false;
	}

	private boolean pairIsValid(Symbol type0, Symbol type1) {
		return validPairs.contains(new Pair<Symbol, Symbol>(type0, type1)) ||
			   validPairs.contains(new Pair<Symbol, Symbol>(type0, WILDCARD)) ||
			   validPairs.contains(new Pair<Symbol, Symbol>(WILDCARD, type1));
	}

	@Override
	public boolean valid(Spanning slot0, Spanning slot1) {
		Symbol type0 = slot0 instanceof Mention ? ((Mention)slot0).entityType().name() : ((ValueMention)slot0).fullType().name();
		Symbol type1 = slot1 instanceof Mention ? ((Mention)slot1).entityType().name() : ((ValueMention)slot1).fullType().name();

		return pairIsValid(type0,type1);
	}

	@Override
	public boolean valid(InstanceIdentifier instanceId, Seed seed) {
		return pairIsValid(Symbol.from(instanceId.getSlotEntityType(0)), Symbol.from(instanceId.getSlotEntityType(1)));
	}

}
