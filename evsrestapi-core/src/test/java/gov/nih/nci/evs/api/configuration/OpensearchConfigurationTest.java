package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.Request;
import org.springframework.mock.env.MockEnvironment;

/** Exercises the actual OpenSearch HTTP client against response header limits. */
class OpensearchConfigurationTest {
  @Test
  void acceptsTaskResourceHeadersFromLargeDeleteResponses() throws Exception {
    requestWithHeaderLimit(null);
  }

  @Test
  void acceptsConfiguredHeaderLimit() throws Exception {
    requestWithHeaderLimit(512);
  }

  @Test
  void stillEnforcesConfiguredHeaderLimit() {
    IOException exception = assertThrows(IOException.class, () -> requestWithHeaderLimit(128));
    assertTrue(exception.getMessage().contains("Maximum header count exceeded"));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsUnboundedHeaderLimits(int limit) {
    assertThrows(IllegalArgumentException.class, () -> requestWithHeaderLimit(limit));
  }

  private void requestWithHeaderLimit(Integer limit) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            for (int i = 0; i < 256; i++) {
              exchange.getResponseHeaders().add("TASK_RESOURCE_USAGE", "{\"taskId\":" + i + "}");
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          }
        });
    server.start();
    try {
      OpensearchConfiguration configuration = new OpensearchConfiguration();
      MockEnvironment environment =
          new MockEnvironment()
              .withProperty("nci.evs.opensearch.server.host", "127.0.0.1")
              .withProperty(
                  "nci.evs.opensearch.server.port", String.valueOf(server.getAddress().getPort()))
              .withProperty("nci.evs.opensearch.server.scheme", "http")
              .withProperty("nci.evs.opensearch.timeout", "5000");
      if (limit != null) {
        environment.withProperty("nci.evs.opensearch.max-response-header-count", limit.toString());
      }
      configuration.env = environment;
      try (var client = configuration.client()) {
        var response = client.getLowLevelClient().performRequest(new Request("GET", "/"));
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertTrue(
            Arrays.stream(response.getHeaders())
                    .filter(header -> "TASK_RESOURCE_USAGE".equalsIgnoreCase(header.getName()))
                    .count()
                > 100);
      }
    } finally {
      server.stop(0);
    }
  }
}
