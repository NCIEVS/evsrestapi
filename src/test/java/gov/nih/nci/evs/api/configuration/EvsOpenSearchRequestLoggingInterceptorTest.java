package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link EvsOpenSearchRequestLoggingInterceptor}.
 *
 * <p>These tests avoid Spring context startup and exercise the Apache HTTP request objects
 * directly, which is the layer where the interceptor reads, logs, and restores outgoing OpenSearch
 * requests.
 */
@ResourceLock("EvsOpenSearchRestTemplateLogger")
class EvsOpenSearchRequestLoggingInterceptorTest {

  /** The logger used by the interceptor. */
  private Logger logger;

  /** The original logger level. */
  private Level originalLevel;

  /** The original logger additive setting. */
  private boolean originalAdditive;

  /** The appender used to inspect log events. */
  private ListAppender<ILoggingEvent> appender;

  /** Configure in-memory logging for each test. */
  @BeforeEach
  void setup() {
    logger = (Logger) LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);
    originalLevel = logger.getLevel();
    originalAdditive = logger.isAdditive();

    // The interceptor is DEBUG-gated. Flip only this logger for the test and make it non-additive
    // so the captured request log does not also go to the root console appender.
    logger.setLevel(Level.DEBUG);
    logger.setAdditive(false);

    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  /** Restore logger state after each test. */
  @AfterEach
  void cleanup() {
    // Logger configuration is JVM-global, so every test restores the original state it changed.
    logger.detachAppender(appender);
    appender.stop();
    logger.setLevel(originalLevel);
    logger.setAdditive(originalAdditive);
  }

  /**
   * Get the single request log emitted by the interceptor.
   *
   * @return the logged request message
   */
  private String getLoggedMessage() {
    // Each interceptor invocation should produce exactly one request log entry.
    assertEquals(1, appender.list.size());
    return appender.list.get(0).getFormattedMessage();
  }

  /** Test that search requests log the actual request details and preserve the payload. */
  @Test
  void logsSearchRequestDetailsAndPreservesPayload() throws Exception {
    final String payload =
        """
        {
          "query": {
            "match": {
              "user.id": "kimchy"
            }
          },
          "fields": [
            "user.id",
            "http.response.*",
            {
              "field": "@timestamp",
              "format": "epoch_millis"
            }
          ],
          "_source": false
        }
        """;
    final BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest(
            "POST", "/my-index-000001/_search?typed_keys=true&search_type=query_then_fetch");
    request.setEntity(
        new StringEntity(
            payload, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));

    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final String logMessage = getLoggedMessage();
    assertTrue(
        logMessage.contains(
            "\n    POST http://localhost:9201/my-index-000001/_search?"
                + "typed_keys=true&search_type=query_then_fetch"));
    assertTrue(logMessage.contains("\n    my-index-000001 = "));
    assertTrue(logMessage.contains("\"fields\""));
    assertTrue(logMessage.contains("\"user.id\""));
    assertTrue(logMessage.contains("\"_source\": false"));
    assertEquals(payload, EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8));
  }

  /**
   * Test that payload logging preserves entity metadata when replacing the consumed request body.
   *
   * <p>{@link EvsOpenSearchRequestLoggingInterceptor} reads the request entity so it can log the
   * serialized OpenSearch payload. That read consumes the original entity, so the interceptor
   * replaces it before the request continues. This test documents that the replacement keeps the
   * same body and HTTP entity metadata needed to send it as JSON.
   */
  @Test
  void preservesPayloadEntityMetadataAfterLogging() throws Exception {
    final String payload = "{\"query\":{\"match_all\":{}}}";
    final BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", "/my-index/_search");
    final StringEntity entity =
        new StringEntity(payload, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
    entity.setContentEncoding("identity");
    request.setEntity(entity);

    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final ContentType restoredContentType = ContentType.get(request.getEntity());
    assertEquals(payload, EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8));
    assertEquals(ContentType.APPLICATION_JSON.getMimeType(), restoredContentType.getMimeType());
    assertEquals(StandardCharsets.UTF_8, restoredContentType.getCharset());
    assertEquals("identity", request.getEntity().getContentEncoding().getValue());
  }

  /**
   * Test that large payloads are truncated in the log but remain intact on the request.
   *
   * <p>The interceptor is meant to make requests replayable without letting DEBUG logging flood the
   * application log. The truncation only applies to the log message; the full entity must be
   * restored before OpenSearch receives the request.
   */
  @Test
  void truncatesLargeLoggedPayloadAndPreservesFullRequestBody() throws Exception {
    final String unloggedSuffix = "UNLOGGED_SUFFIX";
    final String payload = "{\"query\":\"" + "a".repeat(100_100) + unloggedSuffix + "\"}";
    final BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", "/my-index/_search");
    request.setEntity(
        new StringEntity(
            payload, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));

    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final String logMessage = getLoggedMessage();
    assertTrue(logMessage.contains("<... payload truncated after 100000 characters>"));
    assertFalse(logMessage.contains(unloggedSuffix));
    assertEquals(payload, EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8));
  }

  /**
   * Test that non-JSON payloads are not decoded or logged.
   *
   * <p>OpenSearch requests are expected to be JSON or newline-delimited JSON. If another content
   * type reaches the interceptor, logging a placeholder avoids exposing unrelated request bodies
   * and leaves the original entity untouched.
   */
  @Test
  void doesNotLogNonJsonPayloads() throws Exception {
    final String payload = "password=super-secret";
    final BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", "/my-index/_search");
    request.setEntity(
        new StringEntity(payload, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)));

    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final String logMessage = getLoggedMessage();
    assertTrue(logMessage.contains("<payload not logged: content type text/plain>"));
    assertFalse(logMessage.contains("super-secret"));
    assertEquals(payload, EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8));
  }

  /**
   * Test that payloads without content type are still logged as UTF-8 text.
   *
   * <p>The content-type guard allows missing content type because the OpenSearch client usually
   * sends JSON but the low-level Apache entity metadata is not always guaranteed by callers.
   */
  @Test
  void logsPayloadsWithoutContentType() throws Exception {
    final String payload = "{\"query\":{\"match_all\":{}}}";
    final BasicHttpEntityEnclosingRequest request =
        new BasicHttpEntityEnclosingRequest("POST", "/my-index/_search");
    request.setEntity(new ByteArrayEntity(payload.getBytes(StandardCharsets.UTF_8)));

    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final String logMessage = getLoggedMessage();
    assertTrue(logMessage.contains(payload));
    assertEquals(payload, EntityUtils.toString(request.getEntity(), StandardCharsets.UTF_8));
  }

  /** Test that requests without payloads still log a useful placeholder. */
  @Test
  void logsRequestsWithoutPayload() throws Exception {
    final BasicHttpRequest request = new BasicHttpRequest("GET", "/_cluster/health");
    final HttpCoreContext context = HttpCoreContext.create();
    context.setTargetHost(new HttpHost("localhost", 9201, "http"));

    new EvsOpenSearchRequestLoggingInterceptor().process(request, context);

    final String logMessage = getLoggedMessage();
    assertTrue(logMessage.contains("\n    GET http://localhost:9201/_cluster/health"));
    assertTrue(logMessage.contains("\n    <none> = <none>"));
  }
}
