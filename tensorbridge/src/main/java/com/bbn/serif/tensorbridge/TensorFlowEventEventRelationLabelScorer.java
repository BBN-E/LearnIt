package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.learning.tensorflow.ExternalClassificationDecisionWithLabel;
import com.bbn.learning.tensorflow.TensorFlowDecoderWrapper;
import com.bbn.serif.eventrelations.EventEventRelationMentionCandidate;
import com.bbn.serif.eventrelations.EventEventRelationMentionCandidateMessage;
import com.bbn.serif.eventrelations.EventEventRelationMentionCandidateToMessageConverter;
import com.bbn.serif.eventrelations.TensorFlowEventEventRelationMentionFinder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;


public final class TensorFlowEventEventRelationLabelScorer implements TensorFlowEventEventRelationMentionFinder.EventEventRelationLabelScorer,
    Finishable {

  private static final Logger log = LoggerFactory.getLogger(TensorFlowEventEventRelationLabelScorer.class);

  private final TensorFlowDecoderWrapper<EventEventRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel>
      tensorflowWrapper;
  private final EventEventRelationMentionCandidateToMessageConverter messageConverter;

  @Inject
  TensorFlowEventEventRelationLabelScorer(
      final TensorFlowDecoderWrapper<EventEventRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel> tensorflowWrapper,
      final EventEventRelationMentionCandidateToMessageConverter messageConverter) {
    this.tensorflowWrapper = tensorflowWrapper;
    this.messageConverter = messageConverter;
  }

  @Override
  public Map<String, Double> getScoredOutcomes(final EventEventRelationMentionCandidate candidate) {
    try {
      final ImmutableMap.Builder<String, Double> ret = ImmutableMap.builder();

      final ExternalClassificationDecisionWithLabel externalClassificationDecisionWithLabel = tensorflowWrapper
          .decode(
              messageConverter.toMessage(candidate).build());
      ret.put(externalClassificationDecisionWithLabel.label(), externalClassificationDecisionWithLabel.confidence());

      return ret.build();
    } catch (TensorFlowDecoderWrapper.DecodingTimedOutException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void finish() throws IOException {
    tensorflowWrapper.close();
  }

  public static class FromParamsModule extends AbstractParameterizedModule {

    protected FromParamsModule(final Parameters parameters) {
      super(parameters);
    }

    final Duration DEFAULT_STARTUP_TIME = Duration.standardMinutes(5);
    final Duration DEFAULT_SHUTDOWN_TIME = Duration.standardMinutes(2);
    final Duration DEFAULT_REQUEST_TIME = Duration.standardMinutes(1);

    @Override
    public void configure() {
      install(new EventEventRelationMentionCandidateToMessageConverter.FromParamsModule(params()));
      bind(TensorFlowEventEventRelationMentionFinder.EventEventRelationLabelScorer.class).to(TensorFlowEventEventRelationLabelScorer.class);
    }

    @Provides
    TensorFlowDecoderWrapper<EventEventRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel> getDecoderWrapper()
        throws IOException {
      final Parameters subParams =
          params().copyNamespace("com.bbn.serif.tensorbridge.decoderParams");
      return TensorFlowDecoderWrapper
          .start(EventEventRelationMentionCandidateMessage.class, ExternalClassificationDecisionWithLabel.class,
              subParams.getString("pythonDecoderClassNameForEventEventRelation"),
              subParams.copyNamespace("pythonDecoderParams"),
              subParams.getExistingFile("pythonBin"),
              subParams.getString("pythonPath"),
              subParams.getExistingFile("tfServerPythonScript"),
              DEFAULT_STARTUP_TIME.getMillis(),
              DEFAULT_SHUTDOWN_TIME.getMillis(), DEFAULT_REQUEST_TIME.getMillis());
    }
  }
}
