package com.bbn.akbc.neolearnit.scoring.proposers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.processing.PartialInfoWithCounts;

public class AverageFrequencyProposer<T extends LearnItObservation> implements Proposer<T> {

	private final PartialInfoWithCounts<T> info;

	public AverageFrequencyProposer(PartialInfoWithCounts<T> info) {
		this.info = info;
	}

	@Override
	public Iterable<T> propose(Iterable<T> potentials, int amount) {

		List<T> result = new ArrayList<T>();

		int total=0;
		for (T obj : potentials) {
			total += info.getCount(obj);
			result.add(obj);
		}
		final double freq = (double)total/result.size();
		System.out.println("Average Frequency: "+freq);

		Collections.sort(result, new Comparator<T>() {

			@Override
			public int compare(T arg0, T arg1) {

				Double a0Dev = Math.abs(freq - info.getCount(arg0));
				Double a1Dev = Math.abs(freq - info.getCount(arg1));

				return Double.compare(a0Dev, a1Dev);
			}

		});

		result = result.subList(0, Math.min(result.size(), amount));

		System.out.println("Closest to average: ");
		for (T obj : result) {
			System.out.println(obj.toPrettyString()+" with frequency "+info.getCount(obj));
		}

		return result;
	}

}
