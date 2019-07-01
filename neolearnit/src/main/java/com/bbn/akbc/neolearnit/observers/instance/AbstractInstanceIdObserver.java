package com.bbn.akbc.neolearnit.observers.instance;

import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observers.Observer;


public abstract class AbstractInstanceIdObserver<Property> implements Observer<MatchInfo> {

	protected final Recorder<InstanceIdentifier, Property> recorder;

	protected AbstractInstanceIdObserver(Recorder<InstanceIdentifier, Property> recorder) {
		this.recorder = recorder;
	}

	protected void record(MatchInfo instance, Property property) {
		this.recorder.record(InstanceIdentifier.from(instance), property);
	}

}
