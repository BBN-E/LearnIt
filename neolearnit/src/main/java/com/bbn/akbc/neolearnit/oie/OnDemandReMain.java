package com.bbn.akbc.neolearnit.oie;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.OnDemandReHandler;

import java.io.File;

/**
 * Created by bonan on 1/18/17.
 * - Simplied and modified from InitializationPatternWritingServerMain
 */
public class OnDemandReMain {

    public static void main(String[] args) throws Exception {

        String params = args[0];
        String mappingsFile = args[1];
        //The following is the suffix identifying the sub-directory with similarity output,
        //e.g. a combination of target and epoch number (like "all_event_event_pairs-3") for new runs, or
        //just the target (like "all_event_event_pairs") for old runs.
        //This is used by ObservationSimilarityModule
        String suffixForSimDirs = args[2];
        int port = -1;
        //If args[2] is a number, this indicates that we don't want to use similarity feature,
        // in which case suffixForSimDirs's value will be null
        try{
            port = Integer.parseInt(suffixForSimDirs);
            suffixForSimDirs = null;
        }catch (NumberFormatException e){
            port = Integer.parseInt(args[3]);
        }

        LearnItConfig.loadParams(new File(params));

        System.out.println("loading mappings...");
        Mappings mappings = Mappings.deserialize(new File(mappingsFile), true);

        System.out.println("starting server...");
        OnDemandReHandler handler = new OnDemandReHandler(mappings, suffixForSimDirs);

        new SimpleServer(handler, "index.html", port)
                .withIntroMessage("Running On-demand RE  Server on port "+port)
                .run();
    }
}
