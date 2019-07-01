package com.bbn.akbc.neolearnit.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinimumInstanceIdentifier {

    final public String docId;
    final public int sentId;
    final public int slot0Start;
    final public int slot0End;
    final public int slot1Start;
    final public int slot1End;

    public MinimumInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        this.docId = instanceIdentifier.getDocid();
        this.sentId = instanceIdentifier.getSentid();
        this.slot0Start = instanceIdentifier.getSlot0Start();
        this.slot0End = instanceIdentifier.getSlot0End();
        this.slot1Start = instanceIdentifier.getSlot1Start();
        this.slot1End = instanceIdentifier.getSlot1End();
    }

    public MinimumInstanceIdentifier(String docId, int sentId, int slot0Start, int slot0End, int slot1Start, int slot1End) {
        this.docId = docId;
        this.sentId = sentId;
        this.slot0Start = slot0Start;
        this.slot0End = slot0End;
        this.slot1Start = slot1Start;
        this.slot1End = slot1End;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinimumInstanceIdentifier that = (MinimumInstanceIdentifier) o;
        return this.docId.equals(that.docId) && this.sentId == that.sentId && this.slot0Start == that.slot0Start && this.slot0End == that.slot0End && this.slot1Start == that.slot1Start && this.slot1End == that.slot1End;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int ret = this.docId.hashCode();
        ret = ret * prime + this.sentId;
        ret = ret * prime + this.slot0Start;
        ret = ret * prime + this.slot0End;
        ret = ret * prime + this.slot1Start;
        ret = ret * prime + this.slot1End;
        return ret;
    }

    @Override
    public String toString() {
        List<String> ret = new ArrayList<>();
        ret.add(this.docId);
        ret.add(Integer.toString(this.sentId));
        ret.add(Integer.toString(this.slot0Start));
        ret.add(Integer.toString(this.slot0End));
        ret.add(Integer.toString(this.slot1Start));
        ret.add(Integer.toString(this.slot1End));
        return ret.stream().collect(Collectors.joining(",", "[", "]"));
    }

}
