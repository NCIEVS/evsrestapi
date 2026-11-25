package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.mock.env.MockEnvironment;

/** Opt-in test against a disposable loopback OpenSearch 1.3.20 server. */
@EnabledIfEnvironmentVariable(named = "LEGACY_OPENSEARCH_TEST_PORT", matches = "[0-9]+")
class OpenSearchLegacyCompatibilityIntegrationTest {

  /**
   * Exercise request conversion and response parsing using the configured high-level client.
   *
   * @throws Exception if a server operation fails
   */
  @Test
  void indexLifecycleWorksOnLegacyServer() throws Exception {
    final OpensearchConfiguration configuration = new OpensearchConfiguration();
    configuration.env =
        new MockEnvironment()
            .withProperty("nci.evs.opensearch.server.host", "127.0.0.1")
            .withProperty(
                "nci.evs.opensearch.server.port", System.getenv("LEGACY_OPENSEARCH_TEST_PORT"))
            .withProperty("nci.evs.opensearch.server.scheme", "http")
            .withProperty("nci.evs.opensearch.timeout", "10000");
    final String index = "evs-legacy-test-" + UUID.randomUUID();
    try (RestHighLevelClient client = configuration.client()) {
      assertEquals("1.3.20", client.info(RequestOptions.DEFAULT).getVersion().getNumber());
      try {
        assertTrue(
            client
                .indices()
                .create(
                    new CreateIndexRequest(index)
                        .settings(Map.of("index.number_of_replicas", 0))
                        .mapping(Map.of("properties", Map.of("name", Map.of("type", "keyword")))),
                    RequestOptions.DEFAULT)
                .isAcknowledged());
        assertTrue(client.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT));
        assertFalse(
            client
                .bulk(
                    new BulkRequest()
                        .add(new IndexRequest(index).id("1").source(Map.of("name", "test"))),
                    RequestOptions.DEFAULT)
                .hasFailures());
        client.indices().refresh(new RefreshRequest(index), RequestOptions.DEFAULT);
        assertTrue(client.get(new GetRequest(index, "1"), RequestOptions.DEFAULT).isExists());
        assertEquals(
            1,
            client
                .search(
                    new SearchRequest(index)
                        .source(
                            new SearchSourceBuilder()
                                .query(QueryBuilders.termQuery("name", "test"))),
                    RequestOptions.DEFAULT)
                .getHits()
                .getTotalHits()
                .value());
        client.update(
            new UpdateRequest(index, "1").doc(Map.of("name", "updated")), RequestOptions.DEFAULT);
        client.indices().refresh(new RefreshRequest(index), RequestOptions.DEFAULT);
        assertEquals(
            1,
            client
                .deleteByQuery(
                    new DeleteByQueryRequest(index).setQuery(QueryBuilders.matchAllQuery()),
                    RequestOptions.DEFAULT)
                .getDeleted());
      } finally {
        client.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
      }
    }
  }
}
