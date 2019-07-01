package com.bbn.akbc.common;

import com.google.common.base.Optional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StructuredKnowledgeBase {
	private static Map<Pair<String, String>, Set<String>> knownPairs2slot;

	private static Set<String> dictionary;

	public static Optional<Set<String>> matchPair(Pair<String, String> pairOfNames) {
		if(knownPairs2slot.containsKey(pairOfNames))
			return Optional.of(knownPairs2slot.get(pairOfNames));
		else
			return Optional.absent();
	}

	public static boolean isInDictionary(String name) {
		return dictionary.contains(name);
	}

	public static Set<String> getDictionary() {
		return dictionary;
	}

	public static void initFromFile(String strFilePairs) {

		knownPairs2slot = new HashMap<Pair<String, String>, Set<String>>();
		dictionary = new HashSet<String>();

		int nLine=0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(strFilePairs));
			String sline;
			while ((sline = reader.readLine()) != null) {
				if(nLine++%100000==0)
					System.out.println("# lines read: " + nLine);

				String [] items = sline.split("\t");
				String oldSlot = items[2].trim();

				Optional<String> newSlot = SlotConverter.getNewSlot(oldSlot);

				if(newSlot.isPresent()) {
					Pair<String, String> pair;

					String name1 = items[0].trim().toLowerCase();
					String name2 = items[1].trim().toLowerCase();

					if(newSlot.get().endsWith("-1")) {
					  pair = new Pair<String, String>(name2, name1);
					  newSlot = Optional.of(newSlot.get().substring(0, newSlot.get().length()-2));
					}
					else
						pair = new Pair<String, String>(name1, name2);

					// update relations
					if(!knownPairs2slot.containsKey(pair))
						knownPairs2slot.put(pair, new HashSet<String>());
					knownPairs2slot.get(pair).add(newSlot.get());

					// update dictionary
					dictionary.add(name1);
					dictionary.add(name2);
				}

			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
