package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.serif.apf.APFValueMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueMentionAnswer extends SpanningAnswer {

	@JsonProperty
	private final String valueMentionId;
	@JsonProperty
	private final ValueAnswer valueGroup;
	@JsonProperty
	private final String text;

	@JsonCreator
	private ValueMentionAnswer(
			@JsonProperty("docid") String docid,
			@JsonProperty("startOffset") int startOffset,
			@JsonProperty("endOffset") int endOffset,
			@JsonProperty("sentid") int sentid,
			@JsonProperty("startToken") int startToken,
			@JsonProperty("endToken") int endToken,
			@JsonProperty("valueMentionId") String valueMentionId,
			@JsonProperty("text") String text,
			@JsonProperty("valueGroup") ValueAnswer valueGroup) {

		super(docid,startOffset,endOffset,sentid,startToken,endToken);
		this.valueMentionId = valueMentionId;
		this.valueGroup = valueGroup;
		this.text = text;
	}

	public ValueMentionAnswer(String docid, int startOffset,
			int endOffset, OffsetConverter conv,
			String valueMentionId, ValueAnswer valueGroup, String text) {
		super(docid, startOffset, endOffset, conv);
		this.valueMentionId = valueMentionId;
		this.valueGroup = valueGroup;
		this.text = text;
	}

	public ValueAnswer getValueGroup() {
		return valueGroup;
	}

	public String getValueMentionId() {
		return valueMentionId;
	}

	@Override
	public String getText() {
		return text;
	}

	public static ValueMentionAnswer fromAPFValueMention(String docid, OffsetConverter conv, APFValueMention mention, ValueAnswer entity) {
		return new ValueMentionAnswer(docid, mention.getHead().start, mention.getHead().end, conv, mention.getID(), entity, mention.getExtent().text);
	}

	@Override
	public String toString() {
		return "ValueMentionAnswer [valueMentionId=" + valueMentionId
				+ ", valueGroup=" + valueGroup + ", getDocid()=" + getDocid()
				+ ", getStartOffset()=" + getStartOffset()
				+ ", getEndOffset()=" + getEndOffset() + ", getSentid()="
				+ getSentid() + ", getStartToken()=" + getStartToken()
				+ ", getEndToken()=" + getEndToken() + "]";
	}
}
