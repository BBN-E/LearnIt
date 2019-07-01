/**
 * Created by mshafir on 4/29/14.
 */

function patternLoaderSmall(data) {
    return new smallPattern(data);
}

function smallPattern(data) {
    //var self = this;
}

function patternLoader(data) {
    if ('args' in data) {
        //console.log("PropPattern");
        return new PropPattern(data);
    } else if ('firstSlot' in data) {
        //console.log("TextBetweenPattern");
        return new TextBetweenPattern(data);
    } else if ('pat1' in data) {
        //console.log("ComboPattern");
        return new ComboPattern(data);
    } else if ('slot' in data && 'eType' in data) {
        //console.log("SlotEntityTypeRestriction");
        return new SlotEntityTypeRestriction(data);
    } else if ('slot' in data && 'word' in data) {
        //console.log("SlotContainsWordRestriction");
        return new SlotContainsWordRestriction(data);
    } else if ('before' in data) {
        //console.log("BeforeAfterSlotsPattern");
        return new BeforeAfterSlotsPattern(data);
    } 
    else if('patternIDString' in data || 'patternID' in data || 'normalizedPattern' in data){
        return new PatternID(data);
    }
    else {
        console.error(data);
    }
}

function PatternID(data){
    var self = this;
    self.text = data.patternIDString || data.normalizedPattern;
    self.toString = function() {
        return self.text;
    }

    self.toIDString = function() {
        return self.text;
    }
}

function PatternMap(data) {
    var self = this;

    self.map = loadBiMap(data,instanceLoader,patternLoader);
    self.patterns = self.map.values;
}

function TextBetweenPattern(data) {
    var self = this;

    self.firstSlot = data.firstSlot;
    self.secondSlot = data.secondSlot;
    self.language = data.language;
    self.content = [];
    $.each(data.content.content, function(i,c) {
        if (c.text) {
            self.content.push(c.text.string);
        } else if (c.etype) {
            self.content.push("["+ c.etype+"]");
        }
    });

    self.toString = function() {
        return "{"+self.firstSlot+"} "+self.content.join(" ")+" {"+self.secondSlot+"}";
    }

    self.toIDString = function() {
        return "{"+self.firstSlot+"} "+self.content.join(" ")+" {"+self.secondSlot+"}";
    }
}

function PropPattern(data) {
    var self = this;

    self.language = data.language;
    self.predicates = [];
    $.each(data.predicates,function(i,pred) {
        self.predicates.push(pred.string);
    });
    self.predicateType = data.predicateType;
    self.args = [];
    $.each(data.args, function(i,arg) {
        self.args.push(new PropArg(arg));
    });

    self.toString = function() {
        return self.predicateType+":"+self.predicates.join()+"["+self.args.join("][")+"]";
    }

    self.toIDString = function() {
        return self.predicateType+":"+self.predicates.join()+"["+self.args.join("][")+"]";
    }
}

function PropArg(data) {
    var self = this;

    self.prop = data.prop ? new PropPattern(data.prop) : null;
    self.role = data.role.string;
    self.slot = data.slot;

    self.toString = function() {
        if (self.slot != null) {
            return self.role+" = "+self.slot;
        } else if(self.prop){
            return self.role+" = "+self.prop;
        }
        else{
            return self.role;
        }
    }
}

function ComboPattern(data) {
    var self = this;
  
    self.pat1 = patternLoader(data.pat1);
    self.pat2 = patternLoader(data.pat2);
  
    self.toString = function() {
        if (self.pat2.isRestriction) {
            return self.pat2.modifyBasePatternString(self.pat1.toString());
        } else {
            return self.pat1.toString()+" +\n"+self.pat2.toString();
        }
    } 
  
    self.toIDString = function() {
        return self.pat1.toIDString()+"&"+self.pat2.toIDString();
    }
  
}

function SlotEntityTypeRestriction(data) {
    var self = this;
    self.isRestriction = true;
    
    self.slot  = data.slot;
    self.eType = data.eType;
    
    self.toString = function() {
        return "[SLOT"+self.slot+"="+self.eType+"]";
    }
    
    self.toIDString = function() {
        return "[slot="+self.slot+", etype="+self.eType+"]";
    }
    
    self.modifyBasePatternString = function(string) {
        return string.replace(self.slot, self.slot+"-"+ self.eType);
    }
}

function SlotContainsWordRestriction(data) {
    var self = this;
    self.isRestriction = true;
    
    self.slot  = data.slot;
    self.word = data.word;
    self.language = data.language;
    
    self.toString = function() {
        return "[SLOT"+self.slot+"<-"+self.word+"]";
    }
    
    self.toIDString = function() {
        return "[slot="+self.slot+", word="+self.word+", lang="+self.language+"]";
    }
    
    self.modifyBasePatternString = function(string) {
        return string.replace(self.slot, self.slot+"<-\""+ self.word + "\"");
    }
}

function BeforeAfterSlotsPattern(data) {
    var self = this;
    self.isRestriction = true;

    self.slot = data.slot;
    self.before = data.before;
    self.language = data.language;
    self.content = [];
    $.each(data.content, function(i,c) {
        if (c.text) {
            self.content.push(c.text.string);
        }
    });

    self.toString = function() {
        if (self.before) {
          return self.content.join(" ")+" {"+self.slot+"}";
        } else {
          return "{"+self.slot+"} "+self.content.join(" ");
        }
    }

    self.toIDString = function() {
        if (self.before) {
          return self.content.join(" ")+" {"+self.slot+"}";
        } else {
          return "{"+self.slot+"} "+self.content.join(" ");
        }
    }
    
    self.modifyBasePatternString = function(string) {
        if (self.before) {
            return self.content.join(" ") + " " + string;
        } else {
            return string + " " + self.content.join(" ");
        }
    }
}

function patternScoreLoader(data) {
    return new PatternScore(data);
}

function makeDefaultPatternScore() {
    return new PatternScore({
        precision: 0.1,
        confidence: 0,
        frozen: false,
        startIteration: 0,
        iteration: 0,
        frequency: 0,
        knownFrequency: 0,
        seedFrequency: 0,
        knownSeedFrequency:0
    });
}

function PatternScore(data) {
    var self = this;

    self.precision = ko.observable(data.precision);
    self.precisionDisplay = ko.computed(function() {return self.precision().toFixed(3);});

    self.confidence = ko.observable(data.confidence);
    self.confidenceDisplay = ko.computed(function() {return self.confidence().toFixed(3); });

    self.precisionOrScore = ko.computed(function() {return self.precision();});
    self.precisionOrScoreDisplay = ko.computed(function() {return self.precisionDisplay();});

    self.confidenceNumerator = data.confidenceNumerator ? data.confidenceNumerator : 0;
    self.confidenceDenominator = data.confidenceDenominator ? data.confidenceDenominator : 0;
    self.confidenceFromSimilarity = (data.confidenceFromSimilarity) ?
        data.confidenceFromSimilarity : 0.0;

    self.confidenceNumerator = self.confidenceNumerator.toFixed(3);
    self.confidenceDenominator = self.confidenceDenominator.toFixed(3);
    self.confidenceFromSimilarity = self.confidenceFromSimilarity.toFixed(3);

    self.confidenceAnalysisDisplay = "("+self.confidenceNumerator+" / "+
        self.confidenceDenominator+"   w/ "+self.confidenceFromSimilarity+" from sim)";

    self.frozen = ko.observable(data.frozen);
    self.startIteration = data.iteration;
    self.iteration = data.frozenIteration;
    self.frequency = data.frequency;
    self.knownFrequency = data.knownFrequency;
    self.seedFrequency = data.seedFrequency;
    self.knownSeedFrequency = data.knownSeedFrequency;
    self.scoreForFrontendRanking = data.scoreForFrontendRanking;
    self.sources = [];
    self.sourceCounts = {};
    self.originalSources = {};

    self.instances = ko.observableArray([]);

    self.stop = ko.observable(true);
    self.setStop = function() {self.stop(true);}

    self.loadSources = function(data) {
        var tmpSources = [];
        $.each(data.keyList, function(i,seed) {
            tmpSources.push(seedLoader(seed));
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

function PatternViewModel(data,target) {
    var self = this;

    self.target = target;
    self.pattern = patternLoader(data);
    self.score = ko.computed(function() {
        if (self.pattern in self.target.patternScores()) {
            return self.target.patternScores()[self.pattern];
        } else {
            return makeDefaultPatternScore();
        }
    });

    self.banned = ko.observable(false);

    self.frozen = ko.computed(function() {return self.score().frozen();});
    self.frozenGood = ko.computed(function() {return self.frozen() && self.score().precision() >= 0.5;});
    self.frozenBad = ko.computed(function() {return self.frozen() && self.score().precision() < 0.5;});
    self.focus = ko.computed(function() {return vm.focusedPatternOrSeed() == self;});
    self.click = function() {
        self.target.getPatternInstances(self,MaxNumberOfInstance);
        vm.focusedPatternOrSeed(self);
    }

    self.freeze = function() {
        self.score().confidence(1.0);
        self.score().iteration = self.score().startIteration;
        self.score().precision(0.95);
        self.score().frozen(true);
        if (vm.addingPatterns()) {
            self.toggleForAddition();
        }
        self.target.addPattern(self.pattern,"good");
    }

    self.freezeBad = function() {
        self.score().confidence(1.0);
        self.score().iteration = self.score().startIteration;
        self.score().precision(0.05);
        self.score().frozen(true);
        if (vm.addingPatterns()) {
            self.toggleForAddition();
        }
        self.target.addPattern(self.pattern,"bad");
    }

    self.unfreeze = function() {
        self.score().confidence(0.01);
        self.score().iteration = self.score().startIteration;
        self.score().precision(0.0);
        self.score().frozen(false);
        if (vm.addingPatterns()) {
            self.toggleForAddition();
        }
        self.target.removePattern(self.pattern);
    }

    self.selectedForAddition = ko.observable(false);
    self.toggleForAddition = function() {
        self.selectedForAddition(!self.selectedForAddition());
    }

    self.toString = function() {
        return self.pattern.toString();
    }
}

