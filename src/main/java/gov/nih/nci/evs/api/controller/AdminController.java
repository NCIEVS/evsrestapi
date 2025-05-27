package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.configuration.CacheConfiguration;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Admin endpoints")
@Hidden
public class AdminController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

  @Autowired CacheConfiguration cacheConfig;

  @Autowired Environment env;

  /**
   * Clear cache.
   *
   * @param key the key
   * @return the response entity
   */
  @SuppressWarnings("rawtypes")
  @DeleteMapping("/admin/cache")
  @Hidden
  public ResponseEntity clearCache(@RequestParam(name = "key", required = true) final String key) {

    String adminKey = env.getProperty("nci.evs.application.adminKey").toString();

    if (!adminKey.equals(key)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    cacheConfig.evictAll();
    TerminologyUtils.clearCache();
    return ResponseEntity.noContent().build();
  }
}
