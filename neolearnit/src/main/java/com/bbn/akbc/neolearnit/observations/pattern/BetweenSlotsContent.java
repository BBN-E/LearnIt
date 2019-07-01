package com.bbn.akbc.neolearnit.observations.pattern;

import java.util.ArrayList;
import java.util.List;

import com.bbn.bue.common.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class BetweenSlotsContent<ContentType extends RegexableContent> {
	final ImmutableList<ContentType> betweenSlotsContent;
	static int PENALTY_AFTER = 4;
	static float PENALTY_AMOUNT = 0.1F;

	public BetweenSlotsContent(Iterable<ContentType> content) {
		betweenSlotsContent = ImmutableList.copyOf(content);
	}

	@JsonProperty
	private List<ContentType> content() {
		return new ArrayList<ContentType>(this.betweenSlotsContent);
	}

	public ImmutableList<ContentType> getContent() {
		return this.betweenSlotsContent;
	}

	@JsonCreator
	private static <ContentType extends RegexableContent> BetweenSlotsContent<ContentType> from(
			@JsonProperty("content") List<ContentType> content) {
		return new BetweenSlotsContent<ContentType>(content);
	}


	public static class Builder<ContentType extends RegexableContent> {
		private final ImmutableList.Builder<ContentType> betweenSlotsContentBuilder;

		public Builder() {
			this.betweenSlotsContentBuilder = new ImmutableList.Builder<ContentType>();
		}

		public BetweenSlotsContent<ContentType> build() {
			return new BetweenSlotsContent<ContentType>(this.betweenSlotsContentBuilder.build());
		}

		public Builder<ContentType> withAddContent(ContentType content) {
			betweenSlotsContentBuilder.add(content);
			return this;
		}

	}

	@Override
	public String toString() {
		return "{"+StringUtils.SemicolonSpaceJoin.apply(this.betweenSlotsContent)+"}";
	}

	public String toPrettyString() {
		List<String> prettyContent = new ArrayList<String>();
		for (ContentType content : this.betweenSlotsContent) {
			prettyContent.add(content.toPrettyString());
		}
		return StringUtils.SpaceJoin.apply(prettyContent);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((betweenSlotsContent == null) ? 0 : betweenSlotsContent
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		BetweenSlotsContent other = (BetweenSlotsContent) obj;
		if (betweenSlotsContent == null) {
			if (other.betweenSlotsContent != null)
				return false;
		} else if (!betweenSlotsContent.equals(other.betweenSlotsContent))
			return false;
		return true;
	}


}
