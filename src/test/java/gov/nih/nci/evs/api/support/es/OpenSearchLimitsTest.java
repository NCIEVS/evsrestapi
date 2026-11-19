package gov.nih.nci.evs.api.support.es;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests that the running OpenSearch has the limits {@link OpenSearchLimits} says it has. The code
 * checks queries against those constants before searching, so if a server or {@code reindex.sh}
 * change moves a limit, this fails instead of the checks quietly allowing searches OpenSearch
 * rejects, or rejecting searches it allows.
 *
 * <p>Runs against the test OpenSearch with NCIt loaded.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class OpenSearchLimitsTest {

  /** The terminology whose concept index is checked. */
  private static final String TERMINOLOGY = "ncit";

  /** The index setting for the furthest a search can page. */
  private static final String MAX_RESULT_WINDOW_SETTING = "index.max_result_window";

  /** The index setting for the most values in one terms query. */
  private static final String MAX_TERMS_COUNT_SETTING = "index.max_terms_count";

  /** The cluster setting for the most clauses in one query. */
  private static final String MAX_CLAUSE_COUNT_SETTING = "indices.query.bool.max_clause_count";

  /** The OpenSearch client. */
  @Autowired private RestHighLevelClient client;

  /** The terminology utils, to find NCIt's concept index. */
  @Autowired private TerminologyUtils termUtils;

  /** The OpenSearch query service, to find NCIt's concept index. */
  @Autowired private OpensearchQueryService osQueryService;

  /**
   * Test NCIt's concept index pages exactly as far as MAX_RESULT_WINDOW.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptIndexResultWindowMatchesMaxResultWindow() throws Exception {
    assertEquals(
        String.valueOf(OpenSearchLimits.MAX_RESULT_WINDOW),
        fetchConceptIndexSetting(MAX_RESULT_WINDOW_SETTING));
  }

  /**
   * Test NCIt's concept index accepts exactly MAX_TERMS_COUNT values in one terms query.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptIndexTermsCountMatchesMaxTermsCount() throws Exception {
    assertEquals(
        String.valueOf(OpenSearchLimits.MAX_TERMS_COUNT),
        fetchConceptIndexSetting(MAX_TERMS_COUNT_SETTING));
  }

  /**
   * Test the cluster allows exactly MAX_CLAUSE_COUNT clauses in one query.
   *
   * @throws Exception the exception
   */
  @Test
  public void testClusterClauseCountMatchesMaxClauseCount() throws Exception {
    final ClusterGetSettingsRequest request = new ClusterGetSettingsRequest().includeDefaults(true);

    final String clauseCount =
        client
            .cluster()
            .getSettings(request, RequestOptions.DEFAULT)
            .getSetting(MAX_CLAUSE_COUNT_SETTING);

    assertEquals(String.valueOf(OpenSearchLimits.MAX_CLAUSE_COUNT), clauseCount);
  }

  /**
   * Fetches a setting of NCIt's concept index, including its default if the index doesn't set it.
   *
   * @param setting the setting name
   * @return the setting's value
   * @throws Exception the exception
   */
  private String fetchConceptIndexSetting(final String setting) throws Exception {
    final String indexName =
        termUtils.getIndexedTerminology(TERMINOLOGY, osQueryService, true).getIndexName();
    final GetSettingsRequest request =
        new GetSettingsRequest().indices(indexName).includeDefaults(true);

    return client
        .indices()
        .getSettings(request, RequestOptions.DEFAULT)
        .getSetting(indexName, setting);
  }
}
