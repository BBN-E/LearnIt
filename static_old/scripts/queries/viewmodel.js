/**
 * Created by mshafir on 4/29/14.
 */

function QueryViewModel() {
    var self = this;

    //self.queryTerm = ko.observable();
    //self.querySlot = ko.observable();
    //self.targetName = ko.observable();

    self.question = ko.observable();

    self.infoHeader = ko.observable();
    self.info = ko.observable([]);
    self.infoSentenceId = ko.observable([]);
    self.getMoreInstances = function() {};
    self.loadMoreInstances = function() {
        self.getMoreInstances(self.infoHeader(),5);
    }

    self.keywords = ko.observableArray([]);
    self.docs = ko.observableArray([]);

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

            var entities = self.keywords().length > 0 ? self.keywords() : [''];

            $.post('/query/shutdown',{"entities": entities},function(data) {
                alert("You may now refresh this tab");
                self.stopTimer();
            });    
        }
    }

    self.getDocs = function() {
        $.post("/query/get_docs", {}, function(data) {
            $.each(data, function(i,doc) {
                self.docs.push(new DocQueryDisplay(doc));
            });
        });
    }

    self.addEntity = function(text) {
        duplicate = false;
        $.each(self.keywords(), function(i,kw) {
            if (kw == text) duplicate = true;
        });
        if (duplicate) return;
        self.keywords.push(text);
    }

    self.removeEntity = function(key) {
        tempKeys = [];
        $.each(self.keywords(), function(i,kw) {
            if (kw != key) tempKeys.push(kw);
        });
        self.keywords(tempKeys);
    }

    self.keywordForm = new KeywordFormViewModel(self);
}

function KeywordFormViewModel(vm) {
    var self = this;

    self.vm = vm;
    self.keyword = ko.observable("");

    self.submitKeyword = function() {

        if (self.keyword() == "") return;

        tempKeys = [];
        duplicate = false;
        $.each(self.vm.keywords(), function(i,kw) {
            tempKeys.push(kw);
            if (kw == self.keyword()) duplicate = true;
        });
        if (duplicate) return;
        tempKeys.push(self.keyword());
        self.vm.keywords(tempKeys);
    }
}

