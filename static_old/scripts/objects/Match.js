/**
 * Created by mshafir on 4/29/14.
 */

function instanceLoader(data) {
    return new InstanceIdentifier(data);
}

function InstanceIdentifier(data) {
    var self = this;

    this.docid = data.docid;
    this.sentid = data.sentid;
    this.slot0Start = data.slot0Start;
    this.slot0End = data.slot0End;
    this.slot1Start = data.slot1Start;
    this.slot1End = data.slot1End;

    this.toString = function() {
        return self.docid+":"+self.sentid+":"+self.slot0Start+"-"+self.slot0End+":"+self.slot1Start+"-"+self.slot1End;
    }

    this.json = function() {
        return JSON.stringify({docid: self.docid, sentid:self.sentid,
            slot0Start:self.slot0Start, slot0End:self.slot0End,
            slot1Start:self.slot1Start, slot1End:self.slot1End});
    }
}

function matchDisplayLoader(data) {
    return new MatchInfoDisplay(data);
}

function MatchInfoDisplay(data) {
    var self = this;
    
    self.target = loadTarget(data.target);
    self.slot0s = [];
    self.slot1s = [];
    
    self.primaryLanguage = data.primaryLanguage;

    self.docId = null;

    self.langDisplays = {};
    for (var lang in data.langDisplays) {
        self.langDisplays[lang] = new LangMatchInfoDisplay(data.langDisplays[lang]);
        if (self.langDisplays[lang].instance != null) {
            self.slot0s.push(self.langDisplays[lang].seed.slot0);
            self.slot1s.push(self.langDisplays[lang].seed.slot1);
            if (self.docId == null) self.docId = self.langDisplays[lang].instance.docid;
        }
    }
    
    self.slot0String = self.slot0s.join("<br/>");
    self.slot1String = self.slot1s.join("<br/>");

    self.getHtml = function() {
        var result = "";
        for (var lang in self.langDisplays) {
            result += "<strong>"+lang+"</strong>";
            result += "<br />"+self.langDisplays[lang].htmlDisplay()+"<br/>";
        }
        return result;
    }
    self.html = self.getHtml();
	
    self.getNoLinkHtml = function() {
        var result = "";
        for (var lang in self.langDisplays) {
            result += "<strong>"+lang+"</strong>";
            result += "<br />"+self.langDisplays[lang].html.split("<br/>")[0]+"<br/>";
        }
        return result;
    }
    self.noLinkHtml = self.getNoLinkHtml();

    self.getPrimaryLanguageHtml = function() {
        return self.langDisplays[self.primaryLanguage].html;
    }
    self.primaryLanguageHtml = self.getPrimaryLanguageHtml();
	
    self.toString = function() {
        var langStrings = [];
        for (var lang in self.langDisplays) {
            langStrings.push(self.langDisplays[lang]);
        }
        return "[target="+self.target.name()+", langDisplays="+langStrings.join(", ")+"]";
    }

    var slot0RE = /"slot0">([^<]*?)([^<]*?<span class="slot1">.*?<\/span>.*?)?<\/span>/g;
    var slot1RE = /"slot1">(.*?)<\/span>/g;
    var slot1REAlt = /"slot1">([^<]*?<span class="slot0">.*?<\/span>.*?)<\/span>/g;
    //self.extra0 = slot0REAlt.test(self.noLinkHtml) ? self.noLinkHtml.match(slot0REAlt) : self.noLinkHtml.match(slot0RE);
    self.extra0 = self.noLinkHtml.match(slot0RE);
    self.extra1 = slot1REAlt.test(self.noLinkHtml) ? self.noLinkHtml.match(slot1REAlt) : self.noLinkHtml.match(slot1RE);

    self.slot0ExtraString = self.slot0String;
    self.slot1ExtraString = self.slot1String;

    var start = "\"slotX\">".length;
    var end = "</span>".length;
    for (i = 0; i < self.extra0.length; ++i) {
        var string0 = self.extra0[i].substring(start,self.extra0[i].length - end).replace(/<.*?>/,"");
        self.slot0ExtraString += "<br/>\""+string0+"\"";
    }
    for (i = 0; i < self.extra1.length; ++i) {
        var string1 = self.extra1[i].substring(start,self.extra1[i].length - end).replace(/<.*?>/,"");
        self.slot1ExtraString += "<br/>\""+string1+"\"";
    }
}

function LangMatchInfoDisplay(data) {
    var self = this;

    self.language = data.language;
    self.html = data.html;
    self.visualizationLink = data.visualizationLink;
    //self.sentenceTokens = data.sentenceTokens;
    if (data.instance != null) {
        self.instance = instanceLoader(data.instance);
        self.seed = new Seed(data.seed);
    } else {
        self.instance = null;
        self.seed = null;
    }

    self.htmlDisplay = function() {
        return self.html+"<br /><a href=\""+self.visualizationLink+"\" target=\"_blank\">serif info</a>";
    }
    
    self.toString = function() {
        if (self.instance != null) {
            return "[language="+self.language+", instance="+self.instance.toString()+", seed="+self.seed.toString()+"]";
        } else {
            return self.html;
        }
    }
}

function MatchDisplayViewModel(data) {
    var self = this;

    self.matchDisplay = matchDisplayLoader(data);
    self.instance = self.matchDisplay.langDisplays[self.matchDisplay.primaryLanguage].instance;

    self.good = ko.observable(false);
    self.bad  = ko.observable(false);

    self.setGood = function() {
        self.good(true);
        //$.post("/query/add_instance", {instance: self.instance.json(), quality: "good"}, function(){});
    }

    self.setBad = function() {
        self.bad(true);
        //$.post("/query/add_instance", {instance: self.instance.json(), quality: "bad"}, function(){});
    }

    self.unset = function() {
        self.good(false);
        self.bad(false);
        //$.post("/query/remove_instance",{instance: self.instance.json()}, function(){});
    }

    self.toString = function() {
        return self.matchDisplay.toString();
    }
}

function DocQueryDisplay(data) {
    var self = this;

    self.docId = data.docid;
    self.query = data.query;
    self.slot = data.slot;

    self.sentWindows = [];
    $.each(data.sentWindows, function(i,sw) {
        self.sentWindows.push(new SentenceWindow(sw));
    });
    
    //self.html = data.html;
    //self.sentenceEntities = loadBiMap(data.sentenceEntityMap, function(i){return i}, function(s){return s});
}

function SentenceWindow(data) {
    var self = this;
    self.html = data.html;
    self.entities = data.entities;
}