<template>
	<div class="learnitListcard">
		<v-card-title
		 primary-title
		 class="subheading"
		>
			Pairs ( {{numberOfFrozenSeed}}/{{numberOfSeed}} )
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
			 label="Slot0"
			 v-model="seedSlot0Keyword"
			></v-text-field>
			<v-text-field
			 label="Slot1"
			 v-model="seedSlot1Keyword"
			></v-text-field>
			<v-btn @click="searchSeedsBySlots()">Search</v-btn>
		</div>
		<v-list style="overflow-y:scroll;flex-basis:70%">
			<v-list-tile
			 v-for="(item,idx) in seedList"
			 :key="idx"
			 :class="{frozenGood:currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().score >= 0.5,frozenBad:currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().score < 0.5,selected:item===selectedSeed}"
			>
				<v-list-tile-content @click="chooseSeed(item)">{{currentTable[item].toString()}}</v-list-tile-content>
				<v-list-tile-avatar v-if="(!currentTable[item].getOriginalScoreTable().frozen) || (currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().score >= 0.5)">
					<v-btn
					 icon
					 @click="frozen(item,false)"
					>
						<v-icon>remove</v-icon>
					</v-btn>
				</v-list-tile-avatar>
				<v-list-tile-avatar v-if="(!currentTable[item].getOriginalScoreTable().frozen) || (currentTable[item].getOriginalScoreTable().frozen && currentTable[item].getOriginalScoreTable().score < 0.5)">
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
			<v-btn @click="proposeNewSeeds()">Propose</v-btn>
			<!-- <v-btn disabled>Rescore</v-btn> -->
			<v-btn @click="getSimilarSeeds()">Similar</v-btn>
			<!-- <v-btn disabled>Auto-Accept</v-btn> -->
			<!-- <v-btn disabled>Get Additional</v-btn> -->
		</div>
		<div
		 class="scoreTableButtonGroup"
		 v-if="targetName.length > 0 && !showTargetAndScoreTable"
		>
			<v-btn @click="finishAddSeed()">Save and Back</v-btn>
		</div>
	</div>
</template>

<script>
import store from "@/store/index.js";
import Seed from "@/objects/Seed.js";
import service from "@/api/index.js";
import constants from "@/constants.js";
export default {
	name: "SeedPanel",
	store,
	data: function () {
		return {
			showTargetAndScoreTable: true,
			seedCandidates: {},
			showAddPanel: false,
			seedSlot0Keyword: "",
			seedSlot1Keyword: "",
			sortingMethod: "seedPrecision",
			sortingReverse: true
		};
	},
	methods: {
		chooseSeed: function (seedKey) {
			this.$store.commit("updateMatchInfoDisplay", {
				type: Seed.Seed.name,
				key: seedKey,
				learnitObservationObj: this.currentTable[seedKey]
			});
		},
		unfrozen: function (seedKey) {
			if (this.showTargetAndScoreTable) {
				const seed = this.currentTable[seedKey];
				const isFrozen = false;
				const isGood = false;
				this.$store.dispatch("markSeed", {
					seed: seed,
					isFrozen: isFrozen,
					isGood: isGood
				});
			} else {
				const seed = this.currentTable[seedKey];
				const isFrozen = false;
				const isGood = false;
				service
					.markSeed(
						this.targetName,
						seed.getOriginalObjJson(),
						isFrozen,
						isGood
					)
					.then(success => {
						const scores = this.currentTable[
							seedKey
						].getOriginalScoreTable();
						scores.frozen = false;
					},
						fail => {
							this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						}
					);
			}
		},
		frozen: function (seedKey, isGood) {
			if (this.showTargetAndScoreTable) {
				const seed = this.currentTable[seedKey];
				const isFrozen = true;
				this.$store.dispatch("markSeed", {
					seed: seed,
					isFrozen: isFrozen,
					isGood: isGood
				});
			} else {
				const seed = this.currentTable[seedKey];
				const isFrozen = true;
				service
					.markSeed(
						this.targetName,
						seed.getOriginalObjJson(),
						isFrozen,
						isGood
					)
					.then(success => {
						const scores = this.currentTable[
							seedKey
						].getOriginalScoreTable();
						scores.frozen = true;
						if (isGood) {
							scores.score = 0.95;
						} else {
							scores.score = 0.05;
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
		finishAddSeed: function () {
			this.$store.dispatch('loadTargetAndScoreTable', { targetName: this.targetName });
			this.showAddPanel = false;
			this.showTargetAndScoreTable = true;
			this.seedCandidates = {};
			this.seedSlot0Keyword = "";
			this.seedSlot1Keyword = "";
		},
		searchSeedsBySlots: function () {
			service
				.getSeedsBySlot(
					this.targetName,
					this.seedSlot0Keyword,
					this.seedSlot1Keyword,
					constants.MaxNumberOfSeeds
				)
				.then(
					success => {
						const ret = {};
						for (const jsonSeed of success.data) {
							const newSeed = Seed.Seed.fromJson(jsonSeed, {
								frozen: false,
								precision: 0.0
							});
							ret[newSeed.key] = newSeed;
						}
						this.seedCandidates = ret;
					},
					fail => { }
				);
		},
		proposeNewSeeds: function () {
			this.seedCandidates = {};
			this.showTargetAndScoreTable = false;
			this.showAddPanel = false;
			service.proposeSeeds(this.targetName).then(
				success => {
					this.seedCandidates = Seed.loadSeedScoreTable(success.data.data);
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		getSimilarSeeds: function () {
			this.seedCandidates = {};
			this.showTargetAndScoreTable = false;
			this.showAddPanel = false;
			service.getSimilarSeeds(this.targetName).then(
				success => {
					this.seedCandidates = Seed.loadSeedScoreTable(success.data.data);
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		}
	},
	props: {},
	computed: {
		targetName() {
			return this.$store.getters.targetName;
		},
		seedList() {
			const keyPool = Object.keys(this.currentTable);
			const good = keyPool
				.filter(Seed.filterSeedList(true, true, this.currentTable))
				.sort(
					Seed.sortSeedList(
						typeof this.sortingMethod === "undefined"
							? "score"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);

			const netural = keyPool
				.filter(Seed.filterSeedList(false, null, this.currentTable))
				.sort(
					Seed.sortSeedList(
						typeof this.sortingMethod === "undefined"
							? "score"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);

			const bad = keyPool
				.filter(Seed.filterSeedList(true, false, this.currentTable))
				.sort(
					Seed.sortSeedList(
						typeof this.sortingMethod === "undefined"
							? "score"
							: this.sortingMethod,
						this.sortingReverse,
						this.currentTable
					)
				);
			return [].concat(
				good.slice(0, constants.MaxNumberOfSeeds),
				netural.slice(0, constants.MaxNumberOfSeeds),
				bad.slice(0, constants.MaxNumberOfSeeds)
			);
		},
		currentTable() {
			return this.showTargetAndScoreTable
				? this.$store.getters.seedScoreTable
				: this.seedCandidates;
		},
		selectedSeed() {
			return this.$store.getters.targetAndScoreTableChosenItem.type ===
				Seed.Seed.name
				? this.$store.getters.targetAndScoreTableChosenItem.key
				: null;
		},
		numberOfFrozenSeed() {
			const keyPool = Object.keys(this.currentTable);
			const good = keyPool.filter(
				Seed.filterSeedList(true, true, this.currentTable)
			);
			const bad = keyPool.filter(
				Seed.filterSeedList(true, false, this.currentTable)
			);
			return [].concat(
				good.slice(0, constants.MaxNumberOfSeeds),
				bad.slice(0, constants.MaxNumberOfSeeds)
			).length;
		},
		numberOfSeed() {
			return Object.keys(this.currentTable).length;
		},
		sortingMethodCandidates() {
			// const seedObjects = Object.values(this.currentTable);
			// const possibleSet = new Set();
			// for (const seedObj of seedObjects) {
			// 	for (const scoreTableKeys of Object.keys(
			// 		seedObj.getOriginalScoreTable()
			// 	)) {
			// 		possibleSet.add(scoreTableKeys);
			// 	}
			// }
			// return Array.from(possibleSet);
			return constants.seedScoreTableSortableKeys;
		}
	},
	watch: {
		targetName(newVal, oldVal) {
			if (newVal !== oldVal) {
				this.showAddPanel = false;
				this.showTargetAndScoreTable = true;
				this.seedCandidates = {};
				this.seedSlot0Keyword = "";
				this.seedSlot1Keyword = "";
			}
		}
	}
};
</script>

<style scoped>
</style>