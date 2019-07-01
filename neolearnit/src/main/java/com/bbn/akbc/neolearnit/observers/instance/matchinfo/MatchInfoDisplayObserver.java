package com.bbn.akbc.neolearnit.observers.instance.matchinfo;

import java.util.Map;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.akbc.neolearnit.common.InstanceIdentifier;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfo;
import com.bbn.akbc.neolearnit.common.matchinfo.MatchInfoDisplay;
import com.bbn.akbc.neolearnit.mappings.Recorder;
import com.bbn.akbc.neolearnit.observers.instance.AbstractInstanceIdObserver;
import com.google.common.base.Optional;

public class MatchInfoDisplayObserver extends AbstractInstanceIdObserver<MatchInfoDisplay> {

	public MatchInfoDisplayObserver(Recorder<InstanceIdentifier, MatchInfoDisplay> recorder) {
		super(recorder);
	}

	@Override
	public void observe(MatchInfo observable) {
		this.record(observable, MatchInfoDisplay.fromMatchInfo(observable, Optional.<Map<Symbol,Symbol>>absent()));
	}



}
