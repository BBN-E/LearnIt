package com.bbn.akbc.neolearnit.evaluation.offsets;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.apf.APFDocument;
import com.bbn.serif.apf.APFEntity;
import com.bbn.serif.apf.APFEntityMention;
import com.bbn.serif.apf.APFLoader;
import com.bbn.serif.apf.APFSourceFile;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BilingualChineseOffsetConverter extends AbstractOffsetConverter {

	private final Map<String,SentOffSetBuilder> builderCache;

	public BilingualChineseOffsetConverter(String serifxmlDir, String sgmDir, Parameters params, String extension) {
		super(serifxmlDir,sgmDir, params,extension);
		builderCache = new HashMap<String,SentOffSetBuilder>();
	}

	public BilingualChineseOffsetConverter(String serifxmlDir, String sgmDir, Parameters params) {
		super(serifxmlDir,sgmDir, params, ".segment.xml");
		builderCache = new HashMap<String,SentOffSetBuilder>();
	}

	// if this is called, assuming all of three arguments are fully initialized outside of this class
	public BilingualChineseOffsetConverter(Map<String,DocTheory> docsById, Map<String,String> sgmById,
			Map<String, SentOffSetBuilder> sentOffsetBuilderById) {
		super(docsById, sgmById,".segment.xml");
		builderCache = sentOffsetBuilderById;
	}

	public SentOffSetBuilder getBuilder(String docname) {
		String docString = docname.replace(".segment", "");
		if (!builderCache.containsKey(docString)) {
			String sgmDir = getSgmDir();
			String fileSgm = sgmDir+"/"+docString+".sgm";
			String fileSegment = sgmDir.replace("rawtext", "segments")+"/"+docString+".segment";
			SentOffSetBuilder builder;
			try {
				builder = new SentOffSetBuilder(fileSgm, fileSegment);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not make builder from "+fileSgm+" and "+fileSegment);
			}
			builderCache.put(docString, builder);
		}
		return builderCache.get(docString);
	}

	@Override
	public int convertStartToken(int sentidx, int tokenidx, String docname) {
		return getBuilder(docname).getTokenStartOffset(sentidx, tokenidx);
	}



	@Override
	public int convertEndToken(int sentidx, int tokenidx, String docname) {
		return getBuilder(docname).getTokenEndOffset(sentidx, tokenidx);
	}

	public String getDebugStringBuilder(int sentIdx, String docName, List<String> listSegmentSerifTokens) {
		return getBuilder(docName).getDebugString(sentIdx, listSegmentSerifTokens);
	}

	public static void main(String[] args) {
		BilingualChineseOffsetConverter conv = new BilingualChineseOffsetConverter(
				"\\\\mercury-04\\u41\\ChineseACE\\serifxml\\chinese",
				"\\\\mercury-04\\u41\\ChineseACE\\rawtext", null);
		String doc = "CTS20001122.1300.0271.segment";

		//String sgmText = conv.getSgmText(doc);
		String sgmText = conv.getBuilder(doc).eraseXMLRaw(
				new StringBuffer(conv.getSgmText(doc)));

		DocTheory dt = conv.getDocTheory(doc);
		for (SentenceTheory st : dt.sentenceTheories()) {
			for (Mention m : st.mentions()) {
				int sent = m.span().sentenceIndex();
				int start = m.span().startIndex();
				int end = m.span().endIndex();

				int offstart = conv.convertStartToken(sent,start,doc);
				int offend = conv.convertEndToken(sent,end,doc);
				String mentStr = sgmText.substring(offstart, offend+1);

				System.out.println(sent+":"+start+":"+end+"\t\t"+
						offstart+":"+offend+"\t\t"+
						m.span().tokenizedText()+"\t\t"+mentStr);
			}
		}

		System.out.println("\n\nTHE OTHER WAY\n\n");

		try {
			APFSourceFile apf = new APFLoader().loadFrom(new File(
					"\\\\mercury-04\\u41\\ChineseACE\\apf\\CTS20001122.1300.0271.apf.xml"));

			for (APFDocument d : apf.documents()) {
				for (APFEntity e : d.getEntities()) {
					for (APFEntityMention m : e.getMentions()) {
						int offstart = m.getExtent().getStart();
						int offend = m.getExtent().getEnd();

						SentenceTheory sent = conv.getSentence(offstart, doc);
						int start = conv.convertStartOffset(offstart, doc, sent);
						int end = conv.convertEndOffset(offend, doc, sent);


						List<String> span = new ArrayList<String>();
						for (Token t : sent.tokenSequence()) {
							if (t.index() >= start && t.index() <= end) {
								span.add(t.tokenizedText().utf16CodeUnits());
							}
						}

						String mentStr = StringUtils.SpaceJoin.apply(span);

						System.out.println(offstart+":"+offend+"\t\t"+
								sent.index()+":"+start+":"+end+"\t\t"+
								m.getExtent().getText()+"\t\t"+mentStr);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
