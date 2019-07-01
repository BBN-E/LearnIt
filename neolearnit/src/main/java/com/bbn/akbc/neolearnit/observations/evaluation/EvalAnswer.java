package com.bbn.akbc.neolearnit.observations.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bbn.akbc.neolearnit.evaluation.answers.RelationAnswer;
import com.bbn.akbc.neolearnit.observations.LearnItObservation;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public abstract class EvalAnswer extends LearnItObservation {
	@JsonProperty
	private final boolean correct;
	@JsonProperty
	private final List<RelationAnswer> matchedAnnotations;
	private final Set<String> brandyMatches; //for evaluating against P-ELFs

	public EvalAnswer(List<RelationAnswer> matchedAnnotations) {
		this(matchedAnnotations, new HashSet<String>());
	}

	public EvalAnswer(List<RelationAnswer> matchedAnnotations, Set<String> brandyMatches) {
		this.matchedAnnotations = new ArrayList<RelationAnswer>(matchedAnnotations);
		this.correct = !matchedAnnotations.isEmpty();
		this.brandyMatches = brandyMatches;
	}

	public boolean isCorrect() {
		return correct;
	}

	ImmutableMap<String,String> aceSubtype2type = ImmutableMap.<String, String>builder()
			.put("Located", "Physical")  //
			.put("Near", "Physical")  //

			.put("Geographical", "Part-whole")
			.put("Subsidiary", "Part-whole")


			.put("Business", "Personal-Social")
			.put("Family", "Personal-Social")
			.put("Lasting-Personal", "Personal-Social")


			.put("Employment", "ORG-Affiliation")
			.put("Ownership", "ORG-Affiliation")
			.put("Founder", "ORG-Affiliation")
			.put("Student-Alum", "ORG-Affiliation")
			.put("Sports-Affiliation", "ORG-Affiliation")
			.put("Investor-Shareholder", "ORG-Affiliation")
			.put("Membership", "ORG-Affiliation")

			.put("User-Owner-Inventor-Manufacturer", "Agent-Artifact")

			.put("Citizen-Resident-Religion-Ethnicity", "Gen-Affiliation") // GEN-AFF, GPE-AFF
			.put("Org-Location-Origin", "Gen-Affiliation") // GEN-AFF, GPE-AFF

			.build();

	public Optional<Set<String>> getRelationAceType() {
		Set<String> relationTypes = new HashSet<String>();
		for(RelationAnswer relationAnswer : matchedAnnotations) {
			relationTypes.add(aceSubtype2type.get(relationAnswer.getRelationType().trim()));
		}

		if(relationTypes.isEmpty())
			return Optional.absent();
		else
			return Optional.of(relationTypes);
	}

	public List<RelationAnswer> getMatchedAnnotations() {
		return matchedAnnotations;
	}

	/**
	 * This will return an empty set unless you're looking at P-ELFs created from InstanceFinder output!
	 */
	public Set<String> getBrandyMatches() {
		return brandyMatches;
	}

	public synchronized void addBrandyMatch(String brandyPatternString) {
		brandyMatches.add(brandyPatternString);
	}

}
