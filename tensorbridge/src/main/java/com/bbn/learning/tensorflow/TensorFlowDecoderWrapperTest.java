package com.bbn.learning.tensorflow;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.parameters.Parameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Example of using {@link TensorFlowDecoderWrapper}.
 *
 * Example command: {@code TensorFlowDecoderWrapperTest /opt/Python-2.7.8-x86_64/bin/python
 * /export/u10/rgabbard/repos/deep-kbp/tensorflow-bridge/python/TFServer.py}
 */
public class TensorFlowDecoderWrapperTest {
  private static final Logger log = LoggerFactory.getLogger(TensorFlowDecoderWrapperTest.class);

  public static void main(String[] argv) {
    // we wrap the main method in this way to
    // ensure a non-zero return value on failure
    try {
      trueMain(argv);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void trueMain(String[] argv)
      throws IOException, TensorFlowDecoderWrapper.DecodingTimedOutException {
    final File pythonBin = new File(argv[0]);
    final File tensorFlowExampleServer = new File(argv[1]);

    try (final TensorFlowDecoderWrapper<SimpleJsonThingy, SimpleJsonThingy> decoder =
        TensorFlowDecoderWrapper.start(SimpleJsonThingy.class, SimpleJsonThingy.class,
            "SampleProgramSampleProgram", Parameters.builder().build(),
            pythonBin, "", tensorFlowExampleServer, 300 * 1000, 300 * 1000, 300 * 1000))
    {
      log.info("Response was {}", decoder.decode(
          new SimpleJsonThingy.Builder().name("hello").value(42).build()));
    }
  }
}

@JsonSerialize
@JsonDeserialize(as=ImmutableSimpleJsonThingy.class)
@TextGroupImmutable
@Value.Immutable
abstract class SimpleJsonThingy {
  public abstract String name();
  public abstract int value();

  public static SimpleJsonThingy of(String name, int value) {
    return new ImmutableSimpleJsonThingy.Builder().name(name).value(value).build();
  }

  static class Builder extends ImmutableSimpleJsonThingy.Builder {}
}
