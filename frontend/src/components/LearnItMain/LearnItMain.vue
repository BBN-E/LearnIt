<template>
	<div class="rootContainer learnitListcard">
		<OntologyView />
		<v-toolbar>
			<v-toolbar-title>LearnIt KB Builder</v-toolbar-title>
			<span class="title align-content-center pl-4 pr-2">{{targetName}}</span>
			<v-toolbar-items>
				<v-btn
					flat
					:disabled="targetName.length<1"
					@click="clearUnaccept()"
				>Clear Unaccept</v-btn>
				<v-btn
					flat
					:disabled="targetName.length<1"
					@click="clearAll()"
				>Clear Everything</v-btn>
				<v-btn
					flat
					:disabled="targetName.length<1"
					@click="loadTargetAndScoreTable()"
				>Refresh</v-btn>
				<v-btn
					flat
					@click="openOntologyPanel()"
				>Open Ontology Panel</v-btn>
			</v-toolbar-items>

			<v-spacer></v-spacer>
			<v-toolbar-items>
				<v-btn
					flat
					@click="saveProgress()"
				>Save Progress</v-btn>
			</v-toolbar-items>
            <v-toolbar-items>
                <v-btn
                    flat
                    @click="generateVisualization()"
                >Generate Visualization</v-btn>
            </v-toolbar-items>
		</v-toolbar>
		<splitpanes
			horizontal
			style="display:flex;flex-direction:column;height:93%"
		>
			<pane class="learnitListcard">
				<TargetAndScoreTable />
			</pane>

			<pane class="learnitListcard">
				<InstancePanel />
			</pane>

		</splitpanes>

	</div>
</template>

<script>
import service from "@/api/index.js";
import store from '@/store/index.js';
import OntologyView from "@/components/LearnItMain/OntologyView.vue";
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import TargetAndScoreTable from "@/components/LearnItMain/TargetAndScoreTable.vue";
import InstancePanel from "@/components/LearnItMain/InstancePanel.vue";
export default {
	name: "LearnItMain",
	store,
	methods: {
		openOntologyPanel: function () {
			this.$store.commit("toggleDisplayOntologyPanel");
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
		generateVisualization: function() {
            service.generateVisualization().then(
                success => {
                    this.$store.commit('changePendingMessage', { pendingMessage: success.data });
                },
                fail => {
                    this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
                    console.log(fail);
                }
            );
        },
		loadTargetAndScoreTable: function () {
			this.$store.dispatch('loadTargetAndScoreTable', { targetName: this.$store.getters.targetName });
		},
		clearUnaccept: function () {
			service.clearUnknown(this.$store.getters.targetName).then(
				success => {
					this.loadTargetAndScoreTable();
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		},
		clearAll: function () {
			service.clearAll(this.$store.getters.targetName).then(
				success => {
					this.loadTargetAndScoreTable();
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);
		}
	},
	components: {
		OntologyView,
		TargetAndScoreTable,
		InstancePanel,
		Splitpanes,
		Pane
	},
	data() {
		return {

		};
	},
	computed: {
		targetName() {
			return this.$store.getters.targetName;
		}
	},
	created() {

	}
};
</script>

<style scope>
.slot0 {
	background-color: lightblue;
	border-bottom: 2px solid blue;
}

.slot1 {
	background-color: lightgreen;
	border-top: 2px solid green;
}
.learnitListcard {
	height: 100%;
	display: flex;
	flex-direction: column;
	padding:3px 3px 3px 3px;
}

.rootContainer{
	padding:10px 10px 10px 10px;
}

.frozenGood {
	background-color: #4caf50;
}
.frozenBad {
	background-color: #f44336;
}
.splitpanes {
	background-color: #f8f8f8;
}

.splitpanes__splitter {
	background-color: #ccc;
	position: relative;
}
.splitpanes__splitter:before {
	content: "";
	position: absolute;
	left: 0;
	top: 0;
	transition: opacity 0.4s;
	background-color: rgba(255, 0, 0, 0.3);
	opacity: 0;
	z-index: 1;
}

.splitpanes--vertical > .splitpanes__splitter {
	width: 10px;
}
.splitpanes--horizontal > .splitpanes__splitter {
	height: 10px;
}

.splitpanes__splitter:hover:before {
	opacity: 1;
}

.splitpanes--vertical > .splitpanes__splitter:before {
	left: -10px;
	right: -10px;
	height: 100%;
}
.splitpanes--horizontal > .splitpanes__splitter:before {
	top: -10px;
	bottom: -10px;
	width: 100%;
}
</style>
