/**
 * Created by mshafir on 4/16/14.
 */


function PatternSetViewModel(patternSet, evalReport) {
    var self = this;

    self.evalReport = evalReport;
    self.patterns = ko.observableArray($.map(patternSet,
        function(p) {
            return new PatternEvalViewModel(p,evalReport);
        }
    ));

    self.show = ko.observable(false);
    self.toggleShow = function() {self.show(!self.show());};
    self.count = ko.computed(function() {return self.patterns().length;});

    self.containsPattern = function(pattern) {
        for (var i=0;i<self.patterns().length;i++) {
            var patvm = self.patterns()[i];
            if (patvm.display == pattern.toString())
                return true;
        }
        return false;
    }

    self.sortIter = function() {
        self.patterns.sort(function(a,b) {
            var aIter = a.known ? a.score.iteration : -1;
            var bIter = b.known ? b.score.iteration : -1;
            return aIter - bIter
        });
    }

    self.sortCorrect = function() {
        self.patterns.sort(function(a,b) {
            if (b.correct != a.correct) {
                return b.correct - a.correct;
            } else {
                return b.total - a.total;
            }
        });
    }

    self.sortIncorrect = function() {
        self.patterns.sort(function(a,b) {return b.incorrect - a.incorrect});
    }

    self.sortCount = function() {
        self.patterns.sort(function(a,b) {return b.total - a.total});
    }

    self.showZero = ko.observable(false);

    self.sortIter();
}

function SeedSetViewModel(seedSet, evalReport) {
    var self = this;

    self.seeds = ko.observableArray($.map(seedSet,
        function(s) {
            return new SeedEvalViewModel(s,evalReport);
        }
    ));
    self.count = ko.computed(function() {return self.seeds().length;});

    self.show = ko.observable(false);
    self.toggleShow = function() {self.show(!self.show());};

    self.sortIter = function() {
        self.seeds.sort(function(a,b) { return a.sortIter - b.sortIter});
    }

    self.sortIter();
}

function AnswerSetViewModel(answerSet, evalReport) {
    var self = this;

    self.answers = ko.observableArray($.map(answerSet,
        function(a) {return new AnswerViewModel(a,evalReport,true);}));

    self.count = answerSet.length;

    self.show = ko.observable(false);
    self.toggleShow = function() {self.show(!self.show());};
}

function PatternEvalViewModel(pattern, evalReport, count, skipAnswers) {
    var self = this;

    self.evalReport = evalReport
    self.pattern = pattern.toIDString ? pattern : evalReport.patternObjects[pattern];
    self.display = pattern.toString();
    self.report = evalReport;
    self.template = 'pattern-detail-template';
    self.seeds = ko.observable([]);
    self.stop = ko.observable(true);

    self.estimatedConfidenceNumerator = ko.computed(function() {
        var num = 0.0;
        $.each(self.seeds(), function(i,seed) {
            if (seed.simScore() != 'NO_SCORE') {
                num += seed.count*((seed.known) ? 1.0 : seed.simScore());
            }
        });
        return num;
    })
    self.estimatedConfidenceFromSimilarity = ko.computed(function() {
        var num = 0.0;
        $.each(self.seeds(), function(i,seed) {
            if (seed.simScore() != 'NO_SCORE') {
                if (!seed.known) {
                    num += seed.count*seed.simScore();
                }
            }
        });
        return num;
    })
    self.estimatedConfidenceDenominator = ko.computed(function() {
        var num = 0.0;
        $.each(self.seeds(), function(i,seed) {
            num += seed.count;
        });
        return num;
    })
    self.estimatedConfidence = ko.computed(function() {
        return self.estimatedConfidenceNumerator()/self.estimatedConfidenceDenominator();
    })
    self.estimatedConfidenceDisplay = ko.computed(function() {
        return  self.estimatedConfidence().toFixed(3)+" ("+self.estimatedConfidenceNumerator().toFixed(1)+" / "+
            self.estimatedConfidenceDenominator().toFixed(1)+
            "   w/ "+self.estimatedConfidenceFromSimilarity().toFixed(1)+" from sim)";
    })


    self.counted = ko.computed(function() {
        return $.inArray(self, self.evalReport.patternSet().patterns()) != -1;
    })

    self.toggleCounted = function() {
        if (self.counted()) {
            self.evalReport.patternSet().patterns.splice(
                self.evalReport.patternSet().patterns.indexOf(self),1);
        } else {
            self.evalReport.patternSet().patterns.push(self);
        }
        self.evalReport.refreshData();
        return false;
    }

    self.buttonDisplay = ko.computed(function() {
        return (self.counted()) ? "Discount" : "Add to Scored Patterns" ;
    });

    self.count = count ? count : 1;

    if (skipAnswers) {
        self.answers = [];
    } else {
        self.answers = $.map(evalReport.getPatternSetAnswers([self.pattern]),
            function(ans) {return new AnswerViewModel(ans,evalReport);});
    }

    var correct = 0;
    var total = 0;
    for (var i=0;i<self.answers.length;i++) {
        if (self.answers[i].correct) {
            correct++;
        }
        total++;
    }
    self.precision = (correct/total).toFixed(2);
    self.correct = correct;
    self.total = total;
    self.incorrect = self.total - self.correct;

    self.score = evalReport.patternScores[pattern];
    self.known = self.score ? true : false;
    if (self.known) {
        if (self.score.startIteration == 0) {
            self.iterText = "<b>(KNOWN INITIAL)</b>";
        } else if (self.score.iteration == -1) {
            self.iterText = "<b>(KNOWN proposed at iteration "+self.score.startIteration+")</b>";
        } else {
            self.iterText = "<b>(KNOWN accepted at iteration "+self.score.iteration+")</b>";
        }
    } else {
        self.iterText = "(UNKNOWN)";
    }

    self.loadSeeds = function() {
        if (self.score && self.score.sources) {
            var newSeeds = $.map(self.score.sources,
                function(seed) {
                    return new SeedEvalViewModel(seed, self.evalReport, self.score.sourceCounts[seed])});
            newSeeds.sort(function(a,b) { return b.count - a.count;});
            self.seeds(newSeeds);
            $.each(self.seeds(), function(i,seed) {
                if (seed.simScore() == 0.0) seed.loadScore();
            })
        }
    };

    self.visible = ko.computed(function() {
        return self.report.patternSet().showZero() || self.total > 0;
    });

    self.click = function() {
        if (self.score == null) self.score = makeDefaultPatternScore();

        if (self.known) {
            self.loadSeeds();
        }
        // start pull unknown seeds
        self.toggleShowAnswers();
        
        if (self.showAnswers()) {
			self.evalReport.analyzeThis(self);
		}
    }
    
    self.getMatches = function() {
        self.evalReport.getMappingsSeedMatches(self.pattern.toIDString(), self.score, self.loadSeeds);
    }
    
    self.showAnswers = ko.observable(false);
    self.toggleShowAnswers = function() {self.showAnswers(!self.showAnswers());};

}

function SeedEvalViewModel(seed, evalReport, count) {
    var self = this;

    self.seed = seed.json ? seed : evalReport.seedObjects[seed];
    self.display = seed.toString();
    self.evalReport = evalReport;
    self.score = evalReport.seedScores[seed];
    if (self.score == null) {
        self.score = makeDefaultSeedScore(true);
        evalReport.seedScores[seed] = self.score;
    }
    self.known = self.score.known;

    self.template = 'seed-detail-template';
    self.patterns = ko.observable([]);
    self.stop = ko.observable(true);

    self.simScore = ko.computed(function() {
        if (self.score.simScore() != 'NO_SCORE') {
            return self.score.simScore().toFixed(3);
        } else {
            return 'NO_SCORE';
    }})

    if (self.known) {
        if (self.score.startIteration == 0) {
            self.iterText = "<b>(INITIAL)</b>";
            self.sortIter = 0;
        } else if (self.score.iteration == -1) {
            self.iterText = "<b>(KNOWN proposed at iteration "+self.score.startIteration+")</b>";
            self.sortIter = self.score.startIteration;
        } else {
            self.iterText = "<b>(KNOWN accepted at iteration "+self.score.iteration+")</b>";
            self.sortIter = self.score.iteration;
        }
    } else {
        self.iterText = "(UNKNOWN)";
        self.sortIter = -1;
    }

    self.count = count ? count : 1;

    self.loadPatterns = function() {
        if (self.score && self.score.sources) {
            var newPatterns = $.map(self.score.sources,
                function(p) {
                    return new PatternEvalViewModel(p, self.evalReport, self.score.sourceCounts[p], true)});
            newPatterns.sort(function(a,b) { return b.count - a.count;})
            self.patterns(newPatterns);
        }
    };

    self.loadScore = function() {
        evalReport.getSeedScore(self.seed, self.score);
    }

    self.click = function() {
        self.loadScore();
        if (self.known) {
            self.loadPatterns();
        }
		self.evalReport.analyzeThis(self);
    }
    
    self.getMatches = function() {
        self.evalReport.getMappingsPatternMatches(self.seed.json(), self.score, self.loadPatterns);
    }
}

function AnswerViewModel(answer, evalReport, getPatternData) {
    var self = this;

    self.matchedAnnotations = answer.matchedAnnotations;
    self.display = answer.toDisplay();
    self.correct = answer.correct;

    self.matches = $.map(evalReport.getAnswerInstances(answer),
        function(i) {return new InstanceViewModel(i,evalReport,getPatternData);});

    self.showMatches = ko.observable(false);
    self.toggleShowMatches = function() {
        self.showMatches(!self.showMatches());
        if (self.showMatches()) {
            $.each(self.matches, function(i,m) {m.getPatterns();});
        }
    };
}

function InstanceViewModel(instance, evalReport) {
    var self = this;

    self.instance = instance;
    self.evalReport = evalReport;

    self.html = ko.observable('');
    evalReport.getInstanceInfo([instance], function(match) {
        self.match = match;
        self.html(self.match.html);
    });

    self.matchingPatterns = ko.observable([]);
    self.getPatterns = function() {
        self.matchingPatterns($.map(
            self.evalReport.getInstancePatterns(self.instance),
            function(p) {return new PatternEvalViewModel(p,evalReport)}));
    }
}