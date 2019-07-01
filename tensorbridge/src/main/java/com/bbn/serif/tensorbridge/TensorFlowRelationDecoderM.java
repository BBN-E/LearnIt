package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.learning2.FeatureIndexInterpreter;

import com.bbn.serif.relations.RelationMentionFinder;
import com.bbn.serif.relations.RelationMentionTensorFlowDecoderExecutable;
import com.bbn.serif.relations.TensorFlowRelationMentionFinder;
import com.google.inject.Provides;

/**
 * Created by bmin on 3/28/17.
 */
public class TensorFlowRelationDecoderM extends AbstractParameterizedModule{

  protected TensorFlowRelationDecoderM(final Parameters parameters) {
    super(parameters);
  }

  @Override
  public void configure() {
    install(new TensorFlowRelationLabelScorer.FromParamsModule(params()));
    install(new RelationMentionTensorFlowDecoderExecutable.FromParametersModule(params()));
  }

  @Provides
  RelationMentionFinder deserializedRelationMentionFinder(TensorFlowRelationLabelScorer tensorFlowRelationLabelScorer) {
    return new TensorFlowRelationMentionFinder.Builder().labelScorer(tensorFlowRelationLabelScorer).build();
  }
}
