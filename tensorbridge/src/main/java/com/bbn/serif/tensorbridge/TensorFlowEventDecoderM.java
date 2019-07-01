package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.events.EventMentionFinder;
import com.bbn.serif.events.EventMentionTensorFlowDecoderExecutable;
import com.bbn.serif.events.TensorFlowEventMentionFinder;
import com.google.inject.Provides;

/**
 * Created by bmin on 3/28/17.
 */
public class TensorFlowEventDecoderM extends AbstractParameterizedModule{

  protected TensorFlowEventDecoderM(final Parameters parameters) {
    super(parameters);
  }

  @Override
  public void configure() {
    install(new TensorFlowEventLabelScorer.FromParamsModule(params()));
    install(new EventMentionTensorFlowDecoderExecutable.FromParametersModule(params()));
  }

  @Provides
  EventMentionFinder deserializedEventMentionFinder(TensorFlowEventLabelScorer tensorFlowEventLabelScorer) {
    return new TensorFlowEventMentionFinder.Builder().labelScorer(tensorFlowEventLabelScorer).build();
  }
}
