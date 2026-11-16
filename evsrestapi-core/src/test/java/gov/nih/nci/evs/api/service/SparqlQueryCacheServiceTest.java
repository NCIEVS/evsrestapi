package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.util.RESTUtils;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Qualifier discovery must be shared by successive indexing workers. */
class SparqlQueryCacheServiceTest {
  @Test
  void qualifierDiscoveryIsReusedAcrossWorkerThreads() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(CacheConfiguration.class)) {
      var service = context.getBean(SparqlQueryCacheService.class);
      var rest = mock(RESTUtils.class);
      var manager = mock(SparqlQueryManagerService.class);
      when(manager.getQueryURL()).thenReturn("http://graph/query");
      when(rest.runSPARQL(anyString(), eq("http://graph/query")))
          .thenReturn("{\"results\":{\"bindings\":[]}}");
      var terminology = new Terminology();
      terminology.setTerminology("ctcae6");
      terminology.setVersion("6.0");
      terminology.setMetadata(new TerminologyMetadata());
      for (int batch = 0; batch < 2; batch++) {
        try (var worker = Executors.newSingleThreadExecutor()) {
          worker
              .submit(
                  () -> {
                    for (int property = 0; property < 5; property++) {
                      assertEquals(
                          0,
                          service
                              .getAllQualifiers(
                                  terminology, new IncludeParam("minimal"), rest, manager)
                              .size());
                    }
                    return null;
                  })
              .get(10, TimeUnit.SECONDS);
        }
      }
      verify(rest, times(1)).runSPARQL(anyString(), eq("http://graph/query"));

      // Different requested fields and terminology versions must not share a cache entry.
      service.getAllQualifiers(terminology, new IncludeParam("full"), rest, manager);
      terminology.setTerminologyVersion("ctcae6_6.1");
      service.getAllQualifiers(terminology, new IncludeParam("minimal"), rest, manager);
      verify(rest, times(3)).runSPARQL(anyString(), eq("http://graph/query"));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableCaching
  static class CacheConfiguration {
    @Bean(destroyMethod = "close")
    javax.cache.CacheManager nativeCacheManager() throws Exception {
      return javax.cache.Caching.getCachingProvider()
          .getCacheManager(
              getClass().getResource("/ehcache.xml").toURI(), getClass().getClassLoader());
    }

    @Bean
    CacheManager cacheManager(javax.cache.CacheManager nativeCacheManager) {
      return new JCacheCacheManager(nativeCacheManager);
    }

    @Bean
    QueryBuilderService queryBuilderService() {
      var builder = mock(QueryBuilderService.class);
      when(builder.constructPrefix(org.mockito.ArgumentMatchers.any())).thenReturn("");
      when(builder.constructQuery(eq("all.qualifiers"), org.mockito.ArgumentMatchers.any()))
          .thenReturn("SELECT qualifiers");
      return builder;
    }

    @Bean
    SparqlQueryCacheService sparqlQueryCacheService() {
      return new SparqlQueryCacheService();
    }
  }
}
