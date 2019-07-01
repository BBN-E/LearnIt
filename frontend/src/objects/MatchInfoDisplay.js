const Instance = class {
	constructor(instanceIdentifier, htmlStr, annotation, relationType) {
		this.instanceIdentifier = instanceIdentifier;
		this.htmlStr = htmlStr;
		this.annotation = annotation
		this.relationType = relationType;
	}
	toString() {
		return this.instanceHTML;
	}
	static fromJson(jsonObj) {
		return new Instance(jsonObj.instanceIdentifier, jsonObj.htmlStr, jsonObj.annotation, jsonObj.relationType);
	}
}

export default {
	Instance: Instance,
	loadInstanceIdentifierTable: (data) => {
		const ret = {};
		const maxLength = data.InstanceIdentifierSet.length;
		for (let i = 0; i < maxLength; ++i) {
			ret[JSON.stringify(data.InstanceIdentifierSet[i])] = {
				instanceIdentifier: data.InstanceIdentifierSet[i],
				htmlStr: data.HTMLStringSet[i],
				annotation: data.AnnotationSet[i],
				relationType: data.RelationNameSet[i]
			};
		}
		return ret;
	}
}