package com.bbn.akbc.neolearnit.observations.pattern;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.types.EntityType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class EntityTypeContent implements RegexableContent {

	private final EntityType etype;

	@JsonProperty(value="etype")
	private String getType() {
		return etype.toString();
	}

	@JsonCreator
	private static EntityTypeContent from(@JsonProperty("etype") String etype) {
		return new EntityTypeContent(etype);
	}

	public EntityTypeContent(EntityType etype) {
		this.etype = etype;
	}

	public EntityTypeContent(String etype) {
		this.etype = EntityType.of(Symbol.from(etype));
	}

	@Override
	public Pattern getPattern() {
		List<EntityType> typeList = new ArrayList<EntityType>();
		typeList.add(etype);
		return (new MentionPattern.Builder()).withAceTypes(typeList).build();
	}

	@Override
	public String toString() {
		return getPattern().toString();
	}

	@Override
	public String toPrettyString() {
		return "["+etype.toString()+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etype == null) ? 0 : etype.name().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityTypeContent other = (EntityTypeContent) obj;
		if (etype == null) {
			if (other.etype != null)
				return false;
		} else if (!etype.name().equals(other.etype.name()))
			return false;
		return true;
	}

}
