/**
 * Created by mshafir on 4/14/14.
 */

function getURLParameter(name) {
    return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&]+?)(&|#|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20'))||null;
}

function loadMap(data,keyloader,valueloader) {
    var keys = [];
    var vals = [];
    var result = {};
    $.each(data.keyList, function(i,key) {keys.push(keyloader(key));});
    $.each(data.valList, function(i,val) {vals.push(valueloader(val));});
    $.each(data.entries, function(i,entry) {
        result[keys[entry.key]] = vals[entry.value];
    });
    return result;
}

function loadKeyObjectMap(data,keyloader) {
    var result = {};
    $.each(data.keyList, function(i,key) {
        var keyObj = keyloader(key);

        result[keyObj] = keyObj;
    });
    return result;
}

function BiMultimap(keys,values) {
    var self = this;
    self.keys = keys;
    self.values = values;
    self.map = {};
    self.inverseMap = {};

    self.put = function(key,val) {
        if (!(self.keys[key] in self.map)) {
            self.map[self.keys[key]] = [];
        }
        if (!(self.values[val] in self.inverseMap)) {
            self.inverseMap[self.values[val]] = [];
        }
        if (self.keys[key] != null && self.values[val] != null) {
            self.map[self.keys[key]].push(self.values[val]);
            self.inverseMap[self.values[val]].push(self.keys[key]);
        }
    }

    self.get = function(key) {
        if (!(key in self.map)) return [];
        return self.map[key];
    }
    self.inverseGet = function(val) {
        if (!(val in self.inverseMap)) return [];
        return self.inverseMap[val];
    }
}

function loadBiMap(data, keywrap, valuewrap) {

    var keys = [];
    var vals = [];

    $.each(data.keyList, function(i,key) {
        keys.push(keywrap(key));
    });
    $.each(data.valList, function(i,val) {
        vals.push(valuewrap(val));
    });

    var map = new BiMultimap(keys,vals);
    $.each(data.entries, function(i,entry) {
        if (entry.values) {
            $.each(entry.values, function(i,val) {
                map.put(entry.key,val);
            });
        } else {
            map.put(entry.key,entry.value);
        }
    });
    return map;
}


function removeDuplicates(lst) {
    var ret = {};
    $.each(lst, function(i,element) {ret[element] = true;});
    return Object.keys(ret);
}

ko.bindingHandlers.returnKey = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel) {
        ko.utils.registerEventHandler(element, 'keydown', function(evt) {
            if (evt.keyCode === 13) {
                evt.preventDefault();
                evt.target.blur();
                valueAccessor().call(viewModel);
            }
        });
    }
};

String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix, 0) == 0;
};

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

String.prototype.contains = function(it) { return this.indexOf(it) != -1; };

String.prototype.textBeforeFirst = function(it) { return this.substring(0,this.indexOf(it)); };
String.prototype.textAfterFirst = function(it) { return this.substring(this.indexOf(it)+1,this.length-1); };

