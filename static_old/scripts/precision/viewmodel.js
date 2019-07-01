/**
 * Created by mshafir on 4/29/14.
 */

function PrecisionEvalViewModel() {
    var self = this;


    self.infoHeader = ko.observable();
    self.info = ko.observable([]);
    self.infoSentenceId = ko.observable([]);
    self.getMoreInstances = function() {};
    self.loadMoreInstances = function() {
        self.getMoreInstances(self.infoHeader(),5);
    }

    self.instances = ko.observableArray([]);
    self.target = ko.observable();

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
            self.saveWithCallback(function() {
                $.post('/precision/shutdown',{},function() {});
                self.stopTimer();
            });
        }
    }

    self.save = function() {
        self.saveWithCallback(self.getInstances);
    }

    self.saveWithCallback = function(callback) {
        var good = [];
        var bad = [];
        $.each(self.instances(), function(i,inst) {
            if (inst.good())
                good.push(inst.matchDisplay.langDisplays[inst.matchDisplay.primaryLanguage].instance.json());
            if (inst.bad())
                bad.push(inst.matchDisplay.langDisplays[inst.matchDisplay.primaryLanguage].instance.json());
        });

        self.removeJudged();

        if (good.length > 0 && bad.length > 0) {
            $.post("/precision/add_instances", {instances: good, quality: "good"}, function(data) {
                $.post("/precision/add_instances", {instances: bad, quality: "bad"}, function(data) {
                    callback();
                });
            });
        } else if (good.length > 0) {
            $.post("/precision/add_instances", {instances: good, quality: "good"}, function(data) {
                callback();
            });
        } else if (bad.length > 0) {
            $.post("/precision/add_instances", {instances: bad, quality: "bad"},  function(data) {
                callback();
            });
        } else {
            callback();
        }
    }

    self.removeJudged = function() {
        var temp = [];
        $.each(self.instances(), function(i,inst) {
            if (!inst.good() && !inst.bad())
                temp.push(inst);
        });
        self.instances(temp);
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

    self.refresh = function() {
        if (self.currentQuery()) { self.currentQuery().loadScores(); }
    }

    self.loadAdditionalSeeds = function() {
        if (self.currentQuery()) { self.currentQuery().loadAdditionalSeeds(); }
    }

    self.toggleInfoAccept = function() {
        if (self.infoHeader()) {
            self.infoHeader().toggleAccept();
        }
    }

    self.getInstances = function() {
        console.log("Getting instances...");
        tempInsts = []
        $.post("/precision/get_instances", {}, function(data) {
            $.each(data, function(i,inst) {
                tempInsts.push(new MatchDisplayViewModel(inst));
            });
            self.instances(tempInsts);
        });
    }

    self.getTarget = function() {
        $.post("/precision/get_target", {}, function(data) {
            self.target(data);
        });
    }

    self.clearAll = function() {
        if (confirm("Are you sure you want to clear ALL instances for this query?")) {
            $.each(self.instances(), function(i,inst) {
                if (inst.good() || inst.bad()) {
                    inst.unset();
                }
            });
            self.reorderInstances();
        }
    }

}

