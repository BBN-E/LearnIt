<template>
	<div class="learnitListcard">
		<v-card-title
		 primary-title
		 class="subheading"
		>Instances ( {{numberOfFrozenInstance}}/{{numberOfInstances}} )</v-card-title>
		<div style="overflow-y:scroll;flex-basis:85%;flex-grow:1">
			<!-- <div
			 v-for="(value,key) of scoreTable"
			 :key="key"
			>{{key}}: {{value}}</div> -->
			<v-select
			 :items="siblingOntologyTypes"
			 label="Mark all LearnIt Observation Instances"
			 browser-autocomplete="off"
			 v-if="inOTHERTarget"
			 @change="changeInstanceTypeInBatch($event)"
			></v-select>
			<div style="height:45px"></div>
			<div
			 v-for="(item,idx) in instanceList"
			 :key="idx"
			>
				<p v-html="instances[item].htmlStr"></p>
				<v-spacer />
				<v-flex :class="{frozenGood:(instances[item].relationType === targetName) && (instances[item].annotation === constants.instanceFrozenGoodStr),frozenBad:(instances[item].relationType === targetName) && (instances[item].annotation === constants.instanceFrozenBadStr)}">
					<div v-if="!inOTHERTarget">
						<v-btn
						 icon
						 @click="markInstance(item,'bad')"
						>
							<v-icon>remove</v-icon>
						</v-btn>
						<v-btn
						 icon
						 @click="markInstance(item,'good')"
						>
							<v-icon>add</v-icon>
						</v-btn>
						<v-btn
						 icon
						 @click="unMarkInstance(item)"
						>
							<v-icon>clear</v-icon>
						</v-btn>
					</div>
					<v-select
					 :items="siblingOntologyTypes"
					 label="Choose Type"
					 browser-autocomplete="off"
					 @change="changeInstanceType(item,$event)"
					 v-if="inOTHERTarget"
					 :value="instances[item].relationType"
					></v-select>
				</v-flex>
			</div>
		</div>
	</div>
</template>

<script>
import service from "@/api/index.js";
import store from "@/store/index.js";
import Instance from "@/objects/MatchInfoDisplay.js";
import constants from "@/constants.js";
import Seed from "@/objects/Seed.js";
import LearnitPattern from "@/objects/LearnitPattern.js";
export default {
	name: "InstancePanel",
	store,
	data: function () {
		return {
			instances: {},
			scoreTable: {}
		};
	},
	methods: {
		markInstance: function (itemKey, statusStr) {
			const instanceObj = this.instances[itemKey];
			const instanceIdentifier = instanceObj.instanceIdentifier;
			const frozenStr =
				(statusStr === "good")
					? constants.instanceFrozenGoodStr
					: constants.instanceFrozenBadStr;
			service
				.markInstance(this.targetName, instanceIdentifier, statusStr)
				.then(
					success => {
						instanceObj.annotation = frozenStr;
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		},
		unMarkInstance: function (itemKey) {
			const instanceObj = this.instances[itemKey];
			const instanceIdentifier = instanceObj.instanceIdentifier;
			service
				.unMarkInstance(this.targetName, instanceIdentifier)
				.then(
					success => {
						instanceObj.annotation = constants.instanceNoFrozenStr;
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		},
		changeInstanceType: function (itemKey, event) {
			const selectedType = event;
			const instanceObj = this.instances[itemKey];
			const instanceIdentifier = instanceObj.instanceIdentifier;
			service.markInstanceFromOther(selectedType, instanceIdentifier).then(
				success => {
					instanceObj.relationType = selectedType;
					instanceObj.annotation = constants.instanceFrozenGoodStr;
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		changeInstanceTypeInBatch: function (event) {
			const selectedType = event;
			const learnitObservationObj = this.targetAndScoreTableChosenItem.learnitObservationObj;
			if (learnitObservationObj instanceof Seed.Seed) {
				service.markInstanceBySeedFromOther(selectedType,learnitObservationObj.getOriginalObjJson()).then(
					success => {
						this.loadInstancesFromSeed(learnitObservationObj);
					 },
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
			}
			else if (learnitObservationObj instanceof LearnitPattern.normalizedPattern) {
				service.markInstanceByPatternFromOther(selectedType,learnitObservationObj.getOriginalObjJson().toIDString).then(
					success => { 
						this.loadInstancesFromLearnitPatterns(learnitObservationObj);
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
			}
		},
		loadInstancesFromSeed: function (learnitObservationObj) {
			service
				.getSeedInstances(
					this.targetName,
					learnitObservationObj.getOriginalObjJson(),
					constants.MaxNumberOfSeeds,
					this.inOTHERTarget
				)
				.then(
					success => {
						const labeledInstances = Instance.loadInstanceIdentifierTable(
							success.data
						);
						this.instances = labeledInstances;
						for (const [key, val] of Object.entries(
							learnitObservationObj.getOriginalScoreTable()
						)) {
							if (constants.seedScoreTableDisplayableKeys.includes(key)) {
								this.scoreTable[key] = val;
							}
						}
						// this.$store.commit('changePendingMessage',{pendingMessage:""});
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		},
		loadInstancesFromLearnitPatterns: function (learnitObservationObj) {
			service
				.getPatternInstances(
					this.targetName,
					learnitObservationObj.getOriginalObjJson().toIDString,
					this.inOTHERTarget?constants.MaxNumberOfInstanceFromOTHER:constants.MaxNumberOfInstance,
					this.inOTHERTarget
				)
				.then(
					success => {
						const labeledInstances = Instance.loadInstanceIdentifierTable(
							success.data
						);
						this.instances = labeledInstances;
						for (const [key, val] of Object.entries(
							learnitObservationObj.getOriginalScoreTable()
						)) {
							if (
								constants.patternScoreTableDisplayableKeys.includes(key)
							) {
								this.scoreTable[key] = val;
							}
						}
						// this.$store.commit('changePendingMessage',{pendingMessage:""});
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		}
	},
	created() { },
	mounted() { },
	props: {},
	computed: {
		instanceList() {
			return Object.keys(this.instances);
		},
		targetAndScoreTableChosenItem() {
			return this.$store.getters.targetAndScoreTableChosenItem;
		},
		numberOfInstances() {
			return this.instanceList.length;
		},
		numberOfFrozenInstance() {
			return this.instanceList.length;
		},
		constants() {
			return constants;
		},
		siblingOntologyTypes() {
			const ret = [];
			ret.push(...this.$store.getters.siblingOntologyTypes);
			return ret;
		},
		targetName() {
			return this.$store.getters.targetName;
		},
		inOTHERTarget() {
			return this.targetName === constants.OTHERTargetName;
		}
	},
	watch: {
		targetName(newVal, oldVal) {
			if (newVal !== oldVal) {
				this.scoreTable = {};
				this.instances = {};
			}
		},
		targetAndScoreTableChosenItem(newVal, oldVal) {
			if (newVal.type !== null) {
				const type = newVal.type;
				const learnitObservationObj = newVal.learnitObservationObj;
				this.scoreTable = {};
				this.instances = {};
				if (learnitObservationObj instanceof Seed.Seed) {
					this.$store.commit('changePendingMessage', { pendingMessage: "We're getting instances." });
					this.loadInstancesFromSeed(learnitObservationObj);
				}
				else if (learnitObservationObj instanceof LearnitPattern.normalizedPattern) {
					this.$store.commit('changePendingMessage', { pendingMessage: "We're getting instances." });
					this.loadInstancesFromLearnitPatterns(learnitObservationObj);
				}
			}
		}
	}
};
</script>

<style scoped>
</style>
