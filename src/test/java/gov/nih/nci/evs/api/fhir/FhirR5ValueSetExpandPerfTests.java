package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Performance tests for R5 value set expand calls against large, real value sets.
 *
 * <p>Excluded from the default {@code test} task (see build.gradle {@code excludeTags 'perf'}); run
 * explicitly with {@code ./gradlew perfTest}. Requires a local graphdb/opensearch stack loaded with
 * full production-scale NCIt data (e.g. the devreset snapshot), since the small UnitTestData set
 * won't reproduce the member counts these tests target.
 */
@Tag("perf")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5ValueSetExpandPerfTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5ValueSetExpandPerfTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** The fhir VS path. */
  private final String fhirVSPath = "/fhir/r5/ValueSet";

  /** The parser. */
  private static IParser parser;

  /** Max acceptable wall-clock time for expanding a ~1,200-member subset, in milliseconds. */
  private static final long LARGE_SUBSET_MAX_MILLIS = 3000L;

  /**
   * Max acceptable wall-clock time for expanding an is-a hierarchy with a property filter over
   * ~7,600 candidates, in milliseconds.
   */
  private static final long PROPERTY_FILTER_MAX_MILLIS = 5000L;

  /**
   * Max acceptable wall-clock time for expanding a ~1,200-code explicit concept list with
   * activeOnly and includeDesignations, in milliseconds.
   */
  private static final long EXPLICIT_LIST_MAX_MILLIS = 5000L;

  /** Sets the up once. */
  @BeforeAll
  public static void setUpOnce() {
    parser = FhirContext.forR5().newJsonParser().setPrettyPrint(true);
  }

  /**
   * Expands the C54452 (FDA-SPL, aggregate) subset, which has on the order of 1,200 members, and
   * asserts it completes within {@link #LARGE_SUBSET_MAX_MILLIS}.
   *
   * <p>Prior to batching the per-member lookups in the {@code ?fhir_vs=} implicit-subset expand
   * path (one {@code osQueryService.getConcept} call per member), this took ~6s locally against
   * localhost graphdb/opensearch; on a server with real network latency to those backends it is
   * proportionally worse. This test exists to catch a regression back to that N+1 pattern.
   *
   * @throws Exception the exception
   */
  @Test
  public void testExpandLargeSubsetPerformance() throws Exception {
    // Arrange
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54452";
    final String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    final String parameters = "?url=" + url;

    // Act
    final long start = System.nanoTime();
    final String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    final ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    final int total = valueSet.getExpansion().getContains().size();
    assertEquals(total, valueSet.getExpansion().getTotal());
    log.info(
        "Expanded C54452 ({} members) in {} ms (limit {} ms)",
        total,
        elapsedMillis,
        LARGE_SUBSET_MAX_MILLIS);
    assertTrue(
        elapsedMillis < LARGE_SUBSET_MAX_MILLIS,
        "Expanding C54452 ("
            + total
            + " members) took "
            + elapsedMillis
            + " ms, expected under "
            + LARGE_SUBSET_MAX_MILLIS
            + " ms");
  }

  /**
   * Expands an is-a hierarchy (C12219, ~7,600 descendants) combined with a property "exists"
   * filter, and asserts it completes within {@link #PROPERTY_FILTER_MAX_MILLIS}.
   *
   * <p>Property-based filters ({@code property = value} / {@code property exists}) in {@code
   * processInclude} call {@code osQueryService.getConcept()} once per candidate concept just to
   * check the property, for up to the 10,000-concept {@code maxConceptsForPropertyFiltering} cap.
   * Prior to batching this, ~7,600 candidates took ~22s locally against localhost
   * graphdb/opensearch. This test exists to catch a regression back to that N+1 pattern.
   *
   * @throws Exception the exception
   */
  @Test
  public void testExpandPropertyFilterPerformance() throws Exception {
    // Arrange
    final ValueSet inputValueSet = createIsAWithPropertyExistsFilterValueSet();
    final String requestBody = parser.encodeResourceToString(inputValueSet);
    final String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    final long start = System.nanoTime();
    final ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    final ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert
    // Note: contains.size() is the default page size (1000), not the full filtered total
    // (expansion.total, ~7615) -- not asserted on here since this test targets timing.
    assertTrue(expandedValueSet.hasExpansion());
    final int total = expandedValueSet.getExpansion().getContains().size();
    assertTrue(total > 0, "Expected at least one concept to pass the property filter");
    log.info(
        "Expanded is-a(C12219) + property exists filter ({} members) in {} ms (limit {} ms)",
        total,
        elapsedMillis,
        PROPERTY_FILTER_MAX_MILLIS);
    assertTrue(
        elapsedMillis < PROPERTY_FILTER_MAX_MILLIS,
        "Expanding is-a(C12219) + property exists filter ("
            + total
            + " members) took "
            + elapsedMillis
            + " ms, expected under "
            + PROPERTY_FILTER_MAX_MILLIS
            + " ms");
  }

  /**
   * Expands an explicit {@code compose.include.concept} list of ~1,200 codes (reused from the
   * C54452 subset) with {@code activeOnly=true} and {@code includeDesignations=true}, and asserts
   * it completes within {@link #EXPLICIT_LIST_MAX_MILLIS}.
   *
   * <p>{@code processInclude}'s direct-concept-list branch looked up each concept's display, active
   * status, and designations with up to 3 separate {@code osQueryService.getConcept} calls per
   * concept. Prior to batching this, ~1,200 codes with both options enabled took ~17s locally
   * against localhost graphdb/opensearch (worse than the unbatched subset-expand case this
   * mirrors). This test exists to catch a regression back to that N+1 pattern.
   *
   * @throws Exception the exception
   */
  @Test
  public void testExpandExplicitConceptListPerformance() throws Exception {
    // Arrange: reuse the C54452 subset's ~1,200 codes as an explicit include.concept list
    final String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    final String subsetUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54452";
    final String subsetContent =
        this.restTemplate.getForObject(endpoint + "?url=" + subsetUrl, String.class);
    final ValueSet subsetExpansion = parser.parseResource(ValueSet.class, subsetContent);
    final List<String> codes =
        subsetExpansion.getExpansion().getContains().stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toList());
    assertTrue(
        codes.size() > 500,
        "Expected a large code list from C54452 to stress-test explicit concept inclusion");

    final ValueSet inputValueSet = createExplicitConceptListValueSet(codes);
    final String requestBody = parser.encodeResourceToString(inputValueSet);

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    final long start = System.nanoTime();
    final ResponseEntity<String> response =
        this.restTemplate.postForEntity(
            endpoint + "?activeOnly=true&includeDesignations=true", request, String.class);
    final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
    final ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert
    assertTrue(expandedValueSet.hasExpansion());
    final int total = expandedValueSet.getExpansion().getContains().size();
    assertTrue(total > 0, "Expected at least one concept in the explicit list expansion");
    log.info(
        "Expanded explicit concept list ({} codes -> {} members) in {} ms (limit {} ms)",
        codes.size(),
        total,
        elapsedMillis,
        EXPLICIT_LIST_MAX_MILLIS);
    assertTrue(
        elapsedMillis < EXPLICIT_LIST_MAX_MILLIS,
        "Expanding explicit concept list ("
            + codes.size()
            + " codes -> "
            + total
            + " members) took "
            + elapsedMillis
            + " ms, expected under "
            + EXPLICIT_LIST_MAX_MILLIS
            + " ms");
  }

  /**
   * Creates a ValueSet with an explicit {@code compose.include.concept} list of the given codes.
   *
   * @param codes the codes to include
   * @return the value set
   */
  private ValueSet createExplicitConceptListValueSet(List<String> codes) {
    final ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("explicit-list-perf-test");
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/explicit-list-perf-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("ExplicitListPerfTest");
    inputValueSet.setTitle("Explicit Concept List Performance Test ValueSet");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDate(new Date());
    inputValueSet.setDescription(
        "Perf test ValueSet with a large explicit compose.include.concept list");

    final ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    final ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    for (final String code : codes) {
      nciInclude.addConcept(new ValueSet.ConceptReferenceComponent().setCode(code));
    }
    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Creates a ValueSet with an is-a filter (C12219) plus a property "exists" filter
   * (Semantic_Type), used to stress-test the property-filter evaluation path at scale.
   *
   * @return the value set
   */
  private ValueSet createIsAWithPropertyExistsFilterValueSet() {
    final ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("property-filter-perf-test");
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/property-filter-perf-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("PropertyFilterPerfTest");
    inputValueSet.setTitle("Property Filter Performance Test ValueSet");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDate(new Date());
    inputValueSet.setDescription(
        "Perf test ValueSet combining an is-a filter with a property exists filter");

    final ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    final ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    final ValueSet.ConceptSetFilterComponent isAFilter = new ValueSet.ConceptSetFilterComponent();
    isAFilter.setProperty("concept");
    isAFilter.setOp(Enumerations.FilterOperator.ISA);
    isAFilter.setValue("C12219");
    nciInclude.addFilter(isAFilter);

    final ValueSet.ConceptSetFilterComponent propertyFilter =
        new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Semantic_Type");
    propertyFilter.setOp(Enumerations.FilterOperator.EXISTS);
    propertyFilter.setValue("true");
    nciInclude.addFilter(propertyFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }
}
