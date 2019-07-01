package com.bbn.learning.tensorflow;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.annotations.MoveToBUECommon;
import com.bbn.bue.common.parameters.Parameters;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Object to allow access to a Tensorflow (or other) program written in Python from Java.
 * This is intended to be used together with {@code tensorflow-bridge/python/TFServer.py}.
 *
 * Your Python program should be a class which contains a method called "decode" which takes in
 * the JSON representation of some input object and returns the JSON representation of some output
 * object.  Those input and output objects should correspond to POJOs of {@code InMsgType}
 * and {@code OutMsgType}.  I strongly suggest you use Immutables to define these and be sure to
 * mark them with {@code JsonDeserialize(as=....)}.
 *
 * Be sure to call {@link #close()} when you are done using this decoder to make sure
 * the Python server is shutdown.
 *
 * See {@link TensorFlowDecoderWrapperTest} for an example.
 *
 * @param <InMsgType>  The type of input object to be decoded by the Python program.
 *                   Must be JSON-serializable by Jackson. We highly recommend you use
 *                   Immutables to generate this.
 *
 * @param <OutMsgType> The type of object which is the result of decoding by the Python program.
 *                   Must be JSON-serializable by Jackson. We highly recommend you use
 *                   Immutables to generate this.
 */
public final class TensorFlowDecoderWrapper<InMsgType,OutMsgType> implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(TensorFlowDecoderWrapper.class);

  private final String pythonDecoderClassName;
  private final Process process;
  private final WebTarget decodeTarget;
  private final WebTarget shutdownTarget;
  private final Class<OutMsgType> outputClass;
  private final int shutdownDelayMs;
  private final int requestWaitMs;
  private boolean closed = false;

  private TensorFlowDecoderWrapper(final Process process, final String pythonDecoderClassName,
      final WebTarget decodeTarget, WebTarget shutdownTarget, final Class<OutMsgType> outputClass,
      int shutdownDelayMs, int requestWaitMs) {
    this.process = checkNotNull(process);
    this.pythonDecoderClassName = checkNotNull(pythonDecoderClassName);
    checkArgument(!pythonDecoderClassName.isEmpty());
    this.decodeTarget = checkNotNull(decodeTarget);
    this.shutdownTarget = checkNotNull(shutdownTarget);
    this.outputClass = checkNotNull(outputClass);

    this.shutdownDelayMs = shutdownDelayMs;
    checkArgument(shutdownDelayMs >= 0);
    this.requestWaitMs = requestWaitMs;
    checkArgument(requestWaitMs >= 0);
  }

  private static final int STARTUP_CHECK_INTERVAL_MS = 2500;

  /**
   * Starts up a Python program specified and provides a Java wrapper to interact with it.
   *
   * @param inClass  The Java {@link Class} object for the input message type.
   * @param outClass The Java {@link Class} object for the output message type.
   * @param pythonDecoderClassName The name of the Python decoder class being wrapped
   * @param pythonDecoderParams Any parameters which should be passed to the Python decoder class.
   *                            These are currently ignored (issue text-group/deep-kbp#6).
   * @param pythonBin The path to the Python binary
   * @param tfServerPythonScript The path to {@code deep-kbp/tensorflow-bridge/python/TFServer.py}
   * @param startupTimeInMs How many milliseconds to wait for the server to start up.
   * @param shutdownTimeMs How many millisecons to wait for the server to shut down.
   * @param requestTimeMs How many milliseconds to wait for a response to a request.
   * @throws IOException
   */
  public static <InMsgType, OutMsgType> TensorFlowDecoderWrapper<InMsgType, OutMsgType> start(
      Class<InMsgType> inClass, Class<OutMsgType> outClass,
      String pythonDecoderClassName, Parameters pythonDecoderParams,
      File pythonBin, String pythonPath, File tfServerPythonScript,
      long startupTimeInMs, long shutdownTimeMs, long requestTimeMs
  ) throws IOException {
    // write parameters for flask and the decoder program itself to temporary files
    final File tmpFile = File.createTempFile("tensorFlowDecoder", ".params");
    tmpFile.deleteOnExit();
    final File tmpParamsFile = File.createTempFile("tensorFlowDecoder", ".passthrough.params");

    Files.asCharSink(tmpFile, Charsets.UTF_8).write(
        String.format("DECODER_CLASS = '%s'\nPARAMS= '%s'",
            pythonDecoderClassName, tmpParamsFile.getAbsoluteFile()));
    Files.asCharSink(tmpParamsFile, Charsets.UTF_8).write(pythonDecoderParams.dumpWithoutNamespacePrefix());

    // start the Python process
    final String[] command = {pythonBin.getAbsolutePath(), "-m", "flask", "run"};
    final ProcessBuilder processBuilder = new ProcessBuilder(command);
    // make sure any errors from the Python program get printed to stderr
    processBuilder.inheritIO();
    processBuilder.environment().put("TF_SERVER_PARAMS", tmpFile.getAbsolutePath());
    processBuilder.environment().put("FLASK_APP", tfServerPythonScript.getAbsolutePath());
    final int port = 5000; //findOpenPort();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // we don't care if our sleep is interrupted
    }
    processBuilder.environment().put("SERVER_NAME", "127.0.0.1:" + port);
    processBuilder.environment().put("TF_SERVER_PORT", Integer.toString(port));
    final String previousPythonPath = processBuilder.environment().get("PYTHONPATH");
    processBuilder.environment().put("PYTHONPATH", pythonPath + ":"
        + Optional.fromNullable(previousPythonPath).or(""));

    processBuilder.redirectErrorStream(true);
    log.info("Starting up tensorflow server with command {}\nenvironment: {}",
        StringUtils.spaceJoiner().join(command), processBuilder.environment());
    Process process = processBuilder.start();

    // create REST API client
    final javax.ws.rs.client.Client client = ClientBuilder.newClient().register(JacksonFeature.class);
    final WebTarget rootTarget = client.target("http://127.0.0.1:" + port);
    final WebTarget readyTarget = rootTarget.path("ready");

    final Stopwatch startupTimer = Stopwatch.createStarted();
    while (true) {
      try {
        // we keep asking the server if it is ready until either it gets ready or we time out
        final Response readyResponse = readyTarget.request().get();
        log.info("Established connection to Python server in {}s",
            startupTimer.elapsed(TimeUnit.SECONDS));
        checkArgument(shutdownTimeMs < Integer.MAX_VALUE);
        checkArgument(requestTimeMs < Integer.MAX_VALUE);
        return new TensorFlowDecoderWrapper<>(process, pythonDecoderClassName,
            rootTarget.path("decode"), rootTarget.path("shutdown"), outClass,
            (int)shutdownTimeMs, (int)requestTimeMs);
      } catch (ProcessingException pe) {
        if (pe.getCause() instanceof ConnectException
          && startupTimer.elapsed(TimeUnit.MILLISECONDS) <= startupTimeInMs) {
          if (hasTerminated(process)) {
            throw new RuntimeException("Tensorflow server terminated when attempting startup.");
          }
          try {
            Thread.sleep(STARTUP_CHECK_INTERVAL_MS);
          } catch (InterruptedException e) {
            // we don't care if our sleep is interrupted
          }
        } else {
          throw pe;
        }
      }
    }
  }

  /**
   * Returns an open port, if it can find one.  Note the port is not guaranteed to still be open
   * when you try to use it!
   */
  @MoveToBUECommon
  private static int findOpenPort() throws IOException {
    // passing 0 to ServerSocket cause sit to use any open port
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @MoveToBUECommon
  private static boolean hasTerminated(Process process) {
    try {
      process.exitValue();
      return true;
    } catch (IllegalThreadStateException itse) {
      return false;
    }

  }

  public OutMsgType decode(InMsgType input) throws DecodingTimedOutException {
    return decodeTarget.request(MediaType.APPLICATION_JSON_TYPE)
        .property(ClientProperties.READ_TIMEOUT, requestWaitMs)
        .post(Entity.entity(input, MediaType.APPLICATION_JSON_TYPE), outputClass);
  }

  private static final int POST_SHUTDOWN_RETURN_INTERVAL = 60 * 1000;
  public void close() {
    if (!closed) {
      // ask the server nicely to shutdown
      shutdownTarget.request().property(ClientProperties.READ_TIMEOUT, shutdownDelayMs)
          .post(Entity.entity("bye", MediaType.TEXT_PLAIN_TYPE));

      // after it says it will shutdown, wait politely for a while
      synchronized (process) {
        try {
          process.wait(POST_SHUTDOWN_RETURN_INTERVAL);
        } catch (InterruptedException e) {
          // if we get interrupted early while waiting, oh well
        }
      }

      // if it hasn't listened, stop being polite
      process.destroy();

      // wait till it's gone
      while (true) {
        try {
          process.waitFor();
          break;
        } catch (InterruptedException e) {
          // if we're interrupted while waiting, just start waiting again
        }
      }

      // and check its return value
      if (process.exitValue() != 0) {
        throw new RuntimeException(
            String.format("Wrapper for %s terminated with non-zero exit code",
                pythonDecoderClassName));
      }
    }
    closed = true;
  }

  public static final class DecodingTimedOutException extends Exception {
    private DecodingTimedOutException() {
      super("Decoding timed out");
    }
  }

}

