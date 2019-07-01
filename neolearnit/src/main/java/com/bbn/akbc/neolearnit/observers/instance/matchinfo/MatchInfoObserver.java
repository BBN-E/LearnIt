package com.bbn.akbc.neolearnit.observers.instance.matchinfo;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observers.instance.AbstractInstanceIdObserver;

public class MatchInfoObserver extends AbstractInstanceIdObserver<MatchInfo> {

	public MatchInfoObserver(Recorder<InstanceIdentifier, MatchInfo> recorder) {
		super(recorder);
	}

	@Override
	public void observe(MatchInfo observable) {
		this.record(observable, observable);
	}



}
