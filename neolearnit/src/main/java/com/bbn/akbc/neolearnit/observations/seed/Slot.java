package com.bbn.akbc.neolearnit.observations.seed;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class Slot extends LearnItObservation {

	private final List<Symbol> textOptions; //Pre-filtering: 0 -> bestName, 1 -> atomic head
											//Post-filtering: singleton list

	@JsonProperty
	private List<String> text() {
		return Lists.newArrayList(
				Iterables.transform(textOptions, new Function<Symbol, String>() {

					@Override
					public String apply(Symbol input) {
						return input.toString();
					}
				})
			);
	}

	@JsonCreator
	public Slot(@JsonProperty("text") List<String> names) {
		if (names.size() == 1 || names.get(0).equals(names.get(1))) {
			this.textOptions = ImmutableList.of(Symbol.from(names.get(0)));
		} else {
			this.textOptions = ImmutableList.copyOf(
					Iterables.transform(names, new Function<String, Symbol>() {

						@Override
						public Symbol apply(String input) {
							return Symbol.from(input);
						}
					})
				);
		}
	}

	public Slot(Symbol slotText) {
		this.textOptions = ImmutableList.of(slotText);
	}

	public static Slot from(Slot base, boolean useBestName) {
		if (base.textOptions.size() == 1) {
			return base;
		}
		if (useBestName) {
			return new Slot(base.textOptions.get(0));
		} else {
			return new Slot(base.textOptions.get(1));
		}
	}

	public Symbol getText() {
		return textOptions.get(0);
	}

	public Symbol getHeadText() {
		return textOptions.get(textOptions.size()-1);
	}

	@Override
	public String toPrettyString() {
		return toString();
	}

	@Override
	public String toString() {
		return Joiner.on(" || ").join(textOptions);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((toString() == null) ? 0 : toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slot other = (Slot) obj;
		if (toString() == null) {
			if (other.toString() != null)
				return false;
		} else if (!toString().equals(other.toString()))
			return false;
		return true;
	}

	@Override
	public String toIDString() {
		return toString();
	}
}
