package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.akbc.neolearnit.evaluation.pelf.ElfIndividualMention;
import com.bbn.serif.apf.APFEntityMention;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MentionAnswer extends SpanningAnswer {

	@JsonProperty
	private final String mentionId;
	@JsonProperty
	private final EntityAnswer entity;
	@JsonProperty
	private final String text;

	@JsonCreator
	private MentionAnswer(
			@JsonProperty("docid") String docid,
			@JsonProperty("startOffset") int startOffset,
			@JsonProperty("endOffset") int endOffset,
			@JsonProperty("sentid") int sentid,
			@JsonProperty("startToken") int startToken,
			@JsonProperty("endToken") int endToken,
			@JsonProperty("mentionId") String mentionId,
			@JsonProperty("text") String text,
			@JsonProperty("entity") EntityAnswer entity) {

		super(docid,startOffset,endOffset,sentid,startToken,endToken);
		this.mentionId = mentionId;
		this.entity = entity;
		this.text = text;
	}

	public MentionAnswer(String docid, int startOffset,
			int endOffset, OffsetConverter conv,
			String mentionId, EntityAnswer entity,
			String text) {
		super(docid, startOffset, endOffset, conv);
		this.mentionId = mentionId;
		this.entity = entity;
		this.text = text;
	}

	public EntityAnswer getEntity() {
		return entity;
	}

	public String getMentionId() {
		return mentionId;
	}

	@Override
	public String getText() {
		return text;
	}

	public static MentionAnswer fromAPFMention(String docid, OffsetConverter conv, APFEntityMention mention, EntityAnswer entity) {
		return new MentionAnswer(docid, mention.getHead().start, mention.getHead().end, conv, mention.getID(), entity, mention.getHead().text);
	}

	public static MentionAnswer fromElfIndividualMention(String docid, OffsetConverter conv, ElfIndividualMention mention, EntityAnswer entity) {
		String mid = entity.getEntityId()+":"+mention.getStart()+"-"+mention.getEnd();
		return new MentionAnswer(docid, mention.getStart(), mention.getEnd(), conv, mid, entity, mention.getText());
	}

	@Override
	public String toString() {
		return "MentionAnswer [mentionId=" + mentionId + ", entity=" + entity
				+ ", text=" + text
				+ ", getDocid()=" + getDocid() + ", getStartOffset()="
				+ getStartOffset() + ", getEndOffset()=" + getEndOffset()
				+ ", getSentid()=" + getSentid() + ", getStartToken()="
				+ getStartToken() + ", getEndToken()=" + getEndToken() + "]";
	}

}
