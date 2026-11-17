package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.EVSPageable;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.support.es.OpensearchObject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

/**
 * Caches OpenSearch lookups that nearly every request repeats and that only change when a
 * terminology is loaded. It has to be a separate bean from {@link OpensearchQueryServiceImpl},
 * otherwise calls from inside that class bypass the Spring proxy and the @Cacheable annotation is
 * ignored.
 *
 * <p>Cached values are shared between requests. Callers must not modify them; {@link
 * OpensearchQueryServiceImpl} returns copies to the rest of the application.
 *
 * <p>Only the loader writes these indexes, so entries are not evicted on writes. They expire after
 * 10 minutes (see ehcache.xml), and are cleared by reindex.sh through {@code DELETE /admin/cache}
 * and by the nightly {@code CacheConfiguration#evictAll()}.
 */
@Service
public class OpensearchQueryCacheService {

  /** Cache name for index metadata. */
  public static final String INDEX_METADATA_CACHE = "indexMetadata";

  /** Cache name for terminology properties. */
  public static final String PROPERTIES_CACHE = "properties";

  /** The OpenSearch operations. */
  @Autowired OpenSearchOperations operations;

  /**
   * Returns the index metadata.
   *
   * @param completedOnly fetch metadata for completely loaded indexes only
   * @return the index metadata (shared, do not modify)
   */
  @Cacheable(value = INDEX_METADATA_CACHE, key = "#completedOnly")
  public List<IndexMetadata> getIndexMetadata(final boolean completedOnly) {
    // Get first 1000 records
    NativeSearchQueryBuilder queryBuilder =
        new NativeSearchQueryBuilder().withPageable(new EVSPageable(0, 1000, 0));
    if (completedOnly) {
      queryBuilder = queryBuilder.withFilter(QueryBuilders.matchQuery("completed", true));
    }
    return operations
        .search(
            queryBuilder.build(),
            IndexMetadata.class,
            IndexCoordinates.of(OpensearchOperationsService.METADATA_INDEX))
        .stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());
  }

  /**
   * Returns all properties for the terminology, before any include param is applied.
   *
   * @param terminology the terminology
   * @return the properties (shared, do not modify)
   */
  @Cacheable(
      value = PROPERTIES_CACHE,
      key = "#terminology.getObjectIndexName()",
      condition = "#terminology != null")
  public List<Concept> getProperties(final Terminology terminology) {
    final NativeSearchQuery query =
        new NativeSearchQueryBuilder()
            .withFilter(QueryBuilders.termQuery("_id", "properties"))
            .build();
    final List<OpensearchObject> objects =
        operations
            .search(
                query,
                OpensearchObject.class,
                IndexCoordinates.of(terminology.getObjectIndexName()))
            .stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    if (objects.isEmpty() || objects.get(0).getConcepts() == null) {
      return Collections.emptyList();
    }
    return objects.get(0).getConcepts();
  }
}
