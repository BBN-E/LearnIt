<template>
	<v-layout column>
		<!-- <v-btn @click="toggleOntologyPanel">Open Ontology</v-btn> -->
		<v-dialog
			v-model="ontologyPanelToggle"
			fullscreen
			hide-overlay
			transition="dialog-bottom-transition"
		>
			<v-card>
				<v-toolbar
					color="teal"
					dark
				>
					<v-btn
						icon
						dark
						@click="toggleOntologyPanel()"
					>
						<v-icon>close</v-icon>
					</v-btn>
					<v-toolbar-title>Ontology Panel</v-toolbar-title>

					<v-spacer></v-spacer>

				</v-toolbar>

				<v-card-text>
					<v-layout column>
						<span class="subheading">Event <v-btn
								icon
								@click="toggleOntologyTree('unaryEvent')"
							>
								<v-icon>{{toggleUnaryEvent?"expand_more":"expand_less"}}</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="addNewTarget(unaryEvent,$event)"
							>
								<v-icon>add</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="switchToLabelMappingAnnotationView(unaryEvent)"
							>
								<v-icon>reorder</v-icon>
							</v-btn>
						</span>
						<v-flex style="overflow-y:scoll">
							<v-treeview
								:items="unaryEvent.children"
								item-key="originalKey"
								item-text="originalKey"
								:open="unaryEvent.openNodes"
							>
								<template v-slot:label="{ item }">
									<span v-text="item.originalKey"></span>
									<v-btn
										icon
										@click="addNewTarget(item,$event)"
									>
										<v-icon>add</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="changeTarget(item)"
									>
										<v-icon>forward</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="showEventTimeline(item)"
									>
										<v-icon>history</v-icon>
									</v-btn>
								</template>
							</v-treeview>
						</v-flex>
						<span class="subheading">Event-Event Relation <v-btn
								icon
								@click="toggleOntologyTree('binaryEvent')"
							>
								<v-icon>{{toggleBinaryEvent?"expand_more":"expand_less"}}</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="addNewTarget(binaryEvent,$event)"
							>
								<v-icon>add</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="switchToLabelMappingAnnotationView(binaryEvent)"
							>
								<v-icon>reorder</v-icon>
							</v-btn>
						</span>
						<v-flex style="overflow-y:scoll">
							<v-treeview
								:items="binaryEvent.children"
								item-key="originalKey"
								item-text="originalKey"
								:open="binaryEvent.openNodes"
							>
								<template v-slot:label="{ item }">
									<span v-text="item.originalKey"></span>
									<v-btn
										icon
										@click="addNewTarget(item,$event)"
									>
										<v-icon>add</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="changeTarget(item)"
									>
										<v-icon>forward</v-icon>
									</v-btn>
								</template>
							</v-treeview>
						</v-flex>
						<span class="subheading">Entity <v-btn
								icon
								@click="toggleOntologyTree('unaryEntity')"
							>
								<v-icon>{{toggleUnaryEntity?"expand_more":"expand_less"}}</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="addNewTarget(unaryEntity,$event)"
							>
								<v-icon>add</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="switchToLabelMappingAnnotationView(unaryEntity)"
							>
								<v-icon>reorder</v-icon>
							</v-btn>
						</span>
						<v-flex style="overflow-y:scoll">
							<v-treeview
								:items="unaryEntity.children"
								item-key="originalKey"
								item-text="originalKey"
								:open="unaryEntity.openNodes"
							>
								<template v-slot:label="{ item }">
									<span v-text="item.originalKey"></span>
									<v-btn
										icon
										@click="addNewTarget(item,$event)"
									>
										<v-icon>add</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="changeTarget(item)"
									>
										<v-icon>forward</v-icon>
									</v-btn>
								</template>
							</v-treeview>
						</v-flex>
						<span class="subheading">Entity-Entity Relation <v-btn
								icon
								@click="toggleOntologyTree('binaryEntity')"
							>
								<v-icon>{{toggleBinaryEntity?"expand_more":"expand_less"}}</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="addNewTarget(binaryEntity,$event)"
							>
								<v-icon>add</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="switchToLabelMappingAnnotationView(binaryEntity)"
							>
								<v-icon>reorder</v-icon>
							</v-btn>
						</span>
						<v-flex style="overflow-y:scoll">
							<v-treeview
								:items="binaryEntity.children"
								item-key="originalKey"
								item-text="originalKey"
								:open="binaryEntity.openNodes"
							>
								<template v-slot:label="{ item }">
									<span v-text="item.originalKey"></span>
									<v-btn
										icon
										@click="addNewTarget(item,$event)"
									>
										<v-icon>add</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="changeTarget(item)"
									>
										<v-icon>forward</v-icon>
									</v-btn>
								</template>
							</v-treeview>
						</v-flex>
						<span class="subheading">Event-Entity/ValueMention Relation <v-btn
								icon
								@click="toggleOntologyTree('binaryEventEntityOrValueMention')"
							>
								<v-icon>{{toggleBinaryEventEntityOrValueMention?"expand_more":"expand_less"}}</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="addNewTarget(binaryEventEntityOrValueMention,$event)"
							>
								<v-icon>add</v-icon>
							</v-btn>
							<v-btn
								icon
								@click="switchToLabelMappingAnnotationView(binaryEventEntityOrValueMention)"
							>
								<v-icon>reorder</v-icon>
							</v-btn>
						</span>
						<v-flex style="overflow-y:scoll">
							<v-treeview
								:items="binaryEventEntityOrValueMention.children"
								item-key="originalKey"
								item-text="originalKey"
								:open="binaryEventEntityOrValueMention.openNodes"
							>
								<template v-slot:label="{ item }">
									<span v-text="item.originalKey"></span>
									<v-btn
										icon
										@click="addNewTarget(item,$event)"
									>
										<v-icon>add</v-icon>
									</v-btn>
									<v-btn
										icon
										@click="changeTarget(item)"
									>
										<v-icon>forward</v-icon>
									</v-btn>
								</template>
							</v-treeview>
						</v-flex>
					</v-layout>

				</v-card-text>
			</v-card>

		</v-dialog>

		<AddTarget :dialog.sync="addTargetToggle" />
	</v-layout>
</template>

<script>
import AddTarget from "@/components/LearnItMain/AddTarget.vue";
import service from "@/api/index.js";
import store from "@/store/index.js";
export default {
	name: 'OntologyView',
	store,
	computed: {
		ontologyPanelToggle() {
			return this.$store.getters.displayOntologyPanel;
		}
	},
	methods: {
		bindParent: function (root) {
			const self = this;
			root.parent = null;
			for (const child of root.children) {
				self.bindParent(child);
				child.parent = root;
			}
		},
		addNewTarget: function (root, evt) {
			this.addTargetFocusRoot = root;
			this.addTargetToggle = !this.addTargetToggle;
		},
		toggleOntologyPanel: function () {
			this.$store.commit("toggleDisplayOntologyPanel");
		},
		reloadOntologyTree: function () {
			service.getOntology().then(success => {
				this.unaryEvent.children = success.data.unaryEvent.children;
				this.bindParent(this.unaryEvent);
				this.unaryEvent.originalKey = success.data.unaryEvent.originalKey;

				this.binaryEvent.children = success.data.binaryEvent.children;
				this.bindParent(this.binaryEvent);
				this.binaryEvent.originalKey = success.data.binaryEvent.originalKey;

				this.binaryEntity.children = success.data.binaryEntity.children;
				this.bindParent(this.binaryEntity);
				this.binaryEntity.originalKey = success.data.binaryEntity.originalKey;

				this.unaryEntity.children = success.data.unaryEntity.children;
				this.bindParent(this.unaryEntity);
				this.unaryEntity.originalKey = success.data.unaryEntity.originalKey;

				this.binaryEventEntityOrValueMention.children = success.data.binaryEventEntityOrValueMention.children;
				this.bindParent(this.binaryEventEntityOrValueMention);
				this.binaryEventEntityOrValueMention.originalKey = success.data.binaryEventEntityOrValueMention.originalKey;

			},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail)
				}
			);
		},
		dfsGetAllChildren: function (node, bufferArr) {
			bufferArr.add(node.originalKey);
			for (const child of node.children) {
				this.dfsGetAllChildren(child, bufferArr);
			}
		},
		changeTarget: function (node) {
			let root = node;
			while (root.parent) root = root.parent;
			const slot0SpanningType = root.slot0SpanningType;
			const slot1SpanningType = root.slot1SpanningType;

			const siblingTypes = new Set();
			for (const child of root.children) {
				this.dfsGetAllChildren(child, siblingTypes);
			}
			this.$store.dispatch("loadTargetAndScoreTable", { targetName: node.originalKey, description: "NA", slot0EntityTypes: "", slot1EntityTypes: "", symmetric: false, slot0SpanningType: slot0SpanningType, slot1SpanningType: slot1SpanningType, tryCreateTarget: true }).then(
				success => {
					this.$store.commit('changeSiblingOntologyTypes', { 'siblingOntologyTypes': Array.from(siblingTypes) });
					this.toggleOntologyPanel();

				},
				fail => {
					this.$store.commit('changeSiblingOntologyTypes', { 'siblingOntologyTypes': [] });
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}

			);

		},
		showEventTimeline: function (node) {
			this.$store.dispatch("lookingEventTimeline", {timelineEventType:node.originalKey}).then(
				success => {
					this.$router.replace({ name: "TimelineMain" });
				},
				fail => {
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);

		},
		switchToLabelMappingAnnotationView: function (node) {
			let root = node;
			while (root.parent) root = root.parent;
			const slot0SpanningType = root.slot0SpanningType;
			const slot1SpanningType = root.slot1SpanningType;

			const siblingTypes = new Set();
			for (const child of root.children) {
				this.dfsGetAllChildren(child, siblingTypes);
			}
			this.$store.dispatch("loadLabelMappingAnnotationView", { targetName: node.originalKey }).then(
				success => {
					this.$store.commit('changeSiblingOntologyTypes', { 'siblingOntologyTypes': Array.from(siblingTypes) });
					this.$router.replace({ name: "LabeledMappingsView" });
				},
				fail => {
					this.$store.commit('changeSiblingOntologyTypes', { 'siblingOntologyTypes': [] });
					this.$store.commit('changeErrorMessage', { errorMessage: JSON.stringify(fail) });
					console.log(fail);
				}
			);

		},
		toggleTreeOn: function (root, arrPtr) {
			arrPtr.push(root._id);
			for (const child of root.children) {
				this.toggleTreeOn(child, arrPtr);
			}
		},
		toggleOntologyTree: function (treeName) {
			switch (treeName) {
				case "unaryEvent":
					if (this.toggleUnaryEvent) this.toggleTreeOn(this.unaryEvent, this.unaryEvent.openNodes);
					else this.unaryEvent.openNodes = [];
					this.toggleUnaryEvent = !this.toggleUnaryEvent;
					break;

				case "binaryEvent":
					if (this.toggleBinaryEvent) this.toggleTreeOn(this.binaryEvent, this.binaryEvent.openNodes);
					else this.binaryEvent.openNodes = [];
					this.toggleBinaryEvent = !this.toggleBinaryEvent;
					break;

				case "binaryEntity":
					if (this.toggleBinaryEntity) this.toggleTreeOn(this.binaryEntity, this.binaryEntity.openNodes);
					else this.binaryEntity.openNodes = [];
					this.toggleBinaryEntity = !this.toggleBinaryEntity;
					break;
				case "unaryEntity":
					if (this.toggleUnaryEntity) this.toggleTreeOn(this.unaryEntity, this.unaryEntity.openNodes);
					else this.unaryEntity.openNodes = [];
					this.toggleUnaryEntity = !this.toggleUnaryEntity;
					break;
				case "binaryEventEntityOrValueMention":
					if (this.toggleBinaryEventEntityOrValueMention) this.toggleTreeOn(this.binaryEventEntityOrValueMention, this.binaryEventEntityOrValueMention.openNodes);
					else this.binaryEventEntityOrValueMention.openNodes = [];
					this.toggleBinaryEventEntityOrValueMention = !this.toggleBinaryEventEntityOrValueMention;
					break;
			}
		}
	},
	mounted() {
		this.reloadOntologyTree();
	},
	created() {
		const self = this;
		this.$on('closeDialog', (dialogName) => {
			this.addTargetToggle = !this.addTargetToggle;
			this.reloadOntologyTree();
		});

	},
	data: function () {
		const self = this;
		const unaryEvent = {
			originalKey: "Unary Event",
			children: [],
			slot0SpanningType: "EventMention",
			slot1SpanningType: "Empty",
			openNodes: []
		};
		self.bindParent(unaryEvent);
		const binaryEvent = {
			originalKey: "Binary Event",
			children: [],
			slot0SpanningType: "EventMention",
			slot1SpanningType: "EventMention",
			openNodes: []
		};
		self.bindParent(binaryEvent);
		const binaryEntity = {
			originalKey: "Binary Entity",
			children: [],
			slot0SpanningType: "Mention",
			slot1SpanningType: "Mention",
			openNodes: []
		}
		self.bindParent(binaryEntity);
		const unaryEntity = {
			originalKey: "Unary Entity",
			children: [],
			slot0SpanningType: "Mention",
			slot1SpanningType: "Empty",
			openNodes: []
		};
		self.bindParent(unaryEntity);
		const binaryEventEntityOrValueMention = {
			originalKey: "Event EntityOrValueMention",
			children: [],
			slot0SpanningType: "EventMention",
			slot1SpanningType: "Mention,ValueMention",
			openNodes: []
		};

		self.bindParent(binaryEventEntityOrValueMention);
		return {
			unaryEvent: unaryEvent,
			binaryEvent: binaryEvent,
			binaryEntity: binaryEntity,
			unaryEntity: unaryEntity,
			binaryEventEntityOrValueMention: binaryEventEntityOrValueMention,
			addTargetToggle: false,
			addTargetFocusRoot: null,
			toggleUnaryEvent: false,
			toggleBinaryEvent: false,
			toggleBinaryEntity: false,
			toggleUnaryEntity: false,
			toggleBinaryEventEntityOrValueMention: false
		};
	},
	components: {
		AddTarget
	}
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.targetroot {
	margin: 20px;
}
</style>
