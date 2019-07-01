package com.bbn.akbc.neolearnit.mappings.groups;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.logging.Glogger;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToPatternMapping;
import com.bbn.akbc.neolearnit.mappings.impl.InstanceToSeedMapping;
import com.bbn.akbc.neolearnit.observations.pattern.ComboPattern;
import com.bbn.akbc.neolearnit.observations.pattern.LearnitPattern;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.Restriction.RestrictionFactory;
import com.bbn.akbc.neolearnit.observations.pattern.restriction.RestrictionSuite;
import com.bbn.akbc.neolearnit.observations.seed.Seed;
import com.bbn.akbc.neolearnit.scoring.TargetAndScoreTables;
import com.bbn.akbc.neolearnit.storage.MapStorage;
import com.bbn.akbc.neolearnit.storage.StorageUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * This is the default collection of mappings used in the system
 * Adding new mappings to here will ensure they are available in the regular
 * LearnIt scoring and selection process.
 *
 * @author mshafir
 *
 */
public class Mappings {

	private final InstanceToSeedMapping instance2Seed;
	private final InstanceToPatternMapping instance2Pattern;

	@JsonProperty
	private MapStorage<InstanceIdentifier, Seed> seedInstanceMap() {
		return instance2Seed.getStorage();
	}

	@JsonProperty
	private MapStorage<InstanceIdentifier, LearnitPattern> patternInstanceMap() {
		return instance2Pattern.getStorage();
	}

	@JsonCreator
	public Mappings(
			@JsonProperty("seedInstanceMap") MapStorage<InstanceIdentifier, Seed> seed2Instance,
			@JsonProperty("patternInstanceMap") MapStorage<InstanceIdentifier, LearnitPattern> pattern2Instance) {
		this.instance2Seed = new InstanceToSeedMapping(seed2Instance);
		this.instance2Pattern =  new InstanceToPatternMapping(pattern2Instance);
	}

	public Mappings(InstanceToSeedMapping seed2Instance,
			InstanceToPatternMapping pattern2Instance) {
		this.instance2Seed = seed2Instance;
		this.instance2Pattern = pattern2Instance;
	}

	public int size() {
		return instance2Seed.getStorage().size()+instance2Pattern.getStorage().size();
	}

	public InstanceToSeedMapping getInstance2Seed() {
		return instance2Seed;
	}

	public InstanceToPatternMapping getInstance2Pattern() {
		return instance2Pattern;
	}

	// LETS JUST ADD IN A TON OF UTILITY FUNCTIONS HERE

	public Multiset<LearnitPattern> getAllPatterns() {
		return this.instance2Pattern.getAllPatterns();
	}

	public Multiset<Seed> getAllSeeds() {
		return this.instance2Seed.getAllSeeds();
	}

	public Collection<LearnitPattern> getPatternsForInstance(InstanceIdentifier id) {
		return this.instance2Pattern.getPatterns(id);
	}

	public static double getConfidenceSum(Iterable<InstanceIdentifier> ids) {
		double result = 0.0;
		for (InstanceIdentifier id : ids) {
			result += id.getConfidence();
		}
		return result;
	}

	public Set<InstanceIdentifier> getPatternInstances() {
	    	return this.instance2Pattern.getAllInstances().elementSet();
	}

	public Set<InstanceIdentifier> getSeedInstances() {
		Set<InstanceIdentifier> matchedInstances = new HashSet<InstanceIdentifier>();
		for (Seed s : getAllSeeds()) {
			matchedInstances.addAll(getInstancesForSeed(s));
		}
		return matchedInstances;
	}

	public long getInstanceCount() {
		return this.instance2Pattern.getAllInstances().elementSet().size();
	}

	public long getKnownInstanceCount(TargetAndScoreTables data) {
		double scoreSum = 0;
		System.out.println(data.getSeedScores().getFrozen().size()+" frozen seeds for known instance count collection.");
		for (Seed s : data.getSeedScores().getFrozen()) {
			scoreSum += data.getSeedScores().getScore(s).getScore()*this.getInstancesForSeed(s).size();
		}
		return Math.round(scoreSum);
	}

	public Collection<Seed> getSeedsForInstance(InstanceIdentifier id) {
		return this.instance2Seed.getSeeds(id);
	}

	public Collection<InstanceIdentifier> getInstancesForPattern(LearnitPattern pattern) {
		return this.instance2Pattern.getInstances(pattern);
	}

	public Collection<ComboPattern> getRestrictedPatternVariants(TargetAndScoreTables data) {
		return getRestrictedPatternVariants(data, this.getAllPatterns().elementSet());
	}

	public Collection<ComboPattern> getRestrictedPatternVariants(TargetAndScoreTables data, Set<LearnitPattern> patternsToRestrict) {
		Collection<ComboPattern> restrictionPatterns = new HashSet<ComboPattern>();
		Collection<RestrictionFactory> factories = RestrictionSuite.getRestrictionFactories(data, this);

		for (LearnitPattern p : patternsToRestrict) {
			if (p.isProposable(data.getTarget()) && !data.getPatternScores().isKnownFrozen(p)) { //don't double restrict
				for (RestrictionFactory factory : factories) {
					for (Restriction res : factory.getRestrictions(p)) {
						restrictionPatterns.add(new ComboPattern(p, res));
					  Glogger.logger().debug("new restricted pattern: " + (new ComboPattern(p, res)).toIDString());
					}
				}
			}
		}
		return restrictionPatterns;
	}

	public Collection<InstanceIdentifier> getInstancesForSeed(Seed seed) {
		return this.instance2Seed.getInstances(seed);
	}

	public Multiset<LearnitPattern> getPatternsForInstances(Iterable<InstanceIdentifier> ids) {
		Multiset<LearnitPattern> result = HashMultiset.<LearnitPattern>create();
		for (InstanceIdentifier id : ids) {
			result.addAll(getPatternsForInstance(id));
		}
		return result;
	}

	public Multiset<Seed> getSeedsForInstances(Iterable<InstanceIdentifier> ids) {
		Multiset<Seed> result = HashMultiset.<Seed>create();
		for (InstanceIdentifier id : ids) {
			result.addAll(getSeedsForInstance(id));
		}
		return result;
	}

	public Multiset<LearnitPattern> getPatternsForSeed(Seed seed) {
		return getPatternsForInstances(this.instance2Seed.getInstances(seed));
	}

	public Multiset<Seed> getSeedsForPattern(LearnitPattern pattern) {
		return getSeedsForInstances(this.instance2Pattern.getInstances(pattern));
	}

	public Multiset<Seed> getSeedsForPatterns(Set<LearnitPattern> patterns) {
		Multiset<Seed> result = HashMultiset.<Seed>create();
		for (LearnitPattern pattern :patterns) {
			for (InstanceIdentifier id : getInstance2Pattern().getInstances(pattern)) {
				result.addAll(getSeedsForInstance(id));
			}
		}
		return result;
	}

	public Multiset<LearnitPattern> getPatternsForSeeds(Set<Seed> seeds) {
		Multiset<LearnitPattern> result = HashMultiset.<LearnitPattern>create();
		for (Seed seed :seeds) {
			for (InstanceIdentifier id : getInstance2Seed().getInstances(seed)) {
				result.addAll(getPatternsForInstance(id));
			}
		}
		return result;
	}

	public Mappings getUpdatedMappings(TargetAndScoreTables data) {
		Collection<ComboPattern> comboPatterns = data.getPatternScores().getComboPatterns(false);
		return comboPatterns.isEmpty() ? this :
			this.getUpdatedMappingsWithComboPatterns(comboPatterns);
	}

	public Mappings getAllPatternUpdatedMappings(TargetAndScoreTables data) {
		Collection<ComboPattern> comboPatterns = data.getPatternScores().getComboPatterns(true);
		return comboPatterns.isEmpty() ? this :
			this.getUpdatedMappingsWithComboPatterns(comboPatterns);
	}

	public Mappings getUpdatedMappingsWithComboPatterns(Collection<ComboPattern> comboPatterns) {
		MapStorage<InstanceIdentifier,LearnitPattern> storage = this.getInstance2Pattern().getStorage();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> storageBuilder = storage.newBuilder();
	    storageBuilder.putAll(storage);
	    for (ComboPattern comboPattern : comboPatterns) {
	        for (InstanceIdentifier id : comboPattern.getInstances(this)) {
	            storageBuilder.put(id,comboPattern);
	        }
	    }
	    return new Mappings(this.getInstance2Seed(), new InstanceToPatternMapping(storageBuilder.build()));
	}

	public Mappings getVersionForInitializationStep() {
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> storageBuilder = this.getInstance2Pattern().getStorage().newBuilder();

		for (LearnitPattern pattern : this.getAllPatterns().elementSet()) {
			if (pattern.getInitializationVersion().isPresent()) {
				for (InstanceIdentifier id : this.getInstancesForPattern(pattern)) {
					storageBuilder.put(id, pattern.getInitializationVersion().get());
				}
			}
		}
		return new Mappings(this.getInstance2Seed(), new InstanceToPatternMapping(storageBuilder.build()));
	}

	public Mappings makeWithoutIncompletePatterns() {
		MapStorage<InstanceIdentifier,LearnitPattern> storage = this.getInstance2Pattern().getStorage();
		MapStorage.Builder<InstanceIdentifier,LearnitPattern> storageBuilder = storage.newBuilder();
		for (LearnitPattern pattern : this.getAllPatterns().elementSet()) {
			if (pattern.isCompletePattern()) {
				for (InstanceIdentifier id : this.getInstancesForPattern(pattern)) {
					storageBuilder.put(id, pattern);
				}
			}
		}
		return new Mappings(this.getInstance2Seed(), new InstanceToPatternMapping(storageBuilder.build()));
	}

	// SERIALIZE/DESERIALIZE

	public void serialize(File file, boolean compress) throws JsonGenerationException, JsonMappingException, IOException {
		OutputStream out = compress ? new DeflaterOutputStream(new FileOutputStream(file)) : new FileOutputStream(file);
		StorageUtils.getDefaultMapper().writeValue(out, this);
		out.close();
	}

	public static Mappings deserialize(File file, boolean compress) throws JsonParseException, JsonMappingException, IOException {
		InputStream in;
		if (compress) {
			in = new InflaterInputStream(new FileInputStream(file));
		} else {
			in = new FileInputStream(file);
		}
		Mappings result = StorageUtils.getDefaultMapper().readValue(in, Mappings.class);
		in.close();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Mappings mappings = (Mappings) o;

		if (!instance2Seed.equals(mappings.instance2Seed)) return false;
		return instance2Pattern.equals(mappings.instance2Pattern);
	}

	@Override
	public int hashCode() {
		int result = instance2Seed.hashCode();
		result = 31 * result + instance2Pattern.hashCode();
		return result;
	}
}
