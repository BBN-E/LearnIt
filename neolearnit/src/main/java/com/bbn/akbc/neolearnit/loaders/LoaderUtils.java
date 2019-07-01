package com.bbn.akbc.neolearnit.loaders;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class LoaderUtils {

	public static Collection<BilingualDocTheory> loadRegularBilingualFileList(File filelist) throws IOException {
		BilingualDocTheoryInstanceLoader loader = new BilingualDocTheoryInstanceLoader(
				TargetFactory.makeFakeTarget(),new InstanceObservers.Builder().build(),new InstanceObservers.Builder().build(), true);

		loadRegularBilingualFileList(filelist, loader);
		return loader.getLoadedBilingualDocTheories().values();
	}

	public static void loadRegularBilingualFileList(File filelist, final Loader<BilingualDocTheory> loader) throws IOException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();

		final ImmutableList<String> lines = Files.asCharSource(filelist, Charsets.UTF_8).readLines();
		for(final String line : lines) {
			threads.add(new Callable<Boolean>() {

				@Override
				public Boolean call() {
					System.out.println("Processing "+line+"...");

					try {
						BilingualDocTheory bidoc = BilingualDocTheory.fromRegularString(line);
						loader.load(bidoc);
						return true;
					} catch (IOException e) {
						System.err.println("Failed to process "+line);
						e.printStackTrace();
						return false;
					}
				}
			});

		}

		runThreaded(threads);
	}

	public static void loadVariantBilingualFileList(File filelist, final Loader<BilingualDocTheory> loader) throws IOException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();

		//read pairs of lines
		BufferedReader br = new BufferedReader(new FileReader(filelist));
		String line;
		while ((line = br.readLine()) != null) {
			if (!br.ready()) continue;

			final String curLine = line;
			final String curLine2 = br.readLine();

			threads.add(new Callable<Boolean>() {

				@Override
				public Boolean call() {
					System.out.println("Processing "+curLine.split(" ")[0]+"...");
					try {

						BilingualDocTheory bidoc = BilingualDocTheory.fromVariantStyleString(curLine,curLine2);
						loader.load(bidoc);
						return true;
					} catch (IOException e) {
						System.err.println("Failed to process "+curLine);
						e.printStackTrace();
						return false;
					}
				}
			});

		}
		br.close();

		runThreaded(threads);
	}

	public static Collection<DocTheory> loadFileList(File filelist) throws IOException {
		MonolingualDocTheoryInstanceLoader loader = new MonolingualDocTheoryInstanceLoader(
				TargetFactory.makeFakeTarget(),new InstanceObservers.Builder().build(),new InstanceObservers.Builder().build(), true);

		loadFileList(filelist, loader);
		return loader.getLoadedDocTheories().values();
	}


	public static void loadFileList (File fileList, Loader<DocTheory> loader) throws IOException {
        loadFileList(FileUtils.loadFileList(fileList), loader);
	}


    public static void loadFileList(List<File> fileList, final Loader<DocTheory> loader) throws IOException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();

		// final SerifXMLLoader serifxmlLoader =
		//		LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false)?
		//			new SerifXMLLoader.Builder().allowSloppyOffsets().build():
		//				SerifXMLLoader.createFrom(LearnItConfig.params());

		final SerifXMLLoader serifxmlLoader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();

		for (final File serifxml : fileList) {

			threads.add(new Callable<Boolean>() {

				@Override
				public Boolean call() {

					System.out.println("Processing "+serifxml.toString()+"...");

					try {
						loader.load(serifxmlLoader.loadFrom(serifxml));
						return true;

					} catch (IOException e) {
						System.err.println("Failed to process "+serifxml.toString());
						e.printStackTrace();
						return false;
					}
				}
			});

		}


		runThreaded(threads);
	}

	public static void loadSerifXMLString(String serifxmlString,final Loader<DocTheory> loader) throws IOException{
		final SerifXMLLoader serifxmlLoader =
				LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false)?
						new SerifXMLLoader.Builder().allowSloppyOffsets().build():
						SerifXMLLoader.createFrom(LearnItConfig.params());
		loader.load(serifxmlLoader.loadFromString(serifxmlString));
	}


	public static <T> void runThreaded(List<Callable<T>> tasks) {

		int CONCURRENCY = LearnItConfig.getInt("loader_concurrency");

		ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);
		try {
			Collection<Future<T>> results = service.invokeAll(tasks);
			for (Future<T> result : results) {
				result.get(); //fetch any errors burrowed inside
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		service.shutdown();
	}

	public static <T> void runNotThreaded(List<Callable<T>> threads) {
		for (Callable<T> t : threads) {
			try {
				t.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
