package com.bbn.akbc.neolearnit.common.resources;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.google.common.collect.ImmutableSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class StopWords {

	//singleton
	private static StopWords defaultStopWords = null;

	public static StopWords getDefault() {
		if (defaultStopWords == null) {
			defaultStopWords = new StopWords(new File(LearnItConfig.get("stopwords")));
		}
		return defaultStopWords;
	}

    public static StopWords getFromParamsWithBackoff(String param) {
        if (param.equals("overlap_seed_match_filter_word_list")) {
            if (LearnItConfig.defined(param)) {
                return new StopWords(new File(LearnItConfig.get(param)));
            } else if (LearnItConfig.defined("expanded_stopwords")) {
                return new StopWords(new File(LearnItConfig.get("expanded_stopwords")));
            } else {
                return getDefault();
            }
        } else if (param.equals("expanded_stopwords")) {
            if (LearnItConfig.defined(param)) {
                return new StopWords(new File(LearnItConfig.get(param)));
            } else {
                return getDefault();
            }
        } else if (param.equals("stopwords")) {
            return getDefault();
        } else {
            throw new RuntimeException("Parameter " + param + " is not a valid Stopword parameter");
        }
    }

	private final Set<Symbol> stopwords;

	public StopWords(File stopwordFile) {
		stopwords = loadStopWords(stopwordFile);
	}

	@SuppressWarnings("resource")
	private Set<Symbol> loadStopWords(File stopwordFile) {
		ImmutableSet.Builder<Symbol> builder = ImmutableSet.builder();
		BufferedReader cl;
		try {
			cl = new BufferedReader(new InputStreamReader(
					new FileInputStream(stopwordFile), "UTF-8"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not open stopword file at " + stopwordFile.getAbsolutePath() + ".");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not open stopword file.");
		}
		String line;
		try {
			while ((line = cl.readLine()) != null) {
				String [] words = line.trim().split(" ");
				for (String word : words)
					builder.add(Symbol.from(word.toLowerCase()));
			}
			cl.close();
		} catch (IOException e) {
			throw new RuntimeException("Error while reading the stopword file.");
		}
		builder.add(Symbol.from("-lrb-"));
		builder.add(Symbol.from("-rrb-"));
		builder.add(Symbol.from("'s"));
		builder.add(Symbol.from("says"));
		builder.add(Symbol.from("saying"));
		builder.add(Symbol.from("said"));
		builder.add(Symbol.from("told"));
		builder.add(Symbol.from("tell"));

		builder.add(Symbol.from(","));
		builder.add(Symbol.from("-"));
		builder.add(Symbol.from("--"));
		builder.add(Symbol.from("'"));
		builder.add(Symbol.from("''"));
		builder.add(Symbol.from("\""));
		builder.add(Symbol.from(":"));
		builder.add(Symbol.from(";"));
		builder.add(Symbol.from("``"));
		builder.add(Symbol.from("&"));
		builder.add(Symbol.from("/"));

		return builder.build();
	}

	public boolean isStopWord(String word) {
		return stopwords.contains(Symbol.from(word.toLowerCase()));
	}

	public boolean isStopWord(Symbol word) {
		return isStopWord(word.toString());
	}

	public Set<Symbol> getStopWords() {
		return stopwords;
	}
}
