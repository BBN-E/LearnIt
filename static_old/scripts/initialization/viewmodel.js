/**
 * Created by mshafir on 4/29/14.
 */

function InitViewModel() {
    var self = this;

    self.binaryEventTargets = ko.observableArray([]);
    self.unaryEventTargets = ko.observableArray([]);
    self.unaryEntityTargets = ko.observableArray([]);
    self.binaryEntityTargets = ko.observableArray([]);

    self.currentTarget = ko.observable();
    self.stagedTarget = ko.observable();
    self.focusedPatternOrSeed = ko.observable();
    self.OTHERTypeName = OTHERTypeName;
    self.addNewUnaryEventTarget = function() {
        var t = new Target("Untitled","",false,true,[],[],true,true,this);
        self.stagedTarget(t);
        self.currentTarget(null);
    }
    self.addNewBinaryEventTarget = function() {
        var t = new Target("Untitled","",false,true,[],[],false,true,this);
        self.stagedTarget(t);
        self.currentTarget(null);
    }
    self.addNewUnaryEntityTarget = function(){
        var t = new Target("Untitled","",false,true,[],[],true,false,this);
        self.stagedTarget(t);
        self.currentTarget(null);
    }
    self.addNewBinaryEntityTarget = function(){
        var t = new Target("Untitled","",false,true,[],[],false,false,this);
        self.stagedTarget(t);
        self.currentTarget(null);
    }

    self.inOtherRelation = ko.computed(function(){
        if(self.currentTarget() && (self.currentTarget().name().toLowerCase() === self.OTHERTypeName.toLowerCase()))return true;
        return false;
    });
//  self.graph = new Graph();

    self.infoHeader = ko.observable();
    self.info = ko.observableArray([]);
    self.infoSentenceId = ko.observableArray([]);
    self.getMoreInstances = function() {};
    self.loadMoreInstances = function() {
        self.getMoreInstances(self.infoHeader(),5);
    }
    self.patternsInDisplay = ko.computed(function() {
        return self.currentTarget() ? self.currentTarget().patternsInDisplay() : [];})
    self.patternScores = ko.computed(function() {
        return self.currentTarget() ? self.currentTarget().patternScores() : {};})
    self.seedsInDisplay = ko.computed(function() {
        return self.currentTarget() ? self.currentTarget().seedsInDisplay() : [];})
    self.seedScores = ko.computed(function() {
        return self.currentTarget() ? self.currentTarget().seedScores() : {};})

    self.triples = ko.computed(function() {
        return self.currentTarget() ? self.currentTarget().triples() : [];})
    
    self.instances = ko.computed(function(){
        return self.currentTarget() ? self.currentTarget().instances():[];});

    self.patternSortableKeys = ko.computed(function(){
        return self.currentTarget()?self.currentTarget().patternSortableKeys():[];
    });

    self.seedSortableKeys = ko.computed(function(){
        return self.currentTarget()?self.currentTarget().seedSortableKeys():[];
    });


    self.patternSortKey = ko.observable();
    self.seedSortKey = ko.observable();

    self.resortSeeds = function(){
        return self.currentTarget() ? self.currentTarget().resortSeedsOrPatterns("seeds"):function(){};
    };
    self.resortPatterns = function(){
        return self.currentTarget() ? self.currentTarget().resortSeedsOrPatterns("patterns"):function(){};
    };
    self.patternCount = ko.computed(function() {
        // Maybe consider filtering?
        if (self.currentTarget()) {
            var accepted = 0;
            $.each(self.currentTarget().patternsInDisplay(), function(i,pat) {if (pat.frozen()) accepted++;});

            return "("+accepted+"/"+self.currentTarget().patternsInDisplay().length+")";
        } else {
            return "";
        }
    })

    self.seedCount = ko.computed(function() {
        // Maybe consider filtering?
       if (self.currentTarget()) {
           var accepted = 0;
           $.each(self.currentTarget().seedsInDisplay(), function(i,seed) {if (seed.frozen()) accepted++;});

           return "("+accepted+"/"+self.currentTarget().seedsInDisplay().length+")";
       } else {
           return "";
       }
    });

    self.timing = ko.observable(0);
    self.isTiming = ko.observable(false);
    self.timeDisplay = ko.computed(function() {
        var mins = Math.floor(self.timing()/60);
        var secs = self.timing() % 60.0;
        secs = (secs < 10) ? "0"+secs : secs;
        return "time elapsed: "+mins+":"+secs;
    });

    self.timer = null;
    self.startTimer = function() {
        if (self.timer) clearInterval(self.timer);
        self.timer = setInterval(function(){self.timing(self.timing()+1);},1000);
        self.isTiming(true);
    }

    self.stopTimer = function() {
        clearInterval(self.timer);
        self.isTiming(false);
    }


/*
    self.drawGraph = function() {

        self.graph.clear();
        var totalSeeds = 100;
        var seedsPerTarget = totalSeeds/self.targets().length;

        $.each(self.targets(), function(i,target) {

            var seedCount = 0;

            $.each(target.seeds(), function(j, seedvm) {

                // NOTE FOR NOW WE FILTER OUT FROZEN BECAUSE
                // WE DON'T DO THINGS WITH NEGATIVE TRAINING
                if (seedvm.frozen() && seedCount < seedsPerTarget) {

                    var seed = seedvm.seed;

                    if (!self.graph.hasNode(seed.slot0))
                        self.graph.addNode(new Node(seed.slot0,seed.slot0));

                    if (!self.graph.hasNode(seed.slot1))
                        self.graph.addNode(new Node(seed.slot1,seed.slot1));

                    var edge = new Edge(target.name()+"-"+seed.toString(),
                        self.graph.getNode(seed.slot0),
                        self.graph.getNode(seed.slot1),
                        target.name(),
                        seedvm.click,
                        target.color,
                        seedvm.score().confidence);

                    self.graph.addEdge(edge);
                    seedCount++;

                }

            });

        });

    }
*/

    self.shutdown = function() {
        if (confirm("Are you sure you want save current changes?")) {
            $.post(baseURL+'/init/save_progress',{},function(resp) {
		alert(resp)
            });
            //alert("You may now close this tab");
            //self.stopTimer();
        }
    }

    self.clearUnknown = function() {
        if (self.currentTarget()) {
            self.currentTarget().clearUnknown();
        }
    }

    self.clearAll = function() {
        if (confirm("Are you sure you want to clear ALL patterns and seeds for this relation?")) {
            if (self.currentTarget()) {
                self.currentTarget().clearAll();
            }
        }
    }

    self.loadingMessage = ko.observable("");
    self.loading = ko.computed(function() { return self.loadingMessage() != ""; });
    self.setLoading = function(message) {
        self.loadingMessage(message);
    }
    self.unsetLoading = function() {
        self.loadingMessage("");
    }

    self.loadingInfo = ko.observable(false);
    self.setLoadingInsts = function() {
        self.loadingInfo(true);
    }
    self.unsetLoadingInsts = function() {
        self.loadingInfo(false);
    }

    self.autoAcceptSeeds = function() {
        if (self.currentTarget()) {
            self.currentTarget().autoAcceptSeeds();
        }
    }

    self.proposePatterns = function() {
        if (self.currentTarget()) {
            self.currentTarget().proposePatterns();
        }
    }

    self.proposeSeeds = function() {
        if (self.currentTarget()) {
            self.currentTarget().proposeSeeds();
        }
    }

    self.rescorePatterns = function() {
        if (self.currentTarget()) {
            self.currentTarget().rescorePatterns();
        }
    }

    self.rescoreSeeds = function() {
        if (self.currentTarget()) {
            self.currentTarget().rescoreSeeds();
        }
    }

    self.refresh = function() {
        if (self.currentTarget()) { self.currentTarget().loadScores(); }
    }

    self.loadAdditionalSeeds = function() {
        if (self.currentTarget()) { self.currentTarget().loadAdditionalSeeds(); }
    }

    self.toggleInfoAccept = function() {
        if (self.infoHeader()) {
            self.infoHeader().toggleAccept();
        }
    }

    self.getSimilarPatterns = function(){
        if(self.currentTarget()){self.currentTarget().getSimilarPatterns();}
    }

    self.getSimilarSeeds = function(){
        if(self.currentTarget()){self.currentTarget().getSimilarSeeds();}
    }
    self.getSimilarTriples = function(){
        if(self.currentTarget()){self.currentTarget().getSimilarTriples();}
    }
    self.initTriples = function(){
        if(self.currentTarget()){self.currentTarget().initTriples();}
    }

    self.patternForm = new PatternFormViewModel(self);
    self.addingPatterns = ko.observable(false);
    self.setAddingPatterns = function() {
        self.addingPatterns(true);
        self.patternForm.clear();
        $('#keywordBox').focus();
    }
    self.unsetAddingPatterns = function() {
        self.addingPatterns(false);
        self.rescoreSeeds();
    }

    self.seedForm = new SeedFormViewModel(self);
    self.addingSeeds = ko.observable(false);
    self.setAddingSeeds = function() {
        self.addingSeeds(true);
        self.seedForm.clear();
        $('#slot0Box').focus();
    }
    self.unsetAddingSeeds = function() {
        self.addingSeeds(false);
        self.rescorePatterns();
    }

    self.tripleForm = new TripleFormViewModel(self);

    self.instanceForm = new InstanceFormViewModel(self);

}



function PatternFormViewModel(vm) {
    var self = this;

    self.vm = vm;
    self.keyword = ko.observable("");
    self.patterns = ko.observableArray([]);

    self.selectedPatterns = ko.computed(function() {
        var result = [];
        $.each(self.patterns(), function(i,pat) {
            if (pat.selectedForAddition()) {
                result.push(pat);
            }
        });
        return result;
    });

    self.submitKeyword = function() {

        vm.setLoading("Searching for "+self.keyword());
        self.patterns(self.selectedPatterns());

        $.post(baseURL + "/init/get_patterns_by_keyword",
            {keyword:self.keyword(), target: vm.currentTarget().name(),amount:MaxNumberOfPatterns}, function(data) {

                $.each(data, function(i,pat) {
                    if (patternLoader(pat) != null) {
                        self.patterns.push(new PatternViewModel(pat,self.vm.currentTarget()));
                    }
                });
                vm.unsetLoading();
        });

    }

    self.clear = function() {
        self.keyword('');
        self.patterns([]);
    }
}

function TripleFormViewModel(vm) {
    var self = this;

    self.vm = vm;
    self.keyword = ko.observable("");
    self.triples = ko.observableArray([]);

    self.selectedTriples = ko.computed(function() {
        var result = [];
        $.each(self.triples(), function(i,pat) {
            if (pat.selectedForAddition()) {
                result.push(pat);
            }
        });
        return result;
    });

    self.clear = function() {
        self.keyword("");
        self.triples([]);
    }
}

function InstanceFormViewModel(vm){
    var self = this;
    self.vm = vm;
    self.instances = ko.observableArray([]);

    self.clear = function(){
        self.instances([]);
    }
}

function SeedFormViewModel(vm) {
    var self = this;

    self.vm = vm;
    self.slot0 = ko.observable("");
    self.slot1 = ko.observable("");
    self.seeds = ko.observableArray([]);

    self.selectedSeeds = ko.computed(function() {
        var result = [];
        $.each(self.seeds(), function(i,seed) {
            if (seed.selectedForAddition()) {
                result.push(seed);
            }
        });
        return result;
    });

    self.submitSlots = function() {

        vm.setLoading("Searching for ("+self.slot0()+","+self.slot1()+")");
        self.seeds(self.selectedSeeds());

        $.post(baseURL + "/init/get_seeds_by_slots",
            {slot0:self.slot0(), slot1:self.slot1(), target: vm.currentTarget().name(),amount:MaxNumberOfSeeds}, function(data) {

                $.each(data, function(i,seed) {
                    self.seeds.push(new SeedViewModel(seed,self.vm.currentTarget()));
                });
                vm.unsetLoading();
            });

    }

    self.clear = function() {
        self.slot0('');
        self.slot1('');
        self.seeds([]);
    }
}

