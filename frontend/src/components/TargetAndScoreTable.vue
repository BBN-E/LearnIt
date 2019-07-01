<template>
	<splitpanes style="display:flex;flex-direction:row;flex-basis:50%">
		<LearnitPatternPanel />
		<SeedPanel />
	</splitpanes>
</template>

<script>
import Splitpanes from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import LearnitPatternPanel from "@/components/LearnitPatternPanel.vue";
import SeedPanel from "@/components/SeedPanel.vue";
import normalizedPattern from "@/objects/LearnitPattern.js";
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
		Splitpanes
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
</style>
