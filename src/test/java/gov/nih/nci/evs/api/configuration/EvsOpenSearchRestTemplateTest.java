package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.Test;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.core.rest.RestStatus;

/** Tests retries at the shared OpenSearch operations boundary. */
class EvsOpenSearchRestTemplateTest {

  private final RestHighLevelClient client = mock(RestHighLevelClient.class);
  private final List<Long> delays = new ArrayList<>();
  private final EvsOpenSearchRestTemplate template =
      new EvsOpenSearchRestTemplate(client, delays::add);

  @Test
  void retries429AndRecovers() {
    AtomicInteger calls = new AtomicInteger();

    String result =
        template.execute(
            ignored -> {
              if (calls.incrementAndGet() == 1) {
                throw new OpenSearchStatusException("rejected", RestStatus.TOO_MANY_REQUESTS);
              }
              return "ok";
            });

    assertEquals("ok", result);
    assertEquals(2, calls.get());
    assertEquals(1, delays.size());
    assertTrue(delays.get(0) >= 500 && delays.get(0) <= 625);
  }

  @Test
  void retriesNested502AndRecovers() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    ResponseException responseException = responseException(502);

    String result =
        template.execute(
            ignored -> {
              if (calls.incrementAndGet() == 1) {
                throw new RuntimeException(responseException);
              }
              return "ok";
            });

    assertEquals("ok", result);
    assertEquals(2, calls.get());
  }

  @Test
  void stopsAfterFiveAttemptsAndPreservesFailure() {
    AtomicInteger calls = new AtomicInteger();
    OpenSearchStatusException rejection =
        new OpenSearchStatusException("rejected", RestStatus.TOO_MANY_REQUESTS);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                template.execute(
                    ignored -> {
                      calls.incrementAndGet();
                      throw rejection;
                    }));

    assertEquals(5, calls.get());
    assertEquals(4, delays.size());
    assertTrue(thrown == rejection || thrown.getCause() == rejection);
  }

  @Test
  void doesNotRetryOtherStatuses() {
    AtomicInteger calls = new AtomicInteger();
    OpenSearchStatusException badRequest =
        new OpenSearchStatusException("bad request", RestStatus.BAD_REQUEST);

    assertThrows(
        RuntimeException.class,
        () ->
            template.execute(
                ignored -> {
                  calls.incrementAndGet();
                  throw badRequest;
                }));

    assertEquals(1, calls.get());
    assertTrue(delays.isEmpty());
  }

  @Test
  void preservesInterruptStatus() {
    EvsOpenSearchRestTemplate interruptedTemplate =
        new EvsOpenSearchRestTemplate(
            client,
            millis -> {
              throw new InterruptedException("stop");
            });

    try {
      IllegalStateException thrown =
          assertThrows(
              IllegalStateException.class,
              () ->
                  interruptedTemplate.execute(
                      ignored -> {
                        throw new OpenSearchStatusException(
                            "rejected", RestStatus.TOO_MANY_REQUESTS);
                      }));
      assertTrue(Thread.currentThread().isInterrupted());
      assertTrue(thrown.getCause() instanceof InterruptedException);
    } finally {
      Thread.interrupted();
    }
  }

  private static ResponseException responseException(int status) throws Exception {
    ResponseException exception = mock(ResponseException.class);
    Response response = mock(Response.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(exception.getResponse()).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(status);
    return exception;
  }
}
