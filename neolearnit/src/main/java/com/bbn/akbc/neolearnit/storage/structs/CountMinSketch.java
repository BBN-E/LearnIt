package com.bbn.akbc.neolearnit.storage.structs;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class CountMinSketch {

	private static final List<HashFunction> hashFunctions = Lists.newArrayList(
			Hashing.md5(), Hashing.sha1(), Hashing.sha256(), Hashing.murmur3_32(123), Hashing.murmur3_128(12345)) ;

	private final List<Multiset<Integer>> data;

	public CountMinSketch() {
		this.data = new ArrayList<Multiset<Integer>>();
		for (int i=0;i<hashFunctions.size();i++) {
			this.data.add(HashMultiset.<Integer>create());
		}
	}

	private int hash(String text, int hashFunction) {
		return hashFunctions.get(hashFunction).hashString(text, Charset.defaultCharset()).asInt();
	}

	private void add(String value, int count, Multiset<Integer> subdata, int hashId) {
		subdata.add(hash(value, hashId), count);
	}

	public void add(String value, int count) {
		for (int i=0;i<hashFunctions.size();i++) {
			add(value, count, data.get(i), i);
		}
	}

	public void add(String value) {
		add(value, 1);
	}

	public int count(String value) {
		int min = Integer.MAX_VALUE;
		for (int i=0;i<hashFunctions.size();i++) {
			int val = data.get(i).count(hash(value,i));
			if (val < min) {
				min = val;
			}
		}
		return min;

	}

}
