package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.akbc.neolearnit.evaluation.pelf.ElfIndividual;
import com.bbn.serif.apf.APFEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityAnswer {

	@JsonProperty
	private final String entityId;

	@JsonCreator
	private EntityAnswer(@JsonProperty("entityId") String entityId) {
		this.entityId = entityId;
	}

	public String getEntityId() {
		return entityId;
	}

	public static EntityAnswer fromAPFEntity(APFEntity apfEntity) {
		return new EntityAnswer(apfEntity.getID());
	}

	public static EntityAnswer fromElfIndividual(ElfIndividual elfIndividual) {
		return new EntityAnswer(elfIndividual.getId());
	}

	@Override
	public String toString() {
		return "EntityAnswer [entityId=" + entityId + "]";
	}

}
