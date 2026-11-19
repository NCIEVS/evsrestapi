package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.query.QueryTooBroadException;
import gov.nih.nci.evs.api.service.OpenSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Tests that a query too broad to run reaches the client as a bad request carrying the reason.
 * Nodes throw {@link QueryTooBroadException} for limits real data rarely reaches, e.g. a hierarchy
 * target with more than 65,536 codes below it, so the search is mocked to throw one. Each limit's
 * edge is tested where it's owned ({@code HasHierarchyRelativeLimitsTest}); they all reach the
 * client through this one mapping.
 *
 * <p>In its own class because the mock replaces the search service for every test in the class.
 */
public class SearchControllerQueryTooBroadTests extends AbstractSearchControllerQueryTests {

  /** The reason the mocked search gives. */
  private static final String TOO_BROAD_REASON =
      "Too many values for one terms query on 'code': 65537 > 65536";

  /** A query for the mocked search to reject. */
  private static final String QUERY = "{\"type\": \"HasAncestor\", \"target\": \"C12971\"}";

  /** The first record of the search. */
  private static final int FIRST_RECORD = 0;

  /** The page size of the search. */
  private static final int PAGE_SIZE = 10;

  /** The search service, mocked to reject every query as too broad. */
  @MockitoBean private OpenSearchService openSearchService;

  /** Makes every query search reject the query as too broad. */
  @BeforeEach
  public void setUp() {
    when(openSearchService.findQueryConceptsWithExactTotal(any(), any(), any()))
        .thenThrow(new QueryTooBroadException(TOO_BROAD_REASON));
  }

  /** A query too broad to run is rejected as a bad request. */
  @Test
  public void testQueryTooBroadReturnsBadRequest() {
    final ResponseEntity<String> response = exchangeQuery(QUERY, FIRST_RECORD, PAGE_SIZE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A query too broad to run gets an error carrying the reason it's too broad. */
  @Test
  public void testQueryTooBroadErrorKeepsReason() {
    final String message = getErrorMessage(exchangeQuery(QUERY, FIRST_RECORD, PAGE_SIZE));

    assertThat(message).isEqualTo(TOO_BROAD_REASON);
  }
}
