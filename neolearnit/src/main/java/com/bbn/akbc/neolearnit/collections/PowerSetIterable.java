package com.bbn.bue.common.collections;

import com.bbn.bue.common.StringUtils;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

/**
 * @deprecated Prefer {@code Sets.powerSet()}.
 */
@Deprecated
public class PowerSetIterable<T> implements Iterable<Iterable<T>> {

    private final ImmutableList<ImmutableList<T>> choices;

    private PowerSetIterable(List<ImmutableList<T>> choices) {
        this.choices = ImmutableList.copyOf(choices);
    }

    public static void main(String[] args) {
        PowerSetIterable.Builder<String> builder = new PowerSetIterable.Builder<String>();

        builder.withChoiceAdd("I");
        builder.withChoiceAdd("We");
        builder.withChoiceAdd("You");
        builder.withChoiceAdd("They");
        builder.withCommitChoiceSet();

        builder.withChoiceAdd("can");
        builder.withChoiceAdd("may");
        builder.withChoiceAdd("will");
        builder.withCommitChoiceSet();

        builder.withChoiceAdd("print");
        builder.withChoiceAdd("read");
        builder.withCommitChoiceSet();

        System.out.println("Printing power set...");
        PowerSetIterable<String> powerSet = builder.build();
        for (Iterable<String> it : powerSet) {
            System.out.println(StringUtils.SpaceJoin.apply(it));
        }

    }

    @Override
    public Iterator<Iterable<T>> iterator() {
        return new PowerSetIterator();
    }

    public static class Builder<T> {

        private final ImmutableList.Builder<ImmutableList<T>> choiceBuilder;
        private ImmutableList.Builder<T> currentBuilder;

        public Builder() {
            choiceBuilder = new ImmutableList.Builder<ImmutableList<T>>();
            currentBuilder = new ImmutableList.Builder<T>();
        }

        public PowerSetIterable<T> build() {
            return new PowerSetIterable<T>(choiceBuilder.build());
        }

        public Builder<T> withChoiceSetAdd(Iterable<T> choices) {
            choiceBuilder.add(ImmutableList.copyOf(choices));
            return this;
        }

        public Builder<T> withChoiceAdd(T choice) {
            currentBuilder.add(choice);
            return this;
        }

        public Builder<T> withCommitChoiceSet() {
            choiceBuilder.add(currentBuilder.build());
            currentBuilder = new ImmutableList.Builder<T>();
            return this;
        }

    }

    private class PowerSetIterator implements Iterator<Iterable<T>> {

        private int count;
        private int total;

        public PowerSetIterator() {
            this.count = 0;
            this.total = 1;
            for (ImmutableList<T> choiceSet : choices) {
                this.total *= choiceSet.size();
            }
        }

        @Override
        public boolean hasNext() {
            return this.count < this.total;
        }

        @Override
        public Iterable<T> next() {
            PowerSetMemberIterable nextIt = new PowerSetMemberIterable(count);
            this.count++;
            return nextIt;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private class PowerSetMemberIterable implements Iterable<T> {

            private final int seed;

            public PowerSetMemberIterable(int seed) {
                this.seed = seed;
            }

            @Override
            public Iterator<T> iterator() {
                return new PowerSetMemberIterator(seed);
            }

            private class PowerSetMemberIterator implements Iterator<T> {

                private int seedValue;
                private int position;

                public PowerSetMemberIterator(int seed) {
                    this.seedValue = seed;
                    this.position = 0;
                }

                @Override
                public boolean hasNext() {
                    return this.position < choices.size();
                }

                @Override
                public T next() {
                    int choiceSetSize = choices.get(position).size();
                    this.position++;

                    int curIdx = seedValue % choiceSetSize;
                    this.seedValue = seedValue / choiceSetSize;

                    T result = choices.get(position - 1).get(curIdx);
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();

                }
            }
        }

    }

}
