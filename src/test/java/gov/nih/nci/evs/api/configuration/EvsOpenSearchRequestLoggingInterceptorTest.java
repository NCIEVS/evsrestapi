package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.LoggerFactory;

/** Unit tests for {@link EvsOpenSearchRequestLoggingInterceptor}. */
@ResourceLock("EvsOpenSearchRestTemplateLogger")
class EvsOpenSearchRequestLoggingInterceptorTest {

  private Logger logger;
  private Level originalLevel;
  private boolean originalAdditive;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setup() {
    logger = (Logger) LoggerFactory.getLogger(EvsOpenSearchRestTemplate.class);
    originalLevel = logger.getLevel();
    originalAdditive = logger.isAdditive();
    logger.setLevel(Level.DEBUG);
    logger.setAdditive(false);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void cleanup() {
    logger.detachAppender(appender);
    appender.stop();
    logger.setLevel(originalLevel);
    logger.setAdditive(originalAdditive);
  }

  private String process(final String method, final String uri) throws Exception {
    new EvsOpenSearchRequestLoggingInterceptor()
        .process(new BasicHttpRequest(method, uri), null, HttpCoreContext.create());
    assertEquals(1, appender.list.size());
    return appender.list.getFirst().getFormattedMessage();
  }

  @Test
  void logsSearchRequestDetails() throws Exception {
    final String message =
        process("POST", "/my-index-000001/_search?typed_keys=true&search_type=query_then_fetch");

    assertTrue(message.contains("POST /my-index-000001/_search?typed_keys=true&search_type=query_then_fetch"));
    assertTrue(message.contains("index=my-index-000001"));
    assertTrue(message.contains("contentType=<none>"));
    assertTrue(message.contains("payload=<unavailable from the HTTP Components 5 async interceptor>"));
  }

  @Test
  void logsAnAbsoluteRequestUri() throws Exception {
    final String message = process("GET", "http://localhost:9201/_cluster/health");

    assertTrue(message.contains("GET http://localhost:9201/_cluster/health"));
    assertTrue(message.contains("index=_cluster"));
  }
}