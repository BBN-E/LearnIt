package com.bbn.akbc.neolearnit.common.util;

import com.bbn.akbc.neolearnit.common.LearnItConfig;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PathConverter {

	private static List<PathConversion> pathConversions;
	private static List<PathConversion> alignPathConversions;

	public static class PathConversion {
		private final Pattern pattern;
		private final String replacementString;

		private PathConversion(Pattern pattern, String replacementString) {
			this.pattern = pattern;
			this.replacementString = replacementString;
		}

		public static PathConversion fromLine(String line) {
			String parts[] = line.split("\t");
			return new PathConversion(Pattern.compile(parts[0].trim()), parts[1].trim());
		}

		public boolean isApplicable(String docid) {
			return pattern.matcher(docid).matches();
		}

		public String apply(String docid) {
			return pattern.matcher(docid).replaceFirst(replacementString);
		}
	}

	public synchronized static void load() throws IOException {
		pathConversions = new ArrayList<PathConversion>();
		load(new File(LearnItConfig.get("path_conversions")), pathConversions);

		if (LearnItConfig.defined("alignment_path_conversions")) {
			alignPathConversions = new ArrayList<PathConversion>();
			load(new File(LearnItConfig.get("alignment_path_conversions")), alignPathConversions);

		}
	}

	public synchronized static void load(File conv, List<PathConversion> dest) throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(conv));
		String line;
		while ((line = br.readLine()) != null) {

			dest.add(PathConversion.fromLine(line));

		}
		br.close();

	}

	public static String getFullPath(String docid) throws IOException {
		return getFullPath(docid,0);
	}

	public static String getFullPath(String docid, int languageId) throws IOException {
		if (pathConversions == null) load();

		String prefix = LearnItConfig.get("source_dir");
		if (LearnItConfig.optionalParamTrue("bilingual")) {
			prefix = prefix.replace("+language+", LearnItConfig.getList("languages").get(languageId));
		}

		for (PathConversion conversion : pathConversions) {
			if (conversion.isApplicable(docid)) {
				return prefix + "/" +conversion.apply(docid);
			}
		}

		throw new RuntimeException("Could not convert docid "+docid);
	}

	public static String getAlignmentPath(String docid) throws IOException {
		if (alignPathConversions == null) load();

		String prefix = LearnItConfig.get("alignments_dir");
//		return prefix + "/" + docid + ".alignment"; // bad fix for HK_protest_v2 corpus

		for (PathConversion conversion : alignPathConversions) {
//			System.out.println("== conversion.isApplicable(docid): " + conversion.isApplicable(docid) + ", on docid: " + docid);
			if (conversion.isApplicable(docid)) {
//				System.out.println("== " + prefix + "/" +conversion.apply(docid));

				return prefix + "/" +conversion.apply(docid);
			}
		}

		return prefix + "/" + docid + ".alignment"; // bad fix for HK_protest_v2 corpus

//		throw new RuntimeException("Could not convert docid "+docid);

	}

	public static File getFile(String docid) throws IOException {
		return new File(getFullPath(docid));
	}

	public static List<File> getFiles(String docid) throws IOException {
		System.out.println("-0-: " + getFullPath(docid,0));
		System.out.println("-1-: " + getFullPath(docid,1));

		return Lists.newArrayList(new File(getFullPath(docid,0)), new File(getFullPath(docid,1)));
	}

	public static void main(String[] args) throws IOException {

		String params = args[0];
		String docid = args[1];

		LearnItConfig.loadParams(new File(params));

		System.out.println(getFullPath(docid));

	}

}
