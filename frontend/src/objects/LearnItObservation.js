export default class LearnItObservation {
	constructor(key, originalObjJson, IDStr, displayableStr, originalScoreTable) {
		this.key = key; // This is used for frontend. DO NOT SERVE OTHER PURPOSE
		this.originalObjJson = originalObjJson;
		this.displayableStr = displayableStr;
		this.originalScoreTable = originalScoreTable;
		this.IDStr = IDStr;
		this.localLabel = null;
	}
	toString() {
		return this.displayableStr;
	}
	getOriginalObjJson() {
		return this.originalObjJson;
	}
	getOriginalScoreTable() {
		return this.originalScoreTable;
	}
	getKey() {
		return this.key;
	}
	toIDString() {
		return this.IDStr;
	}
	getLocalLabel() {
		return this.localLabel;
	}
	setLocalLabel(label) {
		this.localLabel = label;
	}
}