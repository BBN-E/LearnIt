package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.processing.Stage;
import com.bbn.akbc.neolearnit.processing.postpruning.PostPruner;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.File;
import java.io.IOException;

public class RunLearnitMain {

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		String paramsFile = args[0];
		String scoringInfoFilename = args[1];
		String targetAndScoreFile = args[2];
		String targetAndScoreFileOutput = args[3];
		int port = args.length > 4 ? Integer.parseInt(args[4]) : 8080;

		LearnItConfig.loadParams(new File(paramsFile));

		final TargetAndScoreTables data = TargetAndScoreTables.deserialize(new File(targetAndScoreFile));

		Stage<?> stage = data.getStage();
		if (stage instanceof PostPruner) {
			((PostPruner)stage).setPort(port);
		}

		stage.runOnFile(new File(scoringInfoFilename));
		data.incrementStage();

		data.serialize(new File(targetAndScoreFileOutput));
	}

}
