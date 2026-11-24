package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Tests paging through the query endpoint's matches with fromRecord and pageSize. */
public class SearchControllerQueryPagingTests extends AbstractSearchControllerQueryTests {

  /** The four children of Breast, a query with a few matches to page through. */
  private static final String BREAST_CHILDREN_QUERY =
      """
      {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}
      """;

  /** Gene Ontology, a terminology with a hierarchy far larger than 10,000 concepts. */
  private static final String GENE_ONTOLOGY = "go";

  /** Every descendant of biological_process (GO:0008150), a query with many matches. */
  private static final String BIOLOGICAL_PROCESS_DESCENDANTS_QUERY =
      """
      {"type": "HasAncestor", "target": "GO:0008150"}
      """;

  /**
   * Posts a query tree and returns the codes on one page of its matches.
   *
   * @param queryJson the query tree as JSON
   * @param fromRecord the index of the first concept to return
   * @param pageSize the maximum number of concepts to return
   * @return the codes, in the order returned
   * @throws Exception the exception
   */
  private List<String> fetchCodesOnPage(
      final String queryJson, final int fromRecord, final int pageSize) throws Exception {
    return fetchPage(queryJson, fromRecord, pageSize).getConcepts().stream()
        .map(Concept::getCode)
        .toList();
  }

  /**
   * Posts a query tree, asserts the response is OK, and reads one page of its matches.
   *
   * @param queryJson the query tree as JSON
   * @param fromRecord the index of the first concept to return
   * @param pageSize the maximum number of concepts to return
   * @return the result list
   * @throws Exception the exception
   */
  private ConceptResultList fetchPage(
      final String queryJson, final int fromRecord, final int pageSize) throws Exception {
    return fetchPage(TERMINOLOGY, queryJson, fromRecord, pageSize);
  }

  /**
   * Posts a query tree to search a terminology, asserts the response is OK, and reads one page of
   * its matches.
   *
   * @param terminology the terminology to search
   * @param queryJson the query tree as JSON
   * @param fromRecord the index of the first concept to return
   * @param pageSize the maximum number of concepts to return
   * @return the result list
   * @throws Exception the exception
   */
  private ConceptResultList fetchPage(
      final String terminology, final String queryJson, final int fromRecord, final int pageSize)
      throws Exception {
    final String content =
        postQueryPage(terminology, queryJson, fromRecord, pageSize)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ThreadLocalMapper.get().readValue(content, ConceptResultList.class);
  }

  /**
   * Posts a query tree asking for one page of its matches, searching NCIt.
   *
   * @param queryJson the query tree as JSON
   * @param fromRecord the index of the first concept to return
   * @param pageSize the maximum number of concepts to return
   * @return the result actions
   * @throws Exception the exception
   */
  private ResultActions postQueryPage(
      final String queryJson, final int fromRecord, final int pageSize) throws Exception {
    return postQueryPage(TERMINOLOGY, queryJson, fromRecord, pageSize);
  }

  /**
   * Posts a query tree asking for one page of its matches, searching a terminology.
   *
   * @param terminology the terminology to search
   * @param queryJson the query tree as JSON
   * @param fromRecord the index of the first concept to return
   * @param pageSize the maximum number of concepts to return
   * @return the result actions
   * @throws Exception the exception
   */
  private ResultActions postQueryPage(
      final String terminology, final String queryJson, final int fromRecord, final int pageSize)
      throws Exception {
    return mvc.perform(
        post(QUERY_URL, terminology)
            .param("fromRecord", String.valueOf(fromRecord))
            .param("pageSize", String.valueOf(pageSize))
            .contentType(MediaType.APPLICATION_JSON)
            .content(queryJson));
  }

  /**
   * Test a page from a record returns the matches at those positions: records 1-2 of the four
   * children of Breast are the second and third of all the matches.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPageFromRecordReturnsMatchesAtThosePositions() throws Exception {
    final int fromRecord = 1;
    final int pageSize = 2;

    final List<String> allCodes = fetchCodes(BREAST_CHILDREN_QUERY);
    final List<String> pageCodes = fetchCodesOnPage(BREAST_CHILDREN_QUERY, fromRecord, pageSize);

    assertThat(pageCodes).isEqualTo(allCodes.subList(fromRecord, fromRecord + pageSize));
  }

  /**
   * Test a page from a record past the last match returns no concepts, but still the total.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPageFromRecordPastLastMatchReturnsNoConcepts() throws Exception {
    final int pastLastMatch = BREAST_CHILDREN.size();

    final ConceptResultList results = fetchPage(BREAST_CHILDREN_QUERY, pastLastMatch, 10);

    assertThat(results.getConcepts()).isEmpty();
    assertThat(results.getTotal()).isEqualTo(BREAST_CHILDREN.size());
  }

  /**
   * Test the total counts every match past OpenSearch's default limit of 10,000, as legacy search
   * does: biological_process in Gene Ontology has more descendants than that.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTotalCountsMatchesPastCountedHitLimit() throws Exception {
    final long countedHitLimit = 10_000L;

    final ConceptResultList results =
        fetchPage(GENE_ONTOLOGY, BIOLOGICAL_PROCESS_DESCENDANTS_QUERY, 0, 10);

    assertThat(results.getTotal()).isGreaterThan(countedHitLimit);
  }
}
