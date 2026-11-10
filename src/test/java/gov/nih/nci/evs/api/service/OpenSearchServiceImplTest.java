package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/** Checks that concept searches use the indexes of the resolved terminologies. */
class OpenSearchServiceImplTest {

  private final OpenSearchOperations operations = mock(OpenSearchOperations.class);
  private final OpenSearchServiceImpl service = new OpenSearchServiceImpl();

  @BeforeEach
  void setUp() {
    service.operations = operations;
    final SearchHits<Concept> hits = mock(SearchHits.class);
    when(hits.stream()).thenAnswer(invocation -> Stream.empty());
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(hits);
  }

  @Test
  void emptyCriteriaTerminologyStillUsesVersionedIndex() throws Exception {
    final Terminology terminology = terminology("26.10d");
    final SearchCriteria criteria = criteria();

    service.findConcepts(List.of(terminology), criteria);

    assertArrayEquals(new String[] {"concept_ncit_2610d"}, searchedIndices());
  }

  @Test
  void criteriaTerminologyDoesNotOverrideResolvedIndex() throws Exception {
    final SearchCriteria criteria = criteria();
    criteria.getTerminology().add("ncit_26.10a");

    service.findConcepts(List.of(terminology("26.10d")), criteria);

    assertArrayEquals(new String[] {"concept_ncit_2610d"}, searchedIndices());
  }

  @Test
  void staleWeeklyConceptCannotReplaceMonthlySubsetParent() throws Exception {
    final Concept staleWeeklyParent = new Concept();
    staleWeeklyParent.setVersion("26.10a");
    final Concept monthlyParent = new Concept();
    monthlyParent.setVersion("26.10d");
    final Concept bloodpacSubset = new Concept();
    bloodpacSubset.setCode("C228977");
    monthlyParent.setChildren(List.of(bloodpacSubset));

    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenAnswer(
            invocation -> {
              final IndexCoordinates indices = invocation.getArgument(2);
              return conceptHits(
                  Arrays.asList(indices.getIndexNames()).contains("_all")
                      ? staleWeeklyParent
                      : monthlyParent);
            });

    final Concept result =
        service.findConcepts(List.of(terminology("26.10d")), criteria()).getConcepts().get(0);

    assertEquals("26.10d", result.getVersion());
    assertTrue(result.getChildren().stream().anyMatch(child -> "C228977".equals(child.getCode())));
    assertArrayEquals(new String[] {"concept_ncit_2610d"}, searchedIndices());
  }

  @Test
  void nullTerminologyListIsRejected() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.findConcepts(null, criteria()));

    assertEquals("At least one terminology is required for concept search", exception.getMessage());
    verifyNoInteractions(operations);
  }

  @Test
  void emptyTerminologyListIsRejected() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> service.findConcepts(List.of(), criteria()));

    assertEquals("At least one terminology is required for concept search", exception.getMessage());
    verifyNoInteractions(operations);
  }

  @Test
  void nullTerminologyEntryIsRejected() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.findConcepts(Collections.singletonList(null), criteria()));

    assertEquals("Each terminology must have a concept index name", exception.getMessage());
    verifyNoInteractions(operations);
  }

  @Test
  void emptyIndexNameIsRejected() {
    final Terminology terminology = mock(Terminology.class);
    when(terminology.getIndexName()).thenReturn("");

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.findConcepts(List.of(terminology), criteria()));

    assertEquals("Each terminology must have a concept index name", exception.getMessage());
    verifyNoInteractions(operations);
  }

  private String[] searchedIndices() {
    final ArgumentCaptor<IndexCoordinates> indices =
        ArgumentCaptor.forClass(IndexCoordinates.class);
    verify(operations).search(any(Query.class), eq(Concept.class), indices.capture());
    return indices.getValue().getIndexNames();
  }

  private static SearchHits<Concept> conceptHits(final Concept concept) {
    final SearchHit<Concept> hit = mock(SearchHit.class);
    when(hit.getContent()).thenReturn(concept);
    when(hit.getHighlightFields()).thenReturn(Map.of());
    final SearchHits<Concept> hits = mock(SearchHits.class);
    when(hits.getTotalHits()).thenReturn(1L);
    when(hits.stream()).thenAnswer(invocation -> Stream.of(hit));
    return hits;
  }

  private static SearchCriteria criteria() {
    final SearchCriteria criteria = new SearchCriteria();
    criteria.setTerm("C228977");
    criteria.setType("match");
    return criteria;
  }

  private static Terminology terminology(final String version) {
    final Terminology terminology = new Terminology();
    terminology.setTerminology("ncit");
    terminology.setVersion(version);
    return terminology;
  }
}
