package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptByLabel;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags= "Concept endpoints")
public class EvsController {

	@Autowired
	StardogProperties stardogProperties;

	@Autowired
	SparqlQueryManagerService sparqlQueryManagerService;

	

	/*
	 * Controllers for Concept Information 
	 */
	@ApiOperation(value = "Get full details for the specified concept", response = EvsConceptByLabel.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Concept by Label Details"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}", produces = "application/json")
	public @ResponseBody EvsConcept getEvsConcept(@PathVariable(value = "conceptCode") String conceptCode,
			@RequestParam("db") Optional<String> db,
			@RequestParam("fmt") Optional<String> fmt,
			HttpServletResponse response) throws IOException {
        String dbType = db.orElse("monthly");
        String format = fmt.orElse("byLabel");
		EvsConcept evsConcept = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode, dbType)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			if (format.equals("byLabel")) {
				evsConcept = sparqlQueryManagerService.getEvsConceptByLabel(conceptCode, dbType);
			} else {
				evsConcept = sparqlQueryManagerService.getEvsConceptByCode(conceptCode, dbType);
			}
		}
		return evsConcept;
	}
	
	
	/*
	 * Controllers for Hierarchy Information 
	 */
	@ApiOperation(value = "Get all the root nodes", response = HierarchyNode.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the children of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/rootNodes", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	public @ResponseBody List<HierarchyNode> getRootNodes(@RequestParam("db") Optional<String> db) throws IOException {
		String dbType = db.orElse("monthly");
		List <HierarchyNode> nodes = sparqlQueryManagerService.getRootNodes(dbType);
		return nodes;
	}


	@ApiOperation(value = "Get the children of the specified concept", response = HierarchyNode.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the children of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/childNodes", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	public @ResponseBody List<HierarchyNode> getChildNodes(
			@PathVariable(value = "conceptCode") String conceptCode,
			@RequestParam("db") Optional<String> db) throws IOException {
		String dbType = db.orElse("monthly");
		List <HierarchyNode> nodes = sparqlQueryManagerService.getChildNodes(conceptCode, dbType);
		return nodes;
	}
	
	@ApiOperation(value = "Get the children of the specified concept, for multiple levels", response = HierarchyNode.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the children of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/childNodes/{maxLevel}", produces = "application/json")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	public @ResponseBody List<HierarchyNode> getChildNodesLevel(
			@PathVariable(value = "conceptCode") String conceptCode,
			@PathVariable(value = "maxLevel") int maxLevel,
			@RequestParam("db") Optional<String> db) throws IOException {
		String dbType = db.orElse("monthly");
		List <HierarchyNode> nodes = sparqlQueryManagerService.getChildNodes(conceptCode, maxLevel, dbType);
		return nodes;
	}
	
	@ApiOperation(value = "Get different paths of the specified concept to the root path", response = Paths.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathToRoot", produces = "application/json")
	public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode,
			@RequestParam("db") Optional<String> db,
			HttpServletResponse response) throws IOException {
		String dbType = db.orElse("monthly");
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode, dbType)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			paths = sparqlQueryManagerService.getPathToRoot(conceptCode, dbType);
		}
		return paths;
	}

	@ApiOperation(value = "Get different paths of the specified concept to the specified parent path", response = Paths.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathToParent/{parentConceptCode}", produces = "application/json")
	public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode,
			@PathVariable(value = "parentConceptCode") String parentConceptCode,
			@RequestParam("db") Optional<String> db,
			HttpServletResponse response)
			throws IOException {
		String dbType = db.orElse("monthly");
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode, dbType)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			paths = sparqlQueryManagerService.getPathToParent(conceptCode, parentConceptCode, dbType);
		}
		return paths;
	}
	
	@ApiOperation(value = "Get the path(s) to specified concept from the root nodes", response = HierarchyNode.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@ApiImplicitParams({
		@ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'",
				required = false, dataType = "string", paramType = "query")
	})
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathInHierarchy", produces = "application/json")
	public @ResponseBody List<HierarchyNode> getPathInHierarchy(
			@PathVariable(value = "conceptCode") String conceptCode,
			@RequestParam("db") Optional<String> db) throws IOException {
		String dbType = db.orElse("monthly");
		List <HierarchyNode> nodes = sparqlQueryManagerService.getPathInHierarchy(conceptCode, dbType);
		return nodes;
	}	
}