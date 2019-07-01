/**
 *  author: mshafir@bbn.com
 *  Feb 2014
 */

String.prototype.splice = function( idx, s ) {
    return (this.slice(0,idx) + s + this.slice(idx));
};


function Node(id, text, color, size) {
    var self = this;

    self.id = id;
    self.text = text;
    self.detail = ko.observable('');
    self.edges = [];
    self.color = color ? color : 'lightblue';

    self.graph = null;

    self.fixedSize = size ? size : 20;
    self.size = function() { return self.fixedSize; };

    self.selected = ko.computed(function() { return graphViewmodel.focus() == self; })

    self.select = function() {
        console.log('setting relation focus');
        graphViewmodel.focus(self);
        self.graph.update();
        self.fetchInfo();
    }

    self.fetchInfo = function() {

        /*    HOW TO FETCH MORE INFORMATION ABOUT A NODE FROM THE SERVER   */
        //self.detail('loading...');
        //$.post('/graph/get_detail',{item: self.id}, function (info) {
        //    self.detail(info);
        //})
    }

    self.visible = function() {
        return true;
    };


}