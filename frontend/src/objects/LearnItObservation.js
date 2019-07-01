export default class LearnItObservation {
	constructor(key, originalObjJson, displayableStr, originalScoreTable) {
		this.key = key;
		this.originalObjJson = originalObjJson;
		this.displayableStr = displayableStr;
		this.originalScoreTable = originalScoreTable;
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
}