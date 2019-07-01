package com.bbn.akbc.neolearnit.common.targets.constraints;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo.LanguageMatchInfo;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.Spanning;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractSlotMatchConstraint extends LanguageMatchConstraint implements SlotMatchConstraint {

	@JsonProperty
	protected final int slot;

	@JsonProperty
	protected final boolean canBeEmptySlot;

	public AbstractSlotMatchConstraint(String language, int slot,boolean canBeEmptySlot) {
		super(language);
		this.slot = slot;
		this.canBeEmptySlot = canBeEmptySlot;
	}

	public AbstractSlotMatchConstraint(String language, int slot) {
		super(language);
		this.slot = slot;
		this.canBeEmptySlot = false;
	}

	public AbstractSlotMatchConstraint(int slot) {
		super();
		this.slot = slot;
		this.canBeEmptySlot = false;
	}

	public AbstractSlotMatchConstraint(int slot,boolean canBeEmptySlot) {
		super();
		this.slot = slot;
		this.canBeEmptySlot = canBeEmptySlot;
	}

	@Override
	public int getSlot() {
		return slot;
	}

	@Override
	public boolean valid(LanguageMatchInfo match) {
		if (slot == 0) {
			if(!match.getSlot0().isPresent()) {
				return this.canBeEmptySlot;
			}
			else {
				return valid(match.getSlot0().get());
			}
		} else if (slot == 1) {
			if(!match.getSlot1().isPresent()) {
				return this.canBeEmptySlot;
			}
			else {
				return valid(match.getSlot1().get());
			}
		} else {
			throw new RuntimeException("Unsupported slot "+slot+" in constraint.");
		}
	}

	public abstract boolean valid(Spanning mention);

	@Override
	public abstract Pattern.Builder setBrandyConstraints(Pattern.Builder builder);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + slot;
		result = prime * result + Boolean.hashCode(this.canBeEmptySlot);
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
		AbstractSlotMatchConstraint other = (AbstractSlotMatchConstraint) obj;
		if (slot != other.slot)
			return false;
		if (this.canBeEmptySlot != other.canBeEmptySlot)
			return false;
		return true;
	}
}
