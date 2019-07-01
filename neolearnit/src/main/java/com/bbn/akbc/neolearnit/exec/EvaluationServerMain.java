package com.bbn.akbc.neolearnit.exec;

import java.io.File;
import java.util.List;

import org.eclipse.jetty.server.Handler;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.EvaluationHandler;
import com.bbn.akbc.neolearnit.server.handlers.MainHandler;
import com.google.common.collect.ImmutableList;

public class EvaluationServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
		LearnItConfig.loadParams(new File(params));
		int port = Integer.parseInt(args[1]);
		List<Handler> handlers = ImmutableList.<Handler>of(new MainHandler(), new EvaluationHandler());

		new SimpleServer(handlers, "index.html", port).run();
	}
}
