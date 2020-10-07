<template>
	<div

		class="rootContainer learnitListcard"
	>
		<splitpanes
			horizontal
			style="display:flex;flex-direction:column;height:100%;width:100%"
		>
			<pane
				class="learnitListcard"
				style="display:flex;flex-direction:column"
			>
				<v-chart
					style="flex-grow:1;flex-shrink:1;width:100%;padding-bottom:15px"
					:options="echartOption"
					autoresize
					@click="hoverPoint"
				/>
			</pane>
			<pane class="learnitListcard">
				<div style="display:flex;flex-direction:column;height:100%">
					<v-data-table
						:headers="datatableHeader"
						:items="eventFramesAggr"
						style="height:100%;overflow-y:auto;"
						calculate-widths
						@click:row="getInstances"
					>
						<template v-slot:items="props">
							<td
								v-for="(field,fieldIdx) in datatableHeader"
								:key="fieldIdx"
								@click="getInstances(props.item)"
							>{{props.item[field["value"]]}}</td>
						</template>
					</v-data-table>
				</div>
			</pane>
			<pane class="learnitListcard">
				<div style="display:flex;flex-direction:column;height:100%;overflow-y:scroll">
					<p
						v-for="(instance,instanceIdx) in instanceEventFrames"
						:key="instanceIdx"
					>
						<span
							v-for="(token,tokenIdx) in DocIdToSentIdToTokens[instance.docId][instance.sentIdx]"
							:key="tokenIdx"
						>
							<font :class="markingTriggerOrArgument(instance,tokenIdx)">{{token}}</font>
							<font v-text="space_char"></font>
						</span>
					</p>
				</div>
			</pane>
		</splitpanes>
	</div>
</template>

<script>
import {Splitpanes,Pane} from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import service from "@/api/index.js";
import constants from "@/constants.js";
import ECharts from "vue-echarts";
import echarts from "echarts";
import store from "@/store/index.js";
export default {
	name: "TimelineMain",
	store,
	data: function () {
		return {
			eventFramesAggr:[],
			instanceEventFrames:[],
			DocIdToSentIdToTokens:{},
			space_char:" ",
			focusStartTimeStamp: 0,
			focusEndTimeStamp: 0,
			datatableHeader: []
		};
	},
	components: {
		Splitpanes,
		Pane,
		"v-chart": ECharts
	},
	mounted() { },
	methods: {
		getInstances:function(evt) {
			const self = this;
			const constraint = {};
			for (const [k, v] of Object.entries(evt)) {
				if (k !== "cnt") {
					constraint[k] = v;
				}
			}
			self.instanceEventFrames.splice(0, this.instanceEventFrames.length);
			self.DocIdToSentIdToTokens = {};
			service
				.getEventFrame(
					this.currentTimelineEventType,
					this.focusStartTimeStamp,
					this.focusEndTimeStamp,
					constraint
				)
				.then(
					function(success) {
						const data = success.data;
						self.DocIdToSentIdToTokens = data.doc_id_to_sent_id_to_tokens;
						data.event_frames.map(val => {
							self.instanceEventFrames.push(val);
						});
					},
					function(fail) {
						console.log(fail);
					}
				);
		},
		markingTriggerOrArgument:function(event_frame, tokenIdx) {
			const judgeSpan = (spanStartIdx, spanEndIdx, tokenIdx) => {
				return tokenIdx >= spanStartIdx && tokenIdx <= spanEndIdx;
			};
			const triggerStart = event_frame.anchorStartIdx;
			const triggerEnd = event_frame.anchorEndIdx;
			const marking = {
				triggerSpan: judgeSpan(triggerStart, triggerEnd, tokenIdx),
				argumentSpan: false
			};

			for (const argument of event_frame["arguments"]) {
				const argumentStart = argument["argStartIdx"];
				const argumentEnd = argument["argEndIdx"];
				marking.argumentSpan =
					marking.argumentSpan ||
					judgeSpan(argumentStart, argumentEnd, tokenIdx);
			}
			return marking;
		},
		hoverPoint:function(evt) {
			const self = this;
			const clickedTimeStamp = this.focusEventTypeStatistics[evt.dataIndex][0];
			const startMoment = Math.floor(new Date(clickedTimeStamp) / 1000);
			const endMoment = Math.floor(new Date(clickedTimeStamp) / 1000 + 86400);
			this.focusStartTimeStamp = startMoment;
			this.focusEndTimeStamp = endMoment;
			this.eventFramesAggr.splice(0, this.eventFramesAggr.length);
			this.datatableHeader.splice(0, this.datatableHeader.length);
			service
				.getEntityList(this.currentTimelineEventType, startMoment, endMoment)
				.then(
					function(success) {
						
						for (const newEntry of success.data) {
							self.eventFramesAggr.push(newEntry);
						}
						const keySet = new Set();
						self.eventFramesAggr.map(val => {
							for (const k of Object.keys(val)) {
								keySet.add(k);
							}
						});
						
						[...keySet].map(val => {
							self.datatableHeader.push({
								text: val,
								value: val,
								sortable: true
							});
						});
					},
					function(fail) {
						console.log(fail);
					}
				);
		}
	},
	computed: {
		echartOption() {
			return {
				title: {
					text: this.currentTimelineEventType
				},
				tooltip: {
					trigger: "axis"
				},
				xAxis: {
					data: this.focusEventTypeStatistics.map(val => {
						const date = new Date(val[0]);
						return (
							String(date.getUTCFullYear()) +
							"-" +
							String(date.getUTCMonth() + 1)
						);
					})
				},
				yAxis: {
					splitLine: {
						show: false
					}
				},
				toolbox: {
					left: "center",
					feature: {
						dataZoom: {
							yAxisIndex: "none"
						},
						restore: {},
						saveAsImage: {}
					}
				},
				dataZoom: [
					{},
					{
						type: "inside"
					}
				],
				series: {
					name: this.currentTimelineEventType,
					type: "line",
					data: this.focusEventTypeStatistics.map(val => {
						return val[1];
					})
				}
			};
		},
		currentTimelineEventType() {
			return this.$store.getters.currentTimelineEventType;
		},
		focusEventTypeStatistics() {
			return this.$store.getters.currentEventTypeTimelineCount;
		}
	},
	watch: {

	}
};
</script>

<style scope>
/* #timelineMainView {
	height: 100%;
	width: 100%;
	display: flex;
	padding: 0;
	margin: 0;
} */

.triggerSpan {
	color: darkblue;
	border-bottom: 2px solid blue;
}
.argumentSpan {
	color: darkgreen;
}
</style>
