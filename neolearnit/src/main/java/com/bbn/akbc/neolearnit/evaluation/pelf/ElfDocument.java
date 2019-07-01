package com.bbn.akbc.neolearnit.evaluation.pelf;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

public class ElfDocument {

	private final String docid;
	private final String source;
	private final static String version = "2.2";
	private final static String contents = "P-ELF";
	private final static String xmlns = "http://www.bbn.com/MR/ELF";

	private final Collection<ElfRelation> relations;
	private final Collection<ElfIndividual> individuals;

	private ElfDocument(String docid, String source,
			Collection<ElfRelation> relations,
			Collection<ElfIndividual> individuals) {
		this.docid = docid;
		this.source = source;
		this.relations = ImmutableList.copyOf(relations);
		this.individuals = ImmutableList.copyOf(individuals);
	}

	public static class Builder {

		private final String docid;
		private final String source;
		private final Collection<ElfRelation> relations;
		private final Collection<ElfIndividual> individuals;

		public Builder(String docid, String source) {
			this.docid = docid;
			this.source = source;
			relations = new ArrayList<ElfRelation>();
			individuals = new ArrayList<ElfIndividual>();
		}

		public Builder(String docid) {
			this(docid, "Java Converter");
		}

		public ElfDocument build() {
			return new ElfDocument(this.docid,this.source,this.relations,this.individuals);
		}

		public Builder withAddRelation(ElfRelation relation) {
			relations.add(relation);
			return this;
		}

		public Builder withAddIndividual(ElfIndividual individual) {
			individuals.add(individual);
			return this;
		}

		public int numIndividualsSoFar() {
			return individuals.size();
		}

	}

	public static String getVersion() {
		return version;
	}


	public static String getContents() {
		return contents;
	}

	public static String getXmlns() {
		return xmlns;
	}


	public String getDocid() {
		return docid;
	}

	public String getSource() {
		return source;
	}

	public Collection<ElfRelation> getRelations() {
		return relations;
	}

	public Collection<ElfIndividual> getIndividuals() {
		return individuals;
	}

}
