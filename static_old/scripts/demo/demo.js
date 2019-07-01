function DemoViewModel() {
  var self = this;
  
  self.multimap = null;
	self.relations = [];
	self.graph = {};
	self.showGraph = ko.observable(false);
	self.showEntities = ko.observable(false);
	self.showInstancesOnGraph = ko.observable(false);
	self.instancesToDisplay = ko.observableArray();
	self.searchTerm = ko.observable("");
  self.allKeys = ko.observableArray();
	self.seedFreqs = {};
	self.scoreTables = {};
	self.maxConfs = {};
	self.allEntities = ko.observableArray();
	self.entityCounts = {};
	self.entityKeys = {};
	self.entityProfileStrings = {};
	
	self.withoutXinhua = function(keys) {
		var result = [];
		$.each(keys, function(i,key) {
			if (key.slot0String.indexOf("xinhua") == -1 && key.slot1String.indexOf("xinhua") == -1) {
				result.push(key);
			}
		});
		return result;
	}

	//Items filtered by a search term
	self.filteredKeys = ko.computed(function() {
		var result = [];
		if (self.searchTerm() == "")
			result = self.allKeys();
		else if (self.searchTerm().indexOf("RELATION||") == 0) {
			$.each(self.allKeys(), function(i,key) {
				if(key.target.name() == self.searchTerm().substring("RELATION||".length))
					result.push(key);
			});
		}
		else {
			$.each(self.allKeys(), function(i,key) {
				
				var searchSize = self.searchTerm().split(" ").length;
				var slot0Spans = [];
				var slot0Split = key.slot0String.split("<br/>").join(" ").split(" ");
				var slot1Spans = [];
				var slot1Split = key.slot1String.split("<br/>").join(" ").split(" ");
				
				for (i = 0; i <= slot0Split.length - searchSize; i++)
					slot0Spans.push(slot0Split.slice(i,i+searchSize).join(" "));
				for (i = 0; i <= slot1Split.length - searchSize; i++)
					slot1Spans.push(slot1Split.slice(i,i+searchSize).join(" "));
			
				if ($.inArray(self.searchTerm(), slot0Spans) > -1 || $.inArray(self.searchTerm(), slot1Spans) > -1)
					result.push(key);
			});
		}
		return result;
	});
	
	//Items on the current pages
  self.keysToDisplay = ko.computed(function() {
		var result = [];
		$.each(self.filteredKeys(), function(i,key) {
			if (i >= self.pageOffset()*self.PAGE_SIZE && i < (self.pageOffset()+1)*self.PAGE_SIZE) {
				result.push(key);
			}
		});
		return result;
  });
  
  self.load = function() {
  	console.log("Loading matchInfoDisplay map...");
    $.post("/demo/load_multimap",{},function(data) {
			self.multimap = loadBiMap(data, matchDisplayLoader, patternLoader);
			console.log("Done!");
      console.log(self.multimap);
      
      self.allKeys(self.withoutXinhua(self.multimap.keys));
			
			self.graph = new Graph();
			
			console.log("Loading pattern scores...");
			$.post("/demo/load_pattern_scores",{},function(data) {
      	self.scoreTables = loadMap(data, loadTarget, function(val) {
      		return loadMap(val, patternLoader, patternScoreLoader);
      	});
      	console.log("Done!");

      	console.log("Loading entity info...");
      	var entities = [];
      	$.each(self.allKeys(), function(i,key) {
					if ($.inArray(key.target, self.relations) == -1)
						self.relations.push(key.target);

					var seedString = key.slot0String + "_" + key.slot1String;
					if (seedString in self.seedFreqs)
						self.seedFreqs[seedString]++;
					else
						self.seedFreqs[seedString] = 1;

					var maxConf = 0;
					$.each(self.multimap.get(key), function(i,pattern) {
						var patConf = self.scoreTables[key.target.name()][pattern].confidence();
						if (patConf > maxConf) maxConf = patConf;
					});
					self.maxConfs[key] = maxConf;

					//only care about English entity
					if (key.slot0s.length == 2) {
						var entity = key.slot0s[0];

						if ($.inArray(entity, entities) == -1)
							entities.push(entity);

						if (!(entity in self.entityCounts))
							self.entityCounts[entity] = 0;
						self.entityCounts[entity] = self.entityCounts[entity] + 1;

						if (!(entity in self.entityKeys))
							self.entityKeys[entity] = [];
						self.entityKeys[entity].push(key);
					}

					if (key.slot1s.length == 2) {
						var entity = key.slot1s[0];

						if ($.inArray(entity, entities) == -1)
							entities.push(entity);

						if (!(entity in self.entityCounts))
							self.entityCounts[entity] = 0;
						self.entityCounts[entity] = self.entityCounts[entity] + 1;

						if (!(entity in self.entityKeys))
							self.entityKeys[entity] = [];
						self.entityKeys[entity].push(key);
					}

				});

				console.log("Done!");

				console.log("Generating entity profiles...");
      	//Entities for profiling
				self.allEntities(entities);
				self.allEntities.sort(function(a,b) {
					if (self.entityCounts[a] > self.entityCounts[b]) return -1;
					else if (self.entityCounts[a] < self.entityCounts[b]) return 1;
					else return 0;
				});

				for (ent in self.entityKeys) {
					self.entityProfileStrings[ent] = self.getProfileElements(ent);
				}
				console.log("Done!");

				self.sortByConfidence();

      	ko.applyBindings(self);
      });

    });
  }
	
	/*
	*	Page Navigation
	*/
	self.pageOffset = ko.observable(0);
  self.PAGE_SIZE = 50;
	self.maxPage = ko.computed(function() {
		return Math.floor(self.filteredKeys().length / self.PAGE_SIZE);
	});
	
	self.firstPage = function() {
		self.pageOffset(0);
		window.scrollTo(0,0);
	}
	self.prevPage  = function() {
		if (self.pageOffset() > 0) {
			self.pageOffset(self.pageOffset()-1);
			window.scrollTo(0,0);
		}
	}
	self.nextPage  = function() {
		if (self.pageOffset() < self.maxPage()){
			self.pageOffset(self.pageOffset()+1);
			window.scrollTo(0,0);
		}
	}
	self.lastPage  = function() {
		self.pageOffset(self.maxPage());
		window.scrollTo(0,0);
	}
  
	/*
	*	Sorting
	*/
	self.docIdSortOrder		= ko.observable(0);
  self.targetSortOrder	= ko.observable(0);
  self.slot0SortOrder		= ko.observable(0);
  self.slot1SortOrder		= ko.observable(0);
  self.freqSortOrder		= ko.observable(0);
  self.confSortOrder		= ko.observable(0);
  
  self.resetAllSortsExcept = function(except) {
		if (except != 'docId')	self.docIdSortOrder(0);
		if (except != 'target')	self.targetSortOrder(0);
		if (except != 'slot0') 	self.slot0SortOrder(0);
		if (except != 'slot1') 	self.slot1SortOrder(0);
		if (except != 'freq')		self.freqSortOrder(0);
		if (except != 'conf')		self.confSortOrder(0);
  }
  
  self.sortByDocId = function() {
		if (self.docIdSortOrder() == 1) self.docIdSortOrder(-1);
		else self.docIdSortOrder(1);
		self.resetAllSortsExcept('docId');
		self.allKeys.sort(function(a,b) {
			if (a.docId > b.docId) return self.docIdSortOrder();
			else if (a.docId < b.docId) return -self.docIdSortOrder();
			else return 0;
		});
  }
  
  self.sortByTarget = function() {
		if (self.targetSortOrder() == 1) self.targetSortOrder(-1);
		else self.targetSortOrder(1);
		self.resetAllSortsExcept('target');
		self.allKeys.sort(function(a,b) {
			if (a.target.name() > b.target.name()) return self.targetSortOrder();
			else if (a.target.name() < b.target.name()) return -self.targetSortOrder();
			else return 0;
		});
  }
  
  self.sortBySlot0 = function() {
		if (self.slot0SortOrder() == 1) self.slot0SortOrder(-1);
		else self.slot0SortOrder(1);
		self.resetAllSortsExcept('slot0');
		self.allKeys.sort(function(a,b) {
			if (a.slot0String > b.slot0String) return self.slot0SortOrder();
			else if (a.slot0String < b.slot0String) return -self.slot0SortOrder();
			else return 0;
		});
  }
  
  self.sortBySlot1 = function() {
		if (self.slot1SortOrder() == 1) self.slot1SortOrder(-1);
		else self.slot1SortOrder(1);
		self.resetAllSortsExcept('slot1');
		self.allKeys.sort(function(a,b) {
			if (a.slot1String > b.slot1String) return self.slot1SortOrder();
			else if (a.slot1String < b.slot1String) return -self.slot1SortOrder();
			else return 0;
		});
  }
	
	self.sortByFrequency = function() {
		if (self.freqSortOrder() == 1) self.freqSortOrder(-1);
		else self.freqSortOrder(1);
		self.resetAllSortsExcept('freq');
		self.allKeys.sort(function(a,b) {
			if (self.seedFreqs[a.slot0String + "_" + a.slot1String] < self.seedFreqs[b.slot0String + "_" + b.slot1String])
				return self.freqSortOrder();
			else if (self.seedFreqs[a.slot0String + "_" + a.slot1String] > self.seedFreqs[b.slot0String + "_" + b.slot1String])
				return -self.freqSortOrder();
			else return 0;
		});
		self.pageOffset(0);
	}

	self.sortByConfidence = function() {
		if (self.confSortOrder() == 1) self.confSortOrder(-1);
		else self.confSortOrder(1);
		self.resetAllSortsExcept('conf');
		var maxConfs = {}
		self.allKeys.sort(function(a,b) {
			if (self.maxConfs[a] < self.maxConfs[b])
				return self.confSortOrder();
			else if (self.maxConfs[a] > self.maxConfs[b])
				return -self.confSortOrder();
			else return 0;
		});
		self.pageOffset(0);
	}

	//Visualization graph
	self.toggleGraph = function() {
		self.showGraph(!self.showGraph());
	}
	
	self.toggleInstanceDisplay = function() {
		self.showInstancesOnGraph(!self.showInstancesOnGraph());
	}
	
	self.getInstances = function(slot0,slot1,relation) {
		var htmls = [];
		$.each(self.filteredKeys(), function(i,key) {
			if (key.slot0String == slot0 && key.slot1String == slot1 && key.target.name() == relation)
				htmls.push(key.docId+"<br/>"+"Confidence: "+self.maxConfs[key]+"<br/>"+key.getNoLinkHtml());
		});
		htmls.sort(function(a,b) {
			if (a > b) return 1;
			else if (a < b) return -1;
			else return 0;
		});
		self.instancesToDisplay(htmls);
		self.showInstancesOnGraph(true);
	}
	
	self.drawGraph = function() {
		console.log("Drawing graph...");
		self.showGraph(true);
		self.graph.clear();
		
		var maxNodes = 300;
		var totalNodes = 0;
		
		self.sortByConfidence();

		$.each(self.filteredKeys(), function(i,key) {
		
			var skip = 	self.seedFreqs[key.slot0String + "_" + key.slot1String] <= 1 ||
									self.maxConfs[key] < .54 ||
									key.slot0String.split("<br/>")[0].split(" ").length > 5 ||
									key.slot1String.split("<br/>")[0].split(" ").length > 5 ||
									key.slot0String.split("<br/>")[0] == key.slot1String.split("<br/>")[0];
			
		
			if (!skip && totalNodes <= maxNodes) {
				var newNode = false;
			
				var slot0 = key.slot0String.split("<br/>").join();
				if (!self.graph.hasNode(slot0)) {
					self.graph.addNode(new Node(slot0,slot0));
					newNode = true;
				}
				
				var slot1 = key.slot1String.split("<br/>").join();
				if (!self.graph.hasNode(slot1)) {
					self.graph.addNode(new Node(slot1,slot1));
					newNode = true;
				}

				var edge = new Edge(key.target.name()+"_"+slot0+"_"+slot1,
					self.graph.getNode(slot0),
					self.graph.getNode(slot1),
					key.target.name(),
					function(){self.getInstances(key.slot0String, key.slot1String, key.target.name())},
					key.target.color,
					self.maxConfs[key]);

				self.graph.addEdge(edge);
				if (newNode) totalNodes++;
			}
		});
		console.log("Done.");
	}
	
	/*
	*	Searching
	*/
	self.search = function(term,type) {
		if (type=='relation')
			self.searchTerm("RELATION||" + term);
		else
			self.searchTerm(term.toLowerCase());
		self.pageOffset(0);
		
		if (self.showGraph()) self.drawGraph();
	}
	
	self.searchFromForm = function(form) {
		self.search(form[0].value,'slot');
	}
	
	self.clearFilter = function() {
		self.searchTerm("");
		self.pageOffset(0);
		if (self.showGraph()) self.drawGraph();
	}

	/*
	* Entity View
	*/
	self.toggleEntities = function() {
		self.showEntities(!self.showEntities());
	}

	self.getProfileElements = function(entity) {
		var relevantKeys = self.entityKeys[entity];
		var keyRelCounts = {};

		//Get the counts of entity -> relation
		$.each(relevantKeys, function(i,key) {
			var otherEnt;
			if (key.slot0s[0] == entity)
				otherEnt = key.slot1s[0];
			else
				otherEnt = key.slot0s[0];

			if (otherEnt != entity) {
				var rel = key.target.name();
				if (!(rel in keyRelCounts))
					keyRelCounts[rel] = {};
				if (!(otherEnt in keyRelCounts[rel]))
					keyRelCounts[rel][otherEnt] = 0;
				keyRelCounts[rel][otherEnt] += 1;
			}
		});

		//Put this is a handy html string
		var relStrings = [];
		for (rel in keyRelCounts) {
			var relString = "<strong>" + rel + "</strong><br/>";
			var ents = [];

			for (ent in keyRelCounts[rel]) {
				ents.push(ent);
			}

			ents.sort(function(a,b) {
				if (keyRelCounts[rel][a] < keyRelCounts[rel][b]) return 1;
				else if (keyRelCounts[rel][a] > keyRelCounts[rel][b]) return -1;
				else return 0;
			});

			var sliceSize = rel == "Part_Whole_Geographical" ? 1 : 20;

			$.each(ents.slice(0,sliceSize), function(i,ent) {
				relString = relString.concat("&nbsp;&nbsp;&nbsp;&nbsp;" + ent + ": " + keyRelCounts[rel][ent] + "<br/>");
			});
			relStrings.push(relString);
		}

		//Sort relations alphabetically for consistency
		relStrings.sort(function(a,b) {
			if (a > b) return 1;
			else if (a < b) return -1;
			else return 0;
		});

		return relStrings.join("<br/>");
	}
	
} //End of ViewModel

var demoViewModel = new DemoViewModel();

$(document).ready(function() {        

  demoViewModel.load();
});