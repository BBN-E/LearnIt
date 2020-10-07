package com.bbn.akbc.neolearnit.mappings.callables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.bbn.akbc.neolearnit.mappings.filters.NormalizeSeedsFilter;
import com.bbn.akbc.neolearnit.mappings.groups.Mappings;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Multiset;

public abstract class AbstractMappingExtractorCallable<T> implements Callable<Multiset<T>> {

	protected final TargetAndScoreTables data;
	private final File file;

	private static Map<String,Mappings> mappingsCache = new ConcurrentHashMap<String,Mappings>();

	public AbstractMappingExtractorCallable(File file, TargetAndScoreTables data) throws FileNotFoundException {
		this.file = file;
		this.data = data;
	}

	public abstract Multiset<T> getInstances(Mappings m) throws IOException;

	public abstract Set<LearnitPattern> getPatternsToRestrict(Mappings m);

	private static Mappings getMappings(File f) throws JsonParseException, JsonMappingException, IOException {
		if (mappingsCache.containsKey(f.toString())) {
			return mappingsCache.get(f.toString());
		} else {
			Mappings result = Mappings.deserialize(f, true);
			if (mappingsCache.size() < 5)
				mappingsCache.put(f.toString(), result);
			return result;
		}
	}

	@Override
	public Multiset<T> call() throws Exception {
		Mappings m = getMappings(file);
		System.out.println("Loaded "+file+", getting instances...");

		Collection<ComboPattern> restrictionPatterns = m.getRestrictedPatternVariants(data, getPatternsToRestrict(m));

		Mappings mappingsToUse;
		if (restrictionPatterns.isEmpty()) {
			mappingsToUse = m;
		} else {
			System.out.println("Applying "+restrictionPatterns.size()+" restrictions...");
		    mappingsToUse = m.getUpdatedMappingsWithComboPatterns(restrictionPatterns);
		}

		// we do this a little later so we can take advantage of head text during restrictions
		mappingsToUse = new NormalizeSeedsFilter(data.getTarget()).makeFiltered(mappingsToUse);

		return getInstances(mappingsToUse);
	}

}
