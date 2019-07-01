/**
 * Created by mshafir on 4/14/14.
 */

function nameSort(a,b) {
    if (b.name > a.name) {
        return 1
    } else if (a.name > b.name) {
        return -1;
    } else {
        return 0;
    }
}

function ViewModel() {
    var self = this;

    self.reports = ko.observable();
    self.reportTopLevel = ko.observable();

    $.post("/get_eval_reports",{}, function(data) {
        var reports = $.map(data,function(d) { return new Report(d);});
        reports.sort(function(a,b) {
            if (b.name > a.name) {
                return 1
            } else if (a.name > b.name) {
                return -1;
            } else {
                return 0;
            }
        });

        self.reports(reports);
        self.reportTopLevel(new ReportGroup('Evaluation Reports',reports).regroupItemsByExpt());
    });

    self.refreshCache = function() {
        $.post('/eval/refresh_cache',{}, function(data) {
            console.log(data);

            $('#check').fadeIn();
            setTimeout(function() {$('#check').fadeOut();;}, 2000);
        })
    }

}

function ReportGroup(name,items) {
    var self = this;

    this.name = name;
    this.items = items;
    this.items.sort(nameSort);
    this.addItem = function(item) {
        self.items.push(item);
    }

    this.show = ko.observable(false);
    this.toggleShow = function() {self.show(!self.show());}

    this.regroupItemsByExpt = function() {

        var exptMap = {};
        $.each(self.items, function(i,report) {
            multimapAdd(exptMap, report.experiment, report);
        });
        var items = [];
        for (var expt in exptMap) {
            items.push(new ReportGroup(expt, exptMap[expt]).regroupItemsByRelation());
        }
        return new ReportGroup(self.name, items);

    }

    this.regroupItemsByRelation = function() {

        var relMap = {};
        $.each(self.items, function(i,report) {
            multimapAdd(relMap, report.relation, report);
        });
        var items = [];
        for (var rel in relMap) {
            items.push(new ReportGroup(rel, relMap[rel]));
        }
        return new ReportGroup(self.name, items);
    }
}

function Report(data) {
    var self = this;

    self.rawPath = data;

    var str = data;
    self.experiment = str.textBeforeFirst('/');
    str = str.textAfterFirst('/');
    self.relation = str.textBeforeFirst('/');
    str = str.textAfterFirst('/');
    self.name = str;

    self.date = self.name.split('_')[0].replace(/\./g,'/');
    self.time = self.name.split('_')[1].replace(/\./g,':');

    self.displayText = "Run started on "+self.date+" at "+self.time;


    self.open = function() {
        window.open("/html/evaluation.html?path="+self.rawPath,'_blank');
    }
}


function multimapAdd(map,key,val) {
    if (!(key in map)) {
        map[key] = [];
    }
    map[key].push(val);
}

$(document).ready(function() {
    vm = new ViewModel();
    ko.applyBindings(vm);
});

