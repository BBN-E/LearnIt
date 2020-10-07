import LearnItObservation from '@/objects/LearnItObservation.js';

class Seed extends LearnItObservation {
	constructor(originalObjJson, originalScoreTable) {
		const displayableStr = "(" + originalObjJson.slot0.text[0] + "," + originalObjJson.slot1.text[0] + ")";
		const key = JSON.stringify(originalObjJson);
		super(key, originalObjJson, originalObjJson.toIDString, displayableStr, originalScoreTable);
	}

	static fromJson(originalObjJson, originalScoreTable) {
		return new Seed(originalObjJson, originalScoreTable);
	}
}

const sortSeedList = (key, reverse, kvMapping) => (a, b) => {
	const aOriginalScoreTable = kvMapping[a].getOriginalScoreTable();
	const bOriginalScoreTable = kvMapping[b].getOriginalScoreTable();
	const leftKey = (typeof aOriginalScoreTable[key] === 'undefined') ? 0 : aOriginalScoreTable[key];
	const rightKey = (typeof bOriginalScoreTable[key] === 'undefined') ? 0 : bOriginalScoreTable[key];
	if (!reverse) leftKey - rightKey;
	else return rightKey - leftKey;
};

const filterSeedList = (frozen, isGood, kvMapping) => (seed) => {
	const seedOriginalScoreTable = kvMapping[seed].getOriginalScoreTable();
	if (!frozen) return !seedOriginalScoreTable.frozen;
	else {
		const score = (typeof seedOriginalScoreTable['score'] === 'undefined') ? 0 : seedOriginalScoreTable['score'];
		if (isGood) return seedOriginalScoreTable.frozen && score >= 0.5;
		else return seedOriginalScoreTable.frozen && score < 0.5;
	}
}

export default {
	Seed: Seed,
	loadSeedScoreTable(scoreTable) {
		const resolvedScoreTable = {};
		for (const idx in scoreTable.entries) {
			const kvPair = scoreTable.entries[idx];
			const newSeed = Seed.fromJson(scoreTable.keyList[kvPair.key], scoreTable.valList[kvPair.value]);
			resolvedScoreTable[newSeed.key] = newSeed;
		}
		return resolvedScoreTable;
	},
	sortSeedList: sortSeedList,
	filterSeedList: filterSeedList
}