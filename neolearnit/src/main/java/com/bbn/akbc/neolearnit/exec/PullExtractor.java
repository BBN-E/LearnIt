package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.groups.EvalReportMappings;
import com.bbn.akbc.neolearnit.storage.StorageUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mcrivaro on 8/22/2014.
 */
public class PullExtractor {
    public static void main(String[] args) throws IOException {
        LearnItConfig.loadParams(new File(args[0]));
        EvalReportMappings mappings = StorageUtils.deserialize(new File(args[1]), EvalReportMappings.class, true);
        String location = args[2];

        StorageUtils.serialize(new File(location), mappings.getExtractor(), false);
    }
}
