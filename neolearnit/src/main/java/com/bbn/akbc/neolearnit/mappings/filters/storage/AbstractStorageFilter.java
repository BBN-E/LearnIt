package com.bbn.akbc.neolearnit.mappings.filters.storage;

import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.MapStorage.Builder;

public abstract class AbstractStorageFilter<T, U> implements StorageFilter<T, U> {

    public abstract boolean leftPass(T left, MapStorage<T, U> storage);

    public abstract boolean rightPass(U right, MapStorage<T, U> storage);

    public abstract boolean pairPass(T left, U right, MapStorage<T, U> storage);

    @Override
    public MapStorage<T, U> filter(MapStorage<T, U> input) {

        Builder<T, U> newBuilder = input.newBuilder();

        for (T left : input.getLefts()) {
            if (leftPass(left, input)) {
                for (U right : input.getRight(left)) {
                    if (rightPass(right, input) && pairPass(left, right, input)) {
                        newBuilder.put(left, right);
                    }
                }
            }
        }

        return newBuilder.build();
    }
}
