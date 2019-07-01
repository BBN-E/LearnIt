/**
 * Created by mshafir on 5/14/14.
 */

function PruningViewModel() {
    var self = this;

    self.patterns = ko.observableArray([]);
    self.patternScores = {};

    self.iteration = 0;
    self.relation = ko.observable('');
    
    $.post("/pruning/get_target_for_data",{},function(target) {
        self.target = target;
    });

    self.loadData = function() {

        $.post("/pruning/get_pattern_scores",{},function(data) {
            console.log(data);

            self.iteration = data.iteration;
            self.relation(self.target.name);
            self.patternScores = loadMap(data.patternScores.data, patternLoader,patternScoreLoader);

            $.each(data.patternScores.data.keyList, function(i,data) {
                var pattern = patternLoader(data);
                if (self.patternScores[pattern].iteration >= 0) {
                    self.patterns.push(new PrunePatternViewModel(pattern,self));
                }
            });

            self.resort(self.confidenceSorter);

            ko.applyBindings(self);

            self.startTimer();
        });
    }

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

    self.doSecondarySort = function(a,b) {
        return self.secondarySort(a,b,true);
    }

    self.confidenceSorter = function(a,b,second){
        if (b.score().confidence == a.score().confidence && !second)
            return self.doSecondarySort(a,b);

        return b.score().confidence - a.score().confidence;
    };

    self.iterationSorter = function(a,b,second) {
        if (b.score().iteration == a.score().iteration && !second)
            return self.doSecondarySort(a,b);

        return a.score().iteration - b.score().iteration;
    }

    self.scoreSorter = function(a,b,second) {
        if (b.score().precision == a.score().precision && !second)
            return self.doSecondarySort(a,b);

        return b.score().precision - a.score().precision;
    }

    self.frequencySorter = function(a,b,second) {
        if (b.score().frequency == a.score().frequency && !second)
            return self.doSecondarySort(a,b);

        return b.score().frequency - a.score().frequency;
    }

    self.secondarySort = self.frequencySorter;

    self.resort = function(sortfunc) {
        self.primarySort = sortfunc
        self.patterns.sort(self.primarySort);
        self.secondarySort = self.primarySort;
    }

    self.getScore = function(pattern) {
        return self.patternScores[pattern];
    }

    self.shutdown = function() {

        if (confirm("Are you sure you want save all changes and shutdown the server?")) {
            $.post('/shutdown',{},function(){});
            alert("You may now close this tab");
            self.stopTimer();
        }

    }

}

function PrunePatternViewModel(pattern,vm) {
    var self = this;

    self.vm = vm;
    self.pattern = pattern;
    self.score = function() {return vm.getScore(self.pattern);}
    self.frozen = function() {return self.score().frozen();}
    self.showMatches = ko.observable(false);
    self.matches = ko.observableArray([]);

    self.manuallyClicked = ko.observable(false);

    self.toggleAccept = function() {
        self.manuallyClicked(true);
        if (self.frozen()) {
            self.setUnfrozen()
        } else {
            self.setFrozen();
        }
    }

    self.setUnfrozen = function() {
        $.post("/pruning/set_unfrozen",{pattern: self.pattern.toIDString()}, function(data) {
            if (data == "success") {
                self.score().frozen(false);
            } else {
                alert("Failed to unset frozen for "+self.pattern.toIDString());
            }
        });
    }

    self.setFrozen = function() {
        $.post("/pruning/set_frozen",{pattern: self.pattern.toIDString()}, function(data) {
            if (data == "success") {
                self.score().frozen(true);
            } else {
                alert("Failed to set frozen for "+self.pattern.toIDString());
            }
        });
    }

    self.acceptUp = function() {
        for (var i=self.vm.patterns().indexOf(self);i>=0;i--) {
            if (!self.vm.patterns()[i].manuallyClicked())
                self.vm.patterns()[i].setFrozen();
        }
    }

    self.rejectDown = function() {
        for (var i=self.vm.patterns().indexOf(self);i<self.vm.patterns().length;i++) {
            if (!self.vm.patterns()[i].manuallyClicked())
                self.vm.patterns()[i].setUnfrozen();
        }
    }

    self.click = function() {
        self.showMatches(!self.showMatches());
        console.log(self.pattern.toIDString());
        if (self.matches().length == 0) {
            $.post("/pruning/get_pattern_matches",{pattern: self.pattern.toIDString()}, function(data) {
                self.matches(data.slice(0,20));
            });

        }
    }

}