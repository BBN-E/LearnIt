/**
 * Created by mcrivaro on 8/29/14.
 */

//var evalReport = new EvalReport(getURLParameter("path"));

var vm = new QueryViewModel();

$(document).ready(function() {

    $('#content').layout({
        closable:					true	// pane can open & close
        ,	resizable:					true	// when open, pane can be resized
        ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,	livePaneResizing:			true
        ,   spacing_closed:				20
        ,	spacing_open:				10

        ,   west__togglerContent_closed: "E<BR>N<BR>T<BR>I<BR>T<BR>I<BR>E<BR>S"
        ,	west__togglerLength_closed:	200
        ,	west__togglerAlign_closed:	"center"
        ,   west__size:				400
        ,   west__initClosed: 			false

        ,   north__togglerContent_closed:"SNIPPETS"
        ,	north__togglerLength_closed:	140
        ,	north__togglerAlign_closed:	"center"
        ,   north__size:				50
        ,   north__initClosed: 			false
    });

    ko.applyBindings(vm);

    loadQueryInfo();
    vm.getDocs();
});



function loadQueryInfo() {
    $.post("/query/get_question", function(data) {
        vm.question(data);
    });
    /*$.post("/query/get_query_term", function(text) {
        vm.queryTerm(text);
        $.post("/query/get_query_slot", function(slot) {
            vm.querySlot(slot);
            $.post("/query/get_target_name", function(name) {
                vm.targetName(name);
            });
        });
    });*/
}