package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetricDB;
import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptByLabel;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags= "Documentation endpoints")
public class DocumentationController {

	private static final Logger log = LoggerFactory.getLogger(DocumentationController.class);
	
	@Autowired
	SparqlQueryManagerService sparqlQueryManagerService;

	@ApiOperation(value = "Gets all the named graphs of the specified triple store", response = String.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the graph names"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDB
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/graphnames", produces = "application/json")
	public @ResponseBody List<String> getGraphNames(@RequestParam("db") Optional<String>db , HttpServletRequest request)
			throws IOException {
		
        String dbType = db.orElse("monthly");
		List<String> graphNames= sparqlQueryManagerService.getAllGraphNames(dbType);
		return graphNames;
	}

	@ApiOperation(value = "Gets version information", response = EvsVersionInfo.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the version information"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDB
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/versionInfo", produces = "application/json")
	public @ResponseBody EvsVersionInfo getVersionInfo(@RequestParam("db") Optional<String>db)
			throws IOException {
		
        String dbType = db.orElse("monthly");
		EvsVersionInfo versionInfo = sparqlQueryManagerService.getEvsVersionInfo(dbType);
		return versionInfo;
	}
	
	@ApiOperation(value = "Get a list of all the properties with its details", response = EvsConcept.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved all the properties"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/properties", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	public @ResponseBody List<EvsConcept> getAllProperties(@RequestParam("db") Optional<String> db,
	        @RequestParam("fmt") Optional<String> fmt )
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <EvsConcept> properties = sparqlQueryManagerService.getAllProperties(dbType, format);
		return properties;
	}	

	@ApiOperation(value = "Get a list of all the properties with basic information", response = EvsConcept.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved all the properties"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/propertiesList", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	public @ResponseBody List<EvsProperty> getAllPropertiesList(@RequestParam("db") Optional<String> db,
	        @RequestParam("fmt") Optional<String> fmt )
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <EvsProperty> properties = sparqlQueryManagerService.getAllPropertiesList(dbType, format);
		return properties;
	}	
	
	
	
	
	@ApiOperation(value = "Get full details for the specified property", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the details of the specified property"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/property/{code}", produces = "application/json")
	public @ResponseBody EvsConcept getEvsProperty(@PathVariable(value = "code") String code,
			@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt,
			HttpServletResponse response) throws IOException {
        String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		EvsConcept evsConcept = null;
		if (format.equals("byLabel")) {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByLabel(code, dbType);
		} else {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByCode(code, dbType);
		}
		return evsConcept;
	}
	
	@ApiOperation(value = "Get a list of all the associations with its details", response = EvsConcept.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved all the associations"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/associations", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	public @ResponseBody List<EvsConcept> getAllAssociations(@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt)
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <EvsConcept> associations = sparqlQueryManagerService.getAllAssociations(dbType, format);
		return associations;
	}	
	
	@ApiOperation(value = "Get full details for the specified association code", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the details of the specified association code"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/association/{code}", produces = "application/json")
	public @ResponseBody EvsConcept getEvsAssociation(@PathVariable(value = "code") String code,
			@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt,
			HttpServletResponse response) throws IOException {
        String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		EvsConcept evsConcept = null;
		if (format.equals("byLabel")) {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByLabel(code, dbType);
		} else {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByCode(code, dbType);
		}
		return evsConcept;
	}
	
	@ApiOperation(value = "Get a list of all the roles with its details", response = EvsConcept.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved all the roles"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/roles", produces = "application/json")
	public @ResponseBody List<EvsConcept> getAllRoles(@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt)
			throws IOException {
		String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		List <EvsConcept> roles = sparqlQueryManagerService.getAllRoles(dbType, format);
		return roles;
	}	
	
	@ApiOperation(value = "Get full details for the specified role code", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the details of the specified role code"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query"),
		@ApiImplicitParam(name = "fmt", value = "Specify either 'byLabel' or 'byCode', if not specified defaults to 'byLabel'",
		        required = false, dataType = "string", paramType = "query")
	})
	@RecordMetricDBFormat
	@RequestMapping(method = RequestMethod.GET, value = "/documentation/role/{code}", produces = "application/json")
	public @ResponseBody EvsConcept getEvsRole(@PathVariable(value = "code") String code,
			@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt,
			HttpServletResponse response) throws IOException {
        String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		EvsConcept evsConcept = null;
		if (format.equals("byLabel")) {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByLabel(code, dbType);
		} else {
			evsConcept = sparqlQueryManagerService.getEvsPropertyByCode(code, dbType);
		}
		return evsConcept;
	}
	
	
	
	


}
