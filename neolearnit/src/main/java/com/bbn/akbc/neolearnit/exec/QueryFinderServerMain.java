package com.bbn.akbc.neolearnit.exec;

import com.bbn.bue.common.StringUtils;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.QueryFinderHandler;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.util.Arrays;

public class QueryFinderServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
        String queryFile = args[1];
        String targetName = args[2];
        String questionFile = args[3];
        String output = args[4];
		int port = Integer.parseInt(args[5]);

		LearnItConfig.loadParams(new File(params));

        String queryString = queryFile.substring(queryFile.lastIndexOf("/")+1,queryFile.lastIndexOf("."));
        String query = StringUtils.SpaceJoin.apply(Arrays.asList(queryString.substring(2).split("_")));
        int slot = Integer.parseInt(queryString.substring(0,1));

        String question = targetName + ", Slot: " + slot + ", \"" + query + "\"";
        if (new File(questionFile).exists()) {
            for (String line : Files.readLines(new File(questionFile), Charsets.UTF_8)) {
                String[] items = line.split("\t");
                if (items.length == 2) {
                    if (Integer.parseInt(items[0]) == slot) {
                        question = String.format("%s: " + items[1], targetName, query);
                        break;
                    }
                }
            }
        }

		System.out.println("starting server...");

		QueryFinderHandler handler = new QueryFinderHandler(question, new File(queryFile), output);

		new SimpleServer(handler, "html/queries.html", port)
			.withIntroMessage("Running query \"" + slot + ": " + query + "\" on port " + port)
			.run();
	}
}
