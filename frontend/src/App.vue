<template>
	<v-app id="app">
		<v-content style="width:100%;height:100vh">
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
			<router-view />
		</v-content>
	</v-app>
</template>

<script>
import store from '@/store/index.js';
export default {
	name: 'App',
	store,
	data() {
		return {
			pendingSnackbar: false,
			pendingSnackbarTimeout: 4000,
			pendingSnackbarText: "",
			errorSnackbar: false,
			errorSnackbarTimeout: 4000,
			errorSnackbarText: ""
		}
	},
	computed: {
		pendingMessage() {
			return this.$store.getters.pendingMessage;
		},
		errorMessage() {
			return this.$store.getters.errorMessage;
		}
	},
	watch: {
		pendingMessage: function (newVal, oldVal) {
			if (newVal && newVal.length > 0) {
				this.pendingSnackbarText = newVal;
				this.pendingSnackbar = true;
			}
			else if (!newVal || newVal.length == 0) {
				this.pendingSnackbar = false;
			}
		},
		pendingSnackbar: function (newVal, oldVal) {
			if (!newVal || newVal.length < 1) {
				this.$store.commit('changePendingMessage', { pendingMessage: "" });
			}
		},
		errorMessage: function (newVal, oldVal) {
			if (newVal && newVal.length > 0) {
				this.errorSnackbarText = newVal;
				this.errorSnackbar = true;
			}
			else if (!newVal || newVal.length == 0) {
				this.errorSnackbar = false;
			}
		},
		errorSnackbar: function (newVal, oldVal) {
			if (!newVal || newVal.length < 1) {
				this.$store.commit('changeErrorMessage', { errorMessage: "" });
			}
		}
	}
}
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
</style>
