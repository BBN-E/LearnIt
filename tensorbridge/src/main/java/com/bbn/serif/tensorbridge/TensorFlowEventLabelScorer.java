package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.learning.tensorflow.ExternalClassificationDecisionWithLabel;
import com.bbn.learning.tensorflow.TensorFlowDecoderWrapper;
import com.bbn.serif.events.SentenceEventMentionCandidate;
import com.bbn.serif.events.SentenceEventMentionCandidateMessage;
import com.bbn.serif.events.SentenceEventMentionCandidateToMessageConverter;
import com.bbn.serif.events.TensorFlowEventMentionFinder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;


public final class TensorFlowEventLabelScorer implements TensorFlowEventMentionFinder.EventLabelScorer,
    Finishable {

  private static final Logger log = LoggerFactory.getLogger(TensorFlowEventLabelScorer.class);

  private final TensorFlowDecoderWrapper<SentenceEventMentionCandidateMessage, ExternalClassificationDecisionWithLabel>
      tensorflowWrapper;
  private final SentenceEventMentionCandidateToMessageConverter messageConverter;

  @Inject
  TensorFlowEventLabelScorer(
      final TensorFlowDecoderWrapper<SentenceEventMentionCandidateMessage, ExternalClassificationDecisionWithLabel> tensorflowWrapper,
      final SentenceEventMentionCandidateToMessageConverter messageConverter) {
    this.tensorflowWrapper = tensorflowWrapper;
    this.messageConverter = messageConverter;
  }

  @Override
  public Map<String, Double> getScoredOutcomes(final SentenceEventMentionCandidate candidate) {
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
      install(new SentenceEventMentionCandidateToMessageConverter.FromParamsModule(params()));
      bind(TensorFlowEventMentionFinder.EventLabelScorer.class).to(TensorFlowEventLabelScorer.class);
    }

    @Provides
    TensorFlowDecoderWrapper<SentenceEventMentionCandidateMessage, ExternalClassificationDecisionWithLabel> getDecoderWrapper()
        throws IOException {
      final Parameters subParams =
          params().copyNamespace("com.bbn.serif.tensorbridge.decoderParams");
      return TensorFlowDecoderWrapper
          .start(SentenceEventMentionCandidateMessage.class, ExternalClassificationDecisionWithLabel.class,
              subParams.getString("pythonDecoderClassNameForEventMention"),
              subParams.copyNamespace("pythonDecoderParams"),
              subParams.getExistingFile("pythonBin"),
              subParams.getString("pythonPath"),
              subParams.getExistingFile("tfServerPythonScript"),
              DEFAULT_STARTUP_TIME.getMillis(),
              DEFAULT_SHUTDOWN_TIME.getMillis(), DEFAULT_REQUEST_TIME.getMillis());
    }
  }
}
