package gov.nih.nci.evs.api.configuration;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Class CacheEventLogger. */
public class CacheEventLogger implements CacheEventListener<Object, Object> {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(CacheConfiguration.class);

  /* see superclass */
  @Override
  public void onEvent(final CacheEvent<? extends Object, ? extends Object> cacheEvent) {
    // NOTE: enable the "<listeners>" section in ehcache.xml to report on events
    logger.info(
        "cache event {} {} {}",
        cacheEvent.getKey(),
        cacheEvent.getOldValue(),
        cacheEvent.getNewValue());
  }
}
