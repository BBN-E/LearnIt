package com.bbn.akbc.neolearnit.server.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bbn.akbc.neolearnit.common.LearnItConfig;

public class MainHandler extends SimpleJSONHandler {

	@JettyMethod("/get_eval_reports")
	public List<String> getEvalReports() {
		try {

			List<String> paths = new ArrayList<String>();
			File archiveDir = new File(LearnItConfig.get("archive_dir")+"/reports");
			for (File exptDir : archiveDir.listFiles()) {
				for (File relationDir : exptDir.listFiles()) {
					for (File report : relationDir.listFiles()) {
						paths.add(exptDir.getName()+"/"+relationDir.getName()+"/"+report.getName());
					}
				}
			}
			return paths;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
