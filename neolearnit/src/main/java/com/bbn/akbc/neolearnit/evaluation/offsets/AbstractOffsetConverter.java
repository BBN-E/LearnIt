package com.bbn.akbc.neolearnit.evaluation.offsets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;

public abstract class AbstractOffsetConverter implements OffsetConverter {

	private final String docExtension;

	private final String serifxmlDir;
	private final String sgmDir;
	private final Map<String,DocTheory> docsById;
	private final Map<String,String> sgmById;
	private final Parameters params;

	private final Map<String,SentenceTheory> offsetSentCache;
	private final Map<String,Integer> startOffsetCache;
	private final Map<String,Integer> endOffsetCache;

	public AbstractOffsetConverter(String serifxmlDir, String sgmDir, Parameters params, String docExtension) {
		this.serifxmlDir = serifxmlDir;
		this.sgmDir = sgmDir;
		this.params = params;
		this.docsById = new HashMap<String,DocTheory>();
		this.sgmById = new HashMap<String,String>();
		this.offsetSentCache = new HashMap<String,SentenceTheory>();
		this.startOffsetCache = new HashMap<String,Integer>();
		this.endOffsetCache = new HashMap<String,Integer>();
		this.docExtension = docExtension;
	}

	// if this is called, assuming both of them are initialized outside of this class
	AbstractOffsetConverter(Map<String,DocTheory> id2serifDoc, Map<String,String> id2sgm, String docExtension) {
		this.docsById = id2serifDoc;
		this.sgmById = id2sgm;
		this.offsetSentCache = new HashMap<String,SentenceTheory>();
		this.startOffsetCache = new HashMap<String,Integer>();
		this.endOffsetCache = new HashMap<String,Integer>();
		this.docExtension = docExtension;
		params = null;

		// feed dummy values since these are final
		this.serifxmlDir = "";
		this.sgmDir = "";
	}


	public DocTheory getDocTheory(String docname) {
		if (!docsById.containsKey(docname)) {
			try {
				DocTheory dt;
				if (params != null) {
					dt = SerifXMLLoader.createFrom(params).loadFrom(
							new File(serifxmlDir+"/"+docname.replace(".sgm", "")+docExtension));
				} else {
					dt = SerifXMLLoader.fromStandardACETypes().loadFrom(
							new File(serifxmlDir+"/"+docname.replace(".sgm", "")+docExtension));
				}
				docsById.put(docname,dt);
			} catch (IOException e) {
				System.err.println("Failed to load serifxml for "+docname);
				e.printStackTrace();
			}
		}
		return docsById.get(docname);
	}

	public String getSgmDir() {
		return sgmDir;
	}

	/*
	@Override
	public synchronized String getSgmText(String docname) {
		if (!sgmById.containsKey(docname)) {
			try {
				String docfile = docname.replace(".segment", "")+".sgm";
				FileReader reader = new FileReader(new File(sgmDir+"/"+docfile));
				StringBuilder contents = new StringBuilder();
				int ichar = reader.read();
				while (ichar != -1) {
					char c = (char)ichar;
					contents.append(c);
					ichar = reader.read();
				}
				reader.close();

				sgmById.put(docname,contents.toString());
			} catch (IOException e) {
				System.err.println("Failed to load sgm for "+docname);
				e.printStackTrace();
			}
		}
		return sgmById.get(docname);
	}
	*/

	@Override
	public synchronized String getSgmText(String docname) {
		if (!sgmById.containsKey(docname)) {
			try {
				String docfile = docname.replace(".segment", "")+".sgm";

				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sgmDir+"/"+docfile), "UTF8"));
				StringBuilder contents = new StringBuilder();
				int ichar = reader.read();
				while (ichar != -1) {
					char c = (char)ichar;
					contents.append(c);
					ichar = reader.read();
				}
				reader.close();

				sgmById.put(docname,contents.toString());
			} catch (IOException e) {
				System.err.println("Failed to load sgm for "+docname);
				e.printStackTrace();
			}
		}
		return sgmById.get(docname);
	}

	@Override
	public synchronized SentenceTheory getSentence(int offset, String docname) {
		if (offsetSentCache.containsKey(docname+offset))
			return offsetSentCache.get(docname+offset);

		DocTheory doc = getDocTheory(docname);
		for (SentenceTheory st : doc.sentenceTheories()) {
			if (st.tokenSequence().size() > 0) {
				Token tfirst = st.tokenSequence().token(0);
				Token tlast = st.tokenSequence().token(st.tokenSequence().size()-1);
				int xmlStart = convertStartToken(st.index(),
						tfirst.index(), doc.docid().toString());
				int xmlEnd = convertEndToken(st.index(),
						tlast.index(), doc.docid().toString());

				if (offset >= xmlStart && offset <= xmlEnd+1) {
					offsetSentCache.put(docname+offset,st);
					return st;
				}
				if (offset < xmlStart && offset < xmlEnd) {
					//skipped the sentence
					if (st.index() == 0) return st;
					return doc.sentenceTheory(st.index()-1);
				}
			}
		}

		System.err.println("Couldn't find sentence for " +
				"offset "+offset+" in document "+docname);
		offsetSentCache.put(docname+offset,doc.sentenceTheory(doc.sentenceTheories().size()-1));
		return doc.sentenceTheory(doc.sentenceTheories().size()-1);
	}

	@Override
	public abstract int convertStartToken(int sentidx, int tokenidx, String docname);

	@Override
	public abstract int convertEndToken(int sentidx, int tokenidx, String docname);

	@Override
	public synchronized int convertStartOffset(int offset, String docname, SentenceTheory sent) {
		String id = docname+"--"+sent.index()+"--"+offset;
		if (startOffsetCache.containsKey(id)) {
			return startOffsetCache.get(id);
		}

		for (Token t : sent.tokenSequence()) {
			if (convertStartToken(sent.index(),t.index(),docname) >= offset) {
				startOffsetCache.put(id, t.index());
				return t.index();
			}
		}
		Token last = sent.tokenSequence().token(sent.tokenSequence().size()-1);
		System.err.println("Could not find token bounds, returning last token");
		startOffsetCache.put(id, last.index());
		return last.index();
	}

	@Override
	public synchronized int convertEndOffset(int offset, String docname, SentenceTheory sent) {
		String id = docname+"--"+sent.index()+"--"+offset;
		if (endOffsetCache.containsKey(id)) {
			return endOffsetCache.get(id);
		}

		for (Token t : sent.tokenSequence()) {
			if (convertEndToken(sent.index(),t.index(),docname) >= offset) {
				endOffsetCache.put(id, t.index());
				return t.index();
			}
		}
		Token last = sent.tokenSequence().token(sent.tokenSequence().size()-1);
		System.err.println("Could not find token bounds, returning last token");
		endOffsetCache.put(id, last.index());
		return last.index();
	}
}
