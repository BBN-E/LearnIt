// const baseURL = "http://10.2.10.57:5022";
// const baseURL = "http://afrl103:5022";
// const baseTimelineURL = "http://afrl103:5010";
// const baseURL = "http://10.2.10.57:5022";
// const baseTimelineURL = "http://10.2.10.57:5010";
const baseURL = "";
const baseTimelineURL = ":5010";
export default {
	baseURL: baseURL,
	baseTimelineURL: baseTimelineURL,
	MaxNumberOfPatterns: 200,
	MaxNumberOfSeeds: 200,
	MaxNumberOfInstance: 50,
	MaxNumberOfInstanceFromOTHER: 250,
	NumberOfInstancePerPageInInstanceAnnotation: 50,
	patternScoreTableDisplayableKeys: ['precision', 'recall', 'confidence', 'frequency'],
	seedScoreTableDisplayableKeys: ['confidence', 'frequency'],
	trueNegativeLabel: "NA",
	// patternScoreTableSortableKeys:['precision','recall','confidence','frequency'],
	patternScoreTableSortableKeys: [
		{
			"uitext": "overlap",
			"backendtext": "overlap"
		},
		{
			"uitext": "frequency",
			"backendtext": "frequency"
		},
		{
			"uitext": "precision",
			"backendtext": "patternPrecision"
		},
		{
			"uitext": "weightedPrecision",
			"backendtext": "patternWeightedPrecision"
		},
		{
			"uitext": "similarity",
			"backendtext":"precision"
		}
	],
	// seedScoreTableSortableKeys:['score','confidence','frequency'],
	seedScoreTableSortableKeys: [
		{
			"uitext": "overlap",
			"backendtext": "overlap"
		},
		{
			"uitext": "frequency",
			"backendtext": "frequency"
		},
		{
			"uitext": "weightedPrecision",
			"backendtext": "seedWeightedPrecision"
		},
		{
			"uitext": "precision",
			"backendtext": "seedPrecision"
		},
        {
			"uitext": "similarity",
			"backendtext":"precision"
        }
	],
	instanceFrozenGoodStr: "FROZEN_GOOD",
	instanceFrozenBadStr: "FROZEN_BAD",
	instanceNoFrozenStr: "NO_FROZEN",

	OTHERTargetName: "OTHER"
}
