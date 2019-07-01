// const baseURL = "http://afrl101:5022";
const baseURL = "";
export default {
	baseURL: baseURL,
	MaxNumberOfPatternsAndSeeds: 1000,
	MaxNumberOfPatterns: 1000,
	MaxNumberOfSeeds: 1000,
	MaxNumberOfInstance: 50,
	MaxNumberOfInstanceFromOTHER:250,
	patternScoreTableDisplayableKeys:['precision','recall','confidence','frequency'],
	seedScoreTableDisplayableKeys:['confidence','frequency'],
	// patternScoreTableSortableKeys:['precision','recall','confidence','frequency'],
	patternScoreTableSortableKeys:[
		{
			"uitext":"precision",
			"backendtext":"patternPrecision"
		},
		{
			"uitext":"weightedPrecision",
			"backendtext":"patternWeightedPrecision"
		},
		{
			"uitext":"frequency",
			"backendtext":"patternFrequency"
		}
	],
	// seedScoreTableSortableKeys:['score','confidence','frequency'],
	seedScoreTableSortableKeys:[
		{
			"uitext":"weightedPrecision",
			"backendtext":"seedWeightedPrecision"
		},
		{
			"uitext":"precision",
			"backendtext":"seedPrecision"
		},
		{
			"uitext":"frequency",
			"backendtext":"seedFrequency"
		}
	],
	instanceFrozenGoodStr:"FROZEN_GOOD",
	instanceFrozenBadStr:"FROZEN_BAD",
	instanceNoFrozenStr:"NO_FROZEN",

	OTHERTargetName:"OTHER"
}
