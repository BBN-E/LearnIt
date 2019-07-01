package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.akbc.neolearnit.evaluation.offsets.OffsetConverter;
import com.bbn.serif.theories.SentenceTheory;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class SpanningAnswer {

	@JsonProperty
	private final String docid;
	@JsonProperty
	private final int startOffset;
	@JsonProperty
	private final int endOffset;
	@JsonProperty
	private final int sentid;
	@JsonProperty
	private final int startToken;
	@JsonProperty
	private final int endToken;

	protected SpanningAnswer(String docid,int startOffset,int endOffset,
			int sentid,int startToken,int endToken) {

		this.docid = docid;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.sentid = sentid;
		this.startToken = startToken;
		this.endToken = endToken;
	}

	public SpanningAnswer(String docid, int startOffset,
			int endOffset, OffsetConverter conv) {
		this.docid = docid;
		this.startOffset = startOffset;
		this.endOffset = endOffset;

		SentenceTheory sent = conv.getSentence(startOffset, docid);
		this.sentid = sent.index();
		this.startToken = conv.convertStartOffset(startOffset, docid, sent);
		this.endToken = conv.convertEndOffset(endOffset, docid, sent);
	}

	public String getDocid() {
		return docid;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}

	public int getSentid() {
		return sentid;
	}

	public int getStartToken() {
		return startToken;
	}

	public int getEndToken() {
		return endToken;
	}

	public abstract String getText();

	@Override
	public String toString() {
		return "SpanningAnswer [docid=" + docid + ", startOffset="
				+ startOffset + ", endOffset=" + endOffset + ", sentid="
				+ sentid + ", startToken=" + startToken + ", endToken="
				+ endToken + "]";
	}



}
