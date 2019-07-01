package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.impl.HashMapStorage;
import com.bbn.bue.common.files.FileUtils;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;


public class PrepareTargetMapping {

	public static void main(String[] args) throws IOException {

		String params = args[0];
		String targetName = args[1];
		int batchNum = Integer.parseInt(args[2]);
		String outdir = args[3];

		LearnItConfig.loadParams(new File(params));
		Target t = TargetFactory.fromString(targetName);

		File sourceMappingFiles = getSourceMappingsList(t, batchNum);
		if (!sourceMappingFiles.getParentFile().exists()) {
			throw new RuntimeException("No source mappings found for: " + sourceMappingFiles.getParentFile().getAbsolutePath());
		}

		if(!sourceMappingFiles.exists())
			return;

        ImmutableList<File> list = FileUtils.loadFileList(sourceMappingFiles);
	    TargetFilter filter = new TargetFilter(t);

	    MapStorage.Builder<InstanceIdentifier,Seed> i2Seed = new HashMapStorage.Builder<InstanceIdentifier,Seed>();
	    MapStorage.Builder<InstanceIdentifier,LearnitPattern> i2Pattern = new HashMapStorage.Builder<InstanceIdentifier,LearnitPattern>();

	    for (File f : list) {
	    	System.out.print("\n" + f.toString() + "\n");
	        Mappings info = Mappings.deserialize(f, true);
	        info = filter.makeFiltered(info);

	        System.out.println("Inserting a filtered mapping of size "+info.size()+"...");

	        i2Seed.putAll(info.getInstance2Seed().getStorage());
	        i2Pattern.putAll(info.getInstance2Pattern().getStorage());

	    }

	    Mappings finalInfo = new Mappings(new InstanceToSeedMapping(i2Seed.build()), new InstanceToPatternMapping(i2Pattern.build()));
	    File outfile = new File(outdir, String.format("batch_%d.sjson", batchNum));
	    finalInfo.serialize(outfile, true);
	}

	private static File getSourceMappingsList(Target t, int batchNum) {
		File mappingsListTopDir = new File(LearnItConfig.get("mappings_lists_dir"));
		File mappingsSubDir = new File(mappingsListTopDir,
				t.getMappingsSubDir());

        return new File(mappingsSubDir, String.format("batch_%d", batchNum));
	}

}
