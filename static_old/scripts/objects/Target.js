/**
 * Created by mshafir on 4/29/14.
 */
 
var entityColorScale = d3.scale.ordinal().range(colorbrewer.Set1[8]);

//Rearranging colorbrewer Set2 to space the confusable colors out a bit more
var set2_8 = colorbrewer.Set2[8]
var relationColors = [set2_8[4],set2_8[2],set2_8[1],set2_8[3],set2_8[5],set2_8[0],set2_8[6],set2_8[7]]
var relationColorScale = d3.scale.ordinal().range(relationColors);

var numTargets = 0;
var allTargets = {};


function loadTarget(data,vm) {
	if (data.name in allTargets)
		return allTargets[data.name];

    data.slot0Types = [];
    data.slot1Types = [];
    $.each(data.constraints, function(i,constraint) {
        if (constraint.entityTypes) {
            if (constraint.slot == 0) {
                data.slot0Types = constraint.entityTypes;
            } else {
                data.slot1Types = constraint.entityTypes;
            }
        }
    });

    var result = new Target(data.name, data.description, data.symmetric, data.useBestNames, data.slot0Types, data.slot1Types,
        data.unaryTarget,data.eventTarget,vm);
    allTargets[data.name] = result;
    return result;
}


const nameTypes = ["PER","ORG","GPE","LOC","WEA","VEH","FAC"];
// @hqiu: Please see InstanceIdentifier.SpanningType from Java side.
const SpanningTypes = ["EventMention","Mention","ValueMention","Empty","Unknown"];
function Target(name,desc,symmetric,useBestNames,slot0EntityTypes,slot1EntityTypes,isUnaryTarget,isEventTarget,vm) {
    var self = this;
    self.name = ko.observable(name);
    self.description = ko.observable(desc);
    self.symmetric = ko.observable(symmetric);
    self.useBestNames = ko.observable(useBestNames);
    self.inOtherRelation = (name.toLowerCase() === OTHERTypeName.toLowerCase());
    self.color = relationColorScale(numTargets);
    self.vm = vm;
    self.targetsArray = null;


    // @hqiu: Way too much ugly. Should redesign backend Target and TargetSlot object when possible.
    if(isUnaryTarget && isEventTarget){
        self.targetsArray = vm.unaryEventTargets;
    }
    else if(!isUnaryTarget && isEventTarget){
        self.targetsArray = vm.binaryEventTargets;
    }
    else if(isUnaryTarget && !isEventTarget){
        self.targetsArray = vm.unaryEntityTargets;
    }
    else if(!isUnaryTarget && !isEventTarget){
        self.targetsArray = vm.binaryEntityTargets;
    }
    else{
        console.log("Not supported");
    }

    

    self.isUnaryTarget = isUnaryTarget;
    self.isEventTarget = isEventTarget;
    numTargets++;

    self.patternSortableKeys = ko.observableArray([]);
    self.seedSortableKeys = ko.observableArray([]);

    self.patternsPool = ko.observableArray([]);
    self.seedsPool = ko.observableArray([]);


    self.resortSeedsOrPatterns = function(SeedsOrPatterns){
        SeedsOrPatterns = SeedsOrPatterns.toLowerCase();
        const SortingHelper = function(arr,method){
            const pool = arr;
            const good = pool.filter(pattern=>{
                return pattern.frozenGood();
            }).sort((a,b)=>{
                if(typeof method === 'undefined'){
                    return a.score().precisionOrScore() - b.score().precisionOrScore();
                }
                else{
                    return (a.score().scoreForFrontendRanking[method] || 0) - (b.score().scoreForFrontendRanking[method] || 0);
                }
            });
            const netural = pool.filter(pattern=>{
                return !pattern.frozen();
            }).sort((a,b)=>{
                if(typeof method === 'undefined'){
                    return a.score().precisionOrScore() - b.score().precisionOrScore();
                }
                else{
                    return (a.score().scoreForFrontendRanking[method] || 0) - (b.score().scoreForFrontendRanking[method] || 0);
                }
            });
            const bad = pool.filter(pattern=>{
                return pattern.frozenBad();
            }).sort((a,b)=>{
                if(typeof method === 'undefined'){
                    return a.score().precisionOrScore() - b.score().precisionOrScore();
                }
                else{
                    return (a.score().scoreForFrontendRanking[method] || 0) - (b.score().scoreForFrontendRanking[method] || 0);
                }
            });
            return [].concat(good.slice(0,MaxNumberOfSeeds),netural.slice(0,MaxNumberOfSeeds),bad.slice(0,MaxNumberOfSeeds));
        }
        switch(SeedsOrPatterns){
            case "seeds":
                self.seedsInDisplay(SortingHelper(self.seedsPool(),vm.seedSortKey()));
            break;
            case "patterns":
                self.patternsInDisplay(SortingHelper(self.patternsPool(),vm.patternSortKey()));
            break;
        }
    };

    self.patternsInDisplay = ko.observableArray([]);
    self.patternScores = ko.observable({});

    self.seedsInDisplay = ko.observableArray([]);
    self.bannedSeeds = ko.observableArray([]);
    self.seedScores = ko.observable({});

    self.triples = ko.observableArray([]);

    self.instances = ko.observableArray([]);

    self.slot0EntityTypes = [];
    self.slot1EntityTypes = [];
    $.each(nameTypes, function(i,type) {

        self.slot0EntityTypes.push({
            name:type,
            value:ko.observable($.inArray(slot0EntityTypes, type) == 1)});

        self.slot1EntityTypes.push({
            name:type,
            value:ko.observable($.inArray(slot1EntityTypes, type) == 1)});
    });

    self.loaded = ko.observable(false);
    self.load = function() {
        self.loadScores();
        self.loaded(true);
    }


    self.selectTarget = function() {

        self.loadScores(function() {
            vm.currentTarget(self);});
    }

    self.targetsArray.push(self);

    self.availTargetNamesInGroup = ko.computed(function() { 
        return self.targetsArray().map(i=>i.name); 
    });

    self.toString = function() {
        return self.name();
    }

    self.getSlotTypeString = function(lst) {
        var result = [];
        $.each(lst, function(i,type) {
            if (type.value()) {
                result.push(type.name)
            }
        });
        return result.join(',');
    }

    self.save = function() {
        if(self.name().toLowerCase() === OTHERTypeName.toLowerCase()){
            console.log("WARNING: "+ OTHERTypeName.toLowerCase() + " is reserve for system usage.");
            alert("WARNING: "+ OTHERTypeName.toLowerCase() + " is reserve for system usage.");
            self.targetsArray.remove(self);
            return;
        }

        vm.setLoading("Setting up new target...");

        $.post(baseURL + '/init/add_target',{
            name: self.name,
            description: self.description,
            symmetric: self.symmetric,
            slot0EntityTypes: self.getSlotTypeString(self.slot0EntityTypes),
            slot1EntityTypes: self.getSlotTypeString(self.slot1EntityTypes),
            isUnaryTarget:self.isUnaryTarget,
            isEventTarget:self.isEventTarget
        }, function(result) {
                self.selectTarget();
                vm.unsetLoading();
        });
    }

    self.cancel = function() {
        self.targetsArray.remove(self);
        return true;
    }

    // Bonan: sorting patterns or seeds
    // self.resort = function() {
    //     var sortfunc = function(a,b){
    //         if (a.banned() && b.banned()) {
    //             return (a.toString() > b.toString()) ? 1 : -1;
    //         }
    //         if (a.banned()) return 1;
    //         if (b.banned()) return -1;

    //         if (a.score().precisionOrScore() < 0.5 && b.score().precisionOrScore() < 0.5) {
    //             return (a.toString() > b.toString()) ? 1 : -1;
    //         }
    //         if (a.score().precisionOrScore() < 0.5) return 1;
    //         if (b.score().precisionOrScore() < 0.5) return -1;

    //         if (b.score().confidence() == a.score().confidence()) {
    //             if (b.score().precisionOrScore() == a.score().precisionOrScore()) {
    //                 return (a.toString() > b.toString()) ? 1 : -1;
    //             } else {
    //                 return b.score().precisionOrScore() - a.score().precisionOrScore();
    //             }
    //         } else {
    //             return b.score().confidence() - a.score().confidence();
    //         }};

    //     self.patterns.sort(sortfunc);
    //     self.seeds.sort(sortfunc);
    // }

    self.resort = function(){
        console.log("It should be called.");
    }

    self.loadScores = function(callback) {
        vm.setLoading("Getting scores...");

        $.post(baseURL+"/init/get_extractor",{relation: self.name()}, function(data) {
            if (data) {
                self.patternScores(loadMap(data.patternScores.data, patternLoader, patternScoreLoader));
                self.seedScores(loadMap(data.seedScores.data, seedLoader, seedScoreLoader));

            
                self.patternSortableKeys(data.patternScores.FrontendSortableKeys);
                self.seedSortableKeys(data.seedScores.FrontendSortableKeys);
                

                self.patternsPool([]);
                self.seedsPool([]);
                $.each(data.patternScores.data.keyList, function(i,key) {
                    var pvm = new PatternViewModel(key, self);
                    self.patternsPool.push(pvm);
                });

                $.each(data.seedScores.data.keyList, function(i,key) {
                    var svm = new SeedViewModel(key, self);
                    self.seedsPool.push(svm);
                });

                self.resortSeedsOrPatterns("seeds");
                self.resortSeedsOrPatterns("patterns");

                if (callback) callback();

                // vm.drawGraph();
                vm.unsetLoading();

                vm.startTimer();

            }
        });
    }

    self.clearUnknown = function() {
        vm.setLoading("Clearing unaccepted patterns & seeds...");
        $.post(baseURL+"/init/clear_unknown",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
        });
    }

    self.clearAll = function() {
        vm.setLoading("Clearing all patterns & seeds...");
        $.post(baseURL+"/init/clear_all",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
        });
    }

    self.proposePatterns = function() {
        vm.setLoading("Proposing patterns...");

        $.post(baseURL+"/init/propose_patterns",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
        })
    }

    self.getSimilarPatterns = function(){
        //WARNING:NO IMPLEMENT 
        // console.log("NOT IMPLEMENT");
        // return;


        vm.setLoading("Getting similar patterns");

        var calculated_patterns = [];
        for(var i = 0;i < self.patternsInDisplay().length;++i){
            if(self.patternsInDisplay()[i].frozenGood()){
                calculated_patterns.push(self.patternsInDisplay()[i].pattern.toIDString());
            }
        }
        $.post(baseURL + "/similarity/patterns",{patterns:JSON.stringify({patterns:calculated_patterns}),threshold:0.2,cutoff:50,target: self.name()},function(resp){
            console.log(resp);
            self.patternsInDisplay.remove(function(pattern){
                return !pattern.frozenGood();
            });
            $.each(resp,function(i,pattern){
                self.patternsInDisplay.push(new PatternViewModel(pattern.key,self));
            });
            vm.unsetLoading();
        });
        
    }

    self.getSimilarSeeds = function(){
        //WARNING:NO IMPLEMENT 
        // console.log("NOT IMPLEMENT");
        // return;
        vm.setLoading("Getting similar pairs");
        var calculated_seeds = [];
        for(var i = 0;i < self.seedsInDisplay().length;++i){
            if(self.seedsInDisplay()[i].frozenGood()){
                calculated_seeds.push([self.seedsInDisplay()[i].seed.json()]);
            }
        }
        $.post(baseURL + '/similarity/seeds',{seeds:JSON.stringify({seeds:calculated_seeds}),threshold:0.2,cutoff:50,language:"english",target: self.name()},function(resp){
            self.seedsInDisplay.remove( function(seed) { 
                return !seed.frozenGood(); } );
            $.each(resp,function(i,seed){
                self.seedsInDisplay.push(new SeedViewModel(seed.key,self));
            });
            vm.unsetLoading();
        });
    }

    self.getSimilarTriples = function(){
        //WARNING:NO IMPLEMENT 
        console.log("NOT IMPLEMENT");
        return;
        vm.setLoading("Getting similar triples");
        var calculated_triples = [];
        for(var i = 0;i < self.triples().length;++i){
            if(self.triples()[i].frozenGood()){
                const currentTriple = self.triples()[i];
                calculated_triples.push({slot0:currentTriple.seed.slot0,slot1:currentTriple.seed.slot1,pattern:currentTriple.pattern.toIDString()});
            }
        }
        $.post(baseURL + "/similarity/triples",{language:"english",triples:JSON.stringify({triples:calculated_triples}),threshold:0.2,cutoff:50,target: self.name()},function(resp){
            self.triples.remove(function(triple){
                return !triple.frozenGood();
            });
            $.each(resp,function(i,triple){
                self.triples.push(new TripleViewModel(patternLoader(triple.key.patternID),new Seed(triple.key.seed),self));
            });
            vm.unsetLoading();
        });
    }

    self.proposeSeeds = function() {
        vm.setLoading("Proposing seeds...");

        $.post(baseURL+"/init/propose_seeds",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
        })
    }

    self.initTriples = function(){
        self.triples.remove(function(triple){
            return !triple.frozen();
        });
        for(let i = 0;i < self.seedsInDisplay().length;++i){
            for(let j = 0;j < self.patternsInDisplay().length;++j){
                const seed = self.seedsInDisplay()[i]
                const pattern = self.patternsInDisplay()[j];
                if(seed.frozen() && seed.frozenGood() && pattern.frozen() && pattern.frozenGood()){
                    self.triples.push(new TripleViewModel(pattern.pattern,seed.seed,self));
                }
            }
        }
    }

    self.loadAdditionalSeeds = function(amount) {
        vm.setLoading("Grabbing additional seeds...");
        var num = amount ? amount : 10;

        $.post(baseURL+"/init/get_additional_seeds",{target: self.name(), amount: num}, function(data) {
            console.log(data);
            vm.unsetLoading();
            self.loadScores();
        });

    }

    self.rescore = function() {
        self.rescorePatterns(self.rescoreSeeds);
    }

    self.rescorePatterns = function(cb) {
        if (self.patternsInDisplay().length == 0) {
            self.loadScores();
            return;
        }

        vm.setLoading("Rescoring patterns...");

        $.post(baseURL+"/init/rescore_patterns",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
            if (cb) cb();
        })
    }

    self.rescoreSeeds = function(cb) {
        if (self.seedsInDisplay().length == 0) {
            self.loadScores();
            return;
        }

        vm.setLoading("Rescoring seeds...");

        $.post(baseURL+"/init/rescore_seeds",{target: self.name()}, function() {
            vm.unsetLoading();
            self.loadScores();
            if (cb) cb();
        })
    }

    self.addPatterns = function(patterns, quality, rescore) {

        $.each(patterns, function(i,p) {self.addPattern(p,quality,rescore);});
    }

    self.addPattern = function(pattern, quality, rescore) {

        vm.setLoading("Saving added pattern...");

        $.post(baseURL+'/init/add_pattern',
            {target:self.name(), pattern: pattern.toIDString(), quality: quality}, function() {
                vm.unsetLoading();
                if (rescore) {
                    self.rescoreSeeds();
                } else {
                }
            });
    }

    self.removePattern = function(pattern) {
        vm.setLoading("Saving pattern removal...");

        $.post(baseURL+'/init/remove_pattern',
            {target:self.name(), pattern: pattern.toIDString()}, function() {
                vm.unsetLoading();
            });
    }

    const InstanceResolver = function(data,idx){
        // const relationType = allTargets[data['RelationNameSet'][idx]] || vm.currentTarget();
        return {
            'instanceId':data['InstanceIdentifierSet'][idx],
            'instanceHTML':data['HTMLStringSet'][idx],
            'annotation':data['AnnotationSet'][idx],
            'relationType':data['RelationNameSet'][idx]
        };
    }

    self.getPatternInstances = function(pattern, amount) {
        self.markSeedOrPatternFromOtherVal(undefined);
        if (vm.infoHeader() != pattern) {
            self.instances([]);
        }
        vm.setLoading("Getting pattern instances...");
        vm.setLoadingInsts();
        $.post(baseURL+'/init/get_pattern_instances',
            {target:self.name(), pattern: pattern.pattern.toIDString(), amount: amount,fromOther: self.name().toLowerCase() === OTHERTypeName.toLowerCase()
            }, function(data) {
                if(vm.infoHeader() !== pattern)vm.infoHeader(pattern);
                const returnLength = data.InstanceIdentifierSet.length;
                for(let i = 0;i < returnLength && i < MaxNumberOfInstance;++i){
                    self.instances.push(new InstanceViewModel(InstanceResolver(data,i),self));
                }
                vm.getMoreInstances = self.getPatternInstances;
                vm.unsetLoading();
                vm.unsetLoadingInsts();
            });
    }

    self.getSeedInstances = function(seed, amount) {
        self.markSeedOrPatternFromOtherVal(undefined);
        if (vm.infoHeader() != seed) {
            self.instances([]);
        }
        vm.setLoading("Getting seed instances...");
        vm.setLoadingInsts();

        $.post(baseURL + '/init/get_seed_instances',
            {target:self.name(), seed: seed.seed.json(), amount: amount,fromOther: self.name().toLowerCase() === OTHERTypeName.toLowerCase()}, function(data) {
                if(vm.infoHeader() !== seed)vm.infoHeader(seed);
                const returnLength = data.InstanceIdentifierSet.length;
                for(let i = 0;i < returnLength && i < MaxNumberOfInstance;++i){
                    self.instances.push(new InstanceViewModel(InstanceResolver(data,i),self));
                }
                vm.getMoreInstances = self.getSeedInstances;
                vm.unsetLoading();
                vm.unsetLoadingInsts();
            });
    }

    self.getTripleInstances = function(triple,amount){
        self.markSeedOrPatternFromOtherVal(undefined);
        if (vm.infoHeader() != triple) {
            self.instances([]);
        }
        vm.setLoading("Getting triple instances...");
        vm.setLoadingInsts();

        $.post(baseURL+'/init/get_triple_instances',
            {target:self.name(), triple: JSON.stringify({"language":"english","slot0":triple.seed.slot0,"slot1":triple.seed.slot1,"pattern":triple.pattern.toIDString()}), amount: amount}, function(data) {
                if(vm.infoHeader() !== triple)vm.infoHeader(triple);
                const returnLength = data.InstanceIdentifierSet.length;
                for(let i = 0;i < returnLength && i < MaxNumberOfInstance;++i){
                    self.instances.push(new InstanceViewModel(InstanceResolver(data,i),self));
                }
                vm.getMoreInstances = self.getTripleInstances;
                vm.unsetLoading();
                vm.unsetLoadingInsts();
            });
    }

    self.addSeeds = function(seeds, quality) {

        vm.setLoading("Saving added seeds...");

        var seedStrings = [];
        $.each(seeds, function(i,seed) {seedStrings.push(seed.json());});

        $.post(baseURL + '/init/add_seeds',
            {target:self.name(), seeds: seedStrings, quality:quality}, function() {
                vm.unsetLoading();
            });
    }

    self.removeSeed = function(seed) {
        vm.setLoading("Saving seed removal...");
        $.post(baseURL + '/init/remove_seed',
            {target:self.name(), seed: seed.json()}, function() {
                vm.unsetLoading();
            });
    }

    self.addInstance = function(instance, quality) {
        vm.setLoading("Saving instance acceptance...");
        $.post(baseURL + '/init/add_instance',
            {target:self.name(), instance: instance.instance.instanceId,quality:quality}, function() {
                vm.unsetLoading();
            });
    }

    self.removeInstance = function(instance) {
        vm.setLoading("Saving instance removal...");
        $.post(baseURL + '/init/remove_instance',
            {target:self.name(), instance: instance.instance.instanceId}, function() {
                vm.unsetLoading();
            });
    }

    self.markInstanceFromOther = function(instance){
        vm.setLoading("Saving instance acceptance...");
        const instanceIdentifier = instance.instance.instanceId;
        const newRelationType = instance.markInstanceFromOtherVal();
        if(!newRelationType)return;
        $.post(baseURL + "/init/mark_instance_from_other",{target:newRelationType,instance:instanceIdentifier},function(){
            vm.unsetLoading();
        });
    }

    self.autoAcceptSeeds = function() {

        var seedStrings = self.bannedSeeds().length > 0 ? self.bannedSeeds() : ["null"];  //some value or it gets thrown out

        vm.setLoading("Auto-accepting Seeds...");
        $.post(baseURL + '/init/accept_seeds',{target:self.name(), amount: 99, bannedSeeds: seedStrings}, function() {
            vm.unsetLoading();
            //if (self.patterns().length < 100) {
            //    self.proposePatterns();
            //} else {
            self.rescorePatterns();
            //}
        });
    }
    self.markSeedOrPatternFromOtherVal = ko.observable();
    self.markSeedOrPatternFromOther = function(data,event){
        // console.log(data);
        // console.log(event);
        const choosenRelationTypeName = self.markSeedOrPatternFromOtherVal();
        let choosenRelationTypeString;
        if(typeof choosenRelationTypeName === "undefined"){
            return;
        }
        choosenRelationTypeString = choosenRelationTypeName.name();
        if(vm.infoHeader() instanceof PatternViewModel){
            const pattern = vm.infoHeader().pattern;
            $.post(baseURL + "/init/mark_pattern_from_other",{target:choosenRelationTypeString,pattern:pattern.toIDString()},function(){
                self.instances([]);
                self.getPatternInstances(vm.infoHeader(),MaxNumberOfInstanceForOther);
            });
        }
        if(vm.infoHeader() instanceof SeedViewModel){
            const seed = vm.infoHeader().seed;
            $.post(baseURL + "/init/mark_seed_from_other",{target:choosenRelationTypeString,seed:seed.json()},function(){
                self.instances([]);
                self.getSeedInstances(vm.infoHeader(),MaxNumberOfInstanceForOther);
            });
        }
    }

}


function loadSmallTarget(data) {
    if (data.name in allTargets)
        return allTargets[data.name];
    
    var result = new smallTarget(data.name, data.description, data.symmetric);
    allTargets[data.name] = result;
    return result;
}

function smallTarget(name, desc, symmetric) {
    var self = this;

    self.name = ko.observable(name);
    self.description = ko.observable(desc);
    self.symmetric = ko.observable(symmetric);

    self.color = relationColorScale(numTargets);
    numTargets++;

    self.toString = function() {
        return self.name();
    }
}




