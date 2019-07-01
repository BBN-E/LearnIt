package com.bbn.akbc.neolearnit.evaluation.offsets;

public class PairOfInt implements Comparable<PairOfInt> {
	int first;
	int second;

	PairOfInt(int a, int b) {
		first = a;
		second = b;
	}

	@Override
	public int compareTo(PairOfInt o) {
		if(this.first < o.first)
			return -1;
		else if(this.first > o.first)
			return 1;
		else {
			if(this.second < o.second)
				return -1;
			else if(this.second > o.second)
				return 1;
			else
				return 0;
		}
	}


}
