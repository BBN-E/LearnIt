package com.bbn.akbc.neolearnit.common;

public class Triple<X,Y,Z> {
    final public X x;
    final public Y y;
    final public Z z;
    public Triple(X x,Y y,Z z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int hashCode(){
        int prime = 31;
        int ret = this.x.hashCode();
        ret = ret * prime + this.y.hashCode();
        ret = ret * prime + this.z.hashCode();
        return ret;
    }

    @Override
    public boolean equals(Object o){
        if(o == this)return true;
        if(!o.getClass().equals(this.getClass()))return false;
        Triple that = (Triple)o;
        return this.x.equals(that.x) &&
                this.y.equals(that.y) &&
                this.z.equals(that.z);
    }
}
