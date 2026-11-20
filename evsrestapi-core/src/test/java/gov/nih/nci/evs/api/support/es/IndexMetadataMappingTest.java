package gov.nih.nci.evs.api.support.es;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyStats;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;

/** Statistics are stored as source data, without inferring types from terminology versions. */
class IndexMetadataMappingTest {
  @Test
  void metadataStatisticsAreNotDynamicallyMapped() {
    var operations = new OpenSearchRestTemplate(mock(RestHighLevelClient.class));
    Map<?, ?> mapping = operations.indexOps(IndexMetadata.class).createMapping();
    Map<?, ?> properties = (Map<?, ?>) mapping.get("properties");
    Map<?, ?> terminology = (Map<?, ?>) properties.get("terminology");
    Map<?, ?> terminologyProperties = (Map<?, ?>) terminology.get("properties");
    Map<?, ?> stats = (Map<?, ?>) terminologyProperties.get("stats");
    assertNotNull(stats, mapping::toString);
    assertEquals(Boolean.FALSE, stats.get("enabled"), mapping::toString);
    Map<?, ?> metadata = (Map<?, ?>) terminologyProperties.get("metadata");
    assertEquals(Boolean.FALSE, metadata.get("enabled"), mapping::toString);
    assertEquals("keyword", ((Map<?, ?>) terminologyProperties.get("version")).get("type"));
  }

  @Test
  void auditStatisticsAreNotDynamicallyMapped() {
    var operations = new OpenSearchRestTemplate(mock(RestHighLevelClient.class));
    Map<?, ?> mapping = operations.indexOps(Audit.class).createMapping();
    Map<?, ?> properties = (Map<?, ?>) mapping.get("properties");
    Map<?, ?> stats = (Map<?, ?>) properties.get("stats");
    assertNotNull(stats, mapping::toString);
    assertEquals(Boolean.FALSE, stats.get("enabled"), mapping::toString);
    assertEquals("date", ((Map<?, ?>) properties.get("date")).get("type"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2021-02-23", "1.3.1.1", "2023AA", "6.0"})
  void unindexedStatisticsRemainInDocumentSource(String version) {
    var operations = new OpenSearchRestTemplate(mock(RestHighLevelClient.class));
    TerminologyStats stats = new TerminologyStats();
    stats.setVersion(version);
    Terminology term = new Terminology();
    term.setVersion(version);
    term.setStats(stats);
    IndexMetadata metadata = new IndexMetadata();
    metadata.setIndexName("concept_test");
    metadata.setTerminology(term);
    Map<?, ?> source = operations.getElasticsearchConverter().mapObject(metadata);
    Map<?, ?> terminology = (Map<?, ?>) source.get("terminology");
    assertEquals(version, ((Map<?, ?>) terminology.get("stats")).get("version"));

    Audit audit = new Audit();
    audit.setStats(stats);
    source = operations.getElasticsearchConverter().mapObject(audit);
    assertEquals(version, ((Map<?, ?>) source.get("stats")).get("version"));
  }
}
