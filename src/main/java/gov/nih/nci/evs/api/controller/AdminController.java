package gov.nih.nci.evs.api.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.configuration.CacheConfiguration;
import io.swagger.annotations.Api;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Admin endpoints", hidden = true)
public class AdminController {
  
  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
 
  @Autowired
  CacheConfiguration cacheConfig;
  
  @Autowired
  Environment env;
  
  @SuppressWarnings("rawtypes")
  @RequestMapping(method = RequestMethod.DELETE, value = "/admin/cache")
  @ApiIgnore
  public ResponseEntity clearCache(@RequestParam(name = "key", required = true) final String key) {
    
    String adminKey = env.getProperty("nci.evs.application.adminKey").toString();
    
    if (!adminKey.equals(key)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    cacheConfig.evictAll();
    return ResponseEntity.noContent().build();
  }
}
