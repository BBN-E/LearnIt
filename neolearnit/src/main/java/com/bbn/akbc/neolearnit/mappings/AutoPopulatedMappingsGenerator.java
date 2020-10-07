package com.bbn.akbc.neolearnit.mappings;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.loaders.AbstractInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.LoaderUtils;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.mappings.filters.FrequencyLimitFilter;
import com.bbn.akbc.neolearnit.mappings.filters.RelevantInstancesFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToMatchInfoDisplayMap;
import com.bbn.akbc.neolearnit.modules.BilingualExtractionModule;
import com.bbn.akbc.neolearnit.modules.MonolingualExtractionModule;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.util.List;

public class AutoPopulatedMappingsGenerator {

    public static Mappings generateMappings(Target target, List<DocTheory> docTheoryList, List<BilingualDocTheory> bilingualDocTheoryList) throws Exception{
        Mappings info;
        InstanceToMatchInfoDisplayMap display;

        if (LearnItConfig.optionalParamTrue("bilingual")) {

            BilingualExtractionModule module = new BilingualExtractionModule(false);
            BilingualDocTheoryInstanceLoader bilingualDocTheoryInstanceLoader = module.getDocTheoryLoader(target);
            LoaderUtils.loadBilingualDocTheoryList(bilingualDocTheoryList,bilingualDocTheoryInstanceLoader);
            //get our mappings out of the module (MAGIC!)
            info = module.getInformationForScoring();
            display = module.getMatchInfoDisplayMap();

        } else {

            MonolingualExtractionModule module = new MonolingualExtractionModule(false);
            MonolingualDocTheoryInstanceLoader docTheoryLoader = module.getDocTheoryLoader(target);
            LoaderUtils.loadDocTheroyList(docTheoryList,docTheoryLoader);
            //get our mappings out of the module (MAGIC!)
            info = module.getInformationForScoring();
            display = module.getMatchInfoDisplayMap();
        }
        if (!LearnItConfig.optionalParamTrue("running_test_set")){
            info = new RelevantInstancesFilter().makeFiltered(info);
        }

        Integer minSeedFrequency = LearnItConfig.params().
                getOptionalInteger("min_seed_frequency_per_batch").or(-1);
        Integer minPatternFrequency = LearnItConfig.params().
                getOptionalInteger("min_pattern_frequency_per_batch").or(-1);
        if(minSeedFrequency > 1 || minPatternFrequency > 1) {
            FrequencyLimitFilter frequencyLimitFilter = new FrequencyLimitFilter(minSeedFrequency, -1, minPatternFrequency, -1);
            info = frequencyLimitFilter.makeFiltered(info);
        }
        if(LearnItConfig.optionalParamTrue("debug_extraction")) {
            for(LearnitPattern learnitPattern: info.getAllPatterns()){
                System.out.println(learnitPattern.toPrettyString());
            }
        }
        return info;
    }

}
