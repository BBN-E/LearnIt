/**
 *  author: mshafir@bbn.com
 *  Feb 2014
 */

var graph;
var viewmodel;

$( document ).tooltip();


function entityTypeAheadSource(query, callback) {
    console.log("Looking up entities starting with "+query);
    /*
    $.post('/lookup_entity', { text: query }, function(data) {
        viewmodel.resetServerInfo();

        console.log(data);
        if (!('items' in data)) return;
        if (data.items.length > 0) {
            var filtered = mentionFilterSort(data);
            callback(filtered.slice(0,10));
        }
    });
    */
    callback([{text:'choice1'},{text:'choice2'},{text:'choice3'},{text:'choice4'}]);
}

$(document).ready(function() {

    console.log("Setting up document");
    graph = new Graph().setup();
    ko.applyBindings(viewmodel);

    $('#entity-search').typeahead({minLength: 2},
        {
            displayKey: function(d) { return d.text; },
            source: entityTypeAheadSource,
            templates: {
                suggestion: Handlebars.compile(
                    '<p>{{text}}</p>'
                )
            }
        });
    $('#entity-search').bind("typeahead:selected", function() { viewmodel.doEntitySearch(); });

    $('#contents').layout({
        closable:					true	// pane can open & close
        ,	resizable:					true	// when open, pane can be resized
        ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,	livePaneResizing:			true
        ,   spacing_closed:				20
        ,	spacing_open:				10

        ,   south__togglerContent_closed:"DETAILS"
        ,	south__togglerLength_closed:	140
        ,	south__togglerAlign_closed:	"center"
        ,   south__size:				200
        ,   south__initClosed: 			false
    });

    viewmodel.subscribeGraph(graph);

    graphTest();
});


function graphTest() {

    $.post('/graph/get_sys_kb',{dummy: 'dummy'}, function (sysKB) {	
	$.each(sysKB.listRelations, function(i,relation) {
		var arg1id = relation.srcEntityId;
		var arg2id = relation.dstEntityId;		
		
		var n1 = new Node(arg1id,arg1id);
		var n2 = new Node(arg2id,arg2id);

		graph.addNode(n1);
		graph.addNode(n2);
		
		var e1 = new Edge(arg1id+arg2id,n1,n2, relation.relnType);
		graph.addEdge(e1);
		});
    });

	/*
    var n1 = new Node('n1','PER:Bill Gates');
    var n2 = new Node('n2','PER:Melinda Gates');
    var n3 = new Node('n3','PER:Jennifer Gates');
    var e1 = new Edge('e1',n1,n2, "family");
    var e2 = new Edge('e2',n1,n3, "family2");
    var e3 = new Edge('e3',n2,n3);

    var n4 = new Node('n4','PER:Bill Clinton');
    var n5 = new Node('n5','PER:Hilary Clinton');
    var e4 = new Edge('e4',n4,n5);
    var e5 = new Edge('e5',n4,n5);

    var n10 = new Node("a",'PER:MJ');

    graph.addNodes([n1,n2,n3,n4,n5,n10]);
    graph.addEdges([e1,e2,e3,e4,e5]);
    */
}


