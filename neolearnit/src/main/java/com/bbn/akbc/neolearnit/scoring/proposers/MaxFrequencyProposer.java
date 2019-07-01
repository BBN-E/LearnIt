package com.bbn.akbc.neolearnit.scoring.proposers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.processing.PartialInfoWithCounts;
import com.google.common.collect.Lists;

public class MaxFrequencyProposer<T extends LearnItObservation> implements Proposer<T> {

	private final PartialInfoWithCounts<T> info;

	public MaxFrequencyProposer(PartialInfoWithCounts<T> info) {
		this.info = info;
	}

	@Override
	public Iterable<T> propose(Iterable<T> potentials, int amount) {

		List<T> result = Lists.newArrayList(potentials);

		Collections.sort(result, new Comparator<T>() {

			@Override
			public int compare(T arg0, T arg1) {

				int comparison = -Double.compare(info.getCount(arg0), info.getCount(arg1));

				if (comparison == 0) {
					return arg0.toPrettyString().compareTo(arg1.toPrettyString());
				} else {
					return comparison;
				}

			}

		});

		result = result.subList(0, Math.min(result.size(), amount));

		System.out.println("Largest count: ");
		for (T obj : result) {
			System.out.println(obj.toPrettyString()+" with frequency "+info.getCount(obj));
		}

		return result;
	}

}
