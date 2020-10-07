package com.bbn.akbc.neolearnit.oie;

import com.bbn.akbc.neolearnit.common.Domain;
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
        int port = Integer.parseInt(args[1]);
        LearnItConfig.loadParams(new File(params));

        String extractorDir = Domain.getExtractorsPath();
        System.out.println("starting server...");
        OnDemandReHandler handler = new OnDemandReHandler(extractorDir);

        new SimpleServer(handler, "index.html", port)
                .withIntroMessage("Running On-demand RE  Server on port " + port)
                .run();
    }
}
