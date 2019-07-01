/**
 *  author: mshafir@bbn.com
 *  Feb 2014
 */

function Edge(id,n1,n2,text,clickfunc,color,confidence) {
    var self = this;

    self.id = id;
    self.sourceNode = n1;
    self.targetNode = n2;
    self.text = text;
    self.clickfunc = clickfunc;
    self.color = color ? color : 'black';
    self.selected = ko.computed(function() { return graphViewmodel.focus() == self; });
    self.detail = ko.observable('')

    self.graph = null;

    //register this as a connected edge
    n1.edges.push(self);
    n2.edges.push(self);

    self.select = function() {
        console.log('setting relation focus');
        graphViewmodel.focus(self);
        self.graph.update();
        self.fetchInfo();
    }

    self.fetchInfo = function() {

        self.clickfunc();
    }

    self.confidence = confidence
    self.opacity = function() { return 0.5 + confidence/2.0; };

    self.visible = function() {
        return true;
    };

}
