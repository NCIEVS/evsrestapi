package gov.nih.nci.evs.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.LoadService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.LoadConfig;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;

/**
 * 
 * 
 * @author Arun
 *
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Elasticsearch load endpoint")
public class LoadController {
  
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;
  
  @Autowired
  LoadService loadService;
  
  @RequestMapping(method = RequestMethod.PUT, value = "/load/{terminology}",
      produces = "application/json")
  public ResponseEntity load(
    @PathVariable(value = "terminology") final String terminology,
    @ModelAttribute LoadConfig config)
    throws Exception {
    
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    loadService.load(config, term);
    
    return ResponseEntity.ok().build();
  }
  
}
