package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opensearch.client.Request;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.mock.env.MockEnvironment;

/** Verifies legacy parameter translation and the configured client's actual HTTP requests. */
class OpenSearchLegacyCompatibilityInterceptorTest {

  /**
   * Rewrite query parameter names without decoding or changing other URI components.
   *
   * @param path the original path
   * @param expected the translated path
   */
  @ParameterizedTest
  @CsvSource({
    "/index?cluster_manager_timeout=30s, /index?master_timeout=30s",
    "/index?a=1&cluster_manager_timeout=1m&b=2, /index?a=1&master_timeout=1m&b=2",
    "/index?cluster_manager_timeout=30s&cluster_manager_timeout=1m,"
        + " /index?master_timeout=30s&master_timeout=1m",
    "/cluster_manager_timeout=30s/_search, /cluster_manager_timeout=30s/_search",
    "/index?other_cluster_manager_timeout=30s, /index?other_cluster_manager_timeout=30s",
    "/index?value=cluster_manager_timeout=30s, /index?value=cluster_manager_timeout=30s",
    "/index?value=a%26cluster_manager_timeout%3D30s,"
        + " /index?value=a%26cluster_manager_timeout%3D30s",
    "/index?master_timeout=30s, /index?master_timeout=30s",
    "/_cluster/health, /_cluster/health"
  })
  void rewritesOnlyParameterNames(final String path, final String expected) {
    final BasicHttpRequest request = new BasicHttpRequest("GET", path);
    new OpenSearchLegacyCompatibilityInterceptor().process(request, null, HttpCoreContext.create());
    assertEquals(expected, request.getPath());
  }

  /**
   * The configured client must always translate the parameter on the wire.
   *
   * @throws Exception if the request fails
   */
  @Test
  void configuredClientAlwaysRewritesLegacyParameter() throws Exception {
    final AtomicReference<String> query = new AtomicReference<>();
    final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          query.set(exchange.getRequestURI().getRawQuery());
          final byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      final OpensearchConfiguration configuration = new OpensearchConfiguration();
      final MockEnvironment environment =
          new MockEnvironment()
              .withProperty("nci.evs.opensearch.server.host", "127.0.0.1")
              .withProperty("nci.evs.opensearch.server.port", "" + server.getAddress().getPort())
              .withProperty("nci.evs.opensearch.server.scheme", "http")
              .withProperty("nci.evs.opensearch.timeout", "5000");
      configuration.env = environment;
      try (RestHighLevelClient client = configuration.client()) {
        final Request request = new Request("GET", "/_cluster/health");
        request.addParameter("cluster_manager_timeout", "30s");
        client.getLowLevelClient().performRequest(request);
        assertEquals("master_timeout=30s", query.get());
      }
    } finally {
      server.stop(0);
    }
  }
}
