package com.bbn.akbc.neolearnit.common;

import com.bbn.bue.common.parameters.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.SortedMap;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

public class LearnItConfig {
	private static Parameters params = new Parameters(new HashMap<String,String>());

	public static synchronized void loadParams(File paramFile) {
		try {
			params = Parameters.loadSerifStyle(paramFile);
		} catch (IOException e) {
			System.out.println("Failed to read parameter file!");
			e.printStackTrace();
		}
	}

	public static Parameters params() {
		return params;
	}

	public static File getFile(String param) {
		return params.getExistingFile(param);
	}

	public static String get(String param) {
		return params.getString(param);
	}

    public static List<String> getOptionalList(String param) {
        return defined(param) ? getList(param) : ImmutableList.<String>of();
    }

	public static List<String> getList(String param) {
		return getList(param,",");
	}

	public static List<String> getList(String param, String delimiter) {
		String toList = params.getString(param);
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (String s : Arrays.asList(toList.split(delimiter)))
			builder.add(s.trim());
		return builder.build();
	}

	public static List<Integer> getIntList(String param) {
		return getIntList(param,",");
	}

	public static List<Integer> getIntList(String param, String delimiter) {
		String toList = params.getString(param);
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (String s : Arrays.asList(toList.split(delimiter)))
			builder.add(Integer.parseInt(s.trim()));
		return builder.build();
	}

	public static int getInt(String param) {
		return params.getInteger(param);
	}

	public static double getDouble(String param) {
		return params.getDouble(param);
	}

	public static boolean optionalParamTrue(String param) {
		return defined(param) && Boolean.parseBoolean(get(param));
	}

	public static boolean defined(String param) {
		return params.isPresent(param);
	}

	public static Map<String,String> allParams() {
		String dumpString = params.dump(false);
		ImmutableMap.Builder<String,String> builder = ImmutableMap.builder();
		for (String keyValue : dumpString.split("\n")) {
			String name = keyValue.split(":")[0].trim();
			String value = keyValue.split(":")[1].trim();
			builder.put(name, value);
		}
		return builder.build();
	}

	public static String dumpSorted() {
		final Map<String, String> params = allParams();
		final SortedMap<String, String> sortedParams = new TreeMap(params);

		StringBuffer s = new StringBuffer("");
		for(final Entry<String,String> p : sortedParams.entrySet()) {
			s.append(p.getKey() + " " + p.getValue() + "\n");
		}

		return s.toString();
	}
}
