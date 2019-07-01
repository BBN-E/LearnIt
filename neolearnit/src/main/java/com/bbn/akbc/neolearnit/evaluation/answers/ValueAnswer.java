package com.bbn.akbc.neolearnit.evaluation.answers;

import com.bbn.serif.apf.APFValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValueAnswer {

	@JsonProperty
	private final String valueId;

	@JsonCreator
	private ValueAnswer(@JsonProperty("valueId") String valueId) {
		this.valueId = valueId;
	}

	public String getValueId() {
		return valueId;
	}

	public static ValueAnswer fromAPFValue(APFValue apfValue) {
		return new ValueAnswer(apfValue.getID());
	}

	@Override
	public String toString() {
		return "ValueAnswer [valueId=" + valueId + "]";
	}


}
