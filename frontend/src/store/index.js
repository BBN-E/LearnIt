import Vue from 'vue';
import Vuex from 'vuex';
Vue.use(Vuex);
const debug = process.env.NODE_ENV !== 'production';
import service from '@/api/index.js';
import Seed from "@/objects/Seed.js";
import LearnitPattern from "@/objects/LearnitPattern.js";
export default new Vuex.Store({
	strict: debug,
	state: {
		targetAndScoreTable: {
			patternScoreTable: {},
			seedScoreTable: {}
		},
		targetName: "",
		targetAndScoreTableChosenItem: {
			key: "",
			type: null,
			learnitObservationObj: null
		},
		displayOntologyPanel: false,
		siblingOntologyTypes: [],
		pendingMessage: "",
		errorMessage: "",
		numberOfPagesInLabelMapping: 0,
		currentTimelineEventType: "",
		currentEventTypeTimelineCount: []
	},
	getters: {
		targetName: state => {
			return state.targetName;
		},
		patternScoreTable: state => {
			return state.targetAndScoreTable.patternScoreTable;
		},
		seedScoreTable: state => {
			return state.targetAndScoreTable.seedScoreTable;
		},
		patternListInTargetAndScoreTable: (state, getters) => {
			return Object.keys(getters.patternScoreTable);
		},
		seedListInTargetAndScoreTable: (state, getters) => {
			return Object.keys(getters.seedScoreTable);
		},
		patternScore: (state, getters) => (patternKey) => {
			return getters.patternScoreTable[patternKey];
		},
		seedScore: (state, getters) => (seedKey) => {
			return getters.seedScoreTable[seedKey];
		},
		targetAndScoreTableChosenItem: state => {
			return state.targetAndScoreTableChosenItem;
		},
		displayOntologyPanel: state => {
			return state.displayOntologyPanel;
		},
		siblingOntologyTypes: state => {
			return state.siblingOntologyTypes;
		},
		pendingMessage: state => {
			return state.pendingMessage;
		},
		errorMessage: state => {
			return state.errorMessage;
		},
		numberOfPagesInLabelMapping: state => {
			return state.numberOfPagesInLabelMapping;
		},
		currentTimelineEventType: state => {
			return state.currentTimelineEventType;
		},
		currentEventTypeTimelineCount: state => {
			return state.currentEventTypeTimelineCount;
		}
	},
	mutations: {
		updateTargetAndScoreTable: (state, payload) => {
			state.targetAndScoreTable = payload.targetAndScoreTable;
		},
		updateMatchInfoDisplay: (state, payload) => {
			state.targetAndScoreTableChosenItem = {
				key: payload.key,
				type: payload.type,
				learnitObservationObj: payload.learnitObservationObj
			}
		},
		markSeed: (state, payload) => {
			const scoreTable = state.targetAndScoreTable.seedScoreTable[payload.seedKey].getOriginalScoreTable();
			scoreTable.frozen = payload.isFrozen;
			scoreTable.score = (payload.isGood) ? 0.95 : 0.05;
		},
		markPattern: (state, payload) => {
			const scoreTable = state.targetAndScoreTable.patternScoreTable[payload.patternKey].getOriginalScoreTable();
			scoreTable.frozen = payload.isFrozen;
			scoreTable.precision = (payload.isGood) ? 0.95 : 0.05;
		},
		toggleDisplayOntologyPanel: (state) => {
			state.displayOntologyPanel = !state.displayOntologyPanel;
		},
		changeTargetName: (state, payload) => {
			state.targetName = payload.targetName;
		},
		changeSiblingOntologyTypes: (state, payload) => {
			state.siblingOntologyTypes = payload.siblingOntologyTypes;
		},
		changePendingMessage: (state, payload) => {
			state.pendingMessage = payload.pendingMessage;
		},
		changeErrorMessage: (state, payload) => {
			state.errorMessage = payload.errorMessage;
		},
		setNumberOfPagesInLabelMapping: (state, payload) => {
			state.numberOfPagesInLabelMapping = payload.numberOfPagesInLabelMapping;
		},
		changeCurrentTimelineEventType: (state, payload) => {
			state.currentTimelineEventType = payload.currentTimelineEventType;
		},
		updatetEventTypeTimelineCount: (state, payload) => {
			state.currentEventTypeTimelineCount = payload.currentEventTypeTimelineCount;
		}
	},
	actions: {
		loadTargetAndScoreTable: ({
			commit,
			dispatch
		}, payload) => {
			return new Promise((resolve, reject) => {
				const newTargetName = payload.targetName;
				commit('updateMatchInfoDisplay', {
					key: "",
					type: null,
					learnitObservationObj: null
				});
				service.loadTargetAndScoreTable(newTargetName).then(
					success => {
						const patternScoreTable = LearnitPattern.loadPatternScoreTable(
							success.data.patternScores.data
						);
						const seedScoreTable = Seed.loadSeedScoreTable(
							success.data.seedScores.data
						);
						const targetAndScoreTable = {
							patternScoreTable: patternScoreTable,
							seedScoreTable: seedScoreTable
						};
						commit('changeTargetName', {
							targetName: newTargetName
						});
						commit('updateTargetAndScoreTable', {
							targetAndScoreTable: targetAndScoreTable
						});
						commit('updateMatchInfoDisplay', {
							key: "",
							type: null,
							learnitObservationObj: null
						});
						resolve();
					},
					fail => {
						if (payload.tryCreateTarget === true) {
							payload.tryCreateTarget = false;
							dispatch('addTargetAndScoreTable', payload).then(
								() => {
									dispatch('loadTargetAndScoreTable', payload).then(
										() => { resolve() },
										fail => { reject(fail); }
									);
								}, fail => {
									commit('changeTargetName', {
										targetName: ""
									});
									reject(fail);
								});
						}
						else {
							commit('changeTargetName', {
								targetName: ""
							});
							reject(fail);
						}
					}
				);
			});
		},
		loadLabelMappingAnnotationView: ({
			commit,
			dispatch
		}, payload) => {
			const targetName = payload.targetName;
			commit('setNumberOfPagesInLabelMapping', {
				numberOfPagesInLabelMapping: 0
			});
			commit('changeTargetName', {
				targetName: ""
			});
			return new Promise((resolve, reject) => {
				service.getLabeledMappingNumberOfPages(targetName, false).then(
					success => {
						commit('setNumberOfPagesInLabelMapping', {
							numberOfPagesInLabelMapping: success.data
						});
						commit('changeTargetName', {
							targetName: targetName
						});
						resolve();
					},
					fail => {
						commit('changeTargetName', {
							targetName: ""
						});
						reject(fail);
					}
				)
			});
		},
		addTargetAndScoreTable: ({ }, payload) => {
			return new Promise((resolve, reject) => {
				service.addTargetAndScoreTable(payload.targetName, payload.description, payload.slot0EntityTypes, payload.slot1EntityTypes, payload.symmetric, payload.slot0SpanningType, payload.slot1SpanningType).then(
					() => {
						resolve();
					}, () => {
						reject();
					});
			});
		},
		markSeed: ({
			commit,
			getters
		}, payload) => {
			service.markSeed(getters.targetName, payload.seed.getOriginalObjJson(), payload.isFrozen, payload.isGood).then(
				() => {
					commit('markSeed', {
						seedKey: payload.seed.getKey(),
						isFrozen: payload.isFrozen,
						isGood: payload.isGood
					});
				},
				fail => {
					commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		markPattern: ({
			commit,
			getters
		}, payload) => {
			service.markPattern(getters.targetName, payload.pattern.toIDString(), payload.isFrozen, payload.isGood).then(
				() => {
					commit('markPattern', {
						patternKey: payload.pattern.getKey(),
						isFrozen: payload.isFrozen,
						isGood: payload.isGood
					});
				},
				fail => {
					commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		lookingEventTimeline: ({ commit }, payload) => {
			return new Promise((resolve, reject) => {
				service.getEventTypeStatistics().then(
					success => {
						const typeToCnts = success.data;
						if (payload.timelineEventType in typeToCnts) {
							commit('changeCurrentTimelineEventType', { currentTimelineEventType: payload.timelineEventType });
							commit('updatetEventTypeTimelineCount', { currentEventTypeTimelineCount: typeToCnts[payload.timelineEventType] });
							resolve();
						}
						else{
							reject("Cannot find event timeline date for type "+payload.timelineEventType);
						}
					},
					fail => {
						reject(fail);
					}
				);
			});

		}
	}
})