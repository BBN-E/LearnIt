package com.bbn.akbc.neolearnit.server.handlers;

import com.bbn.akbc.neolearnit.common.matchinfo.DocQueryDisplay;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QueryFinderHandler extends SimpleJSONHandler {

//    private final String query;
//    private final String targetName;
//    private final int slot;
    private final String question;
	private final List<DocQueryDisplay> docs;
    private final String output;

    @SuppressWarnings("unchecked")
	public QueryFinderHandler(String question, File queryFile, String output) throws IOException {
//        this.targetName = targetName;
//        this.query = query;
//        this.slot = slot;
        this.question = question;
		this.docs = StorageUtils.deserialize(queryFile, ArrayList.class, false);
        this.output = output;
	}

	@JettyMethod("/query/shutdown")
	public String shutdown(@JettyArg("entities") String[] entities) throws IOException {
        File out = new File(output);
        if (!out.getParentFile().exists())
            out.getParentFile().mkdirs();

        FileWriter fw = new FileWriter(out);
        for (String entity : entities) {
            fw.write(entity+"\n");
        }
        fw.close();

        Runtime.getRuntime().exit(0);
		return "success";
	}

//    @JettyMethod("/query/get_target_name")
//    public String getTargetName() {
//        return targetName;
//    }
//
//    @JettyMethod("/query/get_query_term")
//    public String getQueryTerm() {
//        return query;
//    }
//
//    @JettyMethod("/query/get_query_slot")
//    public int getSlot() {
//        return slot;
//    }

    @JettyMethod("/query/get_question")
    public String getQuestion() {
        return question;
    }

    @JettyMethod("/query/get_docs")
    public List<DocQueryDisplay> tryGetDoc() throws IOException {
        return this.docs;
    }

}
