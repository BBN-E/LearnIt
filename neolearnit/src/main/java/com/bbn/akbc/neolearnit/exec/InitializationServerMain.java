package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnItPatternFactory;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.initialization.InitializationPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.InitializationHandler;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Set;

public class InitializationServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
		String mappingsFile = args[1];
		String extractorFilename = args[2];
		int iter = Integer.parseInt(args[3]);
		int port = Integer.parseInt(args[4]);

//        Set<String> keywords = args.length > 5 && new File(args[5]).exists() ? readKeywords(new File(args[5])) : ImmutableSet.<String>of();

		LearnItConfig.loadParams(new File(params));
        TargetAndScoreTables data = TargetAndScoreTables.deserialize(new File(extractorFilename));

        Set<InitializationPattern> initPatterns = ImmutableSet.of();
        if (LearnItConfig.optionalParamTrue("use_patterns_to_initialize") && args.length > 5 && new File(args[5]).exists())
            initPatterns = InitializationPattern.getFromFile(new File(args[5]), data.getTarget());

        // read-in even more additional patterns in Sexp format
        Set<LearnitPattern> initPatternsInSexpFormat = ImmutableSet.of();
        if (LearnItConfig.optionalParamTrue("use_patterns_to_initialize") && args.length > 6 && new File(args[6]).exists())
        	initPatternsInSexpFormat = LearnItPatternFactory.fromFile(new File(args[6]), data.getTarget());
        //


		SeedSimilarity.load(data);

		System.out.println("loading mappings...");

		Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);

		System.out.println("starting server...");
		InitializationHandler handler = new InitializationHandler(mappings,data,extractorFilename,iter);

		if (data.getPatternScores().getFrozen().isEmpty()) {
            if (LearnItConfig.optionalParamTrue("use_patterns_to_initialize")) {
                System.out.println("Fetching an ranking patterns based on initialization patterns...");
//              handler.proposeFromInitializationPatterns(data.getTarget().getName(), initPatterns);
                handler.proposeFromInitializationPatterns(data.getTarget().getName(), initPatterns, initPatternsInSexpFormat);
            } else {
                System.out.println("proposing initial patterns...");
                handler.proposeFirstPatterns(data.getTarget().getName());
            }
        }

		new SimpleServer(handler, "html/initialization.html", port)
			.withIntroMessage("Running relation "+handler.getRelation()+" on port "+port)
			.run();
	}
}
