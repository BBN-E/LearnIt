/**
 * Created by Haoling Qiu on 3/9/18.
 */


function TripleViewModel(pattern,seed,target) {
    var self = this;

    self.target = target;
    self.pattern = pattern;
    self.seed = seed;

    self.score = function(){return {
        precisionOrScoreDisplay:function(){
            return 0.000;
        },
        confidenceDisplay:function(){
            return 0.950;
        },
        confidenceAnalysisDisplay:"",
        frequency: 0
    }};
    self.frozen = ko.observable(false);
    self.frozenGood = ko.computed(function() {return self.frozen();});
    self.frozenBad = ko.computed(function() {return false;});
    self.focus = ko.computed(function() {return vm.focusedPatternOrSeed() == self;});
    self.click = function() {
        self.target.getTripleInstances(self,3);
        vm.focusedPatternOrSeed(self);
    }

    self.toString = function() {
        return self.pattern.toString() + " " + self.seed.toString();
    }

    self.toggleAccept = function(){
        self.frozen(!self.frozen());
    }
}

