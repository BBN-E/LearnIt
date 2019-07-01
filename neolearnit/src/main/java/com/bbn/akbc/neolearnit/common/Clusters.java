package com.bbn.akbc.neolearnit.common;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.clusters.Cluster;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by mcrivaro on 8/12/2014.
 */
public class Clusters {
    private static com.bbn.nlp.clusters.Clusters clusters;
    private static Optional<Integer> clusterWordMin = Optional.absent();
    private static Optional<Integer> clusterWordMax = Optional.absent();
    private static Set<Integer> clusterLevels = ImmutableSet.of(12, 14, 16);

    private static void loadClusters() {
        try {
            final Parameters params = LearnItConfig.params();
            clusters = com.bbn.nlp.clusters.Clusters.from(params);
            clusterWordMin = params.getOptionalInteger("clusterWordMin");
            clusterWordMax = params.getOptionalInteger("clusterWordMax");
            if (params.isPresent("cluster_levels")) {
                clusterLevels = ImmutableSet.copyOf(params.getPositiveIntegerList("cluster_levels"));
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static com.bbn.nlp.clusters.Clusters getClusters() {
        if (clusters == null) {
            loadClusters();
        }
        return clusters;
    }

//    public static Symbol getCluster(final Symbol word) {
//        final Optional<Cluster> ret = getClusters().getClusterForWord(word);
//        if (ret.isPresent()) {
//            return ret.get().asSymbol();
//        } else {
//            return Symbol.from("");
//        }
//    }

//    public static Symbol getCluster(final Symbol word, final int level) {
//        final Optional<Cluster> ret= getClusters().getClusterForWord(word);
//
//        if (ret.isPresent()) {
//            return ret.get().asSymbolTruncatedToNBits(level);
//        } else {
//            return Symbol.from("");
//        }
//    }

    public static List<Symbol> getClusters(final Symbol word) {
        Optional<Cluster> clustId = clusters.getClusterForWord(word);
        if (clustId.isPresent())
            return clustId.get().asSymbolTruncatedToNBits(clusterLevels);
        else
            return ImmutableList.of();
    }

    public static List<Symbol> getGoodClusters(final Symbol word) {
        if (clusters == null) getClusters();

        if (!clusterWordMin.isPresent() || !clusterWordMax.isPresent()) {
            return getClusters(word);
        }

        final Optional<Cluster> cluster = clusters.getClusterForWord(word);

        final ImmutableList.Builder<Symbol> clustersBuilder = ImmutableList.builder();

        if (cluster.isPresent()) {
            for (final int level : clusterLevels) {
                if (cluster.get().bits() >= level) {
                    final Collection<Symbol> words = clusters.getWords(cluster.get(), level);
                    if (words.size() > clusterWordMin.get() && words.size() < clusterWordMax.get()) {
                        clustersBuilder.add(cluster.get().asSymbolTruncatedToNBits(level));
                    }
                }
            }
        }
        return clustersBuilder.build();
    }

    public static Collection<Symbol> getWords(final Symbol cluster) {
        return getClusters().getWords(Cluster.fromString(cluster.toString()));
    }
}
