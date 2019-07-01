package com.bbn.akbc.neolearnit.processing.postpruning;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.mappings.filters.PatternMatchFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.processing.AbstractStage;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.server.SimpleServer;
import com.bbn.akbc.neolearnit.server.handlers.PostPruningHandler;

import java.io.IOException;
import java.util.Collection;

public class PostPruner extends AbstractStage<PostPruningInformation> {

	private final int numberOfInstances;
	private int port;

	public PostPruner(TargetAndScoreTables data) {
		this(data, LearnItConfig.getInt("post_pruning_instances"), 0);
	}

	public PostPruner(TargetAndScoreTables data, int port) {
		this(data,LearnItConfig.getInt("post_pruning_instances"),port);
	}

	public PostPruner(TargetAndScoreTables data, int numberOfInstances, int port) {
		super(data);
		this.numberOfInstances = numberOfInstances;
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public PostPruningInformation reduceInformation(Collection<PostPruningInformation> inputs) {
		PostPruningInformation.Builder builder = new PostPruningInformation.Builder(numberOfInstances);
		for (PostPruningInformation info : inputs) {
			builder.withInfo(info);
		}
		return builder.build();
	}

	@Override
	public void runStage(PostPruningInformation input) {
		System.out.println("Getting the most complete counts for all patterns");
		for (LearnitPattern pattern : input.getAllRecordedPatterns()) {
            if (pattern.isCompletePattern()) {
                data.getPatternScores().getScore(pattern).setFrequency(input.getPatternCounts().count(pattern));
            }
		}

		PostPruningHandler handler = new PostPruningHandler(data,input);
		try {
			new SimpleServer(handler, "html/pruning.html", port).run();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Mappings applyStageMappingsFilter(Mappings mappings) {
		return new PatternMatchFilter(data.getPatternScores().getFrozen()).makeFiltered(mappings);
	}

	@Override
	public PostPruningInformation processFilteredMappings(Mappings mappings) {
		PostPruningInformation.Builder builder = new PostPruningInformation.Builder(numberOfInstances);
		for (LearnitPattern pattern : mappings.getAllPatterns()) {
			for (InstanceIdentifier id : mappings.getInstancesForPattern(pattern)) {
				try {
					builder.withPatternMatch(data.getTarget(), pattern, id);
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Failed to get instance "+id);
				}
			}
		}
		return builder.build();
	}

	@Override
	public Class<PostPruningInformation> getInfoClass() {
		return PostPruningInformation.class;
	}




}
