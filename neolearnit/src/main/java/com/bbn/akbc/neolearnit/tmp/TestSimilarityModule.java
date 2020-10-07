package com.bbn.akbc.neolearnit.tmp;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.common.targets.TargetSlot;
import com.bbn.akbc.neolearnit.common.targets.constraints.impl.SpanningTypeConstraint;
import com.bbn.akbc.neolearnit.mappings.filters.TargetFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.observations.similarity.SeedSimilarity;
import com.bbn.akbc.neolearnit.similarity.ObservationSimilarityModule;
import com.bbn.akbc.utility.Pair;
import com.google.common.base.Optional;

import java.io.File;
import java.util.Arrays;

public class TestSimilarityModule {

    public static void main(String[] args) throws Exception {
        String learnitConfigPath = args[0];
        LearnItConfig.loadParams(new File(learnitConfigPath));
        String mappingPath = args[1];
        Mappings mappings = Mappings.deserialize(new File(mappingPath), true);
        String suffix = args[2];
        ObservationSimilarityModule similarityModule = ObservationSimilarityModule.create(mappings, suffix);

        Target newTarget = new Target.Builder("test").
                withTargetSlot(new TargetSlot.Builder(0, "all").build()).
                withTargetSlot(new TargetSlot.Builder(1, "all").build())
                .withAddedConstraint(new SpanningTypeConstraint(0, Arrays.asList(InstanceIdentifier.SpanningType.EventMention)))
                .withAddedConstraint(new SpanningTypeConstraint(1, Arrays.asList(InstanceIdentifier.SpanningType.Empty))).build();

        TargetFilter targetFilter = new TargetFilter(newTarget);
        Mappings targetMappings = targetFilter.makeFiltered(mappings);

        for (Seed seed : targetMappings.getAllSeeds().elementSet()) {
            System.out.println(seed.toPrettyString());

            System.out.println("-----");

            Optional<SeedSimilarity> seedSimilarityOp = similarityModule.getSeedSimilarity(seed);
            if (!seedSimilarityOp.isPresent()) continue;
            SeedSimilarity seedSimilarity = seedSimilarityOp.get();
            for (Pair<? extends LearnItObservation, Double> seedSimilarityEn : seedSimilarity.getSimilarObservationsAsSortedList()) {
                Seed similarSeed = (Seed) seedSimilarityEn.key;
                Double score = seedSimilarityEn.value;
                System.out.println(similarSeed.toPrettyString() + " " + score);
            }

            System.out.println("=====");
        }

    }
}
