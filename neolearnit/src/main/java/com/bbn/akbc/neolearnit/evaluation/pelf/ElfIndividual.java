package com.bbn.akbc.neolearnit.evaluation.pelf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;

public class ElfIndividual implements Iterable<ElfIndividualMention> {

	private final String id;
	private final Collection<ElfIndividualMention> mentions;

	public ElfIndividual(String id, Collection<ElfIndividualMention> mentions) {
		this.id = id;
		this.mentions = ImmutableList.copyOf(mentions);
	}

	public String getId() {
		return id;
	}

	public Collection<ElfIndividualMention> getMentions() {
		return mentions;
	}

	public static class Builder {

		private final String id;
		private final Collection<ElfIndividualMention> mentions;

		public Builder(String id) {
			this.id = id;
			this.mentions = new ArrayList<ElfIndividualMention>();
		}

		public ElfIndividual build() {
			return new ElfIndividual(this.id, this.mentions);
		}

		public Builder withAddMention(ElfIndividualMention mention) {
			this.mentions.add(mention);
			return this;
		}
	}

	@Override
	public Iterator<ElfIndividualMention> iterator() {
		return mentions.iterator();
	}

}
