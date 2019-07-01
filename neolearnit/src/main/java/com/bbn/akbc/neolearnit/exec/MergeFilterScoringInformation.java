package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.processing.PartialInformation;
import com.bbn.akbc.neolearnit.processing.Stage;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.bue.common.files.FileUtils;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MergeFilterScoringInformation {

	/**
	 * This stage performs the top level map-reduce scoring.
	 *
	 *
	 *      MAP STEP         REDUCE STEP        RUN_LEARNIT STEP
	 *
	 * Mappings --> PartialScore -\
	 *                             \
	 * Mappings --> PartialScore ----> PartialScore --> Stage Procedure
	 *                             /
	 * Mappings --> PartialScore -/
	 *
	 * Each stage knows how to handle its own map/reduce functionality.
	 * The only important decision is whether you are
	 * doing a map+reduce (level = 1) or just a reduce (level > 1).
	 *
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException {
		//String
		String params = args[0];
		String inputlist = args[1];
		String output = args[2];
		String extractor = args[3];
		Integer level = Integer.parseInt(args[4]);

		// Optional<String> extractor_out = Optional.absent();
		// if(args.length>5)
		//	 extractor_out = Optional.of(args[5]);

		LearnItConfig.loadParams(new File(params));


		TargetAndScoreTables data = TargetAndScoreTables.deserialize(new File(extractor));
		boolean doMap = level == 1;
        ImmutableList<File> filelist = FileUtils.loadFileList(new File(inputlist));

		Stage<?> stage = data.getStage();
		if (doMap) {

			StorageUtils.serialize(new File(output), stage.processMappingFiles(filelist), true);

		} else if (level == 3) {

			//level 3 means run the stage right away
			processAndRun(stage, filelist);

		} else {

			StorageUtils.serialize(new File(output), stage.processPartialInfoFiles(filelist), true);

			// save initialized tp/tn/fp/fn
			//             if(extractor_out.isPresent())
			//                     data.serialize(new File(extractor_out.get()));
		}
	}

    public static <T extends PartialInformation> void processAndRun(Stage<T> stage, ImmutableList<File> filelist) {
		stage.runStage(stage.processPartialInfoFiles(filelist));
	}
}
