package com.bbn.akbc.neolearnit.processing;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.resources.SeedGroups;
import com.bbn.akbc.neolearnit.common.resources.SeedSimilarity;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.processing.preprocessing.Preprocessor;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public abstract class AbstractStage<T extends PartialInformation> implements Stage<T> {

	protected final TargetAndScoreTables data;

	public AbstractStage(TargetAndScoreTables data) {
		this.data = data;

        if (!(this instanceof Preprocessor) && LearnItConfig.optionalParamTrue("use_seed_groups"))
            SeedGroups.load(data);
	}

	public Mappings applyGeneralMappingsFilter(Mappings mappings) {
		return new NormalizeSeedsFilter(data.getTarget()).makeFiltered(mappings);
	}

	public abstract Mappings applyStageMappingsFilter(Mappings mappings);

	public abstract T processFilteredMappings(Mappings mappings);

	public abstract Class<T> getInfoClass();


	public TargetAndScoreTables getData() {
		return data;
	}

	@Override
	public T processMappings(Mappings mappings) {
		return this.processFilteredMappings(
				applyStageMappingsFilter(
					applyGeneralMappingsFilter(mappings.getUpdatedMappings(data))));
	}

	@Override
	public T processMappingFiles(Iterable<File> files) {

        if (!(this instanceof Preprocessor)) {
            //Doing this here because every stage does it. Doing it in parallel per stage saved a bit
            //of time, but it was also causing the system to hang periodically, which was not worth it
            SeedSimilarity.load(data);
        }

		int CONCURRENCY = LearnItConfig.getInt("loader_concurrency");

		List<Callable<T>> calls = new ArrayList<Callable<T>>();

		for (final File input : files) {

			calls.add(new StageCallable<T>(input,this));

		}

		ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);

		List<T> results = new ArrayList<T>();

		try {
			List<Future<T>> futureResults = service.invokeAll(calls);

			for (Future<T> future : futureResults) {
				try {
					results.add(future.get());
				} catch (ExecutionException ex) {
					ex.printStackTrace();
				}
			}

		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		service.shutdown();

	  Glogger.logger().debug("Merging results...");

		return this.reduceInformation(results);
	}

	@Override
	public T processPartialInfoFiles(Iterable<File> files) {

		List<T> infos = new ArrayList<T>();
	  Glogger.logger().debug("Processing " + ImmutableList.copyOf(files).size() + " files...");
        int count = 0;
		for (final File input : files) {
		  Glogger.logger().debug("files: " + count + ", " + input.getAbsolutePath());
			try {
				infos.add(StorageUtils.deserialize(input, getInfoClass(), true));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
            ++count;
		}
		return this.reduceInformation(infos);
	}

	@Override
	public void runOnFile(File input) throws IOException {
		T info = StorageUtils.deserialize(input, getInfoClass(), true);
		this.runStage(info);
	}

	@Override
	public void runOnMappings(Mappings mappings) {
		this.runStage(this.processMappings(mappings));
	}

	public static class StageCallable<T extends PartialInformation> implements Callable<T> {

		private final File file;
		private final AbstractStage<T> stage;

		public StageCallable(File file, AbstractStage<T> stage) {
			this.file = file;
			this.stage = stage;
		}

		@Override
		public T call() throws IOException {
		  Glogger.logger().debug(
			    "Extracting " + stage.getInfoClass().getName() + " from " + file
				+ "...");
			Mappings info = StorageUtils.deserialize(file, Mappings.class, true);
			return stage.processMappings(info);
		}

	}

}
