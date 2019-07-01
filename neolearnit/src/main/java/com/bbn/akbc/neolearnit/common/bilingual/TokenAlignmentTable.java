package com.bbn.akbc.neolearnit.common.bilingual;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

public class TokenAlignmentTable {
	private final ImmutableMap<String,ImmutableMap<String,ImmutableMultimap<Integer,Integer>>> table;

	private TokenAlignmentTable(ImmutableMap<String,ImmutableMap<String,ImmutableMultimap<Integer,Integer>>> table) {
		this.table = ImmutableMap.copyOf(table);
	}

	public ImmutableCollection<Integer> getAlignedTokens(
			String source, String target, Integer sourceTokenIdx) {

		if (this.table.containsKey(source) &&
				this.table.get(source).containsKey(target) &&
				this.table.get(source).get(target).containsKey(sourceTokenIdx)) {
			return this.table.get(source).get(target).get(sourceTokenIdx);
		} else {
			if (!this.table.containsKey(source) ||
				!this.table.get(source).containsKey(target)) {
					throw new RuntimeException("Could not align from "+source+" to "+target+
							" options are "+this.table.keySet());
			}
			return new ImmutableSet.Builder<Integer>().build();
		}
	}

	public Set<String> getLanguages() {
		return this.table.keySet();
	}

	public static class Builder {
		private final Map<String,Map<String,ImmutableMultimap.Builder<Integer,Integer>>> table;

		public Builder() {
			table = new HashMap<String,Map<String,ImmutableMultimap.Builder<Integer,Integer>>>();
		}

		public TokenAlignmentTable build() {
			ImmutableMap.Builder<String,ImmutableMap<String,ImmutableMultimap<Integer,Integer>>> sourceBuilder =
					new ImmutableMap.Builder<String,ImmutableMap<String,ImmutableMultimap<Integer,Integer>>>();

			for (String source : table.keySet()) {
				ImmutableMap.Builder<String, ImmutableMultimap<Integer,Integer>> targetBuilder = new
						ImmutableMap.Builder<String, ImmutableMultimap<Integer,Integer>>();

				for (String target : table.get(source).keySet()) {
					targetBuilder.put(target,table.get(source).get(target).build());
				}
				sourceBuilder.put(source,targetBuilder.build());
			}
			return new TokenAlignmentTable(sourceBuilder.build());
		}

		public void addAlignment(String source, String target, Integer sourceIdx, Integer targetIdx) {
			if (!table.containsKey(source)) {
				table.put(source, new HashMap<String,ImmutableMultimap.Builder<Integer,Integer>>());
			}
			if (!table.containsKey(target)) {
				table.put(target, new HashMap<String,ImmutableMultimap.Builder<Integer,Integer>>());
			}
			if (!table.get(source).containsKey(target)) {
				table.get(source).put(target, new ImmutableMultimap.Builder<Integer,Integer>());
			}
			if (!table.get(target).containsKey(source)) {
				table.get(target).put(source, new ImmutableMultimap.Builder<Integer,Integer>());
			}
			table.get(source).get(target).put(sourceIdx,targetIdx);
			table.get(target).get(source).put(targetIdx,sourceIdx);
		}

		public void addMultiAlignment(String source, String target,
				Integer sourceIdx, Iterable<Integer> targetIdx) {

			for (Integer i : targetIdx) {
				addAlignment(source,target,sourceIdx,i);
			}
		}

	}

	@Override
	public String toString() {
		return "TokenAlignmentTable [table=" + table + "]";
	}
}
