
/*
	The $.each() function is not the same as $(selector).each(), which is used to iterate over a jQuery object.
	The $.each() function can be used to iterate over any collection, whether it is an object or an array:
	$.each(array, callback) : callback = Function(Integer indexInArray, Object value)
	$.each(object, callback) : callback = Function(String propertyName, Object valueOfProperty)
*/

function DemoViewModel() {
	var self = this;

	// variables having '*' are initialized in self.load function

	self.multimap = null;						// * MatchInfoDisplay -> (PropPattern || TextBetweenPattern)
	self.relations = []; 						// * array of Target (from the .target of each MatchInfoDisplay)

	self.allEntities = ko.observableArray();	// * array of canonical entity strings, as collected from the canonicalSlot0s[0] and canonicalSlot1s[0] of all MatchInfoDisplay
	self.entityCounts = ko.observable(); 		// * map {canonical entity-string -> num# times it appears as either slot0 or slot1 over all MatchInfoDisplay}
	self.entityMap = {}; 						// * map {canonical entity-string -> EntityViewModel}
	self.entityProfileMap = {}; 				// * map {canonical entity-string -> ProfileViewModel}

	self.corefMap = ko.observable({}); 			// * map {canonical entity-string -> canonical entity-string}  ; this starts off with each string mapping to itself
	self.coref1 = ko.observable();
	self.coref2 = ko.observable();
	self.canLink = ko.computed(function() {
		return !(typeof self.coref1() === "undefined" || typeof self.coref2() === "undefined");
	})

	self.entityMatches = {}; 					// * map {canonical entity-string -> array of MatchInfoDisplay having this entity-string as either canonicalSlot0 or canonicalSlot1}
	self.maxConfs = {};							// * {MatchInfoDisplay -> max confidence over the Patterns that match this MatchInfoDisplay}

	self.activeEntityPair = ko.observable(); 	// header (a string) for displayed instances (relation/entity pair) ; set in function self.getInstances
	self.activeDisplays = ko.observableArray(); // Instances to show when clicked ; set in function self.getInstances

	self.graph = null;
	self.seedFreqs = {}; 						// * map from each seedString s to num# of MatchInfoDisplay having seedString s  ; NOTE: seedString constructed from non-canonical entity strings
	self.allDisplays = ko.observableArray();	// * array of MatchInfoDisplay (== self.multimap.keys)
	self.showInstancesOnGraph = ko.observable(false);

	self.showEntities = ko.observable(true); 	// As opposed to graph
	self.freqSort = ko.observable(true); 		// Whether we're sorting by frequency or A-Z
	self.showAllProfiles = ko.observable(true);

	// array of (ProfileViewModel) Profiles to display in the UI main panel
	self.selectedProfiles = ko.computed(function() {
		var selected = [];

		$.each(self.allEntities(), function(i,ent) {	// for each non-canonical entity-string
			ent = self.corefMap()[ent];					// get the canonical entity-string
			if(ent!="Context") {
				if ($.inArray(self.entityProfileMap[ent], selected) == -1) {
					if (self.showAllProfiles() || self.entityMap[ent].selected())
						selected.push(self.entityProfileMap[ent]);
				}
			}
		});

		return selected;
	});

	//Search bar tracking
	self.searchTerm = ko.observable("");
	self.searchEntry = ko.observable();

	// Entities (EntityViewModel) to list for selection in the left panel
	self.entitiesToList = ko.computed(function() {		// array of EntityViewModel
		var selected = [];

		$.each(self.allEntities(), function(i,ent) {	// for each non-canonical entity-string
			ent = self.corefMap()[ent];					// get the canonical entity-string
			if ($.inArray(self.entityMap[ent], selected) == -1) {

				if ( ent!="Context" && (self.searchTerm().length == 0 || self.entityMap[ent].selected() || self.entityMap[ent].corefSelected()) )
					selected.push(self.entityMap[ent])
				else {
					var searchSize = self.searchTerm().split(" ").length;
					var spans = [];
					var split = ent.split(" ");
					
					// let's find canonical entity-strings which contain the search term as a substring
					for (i = 0; i <= split.length - searchSize; i++)
						spans.push(split.slice(i,i+searchSize).join(" "));
				
					if ($.inArray(self.searchTerm(), spans) > -1)
						selected.push(self.entityMap[ent]);
				}

			}
		});

		return selected;
	});

	/*
		Initial load of the data into the viewmodel
		self.multimap : MatchInfoDisplay -> (PropPattern || TextBetweenPattern)
		self.allDisplays : array of MatchInfoDisplay (== self.multimap.keys)

			scoreTables: Target -> map{Pattern -> number}

		self.relations : array of Target (from the .target of each MatchInfoDisplay)
		self.seedFreqs : map from each seedString s to num# of MatchInfoDisplay having seedString s  ; NOTE: seedString constructed from non-canonical entity strings
		self.maxConfs : {MatchInfoDisplay -> max confidence over the Patterns that match this MatchInfoDisplay}

		self.entityCounts : map {canonical entity-string -> num# times it appears as either slot0 or slot1 over all MatchInfoDisplay}

		self.entityMatches : map {canonical entity-string -> array of MatchInfoDisplay having this entity-string as either canonicalSlot0 or canonicalSlot1}

		self.allEntities : array of canonical entity strings, as collected from the canonicalSlot0s[0] and canonicalSlot1s[0] of all MatchInfoDisplay
		self.entityMap : map {canonical entity-string -> EntityViewModel}
		self.corefMap : map {canonical entity-string -> canonical entity-string}  ; this starts off with each string mapping to itself

		invoke function self.makeProfile to generate self.entityProfileMap : map {canonical entity-string -> ProfileViewModel}
	*/
	self.load = function() {
  		console.log("Loading matchInfoDisplay map...");
    	$.post("/demo/load_multimap", {}, function(data) {						// data: EfficientMultimapDataStore<MatchInfoDisplay, LearnitPattern>
			self.multimap = loadBiMap(data, matchDisplayLoader, patternLoader);	// loadBiMap in utils.js, matchDisplayLoader in profile_utils.js, patternLoader in Pattern.js
			// self.multimap : MatchInfoDisplay -> (PropPattern || TextBetweenPattern)  ; MatchInfoDisplay (in profile_utils.js), Pattern (in Pattern.js)
			
			console.log("Done!");
      		console.log(self.multimap);
			
			self.graph = new Graph();
			self.allDisplays(self.multimap.keys);
			
			console.log("Loading pattern scores...");
			$.post("/demo/load_pattern_scores", {}, function(data) {				// EfficientMapDataStore<Target, EfficientMapDataStore<LearnitPattern,Double>>		
      			var scoreTables = loadMap(data, loadTarget, function(val) {			// loadTarget in Target.js
      				return loadMap(val, patternLoader, function(d) {return d});
      			});
      			console.log("Done!");
      			// scoreTables: Target -> map{Pattern -> number}

      			console.log("Loading entity info...");
      			var entities = [];		// array of canonical entity strings, as collected from the canonicalSlot0s[0] and canonicalSlot1s[0] of all MatchInfoDisplay
      			var entitiesEType = [];	// array of entities EntityType , corresponding to the above entities array
      			var initCounts = {};	// map {canonical entity-string -> num# times it appears as either slot0 or slot1 over all MatchInfoDisplay}
      			$.each(self.multimap.keys, function(i,key) {			// foreach MatchInfoDisplay
					if ($.inArray(key.target, self.relations) == -1)
						self.relations.push(key.target);

					//Get overall seed counts (for graph)
					var seedString = key.slot0String + "_" + key.slot1String;	// an example: seedString = "hollande<br/>奥 朗 德_french<br/>法国"
					
					if (seedString in self.seedFreqs)
						self.seedFreqs[seedString]++;
					else
						self.seedFreqs[seedString] = 1;

					//Get max confidence values for each instance
					var maxConf = 0;
					$.each(self.multimap.get(key), function(i,pattern) {		// for each Pattern associated with this particular MatchInfoDisplay (which is the key)
						var patConf = scoreTables[key.target.name()][pattern];	// .confidence() ; example of key.target.name = per_employee_or_member_of
																				// for this relation type, for this pattern, what is the confidence score
						if (patConf > maxConf) 
							maxConf = patConf;
					});
					self.maxConfs[key] = maxConf;

					// Only care about English entities
					// If I'm not using canonical strings, then I would use: key.slot0s instead of key.canonicalSlot0s
					if (key.canonicalSlot0s.length == 2) {
						var entity = key.canonicalSlot0s[0];

						if ($.inArray(entity, entities) == -1) {
							entities.push(entity);
							entitiesEType.push(key.slot0sEntityType[0]);
						}

						if (!(entity in initCounts))
							initCounts[entity] = 0;
						initCounts[entity] = initCounts[entity] + 1;

						if (!(entity in self.entityMatches))
							self.entityMatches[entity] = [];
						self.entityMatches[entity].push(key);
					}

					if (key.canonicalSlot1s.length == 2) {
						var entity = key.canonicalSlot1s[0];

						if ($.inArray(entity, entities) == -1) {
							entities.push(entity);
							entitiesEType.push(key.slot1sEntityType[0]);
						}

						if (!(entity in initCounts))
							initCounts[entity] = 0;
						initCounts[entity] = initCounts[entity] + 1;

						if (!(entity in self.entityMatches))
							self.entityMatches[entity] = [];
						self.entityMatches[entity].push(key);
					}
				});

				self.entityCounts(initCounts);

				console.log("Done!");

				console.log("Generating entity profiles...");

      			//Entities for profiling
      			var initCoref = {}
      			$.each(entities, function(i,ent) {			// for each canonical entity-string
      				var evm = new EntityViewModel(ent,self);
      				if(entitiesEType[i]=="PER" || entitiesEType[i]=="ORG" || entitiesEType[i]=="GPE") {	
      					self.allEntities.push(ent);
      				}

      				self.entityMap[ent] = evm;
      				initCoref[ent] = ent;
      			});
      			self.corefMap(initCoref);
				
				for (ent in self.entityMatches) {			// for each canonical entity-string								 
					self.makeProfile(ent);										
				}

				self.sortEntitiesByFrequency();
				
				console.log("Done!");

      			ko.applyBindings(self);
      		});	// END $.post("/demo/load_pattern_scores"
    	}); // END $.post("/demo/load_multimap"
	} // END self.load

	// Create a ProfileViewModel for each canonical entity-string and store in self.entityProfileMap
	// Requires: self.entityMatches
	//
	// From self.entityMatches, retrive the array of MatchInfoDisplay associated with 'entity' (canonical entity-string)
	// for each MatchInfoDisplay 'match':
	//   - determine whether other entity is in slot 0 or slot 1. Record this in 'slot', and assign 'otherEnt' to canonical entity-string of other entity
	//   - set 'rel' = slot + relation name 
	//   - increment relKeyCounts[rel][otherEnt] by 1
	//   - push 'match' (MatchInfoDisplay) into matchesByRel[rel][otherEnt]
	// finally, create a ProfileViewModel 'pvm' and assign: self.entityProfileMap[entity] = pvm
	self.makeProfile = function(entity) {
		var allEntMatches = self.entityMatches[entity];		// array of MatchInfoDisplay, declared in profile_utils.js ; 'entity' is canonical entity-string
		var relKeyCounts = {};
		var matchesByRel = {}

		//Get all the pairs involving the target entity
		$.each(allEntMatches, function(i,match) {
			var otherEnt;
			var slot;
			/*
			if (match.slot0s[0] == entity) {
				slot = 1;
				otherEnt = match.slot1s[0];
			}
			else {
				slot = 0;
				otherEnt = match.slot0s[0];
			}
			*/
			if (match.canonicalSlot0s[0] == entity) {
				slot = 1;
				otherEnt = match.canonicalSlot1s[0];
			}
			else {
				slot = 0;
				otherEnt = match.canonicalSlot0s[0];
			}

			if (otherEnt != entity) {
				var rel = slot + ": " + match.target.name();	// this would match relDescs

				if (!(rel in relKeyCounts))
					relKeyCounts[rel] = {};
				if (!(otherEnt in relKeyCounts[rel]))
					relKeyCounts[rel][otherEnt] = 0;
				relKeyCounts[rel][otherEnt] += 1;

				if (!(rel in matchesByRel))
					matchesByRel[rel] = {};
				if (!(otherEnt in matchesByRel[rel]))
					matchesByRel[rel][otherEnt] = [];
				matchesByRel[rel][otherEnt].push(match);
			}
		});

		if (!(typeof relKeyCounts === "undefined")) {
			var pvm = new ProfileViewModel(entity,relKeyCounts, matchesByRel, self);
			self.entityProfileMap[entity] = pvm;
		}
	} // END self.makeProfile

	/*
	*	Functions for manual, in-UI coreference linking
	*/
	self.setCorefItem = function(entity) {
		if (typeof self.coref1() === "undefined") {
			self.coref1(entity);
		}
		else {
			if (!(typeof self.coref2() === "undefined"))
				self.entityMap[self.coref2()].corefSelected(false);
			self.coref2(entity);
		}
	}

	self.unsetCorefItem = function(entity) {
		if (self.coref1() == entity) {
			self.coref1(self.coref2());
		}
		self.coref2 = ko.observable();
	}

	self.linkSelected = function() {
		var canon = self.forceCoref(self.coref1(), self.coref2());
		self.entityMap[self.coref1()].corefSelected(false);
		self.entityMap[self.coref2()].corefSelected(false);
		self.coref2 = ko.observable();

		self.coref1 = ko.observable(canon);
		self.entityMap[canon].corefSelected(true);
	}

	//Manual coref from within the interface
	self.forceCoref = function(e1, e2) {
		var canon = e1;
		var other = e2;
		if (self.entityCounts()[e2] > self.entityCounts()[e1] || 
			 (self.entityCounts()[e2] == self.entityCounts()[e1] && e2.length > e1.length))
		{
			canon = e2;
			other = e1;
		}

		var tempCoref = self.corefMap();
		for (base in tempCoref) {
			if (tempCoref[base] == other)
				tempCoref[base] = canon;
		}
		self.corefMap(tempCoref);
		
		var tempCounts = self.entityCounts();
		tempCounts[canon] += tempCounts[other];
		self.entityCounts(tempCounts);

		self.entityProfileMap[canon].combineWith(self.entityProfileMap[other]);

		return canon;
	}

	self.toggleEntitySort = function() {
		self.freqSort(!self.freqSort());
		self.resort();
	}

	self.resort = function() {
		if (self.freqSort())
			self.sortEntitiesByFrequency();
		else
			self.sortEntitiesAlphabetically();
	}

	self.sortEntitiesByFrequency = function() {
		self.allEntities.sort(function(a,b) {
			if (self.entityMap[a].selected() && !self.entityMap[b].selected()) return -1;
			else if (self.entityMap[b].selected() && !self.entityMap[a].selected()) return 1;
			else if (self.entityMap[a].corefSelected() && !self.entityMap[b].corefSelected()) return -1;
			else if (self.entityMap[b].corefSelected() && !self.entityMap[a].corefSelected()) return 1;
			else if (self.entityCounts()[a] > self.entityCounts()[b]) return -1;
			else if (self.entityCounts()[a] < self.entityCounts()[b]) return 1;
			else return 0;
		});
	}

	self.sortEntitiesAlphabetically = function() {
		self.allEntities.sort(function(a,b) {
			if (self.entityMap[a].selected() && !self.entityMap[b].selected()) return -1;
			else if (self.entityMap[b].selected() && !self.entityMap[a].selected()) return 1;
			else if (self.entityMap[a].corefSelected() && !self.entityMap[b].corefSelected()) return -1;
			else if (self.entityMap[b].corefSelected() && !self.entityMap[a].corefSelected()) return 1;
			else if (a < b) return -1;
			else if (a > b) return 1;
			else return 0;
		});
	}

	self.toggleEntities = function() {
		self.showEntities(!self.showEntities());
		self.activeDisplays = ko.observableArray();
	}

	self.showAll = function() {
		self.showAllProfiles(true);
		self.searchTerm("");
		$.each(self.allEntities(), function(i,ent) {
			self.entityMap[ent].selected(false);
		});
		self.resort();
	}

	self.setSearchTerm = function() {
		self.searchTerm(self.searchEntry());
	}

	self.matchDisplaySortFunc = function(a,b) {
		if (self.maxConfs[a] > self.maxConfs[b]) return -1;
			else if (self.maxConfs[b] > self.maxConfs[a]) return 1;
			else if (a.toString() < b.toString()) return -1;
			else if (a.toString() > b.toString()) return 1;
			else return 0;
	}

	/*
	*	Graph Stuff
	*/

	self.graphFilter = ko.observable();
	self.filteredDisplays = ko.computed(function() {
		if (typeof self.graphFilter() === "undefined")
			return self.allDisplays();

		var filtered = [];
		$.each(self.allDisplays(), function(i,key) {
			if (key.target.name() == self.graphFilter())
				filtered.push(key);
		});
		return filtered;
	});

	self.drawGraph = function() {
		console.log("Drawing graph...");
		self.showEntities(false);
		self.graph.clear();
		
		var maxNodes = 300;
		var totalNodes = 0;
		
		var allMatches = self.filteredDisplays();
		allMatches.sort(self.matchDisplaySortFunc);

		$.each(allMatches, function(i,key) {
		
			var skip = 	self.seedFreqs[key.slot0String + "_" + key.slot1String] <= 1 ||
									self.maxConfs[key] < .5 ||
									key.slot0String.split("<br/>")[0].split(" ").length > 5 ||
									key.slot1String.split("<br/>")[0].split(" ").length > 5 ||
									key.slot0String.split("<br/>")[0] == key.slot1String.split("<br/>")[0] ||
									(key.slot0String+key.slot1String).indexOf("xinhua") > -1;
			
		
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

	self.toggleInstanceDisplay = function() {
		self.showInstancesOnGraph(!self.showInstancesOnGraph());
	}

	self.filterGraph = function(relation) {
		self.graphFilter(relation);
		self.drawGraph();
	}

	self.clearGraphFilter = function() {
		self.graphFilter = ko.observable();
		self.drawGraph();
	}

	self.getInstances = function(slot0,slot1,relation) {
		var toDisplay = [];
		$.each(self.filteredDisplays(), function(i,key) {
			if (key.slot0String == slot0 && key.slot1String == slot1 && key.target.name() == relation)
				toDisplay.push(key);
		});
		toDisplay.sort(self.matchDisplaySortFunc);

		self.activeDisplays(toDisplay);
		seed = "(<span class=\"slot0\">"+slot0.split("<br/>")[0]+
					 "</span>, <span class=\"slot1\">"+slot1.split("<br/>")[0]+"</span>)";
		self.activeEntityPair("<strong>" + relation + "</strong>: " + seed);

		self.showInstancesOnGraph(true);
	}
} // END DemoViewModel

/**
* Component ViewModels
*/

// each entity in the left pane
function EntityViewModel(entity,vm) {
	var self = this;
	self.entity = entity;
	self.vm = vm;

	self.selected = ko.observable(false);
	self.corefSelected = ko.observable(false);

	self.toggle = function() {
    	self.selected(!self.selected());
    	vm.showAllProfiles(false);
    	vm.resort();
  	}

  	self.setLink = function() {
  		self.corefSelected(true);
  		self.vm.setCorefItem(self.entity);
  		vm.resort();
  	}

  	self.unsetLink = function() {
  		self.corefSelected(false);
  		self.vm.unsetCorefItem(self.entity);
  		vm.resort();
  	}
}

// Displays one 'box' in the main panel, which contain array of RelationListViewModel
// entity: string (NOTE: this should be the canonical entity-string)
// relKeyCounts: string (base relation string with slot info) -> {'other' canonical entity-strings -> int}
// matchesByRel: string (vase relation string with slot info) -> {'other' canonical entity-strings -> MatchInfoDisplay}
//
// self.entity : the source canonical entity-string
// self.rels : array of base-relation-string associated with self.entity
// self.relations : map {relation-string -> RelationListViewModel}, e.g. "Contains GPEs:" -> RelationListViewModel
function ProfileViewModel(entity, relKeyCounts, matchesByRel, vm) {
	var self = this;

	self.entity = entity;
	self.vm = vm;

	self.rels = [];		// relation strings, e.g. "0: Part_Whole_Geographical"
	for (var k in relKeyCounts) self.rels.push(k);

	//Present relations alphabetically, for consistency
	self.sortRels = function() {
		self.rels.sort(function(a,b) {
			if (a > b) 
				return 1;
			else if (a < b) 
				return -1;
			else 
				return 0;
		});
	}
	self.sortRels();

	// Set up the relation lists per relation-description
	var initRelations = {};
	$.each(self.rels, function(i,rel) {
		var desc = relDescs[rel];  // relDescs from profile_utils.js, e.g.: relDescs["0: Part_Whole_Geographical"] = "Contains GPEs:";
		var newRel = new RelationListViewModel(entity, rel, relKeyCounts[rel], matchesByRel[rel], vm);
		if (!(desc in initRelations))
			initRelations[desc] = newRel;
		else 
			initRelations[desc].combineWith(newRel);	// this would happen for bi-directional relations, e.g. Per_Social_Family
	});
	self.relations = ko.observable(initRelations);		// e.g. "Contains GPEs:" -> RelationListViewModel

	// returns an array of RelationListViewModel , sorted in order of desc (non-base) relation-string
	// handle reordering for the display (for when we combine and need to worry about that)
	// based on the sorted relation descriptions on self.rels, construct self.displayRelations, which is an array of RelationListViewModel
	self.displayRelations = ko.computed(function() {
		var ordered = [];
		var descs = [];
		$.each(self.rels, function(i,rel) {
			var desc = relDescs[rel];
			if ($.inArray(desc, descs) < 0)
				descs.push(desc);
		});
		
		$.each(descs, function(i,desc) {
			ordered.push(self.relations()[desc]);
		});
		return ordered;
	});
	// NOTE: self.rels does not match up with self.displayRelations
	// self.rels is a sorted array of baseRelation strings (which contain slot0/1 info); displayRelations is an array of RelationListViewModel sorted in terms of self.rels
	// self.displayRelations *can* contain fewer keys than self.rels , since some relations are bi-directional, e.g. Per_Social_Family


	/*self.totalCount = ko.computed(function() {
		var total = 0;
		$.each(self.relations(), function(i,rel) {
			total += rel.totalCount();
		});
		return total;
	});*/
	
	// param other: RelationListViewModel
	// invoked above: initRelations[desc].combineWith(newRel)
	//For coref linking. Update this profile by adding information from the other.
	self.combineWith = function(other) {
		$.each(other.rels, function(i,rel) {	// for each relation string in the object 'other'
			if ($.inArray(rel, self.rels) == -1)
				self.rels.push(rel);
		});
		self.sortRels();

		var newRelations = self.relations();
		$.each(self.rels, function(i,rel) {
			var desc = relDescs[rel];
			if (desc in newRelations && desc in other.relations()) {
				newRelations[desc].combineWith(other.relations()[desc]);
			} else if (desc in other.relations()) {
				newRelations[desc] = other.relations()[desc];
			}
			newRelations[desc].parentEntity = entity;
		});
		self.relations(newRelations);
	}
}  // END ProfileViewModel

// Encapsulates information on: a relation, and its associated entities ; to be displayed in the main panel
// parentEnt : canonical source entity-string
// relation: relation string with slot differentiation, e.g. "0: Part_Whole_Geographical"
// entityCounts: 'other' canonical entity-string -> int
// matches: 'other' canonical entity-string -> array of MatchInfoDisplay
function RelationListViewModel(parentEnt, relation, entityCounts, matches, vm) {
	var self = this;
	self.parentEntity = parentEnt;
	self.bareRelation = relation;			// e.g. "0: Part_Whole_Geographical"
	self.relation = relDescs[relation];		// e.g. "Contains GPEs:"
	self.baseEntityCounts = ko.observable(entityCounts);	// canonical entity-string -> instance count
	self.baseMatches = ko.observable(matches);				// canonical entity-string -> array of MatchInfoDisplay

	// accounting for coref-based updates
	// Basically, collapse from 'self.baseEntityCounts' into 'self.entityCounts', where collapsing is based on canonical entity-strings via 'corefMap'. 
	// self.baseEntityCounts, i.e. incoming param 'entityCounts' do not change, 
	// but because 'corefMap' which holds the current mapping of 'entity-string' -> 'canonical entity-string' might get updated,
	// we will loop through all entity-strings in baseEntityCounts, get its canonical entity-string from corefMap, and thus aggregate instance counts into 'newCounts'
	self.entityCounts = ko.computed(function() {	// canonical entity-string -> instance count
		var newCounts = {};
		for (e in self.baseEntityCounts()) {		// canonical entity-string
			var coref = vm.corefMap()[e];			// get canonical entity-string
			if (!(coref in newCounts))
				newCounts[coref] = 0;
			newCounts[coref] += self.baseEntityCounts()[e];	// aggregate instance counts based on canonical entity-string
		}
		return newCounts;
	});

	// accounting for coref-based updates
	// Basically, collapse from 'self.baseMatches' into 'self.matches', where collapsing is based on canonical entity-strings via 'corefMap'.
	self.matches = ko.computed(function() {			// canonical entity-string -> array of MatchInfoDisplay
		var newMatches = {};
		for (e in self.baseMatches()) {				// canonical entity-string
			var coref = vm.corefMap()[e];			// get canonical entity-string
			if (!(coref in newMatches))
				newMatches[coref] = [];
			$.each(self.baseMatches()[e], function(i,match) {
				newMatches[coref].push(match);
			});
		}
		return newMatches;
	});

	// NOTE: this places constraints on the output. In particular:
	// - if relation is "Geographically part of:", we will only output the top 1 entity
	// - for all other relations, we will only output the top 20 entities
	// Push only top-count entites out to the display
	self.sliceSize = (self.relation == "Geographically part of:") ? 1 : 20;
	self.entities = ko.computed(function() {		// the top 1, or 20, canonical entity-strings with the most instance counts
		var temp = []
		for (var e in self.entityCounts())			// canonical entity-strings
			temp.push(e);

		temp.sort(function(a,b) {
			if (self.entityCounts()[a] > self.entityCounts()[b]) return -1;
			else if (self.entityCounts()[a] < self.entityCounts()[b]) return 1;
			else if (a > b) return 1;
			else if (a < b) return -1;
			else return 0;
		});

		temp = temp.slice(0,self.sliceSize);

		return temp;
	});

	/*self.totalCount = ko.computed(function() {
		var total = 0;
		$.each(self.entities(), function(i,e) {
			total += self.entityCounts()[e];
		});
		return total;
	});*/

	// Fetch MatchInfoDisplay objects for display in the south panel
	// sets 2 things in the 'vm':
	// - vm.activeEntityPair : bareRelationString and seed/slots entity strings, e.g. org_place_of_headquarters: (securities and exchange commission, nigeria) 
	// - vm.activeDisplays : array of MatchInfoDisplay for the canonical entity-string 'entity'
	// NOTE: param 'entity' needs to be a canonical entity-string, and for display purposes, 'parentEntity' should also be a canonical entity-string
	self.getDisplays = function(entity) {		
		var seed;
		if (self.bareRelation.charAt(0) == "0")
			seed = "(<span class=\"slot0\">"+entity+"</span>, <span class=\"slot1\">"+self.parentEntity+"</span>)";
		else
			seed = "(<span class=\"slot0\">"+self.parentEntity+"</span>, <span class=\"slot1\">"+entity+"</span>)";
		vm.activeEntityPair("<strong>" + self.bareRelation.substring(3,relation.length) + "</strong>: " + seed);

		var displays = self.matches()[entity];		// NOTE: 'entity' needs to be a canonical entity-string ; displays = array of MatchInfoDisplay
		displays.sort(vm.matchDisplaySortFunc);
		vm.activeDisplays(displays);
	}

	// Update this relation list by adding the information from the 'other' RelationListViewModel
	// updates self.baseEntityCounts and self.baseMatches
	self.combineWith = function(other) {
		var newCounts = self.baseEntityCounts();
		for (e in other.baseEntityCounts()) {
			if (e in self.baseEntityCounts())
				newCounts[e] += other.baseEntityCounts()[e];
			else
				newCounts[e] = other.baseEntityCounts()[e];
		}
		self.baseEntityCounts(newCounts);

		var newMatches = self.baseMatches();
		for (e in other.baseMatches()) {
			if (e in self.baseMatches()) {
				$.each(other.baseMatches()[e], function(i,match) {
					newMatches[e].push(match);
				});
			}
			else
				newMatches[e] = other.baseMatches()[e];
		}
		self.baseMatches(newMatches);
	}
} // END RelationListViewModel

//See profile_utils.js for other objects

/**
*	Start up the main viewmodel
*/
var demoViewModel = new DemoViewModel();

$(document).ready(function() {

	$('#content').layout({
        closable:					true	// pane can open & close
        ,	resizable:					true	// when open, pane can be resized
        ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,	livePaneResizing:			true
        , spacing_closed:				20
        ,	spacing_open:				10

        ,   west__togglerContent_closed: "E<BR>N<BR>T<BR>I<BR>T<BR>I<BR>E<BR>S"
        ,	west__togglerLength_closed:	200
        ,	west__togglerAlign_closed:	"center"
        ,   west__size:				300
        ,   west__initClosed: 			false
        ,		west__resizable:				false

        , center__childOptions : {
        			south__size: 200
        	,		south__initClosed: false
        }
    });      

  demoViewModel.load();
});
