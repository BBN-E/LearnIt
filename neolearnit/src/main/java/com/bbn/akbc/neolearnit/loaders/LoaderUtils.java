package com.bbn.akbc.neolearnit.loaders;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.common.bilingual.BilingualDocTheory;
import com.bbn.akbc.neolearnit.common.targets.TargetFactory;
import com.bbn.akbc.neolearnit.common.util.TabularPathListsConverter;
import com.bbn.akbc.neolearnit.loaders.impl.BilingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.loaders.impl.MonolingualDocTheoryInstanceLoader;
import com.bbn.akbc.neolearnit.observers.InstanceObservers;
import com.bbn.akbc.neolearnit.util.GeneralUtils;
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
import java.util.*;
import java.util.concurrent.*;

public class LoaderUtils {

	public static Collection<BilingualDocTheory> loadRegularBilingualFileList(File filelist) throws IOException,ExecutionException,InterruptedException {
		BilingualDocTheoryInstanceLoader loader = new BilingualDocTheoryInstanceLoader(
				TargetFactory.makeFakeTarget(),new InstanceObservers.Builder().build(),new InstanceObservers.Builder().build(), true);

		loadRegularBilingualFileList(filelist, loader);
		return loader.getLoadedBilingualDocTheories().values();
	}

	public static void loadRegularBilingualFileList(File filelist, final Loader<BilingualDocTheory> loader) throws IOException,ExecutionException,InterruptedException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();

		final ImmutableList<String> lines = Files.asCharSource(filelist, Charsets.UTF_8).readLines();
		for(final String line : lines) {
			threads.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					System.out.println("Processing "+line+"...");

					try {
						BilingualDocTheory bidoc = BilingualDocTheory.fromRegularString(line);
						loader.load(bidoc);
						return true;
					} catch (Exception e) {
						System.err.println("Failed to process "+line);
						throw e;
					}
				}
			});

		}

		runThreaded(threads);
	}

	public static void loadTabularPathLists(String listPath, final Loader<BilingualDocTheory> loader) throws IOException,ExecutionException,InterruptedException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();
		Map<String, Map<String, String>> docIdToEntries = TabularPathListsConverter.parseSingleTabularList(listPath);
		for (final Map.Entry<String, Map<String, String>> ens : docIdToEntries.entrySet()) {
			threads.add(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					System.out.println("Processing " + ens.getKey() + "...");
					try{
						BilingualDocTheory bidoc = BilingualDocTheory.fromTabularPathLists(ens.getKey(), ens.getValue());
						loader.load(bidoc);
						return true;
					}
					catch (Exception e) {
						System.err.println("Failed to process "+ens.getKey());
						throw e;
					}
				}
			});

		}
		runThreaded(threads);
	}

	public static void loadBilingualDocTheoryList(List<BilingualDocTheory> bilingualDocTheoryList, final Loader<BilingualDocTheory> loader) throws IOException,ExecutionException,InterruptedException{
		List<Callable<Boolean>> threads = new ArrayList<>();
		for(final BilingualDocTheory bilingualDocTheory: bilingualDocTheoryList){
			threads.add(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					System.out.println("Processing " + bilingualDocTheory.toString() + "...");
					try{
						loader.load(bilingualDocTheory);
						return true;
					}
					catch (Exception e) {
						System.err.println("Processing " + bilingualDocTheory.toString() + "...");
						throw e;
					}
				}
			});
		}
	}


	public static void loadVariantBilingualFileList(File filelist, final Loader<BilingualDocTheory> loader) throws IOException,ExecutionException,InterruptedException {
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
				public Boolean call() throws Exception{
					System.out.println("Processing "+curLine.split(" ")[0]+"...");
					try {

						BilingualDocTheory bidoc = BilingualDocTheory.fromVariantStyleString(curLine,curLine2);
						loader.load(bidoc);
						return true;
					} catch (Exception e) {
						System.err.println("Failed to process "+curLine);
						throw e;
					}
				}
			});

		}
		br.close();

		runThreaded(threads);
	}

	public static Collection<DocTheory> loadFileList(File filelist) throws IOException,ExecutionException,InterruptedException {
		MonolingualDocTheoryInstanceLoader loader = new MonolingualDocTheoryInstanceLoader(
				TargetFactory.makeFakeTarget(),new InstanceObservers.Builder().build(),new InstanceObservers.Builder().build(), true);

		loadFileList(filelist, loader);
		return loader.getLoadedDocTheories().values();
	}


	public static void loadFileList (File fileList, Loader<DocTheory> loader) throws ExecutionException,IOException,InterruptedException {
        loadFileList(FileUtils.loadFileList(fileList), loader);
	}


    public static void loadFileList(List<File> fileList, final Loader<DocTheory> loader) throws ExecutionException,InterruptedException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();

		// final SerifXMLLoader serifxmlLoader =
		//		LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false)?
		//			new SerifXMLLoader.Builder().allowSloppyOffsets().build():
		//				SerifXMLLoader.createFrom(LearnItConfig.params());

		final SerifXMLLoader serifxmlLoader = new SerifXMLLoader.Builder().allowSloppyOffsets().build();

		for (final File serifxml : fileList) {

			threads.add(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception{

					System.out.println("Processing "+serifxml.toString()+"...");

					try {
						loader.load(serifxmlLoader.loadFrom(serifxml));
						return true;

					} catch (Exception e) {
						System.err.println("Failed to process "+serifxml.toString());
						throw e;
					}
				}
			});

		}


		runThreaded(threads);
	}

	public static void loadDocTheroyList(List<DocTheory> docTheoryList, final Loader<DocTheory> loader) throws ExecutionException,InterruptedException {
		List<Callable<Boolean>> threads = new ArrayList<Callable<Boolean>>();
		for (final DocTheory docTheory : docTheoryList) {
			threads.add(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception{

					System.out.println("Processing "+docTheory.docid().toString()+"...");

					try {
						loader.load(docTheory);
						return true;

					} catch (Exception e) {
						System.err.println("Failed to process "+docTheory.docid().toString());
						throw e;
					}
				}
			});

		}
		runThreaded(threads);
	}

	public static Set<DocTheory> resolvedDocTheoryFromPathList(Collection<String> serifList) throws InterruptedException, ExecutionException {
		Map<String, DocTheory> docPathMap = new ConcurrentHashMap<>();
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (String docPath : serifList) {
			tasks.add(() -> {
				final SerifXMLLoader serifxmlLoader =
						LearnItConfig.params().getOptionalBoolean("load_serifxml_with_sloppy_offsets").or(false) ?
								new SerifXMLLoader.Builder().allowSloppyOffsets().build() :
								SerifXMLLoader.createFrom(LearnItConfig.params());
				docPathMap.put(docPath, serifxmlLoader.loadFrom(new File(docPath)));
				return true;
			});
		}
		LoaderUtils.runThreaded(tasks);
		return new HashSet<>(docPathMap.values());
	}

	public static class BiDocTheorySingleDocumentWorker implements Callable<Boolean> {
		final String sourceLang;
		final String sourceLangPath;
		final String targetLang;
		final String targetLangPath;
		final String alignmentPath;
		BilingualDocTheory ret = null;

		public BiDocTheorySingleDocumentWorker(String sourceLang, String sourceLangPath,String targetLang,String targetLangPath,String alignmentPath) {
			this.sourceLang = sourceLang;
			this.sourceLangPath = sourceLangPath;
			this.targetLang = targetLang;
			this.targetLangPath = targetLangPath;
			this.alignmentPath = alignmentPath;
		}

		@Override
		public Boolean call() throws Exception {
			ret = BilingualDocTheory.fromPaths(this.sourceLang,this.sourceLangPath,this.targetLang,this.targetLangPath,this.alignmentPath);
			return true;
		}
	}

	public static Set<BilingualDocTheory> resolveBilingualDocTheoryFromBiEntries(Collection<Map<String, String>> biEntries) throws ExecutionException, InterruptedException {
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for(Map<String,String> biEntry:biEntries){
			String lang1 = LearnItConfig.getList("languages").get(0);
			String lang2 = LearnItConfig.getList("languages").get(1);
			tasks.add(new BiDocTheorySingleDocumentWorker(lang1, biEntry.get(lang1), lang2, biEntry.get(lang2), biEntry.get("alignment")));
		}
		runThreaded(tasks);
		Set<BilingualDocTheory> ret = new HashSet<>();
		for(Callable<Boolean> t: tasks){
			BiDocTheorySingleDocumentWorker biDocWorker = (BiDocTheorySingleDocumentWorker)t;
			assert biDocWorker.ret != null;
			ret.add(biDocWorker.ret);
		}
		return ret;
	}

	public static <T> void runThreaded(List<Callable<T>> tasks) throws ExecutionException,InterruptedException{

		int CONCURRENCY = LearnItConfig.getInt("loader_concurrency");
		ExecutorService service = Executors.newFixedThreadPool(CONCURRENCY);
		Collection<Future<T>> results = service.invokeAll(tasks);
		for (Future<T> result : results) {
			result.get(); //fetch any errors burrowed inside
		}
		service.shutdown();
	}

	public static <T> void runNotThreaded(List<Callable<T>> threads) throws Exception {
		for (Callable<T> t : threads) {
			t.call();
		}
	}

}
