package com.bbn.akbc.neolearnit.mappings.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * This one is a little different from the others so I called it just a map
 * it's a one-way map from InstanceId to MatchInfo. MatchInfo isn't hashable
 * and we don't really need to store it every which way
 * @author mshafir
 *
 */
public class InstanceToMatchInfoMap {

	private final Map<InstanceIdentifier,MatchInfo> map;

	public InstanceToMatchInfoMap(Map<InstanceIdentifier,MatchInfo> map) {
		this.map = ImmutableMap.copyOf(map);
	}

	public MatchInfo getMatchInfo(InstanceIdentifier instanceId) {
		return map.get(instanceId);
	}

	public SentenceTheory getSentenceTheory(String docid, int sentence) {
		for (InstanceIdentifier id : map.keySet()) {
			if (id.getDocid().equals(docid)) {
				return map.get(id).getPrimaryLanguageMatch().getDocTheory().sentenceTheory(sentence);
			}
		}
		System.out.println("Couldn't find an instance from document "+docid);
		return null;
	}

	public Collection<Spanning> getSpanningsInSentence(String docid, int sentence) {
		Set<Spanning> result = new HashSet<Spanning>();
		for (InstanceIdentifier id : map.keySet()) {
			if (id.getDocid().replace(".segment", "").equals(docid) && id.getSentid() == sentence) {
				if(map.get(id).getPrimaryLanguageMatch().getSlot0().isPresent()) {
					result.add(map.get(id).getPrimaryLanguageMatch().getSlot0().get());
				}
				if(map.get(id).getPrimaryLanguageMatch().getSlot1().isPresent()) {
					result.add(map.get(id).getPrimaryLanguageMatch().getSlot1().get());
				}
			}
		}
		return result;
	}

	public static class Builder implements Recorder<InstanceIdentifier, MatchInfo> {

		private final Map<InstanceIdentifier, MatchInfo> map;

		@Inject
		public Builder() {
			map = new HashMap<InstanceIdentifier, MatchInfo>();
		}

		@Override
		public synchronized void record(InstanceIdentifier item, MatchInfo property) {
			map.put(item,property);
		}

		public InstanceToMatchInfoMap build() {
			return new InstanceToMatchInfoMap(map);
		}
	}

}
