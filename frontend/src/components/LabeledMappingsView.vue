<template>
	<div class="learnitListcard">
		<v-toolbar>
			<v-toolbar-title>LearnIt Instance Annotation View</v-toolbar-title>
			<span class="title align-content-center pl-4 pr-2">{{targetName}}</span>
			<v-toolbar-items class="hidden-sm-and-down">
				<v-btn
					flat
					@click="returnToMain()"
				>Return To Main</v-btn>
				<v-btn
					flat
					@click="markAllUnFrozenToNA()"
				>
					Mark All UnFrozen To NA
				</v-btn>
			</v-toolbar-items>

			<v-spacer></v-spacer>
			<v-toolbar-items>
				<v-btn
					flat
					@click="saveProgress()"
				>Save Progress</v-btn>
			</v-toolbar-items>
		</v-toolbar>

		<div style="overflow-y:scroll;flex-basis:85%;flex-grow:1">
			<div
				v-for="(item,idx) in instanceList"
				:key="idx"
			>

                <div
                    v-for="(languageDisplay,langIdx) in instances[item].matchInfoDisplay.langDisplays"
                    :key="langIdx"
                >
                    <br />
                    <span><b v-text="languageDisplay.language"></b></span>
                    <br />
                    <p style="margin-bottom:0">
                        <span
                            v-for="(token,tokenIdx) in languageDisplay.sentenceTokens"
                            :key="tokenIdx"
                        >
                            <font :class="{'slot0':languageDisplay.instance && languageDisplay.instance.slot0Start <= tokenIdx && languageDisplay.instance.slot0End >= tokenIdx,'slot1':languageDisplay.instance && languageDisplay.instance.slot1Start <= tokenIdx && languageDisplay.instance.slot1End >= tokenIdx}">{{token}}</font>
                            <font v-text="space_char"></font>
                        </span>
                    </p>
                    <span>(
                        <span
                            v-if="languageDisplay.seed"
                            class="slot0"
                        >{{languageDisplay.seed.slot0.text[0]}}</span>,
                        <span
                            v-if="languageDisplay.seed"
                            class="slot1"
                        >{{languageDisplay.seed.slot1.text[0]}}</span>)</span>
                </div>

				<v-spacer />
				<v-flex :class="{frozenGood: instances[item].annotation === constants.instanceFrozenGoodStr,frozenBad:instances[item].annotation === constants.instanceFrozenBadStr}">
					<v-select
						:items="siblingOntologyTypes"
						label="Choose Type"
						browser-autocomplete="off"
						@input="changeInstanceType(item,$event)"
						:value="instances[item].relationType"
					></v-select>
				</v-flex>
			</div>
		</div>

		<v-pagination
			v-model="currentPage"
			:length="numberOfPagesInLabelMapping"
		></v-pagination>
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
	name: "LabeledMappingsView",
	store,
	data: function () {
		return {
			instances: {},
			scoreTable: {},
			currentPage: 1,
			space_char:" "
		};
	},
	methods: {
		changeInstanceType: function (itemKey, event) {
			const selectedType = event;
			const instanceObj = this.instances[itemKey];
			const instanceIdentifier = instanceObj.instanceIdentifier;
			service.markInstanceFromLabeledMappings(selectedType, instanceIdentifier).then(
				success => {
					instanceObj.relationType = selectedType;
					instanceObj.annotation = success.data;
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		loadInstancesFromLabelMappings: function (targetPage) {
			this.$store.commit('changePendingMessage', { pendingMessage: "We're getting instances." });
			service
				.getLabeledMappingInstances(
					this.targetName, false, targetPage - 1
				)
				.then(
					success => {
						const labeledInstances = Instance.loadInstanceIdentifierTable(
							success.data
						);
						this.instances = labeledInstances;
						// this.$store.commit('changePendingMessage',{pendingMessage:""});
					},
					fail => {
						this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
						console.log(fail);
					}
				);
		},
		saveProgress: function () {
			service.saveProgress().then(
				success => {
					this.$store.commit('changePendingMessage', { pendingMessage: "Your extractors have been saved." });
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		returnToMain: function () {
			this.$router.replace({ name: "LearnItMain" });
		},
		markAllUnFrozenToNA: function () {
			for (const [instanceIdStr, instance] of Object.entries(
				this.instances
			)
			) {
				if (instance.relationType === constants.trueNegativeLabel && instance.annotation === constants.instanceNoFrozenStr) {
					this.changeInstanceType(instanceIdStr, constants.trueNegativeLabel);
				}
			}
		}
	},
	created() { },
	mounted() {
		this.loadInstancesFromLabelMappings(this.currentPage);
	},
	props: {},
	computed: {
		instanceList() {
			return Object.keys(this.instances);
		},
		constants() {
			return constants;
		},
		targetName() {
			return this.$store.getters.targetName;
		},
		siblingOntologyTypes() {
			const ret = [];
			ret.push({
				text: 0 + "-" + constants.trueNegativeLabel,
				value:constants.trueNegativeLabel
			});
			let idx = 1;
			for(const siblingOntologyType of this.$store.getters.siblingOntologyTypes){
				ret.push({
					text: idx + "-" + siblingOntologyType,
					value:siblingOntologyType
				});
				idx++;
			}
			return ret;
		},
		numberOfPagesInLabelMapping() {
			return this.$store.getters.numberOfPagesInLabelMapping;
		}
	},
	watch: {
		currentPage(newVal, oldVal) {
			if (newVal !== oldVal) {
				this.loadInstancesFromLabelMappings(newVal);
			}
		}
	}
};
</script>

<style scoped>
.learnitListcard {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	padding-left: 10px;
	padding-right: 10px;
}
</style>