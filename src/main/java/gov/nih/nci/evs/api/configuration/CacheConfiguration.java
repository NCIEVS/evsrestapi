package gov.nih.nci.evs.api.configuration;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

/**
 * Cache configuration
 * 
 * @author Arun
 */
@Configuration
public class CacheConfiguration {

  /** the constant logger **/
  private static final Logger logger = LoggerFactory.getLogger(CacheConfiguration.class);
  
  /** the cache manager **/
  @Autowired
  CacheManager cacheManager;
  
  /**
   * Scheduled method to evict all cache managed by spring cache manager.
   * NOTE: while @Cacheable is no longer used, we'll keep this becuase
   * it has no major effect and will be desired if we bring back any
   * caching features
   * 
   * The schedule is defined by the cron expression.
   */
  @Scheduled(cron = "0 0 0 * * ?", zone = "America/Los_Angeles")
  public void evictAll() {
    logger.info("evictAll()");
    Collection<String> cacheNames = cacheManager.getCacheNames();
    if (CollectionUtils.isEmpty(cacheNames)) return;

    cacheNames.stream().forEach(name -> cacheManager.getCache(name).clear());
  }
  
}
