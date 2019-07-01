package com.bbn.akbc.neolearnit.exec;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.evaluation.result.BilingualExtractionResult;
import com.bbn.akbc.neolearnit.evaluation.result.MonolingualExtractionResult;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.bbn.akbc.neolearnit.storage.structs.EfficientMultimapDataStore;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Outputs a mapping of MatchInfoDisplay objects to their matching patterns for
 * a given set of extractors run over a given set of files.
 */
public class SimpleRelationExtractor {

	public static void main(String[] args) throws IOException {
		final String paramsFile = args[0];
		final String inputList = args[1];	// (eng chi alignment) file
		final String outputDir = args[2];	// write extracted relation instances json file to here

		LearnItConfig.loadParams(new File(paramsFile));
		System.out.println(LearnItConfig.dumpSorted());

		final Double threshold = LearnItConfig.getDouble("extractor.threshold");

		// extractors containing patterns for each relation type
		final File[] extractorFiles = new File(LearnItConfig.get("extractor.directory")).listFiles();

		Collection<TargetAndScoreTables> extractors = new ArrayList<TargetAndScoreTables>();
		for(final File file : extractorFiles) {
			if(file.getName().endsWith("json")) {
				System.out.println("Loading extractor: " + file.getAbsolutePath());
				TargetAndScoreTables extractor = TargetAndScoreTables.deserialize(file);
				extractor.setConfidenceThreshold(threshold);
				System.out.println(extractor.getTarget().toString());
				extractors.add(extractor);
			}
		}

		/*
		// when you call this for the first time, make sure your neolearnit/inputs/targets contain a per_statement.target.xml file
		// We will create a Target based on the xml file, then serialize out the Target object to neolearnit/inputs/targets/json/per_statement.json
		// And then in future, we will just read the json file if it already exists
		final TargetAndScoreTables statementExtractor = new TargetAndScoreTables("unary_per_statement");
		statementExtractor.setConfidenceThreshold(threshold);
		System.out.println("Loading extractor:\n" + statementExtractor.getTarget().toString());
		extractors.add(statementExtractor);
		*/


		File outdir = new File(outputDir);
		if (!outdir.exists()) outdir.mkdirs();


		// ==== transliterations ====
		Optional<Multimap<Symbol, Symbol>> transliterations = Optional.absent();
		//if(LearnItConfig.defined("transliterationFile")) {
		//	transliterations = Optional.of(loadTransliterations(Files.asCharSource(LearnItConfig.getFile("transliterationFile"), Charsets.UTF_8)));
		//}

		// ==== chi-eng name mapping ====
		Optional<Map<Symbol, Symbol>> chiEngNameMapping = Optional.absent();
		if(LearnItConfig.defined("chiEngNameMappingFile")) {
			chiEngNameMapping = Optional.of(loadChiEngNameMapping(Files.asCharSource(LearnItConfig.getFile("chiEngNameMappingFile"), Charsets.UTF_8)));
		}


		Multimap<MatchInfoDisplay,LearnitPattern> allDisplays = HashMultimap.create();

		File fileinput = new File(inputList);

		if (LearnItConfig.optionalParamTrue("bilingual")) {

			Collection<BilingualDocTheory> docs = LoaderUtils.loadRegularBilingualFileList(fileinput);
			for (BilingualDocTheory doc : docs) {

//				Writer writer = new OutputStreamWriter(new FileOutputStream(
//						new File(outputDir+"/"+doc.getSourceDoc().docid().toString())),"UTF-8");
				BilingualExtractionResult.Builder resultBuilder = new BilingualExtractionResult.Builder();

				resultBuilder.withDocument(doc.getSourceDoc().docid().toString(), doc);


				for (TargetAndScoreTables extractor : extractors) {
					resultBuilder.withMatches(extractor.extractRelations(doc));
				}

				BilingualExtractionResult result = resultBuilder.build();
//				result.writeOutput(writer);
//				writer.close();

				final Multimap<MatchInfoDisplay, LearnitPattern> displays = result.getAllDisplays(chiEngNameMapping);
				if(transliterations.isPresent()) {
					allDisplays.putAll(setTransliterationFlagInDisplay(displays, transliterations.get()));
				}
				else {
					allDisplays.putAll(displays);
				}
			}

		} else {

			Collection<DocTheory> docs = LoaderUtils.loadFileList(fileinput);
			for (DocTheory doc : docs) {
//				Writer writer = new OutputStreamWriter(new FileOutputStream(
//						new File(outputDir+"/"+doc.docid().toString())),"UTF-8");
				MonolingualExtractionResult.Builder resultBuilder = new MonolingualExtractionResult.Builder();

				resultBuilder.withDocument(doc.docid().toString(), doc);
				for (TargetAndScoreTables extractor : extractors) {
					resultBuilder.withMatches(extractor.extractRelations(doc));
				}

				MonolingualExtractionResult result = resultBuilder.build();
//				result.writeOutput(writer);
//				writer.close();

				allDisplays.putAll(result.getAllDisplays(chiEngNameMapping));
			}

		}

		StorageUtils.serialize(new File(outputDir, String.format("%s_display.json",fileinput.getName())),
				EfficientMultimapDataStore.fromMultimap(allDisplays), false);
	}


	private static Multimap<MatchInfoDisplay, LearnitPattern> setTransliterationFlagInDisplay(
			final Multimap<MatchInfoDisplay, LearnitPattern> displays, Multimap<Symbol, Symbol> transliterations) {
		final ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern> ret = new ImmutableMultimap.Builder<MatchInfoDisplay, LearnitPattern>();

		for(final MatchInfoDisplay infoDisplay : displays.keySet()) {
			final Collection<LearnitPattern> patterns = displays.get(infoDisplay);

			final Seed engSeed = infoDisplay.getLanguageMatchInfoDisplay("english").seed();
			final Seed chiSeed = infoDisplay.getLanguageMatchInfoDisplay("chinese").seed();

			int isValidTransliteration = 1;
			if(engSeed!=null && chiSeed!=null) {
				final Symbol engS0 = engSeed.getSlot(0);
				final Symbol engS1 = engSeed.getSlot(1);
				final Symbol chiS0 = chiSeed.getSlot(0);
				final Symbol chiS1 = chiSeed.getSlot(1);

				if(!transliterations.containsEntry(chiS0, engS0) || !transliterations.containsEntry(chiS1, engS1)) {
					isValidTransliteration = 0;
				}
			}

			final MatchInfoDisplay newInfoDisplay = infoDisplay.copyWithTransliteration(isValidTransliteration);
			ret.putAll(newInfoDisplay, patterns);
		}

		return ret.build();
	}

	private static Multimap<Symbol, Symbol> loadTransliterations(final CharSource in) throws IOException {
		final ImmutableMultimap.Builder<Symbol, Symbol> ret = new ImmutableMultimap.Builder<Symbol, Symbol>();

		for(final String line : in.readLines()) {
			final String[] tokens = line.split("\t");
			final String chi = tokens[0];
			final String eng = tokens[1];
			ret.put(Symbol.from(chi), Symbol.from(eng));
		}

		return ret.build();
	}

	private static Map<Symbol, Symbol> loadChiEngNameMapping(final CharSource in) throws IOException {
		final ImmutableMap.Builder<Symbol, Symbol> ret = new ImmutableMap.Builder<Symbol, Symbol>();

		for(final String line : in.readLines()) {
			final String[] tokens = line.split("\t");
			final String chi = tokens[0];
			final String eng = tokens[1];
			//final String eng = tokens[1].substring(0, tokens[1].lastIndexOf(":"));
			ret.put(Symbol.from(chi), Symbol.from(eng));
		}

		return ret.build();
	}

}
