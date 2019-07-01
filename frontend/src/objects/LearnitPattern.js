import LearnItObservation from '@/objects/LearnItObservation.js';

class normalizedPattern extends LearnItObservation {
	constructor(originalObjJson, originalScoreTable) {
		const key = JSON.stringify(originalObjJson);
		const displayableStr = originalObjJson.toIDString;
		super(key, originalObjJson, displayableStr, originalScoreTable);
	}
	static fromJson(originalObjJson, originalScoreTable) {
		return new normalizedPattern(originalObjJson, originalScoreTable);
	}
}

const sortPatternList = (key, reverse, kvMapping) => (a, b) => {
	const aOriginalScoreTable = kvMapping[a].getOriginalScoreTable();
	const bOriginalScoreTable = kvMapping[b].getOriginalScoreTable();
	const leftKey = (typeof aOriginalScoreTable[key] === 'undefined') ? 0 : aOriginalScoreTable[key];
	const rightKey = (typeof bOriginalScoreTable[key] === 'undefined') ? 0 : bOriginalScoreTable[key];
	if (!reverse) leftKey - rightKey;
	else return rightKey - leftKey;
};

const filterPatternList = (frozen, isGood, kvMapping) => (pattern) => {
	const patternOriginalScoreTable = kvMapping[pattern].getOriginalScoreTable();
	if (!frozen) return !patternOriginalScoreTable.frozen;
	else {
		const precision = (typeof patternOriginalScoreTable['precision'] === 'undefined') ? 0 : patternOriginalScoreTable['precision'];
		if (isGood) return patternOriginalScoreTable.frozen && precision >= 0.5;
		else return patternOriginalScoreTable.frozen && precision < 0.5;
	}
}

export default {
	normalizedPattern: normalizedPattern,
	loadPatternScoreTable: function (scoreTable) {
		const resolvedScoreTable = {};
		for (const idx in scoreTable.entries) {
			const kvPair = scoreTable.entries[idx];
			const newLearnitPattern = normalizedPattern.fromJson(scoreTable.keyList[kvPair.key], scoreTable.valList[kvPair.value]);
			resolvedScoreTable[newLearnitPattern.key] = newLearnitPattern;
		}
		return resolvedScoreTable;
	},
	sortPatternList: sortPatternList,
	filterPatternList: filterPatternList
};