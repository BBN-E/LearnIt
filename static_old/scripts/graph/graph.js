/**
 *  graph.js - graph convenience wrapper for D3 force layout graphs.
 *  ================================================================
 *  author: mshafir@bbn.com
 *  Feb 2014
 */



/**
 * Core D3 Graph Wrapper for convenience
 * @constructor
 */
function Graph () {
	var self = this;
	
	self.width = 10000;
	self.height = 10000;
	self.charge = -4000;
	self.linkDistance = 200;

    self.nodes = [];
    self.nodeMap = {};
    self.edgeMap = {};
    self.edges = [];
	self.force = null;

    self.vis = null;
    self.circle = null;
    self.link = null;
    self.text = null;

    self.labeledEdges = false;


    /**
     * Configure force layout
     */
    self.reconfigure = function() {
        self.force.charge(-1000);
        self.update();
    }


    /**
     * Function for setting up up the Graph initially. Only needs to be called once.
     */
	self.setup = function() {
		self.force = d3.layout.force()
			.linkDistance(self.linkDistance)
            .charge(-1000)
            .size([$('#graph').width(), $('#graph').height()]);

		var svg = d3.select("#graph").append("svg")
			.attr("pointer-events", "all")
			.attr("width", self.width)
			.attr("height", self.height);
			
		self.vis = svg
		  .append('svg:g')
          .call(d3.behavior.zoom()
            .on("zoom", self.rescale))
			.on("dblclick.zoom", null)
		    .append('svg:g')
			  .on("mousedown", self.mousedown);

        self.vis.append('svg:rect')
			.attr('x', -self.width)
			.attr('y', -self.height)
			.attr('width', self.width*2)
			.attr('height', self.height*2)
			.attr('cursor','move')
			.attr('fill', 'white');

        self.vis.call(d3.behavior.zoom().on("zoom"), self.rescale);
		
		self.force
		  .nodes(self.nodes)
		  .links(self.edges)
		  .on("tick", self.tick);

        self.link   = self.vis.append("g").selectAll("line");
        self.linkText = self.vis.append("g").selectAll(".linkText");
        self.circle = self.vis.append("g").selectAll("circle");
        self.text   = self.vis.append("g").selectAll(".nodeText");
        self.edgepaths = self.vis.selectAll(".edgepath");
        self.edgelabels = self.vis.selectAll(".edgelabel");

        return self;
    }


    /**
     * Function for updating the graph after nodes/edges change
     */
    self.update = function() {
		self.force.start();

		self.link = self.link.data(self.edges);
        self.linkText = self.linkText.data(self.edges);
        self.circle = self.circle.data(self.nodes);
        self.text = self.text.data(self.nodes);
        self.edgepaths = self.edgepaths.data(self.edges);
        self.edgelabels = self.edgelabels.data(self.edges);

        /*
         Only for new items
         */
        self.link
            .enter().append("line")
            .on("mouseup", function(d) { d.select(); })
            .style("stroke", function(d) { return d.color; });


		self.circle
            .enter().append("circle")
            .style("fill", function(d) { return d.color; })
            .on("mousedown", function(d) { d.fixed = true; })
            .on("click", function(d) {d.select();})
            .call(self.force.drag);

		self.text
            .enter().append("text")
            .attr("class", "nodeText")
            .attr("y", ".31em")
            .text(function(d) { return d.text; });

        if (self.labeledEdges) {
            self.edgepaths
                .enter().append('path')
                .attr('d', function(d) {return 'M '+d.source.x+' '+d.source.y+' L '+ d.target.x +' '+d.target.y;})
                .attr('class','edgepath')
                .attr('fill-opacity',0)
                .attr('stroke-opacity',0)
                .attr('id',function(d,i) {return 'edgepath'+i;})
                .style("pointer-events", "none");

            self.edgelabels
                .enter().append('text')
                .style('pointer-events', 'none')
                .attr('class','edgelabel edgeText')
                .attr('id',function(d,i){return 'edgelabel'+i;})

                .append('textPath')
                .attr('xlink:href',function(d,i) {return '#edgepath'+i})
                .style("pointer-events", "none")
                .text(function(d,i){return d.text});
        }

        /*
         Every Refresh
         */
        self.vis.selectAll("line")
            .attr("class", function(d) { return d.selected() ? "selectedLink" : "link"; })
            .on("click", function(d) { d.select();})
            .style("opacity", function(d) { return d.opacity(); })
            .style("visibility", function(d) {
                return d.visible() ? "visible" : "hidden"});

        self.vis.selectAll("circle")
            .attr("class", function(d) { return d.selected() ? "selectedNode" : "node"; })
            .attr("r", function(d) { return d.size(); })
            .style("visibility", function(d) {
                return d.visible() ? "visible" : "hidden"});

        self.vis.selectAll(".nodeText")
            .attr("class", function(d) { return "nodeText "+(d.selected() ? "selectedText" : "text"); })
            .attr("x", function(d) { return d.size()+3; })
            .style("visibility", function(d) {
                return d.visible() ? "visible" : "hidden"});

        if (self.labeledEdges) {
            self.vis.selectAll(".edgeText")
                .attr('dx',80)
                .attr('dy',0)
                .attr('font-size',15);
        }
			
		if (d3.event) {
			// prevent browser's default behavior
			d3.event.preventDefault();
		} 
	}

    /**
     * MouseDown event
     */
    self.mousedown = function() {
        // allow panning if nothing is selected
        self.vis.call(d3.behavior.zoom().on("zoom"), self.rescale);
    }

    /**
     * rescale g
     */
    self.rescale = function() {
        var trans=d3.event.translate;
        var scale=d3.event.scale;
        self.vis.attr("transform",
               "translate(" + trans + ") scale(" + scale + ")");
    }

    self.tick = function() {
        self.link
            .attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });
        self.linkText.attr("transform", self.transformEdgeTextSimple);
        self.circle.attr("transform", self.transform);
        self.text.attr("transform", self.transform);
        if (self.labeledEdges) {
            self.edgepaths
                .attr('d',
                    function(d) {
                        var path='M '+d.source.x+' '+d.source.y+' L '+ d.target.x +' '+d.target.y;
                        return path
                    });

            self.edgelabels.attr('transform',function(d,i){
                if (d.target.x<d.source.x){
                    bbox = this.getBBox();
                    rx = bbox.x+bbox.width/2;
                    ry = bbox.y+bbox.height/2;
                    return 'rotate(180 '+rx+' '+ry+')';
                }
                else {
                    return 'rotate(0)';
                }
            });
        }
    }

    self.toDegrees = function(angle) {
        return angle * (180 / Math.PI);
    }

    self.transformEdgeText = function(edge) {
        //get center
        var centx = edge.source.x;
        var centy = edge.source.y;

        //toa
        var adj = (edge.target.x - edge.source.x);
        var op = (edge.target.y - edge.source.y);
        var angle = self.toDegrees(Math.atan(op/adj));

        var midWid =  this.getComputedTextLength()/2;
        var midLine = Math.sqrt(adj*adj+op*op)/2;

        if (edge.target.x > edge.source.x) {
            var lenChange = midLine-midWid;
        } else {
            var lenChange = -(midLine+midWid);
        }

        var translate = "translate("+centx+","+centy+")";
        var rotate = "rotate("+angle+")";
        var translate2 = "translate("+lenChange+",0)";
        return translate+" "+rotate+" "+translate2;
    }

    self.transformEdgeTextSimple = function(d,i) {
        if (d.target.x<d.source.x){
            bbox = this.getBBox();
            rx = bbox.x+bbox.width/2;
            ry = bbox.y+bbox.height/2;
            return 'rotate(180 '+rx+' '+ry+')';
        }
        else {
            return 'rotate(0)';
        }
    }

    /**
     * Function for translating
     * @param d
     * @returns {string}
     */
    self.transform = function(d) {
        return "translate(" + d.x + "," + d.y + ")";
    }


    /*
      *PUBLIC* FUNCTIONS FOR CALLING
      ===========================================

      For our purposes here, node = EntityNode and edge = RelationEdge
     */

    self.addNodes= function(nodes) {
        $.each(nodes, function(i,node) { self.addNode(node); });
    }

    self.addNode = function(node) {
        if (node.id in self.nodeMap) return;
        self.nodeMap[node.id] = node;
        node.graph = self;
        node.index = self.nodes.length;
        self.nodes.push(node);
        self.update();
    }

    self.getNode = function(id) {
        return self.nodeMap[id];
    }

    self.hasNode = function(id) {
        return id in self.nodeMap;
    }

    self.addEdges = function(edges) {
        $.each(edges, function(i,edge) { self.addEdge(edge); });
    }

    self.addEdge = function(edge) {
        edge.source = self.getNode(edge.sourceNode.id).index;
        edge.target = self.getNode(edge.targetNode.id).index;
        if (edge.sourceNode.id in self.edgeMap && edge.targetNode.id in self.edgeMap[edge.sourceNode.id]) return;
        if (!(edge.sourceNode.id in self.edgeMap)) self.edgeMap[edge.sourceNode.id] = {};
        if (!(edge.targetNode.id in self.edgeMap)) self.edgeMap[edge.targetNode.id] = {};
        self.edgeMap[edge.sourceNode.id][edge.targetNode.id] = true;
        self.edgeMap[edge.targetNode.id][edge.sourceNode.id] = true;
        edge.graph = self;
        self.edges.push(edge);
        self.update();
    }

    self.outgoingEdges = function(node) {
        if (node.id in self.edgeMap) {
            return Object.keys(self.edgeMap[node.id]).length;
        } else {
            return 0;
        }
    }

    self.clear = function() {
        self.nodes = [];
        self.edges = [];
        self.edgeMap = {};
        self.nodeMap = {};
        d3.select('svg').remove();
        self.setup();
    }

}


