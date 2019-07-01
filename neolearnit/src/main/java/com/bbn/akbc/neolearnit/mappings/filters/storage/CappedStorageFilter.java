package com.bbn.akbc.neolearnit.mappings.filters.storage;

import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class CappedStorageFilter<T,U> extends AbstractStorageFilter<T,U> {

	private final int leftCap;
	private final int rightCap;
	private final Multiset<T> leftCounts;
	private final Multiset<U> rightCounts;

	public CappedStorageFilter(int leftCap, int rightCap) {
		this.leftCap = leftCap;
		this.rightCap = rightCap;
		leftCounts = HashMultiset.<T>create();
		rightCounts = HashMultiset.<U>create();
	}

	@Override
	public boolean leftPass(T left, MapStorage<T, U> storage) {
		return true;
	}

	@Override
	public boolean rightPass(U right, MapStorage<T, U> storage) {
		return true;
	}

	@Override
	public boolean pairPass(T left, U right, MapStorage<T, U> storage) {
		if (leftCap != -1 && leftCounts.count(left) > leftCap)
			return false;
		if (rightCap != -1 && rightCounts.count(right) > rightCap)
			return false;

		leftCounts.add(left);
		rightCounts.add(right);
		return true;
	}

}
