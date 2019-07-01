/**
 * Created by mshafir on 4/29/14.
 */

function seedLoader(data) {
    return new Seed(data);
}

function Seed(data) {
    var self = this;

    self.language = data.language;
    self.slot0 = data.slot0.text[0];
    self.slot1 = data.slot1.text[0];
    self.toString = function() {
        return "("+self.slot0+","+self.slot1+")";
    }
    self.json = function() {
        return JSON.stringify({language:self.language,
            slot0:{'@class':'.Slot', text: [self.slot0]},
            slot1:{'@class':'.Slot', text: [self.slot1]}});
    }
    self.html = function() {
        return "(<span class=\"slot0\">"+self.slot0+"</span>, <span class=\"slot1\">"+self.slot1+"</span>)";
    }
}

function seedScoreLoader(data) {
    return new SeedScore(data);
}

function makeDefaultSeedScore(justAdded) {
    return new SeedScore({
        score: 0.1,
        confidence: 0,
        frozen: false,
        startIteration: 0,
        iteration: 0,
        frequency: 0,
        knownFrequency: 0,
        uniqueFrequency: 0,
        knownUniqueFrequency:0,
        justAdded: justAdded
    });
}

function SeedScore(data) {
    var self = this;

    if (data.justAdded) {
        self.known = false;
    } else {
        self.known = true;
    }

    self.score = ko.observable(data.score);
    self.simScore = ko.observable(0.0);
    self.scoreDisplay = ko.computed(function() {return self.score().toFixed(3);});

    self.confidence = ko.observable(data.confidence);
    self.confidenceDisplay = ko.computed(function() {return self.confidence().toFixed(3); });

    self.confidenceNumerator = data.confidenceNumerator ? data.confidenceNumerator : 0;
    self.confidenceDenominator = data.confidenceDenominator ? data.confidenceDenominator : 0;

    self.confidenceNumerator = self.confidenceNumerator.toFixed(3);
    self.confidenceDenominator = self.confidenceDenominator.toFixed(3);

    self.confidenceAnalysisDisplay = "("+self.confidenceNumerator+" / "+
        self.confidenceDenominator+")";

    self.precisionOrScore = ko.computed(function() {return self.score();});
    self.precisionOrScoreDisplay = ko.computed(function() {return self.scoreDisplay();});

    self.frozen = ko.observable(data.frozen);
    self.startIteration = data.iteration;
    self.iteration = data.frozenIteration;
    self.frequency = data.frequency;
    self.knownFrequency = data.knownFrequency;

    self.sources = [];
    self.sourceCounts = {};
    self.originalSources = {};

    self.instances = ko.observableArray([]);
    self.scoreForFrontendRanking = data.scoreForFrontendRanking;
    self.stop = ko.observable(true);
    self.setStop = function() {self.stop(true);}

    self.loadSources = function(data) {
        var tmpSources = [];
        $.each(data.keyList, function(i,p) {
            tmpSources.push(patternLoader(p));
        });
        $.each(data.entries, function(i,entry) {
            if (!(tmpSources[entry.key].toString() in self.originalSources)) {
                if (tmpSources[entry.key] in self.sourceCounts) {
                    self.sourceCounts[tmpSources[entry.key]] += entry.count;
                } else {
                    self.sourceCounts[tmpSources[entry.key]] = entry.count;
                    self.sources.push(tmpSources[entry.key]);
                }
            }
        });
    }

    if (data.sources) {
        $.each(data.sources.keyList, function(i,obj) {self.originalSources[obj] = true;});
        self.loadSources(data.sources);
    }
}

function SeedViewModel(data,target) {
    var self = this;

    self.target = target;
    self.seed = seedLoader(data);
    self.score = ko.computed(function() {
        if (self.seed in self.target.seedScores()) {
            return self.target.seedScores()[self.seed];
        } else {
            return makeDefaultSeedScore();
        }
    });

    self.banned = ko.computed(function() {
        return $.inArray(self.seed.json(), self.target.bannedSeeds()) != -1;
    })

    self.toggleBanned = function() {
        if (!self.banned()) {
            self.target.bannedSeeds.push(self.seed.json());
            self.unfreeze();
        } else {
            var idx = self.target.bannedSeeds().indexOf(self.seed.json());
            self.target.bannedSeeds.splice(idx,1);
        }
        self.target.resort();
    }

    self.frozen = ko.computed(function() {return self.score().frozen();});
    self.frozenGood = ko.computed(function() {return self.score().frozen() && self.score().score() >= 0.5;});
    self.frozenBad = ko.computed(function() {return self.score().frozen() && self.score().score() < 0.5;});
    self.focus = ko.computed(function() {return vm.focusedPatternOrSeed() == self;});
    self.click = function() {
        self.target.getSeedInstances(self,MaxNumberOfInstance);
        vm.focusedPatternOrSeed(self);
    }

    self.freeze = function() {
        self.score().confidence(1.0);
        self.score().iteration = self.score().startIteration;
        self.score().score(0.9);
        self.score().frozen(true);
        if (vm.addingSeeds()) {
            self.toggleForAddition();
        }
        self.target.addSeeds([self.seed],"good");
    }

    self.freezeBad = function() {

        self.score().confidence(1.0);
        self.score().iteration = self.score().startIteration;
        self.score().score(0.1);
        self.score().frozen(true);
        if (vm.addingSeeds()) {
            self.toggleForAddition();
        }
        self.target.addSeeds([self.seed],"bad");
    }

    self.unfreeze = function() {
        self.score().frozen(false);
        self.score().confidence(0.01);
        self.score().iteration = -1;
        self.score().score(0.0);
        if (vm.addingSeeds()) {
            self.toggleForAddition();
        }
        self.target.removeSeed(self.seed);
    }

    self.toggleAccept = function() {
        if (!self.frozen()) {
            self.freeze();
        } else {
            self.unfreeze();
        }
    }

    self.selectedForAddition = ko.observable(false);
    self.toggleForAddition = function() {
        self.selectedForAddition(!self.selectedForAddition());
    }

    self.toString = function() {
        return self.seed.toString();
    }
}

function SimpleSeedViewModel(data,vm,human) {
    var self = this;

    self.seed = seedLoader(data);
    self.vm = vm;
    self.human = human;

    self.good = ko.observable(false);
    self.bad = ko.observable(false);
    self.redundant = ko.observable(false);
    self.linked = ko.observable(false);

    self.frozen = ko.computed(function() {return self.good() || self.bad() || self.linked() || self.redundant();});

    self.focused = ko.observable(false);

    self.seedList = function() {
        if (self.human) return self.vm.humanSeedList;
        else return self.vm.systemSeedList;
    }

    self.click = function() {
        self.seedList().setFocus(self);
    }

    self.setGood = function() {
        self.good(true);
        self.seedList().vm.addGood(self.seed);
    }

    self.setBad = function() {
        self.bad(true);
        self.seedList().vm.addBad(self.seed);
    }

    self.setRedundant = function() {
        self.good(false);
        self.bad(false);
        self.redundant(true);
        self.seedList().reSort();
    }

    self.unset = function() {
        self.good(false);
        self.bad(false);
        self.redundant(false);
        self.linked(false);
        self.seedList().vm.unset(self.seed);
    }

    self.link = function() {
        self.seedList().focusedSeed(self);
        if (self.seedList().vm.setCoref()) {
            self.linked(true);
            self.seedList().vm.addGood(self.seed);
        }
    }

    self.getInstances = function() {
        self.seedList().vm.getClickedInstances(self.seed);
    }

    self.toString = function() {
        return self.seed.toString();
    }
}
