package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.InitializationHandler;

import java.io.File;

public class InitializationPatternWritingServerMain {

  public static void main(String[] args) throws Exception {
    String params = args[0];
    String mappingsFile = args[1];
//		String targetPath = args[2];
//		String extractorFilename = args[3];
    String outputDirOrJsonFile = args[2];
    int port = Integer.parseInt(args[3]);
//		int iter = 0;

    double subsampling_ratio = 1.0;
    if (args.length > 4) {
      subsampling_ratio = Double.parseDouble(args[4]);
    }

    LearnItConfig.loadParams(new File(params));
//      TargetAndScoreTables data = new TargetAndScoreTables(targetPath);

//		SeedSimilarity.load(data);

    System.out.println("loading mappings...");

    Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);

    System.out.println("starting server...");
//		InitializationHandler handler = new InitializationHandler(mappings,data,extractorFilename,iter);
    InitializationHandler handler =
        new InitializationHandler(mappings, outputDirOrJsonFile, subsampling_ratio);
//		InitializationHandler handler = new InitializationHandler(mappings, outputDirOrJsonFile);

    new SimpleServer(handler, "html/initialization.html", port)
        .withIntroMessage("Running Pattern Writing Server on port " + port)
        .run();
  }
}
