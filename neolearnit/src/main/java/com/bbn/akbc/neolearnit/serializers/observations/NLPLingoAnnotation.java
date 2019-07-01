package com.bbn.akbc.neolearnit.serializers.observations;

import com.bbn.akbc.neolearnit.common.Triple;
import com.bbn.bue.common.symbols.Symbol;

import java.util.HashSet;
import java.util.Set;

public class NLPLingoAnnotation {

    final static public class TriggerMarkingSpan{
        public final Symbol docId;
        public final Symbol docPath;
        public final int sentStartCharOff;
        public final int sentEndCharOff;
        public final int triggerStartCharOff;
        public final int triggerEndCharOff;
        public final Symbol triggerWord;

        public TriggerMarkingSpan(final Symbol docId,final Symbol docPath,final int sentStartCharOff,final int sentEndCharOff,final int triggerStartCharOff,final int triggerEndCharOff,final Symbol triggerWord){
            this.docId = docId;
            this.docPath = docPath;
            this.sentStartCharOff = sentStartCharOff;
            this.sentEndCharOff = sentEndCharOff;
            this.triggerStartCharOff = triggerStartCharOff;
            this.triggerEndCharOff = triggerEndCharOff;
            this.triggerWord = triggerWord;
        }
    }
    public static class SentSpan{
        final Symbol docId;
        final int sentCharStart;
        final int sentCharEnd;

        public SentSpan(final Symbol docId,final int sentCharStart,final int sentCharEnd){
            this.docId = docId;
            this.sentCharStart = sentCharStart;
            this.sentCharEnd = sentCharEnd;
        }

        @Override
        public boolean equals(Object o){
            if(o == this)return true;
            if(o.getClass() != this.getClass())return false;
            SentSpan that = (SentSpan)o;
            return that.sentCharStart == this.sentCharStart &&
                    that.sentCharEnd == this.sentCharEnd &&
                    that.docId.equalTo(this.docId);
        }

        @Override
        public int hashCode(){
            int prime = 31;
            int ret = sentCharStart;
            ret = ret*prime + sentCharEnd;
            ret = ret * prime + docId.hashCode();
            return ret;
        }

        public String toNlplingoSentSpan(){
            return String.format("%s %d %d\n",docId,sentCharStart,sentCharEnd+1);
        }
    }

    final static public class NlplingoEventMention{
        final Symbol docId;
        final int sentStartCharOff;
        final int sentEndCharOff;
        final int triggerStartCharOff;
        final int triggerEndCharOff;
        final Symbol eventType;
        final Set<Triple<Symbol,Integer,Integer>> arguments;
        public NlplingoEventMention(TriggerMarkingSpan triggerMarkingSpan,Symbol eventType){
            this.docId = triggerMarkingSpan.docId;
            this.sentStartCharOff = triggerMarkingSpan.sentStartCharOff;
            this.sentEndCharOff = triggerMarkingSpan.sentEndCharOff;
            this.triggerStartCharOff = triggerMarkingSpan.triggerStartCharOff;
            this.triggerEndCharOff = triggerMarkingSpan.triggerEndCharOff;
            this.eventType = eventType;
            this.arguments = new HashSet<>();
        }
        public NlplingoEventMention(final Symbol docId,final int sentStartCharOff,final int sentEndCharOff,final int triggerStartCharOff,final int triggerEndCharOff,Symbol eventType){
            this.docId = docId;
            this.sentStartCharOff = sentStartCharOff;
            this.sentEndCharOff = sentEndCharOff;
            this.triggerStartCharOff = triggerStartCharOff;
            this.triggerEndCharOff = triggerEndCharOff;
            this.eventType = eventType;
            this.arguments = new HashSet<>();
        }
        NlplingoEventMention withArgument(Symbol argumentType,int argStartCharOff,int argEndCharOff){
            arguments.add(new Triple<>(argumentType,argStartCharOff,argEndCharOff));
            return this;
        }

        public String toNlplingoSpan(){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("<Event type=\"%s\">\n",this.eventType));
            stringBuilder.append(String.format("%s %d %d\n",this.eventType,this.sentStartCharOff,this.sentEndCharOff+1));
            for(Triple<Symbol,Integer,Integer> triple: arguments){
                stringBuilder.append(String.format("%s/%s %d %d\n",this.eventType,triple.x,triple.y,triple.z+1));
            }
            stringBuilder.append(String.format("anchor %d %d\n",this.triggerStartCharOff,triggerEndCharOff+1));
            stringBuilder.append("</Event>\n");
            return stringBuilder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NlplingoEventMention that = (NlplingoEventMention) o;

            return that.docId.equalTo(this.docId) &&
                    that.sentStartCharOff == this.sentStartCharOff &&
                    that.sentEndCharOff == this.sentEndCharOff &&
                    that.triggerStartCharOff == this.triggerStartCharOff &&
                    that.triggerEndCharOff == this.triggerEndCharOff &&
                    that.eventType.equalTo(this.eventType) &&
                    that.arguments.equals(this.arguments);
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int ret = this.docId.hashCode();
            ret += ret * prime + this.sentStartCharOff;
            ret += ret * prime + this.sentEndCharOff;
            ret += ret * prime + this.triggerStartCharOff;
            ret += ret * prime + this.triggerEndCharOff;
            ret += ret * prime + this.eventType.hashCode();
            ret += ret * prime + this.arguments.hashCode();
            return ret;
        }
    }

}
