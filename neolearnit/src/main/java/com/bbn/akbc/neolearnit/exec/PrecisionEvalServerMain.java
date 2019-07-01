package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.PrecisionEvalHandler;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PrecisionEvalServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
		String mappingsFile = args[1]+".sjson";
		String displayMapFile = args[1]+".display.sjson";
        String targetName = args[2];
        String output = args[3];
		int port = Integer.parseInt(args[4]);

		LearnItConfig.loadParams(new File(params));

		System.out.println("loading mappings...");

		Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);
        Map<InstanceIdentifier,MatchInfoDisplay> displayMap = StorageUtils.deserialize(new File(displayMapFile), EfficientMapDataStore.class, true).makeMap();

        System.out.println(displayMap.keySet().size());

        File goodPrecalculated = new File(output+"_good.json");
        File badPrecalculated = new File(output+"_bad.json");
        Map<InstanceIdentifier,Boolean> precalculatedJudgments = new HashMap<InstanceIdentifier, Boolean>();

        if (goodPrecalculated.exists()) {
            for (InstanceIdentifier id : Mappings.deserialize(goodPrecalculated,false).getPatternInstances())
                precalculatedJudgments.put(id,true);
        }

        if (badPrecalculated.exists()) {
            for (InstanceIdentifier id : Mappings.deserialize(badPrecalculated,false).getPatternInstances())
                precalculatedJudgments.put(id,false);
        }

		System.out.println("starting server...");
        PrecisionEvalHandler handler = new PrecisionEvalHandler(mappings, targetName, precalculatedJudgments, displayMap, output);

		new SimpleServer(handler, "html/precisionEval.html", port)
			.withIntroMessage("Running relation "+targetName+" on port "+port)
			.run();
	}
}
