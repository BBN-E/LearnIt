package com.bbn.serif.tensorbridge;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.io.GZIPByteSource;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.serialization.jackson.JacksonSerializer;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.bue.learning.alphabets.Alphabets;
import com.bbn.bue.learning.alphabets.OneToOneAlphabet;
import com.bbn.bue.learning2.LogLinearClassificationModeller;
import com.bbn.learning.tensorflow.ExternalClassificationDecision;
import com.bbn.learning.tensorflow.ExternalClassificationDecisionWithLabel;
import com.bbn.learning.tensorflow.TensorFlowDecoderWrapper;

import com.bbn.serif.relations.SentenceRelationMentionCandidate;
import com.bbn.serif.relations.SentenceRelationMentionCandidateMessage;
import com.bbn.serif.relations.SentenceRelationMentionCandidateToMessageConverter;
import com.bbn.serif.relations.TensorFlowRelationMentionFinder;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Provides;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Qualifier;


public final class TensorFlowRelationLabelScorer implements TensorFlowRelationMentionFinder.RelationLabelScorer,
    Finishable {

  private static final Logger log = LoggerFactory.getLogger(TensorFlowRelationLabelScorer.class);

  private final TensorFlowDecoderWrapper<SentenceRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel>
      tensorflowWrapper;
  private final SentenceRelationMentionCandidateToMessageConverter messageConverter;

  @Inject
  TensorFlowRelationLabelScorer(
      final TensorFlowDecoderWrapper<SentenceRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel> tensorflowWrapper,
      final SentenceRelationMentionCandidateToMessageConverter messageConverter) {
    this.tensorflowWrapper = tensorflowWrapper;
    this.messageConverter = messageConverter;
  }

  @Override
  public Map<String, Double> getScoredOutcomes(final SentenceRelationMentionCandidate candidate) {
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
      install(new SentenceRelationMentionCandidateToMessageConverter.FromParamsModule(params()));
      bind(TensorFlowRelationMentionFinder.RelationLabelScorer.class).to(TensorFlowRelationLabelScorer.class);
    }

    @Provides
    TensorFlowDecoderWrapper<SentenceRelationMentionCandidateMessage, ExternalClassificationDecisionWithLabel> getDecoderWrapper()
        throws IOException {
      final Parameters subParams =
          params().copyNamespace("com.bbn.serif.tensorbridge.decoderParams");
      return TensorFlowDecoderWrapper
          .start(SentenceRelationMentionCandidateMessage.class, ExternalClassificationDecisionWithLabel.class,
              subParams.getString("pythonDecoderClassNameForRelation"),
              subParams.copyNamespace("pythonDecoderParams"),
              subParams.getExistingFile("pythonBin"),
              subParams.getString("pythonPath"),
              subParams.getExistingFile("tfServerPythonScript"),
              DEFAULT_STARTUP_TIME.getMillis(),
              DEFAULT_SHUTDOWN_TIME.getMillis(), DEFAULT_REQUEST_TIME.getMillis());
    }
  }
}
