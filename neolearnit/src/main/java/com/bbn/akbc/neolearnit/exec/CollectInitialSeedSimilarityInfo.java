package com.bbn.akbc.neolearnit.exec;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.common.targets.Target;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.hash.Hashing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mcrivaro on 8/20/2014.
 */
public class CollectInitialSeedSimilarityInfo {

    private static final Multimap<String,Integer> matrix = Multimaps.synchronizedSetMultimap(HashMultimap.<String,Integer>create());

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        File mappingsDir = new File(args[0] + "/mappings");
        LearnItConfig.loadParams(new File(args[1]));
        File extractor = new File(args[2]);

        TargetAndScoreTables data = TargetAndScoreTables.deserialize(extractor);

        Set<Seed> frozen = data.getSeedScores().getFrozen();

        int CONCURRENCY = LearnItConfig.getInt("loader_concurrency");

        List<Callable<Void>> calls = new ArrayList<Callable<Void>>();

        for (final File input : mappingsDir.listFiles()) {
            calls.add(new SeedInfoCallable(input,data.getTarget(),frozen));
        }

        ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);
//        List<Multimap<String,Integer>> results = new ArrayList<Multimap<String,Integer>>();

        try {
            /*List<Future<Void>> futureResults = */service.invokeAll(calls);

//            for (Future<Void> future : futureResults) {
//                try {
//                    results.add(future.get());
//                } catch (ExecutionException ex) {
//                    ex.printStackTrace();
//                }
//            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        service.shutdown();

//        for (Multimap<String,Integer> result : results)
//            matrix.putAll(result);
        Multimap<String,Integer> updateMatrix = HashMultimap.create();
        for (String key : matrix.keySet())
            if (matrix.get(key).size() > 1) updateMatrix.putAll(key,matrix.get(key));

        SeedSimilarity.load(data);
//        for (Seed seed : frozen) {
//            String s = seed.withProperText(data.getTarget()).toSimpleString();
//            if (updateMatrix.containsKey(s)) {
//                System.out.printf("%s: %d\n", s, updateMatrix.get(s).size());
//                if (SeedSimilarity.seedHasScore(s)) {
//                    System.out.println("(Already in seedSim)");
//                }
//            }
//        }
        SeedSimilarity.updateSeedSimilarity(data, updateMatrix);

        long end = System.currentTimeMillis();

        System.out.println("Added initial seed data in " + ((end-start)/1000) + " seconds");
    }

    public static class SeedInfoCallable implements Callable<Void> {

        private final File file;
        private Target target;
        private Set<Seed> frozen;

        public SeedInfoCallable(File file, Target target, Set<Seed> frozen) {
            this.file = file;
            this.target = target;
            this.frozen = frozen;
        }

        @Override
        public Void call() throws IOException {
//            Multimap<String,Integer> matrix = HashMultimap.create();
            System.out.println("Extracting from "+file.getName()+"...");
            Mappings mappings = StorageUtils.deserialize(file, Mappings.class, true);
            for (Seed seed : frozen) {
                for (LearnitPattern pattern : mappings.getPatternsForSeed(seed)) {
                    matrix.put(seed.withProperText(target).toSimpleString(),
                    Hashing.md5().hashString(pattern.toIDString(), Charset.defaultCharset()).asInt());
                }
            }
            return null;
        }

    }
}
