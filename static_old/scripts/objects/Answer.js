/**
 * Created by mshafir on 4/29/14.
 */

function answerLoader(data) {
    return new Answer(data);
}

function AnswerMap(data) {
    var self = this;

    self.map = loadBiMap(data,instanceLoader,answerLoader);
    self.answers = self.map.values;
    self.correct = [];
    $.each(self.answers, function(i,ans) {
        if (ans.correct) {
            self.correct.push(ans);
        }
    });
}

function Answer(data) {
    var self = this;

    self.arg0EntityId = data.arg0EntityId;
    self.arg1EntityId = data.arg1EntityId;
    self.correct = data.correct;
    self.docid = data.docid;
    self.sentid = data.sentid;
    self.matchedAnnotations = $.map(data.matchedAnnotations, function(a) { return new RelationAnnotation(a);});

    self.toString = function() {
        return self.docid+"-"+self.sentid+"-"+self.arg0EntityId+"-"+self.arg1EntityId;
    }
    self.toDisplay = function() {
        return "document: "+self.docid+
            ", sentence: "+self.sentid+
            ", entities: "+self.arg0EntityId+":"+self.arg1EntityId;
    }
}

function RelationAnnotation(data) {
    var self = this;

    self.relationType = data.relationType;
    self.arg0 = new SpanningAnnotation(data.arg0);
    self.arg1 = new SpanningAnnotation(data.arg1);
    self.text = data.text;

    self.toString = function() {
        return self.relationType+", arg0=["+self.arg0+"], arg1=["+self.arg1+"], text="+self.text;
    }
}

function SpanningAnnotation(data) {
    var self = this;

    self.docid = data.docid;
    self.startOffset = data.startOffset;
    self.endOffset = data.endOffset;
    self.sentid = data.sentid;
    self.startToken = data.startToken;
    self.endToken = data.endToken;

    self.toString = function() {
        return self.docid+":"+self.sentid+":("+
            self.startOffset+"-"+self.endOffset+"):("+self.startToken+":"+self.endToken+")";
    }
}
