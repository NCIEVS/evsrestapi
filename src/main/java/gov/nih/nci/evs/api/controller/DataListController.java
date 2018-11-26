package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@ApiIgnore
public class DataListController {

	@Autowired
	SparqlQueryManagerService sparqlQueryManagerService;
	
	@RequestMapping(method = RequestMethod.GET, value = "/propertiesList", produces = "application/json")	
	public @ResponseBody List<String> getAllPropertiesForDocumentation(@RequestParam("db") Optional<String> db,
	        @RequestParam("fmt") Optional<String> fmt )
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <String> properties = sparqlQueryManagerService.getAllPropertiesForDocumentation(dbType);
		return properties;
	}	

	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/associationsList", produces = "application/json")	
	public @ResponseBody List<String> getAllAssociationsForDocumentation(@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt)
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <String> associations = sparqlQueryManagerService.getAllAssociationsForDocumentation(dbType);
		return associations;
	}	

	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/rolesList", produces = "application/json")
	public @ResponseBody List<String> getAllRolesForDocumentation(@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt)
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <String> roles = sparqlQueryManagerService.getAllRolesForDocumentation(dbType);
		return roles;
	}	
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/contributingSourcesList", produces = "application/json")
	public @ResponseBody List<String> getContributingSourcesForDocumentation()
		throws IOException {
		
		List<String> contributingSources = sparqlQueryManagerService.getContributingSourcesForDocumentation();
		return contributingSources;
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/conceptStatuesList", produces = "application/json")
	public @ResponseBody List<String> getConceptStatusForDocumentation()
		throws IOException {
		
		List<String> contributingSources = sparqlQueryManagerService.getConceptStatusForDocumentation();
		return contributingSources;
	}
}
