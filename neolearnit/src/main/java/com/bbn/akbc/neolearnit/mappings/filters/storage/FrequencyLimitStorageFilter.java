package com.bbn.akbc.neolearnit.mappings.filters.storage;

import com.bbn.akbc.neolearnit.storage.MapStorage;

import java.util.HashSet;

public class FrequencyLimitStorageFilter<T, U> implements StorageFilter<T, U> {

	private final int leftMin;
	private final int leftMax;
	private final int rightMin;
	private final int rightMax;

	public FrequencyLimitStorageFilter(int leftMin, int leftMax, int rightMin,
			int rightMax) {
		this.leftMin = leftMin;
		this.leftMax = leftMax;
		this.rightMin = rightMin;
		this.rightMax = rightMax;
	}


	@Override
	public MapStorage<T, U> filter(MapStorage<T, U> input) {
		HashSet<T> tSet = new HashSet<>();
		for (T t : input.getLefts()) {
			if ((leftMin == -1 || input.getLefts().count(t) >= leftMin) && (leftMax == -1 || input.getLefts().count(t) <= leftMax)) {
				tSet.add(t);
			}
		}
		HashSet<U> uSet = new HashSet<>();
		for (U u : input.getRights()) {
			if ((rightMin == -1 || input.getRights().count(u) >= rightMin) && (rightMax == -1 || input.getRights().count(u) <= rightMax)) {
				uSet.add(u);
			}
		}
		return new MemberStorageFilter<>(tSet, uSet).filter(input);
	}
}
