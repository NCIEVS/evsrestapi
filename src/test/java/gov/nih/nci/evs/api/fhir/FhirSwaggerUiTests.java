package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

/** Integration tests for the REST and FHIR Swagger UI documentation pages. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FhirSwaggerUiTests {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void fhirOpenApiDocumentsAreAvailable() {
    assertOpenApiDocument("r4");
    assertOpenApiDocument("r5");
  }

  @Test
  void fhirSwaggerPagesContainAllDocumentationLinks() {
    assertFhirSwaggerPage("r4");
    assertFhirSwaggerPage("r5");
  }

  @Test
  void restSwaggerPageContainsFhirDocumentationLinks() {
    final ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl() + "/swagger-ui/index.html", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertThat(response.getBody())
        .contains("href=\"./index.html#\"")
        .contains("href=\"../fhir/r4/swagger-ui/\"")
        .contains("href=\"../fhir/r5/swagger-ui/\"")
        .contains("documentation-links.css");

    assertNavigationStylesheetIsAvailable(baseUrl() + "/swagger-ui/documentation-links.css");
  }

  private void assertOpenApiDocument(final String version) {
    final String fhirBaseUrl = baseUrl() + "/fhir/" + version;
    final URI uri =
        UriComponentsBuilder.fromHttpUrl(fhirBaseUrl + "/api-docs")
            .queryParam("baseUrl", fhirBaseUrl)
            .build()
            .encode()
            .toUri();
    final ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertThat(response.getBody()).contains("openapi:");
  }

  private void assertFhirSwaggerPage(final String version) {
    final String fhirBaseUrl = baseUrl() + "/fhir/" + version;
    final URI uri =
        UriComponentsBuilder.fromHttpUrl(fhirBaseUrl + "/swagger-ui/")
            .queryParam("baseUrl", fhirBaseUrl)
            .build()
            .encode()
            .toUri();
    final ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertThat(response.getBody())
        .contains("href=\"../../../swagger-ui/index.html#\"")
        .contains("href=\"../../r4/swagger-ui/\"")
        .contains("href=\"../../r5/swagger-ui/\"")
        .contains("documentation-links.css");

    assertNavigationStylesheetIsAvailable(fhirBaseUrl + "/swagger-ui/documentation-links.css");
  }

  private void assertNavigationStylesheetIsAvailable(final String stylesheetUrl) {
    final ResponseEntity<String> response = restTemplate.getForEntity(stylesheetUrl, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertThat(response.getBody()).contains(".apiDocumentationNavigation");
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }
}
