package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.support.es.OpensearchObject;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Checks caching of index metadata and properties: repeated reads hit OpenSearch once and callers
 * get copies.
 */
@SpringJUnitConfig(OpensearchQueryCacheServiceTest.Config.class)
class OpensearchQueryCacheServiceTest {

  /** Minimal context with real caching; OpenSearch and terminology utils are mocked below. */
  @Configuration
  @EnableCaching
  @Import({OpensearchQueryCacheService.class, OpensearchQueryServiceImpl.class})
  static class Config {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(
          OpensearchQueryCacheService.INDEX_METADATA_CACHE,
          OpensearchQueryCacheService.PROPERTIES_CACHE);
    }
  }

  @Autowired OpensearchQueryService queryService;

  @MockitoBean OpenSearchOperations operations;

  @MockitoBean TerminologyUtils termUtils;

  @Autowired CacheManager cacheManager;

  private final Terminology terminology = terminology();

  @BeforeEach
  void setUp() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    stubSearch(IndexMetadata.class, this::indexMetadata);
    stubSearch(OpensearchObject.class, this::propertiesObject);
  }

  @Test
  void indexMetadataIsReadOnce() {
    queryService.getIndexMetadata(true);
    queryService.getIndexMetadata(true);

    verifySearches(IndexMetadata.class, 1);
  }

  @Test
  void indexMetadataCallersGetCopies() {
    final IndexMetadata first = queryService.getIndexMetadata(true).get(0);
    first.getTerminology().setLatest(false);
    first.getTerminology().getTags().put("changed", "true");
    first.getTerminology().getMetadata().setUiLabel("changed");

    final IndexMetadata second = queryService.getIndexMetadata(true).get(0);
    assertTrue(second.getTerminology().getLatest());
    assertFalse(second.getTerminology().getTags().containsKey("changed"));
    assertEquals("NCI Thesaurus", second.getTerminology().getMetadata().getUiLabel());
  }

  @Test
  void propertiesAreReadOnceAcrossIncludesAndGetProperty() throws Exception {
    final List<Concept> minimal = queryService.getProperties(terminology, new IncludeParam());
    final List<Concept> summary =
        queryService.getProperties(terminology, new IncludeParam("summary"));
    final Concept property =
        queryService.getProperty("P1", terminology, new IncludeParam("summary")).orElse(null);

    verifySearches(OpensearchObject.class, 1);
    assertTrue(minimal.get(0).getSynonyms().isEmpty());
    assertEquals(1, summary.get(0).getSynonyms().size());
    assertNotNull(property);
    assertEquals("P1", property.getCode());
  }

  @Test
  void propertiesCallersGetCopies() throws Exception {
    final Concept first =
        queryService.getProperties(terminology, new IncludeParam("summary")).get(0);
    first.setName("changed");
    first.getSynonyms().clear();

    final Concept second =
        queryService.getProperties(terminology, new IncludeParam("summary")).get(0);
    assertEquals("Property One", second.getName());
    assertEquals(1, second.getSynonyms().size());
  }

  @Test
  void indexMetadataCopyIsIndependent() {
    final IndexMetadata original = indexMetadata();
    final IndexMetadata copy = new IndexMetadata(original);
    copy.getTerminology().setLatest(false);

    assertEquals(original.getIndexName(), copy.getIndexName());
    assertTrue(original.getTerminology().getLatest());
    assertNull(new IndexMetadata(new IndexMetadata()).getTerminology());
  }

  @SuppressWarnings("unchecked")
  private <T> void stubSearch(final Class<T> clazz, final java.util.function.Supplier<T> content) {
    when(operations.search(any(Query.class), eq(clazz), any(IndexCoordinates.class)))
        .thenAnswer(
            invocation -> {
              final SearchHit<T> hit = mock(SearchHit.class);
              when(hit.getContent()).thenReturn(content.get());
              final SearchHits<T> hits = mock(SearchHits.class);
              when(hits.stream()).thenAnswer(i -> Stream.of(hit));
              return hits;
            });
  }

  private void verifySearches(final Class<?> clazz, final int count) {
    verify(operations, times(count))
        .search(any(Query.class), eq(clazz), any(IndexCoordinates.class));
  }

  private static Terminology terminology() {
    final Terminology terminology = new Terminology();
    terminology.setTerminology("ncit");
    terminology.setVersion("26.10d");
    terminology.setTerminologyVersion("ncit_26.10d");
    terminology.setLatest(true);
    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setUiLabel("NCI Thesaurus");
    terminology.setMetadata(metadata);
    return terminology;
  }

  private IndexMetadata indexMetadata() {
    final IndexMetadata iMeta = new IndexMetadata();
    iMeta.setIndexName("concept_ncit_2610d");
    iMeta.setCompleted(true);
    iMeta.setTerminology(terminology());
    return iMeta;
  }

  private OpensearchObject propertiesObject() {
    final Concept property = new Concept();
    property.setCode("P1");
    property.setName("Property One");
    final Synonym synonym = new Synonym();
    synonym.setName("Prop 1");
    property.getSynonyms().add(synonym);
    final OpensearchObject object = new OpensearchObject("properties");
    object.setConcepts(List.of(property));
    return object;
  }
}
