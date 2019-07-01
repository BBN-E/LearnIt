/**
 * Created by mshafir on 4/16/14.
 */

//var evalReport = new EvalReport(getURLParameter("path"));

var vm = new InitViewModel();

$(document).ready(function() {

    $('#content').layout({
        closable:					true	// pane can open & close
        ,	resizable:					true	// when open, pane can be resized
        ,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
        ,	livePaneResizing:			true
        ,   spacing_closed:				20
        ,	spacing_open:				10

        ,   east__togglerContent_closed: "P<BR>A<BR>T<BR>T<BR>E<BR>R<BR>N<BR>S<BR><BR>A<BR>N<BR>D<BR><BR>S<BR>E<BR>E<BR>D<BR>S"
        ,	east__togglerLength_closed:	300
        ,	east__togglerAlign_closed:	"center"
        ,   east__size:				400
        ,   east__initClosed: 			false

        ,   west__togglerContent_closed: "R<BR>E<BR>L<BR>A<BR>T<BR>I<BR>O<BR>N<BR>S"
        ,	west__togglerLength_closed:	200
        ,	west__togglerAlign_closed:	"center"
        ,   west__size:				400
        ,   west__initClosed: 			false

        ,   north__togglerContent_closed:"RELATIONS"
        ,	north__togglerLength_closed:	140
        ,	north__togglerAlign_closed:	"center"
        ,   north__size:				100
        ,   north__initClosed: 			false

        ,   south__togglerContent_closed:"INSTANCES"
        ,	south__togglerLength_closed:	140
        ,	south__togglerAlign_closed:	"center"
        ,   south__size:				300
        ,   south__initClosed: 			false
    });

    ko.applyBindings(vm);

    loadTargets();

    // vm.graph.setup();

});

// function makeType() {
//     vm.addNewTarget();
//     vm.currentTarget().name("Spouse");
//     vm.currentTarget().symmetric(true);
//     vm.currentTarget().slot0Types[0].value(true);
//     vm.currentTarget().slot1Types[0].value(true);
//     vm.currentTarget().save();
// }



function loadTargets() {
    $.post(baseURL + "/init/get_targets", function(data) {
        $.each(data, function(i,target) {
            loadTarget(target,vm);
        })
    });
}