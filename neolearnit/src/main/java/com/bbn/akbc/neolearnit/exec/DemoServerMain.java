package com.bbn.akbc.neolearnit.exec;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.scoring.scores.PatternScore;
import com.bbn.akbc.neolearnit.scoring.tables.PatternScoreTable;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.DemoHandler;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMapDataStore;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DemoServerMain {

	public static void main(String[] args) throws Exception {
		String params = args[0];
		String displayDir = args[1];
		String extractorDir = args[2];
		int port = Integer.parseInt(args[3]);

        Set<Symbol> names = new HashSet<Symbol>();
        if (args.length > 4) {
            System.out.println("Loading names...");
            BufferedReader reader = new BufferedReader(new FileReader(args[4]));
            String line = reader.readLine();
            while (line != null) {
                names.add(Symbol.from(line.trim()));
                line = reader.readLine();
            }
        }

		LearnItConfig.loadParams(new File(params));

		Multimap<MatchInfoDisplay, LearnitPattern> displayMap = HashMultimap.create();

		int displayCounterTotal = 0;
		int displayCounterAdded = 0;
		System.out.println("Loading multimaps...");
		for (File displayFile : new File(displayDir).listFiles()) {
			@SuppressWarnings("unchecked")
			EfficientMultimapDataStore<MatchInfoDisplay, LearnitPattern> dataStore =
					StorageUtils.deserialize(displayFile, EfficientMultimapDataStore.class, false);

            if (!names.isEmpty()) {
                Multimap<MatchInfoDisplay, LearnitPattern> dataMap = dataStore.makeMultimap();
                for (MatchInfoDisplay display : dataMap.keySet()) {
                	if(display.getTarget().getName().toLowerCase().equals("org_aff_membership")) // ugly fix for Jan 2015 D2D demo
                		continue;

                	displayCounterTotal += 1;
                    for (String lang : display.getAvailableLanguages()) {
                        MatchInfoDisplay.LanguageMatchInfoDisplay lmi = display.getLanguageMatchInfoDisplay(lang);
                        if (lmi.getSeed().isPresent()) {
                            if (names.contains(lmi.getSeed().get().getSlot(0)) || names.contains(lmi.getSeed().get().getSlot(1))) {
                                displayMap.putAll(display, dataMap.get(display));
                                displayCounterAdded += 1;
                                break;
                            }
                        }
                    }
                }
            } else {
                displayMap.putAll(dataStore.makeMultimap());
            }
		}

		displayMap = filterDisplays(displayMap);


		/*
		// Bonan: this is for debug since we weren't able to start demoserver b/c of a 404 error.
		String strFileOutDebug = "/nfs/mercury-04/u22/Active/Projects/neolearnit/temp/log.demoserver";
		BufferedWriter bwDbg = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(strFileOutDebug), "UTF-8"));

		for(MatchInfoDisplay key : displayMap.keySet()) {
			for(LearnitPattern pattern : displayMap.get(key)) {
				//bwDbg.write("pattern: " + pattern.toIDString() + ", MatchInfo: " + key.html().replace("\n", " ").replace("\t", " ") + "\n");
				bwDbg.write("pattern: " + pattern.toPrettyString() + "\n" + "MatchInfo: " + key.toString() + "\n\n");
			}
		}
		bwDbg.close();
		//
		//System.out.println("Added " + displayCounterAdded + " out of " + displayCounterTotal + " MatchInfoDisplay");
		*/
		/*
		System.out.println("==== START printing the MatchInfoDisplay loaded ====");
		PrintStream ps = new PrintStream(System.out, true, "UTF-8");
		for(final MatchInfoDisplay d : displayMap.keySet()) {
			//ps.println(d.toString());
			//System.out.println(d.toString());
		}
		System.out.println("==== DONE printing the MatchInfoDisplay loaded ====");
		*/

		System.out.println("Loading score tables...");
		Map<Target,EfficientMapDataStore<LearnitPattern,PatternScore>> patternScoreTables = new HashMap<Target,EfficientMapDataStore<LearnitPattern,PatternScore>>();
		for (File extractorFile : new File(extractorDir).listFiles()) {
			if(extractorFile.getName().endsWith("json")) {
				TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(extractorFile);
				PatternScoreTable patternScores = extractor.getPatternScores();
				patternScores.reduceSize();
				patternScores.removeProposed();

				Map<LearnitPattern,PatternScore> reducedScoreMap = new HashMap<LearnitPattern, PatternScore>();
				for (LearnitPattern key : patternScores.getFrozen()) {
					if (displayMap.containsValue(key)) {
						reducedScoreMap.put(key, patternScores.getScore(key));
					}
				}

				patternScoreTables.put(extractor.getTarget(), EfficientMapDataStore.fromMap(reducedScoreMap));
			}
		}


		// ==== constraint using transliterations ====
		//final Multimap<Symbol, Symbol> transliterations =
		//	loadTransliterations( Files.asCharSource(LearnItConfig.getFile("transliterationFile"), Charsets.UTF_8) );
		//displayMap = filterByTransliterations(displayMap, transliterations);


		System.out.println("starting server...");
		DemoHandler handler = new DemoHandler(displayMap, patternScoreTables);

		new SimpleServer(handler, "html/profiles.html", port)
			.withIntroMessage("Running on "+port)
			.run();
	}

	public static Multimap<MatchInfoDisplay, LearnitPattern> filterDisplays(Multimap<MatchInfoDisplay, LearnitPattern> displayMap) {
		Multimap<MatchInfoDisplay, LearnitPattern> ret = HashMultimap.create();

		Set<Symbol> interestingEntities = Sets.newHashSet();
		for(final MatchInfoDisplay display : displayMap.keySet()) {
			final String relationName = display.getTarget().getName();

			if(relationName.compareTo("unary_per_statement")!=0) {
				final Optional<Seed> seed = display.getLanguageMatchInfoDisplay("english").getSeed();
				final Optional<Seed> canonicalSeed = display.getLanguageMatchInfoDisplay("english").getCanonicalSeed();

				if(canonicalSeed.isPresent()) {
					interestingEntities.add(canonicalSeed.get().getSlot(0));
					interestingEntities.add(canonicalSeed.get().getSlot(1));
				}
				else if(seed.isPresent()) {
					interestingEntities.add(seed.get().getSlot(0));
					interestingEntities.add(seed.get().getSlot(1));
				}
			}
		}

		for(final MatchInfoDisplay display : displayMap.keySet()) {
			final Optional<Seed> seed = display.getLanguageMatchInfoDisplay("english").getSeed();
			final Optional<Seed> canonicalSeed = display.getLanguageMatchInfoDisplay("english").getCanonicalSeed();

			if(canonicalSeed.isPresent()) {
				if(interestingEntities.contains(canonicalSeed.get().getSlot(0)) || interestingEntities.contains(canonicalSeed.get().getSlot(1))) {
					ret.putAll(display, displayMap.get(display));
				}
			}
			else if(seed.isPresent()) {
				if(interestingEntities.contains(seed.get().getSlot(0)) || interestingEntities.contains(seed.get().getSlot(1))) {
					ret.putAll(display, displayMap.get(display));
				}
			}

		}

		return ret;
	}


	private static Multimap<MatchInfoDisplay, LearnitPattern> filterByTransliterations(
			final Multimap<MatchInfoDisplay, LearnitPattern> displayMap,
			final Multimap<Symbol, Symbol> transliterations) throws Exception {
		final ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern> ret = new ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern>();

		PrintStream ps = new PrintStream(System.out, true, "UTF-8");

		for(final MatchInfoDisplay infoDisplay : displayMap.keySet()) {
			final Seed engSeed = infoDisplay.getLanguageMatchInfoDisplay("english").seed();
			final Seed chiSeed = infoDisplay.getLanguageMatchInfoDisplay("chinese").seed();

			if(engSeed!=null && chiSeed!=null) {
				final Symbol engS0 = engSeed.getSlot(0);
				final Symbol engS1 = engSeed.getSlot(1);
				final Symbol chiS0 = chiSeed.getSlot(0);
				final Symbol chiS1 = chiSeed.getSlot(1);

				if(transliterations.containsEntry(chiS0, engS0) && transliterations.containsEntry(chiS1, engS1)) {
					ps.println(chiS0+"=="+engS0 + " ||| " + chiS1+"=="+engS1);
					ret.putAll(infoDisplay, displayMap.get(infoDisplay));
				}
			}
		}

		return ret.build();
	}

	private static Multimap<Symbol, Symbol> loadTransliterations(final CharSource in) throws IOException {
		final ImmutableMultimap.Builder<Symbol, Symbol> ret = new ImmutableMultimap.Builder<Symbol, Symbol>();

		for(final String line : in.readLines()) {
			final String[] tokens = line.split("\t");
			final String chi = tokens[0];
			final String eng = tokens[2];
			final String acceptFlag = tokens[1];

			if(acceptFlag.compareTo("0")!=0) {
				ret.put(Symbol.from(chi), Symbol.from(eng));
			}
		}

		return ret.build();
	}
}
