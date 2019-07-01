package com.bbn.akbc.neolearnit.evaluation.offsets;

import com.bbn.serif.theories.SentenceTheory;

public interface OffsetConverter {

	public String getSgmText(String docname);
	public int convertStartToken(int sentidx, int tokenidx, String docname);
	public int convertEndToken(int sentidx, int tokenidx, String docname);
	public SentenceTheory getSentence(int offset, String docname);
	public int convertStartOffset(int offset, String docname, SentenceTheory sent);
	public int convertEndOffset(int offset, String docname, SentenceTheory sent);
}
