package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** Checks that the loader asks each configured API instance to clear its caches. */
class LoaderServiceImplCacheClearTest {

  private RestTemplate restTemplate;

  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
  }

  @Test
  void clearsEachConfiguredInstance() {
    server
        .expect(requestTo("http://api1:8080/api/v1/admin/cache?key=secret%26key"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());
    server
        .expect(requestTo("http://api2:8080/api/v1/admin/cache?key=secret%26key"))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());

    final int cleared =
        LoaderServiceImpl.clearApiCaches(
            " http://api1:8080/api/v1 , http://api2:8080/api/v1/ ,", "secret&key", restTemplate);

    assertEquals(2, cleared);
    server.verify();
  }

  @Test
  void failedInstanceDoesNotStopOthers() {
    server
        .expect(ExpectedCount.once(), requestTo("http://down:8080/api/v1/admin/cache?key=k"))
        .andRespond(withServerError());
    server
        .expect(ExpectedCount.once(), requestTo("http://up:8080/api/v1/admin/cache?key=k"))
        .andRespond(withNoContent());

    final int cleared =
        LoaderServiceImpl.clearApiCaches(
            "http://down:8080/api/v1,http://up:8080/api/v1", "k", restTemplate);

    assertEquals(1, cleared);
    server.verify();
  }

  @Test
  void noUrlsConfiguredDoesNothing() {
    assertEquals(0, LoaderServiceImpl.clearApiCaches(null, "k", restTemplate));
    assertEquals(0, LoaderServiceImpl.clearApiCaches("  ", "k", restTemplate));
    server.verify();
  }
}
