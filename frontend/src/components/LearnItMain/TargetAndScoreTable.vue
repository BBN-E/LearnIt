<template>
	<div class="learnitListcard">
		<splitpanes style="display:flex;flex-direction:row" class="learnitListcard">
			<pane class="learnitListcard">
				<LearnitPatternPanel />
			</pane>
			<pane class="learnitListcard">
				<SeedPanel />
			</pane>

		</splitpanes>
	</div>
</template>

<script>
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import LearnitPatternPanel from "@/components/LearnItMain/LearnitPatternPanel.vue";
import SeedPanel from "@/components/LearnItMain/SeedPanel.vue";
export default {
	name: "TargetAndScoreTable",
	data: function () {
		return {

		};
	},
	methods: {

	},
	components: {
		LearnitPatternPanel,
		SeedPanel,
		Splitpanes,
		Pane
	},
	created: function () {

	},
	props: {

	},
	computed: {

	}
};
</script>

<style scope>
.selected {
	border: 2px black solid;
}
rect {
	fill: #ccc;
	cursor: pointer;
}
.active {
	fill: orange;
	stroke: orange;
}

.activelink {
	fill: none;
	stroke: orange;
	stroke-width: 2.5px;
}

.label {
	fill: white;
	font-family: sans-serif;
	pointer-events: none;
}
.link {
	fill: none;
	stroke: #ccc;
	stroke-width: 2.5px;
}
</style>
