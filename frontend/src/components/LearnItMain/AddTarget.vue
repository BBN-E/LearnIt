<template>
	<v-layout
		row
		justify-center
	>

		<v-dialog
			v-model="dialog"
			persistent
			max-width="600"
		>
			<!-- <template v-slot:activator="{ on }">
				<v-btn color="primary" dark v-on="on">Open Dialog</v-btn>
			</template> -->
			<v-card>
				<v-card-title class="headline">Add New Target</v-card-title>
				<v-card-text>
					<v-form lazy-validation>
						<v-container>
							<v-text-field
								label="Name"
								v-model="newTargetName"
							></v-text-field>
							<v-text-field
								label="Description"
								v-model="description"
							></v-text-field>
							<v-switch label="Symmetrical"></v-switch>
							<v-select
								multiple
								label="First Slot Types"
								:items="entityTypes"
							></v-select>
							<v-select
								multiple
								label="Second Slot Types"
								:items="entityTypes"
							></v-select>
						</v-container>
					</v-form>
				</v-card-text>
				<v-card-actions>
					<v-spacer></v-spacer>
					<v-btn
						color="red darken-1"
						flat
						@click="closeDialog()"
					>Cancel</v-btn>
					<v-btn
						color="green darken-1"
						flat
						@click="addOntologyNode()"
					>Add</v-btn>
				</v-card-actions>
			</v-card>
		</v-dialog>
	</v-layout>
</template>

<script>
import service from '@/api/index.js';
export default {
	name: 'addTargetDialog',
	props: {
		dialog: {
			required: true,
			type: Boolean
		}
	},
	data: function () {
		return {
			entityTypes: ["PER", "ORG", "GPE", "LOC", "WEA", "VEH", "FAC"],
			newTargetName: "",
			description: ""
		};
	},
	methods: {
		closeDialog: function () {
			this.$parent.$emit('closeDialog', "dialog");
		},
		addOntologyNode: function () {
			const getRoot = (node) => {
				while (node.parent) node = node.parent;
				return node;
			}
			const slot0SpanningType = getRoot(this.$parent.addTargetFocusRoot).slot0SpanningType;
			const slot1SpanningType = getRoot(this.$parent.addTargetFocusRoot).slot1SpanningType;
			service.addOntologyNode(this.$parent.addTargetFocusRoot.originalKey, this.newTargetName, this.description, slot0SpanningType, slot1SpanningType).then(
				success => {
					// This is not fully implemented!!!!!!
					service.addTargetAndScoreTable(this.newTargetName, this.description, "", "", false, slot0SpanningType, slot1SpanningType).then(
						success => {
							this.newTargetName = "";
							this.description = "";
							this.closeDialog();
						},
						fail => {
							this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
							console.log(fail);
						}
					);

				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail)
				}
			);
		}
	}
}
</script>

<style scoped>
</style>