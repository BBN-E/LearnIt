package com.bbn.akbc.neolearnit.modules;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.filters.MappingsFilter;
import com.bbn.akbc.neolearnit.mappings.filters.PatternMatchFilter;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoMap;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.observers.instance.SeedObserver;
import com.bbn.akbc.neolearnit.observers.instance.matchinfo.MatchInfoObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.PropObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Binary.TextEntityTypeBetweenSlotsObserver;
import com.bbn.akbc.neolearnit.observers.instance.pattern.Unary.UnaryPropObserver;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.Set;

public class DecodingExtractionModule {

	private final Injector recorderInjector;

	private final InstanceToSeedMapping.Builder seedRecorder;
	private final InstanceToPatternMapping.Builder observationRecorder;
	private final InstanceToMatchInfoMap.Builder matchRecorder;
	private final InstanceObservers binaryObservers;
	private final InstanceObservers unaryObservers;

	public DecodingExtractionModule() {
		recorderInjector = Guice.createInjector(new RecorderModule());

		// mapping builders are recorders
		this.seedRecorder = recorderInjector.getInstance(InstanceToSeedMapping.Builder.class);
		this.observationRecorder = recorderInjector.getInstance(InstanceToPatternMapping.Builder.class);
		this.matchRecorder = recorderInjector.getInstance(InstanceToMatchInfoMap.Builder.class);

		// observers require recorders of the correct types
		InstanceObservers.Builder binaryObsBuilder = new InstanceObservers.Builder();

		InstanceObservers.Builder unaryObsBuilder = new InstanceObservers.Builder();

		binaryObsBuilder.withObserver(new MatchInfoObserver(matchRecorder));
		binaryObsBuilder.withObserver(new SeedObserver(seedRecorder));

		for (String language : LearnItConfig.getList("languages")) {
			binaryObsBuilder
				.withObserver(new PropObserver(observationRecorder, language, LearnItConfig.getInt("max_prop_depth")))
				.withObserver(new TextEntityTypeBetweenSlotsObserver(observationRecorder, language, LearnItConfig.getInt("max_regexp_pattern_words")));
		}

        binaryObservers = binaryObsBuilder.build();
		unaryObservers = unaryObsBuilder.build();
	}

	public BilingualDocTheoryInstanceLoader getBilingualDocTheoryLoader(Target target) {
		return new BilingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers, true);
	}

	public MonolingualDocTheoryInstanceLoader getMonolingualDocTheoryLoader(Target target) {
		return new MonolingualDocTheoryInstanceLoader(target, binaryObservers,unaryObservers, true);
	}

	public InstanceToSeedMapping getSeedInstanceData() {
		return seedRecorder.build();
	}

	public InstanceToPatternMapping getPatternData() {
		return observationRecorder.build();
	}

	public Mappings getInformationForScoring() {
		return new Mappings(getSeedInstanceData(), getPatternData());
	}

	public InstanceToMatchInfoMap getMatchInfoMap() {
		return matchRecorder.build();
	}

	public Multimap<MatchInfo,LearnitPattern> instancesToMatches(Mappings mappings) {
		InstanceToMatchInfoMap matchMap = getMatchInfoMap();
		Multimap<MatchInfo,LearnitPattern> matches = HashMultimap.create();

		for (InstanceIdentifier id : mappings.getPatternInstances()) {
            MatchInfo match = matchMap.getMatchInfo(id);

            //Check that this match isn't ugly
            boolean goodMatch = true;
            for (String lang : match.getAvailableLanguages()) {
                MatchInfo.LanguageMatchInfo langMatch = match.getLanguageMatch(lang);

                String span0;
                if (langMatch.getSlot0().get() instanceof Mention) {
                    Mention m0 =(Mention)langMatch.getSlot0().get();
                    if (m0.entity(langMatch.getDocTheory()).isPresent())
                        span0 = m0.entity(langMatch.getDocTheory()).get().representativeMention().span().tokenizedText().utf16CodeUnits();
                    else
                        span0 = m0.atomicHead().span().tokenizedText().utf16CodeUnits();
                } else {
                    span0 = langMatch.getSlot0().get().span().tokenizedText().utf16CodeUnits();
                }
                String span1;
                if (langMatch.getSlot1().get() instanceof Mention) {
                    Mention m1 =(Mention)langMatch.getSlot1().get();
                    if (m1.entity(langMatch.getDocTheory()).isPresent())
                        span1 = m1.entity(langMatch.getDocTheory()).get().representativeMention().span().tokenizedText().utf16CodeUnits();
                    else
                        span1 = m1.atomicHead().span().tokenizedText().utf16CodeUnits();
                } else {
                    span1 = langMatch.getSlot1().get().span().tokenizedText().utf16CodeUnits();
                }

                if (span0.equals(span1)) {
                    goodMatch = false;
                    break;
                }

                final int sentenceLength = langMatch.getSentTheory().span().tokenizedText().utf16CodeUnits().split(" ").length;
                final int span0Length = span0.split(" ").length;
                final int span1Length = span1.split(" ").length;

                if ( (span0Length>=10 && span0Length!=sentenceLength) || (span1Length>=10 && span1Length!=sentenceLength) ) {
                    goodMatch = false;
                    break;
                }
            }

            if (goodMatch)
			    matches.putAll(match, mappings.getPatternsForInstance(id));
		}
		return matches;
	}

	public Multimap<MatchInfo,LearnitPattern> getExtractionResult(TargetAndScoreTables extractor) {
		System.out.println("\nApplying extractor: " + extractor.getTarget().getDescription());
		final Set<LearnitPattern> patterns = extractor.getPatternScores().getFrozen();
		MappingsFilter filter = new PatternMatchFilter(patterns);

		// instanceToSeedMapping, instanceToPatternMapping (these are the instantiated patterns, not user defined patterns)
		final Mappings info = getInformationForScoring();

		// filters instantiated observations by applying user-defined patterns
		System.out.println("DecodingExtractionModule.getExtractionResult(.): before filtering, " + info.getInstanceCount() + " candidate instances");
		Mappings filtered = filter.makeFiltered(info);
		System.out.println("DecodingExtractionModule.getExtractionResult(.): after filtering, left " + filtered.getInstanceCount() + " relevant instances");

		TargetFilter targetFilter = new TargetFilter(extractor.getTarget());
		Mappings targetFiltered = targetFilter.makeFiltered(filtered);	// TODO : why is this second filtering against Target necessary?
		System.out.println("Finally, left with " + targetFiltered.getInstanceCount() + " relevant instances");

		// this checks that slot0 and slot1 are not too long (token size)
		final Multimap<MatchInfo,LearnitPattern> finalMatches = instancesToMatches(targetFiltered);
		System.out.println("Final count : " + finalMatches.keySet().size());
		return finalMatches;
	}

	public static Multimap<MatchInfo,LearnitPattern> extractRelations(DocTheory document, TargetAndScoreTables extractor) {
		DecodingExtractionModule module = new DecodingExtractionModule();
		MonolingualDocTheoryInstanceLoader loader = module.getMonolingualDocTheoryLoader(extractor.getTarget());
		loader.load(document);
		return module.getExtractionResult(extractor);
	}

	public static Multimap<MatchInfo,LearnitPattern> extractRelations(BilingualDocTheory document, TargetAndScoreTables extractor) {
		DecodingExtractionModule module = new DecodingExtractionModule();

		BilingualDocTheoryInstanceLoader loader = module.getBilingualDocTheoryLoader(extractor.getTarget());
		loader.load(document);

		// the various Observers have been applied to obtain instances. Now apply user-defined patterns to filters instances
		final Multimap<MatchInfo, LearnitPattern> ret = module.getExtractionResult(extractor);
		return ret;
	}


}
