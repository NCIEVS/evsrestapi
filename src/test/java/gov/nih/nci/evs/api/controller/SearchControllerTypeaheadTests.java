package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.Typeahead;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests typeahead, which completes an incomplete term to the best matching concepts, through GET
 * /concept/typeahead and through the Typeahead node of the query endpoint.
 */
public class SearchControllerTypeaheadTests extends AbstractSearchControllerQueryTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(SearchControllerTypeaheadTests.class);

  /** The application properties. */
  @Autowired ApplicationProperties appProperties;

  /** The Opensearch operations service instance *. */
  @Autowired OpensearchOperationsService opensearchOperationsService;

  /** The Opensearch wuery service instance *. */
  @Autowired OpensearchQueryService opensearchQueryService;

  /** The terminology utils. */
  @Autowired TerminologyUtils terminologyUtils;

  /** The search controller. */
  @Autowired SearchController searchController;

  /** The most matches typeahead counts before its total stops at this number. */
  private static final long TYPEAHEAD_COUNTED_HIT_LIMIT = 10_000L;

  /** The typeahead endpoint url. */
  private static final String TYPEAHEAD_URL = "/api/v1/concept/typeahead";

  /** The number of suggestions typeahead returns when no page size is requested. */
  private static final int TYPEAHEAD_DEFAULT_PAGE_SIZE = 10;

  /** The smallest page size typeahead accepts. */
  private static final int TYPEAHEAD_MIN_PAGE_SIZE = 1;

  /** The largest page size typeahead accepts. */
  private static final int TYPEAHEAD_MAX_PAGE_SIZE = 1000;

  /** The typeahead for the term "melanoma". */
  private static final String MELANOMA_TYPEAHEAD_QUERY =
      """
      {"type": "Typeahead", "term": "melanoma"}
      """;

  /**
   * Test Typeahead returns Melanoma first for "melanoma".
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadReturnsBestMatchFirst() throws Exception {
    assertThat(fetchCodes(MELANOMA_TYPEAHEAD_QUERY)).first().isEqualTo(MELANOMA);
  }

  /**
   * Test Typeahead with a misspelled property (incompleteTerm, not term) is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWithMisspelledPropertyReturnsBadRequest() throws Exception {
    final String incompleteTermInsteadOfTermQuery =
        """
        {"type": "Typeahead", "incompleteTerm": "melanoma"}
        """;

    postQuery(incompleteTermInsteadOfTermQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test typeahead with no terminology falls back on ncit and returns only ncit concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadMissingTerminologyDefaultsToNcit() throws Exception {
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = cancer, pageSize = " + pageSize);
    final ConceptResultList list =
        searchController.typeaheadService("cancer", null, null, pageSize, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> "ncit".equals(c.getTerminology()));
  }

  /**
   * Test the typeahead endpoint rejects an unknown terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadBadTerminologyReturnsNotFound() throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=ncitXXX&term=melan");
    mvc.perform(get(TYPEAHEAD_URL).param("terminology", "ncitXXX").param("term", "melan"))
        .andExpect(status().isNotFound());
  }

  /**
   * Test typeahead rejects an include value it doesn't recognize.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadBadIncludeThrowsError() throws Exception {
    final String badInclude = "minimal,bogus";
    log.info("Testing typeahead - incompleteTerm = melan, include = " + badInclude);
    final ResponseStatusException e =
        assertThrows(
            ResponseStatusException.class,
            () -> searchController.typeaheadService("melan", null, null, null, badInclude));
    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /**
   * Test typeahead with no include echoes the minimal include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadMissingIncludeDefaultsToMinimal() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan, include = null");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getParameters().getInclude()).isEqualTo("minimal");
  }

  /**
   * Test typeahead with no page size returns the default number of suggestions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadMissingPageSizeDefaultsToTen() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan, pageSize = null");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getConcepts()).hasSize(TYPEAHEAD_DEFAULT_PAGE_SIZE);
  }

  /**
   * Test typeahead returns the requested number of suggestions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadExplicitPageSizeReturnsRequestedPageSize() throws Exception {
    final int requestedPageSize = 3;
    log.info("Testing typeahead - incompleteTerm = melan, pageSize = " + requestedPageSize);
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, requestedPageSize, null);
    assertThat(list.getConcepts()).hasSize(requestedPageSize);
  }

  /**
   * Test typeahead accepts the smallest allowed page size.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadSmallestPageSizeReturnsOneConcept() throws Exception {
    log.info("Testing typeahead - incompleteTerm = c, pageSize = " + TYPEAHEAD_MIN_PAGE_SIZE);
    final ConceptResultList list =
        searchController.typeaheadService("c", null, null, TYPEAHEAD_MIN_PAGE_SIZE, null);
    assertThat(list.getConcepts()).hasSize(TYPEAHEAD_MIN_PAGE_SIZE);
  }

  /**
   * Test typeahead accepts the largest allowed page size. "c" matches far more than 1000 names.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadLargestPageSizeReturnsThousandConcepts() throws Exception {
    log.info("Testing typeahead - incompleteTerm = c, pageSize = " + TYPEAHEAD_MAX_PAGE_SIZE);
    final ConceptResultList list =
        searchController.typeaheadService("c", null, null, TYPEAHEAD_MAX_PAGE_SIZE, null);
    assertThat(list.getConcepts()).hasSize(TYPEAHEAD_MAX_PAGE_SIZE);
  }

  /**
   * Test typeahead rejects a page size above the largest allowed.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadPageSizeTooBigThrowsError() throws Exception {
    final int tooBigPageSize = TYPEAHEAD_MAX_PAGE_SIZE + 1;
    log.info("Testing typeahead - incompleteTerm = melan, pageSize = " + tooBigPageSize);
    final ResponseStatusException e =
        assertThrows(
            ResponseStatusException.class,
            () -> searchController.typeaheadService("melan", null, null, tooBigPageSize, null));
    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /**
   * Test typeahead rejects a page size below the smallest allowed.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadPageSizeTooSmallThrowsError() throws Exception {
    final int tooSmallPageSize = TYPEAHEAD_MIN_PAGE_SIZE - 1;
    log.info("Testing typeahead - incompleteTerm = melan, pageSize = " + tooSmallPageSize);
    final ResponseStatusException e =
        assertThrows(
            ResponseStatusException.class,
            () -> searchController.typeaheadService("melan", null, null, tooSmallPageSize, null));
    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /**
   * Test typeahead returns an empty result for an incomplete term without words: null, blank, or
   * only punctuation.
   *
   * @param incompleteTerm the term without words
   * @throws Exception the exception
   */
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "---", " , . "})
  public void testTypeaheadTermWithoutWordsReturnsEmptyList(final String incompleteTerm)
      throws Exception {
    log.info("Testing typeahead - incompleteTerm = [" + incompleteTerm + "]");
    final ConceptResultList list =
        searchController.typeaheadService(incompleteTerm, null, null, null, null);
    assertThat(list.getConcepts()).isEmpty();
    assertThat(list.getTotal()).isEqualTo(0L);
  }

  /**
   * Test typeahead finds concepts for a partially typed term.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadIncompleteTermReturnsMatches() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getTotal()).isGreaterThan(0L);
  }

  /**
   * Test typeahead echoes the partially typed term in the response parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadIncompleteTermEchoesTerm() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getParameters().getTerm()).isEqualTo("melan");
  }

  /**
   * Test typeahead reports its search type as startsWith in the response parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadIncompleteTermEchoesStartsWithType() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getParameters().getType()).isEqualTo("startsWith");
  }

  /**
   * Test typeahead ranks Chronic Granulomatous Disease first for "chronic granu".
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadIncompleteTermRanksChronicGranulomatousDiseaseFirst() throws Exception {
    log.info("Testing typeahead - incompleteTerm = chronic granu");
    final ConceptResultList list =
        searchController.typeaheadService("chronic granu", null, null, null, null);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Chronic Granulomatous Disease");
  }

  /**
   * Test typeahead puts general concepts with short names first.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadShortGeneralNamesRankFirst() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getConcepts())
        .extracting(Concept::getName)
        .startsWith("Melanoma", "Melanocytic Neoplasm");
  }

  /**
   * Test typeahead suggests the expected concept within the first five results for terms users have
   * asked about.
   *
   * @param incompleteTerm the partially typed term
   * @param expectedName the preferred name expected in the first five results
   * @throws Exception the exception
   */
  @ParameterizedTest
  @CsvSource({
    "chronic granu, Chronic Granulomatous Disease",
    "chronic granul, Chronic Granulomatous Disease",
    "giant subep, Subependymal Giant Cell Astrocytoma",
    "giant astro, Subependymal Giant Cell Astrocytoma",
    "lymphoblastic, Acute Lymphoblastic Leukemia"
  })
  public void testTypeaheadReportedTermsSuggestExpectedConceptInTopFive(
      final String incompleteTerm, final String expectedName) throws Exception {
    final int topResultCount = 5;
    log.info("Testing typeahead - incompleteTerm = " + incompleteTerm);
    final ConceptResultList list =
        searchController.typeaheadService(incompleteTerm, null, null, topResultCount, null);
    assertThat(list.getConcepts()).extracting(Concept::getName).contains(expectedName);
  }

  /**
   * Test typeahead puts the more general concept first when two names match equally well.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEqualMatchesRankMoreGeneralConceptFirst() throws Exception {
    log.info("Testing typeahead - incompleteTerm = lung ca");
    final ConceptResultList list =
        searchController.typeaheadService("lung ca", null, null, null, null);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Lung Carcinoma");
  }

  /**
   * Test the typeahead endpoint accepts a comma-separated list of terminologies and echoes each one
   * in the response parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadMultipleTerminologiesEchoesEachTerminology() throws Exception {
    final String requestedTerminologies = "ncit,ncim";
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=" + requestedTerminologies);
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL).param("terminology", requestedTerminologies).param("term", "melan"));

    // Only the echoed terminologies are checked: ncim may not have the typeahead field indexed
    assertThat(list.getParameters().getTerminology()).containsExactlyInAnyOrder("ncit", "ncim");
  }

  /**
   * Test the typeahead endpoint echoes an explicit single terminology in the response parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadExplicitTerminologyEchoesTerminology() throws Exception {
    final String requestedTerminology = "ncit";
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=" + requestedTerminology);
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL).param("terminology", requestedTerminology).param("term", "melan"));
    assertThat(list.getParameters().getTerminology()).containsExactly(requestedTerminology);
  }

  /**
   * Test the typeahead endpoint with an explicit single terminology returns only that terminology's
   * concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadExplicitTerminologyReturnsOnlyThatTerminology() throws Exception {
    final String requestedTerminology = "ncit";
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=" + requestedTerminology);
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL).param("terminology", requestedTerminology).param("term", "melan"));
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> requestedTerminology.equals(c.getTerminology()));
  }

  /**
   * Test the typeahead endpoint rejects a license-restricted terminology without a license key.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadLicenseRestrictedTerminologyWithoutLicenseReturnsForbidden()
      throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=mdr&term=canc");
    mvc.perform(get(TYPEAHEAD_URL).param("terminology", "mdr").param("term", "canc"))
        .andExpect(status().isForbidden());
  }

  /**
   * Test the typeahead endpoint accepts a license-restricted terminology with a license key.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadLicenseRestrictedTerminologyWithLicenseReturnsOk() throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?terminology=mdr&term=canc (with license)");
    mvc.perform(
            get(TYPEAHEAD_URL)
                .header("X-EVSRESTAPI-License-Key", appProperties.getUiLicense())
                .param("terminology", "mdr")
                .param("term", "canc"))
        .andExpect(status().isOk());
  }

  /**
   * Test typeahead across ncit and ncim finds every match from both. A failure ranking one
   * terminology's matches would drop them without failing the request, so the totals are compared.
   * Skipped until ncim is reindexed with the typeahead field.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadMultipleTerminologiesFindsMatchesFromEach() throws Exception {
    assumeTrue(getHasTypeaheadField("ncim"), "ncim is not indexed with the typeahead field");
    final String incompleteTerm = "melan";

    final ConceptResultList ncitList =
        searchController.typeaheadService(incompleteTerm, List.of("ncit"), null, null, null);
    final ConceptResultList ncimList =
        searchController.typeaheadService(incompleteTerm, List.of("ncim"), null, null, null);
    final ConceptResultList bothList =
        searchController.typeaheadService(
            incompleteTerm, List.of("ncit", "ncim"), null, null, null);

    // Without ncim matches, the totals would agree even if ncim were dropped
    assertThat(ncimList.getTotal()).isGreaterThan(0L);
    assertThat(bothList.getTotal()).isEqualTo(ncitList.getTotal() + ncimList.getTotal());
  }

  /**
   * Test typeahead returns an empty result, not an error, when nothing matches.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadNoMatchesReturnsEmptyList() throws Exception {
    log.info("Testing typeahead - incompleteTerm = xyzzq");
    final ConceptResultList list =
        searchController.typeaheadService("xyzzq", null, null, null, null);
    assertThat(list.getConcepts()).isEmpty();
    assertThat(list.getTotal()).isEqualTo(0L);
  }

  /**
   * Test typeahead's total is the exact number of matches while there are fewer than 10,000.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadTotalIsExactBelowCountedHitLimit() throws Exception {
    // "car" starts a word in a few thousand ncit names, and no ncit code
    final String incompleteTerm = "car";
    log.info("Testing typeahead - incompleteTerm = " + incompleteTerm);
    final ConceptResultList list =
        searchController.typeaheadService(incompleteTerm, null, null, null, null);

    final long matchCount = fetchTypeaheadMatchCount(incompleteTerm);
    assertThat(matchCount).isLessThan(TYPEAHEAD_COUNTED_HIT_LIMIT);
    assertThat(list.getTotal()).isEqualTo(matchCount);
  }

  /**
   * Test typeahead's total stops at 10,000, OpenSearch's default limit of counted hits. This is
   * expected: typeahead doesn't page, so unlike search it doesn't run a second query for the exact
   * count. If typeahead starts reporting exact totals, this test should change with it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadTotalCapsAtCountedHitLimit() throws Exception {
    // "c" starts a word in tens of thousands of ncit names
    final String incompleteTerm = "c";
    log.info("Testing typeahead - incompleteTerm = " + incompleteTerm);
    final ConceptResultList list =
        searchController.typeaheadService(incompleteTerm, null, null, null, null);

    final long matchCount = fetchTypeaheadMatchCount(incompleteTerm);
    assertThat(matchCount).isGreaterThan(TYPEAHEAD_COUNTED_HIT_LIMIT);
    assertThat(list.getTotal()).isEqualTo(TYPEAHEAD_COUNTED_HIT_LIMIT);
  }

  /**
   * Counts every ncit concept a typeahead matches, with no limit.
   *
   * @param incompleteTerm the partially typed term
   * @return the number of matching concepts
   * @throws Exception the exception
   */
  private long fetchTypeaheadMatchCount(final String incompleteTerm) throws Exception {
    final Terminology ncit =
        terminologyUtils.getIndexedTerminology("ncit", opensearchQueryService, true);
    final Typeahead typeahead = new Typeahead(incompleteTerm);
    final List<Terminology> terminologies = List.of(ncit);
    final OpenSearchOperations operations = opensearchOperationsService.getOpenSearchOperations();
    final NativeSearchQuery countQuery =
        new NativeSearchQueryBuilder()
            .withQuery(typeahead.fetchOpenSearchQuery(operations, terminologies))
            .build();
    return operations.count(countQuery, Concept.class, EvsQuery.getIndexCoordinates(terminologies));
  }

  /**
   * Test typeahead highlights the typed words of a matched name, using OpenSearch's highlights.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadNameMatchHighlightsTypedWords() throws Exception {
    log.info("Testing typeahead - incompleteTerm = lung ca, include = minimal,highlights");
    final ConceptResultList list =
        searchController.typeaheadService("lung ca", null, null, null, "minimal,highlights");
    final Concept lungCarcinoma = list.getConcepts().get(0);
    assertThat(lungCarcinoma.getName()).isEqualTo("Lung Carcinoma");
    assertThat(lungCarcinoma.getHighlight()).isEqualTo("<em>Lung</em> <em>Carcinoma</em>");
  }

  /**
   * Test typeahead highlights leave names as stored, without highlight markup.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadHighlightsLeaveNamesUnmarked() throws Exception {
    log.info("Testing typeahead - incompleteTerm = lung ca, include = minimal,highlights");
    final ConceptResultList list =
        searchController.typeaheadService("lung ca", null, null, null, "minimal,highlights");
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> !c.getName().contains("<em>"));
  }

  /**
   * Test typeahead highlights the code when only the code matched.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadCodeMatchHighlightsCode() throws Exception {
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = C322, include = minimal,highlights");
    final ConceptResultList list =
        searchController.typeaheadService("C322", null, null, pageSize, "minimal,highlights");
    final Concept melanoma =
        list.getConcepts().stream().filter(c -> "C3224".equals(c.getCode())).findFirst().get();
    assertThat(melanoma.getHighlight()).isEqualTo("<em>C3224</em>");
  }

  /**
   * Test typeahead returns only the minimal fields: enough to identify and show each suggestion.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadDefaultIncludeReturnsMinimalFields() throws Exception {
    log.info("Testing typeahead - incompleteTerm = melan");
    final ConceptResultList list =
        searchController.typeaheadService("melan", null, null, null, null);
    assertThat(list.getConcepts()).isNotEmpty();
    for (final Concept concept : list.getConcepts()) {
      assertThat(concept.getCode()).isNotBlank();
      assertThat(concept.getName()).isNotBlank();
      assertThat(concept.getTerminology()).isEqualTo("ncit");
      assertThat(concept.getVersion()).isNotBlank();
      assertThat(concept.getSynonyms()).isEmpty();
      assertThat(concept.getDefinitions()).isEmpty();
      assertThat(concept.getProperties()).isEmpty();
      assertThat(concept.getParents()).isEmpty();
      assertThat(concept.getChildren()).isEmpty();
      assertThat(concept.getDescendants()).isEmpty();
    }
  }

  /**
   * Test the typeahead endpoint returns no highlights by default.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointDefaultIncludeReturnsNoHighlights() throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=lung ca");
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(get(TYPEAHEAD_URL).param("term", "lung ca"));
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> c.getHighlight() == null);
  }

  /**
   * Test the typeahead endpoint returns a highlight for every suggestion with
   * include=minimal,highlights.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointHighlightsIncludeReturnsHighlights() throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=lung ca&include=minimal,highlights");
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL).param("term", "lung ca").param("include", "minimal,highlights"));
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> c.getHighlight() != null);
  }

  /**
   * Test the typeahead endpoint with no page size returns the default number of suggestions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointMissingPageSizeDefaultsToTen() throws Exception {
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=melan");
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(get(TYPEAHEAD_URL).param("term", "melan"));
    assertThat(list.getConcepts()).hasSize(TYPEAHEAD_DEFAULT_PAGE_SIZE);
  }

  /**
   * Test the typeahead endpoint returns the requested number of suggestions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointExplicitPageSizeReturnsRequestedPageSize() throws Exception {
    final int requestedPageSize = 3;
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=melan&pageSize=" + requestedPageSize);
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL)
                .param("term", "melan")
                .param("pageSize", String.valueOf(requestedPageSize)));
    assertThat(list.getConcepts()).hasSize(requestedPageSize);
  }

  /**
   * Test the typeahead endpoint echoes the requested page size in the response parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointExplicitPageSizeEchoesPageSize() throws Exception {
    final int requestedPageSize = 3;
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=melan&pageSize=" + requestedPageSize);
    final ConceptResultList list =
        fetchTypeaheadEndpointResults(
            get(TYPEAHEAD_URL)
                .param("term", "melan")
                .param("pageSize", String.valueOf(requestedPageSize)));
    assertThat(list.getParameters().getPageSize()).isEqualTo(requestedPageSize);
  }

  /**
   * Test the typeahead endpoint rejects a page size below the smallest allowed.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointPageSizeTooSmallReturnsBadRequest() throws Exception {
    final int tooSmallPageSize = TYPEAHEAD_MIN_PAGE_SIZE - 1;
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=melan&pageSize=" + tooSmallPageSize);
    mvc.perform(
            get(TYPEAHEAD_URL)
                .param("term", "melan")
                .param("pageSize", String.valueOf(tooSmallPageSize)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test the typeahead endpoint rejects a page size that isn't a number.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadEndpointNonNumericPageSizeReturnsBadRequest() throws Exception {
    final String nonNumericPageSize = "abc";
    log.info("Testing url - " + TYPEAHEAD_URL + "?term=melan&pageSize=" + nonNumericPageSize);
    mvc.perform(get(TYPEAHEAD_URL).param("term", "melan").param("pageSize", nonNumericPageSize))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test typeahead doesn't suggest a retired concept. "Melan-A" (C17878) is retired in ncit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadRetiredConceptIsExcluded() throws Exception {
    final String retiredCode = "C17878";
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = melan-a, pageSize = " + pageSize);
    final ConceptResultList list =
        searchController.typeaheadService("melan-a", null, null, pageSize, null);
    assertThat(list.getConcepts()).extracting(Concept::getCode).doesNotContain(retiredCode);
  }

  /**
   * Test typeahead suggests only active concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadReturnsOnlyActiveConcepts() throws Exception {
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = melan-a, pageSize = " + pageSize);
    final ConceptResultList list =
        searchController.typeaheadService("melan-a", null, null, pageSize, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(Concept::getActive);
  }

  /**
   * Test typeahead completes a partial code to the codes that start with it. Codes are case
   * sensitive.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadPartialCodeReturnsCompletedCodes() throws Exception {
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = C322, pageSize = " + pageSize);
    final ConceptResultList list =
        searchController.typeaheadService("C322", null, null, pageSize, null);
    assertThat(list.getConcepts()).extracting(Concept::getCode).contains("C3224", "C32240");
  }

  /**
   * Test typeahead on a partial code returns only codes that start with it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadPartialCodeReturnsOnlyCodesStartingWithIt() throws Exception {
    final int pageSize = 100;
    log.info("Testing typeahead - incompleteTerm = C322, pageSize = " + pageSize);
    final ConceptResultList list =
        searchController.typeaheadService("C322", null, null, pageSize, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts()).allMatch(c -> c.getCode().startsWith("C322"));
  }

  /**
   * Test typeahead ranks the concept with a whole typed code first, ahead of the longer codes that
   * start with it (C32240 to C32249).
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWholeCodeRanksAboveLongerCodes() throws Exception {
    log.info("Testing typeahead - incompleteTerm = C3224");
    final ConceptResultList list =
        searchController.typeaheadService("C3224", null, null, null, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
  }

  /**
   * Test typeahead ranks name matches above concepts that only matched by code. Every ncit code
   * starts with "C", so "c3" matches thousands of codes, but names like "C3 Gene" come first.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadNameMatchesRankAboveCodes() throws Exception {
    final String incompleteTerm = "c3";
    log.info("Testing typeahead - incompleteTerm = " + incompleteTerm);
    final ConceptResultList list =
        searchController.typeaheadService(incompleteTerm, null, null, null, null);
    assertThat(list.getConcepts()).isNotEmpty();
    assertThat(list.getConcepts())
        .allMatch(c -> ConceptUtils.normalize(c.getName()).matches("(^|.* )c3.*"));
  }

  /**
   * Test typeahead on a terminology whose index doesn't have the typeahead field yet succeeds with
   * no suggestions, rather than failing. Skipped once ncim is reindexed with the typeahead field.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadTerminologyWithoutTypeaheadFieldReturnsEmptyList() throws Exception {
    assumeFalse(getHasTypeaheadField("ncim"), "ncim is indexed with the typeahead field");
    log.info("Testing typeahead - incompleteTerm = melan, terminology = ncim");
    final ConceptResultList list =
        searchController.typeaheadService("melan", List.of("ncim"), null, null, null);
    assertThat(list.getConcepts()).isEmpty();
    assertThat(list.getTotal()).isEqualTo(0L);
  }

  /**
   * Fetches typeahead suggestions from the endpoint, expecting the request to succeed.
   *
   * @param request the typeahead endpoint request
   * @return the suggestions
   * @throws Exception the exception
   */
  private ConceptResultList fetchTypeaheadEndpointResults(
      final MockHttpServletRequestBuilder request) throws Exception {
    final MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    return ThreadLocalMapper.get().readValue(content, ConceptResultList.class);
  }

  /**
   * Indicates whether a terminology's concept index has the typeahead field, which is only added
   * when the terminology is reindexed.
   *
   * @param terminology the terminology name
   * @return whether preferred names are indexed for typeahead
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private boolean getHasTypeaheadField(final String terminology) throws Exception {
    final String indexName =
        terminologyUtils
            .getIndexedTerminology(terminology, opensearchQueryService, true)
            .getIndexName();
    final Map<String, Object> mapping =
        opensearchOperationsService
            .getOpenSearchOperations()
            .indexOps(IndexCoordinates.of(indexName))
            .getMapping();
    final Map<String, Object> properties =
        (Map<String, Object>) mapping.getOrDefault("properties", Map.of());
    final Map<String, Object> nameField =
        (Map<String, Object>) properties.getOrDefault("name", Map.of());
    final Map<String, Object> nameSubfields =
        (Map<String, Object>) nameField.getOrDefault("fields", Map.of());
    return nameSubfields.containsKey(ConceptMinimal.SEARCH_AS_YOU_TYPE_SUFFIX);
  }

  /**
   * Test Typeahead without a term is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWithoutTermReturnsBadRequest() throws Exception {
    final String withoutTermQuery =
        """
        {"type": "Typeahead"}
        """;

    postQuery(withoutTermQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test Typeahead with a blank term, which has no words to complete, is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWithBlankTermReturnsBadRequest() throws Exception {
    final String blankTermQuery =
        """
        {"type": "Typeahead", "term": "   "}
        """;

    postQuery(blankTermQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test Typeahead with a punctuation-only term, which has no words to complete, is rejected as a
   * bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWithPunctuationOnlyTermReturnsBadRequest() throws Exception {
    final String punctuationOnlyTermQuery =
        """
        {"type": "Typeahead", "term": "---"}
        """;

    postQuery(punctuationOnlyTermQuery).andExpect(status().isBadRequest());
  }
}
