/**
 * Created by mshafir on 4/16/14.
 */

var evalReport = new EvalReport(getURLParameter("path"));


$(document).ready(function() {
    evalReport.load();

});