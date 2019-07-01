var entityColorScale = d3.scale.ordinal().range(colorbrewer.Set1[8]);

//Rearranging colorbrewer Set2 to space the confusable colors out a bit more
var set2_8 = colorbrewer.Set2[8]
var relationColors = [set2_8[4],set2_8[2],set2_8[1],set2_8[3],set2_8[5],set2_8[0],set2_8[6],set2_8[7]]
var relationColorScale = d3.scale.ordinal().range(relationColors);

function ViewModel() {
    var self = this;

    self.focus = ko.observable(null);
    self.charge = ko.observable(-5000);
    self.information = ko.observable("");

    self.subscribeGraph = function(graph) {
        self.focus.subscribe(graph.update);
    }

    self.doEntitySearch = function() {
        var val = $('#entity-search').typeahead('val');

        alert("write this function using: "+val);
    }
}

var graphViewmodel = new ViewModel();
