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

        ,   west__togglerContent_closed: "K<BR>E<BR>Y<BR>W<BR>O<BR>R<BR>D<BR>S"
        ,	west__togglerLength_closed:	200
        ,	west__togglerAlign_closed:	"center"
        ,   west__size:				400
        ,   west__initClosed: 			false

        ,   east__togglerContent_closed: "K<BR>E<BR>Y<BR>W<BR>O<BR>R<BR>D<BR>S"
        ,   east__togglerLength_closed: 200
        ,   east__togglerAlign_closed:  "center"
        ,   east__size:             .6
        ,   east__initClosed:           false

        ,   north__togglerContent_closed:"QUERIES"
        ,	north__togglerLength_closed:	140
        ,	north__togglerAlign_closed:	"center"
        ,   north__size:				50
        ,   north__initClosed: 			false

        ,   center__maxWidth:             400
        ,   center__minWidth:             400
    });

    ko.applyBindings(vm);

    vm.getQueries();
    vm.getAllSeeds();
});

function loadTargets() {
    $.post(baseURL + "/query/get_targets", function(data) {
        $.each(data, function(i,target) {
            vm.targets.push(loadTarget(target));
        })
        if (vm.targets().length == 0) {
            // makeType();
        }
    });
}