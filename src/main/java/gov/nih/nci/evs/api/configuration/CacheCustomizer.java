package gov.nih.nci.evs.api.configuration;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

/**
 * Cache customizer
 * 
 * @author Arun
 *
 */
@Component
public class CacheCustomizer 
    implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

  /**
   * Customize the cache manager.
   * 
   * @param cacheManager the {@code CacheManager} to customize
   */
  @Override
  public void customize(ConcurrentMapCacheManager cacheManager) {
    cacheManager.setCacheNames(Arrays.asList("metadata"));
  }
}