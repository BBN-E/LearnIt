function Instance(data) {
    var self = this;

    self.instanceId = data.instanceId;
    self.instanceHTML = data.instanceHTML;
    self.toString = function() {
        return self.instanceHTML;
    }
    self.json = function() {
        const ret = {};
        ret[self.instanceId] = self.instanceHTML;
        return JSON.stringify(ret);
    }
    self.html = function() {
        return self.instanceHTML;
    }
}

function InstanceViewModel(data,target){
    var self = this;
    self.instance = new Instance(data);
    self.target = target;
    self.markInstanceFromOtherVal = ko.observable(data.relationType);
    self.toggleGood = function(){
        if(self.frozen() && self.good()){
            self.frozen(false);
            self.target.removeInstance(self);
        }
        else{
            self.frozen(true);
            self.good(true);
            self.target.addInstance(self,"good");
        }
    }
    self.toggleBad = function(){
        if(self.frozen() && !self.good()){
            self.frozen(false);
            self.target.removeInstance(self);
        }
        else{
            self.frozen(true);
            self.good(false);
            self.target.addInstance(self,"bad");
        }
    }

    self.unfrozen = function(){
        self.frozen(false);
        self.target.removeInstance(self);
    }


    const AnnotationDataMapper = function(annotation){
        switch(annotation){
            case "FROZEN_GOOD":
                self.frozen = ko.observable(true);
                self.good = ko.observable(true);
                return;
            case "NO_FROZEN":
                self.frozen = ko.observable(false);
                self.good = ko.observable(false);
                return;
            case "FROZEN_BAD":
                self.frozen = ko.observable(true);
                self.good = ko.observable(false);
                return;
        }
    }
    AnnotationDataMapper(data.annotation);
    self.frozenGood = ko.computed(function() {return self.frozen() && self.good();});
    self.frozenBad = ko.computed(function() {return self.frozen() && !self.good();});
    
}