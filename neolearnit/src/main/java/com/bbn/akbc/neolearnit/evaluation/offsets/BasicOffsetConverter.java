package com.bbn.akbc.neolearnit.evaluation.offsets;

import java.util.Map;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

public class BasicOffsetConverter extends AbstractOffsetConverter {

	private final boolean useEDT;

	public BasicOffsetConverter(String serifxmlDir, Parameters params) {
		this(serifxmlDir, params, true);
	}

	public BasicOffsetConverter(String serifxmlDir, Parameters params, boolean useEDT) {
		super(serifxmlDir, "", params,".xml");
		this.useEDT = useEDT;
	}

	// if this is called, assuming all of three arguments are fully initialized outside of this class
	public BasicOffsetConverter(Map<String,DocTheory> docsById, Map<String,String> sgmById) {
		this(docsById, sgmById,true);
	}

	// if this is called, assuming all of three arguments are fully initialized outside of this class
	public BasicOffsetConverter(Map<String,DocTheory> docsById, Map<String,String> sgmById, boolean useEDT) {
		super(docsById, sgmById,".xml");
		this.useEDT = useEDT;
	}

	@Override
	public int convertStartToken(int sentidx, int tokenidx, String docname) {
		DocTheory doc = getDocTheory(docname);
		SentenceTheory sent = doc.sentenceTheory(sentidx);
		if (useEDT) {
			return sent.tokenSequence().token(tokenidx).startEDTOffset().value();
		} else {
			return sent.tokenSequence().token(tokenidx).startCharOffset().value();
		}
	}

	@Override
	public int convertEndToken(int sentidx, int tokenidx, String docname) {
		DocTheory doc = getDocTheory(docname);
		SentenceTheory sent = doc.sentenceTheory(sentidx);
		if (useEDT) {
			return sent.tokenSequence().token(tokenidx).endEDTOffset().value();
		} else {
			return sent.tokenSequence().token(tokenidx).endCharOffset().value();
		}
	}




}
