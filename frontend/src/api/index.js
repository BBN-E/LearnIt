import axios from 'axios';
import constants from '@/constants.js';

export default {
	getOntology: function () {
		return axios({
			baseURL: constants.baseURL,
			url: "/ontology/current_tree",
			method: "GET",
		});
	},
	addOntologyNode: function (parent, newRootId, description, slot0SpanningType, slot1SpanningType) {
		return axios({
			baseURL: constants.baseURL,
			url: "/ontology/add_target",
			method: "POST",
			params: {
				parentNodeId: parent,
				id: newRootId,
				description: description,
				slot0SpanningType: slot0SpanningType,
				slot1SpanningType: slot1SpanningType
			}
		});
	},
	addTargetAndScoreTable: function (targetName, description, slot0EntityTypes, slot1EntityTypes, symmetric, slot0SpanningType, slot1SpanningType) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/add_target",
			method: "POST",
			params: {
				name: targetName,
				description: description,
				slot0EntityTypes: slot0EntityTypes,
				slot1EntityTypes: slot1EntityTypes,
				symmetric: symmetric,
				slot0SpanningType: slot0SpanningType,
				slot1SpanningType: slot1SpanningType
			}
		});
	},
	loadTargetAndScoreTable: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_extractor",
			method: "POST",
			params: { relation: targetName }
		});
	},
	getPatternsByKeyword: function (targetName, keyword, amount, fullTextSearchKey, sortingMethod) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_patterns_by_keyword",
			method: "POST",
			params: { target: targetName, keyword: keyword, amount: amount, fullTextSearchKey: fullTextSearchKey, sortingMethod: sortingMethod }
		});
	},
	getSeedsBySlot: function (targetName, slot0, slot1, amount, fullTextSearchKey, sortingMethod) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_seeds_by_slots",
			method: "POST",
			params: { target: targetName, slot0: slot0, slot1: slot1, amount: amount, fullTextSearchKey: fullTextSearchKey, sortingMethod: sortingMethod }
		});
	},
	getPatternInstances: function (targetName, patternStr, amount, fromOther) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_pattern_instances",
			method: "POST",
			params: { target: targetName, pattern: patternStr, amount: amount, fromOther: fromOther }
		});
	},
	getSeedInstances: function (targetName, seedJson, amount, fromOther) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_seed_instances",
			method: "POST",
			params: { target: targetName, seed: seedJson, amount: amount, fromOther: fromOther }
		});
	},
	proposeSeeds: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/propose_seeds_new",
			method: "POST",
			params: { target: targetName }
		});
	},
	proposeLearnitPatterns: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/propose_patterns_new",
			method: "POST",
			params: { target: targetName }
		});
	},
	getSimilarSeeds: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/similar_seeds",
			method: "POST",
			params: { target: targetName }
		});
	},
	getSimilarLearnitPatterns: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/similar_patterns",
			method: "POST",
			params: { target: targetName }
		});
	},
	markSeed: function (targetName, seedObj, isFrozen, isGood) {
		if (isFrozen) {
			return axios({
				baseURL: constants.baseURL,
				url: "/init/add_seeds",
				method: "POST",
				params: { target: targetName, seeds: [seedObj], quality: isGood ? "good" : "bad" }
			});
		}
		else {
			return axios({
				baseURL: constants.baseURL,
				url: "/init/remove_seed",
				method: "POST",
				params: { target: targetName, seed: seedObj }
			})
		}
	},
	markPattern: function (targetName, patternStr, isFrozen, isGood) {
		if (isFrozen) {
			return axios({
				baseURL: constants.baseURL,
				url: "/init/add_pattern",
				method: "POST",
				params: { target: targetName, pattern: patternStr, quality: isGood ? "good" : "bad" }
			});
		}
		else {
			return axios({
				baseURL: constants.baseURL,
				url: "/init/remove_pattern",
				method: "POST",
				params: { target: targetName, pattern: patternStr }
			});
		}
	},
	saveProgress: function () {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/save_progress",
			method: "POST",
		});
	},
    generateVisualization: function () {
        return axios({
            baseURL: constants.baseURL,
            url: "/init/generate_eer_graph",
            method: "POST",
        });
    },
	clearUnknown: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/clear_unknown",
			method: "POST",
			params: { target: targetName }
		});
	},
	clearAll: function (targetName) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/clear_all",
			method: "POST",
			params: { target: targetName }
		});
	},
	markInstance: function (targetName, instanceIdentifier, quality) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/add_instance",
			method: "POST",
			params: { target: targetName, instance: instanceIdentifier, quality }
		});
	},
	unMarkInstance: function (targetName, instanceIdentifier) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/remove_instance",
			method: "POST",
			params: { target: targetName, instance: instanceIdentifier }
		});
	},
	markInstanceFromOther: function (ontoloyTypeName, instanceIdentifier) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/mark_instance_from_other",
			method: "POST",
			params: { target: ontoloyTypeName, instance: instanceIdentifier }
		})
	},
	markInstanceByPatternFromOther: function (ontoloyTypeName, patternStr) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/mark_pattern_from_other",
			method: "POST",
			params: { target: ontoloyTypeName, pattern: patternStr }
		});
	},
	markInstanceBySeedFromOther: function (ontoloyTypeName, seedJson) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/mark_seed_from_other",
			method: "POST",
			params: { target: ontoloyTypeName, seed: seedJson }
		});
	},
	getLabeledMappingInstances: function (target, fromOther, currentPage) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_labeled_mapping_instances",
			method: "POST",
			params: { target: target, fromOther: fromOther, instancePerPage: constants.NumberOfInstancePerPageInInstanceAnnotation, currentPage: currentPage }
		});
	},
	getLabeledMappingNumberOfPages: function (target, fromOther) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/get_labeled_mapping_number_of_pages",
			method: "POST",
			params: { target: target, fromOther: fromOther, instancePerPage: constants.NumberOfInstancePerPageInInstanceAnnotation }
		});
	},
	markInstanceFromLabeledMappings: function (target, instance) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/mark_instance_from_labeled_mappings",
			method: "POST",
			params: { target: target, instance: instance }
		});
	},
	dictionaryLookup: function (word) {
		return axios({
			baseURL: constants.baseURL,
			url: "/init/dictionary_lookup",
			method: "POST",
			params: { "word": word }
		});
	},
	getEventTypeStatistics: function () {
		return axios({
			baseURL: constants.baseTimelineURL,
			url: "/event_type_statistics",
			method: "GET",
		});
	},
	getEntityList: function (eventType, startTime, endTime) {
		return axios({
			baseURL: constants.baseTimelineURL,
			url: "/entity_list/",
			method: "GET",
			params: {
				event_type: eventType,
				start_timestamp: startTime,
				end_timestamp: endTime
			}
		});
	},
	getEventFrame: function (eventType, startTime, endTime, otherConstraint) {
		return axios({
			baseURL: constants.baseTimelineURL,
			url: "/event_frame/",
			method: "POST",
			data: {
				event_type: eventType,
				start_timestamp: startTime,
				end_timestamp: endTime,
				other_constraint: otherConstraint
			}
		});
	}
}