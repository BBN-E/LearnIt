package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.eventrelations.EventEventRelationMentionTensorFlowDecoderExecutable;
import com.bbn.serif.eventrelations.TensorFlowEventEventRelationMentionFinder;
import com.bbn.serif.relations.EventEventRelationMentionFinder;
import com.bbn.serif.relations.RelationMentionFinder;
import com.google.inject.Provides;

/**
 * Created by bmin on 3/28/17.
 */
public class TensorFlowEventEventRelationDecoderM extends AbstractParameterizedModule{

  protected TensorFlowEventEventRelationDecoderM(final Parameters parameters) {
    super(parameters);
  }

  @Override
  public void configure() {
    install(new TensorFlowEventEventRelationLabelScorer.FromParamsModule(params()));
    install(new EventEventRelationMentionTensorFlowDecoderExecutable.FromParametersModule(params()));
  }

  @Provides
  EventEventRelationMentionFinder deserializedEventEventRelationMentionFinder(TensorFlowEventEventRelationLabelScorer tensorFlowEventEventRelationLabelScorer) {
    return new TensorFlowEventEventRelationMentionFinder.Builder().labelScorer(tensorFlowEventEventRelationLabelScorer).build();
  }
}
