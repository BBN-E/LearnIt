<template>
	<v-app id="app">
		<v-content style="width:100%;height:100vh">
			<OntologyView />
			<v-toolbar>
				<v-toolbar-title>LearnIt KB Builder</v-toolbar-title>
				<span class="title align-content-center pl-4 pr-2">{{targetName}}</span>
				<v-toolbar-items class="hidden-sm-and-down">
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
			</v-toolbar>
			<splitpanes
			 horizontal
			 style="display:flex;flex-direction:column;height:93%"
			>
				<TargetAndScoreTable />
				<InstancePanel />
			</splitpanes>
			<v-snackbar
			 v-model="pendingSnackbar"
			 multi-line
			 :timeout="pendingSnackbarTimeout"
			 top
			 color="info"
			>
				{{pendingSnackbarText}}
				<v-btn
				 color="white"
				 flat
				 @click="pendingSnackbar = false"
				>
					Close
				</v-btn>
			</v-snackbar>
			<v-snackbar
			 v-model="errorSnackbar"
			 multi-line
			 :timeout="errorSnackbarTimeout"
			 top
			 color="error"
			>
				{{ errorSnackbarText }}
				<v-btn
				 color="white"
				 flat
				 @click="errorSnackbar = false"
				>
					Close
				</v-btn>
			</v-snackbar>
		</v-content>
	</v-app>
</template>

<script>
import service from "@/api/index.js";
import store from '@/store/index.js';
import OntologyView from "@/components/OntologyView.vue";
import Splitpanes from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import TargetAndScoreTable from "@/components/TargetAndScoreTable.vue";
import InstancePanel from "@/components/InstancePanel.vue";
export default {
	name: "app",
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
					this.$store.commit('changeErrorMessage',{errorMessage:JSON.stringify(fail)});
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
					this.$store.commit('changeErrorMessage',{errorMessage:JSON.stringify(fail)});
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
					this.$store.commit('changeErrorMessage',{errorMessage:JSON.stringify(fail)});
					console.log(fail);
				}
			);
		}
	},
	components: {
		OntologyView,
		TargetAndScoreTable,
		InstancePanel,
		Splitpanes
	},
	data() {
		return {
			pendingSnackbar: false,
			pendingSnackbarTimeout: 4000,
			pendingSnackbarText: "",
			errorSnackbar: false,
			errorSnackbarTimeout: 4000,
			errorSnackbarText: ""
		};
	},
	computed: {
		targetName() {
			return this.$store.getters.targetName;
		},
		pendingMessage() {
			return this.$store.getters.pendingMessage;
		},
		errorMessage() {
			return this.$store.getters.errorMessage;
		}
	},
	created() {

	},
	watch: {
		pendingMessage: function (newVal, oldVal) {
			if (newVal && newVal.length > 0) {
				this.pendingSnackbarText = newVal;
				this.pendingSnackbar = true;
			}
			else if(!newVal || newVal.length == 0){
				this.pendingSnackbar = false;
			}
		},
		pendingSnackbar:function(newVal,oldVal){
			if(!newVal || newVal.length < 1){
				this.$store.commit('changePendingMessage',{pendingMessage:""});
			}
		},
		errorMessage: function (newVal, oldVal) {
			if (newVal && newVal.length > 0) {
				this.errorSnackbarText = newVal;
				this.errorSnackbar = true;
			}
			else if(!newVal || newVal.length == 0){
				this.errorSnackbar = false;
			}
		},
		errorSnackbar:function(newVal,oldVal){
			if(!newVal || newVal.length<1){
				this.$store.commit('changeErrorMessage',{errorMessage:""});
			}
		}
	}
};
</script>

<style>
html,
body,
#app {
	/* font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px; */
	width: 100% !important;
	height: 100vh !important;
	overflow: hidden !important;
}

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
	padding-left: 10px;
	padding-right:10px;
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
