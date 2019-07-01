package com.bbn.akbc.neolearnit.mappings.impl;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.AbstractMapping;
import com.bbn.akbc.neolearnit.mappings.AbstractMappingRecorder;
import com.bbn.akbc.neolearnit.observations.evaluation.EvalAnswer;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class InstanceToAnswerMapping extends AbstractMapping<InstanceIdentifier, EvalAnswer> {

		public InstanceToAnswerMapping(MapStorage<InstanceIdentifier, EvalAnswer> storage) {
			super(storage);
		}

		public EvalAnswer getAnswer(InstanceIdentifier id) {
			if (storage.getRight(id).size() != 1) throw new RuntimeException("Incorrect number of answers for instance "+id);
			return storage.getRight(id).iterator().next();
		}

		public Collection<InstanceIdentifier> getInstances(EvalAnswer ans) {
			return storage.getLeft(ans);
		}

		public Multiset<InstanceIdentifier> getAllInstances() {
			return storage.getLefts();
		}

		public Set<EvalAnswer> getAllAnswers() {
			return storage.getRights().elementSet();
		}

		public Set<EvalAnswer> getGoldAnswers() {
			Set<EvalAnswer> golds = new HashSet<EvalAnswer>();
			for (EvalAnswer ans : getAllAnswers()) {
				if (ans.isCorrect()) {
					golds.add(ans);
				}
			}
			return golds;
		}

		public static class Builder extends AbstractMappingRecorder<InstanceIdentifier, EvalAnswer> {

			@Inject
			public Builder(MapStorage.Builder<InstanceIdentifier, EvalAnswer> storage) {
				super(storage);
			}

			public InstanceToAnswerMapping build() {
				return new InstanceToAnswerMapping(this.buildMapping());
			}
		}

	}
