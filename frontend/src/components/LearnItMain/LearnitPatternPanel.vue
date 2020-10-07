<template>
	<div class="learnitListcard">
		<v-card-title
			primary-title
			class="subheading"
		>
			Patterns ( {{numberOfFrozenPattern}}/{{numberOfPattern}} )
			<v-spacer />
			<v-select
				:items="sortingMethodCandidates"
				label="Sorting Method"
				v-model="sortingMethod"
				:disabled="targetName.length <1"
				item-text="uitext"
				item-value="backendtext"
			></v-select>
			<v-switch
				v-model="sortingReverse"
				label="Reverse"
				:disabled="targetName.length <1"
			></v-switch>
		</v-card-title>
		<div
			v-if="showAddPanel"
			style="display:flex"
		>
			<v-text-field
				label="Keyword"
				v-model="patternKeyword"
				style="flex:2"
			></v-text-field>
			<v-text-field
				label="FullTextSearchKey"
				v-model="fullTextSearchKey"
				style="flex:1"
			></v-text-field>
			<v-btn
				@click="searchPatternsByKeywords"
				style="flex:2"
			>Search</v-btn>
		</div>
		<v-list style="overflow-y:scroll;flex-basis:70%" three-line>
			<v-list-tile
				v-for="(item,idx) in patternList"
				:key="idx"
				:class="{frozenGood:currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().precision >= 0.5,frozenBad:currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().precision < 0.5,selected:item===selectedPattern}"
			>
				<v-list-tile-content
					@click="choosePattern(item)"
					v-text="currentTable[item].toString()"
				>
				</v-list-tile-content>
				<v-list-tile-action>
					<v-select
						:items="siblingOntologyTypes"
						label="Target"
						browser-autocomplete="off"
						@change="changeFocusType(item,$event)"
						:value="currentTable[item].getLocalLabel()"
						v-if="!showTargetAndScoreTable"
					></v-select>
				</v-list-tile-action>
				<v-list-tile-avatar v-if="((!currentTable[item].getOriginalScoreTable().frozen) || (currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().precision >= 0.5))">
					<v-btn
						icon
						@click="frozen(item,false)"
					>
						<v-icon>remove</v-icon>
					</v-btn>
				</v-list-tile-avatar>
				<v-list-tile-avatar v-if="((!currentTable[item].getOriginalScoreTable().frozen) || (currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().precision < 0.5))">
					<v-btn
						icon
						@click="frozen(item,true)"
					>
						<v-icon>add</v-icon>
					</v-btn>
				</v-list-tile-avatar>

				<v-list-tile-avatar v-if="currentTable[item].getOriginalScoreTable().frozen">
					<v-btn
						icon
						@click="unfrozen(item)"
					>
						<v-icon>clear</v-icon>
					</v-btn>
				</v-list-tile-avatar>

			</v-list-tile>
		</v-list>
		<div
			class="scoreTableButtonGroup"
			v-if="targetName.length > 0 && showTargetAndScoreTable"
		>
			<v-btn @click="toggleAddNew()">Add New</v-btn>
			<v-btn @click="proposeNewLearnitPatterns()">Propose</v-btn>
			<!-- <v-btn disabled>Rescore</v-btn> -->
			<v-btn @click="getSimilarLearnitPatterns()">Similar</v-btn>
		</div>
		<div
			class="scoreTableButtonGroup"
			v-if="targetName.length > 0 && !showTargetAndScoreTable"
		>
			<v-btn @click="finishAddPattern()">Save and Back</v-btn>
		</div>
	</div>
</template>

<script>
import { Splitpanes, Pane } from "splitpanes";
import LearnitPattern from "@/objects/LearnitPattern.js";
import store from "@/store/index.js";
import service from "@/api/index.js";
import constants from "@/constants.js";
export default {
	name: "LearnitPatternPanel",
	store,
	data: function () {
		return {
			showTargetAndScoreTable: true,
			showAddPanel: false,
			patternKeyword: "",
			patternCandidates: {},
			sortingMethod: "frequency",
			sortingReverse: true,
			fullTextSearchKey: ""
		};
	},
	methods: {
		unfrozen: function (patternKey) {
			if (this.showTargetAndScoreTable) {
				const pattern = this.currentTable[patternKey];
				const isFrozen = false;
				const isGood = false;
				this.$store.dispatch("markPattern", {
					pattern: pattern,
					isFrozen: isFrozen,
					isGood: isGood
				});
			} else {
				const pattern = this.currentTable[patternKey];
				const isFrozen = false;
				const isGood = false;
				service
					.markPattern(
						pattern.getLocalLabel(),
						pattern.toIDString(),
						isFrozen,
						isGood
					)
					.then(success => {
						const scores = this.currentTable[
							patternKey
						].getOriginalScoreTable();
						scores.frozen = false;
					},
						fail => {
							this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						}
					);
			}
		},
		frozen: function (patternKey, isGood) {
			if (this.showTargetAndScoreTable) {
				const pattern = this.currentTable[patternKey];
				const isFrozen = true;
				this.$store.dispatch("markPattern", {
					pattern: pattern,
					isFrozen: isFrozen,
					isGood: isGood
				});
			} else {
				const pattern = this.currentTable[patternKey];
				const isFrozen = true;
				service
					.markPattern(
						pattern.getLocalLabel(),
						pattern.toIDString(),
						isFrozen,
						isGood
					)
					.then(success => {
						const scores = this.currentTable[
							patternKey
						].getOriginalScoreTable();
						scores.frozen = true;
						if (isGood) {
							scores.precision = 0.95;
						} else {
							scores.precision = 0.05;
						}
					},
						fail => {
							this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						}
					);
			}
		},

		toggleAddNew: function () {
			this.showTargetAndScoreTable = false;
			this.showAddPanel = true;
		},
		searchPatternsByKeywords: function () {
			service
				.getPatternsByKeyword(
					this.targetName,
					this.patternKeyword,
					constants.MaxNumberOfPatterns,
					this.fullTextSearchKey,
					this.sortingMethod
				)
				.then(
					success => {
						const ret = {};
						let idx = 0;
						for (const jsonPattern of success.data) {
							const scoreTable = {
								frozen: false
							};
							scoreTable[this.sortingMethod] = (this.sortingReverse) ? -idx : idx;
							const newPattern = LearnitPattern.normalizedPattern.fromJson(
								jsonPattern,
								scoreTable
							);
							newPattern.setLocalLabel(this.targetName);
							ret[newPattern.key] = newPattern;
							idx++;
						}
						this.patternCandidates = ret;
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		},
		finishAddPattern: function () {
			this.$store.dispatch("loadTargetAndScoreTable", { targetName: this.targetName });
			this.showAddPanel = false;
			this.showTargetAndScoreTable = true;
			this.patternCandidates = {};
			this.patternKeyword = "";
		},
		choosePattern: function (patternKey) {
			this.$store.commit("updateMatchInfoDisplay", {
				type: LearnitPattern.normalizedPattern.name,
				key: patternKey,
				learnitObservationObj: this.currentTable[patternKey]
			});
		},
		proposeNewLearnitPatterns: function () {
			this.patternCandidates = {};
			this.showTargetAndScoreTable = false;
			this.showAddPanel = false;
			service.proposeLearnitPatterns(this.targetName).then(
				success => {
					this.patternCandidates = LearnitPattern.loadPatternScoreTable(
						success.data.data
					);
					for (const [key, val] of Object.entries(
						this.patternCandidates
					)) {
						val.setLocalLabel(this.targetName);
					}
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		getSimilarLearnitPatterns: function () {
			this.patternCandidates = {};
			this.showTargetAndScoreTable = false;
			this.showAddPanel = false;
			service.getSimilarLearnitPatterns(this.targetName).then(
				success => {
					this.patternCandidates = LearnitPattern.loadPatternScoreTable(
						success.data.data
					);
					for (const [key, val] of Object.entries(
						this.patternCandidates
					)) {
						val.setLocalLabel(this.targetName);
					}
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		changeFocusType: function (patternKey, targetName) {
			const pattern = this.currentTable[patternKey];
			pattern.setLocalLabel(targetName);
		}
	},
	props: {},
	watch: {
		targetName(newVal, oldVal) {
			if (oldVal !== newVal) {
				this.showTargetAndScoreTable = true;
				this.showAddPanel = false;
				this.patternKeyword = "";
				this.patternCandidates = {};
			}
		}
	},
	components: {
		Splitpanes,
		Pane
	},
	computed: {
		targetName() {
			return this.$store.getters.targetName;
		},
		numberOfPattern() {
			return Object.keys(this.currentTable).length;
		},
		numberOfFrozenPattern() {
			const keyPool = Object.keys(this.currentTable);
			const good = keyPool.filter(
				LearnitPattern.filterPatternList(true, true, this.currentTable)
			);
			const bad = keyPool.filter(
				LearnitPattern.filterPatternList(true, false, this.currentTable)
			);
			return [].concat(
				good.slice(0, constants.MaxNumberOfPatterns),
				bad.slice(0, constants.MaxNumberOfPatterns)
			).length;
		},
		patternList() {
			const keyPool = Object.keys(this.currentTable);

			const good = keyPool
				.filter(LearnitPattern.filterPatternList(true, true, this.currentTable))
				.sort(
					LearnitPattern.sortPatternList(
						typeof this.sortingMethod === "undefined"
							? "precision"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);

			const netural = keyPool
				.filter(
					LearnitPattern.filterPatternList(false, null, this.currentTable)
				)
				.sort(
					LearnitPattern.sortPatternList(
						typeof this.sortingMethod === "undefined"
							? "precision"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);

			const bad = keyPool
				.filter(
					LearnitPattern.filterPatternList(true, false, this.currentTable)
				)
				.sort(
					LearnitPattern.sortPatternList(
						typeof this.sortingMethod === "undefined"
							? "precision"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);
			return [].concat(
				good.slice(0, constants.MaxNumberOfPatterns),
				netural.slice(0, constants.MaxNumberOfPatterns),
				bad.slice(0, constants.MaxNumberOfPatterns)
			);
		},
		currentTable() {
			return this.showTargetAndScoreTable
				? this.$store.getters.patternScoreTable
				: this.patternCandidates;
		},
		selectedPattern() {
			return this.$store.getters.targetAndScoreTableChosenItem.type ===
				LearnitPattern.normalizedPattern.name
				? this.$store.getters.targetAndScoreTableChosenItem.key
				: null;
		},
		sortingMethodCandidates() {
			// const patternObjects = Object.values(this.currentTable);
			// const possibleSet = new Set();
			// for (const patternObj of patternObjects) {
			// 	for (const scoreTableKeys of Object.keys(
			// 		patternObj.getOriginalScoreTable()
			// 	)) {
			// 		possibleSet.add(scoreTableKeys);
			// 	}
			// }
			// return Array.from(possibleSet);
			return constants.patternScoreTableSortableKeys;
		},
		siblingOntologyTypes() {
			const ret = [];
			ret.push(...this.$store.getters.siblingOntologyTypes);
			return ret;
		},
	}
};
</script>

<style scoped>

</style>