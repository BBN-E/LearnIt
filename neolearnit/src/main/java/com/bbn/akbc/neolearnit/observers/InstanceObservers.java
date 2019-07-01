package com.bbn.akbc.neolearnit.observers;

import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;

public class InstanceObservers implements Iterable<Observer<MatchInfo>> {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	private final Iterable<Observer<MatchInfo>> observers;

	public InstanceObservers(Iterable<Observer<MatchInfo>> observers) {
		this.observers = ImmutableList.copyOf(observers);
	}

	@Override
	public Iterator<Observer<MatchInfo>> iterator() {
		return observers.iterator();
	}

	public static class Builder {

		private final ImmutableList.Builder<Observer<MatchInfo>> observerBuilder;

		public Builder() {
			observerBuilder = new ImmutableList.Builder<Observer<MatchInfo>>();
		}

		public Builder withObserver(Observer<MatchInfo> observer) {
			observerBuilder.add(observer);
			return this;
		}

		public InstanceObservers build() {
			return new InstanceObservers(observerBuilder.build());
		}

	}

}
