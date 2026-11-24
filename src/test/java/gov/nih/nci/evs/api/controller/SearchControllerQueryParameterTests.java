package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.query.EvsQuery;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests the query endpoint's request parameters. They behave the same whatever the query, so they
 * are tested once here, with a term search as the sample body, rather than in every node's tests.
 *
 * <p>Runs against the test OpenSearch with NCIt loaded.
 */
public class SearchControllerQueryParameterTests extends AbstractSearchControllerQueryTests {

  /** A page size for these tests. */
  private static final int SMALL_PAGE_SIZE = 10;

  /** NCIt named with a loaded version, as clients may name a terminology. */
  private static final String TERMINOLOGY_WITH_VERSION = "ncit_25.12e";

  /** A terminology that isn't loaded. */
  private static final String UNKNOWN_TERMINOLOGY = "notaterminology";

  /** A license-restricted terminology: MedDRA, which needs the license key header. */
  private static final String LICENSED_TERMINOLOGY = "mdr";

  /** The header carrying the license key for restricted terminologies. */
  private static final String LICENSE_HEADER = "X-EVSRESTAPI-License-Key";

  /** A query that runs on any terminology. */
  private static final String CANCER_QUERY = "{\"type\": \"Contains\", \"term\": \"cancer\"}";

  /**
   * A sample body for every node type, each using every field the node declares, on evstest. Every
   * node type must have one: {@link #testEveryNodeTypeHasSampleBody} fails when a node is added
   * without a sample.
   */
  private static final Map<String, String> SAMPLE_BODY_BY_NODE_TYPE = getSampleBodyByNodeType();

  /** The application properties, for the UI license key the tests use. */
  @Autowired private ApplicationProperties appProperties;

  // ---------------------------------------------------------------------------------------------
  // terminology: a loaded terminology, by name or name and version; else 404
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a terminology named without a version is accepted: its latest loaded version is searched.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminologyByNameReturnsOk() throws Exception {
    postQueryTo(TERMINOLOGY, null, SMALL_PAGE_SIZE).andExpect(status().isOk());
  }

  /**
   * Test a terminology named with its version is accepted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminologyByNameAndVersionReturnsOk() throws Exception {
    postQueryTo(TERMINOLOGY_WITH_VERSION, null, SMALL_PAGE_SIZE).andExpect(status().isOk());
  }

  /**
   * Test a search without a terminology in the path is rejected: the path no longer matches the
   * query endpoint, and only GET is mapped to it. An empty terminology ("/concept//search") gets
   * the same response, since the server collapses the empty segment.
   *
   * @throws Exception the exception
   */
  @Test
  public void testWithoutTerminologyReturnsMethodNotAllowed() throws Exception {
    final String urlWithoutTerminology = "/api/v1/concept/search";

    mvc.perform(
            post(urlWithoutTerminology)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CANCER_QUERY))
        .andExpect(status().isMethodNotAllowed());
  }

  /**
   * Test a blank terminology is rejected as not found, like any terminology that isn't loaded.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBlankTerminologyReturnsNotFound() throws Exception {
    final String blankTerminology = " ";

    postQueryTo(blankTerminology, null, SMALL_PAGE_SIZE).andExpect(status().isNotFound());
  }

  /**
   * Test a terminology that isn't loaded is rejected as not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnknownTerminologyReturnsNotFound() throws Exception {
    postQueryTo(UNKNOWN_TERMINOLOGY, null, SMALL_PAGE_SIZE).andExpect(status().isNotFound());
  }

  /**
   * Test a terminology that isn't loaded gets an error naming it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnknownTerminologyErrorNamesTerminology() throws Exception {
    postQueryTo(UNKNOWN_TERMINOLOGY, null, SMALL_PAGE_SIZE)
        .andExpect(status().reason(containsString(UNKNOWN_TERMINOLOGY)));
  }

  // ---------------------------------------------------------------------------------------------
  // License key: optional; required for license-restricted terminologies, else 403
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a terminology that isn't license-restricted is searched without the license key.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnlicensedTerminologyWithoutLicenseKeyReturnsOk() throws Exception {
    postQueryTo(TERMINOLOGY, null, SMALL_PAGE_SIZE).andExpect(status().isOk());
  }

  /**
   * Test a terminology that isn't license-restricted ignores a license key sent with it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnlicensedTerminologyWithLicenseKeyReturnsOk() throws Exception {
    postQueryTo(TERMINOLOGY, appProperties.getUiLicense(), SMALL_PAGE_SIZE)
        .andExpect(status().isOk());
  }

  /**
   * Test a license-restricted terminology without the license key is forbidden.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyWithoutLicenseKeyReturnsForbidden() throws Exception {
    postQueryTo(LICENSED_TERMINOLOGY, null, SMALL_PAGE_SIZE).andExpect(status().isForbidden());
  }

  /**
   * Test a license-restricted terminology without the license key gets an error naming the header
   * it needs.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyWithoutLicenseKeyErrorNamesHeader() throws Exception {
    postQueryTo(LICENSED_TERMINOLOGY, null, SMALL_PAGE_SIZE)
        .andExpect(status().reason(containsString(LICENSE_HEADER)));
  }

  /**
   * Test a license-restricted terminology with the license key is searched.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyWithLicenseKeyReturnsOk() throws Exception {
    postQueryTo(LICENSED_TERMINOLOGY, appProperties.getUiLicense(), SMALL_PAGE_SIZE)
        .andExpect(status().isOk());
  }

  // ---------------------------------------------------------------------------------------------
  // pageSize on a license-restricted terminology: at most MAX_LICENSED_PAGE_SIZE, else 400.
  // A query search has no search term, so the bulk limit always applies.
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a license-restricted terminology accepts a page exactly at the bulk limit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyPageSizeAtLimitReturnsOk() throws Exception {
    postQueryTo(
            LICENSED_TERMINOLOGY,
            appProperties.getUiLicense(),
            SearchCriteriaWithoutTerminology.MAX_LICENSED_PAGE_SIZE)
        .andExpect(status().isOk());
  }

  /**
   * Test a license-restricted terminology accepts an ordinary page below the bulk limit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyPageSizeBelowLimitReturnsOk() throws Exception {
    final int typicalLicensedPageSize = 5;

    postQueryTo(LICENSED_TERMINOLOGY, appProperties.getUiLicense(), typicalLicensedPageSize)
        .andExpect(status().isOk());
  }

  /**
   * Test a license-restricted terminology rejects a page one past the bulk limit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyPageSizeOverLimitReturnsBadRequest() throws Exception {
    postLicensedPageOverLimit().andExpect(status().isBadRequest());
  }

  /**
   * Test a license-restricted terminology's page past the bulk limit gets an error naming the
   * limit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLicensedTerminologyPageSizeOverLimitErrorNamesLimit() throws Exception {
    postLicensedPageOverLimit()
        .andExpect(
            status()
                .reason(
                    containsString(
                        "limited to working on "
                            + SearchCriteriaWithoutTerminology.MAX_LICENSED_PAGE_SIZE)));
  }

  // ---------------------------------------------------------------------------------------------
  // body: required; every node type, and every field a node declares, is accepted
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a search without a body is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testWithoutBodyReturnsBadRequest() throws Exception {
    mvc.perform(post(QUERY_URL, TERMINOLOGY).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test And with a misspelled children property (child, not children) is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAndWithMisspelledChildrenPropertyReturnsBadRequest() throws Exception {
    final String misspelledChildrenQuery =
        """
        {"type": "And", "child": [
          {"type": "HasRole", "role": "R101"}]}
        """;

    postQuery(misspelledChildrenQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test Not with a child given as a type name, not a query, is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotWithChildGivenAsTypeNameReturnsBadRequest() throws Exception {
    final String childAsTypeNameQuery =
        """
        {"type": "Not", "child": "HasRole"}
        """;

    postQuery(childAsTypeNameQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test InSubset with a constructor parameter name (subsetCode) instead of a property is rejected
   * as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInSubsetWithConstructorParameterNameReturnsBadRequest() throws Exception {
    final String subsetCodeQuery =
        """
        {"type": "InSubset", "subsetCode": "C103042"}
        """;

    postQuery(subsetCodeQuery).andExpect(status().isBadRequest());
  }

  /** Test every node type the body accepts has a sample body here, so none goes untested. */
  @Test
  public void testEveryNodeTypeHasSampleBody() {
    final List<String> nodeTypes =
        Arrays.stream(EvsQuery.class.getAnnotation(JsonSubTypes.class).value())
            .map(JsonSubTypes.Type::name)
            .toList();

    assertThat(SAMPLE_BODY_BY_NODE_TYPE.keySet()).containsExactlyInAnyOrderElementsOf(nodeTypes);
  }

  /**
   * Test every node type, with every field it declares, is accepted on evstest.
   *
   * @param nodeType the node type, for the test name
   * @param sampleBody the sample body
   * @throws Exception the exception
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("getSampleBodies")
  public void testEveryNodeTypeWithEveryFieldReturnsOk(
      final String nodeType, final String sampleBody) throws Exception {
    // fetchQueryResults asserts the response is OK
    fetchQueryResults(EVSTEST, sampleBody, "minimal");
  }

  /**
   * Returns the sample bodies, one argument pair per node type.
   *
   * @return the node types and their sample bodies
   */
  private static Stream<Arguments> getSampleBodies() {
    return SAMPLE_BODY_BY_NODE_TYPE.entrySet().stream()
        .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
  }

  /**
   * Returns a sample body for every node type, each using every field the node declares, with
   * evstest codes.
   *
   * @return the sample bodies by node type
   */
  private static Map<String, String> getSampleBodyByNodeType() {
    final Map<String, String> sampleBodyByNodeType = new LinkedHashMap<>();
    final String leaf = "{\"type\": \"Exact\", \"term\": \"FK100\"}";

    sampleBodyByNodeType.put("And", "{\"type\": \"And\", \"children\": [" + leaf + "]}");
    sampleBodyByNodeType.put("Or", "{\"type\": \"Or\", \"children\": [" + leaf + "]}");
    sampleBodyByNodeType.put("Not", "{\"type\": \"Not\", \"child\": " + leaf + "}");
    for (final String termSearch : List.of("Exact", "StartsWith", "Contains", "Fuzzy")) {
      sampleBodyByNodeType.put(
          termSearch, "{\"type\": \"" + termSearch + "\", \"term\": \"melanoma\"}");
    }
    for (final String hierarchyNode : List.of("HasAncestor", "HasDescendant")) {
      sampleBodyByNodeType.put(
          hierarchyNode,
          "{\"type\": \"" + hierarchyNode + "\", \"target\": \"FK100\", \"maxLevel\": 1}");
    }
    for (final String roleNode : List.of("HasRole", "HasInverseRole")) {
      sampleBodyByNodeType.put(
          roleNode, "{\"type\": \"" + roleNode + "\", \"role\": \"R101\", \"target\": \"FK21\"}");
    }
    for (final String associationNode : List.of("HasAssociation", "HasInverseAssociation")) {
      sampleBodyByNodeType.put(
          associationNode,
          "{\"type\": \"" + associationNode + "\", \"association\": \"A8\", \"target\": \"FK31\"}");
    }
    sampleBodyByNodeType.put(
        "HasSynonym",
        "{\"type\": \"HasSynonym\", \"target\": \"Melanoma\", \"source\": \"NCI\","
            + " \"termType\": \"PT\", \"subSource\": \"NCI\"}");
    sampleBodyByNodeType.put("InSubset", "{\"type\": \"InSubset\", \"target\": \"FK31\"}");
    // Also uses every qualifier condition type
    sampleBodyByNodeType.put(
        "HasProperty",
        "{\"type\": \"HasProperty\", \"property\": \"P211\", \"target\": \"DNA repair\","
            + " \"qualifiers\": {\"type\": \"AllQualifiers\", \"conditions\": [{\"type\":"
            + " \"PropertyQualifier\", \"qualifier\": \"P387\", \"target\": \"GO:0006281\"},"
            + " {\"type\": \"NoQualifiers\", \"condition\": {\"type\": \"AnyQualifiers\","
            + " \"conditions\": [{\"type\": \"PropertyQualifier\", \"qualifier\": \"P389\","
            + " \"target\": \"NR\"}]}}]}}");
    return sampleBodyByNodeType;
  }

  /**
   * Posts a page of a license-restricted terminology, with the license key, one past the bulk
   * limit.
   *
   * @return the result actions
   * @throws Exception the exception
   */
  private ResultActions postLicensedPageOverLimit() throws Exception {
    return postQueryTo(
        LICENSED_TERMINOLOGY,
        appProperties.getUiLicense(),
        SearchCriteriaWithoutTerminology.MAX_LICENSED_PAGE_SIZE + 1);
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
