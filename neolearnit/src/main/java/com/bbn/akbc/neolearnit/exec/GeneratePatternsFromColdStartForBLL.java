package com.bbn.akbc.neolearnit.exec;

import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpReader;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


public class GeneratePatternsFromColdStartForBLL {
/*
	static ImmutableSet<String> kbpCollapsedSlots = (new ImmutableSet.Builder<String>())
			.add("kbp_org_alternate_names")
			.add("kbp_org_date_dissolved")
			.add("kbp_org_date_founded")
			.add("kbp_org_founded_by")
			.add("kbp_org_members")
			.add("kbp_org_number_of_employees_members")
			.add("kbp_org_parents")
			.add("kbp_org_place_of_headquarters")
			.add("kbp_org_shareholders")
			.add("kbp_org_website")
			.add("kbp_per_age")
			.add("kbp_per_alternate_names")
			.add("kbp_per_date_of_birth")
			.add("kbp_per_date_of_death")
			.add("kbp_per_employee_or_member_of")
			.add("kbp_per_origin")
			.add("kbp_per_other_family")
			.add("kbp_per_parents")
			.add("kbp_per_place_of_birth")
			.add("kbp_per_place_of_death")
			.add("kbp_per_place_of_residence")
			.add("kbp_per_schools_attended")
			.add("kbp_per_siblings")
			.add("kbp_per_spouse")
			.add("kbp_per_title")
			.build();
*/

	static boolean isLegalSexpRoot(String sline) {
		sline = sline.trim();

		if(!sline.startsWith("(nprop") && !sline.startsWith("(vprop") && !sline.startsWith("(mprop") &&
				!sline.startsWith("(sprop") && !sline.startsWith("(anyprop") && !sline.startsWith("(cprop") &&
				!sline.startsWith("(regex"))
			return false;

		String [] items = sline.split(" ");
		int numProps = 0;
		for(String item : items) {
			if(item.contains("nprop") || item.contains("vprop") || item.contains("mprop") || item.contains("sprop") ||
					item.contains("anyprop") || item.contains("cprop"))
					numProps++;
		}

		if(numProps>1)
			return false;

		SexpReader sexpreader = SexpReader.createDefault(); // (new SexpReade.()).build();
		try {
			Sexp sexp = sexpreader.read(sline.trim());
		} catch(Exception e) {
			System.out.println("SKIP non-root sexp line: " + sline);
			return false;
		}

		return true;
	}

	public static void main(String [] argv) throws IOException {
//		String dirTarget = "/nfs/mercury-04/u42/bmin/source/Active/Projects/neolearnit/inputs/targets/kbp_";
		String strDirTarget = "/nfs/mercury-04/u42/bmin/source/Active/Projects/neolearnit/inputs/targets/";
		File dirTarget = new File(strDirTarget);
		for(File fileTarget : dirTarget.listFiles()) {
			if(fileTarget.getName().startsWith("kbp_")) {
				String relnType = fileTarget.getName().replace(".target.xml", "").replace("kbp_", "");

				String patternFileOut = "/nfs/mercury-04/u42/bmin/source/Active/Projects/neolearnit/inputs/patterns/bilingual_chinese_gigaword_default.2/kbp_"
						+ relnType + ".sexp";
				Writer patternWriter = new OutputStreamWriter(new FileOutputStream(new File(patternFileOut)),"UTF-8");

				String patternFileAssessment = "/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/experiments/pattern_files_for_cs2014/from-assessment/cat_sf2012_sf2013_cs2013-"
						+ relnType + ".pattern.cs.review";
				String patternFileDs = "/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/experiments/pattern_files_for_cs2014/from-ds/ds_freebase_on_gigawordv5_and_tac_corpus_patterns-"
						+ relnType + ".dat.NoId.pattern.cs.review";
				String patternFileLearnIt = "/nfs/mercury-04/u42/bmin/Git_repo/repo/kbp/coldstart/experiments/pattern_files_for_cs2014/kbp-tac-pruned/"
						+ relnType + "-patterns.txt";

				System.out.println("==== " + relnType);
				System.out.println("== " + patternFileAssessment);
				System.out.println("== " + patternFileDs);
				System.out.println("== " + patternFileLearnIt);
				System.out.println();

				List<String> lines = new ArrayList<String>();
				if((new File(patternFileAssessment)).exists())
					lines.addAll(Files.readLines(new File(patternFileAssessment), Charsets.UTF_8));
				if((new File(patternFileDs)).exists())
					lines.addAll(Files.readLines(new File(patternFileDs), Charsets.UTF_8));
				if((new File(patternFileLearnIt)).exists())
					lines.addAll(Files.readLines(new File(patternFileLearnIt), Charsets.UTF_8));

				for(String line : lines) {
					if(isLegalSexpRoot(line))
						patternWriter.write(line + "\n");
				}

				patternWriter.close();
			}
		}

/*
		String dir1 = "";
		String dir2 = "";
		String dir3 = "";

		String patternFile = "";
		for (String line : Files.readLines(new File(patternFile), Charsets.UTF_8)) {

		}
*/
	}
}
