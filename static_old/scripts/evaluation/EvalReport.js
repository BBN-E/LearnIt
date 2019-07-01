/**
 * Created by mshafir on 4/16/14.
 */

function fullFilter(precision,confidence,iteration,frozen) {
    return function(pat) {
        return pat.precision() >= precision && pat.confidence() >= confidence &&
            pat.iteration <= iteration &&
            (!frozen || pat.frozen());
    };
}

function EvalReport(path) {
    var self = this;

    self.path = path;

    //bindable values
    self.confidenceLimit = ko.observable(0.01);
    self.precisionLimit = ko.observable(0.5);
    self.onlyFrozen = ko.observable(true);



    self.params = ko.observable(null);
    self.paramText = ko.computed(function() {
        var result = [];
        for (var param in self.params()) {
            result.push("<b>"+param+"</b>: "+self.params()[param]);
        }
        result.sort();
        return result.join("<br />");
    });
    self.showParams = ko.observable(false);
    self.toggleShowParams = function() { self.showParams(!self.showParams()); }

    self.patternSet = ko.observable(new PatternSetViewModel([],self));
    self.seedSet = ko.observable(new SeedSetViewModel([],self));
    self.missSet = ko.observable(new AnswerSetViewModel([], self));
    self.missedPatterns = ko.observable(new PatternSetViewModel([],self));


    self.totalCorrect = ko.observable(0);

    self.mappings = [];

    self.analysis = ko.observable(null);
    self.analysisStack = [];

    self.analyzeThis = function(obj) {
        if (self.analysis() != null)
            self.analysisStack.push(self.analysis());

        self.analysis(obj);
        self.analysis.subscribe(function() {obj.score.stop(true);});
    }

    self.analysisGoBack = function() {
        if (self.analysisStack.length > 0)
            self.analysisStack.pop().click();
    }


    self.load = function() {
    
        //$.post("/eval/get_target",{path:self.path},function(target) {
        //  self.relation = extractor.target.name;
        //});
        $('#content').hide();
        $('#loading').show();

        console.log("loading extractor...");
        $('#loadStage').text("Getting Pattern/Seed Scores...");
        $.post('/eval/get_extractor',{path:self.path}, function(extractor) {

            console.log("getting pattern/seed scores...");
            $('#loadStage').text("Reading Pattern/Seed Scores...");
            self.iteration = extractor.iteration;
            self.relation = extractor.targetPath;
            self.params(extractor.params);
            self.patternScores = loadMap(extractor.patternScores.data,patternLoader,patternScoreLoader);
            self.patternObjects = loadKeyObjectMap(extractor.patternScores.data,patternLoader);
            self.seedScores = loadMap(extractor.seedScores.data,seedLoader,seedScoreLoader);
            self.seedObjects = loadKeyObjectMap(extractor.seedScores.data,seedLoader);

            console.log("loading answer mapping...");
            $('#loadStage').text("Loading Answer Mapping...");
            $.post('/eval/get_answer_map',{path:self.path}, function(answerMap) {

                self.answerMap = new AnswerMap(answerMap.data);
                self.totalCorrect(self.answerMap.correct.length);

                console.log("loading pattern mapping...");
                $('#loadStage').text("Loading Pattern Mapping...");
                $.post('/eval/get_pattern_map',{path:self.path}, function(patternMap) {
                    self.patternMap = new PatternMap(patternMap.data);

                    $('#loadStage').text("Getting filelist of mappings...");
                    $.post('/eval/get_mapping_files', {path:self.path}, function(files) {
                        $.each(files, function(i,file) {self.mappings.push(file);});

                        console.log("final binding and setup...");
                        $('#loadStage').text("Final Binding and Setup...");
                        ko.applyBindings(self);

                        $('#content').show();
                        $('#loading').hide();

                        $('#content').layout({
                            closable:					true	// pane can open & close
                            ,	resizable:					true	// when open, pane can be resized
                            ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
                            ,	livePaneResizing:			true
                            ,   spacing_closed:				20
                            ,	spacing_open:				10

                            ,   south__togglerContent_closed:"DETAILS"
                            ,	south__togglerLength_closed:	140
                            ,	south__togglerAlign_closed:	"center"
                            ,   south__size:				270
                            ,   south__initClosed: 			false
                        });

                        self.setup(function() {

                            $('#loading').hide();

                        });

                    });

                });
            });
        });
    }

    self.parallel = 3;
    self.currentList = ko.observable('');

    self.getMappingsMatchesParallel = function(func, obj, score, stack, cb, updateList) {
        if (score.stop()) return;

        if (stack.length != 0) {

            var f = stack.pop();
            if (updateList) {
                self.currentList((self.mappings.length-stack.length)+" of "+(self.mappings.length));
            }

            $.post(func, {path: self.path, file: f, object: obj}, function(data) {

                if (data) {
                    if (cb(data)) {
                        self.getMappingsMatchesParallel(func, obj, score, stack, cb, updateList);
                    }
                }
            });
        }

        if (stack.length == 0) {
            score.stop(true);
        }
    }

    self.getMappingsMatches = function(func, object, score, cb) {
        score.stop(false);

        if (score.mappingStack == null)
            score.mappingStack = self.mappings.slice(0); // make a copy

        for (var i=0;i<self.parallel;i++) {
            self.getMappingsMatchesParallel(func, object, score, score.mappingStack, cb, true);
        }

        self.getMappingsInstances(object,score,function(){});
    }

    self.getMappingsSeedMatches = function(patternId, patternScore, cb) {
        self.getMappingsMatches('/eval/get_seeds_from_mappings',patternId,patternScore,
            function(data) {
                patternScore.loadSources(data);
                cb();
                return true;
            });
    }

    self.getMappingsPatternMatches = function(seed, seedScore, cb) {
        self.getMappingsMatches('/eval/get_patterns_from_mappings',seed,seedScore,
            function(data) {
                seedScore.loadSources(data);
                cb();
                return true;
            });
    }

    self.getMappingsInstances = function(obj, score, cb) {

        var func = (score instanceof PatternScore) ? '/eval/get_pattern_instances_from_mappings' : '/eval/get_seed_instances_from_mappings';

        if (score.instMappingStack == null)
            score.instMappingStack = self.mappings.slice(0); // make a copy

        for (var i=0;i<self.parallel;i++) {
            self.getMappingsMatchesParallel(func, obj, score, score.instMappingStack,
                function(data) {
                    $.each(data, function(i,inst) { score.instances.push(inst); });
                    cb();
                    return true;
                }, false);
        }
    }

    self.getSeedScore = function(seed, score) {
        $.post("/eval/get_seed_score", {path: self.path, slot0: seed.slot0, slot1: seed.slot1}, function(data) {
            score.simScore(data != -1 ? data : "NO_SCORE");
        });
    }

    self.getAnswers = function(insts) {
        var result = {};
        $.each(insts,function(i,inst) {
            var answs = self.answerMap.map.get(inst);
            if (answs.length != 1) {
                console.log("ERROR: INCORRECT NUMBER OF ANSWERS FOR INSTANCE "+ inst.toString());
            }

            $.each(answs, function(i,ans) {
                result[ans] = ans;
            });
        });
        return result;
    }

    self.getInstanceInfo = function(insts, callback) {
        $.each(insts, function(i,inst) {

            $.post('/eval/get_instance_info', {path:self.path, instance:inst.json()}, function(data) {

                callback(matchDisplayLoader(data));

            });
        });
    }

    self.getAnswerInstances = function(answer) {
        return self.answerMap.map.inverseGet(answer);
    }

    self.getPatternInstances = function(pattern) {
        return self.patternMap.map.inverseGet(pattern);
    }

    self.getInstancePatterns = function(instance) {
        return self.patternMap.map.get(instance);
    }

    self.getPatternAnswers = function(pattern) {
        return self.getAnswers(self.getPatternInstances(pattern));
    }

    self.getPatternSetAnswerMap = function(patterns) {
        var totalAnswers = {};
        $.each(patterns, function(i,pat) {
            var answerMap = self.getPatternAnswers(pat);
            for (var answer in answerMap) {
                totalAnswers[answer] = answerMap[answer];
            }
        });
        return totalAnswers;
    }

    self.getPatternSetAnswers = function(patterns) {
        var totalAnswers = self.getPatternSetAnswerMap(patterns);
        var answerList = [];
        for (var ans in totalAnswers) {
            answerList.push(totalAnswers[ans]);
        }
        return answerList;
    }

    self.getPatterns = function(filter) {
        var result = [];
        for (var pat in self.patternScores) {
           if (filter(self.patternScores[pat])) result.push(pat);
        }
        return result;
    }

    self.showAsOracle = function(pattern) {
        var count = 0;
        var total = 0;
        var answers = self.getPatternAnswers(pattern);

        for (var ans in answers) {
            if (answers[ans].correct) count++;
            total++;
        }
        return count > 1 && count/total >= 0.2;
    }
    
    self.getMissedPatterns = function() {
        var result = []
        for (var i in self.patternMap.patterns) {
            var pat = self.patternMap.patterns[i];
            if (!self.patternSet().containsPattern(pat)) {
                if (self.showAsOracle(pat)) {
                    result.push(pat);
                }
            }
        }
        return result;
    }

    self.defaultIterFilter = function(iteration) {
        return fullFilter(self.precisionLimit(),self.confidenceLimit(),iteration,self.onlyFrozen());
    }

    self.getPatternSetMisses = function(patterns) {
        var golds = self.answerMap.correct;
        var misses = [];
        var ansMap = self.getPatternSetAnswerMap(patterns);

        $.each(golds, function(i,gold) {
            if (!ansMap[gold]) misses.push(gold);
        });

        return misses;
    }

    self.scorePatterns = function(patterns) {

        var golds = self.answerMap.correct;
        var systems = self.getPatternSetAnswers(patterns);

        var totalCorrect = 0;
        for (var i=0;i<systems.length;i++) {
            if (systems[i].correct) {
                totalCorrect++;
            }
        }

        var precision = systems.length > 0 ? totalCorrect/systems.length : 0;
        var recall = golds.length > 0 ? totalCorrect/ golds.length : 0;
        var f1 = precision + recall > 0 ? 2*precision*recall/(precision+recall) : 0;

        return {precision: precision, recall: recall, f1: f1};
    }

    // SCORING OBSERVABLES
    self.answers = ko.computed(function() {
        console.log("recalculating total over "+self.patternSet().patterns().length+" patterns");
        return self.getPatternSetAnswers($.map(self.patternSet().patterns(), function(p){return p.pattern}));
    });

    self.correct = ko.computed(function() {
        var result = 0;
        $.each(self.answers(), function(i,ans) {
            if (ans.correct) result++;
        });
        console.log(result + " CORRECT");
        return result;
    });

    self.totalCorrect = ko.observable(0);

    self.totalSystem = ko.computed(function() {
        return self.answers().length;
    })

    self.precision = ko.computed(function() {
        return self.totalSystem() == 0 ? 0.0 : self.correct()/self.totalSystem();});
    self.recall = ko.computed(function() {
        return self.totalCorrect() == 0 ? 0.0 : self.correct()/self.totalCorrect();});
    self.f1 = ko.computed(function() {
        return self.precision()+self.recall() == 0 ? 0.0 :
            (2*self.precision()*self.recall())/(self.precision()+self.recall());});

    self.setup = function(cb) {
        nv.addGraph(function() {
            self.iterChart = nv.models.lineChart();
            self.iterChart .xAxis
                .axisLabel('Iteration')
                .tickFormat(d3.format('d'));

            self.iterChart .forceY([0.0,1.0])
            self.iterChart .yAxis
                .axisLabel('Score')
                .tickFormat(d3.format('.02f'));

            self.reloadData();

            cb();

            return self.iterChart;
        });
    }

    self.getDefaultPatterns = function() {
        return self.getPatterns(self.defaultIterFilter(self.iteration));
    }

    self.getDefaultSeeds = function() {
        var result = [];
        for (var seed in self.seedScores) {
            if (self.seedScores[seed].iteration >= 0) {
                result.push(seed);
            }
        }
        return result;
    }

    self.refreshData = function() {
        var curpats = [];
        $.each(self.patternSet().patterns(), function(i,pat) {
            curpats.push(pat.pattern);
        })
        self.reload(curpats);
    }

    self.reloadData = function() {
        self.reload(false);
    }

    self.reload = function(patSet) {
        if (!patSet) {
            patSet = self.getDefaultPatterns();
            self.patternSet(new PatternSetViewModel(patSet,self));

            var missedPats = new PatternSetViewModel(self.getMissedPatterns(),self);
            missedPats.sortCorrect();
            self.missedPatterns(missedPats);
        }

        d3.select('#iterationChart svg')
            .datum(self.iterChartData(patSet))
            .transition().duration(500)
            .call(self.iterChart);

        self.seedSet(new SeedSetViewModel(self.getDefaultSeeds(),self));
        self.missSet(new AnswerSetViewModel(self.getPatternSetMisses(patSet),self));

    }

    self.filterIter = function(pset, iter) {
        var result = [];
        $.each(pset, function(i,p) {
            if (p in self.patternScores) {
                var patIter = self.onlyFrozen() ? self.patternScores[p].iteration : self.patternScores[p].startIteration;

                if (iter >= 0 && patIter >= 0) {
                    if (patIter <= iter) {
                        result.push(p);
                    }
                }
                if (iter == -1 || iter == -2) {
                    result.push(p);
                }
            } else {
                if (iter == -2) result.push(p);
            }
        });
        return result;
    }

    self.iterChartData = function(pset) {
        var obj = {prec: [], rec: [], f1: []};
        var i = 0;
        for (i=0;i<=self.iteration;i++) {
            var results = self.scorePatterns(self.filterIter(pset, i));
            obj.prec.push({x: i, y: results.precision});
            obj.rec.push({x: i, y: results.recall});
            obj.f1.push({x: i, y: results.f1});
        }

        // add -1 - proposed not accepted
        var results = self.scorePatterns(self.filterIter(pset, -1));
        obj.prec.push({x: i, y: results.precision});
        obj.rec.push({x: i, y: results.recall});
        obj.f1.push({x: i, y: results.f1});
        i+=1;

        results = self.scorePatterns(self.filterIter(pset, -2));
        obj.prec.push({x: i, y: results.precision});
        obj.rec.push({x: i, y: results.recall});
        obj.f1.push({x: i, y: results.f1});

        return [
            {values: obj.prec, key: 'Precision'},
            {values: obj.rec, key: 'Recall'},
            {values: obj.f1, key: 'F-Score'}];
    }


}