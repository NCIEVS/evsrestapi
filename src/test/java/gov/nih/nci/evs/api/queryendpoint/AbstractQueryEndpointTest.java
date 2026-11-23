package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base class for the per-node tests of the query endpoint ({@code POST
 * /concept/{terminology}/search} with a JSON body). Each subclass tests one query node with three
 * kinds of test:
 *
 * <ol>
 *   <li><b>Integration test</b>: posts a query tree as the literal JSON a client would write, using
 *       {@link #fetchCodes(String)} or {@link #fetchQueryResults(String, String)}, and checks the
 *       concepts that come back.
 *   <li><b>Mistake test</b>: posts JSON with a mistake a client could make using {@link
 *       #postQuery(String)}, and expects a bad request.
 *   <li><b>Round-trip test</b>: calls {@link #assertRoundTripsUnchanged(String)}, which reads the
 *       JSON as an {@link EvsQuery} with the application's {@link ObjectMapper} and writes it back,
 *       and checks the result equals the original JSON.
 * </ol>
 *
 * <p>Integration tests run against the test OpenSearch with NCIt loaded, as the concept search
 * tests do. The expected concepts are from the NCIt version {@code ncit} resolves to (25.12e, the
 * latest monthly). Most tests use a small part of the hierarchy: Breast (C12971) has exactly four
 * children, Female Breast (C12851), Male Breast (C33048), Left Breast (C47855), and Right Breast
 * (C47856), and Female Breast's only parent is Breast.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractQueryEndpointTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(AbstractQueryEndpointTest.class);

  /** The query endpoint url, with the terminology as a path variable. */
  protected static final String QUERY_URL = "/api/v1/concept/{terminology}/search";

  /** The terminology every test searches. */
  protected static final String TERMINOLOGY = "ncit";

  /** A page large enough to hold every match in these tests. */
  protected static final String PAGE_SIZE = "1000";

  /** The fields returned for each concept unless a test asks for more. */
  private static final String MINIMAL_INCLUDE = "minimal";

  /** Breast, the target of most hierarchy tests. */
  protected static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  protected static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /** The four children of Breast. */
  protected static final List<String> BREAST_CHILDREN =
      List.of("C12851", "C33048", "C47855", "C47856");

  /** Female Breast, whose only parent is Breast. */
  protected static final String FEMALE_BREAST = "C12851";

  /** Left Breast, the only child of Breast with a synonym starting "left". */
  protected static final String LEFT_BREAST = "C47855";

  /** Breast Carcinoma, whose primary anatomic site is Breast. */
  protected static final String BREAST_CARCINOMA = "C4872";

  /** Melanoma, the only concept with an NCI preferred term normalized to "melanoma". */
  protected static final String MELANOMA = "C3224";

  /** The role from a disease to its primary anatomic site (Disease_Has_Primary_Anatomic_Site). */
  protected static final String PRIMARY_ANATOMIC_SITE_ROLE = "R101";

  /** The association from a concept to a subset it's in (Concept_In_Subset). */
  protected static final String IN_SUBSET_ASSOCIATION = "A8";

  /** A subset whose only member is Breast Carcinoma. */
  protected static final String BREAST_CARCINOMA_ONLY_SUBSET = "C103042";

  /** Autowired mock MVC. */
  @Autowired protected MockMvc mvc;

  /** Autowired application object mapper, the one configured by JacksonConfiguration. */
  @Autowired protected ObjectMapper objectMapper;

  /**
   * Reads query JSON as an {@link EvsQuery}, writes it back, and asserts the result equals the
   * original. The two are compared as {@link JsonNode}s, so key order and whitespace don't matter.
   *
   * @param queryJson the query tree as JSON
   * @throws Exception the exception
   */
  protected void assertRoundTripsUnchanged(final String queryJson) throws Exception {
    final JsonNode original = objectMapper.readTree(queryJson);

    final EvsQuery query = objectMapper.readValue(queryJson, EvsQuery.class);
    final JsonNode roundTripped = objectMapper.readTree(objectMapper.writeValueAsString(query));

    assertThat(roundTripped).isEqualTo(original);
  }

  /**
   * Asserts the app's object mapper writes the query as the expected JSON, compared as JSON trees
   * so key order and whitespace don't matter. A regression check on the wire format: a renamed
   * class, property, or enum constant changes the JSON clients send and receive.
   *
   * @param query the query, built in Java
   * @param expectedJson the JSON it must be written as
   * @throws Exception the exception
   */
  protected void assertSerializesTo(final EvsQuery query, final String expectedJson)
      throws Exception {
    final JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(query));

    assertThat(serialized).isEqualTo(objectMapper.readTree(expectedJson));
  }

  /**
   * Posts a query tree and returns the codes of the concepts it matches, in the order returned.
   *
   * @param queryJson the query tree as JSON
   * @return the codes
   * @throws Exception the exception
   */
  protected List<String> fetchCodes(final String queryJson) throws Exception {
    return fetchQueryResults(queryJson, MINIMAL_INCLUDE).getConcepts().stream()
        .map(Concept::getCode)
        .toList();
  }

  /**
   * Posts a query tree, asserts the response is OK, and reads the concepts it returns.
   *
   * @param queryJson the query tree as JSON
   * @param include the fields to return for each concept
   * @return the result list
   * @throws Exception the exception
   */
  protected ConceptResultList fetchQueryResults(final String queryJson, final String include)
      throws Exception {
    final MvcResult result =
        postQuery(queryJson, PAGE_SIZE, include).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    return ThreadLocalMapper.get().readValue(content, ConceptResultList.class);
  }

  /**
   * Posts a query tree with the default page size, returning minimal concepts. Mistake tests use
   * this and expect a bad request.
   *
   * @param queryJson the query tree as JSON
   * @return the result actions
   * @throws Exception the exception
   */
  protected ResultActions postQuery(final String queryJson) throws Exception {
    return postQuery(queryJson, PAGE_SIZE, MINIMAL_INCLUDE);
  }

  /**
   * Posts a query tree to the query endpoint, searching NCIt.
   *
   * @param queryJson the query tree as JSON
   * @param pageSize the maximum number of concepts to return
   * @param include the fields to return for each concept
   * @return the result actions
   * @throws Exception the exception
   */
  protected ResultActions postQuery(
      final String queryJson, final String pageSize, final String include) throws Exception {
    log.info(
        "Testing url - " + QUERY_URL + " (terminology=" + TERMINOLOGY + "), body = " + queryJson);
    return mvc.perform(
        post(QUERY_URL, TERMINOLOGY)
            .param("pageSize", pageSize)
            .param("include", include)
            .contentType(MediaType.APPLICATION_JSON)
            .content(queryJson));
  }

  /**
   * Indicates whether a concept's primary anatomic site is Breast.
   *
   * @param concept the concept, with its roles
   * @return whether it has the role to Breast
   */
  protected static boolean getHasPrimaryAnatomicSiteBreast(final Concept concept) {
    return concept.getRoles().stream()
        .anyMatch(
            (Role role) ->
                PRIMARY_ANATOMIC_SITE_ROLE.equals(role.getCode())
                    && BREAST.equals(role.getRelatedCode()));
  }

  /**
   * Returns the codes with Breast added.
   *
   * @param codes the codes
   * @return the codes and Breast
   */
  protected static List<String> getWithBreast(final List<String> codes) {
    return Stream.concat(codes.stream(), Stream.of(BREAST)).toList();
  }
}
