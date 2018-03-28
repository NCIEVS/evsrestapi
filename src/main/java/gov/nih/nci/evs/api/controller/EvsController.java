package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSubconcept;
import gov.nih.nci.evs.api.model.evs.EvsSuperconcept;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.FilterCriteriaFields;
import gov.nih.nci.evs.api.support.FilterParameter;
import gov.nih.nci.evs.api.support.MatchedConcept;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("${nci.evs.application.contextPath}")
public class EvsController {

	@Autowired
	StardogProperties stardogProperties;

	@Autowired
	SparqlQueryManagerService sparqlQueryManagerService;

	@ApiOperation(value = "Get details on the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Details"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}", produces = "application/json")
	public @ResponseBody EvsConcept getEvsConceptDetail(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		EvsConcept evsConcept = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsConcept = sparqlQueryManagerService.getEvsConceptDetail(conceptCode);
		}
		return evsConcept;
	}

	@ApiOperation(value = "Get relationships on the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Relationships"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/relationships", produces = "application/json")
	public @ResponseBody EvsRelationships getEvsRelationships(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		EvsConcept evsConcept = null;
		EvsRelationships relationships = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			relationships = sparqlQueryManagerService.getEvsRelationships(conceptCode);
		}
		return relationships;
	}

	@ApiOperation(value = "Get different paths of the specified concept to the root path", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathToRoot", produces = "application/json")
	public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		EvsConcept evsConcept = null;
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			paths = sparqlQueryManagerService.getPathToRoot(conceptCode);
		}
		return paths;
	}

	@ApiOperation(value = "Get different paths of the specified concept to the specified parent path", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathToParent/{parentConceptCode}", produces = "application/json")
	public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode,
			@PathVariable(value = "parentConceptCode") String parentConceptCode, HttpServletResponse response)
			throws IOException {
		EvsConcept evsConcept = null;
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			paths = sparqlQueryManagerService.getPathToParent(conceptCode, parentConceptCode);
		}
		return paths;
	}

	@ApiOperation(value = "Get properties of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the properties of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/properties", produces = "application/json")
	public @ResponseBody List<EvsProperty> getPropertiesForCode(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		List<EvsProperty> evsProperties = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsProperties = sparqlQueryManagerService.getEvsProperties(conceptCode);
		}
		return evsProperties;
	}

	@ApiOperation(value = "Get axioms of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the axioms of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/axioms", produces = "application/json")
	public @ResponseBody List<EvsAxiom> getAxiomsForCode(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		List<EvsAxiom> evsAxioms = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsAxioms = sparqlQueryManagerService.getEvsAxioms(conceptCode);
		}
		return evsAxioms;
	}

	@ApiOperation(value = "Get subconcepts of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the subconcepts of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/subconcepts", produces = "application/json")
	public @ResponseBody List<EvsSubconcept> getSubconceptsForCode(
			@PathVariable(value = "conceptCode") String conceptCode, HttpServletResponse response) throws IOException {
		List<EvsSubconcept> evsSubconcepts = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsSubconcepts = sparqlQueryManagerService.getEvsSubconcepts(conceptCode);
		}
		return evsSubconcepts;
	}

	@ApiOperation(value = "Get superconcepts of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the superconcepts of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/superconcepts", produces = "application/json")
	public @ResponseBody List<EvsSuperconcept> getSuperconceptsForCode(
			@PathVariable(value = "conceptCode") String conceptCode, HttpServletResponse response) throws IOException {
		List<EvsSuperconcept> evsSuperconcepts = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsSuperconcepts = sparqlQueryManagerService.getEvsSuperconcepts(conceptCode);
		}
		return evsSuperconcepts;
	}

	@ApiOperation(value = "Get associations of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the associations of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/associations", produces = "application/json")
	public @ResponseBody List<EvsAssociation> getAssociationsForCode(
			@PathVariable(value = "conceptCode") String conceptCode, HttpServletResponse response) throws IOException {
		List<EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsAssociations = sparqlQueryManagerService.getEvsAssociations(conceptCode);
		}
		return evsAssociations;
	}

	@ApiOperation(value = "Get inverse associations of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse associations of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/inverseassociations", produces = "application/json")
	public @ResponseBody List<EvsAssociation> getInverseAssociationsForCode(
			@PathVariable(value = "conceptCode") String conceptCode, HttpServletResponse response) throws IOException {
		List<EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsAssociations = sparqlQueryManagerService.getEvsInverseAssociations(conceptCode);
		}
		return evsAssociations;
	}

	@ApiOperation(value = "Get roles of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully retrieved the roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/roles", produces = "application/json")
	public @ResponseBody List<EvsAssociation> getRolesForCode(@PathVariable(value = "conceptCode") String conceptCode,
			HttpServletResponse response) throws IOException {
		List<EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsAssociations = sparqlQueryManagerService.getEvsRoles(conceptCode);
		}
		return evsAssociations;
	}

	@ApiOperation(value = "Get inverse roles of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/inverseroles", produces = "application/json")
	public @ResponseBody List<EvsAssociation> getInverseRolesForCode(
			@PathVariable(value = "conceptCode") String conceptCode, HttpServletResponse response) throws IOException {
		List<EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else {
			evsAssociations = sparqlQueryManagerService.getEvsInverseRoles(conceptCode);
		}
		return evsAssociations;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/rootNodes", produces = "application/json")
	public @ResponseBody List<HierarchyNode> getRootNodes() throws IOException {
		List <HierarchyNode> nodes = sparqlQueryManagerService.getRootNodes();
		return nodes;
	}


	@ApiOperation(value = "Get the children of the specified concept", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/childNodes", produces = "application/json")
	public @ResponseBody List<HierarchyNode> getChildNodes(
			@PathVariable(value = "conceptCode") String conceptCode) throws IOException {
		List <HierarchyNode> nodes = sparqlQueryManagerService.getChildNodes(conceptCode);
		return nodes;
	}
	
	@ApiOperation(value = "Get the children of the specified concept, for multiple levels", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/childNodes/{maxLevel}", produces = "application/json")
	public @ResponseBody List<HierarchyNode> getChildNodesLevel(
			@PathVariable(value = "conceptCode") String conceptCode,
			@PathVariable(value = "maxLevel") int maxLevel) throws IOException {
		List <HierarchyNode> nodes = sparqlQueryManagerService.getChildNodes(conceptCode, maxLevel);
		return nodes;
	}

	@ApiOperation(value = "Get the path(s) to specified concept from the root nodes", response = EvsConcept.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/{conceptCode}/pathInHierarchy", produces = "application/json")
	public @ResponseBody List<HierarchyNode> getPathInHierarchy(
			@PathVariable(value = "conceptCode") String conceptCode) throws IOException {
		List <HierarchyNode> nodes = sparqlQueryManagerService.getPathInHierarchy(conceptCode);
		return nodes;
	}	

	/*@ApiOperation(value = "Search")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	@RequestMapping(method = RequestMethod.POST, value = "/concept/search", produces = "application/json")
	public @ResponseBody List<MatchedConcept> search(@RequestParam("searchTerm") String searchTerm,
			HttpServletResponse response) throws IOException {
		List<MatchedConcept> matchedConcepts = new ArrayList<MatchedConcept>();
		matchedConcepts = sparqlQueryManagerService.search(searchTerm, "");
		return matchedConcepts;
	}*/

	@ApiOperation(value = "Searches for the term or phrase in the RDF triple's object field.",
			      notes= ""
			    + "# Term\n"
			    + "\n"
			    + "The **term** paramater is used to enter the term or phrase to use in the search."
			    + "\n"
			    + "# Type\n"
				+ "The **type** parameter determines the kind of search to perform."
			    + "If no **type** parameter is specified the default is to do a word search within the field. "
				+ "All searches are case-insensitive."
			    + " Valid entries for **type** specifications are: phrase, AND, startswith, match, contains"
			    + "\n"
			    + "# Property\n"
			    + "The **property** parameter can be used to control which properties are searched. "
			    + "For example if *P107,P108* were passed in the **property** parameter only the "
			    + "P107 (Display Name) and the P108 (Preferred_Name) properties would be searched."
			    + "\n"
			    + "# Limit\n"
			    + "The **limit** pararmeter is use to restrict the number of results returned. "
			    + "The default is to return all results."
			    + "\n"
			    + "# Example Queries</strong>\n"
				+ "api/v1/ctrp/concept/search?term=Ultrasound - "
				+ "Search for the word Ultrasound."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=Ultrasound&limit=10 - "
				+ "Search for the word Ultrasound and limit the results to 10."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=Ultrasound Endoscopic - "
				+ "Search for the words Ultrasound <b>or</b> Endoscopic."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=mesangial phagocytic&type=AND - "
				+ "Search for the words mesangial <strong>AND</strong> phagocytic. "
				+ "\n\n"
				+ "api/v1/ctrp/concept/search?term=mesangial phagocytic&type=phrase - "
				+ "Search for the phrase 'mesangial phagocytic'."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=Endoscopic Ultrasound&type=startwith - "
				+ "Search for the words 'Endoscopic Ultrasound' at the beginning of a string."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=thrombo&type=contains - "
				+ "Search for the letters 'thrombo' anywhere within a word in the string."
			    + "\n\n"
				+ "api/v1/ctrp/concept/search?term=hromosom&type=contains&property=P107,108 - "
				+ "Search for the letters 'hromosome' anywhere within a word in the string. "
				+ "The search is also restricted to the RDF triples where the predicate is either P107 or P108."
			    + "\n"
			)
	/*
	@ApiOperation(value = "Searches for the term passed as a parameter to this endpoint. The type parameter value passed "
			+ " determines the kind of search that will take place. A list of comma separated property codes "
			+ "can be passed in as a value to the property parameter to "
			+ "restrict the property triples that the search will take place in. The search is case-insensitive. e.g. </br></br>"			
			+ "* <b>api/v1//ctrp/concept/search?term=Ultrasound Endoscopic.</b>  Here there is no value passed to the type and property parameter. This will default to a search of the words Ultrasound <b>or</b> Endoscopic in all of the property triples.</br></br>"
			+ "* <b>api/v1//ctrp/concept/search?term=Ultrasound.</b>  Here there is no value passed to the type and property parameter. This will default to a search of the word Ultrasound in all of the property triples.</br></br>"			
			+ "* <b>api/v1//ctrp/concept/search?term=mesangial phagocytic&type=AND</b> This will search for the words mesangial <b>and</b> phagocytic in all of the property triples.</br></br>"			
			+ "* <b>api/v1//ctrp/concept/search?term=mesangial phagocytic&type=phrase</b> This will search for the phrase 'mesangial phagocytic' in all of the property triples.</br></br>"
			+ "* <b>api/v1//ctrp/concept/search?term=Endoscopic Ultrasound&type=startswith</b> This will search for the properties that starts with the phrase 'Endoscopic Ultrasound'.</br></br>"
			+ "* <b>api/v1//ctrp/concept/search?term=Endoscopic Ultrasound&type=match</b> This will search for the properties that exactly matches the phrase 'Endoscopic Ultrasound'.</br></br>"
			+ "* <b>api/v1//ctrp/concept/search?term=thrombo&type=contains</b> This will search for the properties that contains a substring 'thrombo' in any of its words.</br></br>"
			+ "* <b>api/v1//ctrp/concept/search?term=hromosom&type=contains&property=P107,P108</b> This will search only in the properties P107 and P108 that conatins a substring 'thrombo' in any of its words.</br></br>"
			)
	*/
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 401, message = "You are not authorized to view the resource"),
			@ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
			@ApiResponse(code = 404, message = "The resource you were trying to reach is not found") })
	 @ApiImplicitParams({
	        @ApiImplicitParam(name = "term", value = "The term to be searched", required = true, dataType = "string", paramType = "query", defaultValue=""),
	        @ApiImplicitParam(name = "type", value = "The type can be contains,match,startswith,phrase,AND,fuzzy. If no type is specified , the search will be of the type OR", required = false, dataType = "string", paramType = "query", defaultValue=""),
	        @ApiImplicitParam(name = "limit", value = "Restrict the number of results returned to a specified number.", required = false, dataType = "string", paramType = "query", defaultValue=""),
	        @ApiImplicitParam(name = "property", value = "The list of comma separated properties.  Only the specified property triples will be searched. e.g P107,P108", required = false, dataType = "string", paramType = "query", defaultValue="")
	      })
	@RequestMapping(method = RequestMethod.GET, value = "/concept/search", produces = "application/json")
	public @ResponseBody List<MatchedConcept> search(@ModelAttribute FilterCriteriaFields filterCriteriaFields)
			throws IOException {
		String queryTerm = filterCriteriaFields.getTerm();
		queryTerm = escapeLuceneSpecialCharacters(queryTerm);
		filterCriteriaFields.setTerm(queryTerm);
		List<MatchedConcept> matchedConcepts = new ArrayList<MatchedConcept>();
		matchedConcepts = sparqlQueryManagerService.search(filterCriteriaFields);
		return matchedConcepts;
	}
	
	private String escapeLuceneSpecialCharacters(String before) {
		String patternString = "([+:!~*?/\\-/{}\\[\\]\\(\\)\\^\\\"])";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(before);
		StringBuffer buf = new StringBuffer();
        while(matcher.find()) {
          matcher.appendReplacement(buf,
                before.substring(matcher.start(),matcher.start(1)) +
                "\\\\" + "\\\\" + matcher.group(1)
                + before.substring(matcher.end(1),matcher.end()));
        }
        String after = matcher.appendTail(buf).toString();
        return after;
	}

}