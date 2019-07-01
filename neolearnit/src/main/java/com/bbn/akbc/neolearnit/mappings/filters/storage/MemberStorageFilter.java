package com.bbn.akbc.neolearnit.mappings.filters.storage;

import com.bbn.akbc.neolearnit.storage.MapStorage;

import java.util.Collection;
import java.util.HashSet;

public class MemberStorageFilter<T, U> implements StorageFilter<T, U> {

	private final Collection<T> tSet;
	private final Collection<U> uSet;
	private final boolean matchT;
	private final boolean matchU;

	public MemberStorageFilter(Collection<T> tSet, Collection<U> uSet) {
		if (tSet == null) {
			this.tSet = new HashSet<>();
			this.matchT = false;
		} else {
			this.tSet = tSet;
			this.matchT = true;
		}

		if (uSet == null) {
			this.uSet = new HashSet<>();
			this.matchU = false;
		} else {
			this.uSet = uSet;
			this.matchU = true;
		}
	}

	public MemberStorageFilter(Collection<T> tSet) {
		this.tSet = tSet;
		this.matchT = true;
		this.uSet = new HashSet<>();
		this.matchU = false;
	}

	public static <T,U> MemberStorageFilter<T,U> fromLeftSet(Collection<T> tSet) {
		return new MemberStorageFilter<>(tSet, null);
	}

	public static <T,U> MemberStorageFilter<T,U> fromRightSet(Collection<U> uSet) {
		return new MemberStorageFilter<>(null, uSet);
	}

    @Override
    public MapStorage<T, U> filter(MapStorage<T, U> input) {
        MapStorage.Builder<T, U> newBuilder = input.newBuilder();

        Collection<T> leftSetPtr = matchT ? tSet : input.getLefts();
        Collection<U> rightSetPtr = matchU ? uSet : input.getRights();

        if (matchT) {
            for (T t : tSet) {
                for (U u : input.getRight(t)) {
                    if (rightSetPtr.contains(u)) {
                        newBuilder.put(t, u);
                    }
                }
            }
        }

        if (matchU) {
            for (U u : uSet) {
                for (T t : input.getLeft(u)) {
                    if (leftSetPtr.contains(t)) {
                        newBuilder.put(t, u);
                    }
                }
            }
        }

        return newBuilder.build();
    }

}
