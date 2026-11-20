package gov.nih.nci.evs.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercise actual HTTP requests against the SPARQL client. */
class RESTUtilsTest {
  private HttpServer server;
  private final CountDownLatch release = new CountDownLatch(1);
  private final CountDownLatch started = new CountDownLatch(1);
  private final java.util.concurrent.ExecutorService workers = Executors.newCachedThreadPool();

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(workers);
    server.createContext(
        "/waiting",
        exchange -> {
          exchange.getRequestBody().readAllBytes();
          started.countDown();
          try {
            release.await(10, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    release.countDown();
    server.stop(0);
    workers.shutdownNow();
  }

  private String url(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  @Test
  void preservesAuthenticationAndFormEncodedQuery() {
    String query = "SELECT * { ?s ?p \"café & +\" }";
    AtomicBoolean requestVerified = new AtomicBoolean();
    server.createContext(
        "/query",
        exchange -> {
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          assertEquals("query=" + query, URLDecoder.decode(body, StandardCharsets.UTF_8));
          assertEquals(
              "Basic dXNlcjpwYXNz", exchange.getRequestHeaders().getFirst("Authorization"));
          assertEquals(
              "application/sparql-results+json", exchange.getRequestHeaders().getFirst("Accept"));
          requestVerified.set(true);
          byte[] response = "{\"results\":{\"bindings\":[]}}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/sparql-results+json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    assertEquals(
        "{\"results\":{\"bindings\":[]}}",
        new RESTUtils("user", "pass", 2, 2).runSPARQL(query, url("/query")));
    assertTrue(requestVerified.get());
  }

  @Test
  void explicitQueryTimeoutStillReturnsTimeoutException() {
    assertTimeoutPreemptively(
        Duration.ofSeconds(4),
        () ->
            assertThrows(
                TimeoutException.class,
                () -> new RESTUtils("user", "pass", 60, 1).runSPARQL("query", url("/waiting"), 1)));
  }
}
