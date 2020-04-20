package gov.nih.nci.evs.api.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.service.ElasticSearchLoadService;
import gov.nih.nci.evs.api.support.LoadConfig;
import io.swagger.annotations.Api;

/**
 * 
 * 
 * @author Arun
 *
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "ElasticSearch load endpoint")
public class ElasticSearchLoadController {
  
  @Autowired
  ElasticSearchLoadService loadService;
  
  @RequestMapping(method = RequestMethod.GET, value = "/load/{terminology}",
      produces = "application/json")
  public ResponseEntity searchSingleTerminology(
    @PathVariable(value = "terminology") final String terminology,
    @ModelAttribute LoadConfig config)
    throws IOException {
    loadService.load(config);
    
    return ResponseEntity.ok().build();
  }
  
}
