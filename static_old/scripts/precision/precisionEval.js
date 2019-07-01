/**
 * Created by mcrivaro on 8/29/14.
 */

//var evalReport = new EvalReport(getURLParameter("path"));

var vm = new PrecisionEvalViewModel();

$(document).ready(function() {

    $('#content').layout({
        closable:					true	// pane can open & close
        ,	resizable:					true	// when open, pane can be resized
        ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,	livePaneResizing:			true
        ,   spacing_closed:				20
        ,	spacing_open:				10

        ,   north__togglerContent_closed:"RELATIONS"
        ,	north__togglerLength_closed:	140
        ,	north__togglerAlign_closed:	"center"
        ,   north__size:				50
        ,   north__initClosed: 			false
    });

    ko.applyBindings(vm);

    vm.getTarget();
    vm.getInstances();
});