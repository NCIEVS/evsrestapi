package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Include;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests the query endpoint's request parameters. They behave the same whatever the query, so they
 * are tested once here, with a role query as the sample body, rather than in every node's tests.
 *
 * <p>Runs against the test OpenSearch with NCIt loaded.
 */
public class SearchControllerQueryParameterTests extends AbstractSearchControllerQueryTests {

  /** The concepts whose primary anatomic site is Breast, e.g. Breast Carcinoma. */
  private static final String PRIMARY_ANATOMIC_SITE_BREAST_QUERY =
      "{\"type\": \"HasRole\", \"role\": \"R101\", \"target\": \"C12971\"}";

  /** The first record of a search. */
  private static final int FIRST_RECORD = 0;

  /** A page size for these tests. */
  private static final int SMALL_PAGE_SIZE = 10;

  /** The header carrying the license key for restricted terminologies. */
  private static final String LICENSE_HEADER = "X-EVSRESTAPI-License-Key";

  /** A query that runs on any terminology. */
  private static final String CANCER_QUERY = "{\"type\": \"Contains\", \"term\": \"cancer\"}";

  // ---------------------------------------------------------------------------------------------
  // include: comma-separated Include values; the response gets every field any value turns on
  // ---------------------------------------------------------------------------------------------

  /**
   * Test include is optional: a search without it is accepted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testWithoutIncludeReturnsOk() throws Exception {
    postQueryTo(TERMINOLOGY, null, SMALL_PAGE_SIZE).andExpect(status().isOk());
  }

  /**
   * Test every include value is accepted on its own.
   *
   * @param include the include value
   */
  @ParameterizedTest
  @EnumSource(Include.class)
  public void testEveryIncludeValueReturnsOk(final Include include) {
    final ResponseEntity<String> response =
        exchangeQuery(
            PRIMARY_ANATOMIC_SITE_BREAST_QUERY,
            FIRST_RECORD,
            SMALL_PAGE_SIZE,
            include.getParameterValue());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /**
   * Test comma-separated include values return every field any of them turns on: summary's synonyms
   * and the roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCommaSeparatedIncludeReturnsEveryField() throws Exception {
    final List<Concept> concepts =
        fetchQueryResults(PRIMARY_ANATOMIC_SITE_BREAST_QUERY, "summary,roles").getConcepts();

    assertThat(concepts).isNotEmpty();
    assertThat(concepts).allMatch(concept -> !concept.getSynonyms().isEmpty());
    assertThat(concepts).allMatch(concept -> !concept.getRoles().isEmpty());
  }

  /**
   * Test include given as a repeated parameter returns the same fields as comma-separated, the form
   * generated clients send for a list parameter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRepeatedIncludeReturnsEveryField() throws Exception {
    final String content =
        mvc.perform(
                post(QUERY_URL, TERMINOLOGY)
                    .param("include", "summary", "roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(PRIMARY_ANATOMIC_SITE_BREAST_QUERY))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final List<Concept> concepts =
        ThreadLocalMapper.get().readValue(content, ConceptResultList.class).getConcepts();

    assertThat(concepts).isNotEmpty();
    assertThat(concepts).allMatch(concept -> !concept.getRoles().isEmpty());
  }

  /**
   * Test an empty include value is ignored rather than rejected.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEmptyIncludeValueIsIgnored() throws Exception {
    final List<Concept> concepts =
        fetchQueryResults(PRIMARY_ANATOMIC_SITE_BREAST_QUERY, ",roles").getConcepts();

    assertThat(concepts).allMatch(concept -> !concept.getRoles().isEmpty());
  }

  /** Test an unknown include value is rejected as a bad request. */
  @Test
  public void testUnknownIncludeReturnsBadRequest() {
    final ResponseEntity<String> response = exchangeUnknownInclude();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /**
   * Test an unknown include value gets an error naming the parameter, the value, and what's
   * allowed.
   */
  @Test
  public void testUnknownIncludeErrorNamesParameterValueAndAllowedValues() {
    final String message = getErrorMessage(exchangeUnknownInclude());

    assertThat(message)
        .contains("include")
        .contains("rols")
        .contains(Include.ROLES.getParameterValue());
  }

  /**
   * Posts a query with an unknown include value.
   *
   * @return the response
   */
  private ResponseEntity<String> exchangeUnknownInclude() {
    return exchangeQuery(PRIMARY_ANATOMIC_SITE_BREAST_QUERY, FIRST_RECORD, SMALL_PAGE_SIZE, "rols");
  }

  /**
   * Posts the cancer query to search a terminology.
   *
   * @param terminology the terminology to search
   * @param license the license key, or null to send none
   * @param pageSize the maximum number of concepts to return
   * @return the result actions
   * @throws Exception the exception
   */
  private ResultActions postQueryTo(
      final String terminology, final String license, final int pageSize) throws Exception {
    final MockHttpServletRequestBuilder request =
        post(QUERY_URL, terminology)
            .param("pageSize", String.valueOf(pageSize))
            .contentType(MediaType.APPLICATION_JSON)
            .content(CANCER_QUERY);

    final boolean hasLicense = license != null;
    if (hasLicense) {
      request.header(LICENSE_HEADER, license);
    }

    return mvc.perform(request);
  }
}
