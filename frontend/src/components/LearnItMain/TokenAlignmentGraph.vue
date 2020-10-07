<template>
	<div style="width:95%;overflow-x:scroll"> <svg
			:width="width"
			:height="height"
		>
			<g>
				<g class="nodes">
					<g
						class="unit"
						v-for="(node,nodeIdx) in computedNodes"
						:key="node.nid"
					>
						<rect
							:x="node.x"
							:y="node.y"
							:width="node.boxWidth"
							:height="node.boxHeight"
							rx="6"
							ry="6"
							:class="{node:true,active:node.highlighted}"
							@mouseover="highlightAllConnectedNodesAndEdges(node,true)"
							@mouseout="highlightAllConnectedNodesAndEdges(node,false)"
						></rect>
						<text
							class="label"
							:x="node.x+14"
							:y="node.y+15"
						>{{node.name}}</text>
					</g>
					<path
						:class="{link:!edge.highlighted,activelink:edge.highlighted}"
						v-for="(edge,edgeIdx) in computedEdges"
						:key="edge.eid"
						:d="edge.d"
					></path>
				</g>
			</g>

		</svg></div>

</template>

<script>

export default {
	name: "TokenAlignmentGraph",
	data() {
		return {
			width: 800,
			height: 150,
			boxWidth: 120,
			boxHeight: 20,
			gap: {
				width: 12,
				height: 80
			},
			margin: {
				top: 16,
				right: 16,
				bottom: 16,
				left: 16
			},
			nodes: [],
			edges: []
		};
	},
	props: {
		matchInfoDisplay: Object,
		callbackForHover: Function
	},
	components: {

	},
	mounted() {
		const self = this;
		const matchInfoDisplay = self.matchInfoDisplay;
		const primaryLanguage = matchInfoDisplay.primaryLanguage;
		const targetLanguages = [];
		for (const language of Object.keys(matchInfoDisplay.langDisplays)) {
			if (language !== primaryLanguage) {
				targetLanguages.push(language);
			}
		}
		const targetLanguage = targetLanguages[0];

		const srcTokens = matchInfoDisplay.langDisplays[primaryLanguage].sentenceTokens;
		const dstTokens = matchInfoDisplay.langDisplays[targetLanguage].sentenceTokens;

		const maximumTokenLength = Math.max(srcTokens.length, dstTokens.length);
		self.width = maximumTokenLength * self.boxWidth + self.gap.width * (maximumTokenLength - 1) + self.margin.left + self.margin.right+1;
		const tokenAlignmentTable = matchInfoDisplay.tokenAlignmentTable.table[primaryLanguage][targetLanguage];

		self.nodes = [];
		srcTokens.forEach((item, idx) => {
			self.nodes.push({ "lvl": 0, "name": item, "highlighted": false, "aux": {}, "nid": "src_" + idx });
		});
		dstTokens.forEach((item, idx) => {
			self.nodes.push({ "lvl": 1, "name": item, "highlighted": false, "aux": {}, "nid": "dst_" + idx });
		});
		self.edges = [];
		for (const [srcTokenIdx, dstTokenArray] of Object.entries(tokenAlignmentTable)) {
			for (const dstTokenIdx of dstTokenArray) {
				self.edges.push({
					"source": "src_" + srcTokenIdx,
					"target": "dst_" + dstTokenIdx,
					"highlighted": false,
					"aux": {}
				})
			}
		}
	},
	methods: {
		diagonal: function link(d) {
			return "M" + d.source.y + "," + d.source.x
				+ "C" + (d.source.y + d.target.y) / 2 + "," + d.source.x
				+ " " + (d.source.y + d.target.y) / 2 + "," + d.target.x
				+ " " + d.target.y + "," + d.target.x;
		},
		getAllConnectedNodes: function (startingNode, connectedNodes, connectedEdges) {
			const self = this;
			if (connectedNodes.includes(startingNode)) return;
			connectedNodes.push(startingNode);
			for (const edge of startingNode.connectedEdges) {
				// Only go one hoop
				if(edge.source === startingNode){
					connectedNodes.push(edge.target);
				}
				if(edge.target === startingNode){
					connectedNodes.push(edge.source);
				}
				connectedEdges.push(edge);
			}
		},
		highlightAllConnectedNodesAndEdges(node, shouldHighlight) {
			const self = this;
			const connectedNodeBuffer = [];
			const connectedEdgeBuffer = [];
			self.getAllConnectedNodes(node, connectedNodeBuffer, connectedEdgeBuffer);
			for (const node of connectedNodeBuffer) {
				node.aux.highlighted = shouldHighlight;
			}
			for (const edge of connectedEdgeBuffer) {
				edge.aux.highlighted = shouldHighlight;
			}
			if(shouldHighlight){
				self.callbackForHover(node);
			}
		}
	},
	computed: {
		computedNodes: function () {
			const self = this;
			const currentCursor = [];
			const count = [];

			self.nodes.forEach(node => {
				currentCursor[node.lvl] = 0;
				count[node.lvl] = 0;
			});
			let maxNumberOfNodes = 0;
			self.nodes.forEach(node =>{
				count[node.lvl]++;
				maxNumberOfNodes = Math.max(count[node.lvl],maxNumberOfNodes);
			});
			const ret = [];
			self.nodes.forEach((node, idx) => {
				const nodeObj = {
					x: self.margin.left +
						(self.boxWidth + self.gap.width) * (maxNumberOfNodes / count[node.lvl] ) * currentCursor[node.lvl],
					y:

						self.margin.top + node.lvl * (self.boxHeight + self.gap.height),
					name: node.name,
					boxWidth: self.boxWidth,
					boxHeight: self.boxHeight,
					nid: node.nid,
					aux: node,
					connectedEdges: [],
					highlighted: node.highlighted
				};
				ret.push(nodeObj);
				currentCursor[node.lvl]++;
			});
			return ret;
		},
		computedEdges: function () {
			const self = this;
			const nodeIdToNode = {};
			for (const nodeObj of self.computedNodes) {
				nodeIdToNode[nodeObj["nid"]] = nodeObj;
			}
			const ret = [];
			self.edges.forEach((edge, idx) => {
				const sourceNode = nodeIdToNode[edge.source];
				const targetNode = nodeIdToNode[edge.target];
				const oTarget = {
					x: targetNode.y,
					y: targetNode.x + 0.5 * targetNode.boxWidth

				};
				const oSource = {
					x: sourceNode.y + sourceNode.boxHeight,
					y: sourceNode.x + 0.5 * sourceNode.boxWidth,
				}
				const edgeObj = { 'eid': "edge_" + idx, 'd': self.diagonal({ source: oSource, target: oTarget, aux: edge.aux }), 'source': sourceNode, 'target': targetNode, 'highlighted': edge.highlighted, aux: edge };
				ret.push(edgeObj);
				sourceNode.connectedEdges.push(edgeObj);
				targetNode.connectedEdges.push(edgeObj);
			});
			return ret;
		}
	}
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
	margin: 40px 0 0;
}
ul {
	list-style-type: none;
	padding: 0;
}
li {
	display: inline-block;
	margin: 0 10px;
}
a {
	color: #42b983;
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
