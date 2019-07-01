package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;

import java.io.File;
import java.io.IOException;

public class UpdateStage {

	public static void main(String[] args) throws IOException {
		String params = args[0];
		String extractor = args[1];
		int iter = Integer.parseInt(args[2]);
		String stage = args[3];
		boolean setTime = args.length == 5 && args[4].equals("--set-time");

		LearnItConfig.loadParams(new File(params));
		TargetAndScoreTables data = TargetAndScoreTables.deserialize(new File(extractor));
		if (data.getIteration() != iter) {
			System.out.println("Changing iteration from "+data.getIteration()+" to "+iter);
			data.setIteration(iter);
		}
		if (!data.getStageName().equals(stage)) {
			System.out.println("Changing stage from "+data.getStageName()+" to "+stage);
			data.updateStage(stage);
		}
		if (setTime) {
			data = data.withNewStartDate();
		}

		data.serialize(new File(extractor));
	}

}
