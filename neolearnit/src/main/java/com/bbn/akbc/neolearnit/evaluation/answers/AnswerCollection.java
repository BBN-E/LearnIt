package com.bbn.akbc.neolearnit.evaluation.answers;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class AnswerCollection {

	private final Multimap<EntityAnswer, MentionAnswer> coreferences;
	private final Multimap<ValueAnswer, ValueMentionAnswer> valueGroups;
	private final List<RelationAnswer> relations;

	private AnswerCollection(Multimap<EntityAnswer, MentionAnswer> coreferences,
			Multimap<ValueAnswer, ValueMentionAnswer> valueGroups,
			List<RelationAnswer> relations) {
		this.coreferences = coreferences;
		this.valueGroups = valueGroups;
		this.relations = relations;
	}

	public Multimap<EntityAnswer, MentionAnswer> getCoreferences() {
		return coreferences;
	}

	public Multimap<ValueAnswer, ValueMentionAnswer> getValueGroups() {
		return valueGroups;
	}

	public List<RelationAnswer> getRelations() {
		return relations;
	}

	public List<RelationAnswer> getRelations(String document, int sentence) {
		List<RelationAnswer> results = new ArrayList<RelationAnswer>();
		String docName = document.replace(".segment","");
		for (RelationAnswer ans : relations) {
			if (ans.getArg0().getDocid().equals(docName) && ans.getArg0().getSentid() == sentence) {
				results.add(ans);
			}
		}
		return results;
	}

	public static class Builder {

		private final ImmutableMultimap.Builder<EntityAnswer, MentionAnswer> coreferences;
		private final ImmutableMultimap.Builder<ValueAnswer, ValueMentionAnswer> valueGroups;
		private final ImmutableList.Builder<RelationAnswer> relations;

		public Builder() {
			coreferences = new ImmutableMultimap.Builder<EntityAnswer,MentionAnswer>();
			valueGroups = new ImmutableMultimap.Builder<ValueAnswer,ValueMentionAnswer>();
			relations = new ImmutableList.Builder<RelationAnswer>();
		}

		public Builder withAddedRelation(RelationAnswer relation) {
			relations.add(relation);
			return this;
		}

		public Builder withAddedCoreference(EntityAnswer entity, MentionAnswer mention) {
			coreferences.put(entity,mention);
			return this;
		}

		public Builder withAddedValueGroup(ValueAnswer value, ValueMentionAnswer mention) {
			valueGroups.put(value,mention);
			return this;
		}

		public AnswerCollection build() {
			return new AnswerCollection(coreferences.build(), valueGroups.build(), relations.build());
		}

	}

}
