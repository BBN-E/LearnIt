package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MappingDump {

	public static String patternIdListString(Collection<LearnitPattern> patterns) {
		StringBuilder result = new StringBuilder();
		result.append("\n\t[patterns: \n");
		for (LearnitPattern p : patterns) {
			result.append("\t\t"+p.toIDString()+"\n");
		}
		result.append("\t]");
		return result.toString();
	}

	public static void main(String[] args) throws IOException {
		String mapping = args[0];
		Integer sample = Integer.parseInt(args[1]);

		Mappings info = Mappings.deserialize(new File(mapping), true);

		List<InstanceIdentifier> insts = Lists.newArrayList(info.getInstance2Pattern().getAllInstances().elementSet());
		Collections.shuffle(insts);

		for (InstanceIdentifier id : insts.subList(0, Math.min(sample, insts.size()))) {
			System.out.println(id.toShortString()+": \n\t[seeds: "+info.getSeedsForInstance(id)+"] "+patternIdListString(info.getPatternsForInstance(id)));
		}

	}

}
