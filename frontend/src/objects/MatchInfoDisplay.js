const MatchInfoDisplay = class {
	constructor(instanceIdentifier, matchInfoDisplay, annotation, relationType) {
		this.instanceIdentifier = instanceIdentifier;
		this.matchInfoDisplay = matchInfoDisplay;
		this.annotation = annotation
		this.relationType = relationType;
	}
	toString() {
		return this.instanceHTML;
	}
	static fromJson(jsonObj) {
		return new MatchInfoDisplay(jsonObj.instanceIdentifier, jsonObj.matchInfoDisplay, jsonObj.annotation, jsonObj.relationType);
	}
}

export default {
	loadInstanceIdentifierTable: (data) => {
		const ret = {};
		const maxLength = data.InstanceIdentifierSet.length;
		for (let i = 0; i < maxLength; ++i) {
			ret[JSON.stringify(data.InstanceIdentifierSet[i])] = {
				instanceIdentifier: data.InstanceIdentifierSet[i],
				matchInfoDisplay: JSON.parse(data.HTMLStringSet[i]),
				annotation: data.AnnotationSet[i],
				relationType: data.RelationNameSet[i]
			};
		}
		return ret;
	}
}