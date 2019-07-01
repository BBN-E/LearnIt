package com.bbn.akbc.neolearnit.common;

public class SlotPair {
	final Integer slot1;
	final Integer slot2;
	public SlotPair(Integer slot1, Integer slot2) {
		this.slot1 = slot1;
		this.slot2 = slot2;
	}

	public Integer getFirstSlot() {
		return this.slot1;
	}

	public Integer getSecondSlot() {
		return this.slot2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((slot1 == null) ? 0 : slot1.hashCode());
		result = prime * result + ((slot2 == null) ? 0 : slot2.hashCode());
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
		SlotPair other = (SlotPair) obj;
		if (slot1 == null) {
			if (other.slot1 != null)
				return false;
		} else if (!slot1.equals(other.slot1))
			return false;
		if (slot2 == null) {
			if (other.slot2 != null)
				return false;
		} else if (!slot2.equals(other.slot2))
			return false;
		return true;
	}
}
