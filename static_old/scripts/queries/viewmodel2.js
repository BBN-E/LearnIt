/**
 * Created by mshafir on 4/29/14.
 */

function QueryViewModel() {
    var self = this;

    self.queries = ko.observableArray([]);
    self.selectedQuery = ko.observable("Show All");
    self.instances = ko.observableArray([]);

    self.systemSeedList = new SeedListViewModel(self);
    self.humanSeedList = new SeedListViewModel(self);

    self.seedVMMap = {};

    /*self.systemCoref = ko.observable();
    self.humanCoref = ko.observable();
    self.systemCorefOn = ko.obervable(false);
    self.humanCorefOn = ko.obervable(false);*/

    self.getQueries = function() {
        $.post("/query/get_queries", {}, function(qStrings) {
            self.queries(qStrings);
            self.queries.push("Show All");
        });
    }

    self.selectQuery = function(query) {
        self.selectedQuery(query);
        self.getQuerySeeds();
    }

    self.getAllSeeds = function() {
        $.post("/query/get_all_seeds", {}, function(seeds) {
            self.systemSeedList.setSeeds(seeds,false);
        });
        $.post("/query/get_all_human_seeds", {}, function(seeds) {
            self.humanSeedList.setSeeds(seeds,true);
        });
    }

    self.getQuerySeeds = function() {
        $.post("/query/get_query_seeds", {query: self.selectedQuery()}, function(seeds) {
            self.systemSeedList.setSeeds(seeds,false);
        });
        $.post("/query/get_human_query_seeds", {query: self.selectedQuery()}, function(seeds) {
            self.humanSeedList.setSeeds(seeds,true);
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

    self.shutdown = function() {
        if (confirm("Are you sure you want save all changes and shutdown the server?")) {
            $.post('/query/shutdown',{},function() {self.stopTimer(); alert("You may now close this tab");});
        }
    }

    /*self.save = function() {
        var good = [];
        var bad = [];
        $.each(self.instances(), function(i,inst) {
            if (inst.good())
                good.push(inst.matchDisplay.langDisplays[inst.matchDisplay.primaryLanguage].instance.json());
            if (inst.bad())
                bad.push(inst.matchDisplay.langDisplays[inst.matchDisplay.primaryLanguage].instance.json());
        });

        if (good.length > 0)
            $.post("/query/add_instances", {instances: good, quality: "good"}, self.reorderInstances());
        if (bad.length > 0)
            $.post("/query/add_instances", {instances: bad, quality: "bad"}, self.reorderInstances());
    }*/

    self.addGood = function(seed) {
        $.post("/query/add_seed", {seed: seed.json(), quality: "good"}, self.systemSeedList.reSort());
    }
    self.addBad = function(seed) {
        $.post("/query/add_seed", {seed: seed.json(), quality: "bad"}, self.systemSeedList.reSort());
    }

    /*self.setSystemCoref = function(seed) {
        self.systemCoref(seed);
        self.systemCorefOn(true);
    }
    self.unsetSystemCoref = function() {
        self.systemCoref();
        self.systemCorefOn(false);
    }
    self.setHumanCoref = function(seed) {
        self.humanCoref(seed);
        self.humanCorefOn(true);
    }
    self.unsetHumanCoref = function() {
        self.humanCoref();
        self.humanCorefOn(false);
    }*/

    self.setCoref = function() {
        var systemSeed = self.systemSeedList.focusedSeed();
        var humanSeed  = self.humanSeedList.focusedSeed();
        if (systemSeed != null && humanSeed != null) {
            $.post("/query/coref_seed", {humanSeed: humanSeed.seed.json(), systemSeed: systemSeed.seed.json()}, 
                self.addGood(systemSeed.seed));
            return true;
        } else {
            return false;
        }
    }

    self.unset = function(seed) {
        $.post("/query/remove_seed", {seed: seed.json()}, self.systemSeedList.reSort());
    }

    self.getClickedInstances = function(seed) {
        $.post("/query/get_seed_instances", {seed: seed.json()}, function(instances) {
            var temp = [];
            $.each(instances, function(i,inst) {
                temp.push(new MatchDisplayViewModel(inst));
            });
            self.instances(temp);
        });
    }

    self.getInstances = function() {
        if (self.systemSeedList.focusedSeed() != null) {
            $.post("/query/get_seed_instances", {seed: self.systemSeedList.focusedSeed().seed.json()}, function(instances) {
                self.instances(instances);
            });
        }
    }
}

function SeedListViewModel(vm) {
    var self = this;

    self.vm = vm;
    self.seeds = ko.observableArray([]);
    self.focusedSeed = ko.observable();

    self.setSeeds = function(seeds,human) {
        var temp = []
        $.each(seeds, function(i,seed) {
            var key = seedLoader(seed).toString();
            if (!(key in vm.seedVMMap)) {
                vm.seedVMMap[key] = new SimpleSeedViewModel(seed,self.vm,human);
            }
            temp.push(vm.seedVMMap[key]);
        });
        self.seeds(temp);
        self.reSort();
        self.focusedSeed = ko.observable(null);
    }

    self.setFocus = function(seed) {
        if (self.focusedSeed() != null)
            self.focusedSeed().focused(false);
        self.focusedSeed(seed);
        self.focusedSeed().focused(true);
    }

    self.reSort = function() {
        self.seeds.sort(function(a,b){
            if (a.good() && !b.good()) return -1;
            if (b.good() && !a.good()) return 1;
            if (a.linked() && !b.linked()) return -1;
            if (b.linked() && !a.linked()) return 1;
            if (a.bad() && !b.bad()) return -1;
            if (b.bad() && !a.bad()) return 1;
            if (a.redundant() && !b.redundant()) return 1;
            if (b.redundant() && !a.redundant()) return -1;

            if (a.toString() <= b.toString()) return -1;
            else return 1;
        });
    }
}

