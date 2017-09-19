package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
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
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("api/v1")
public class EvsController {
    
	
	@Autowired
 	StardogProperties stardogProperties;

   @Autowired
   SparqlQueryManagerService sparqlQueryManagerService;
   
   
   
   @ApiOperation(value = "Get details on the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the EVS Details"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}",produces = "application/json")
    public @ResponseBody EvsConcept getEvsConceptDetail(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		EvsConcept evsConcept = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{			
			evsConcept = sparqlQueryManagerService.getEvsConceptDetail(conceptCode);
		}
    	return evsConcept;
    }
   
   @ApiOperation(value = "Get relationships on the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the EVS Relationships"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/relationships",produces = "application/json")
    public @ResponseBody EvsRelationships getEvsRelationships(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		EvsConcept evsConcept = null;
		EvsRelationships relationships = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			relationships = sparqlQueryManagerService.getEvsRelationships(conceptCode);
		}
    	return relationships;
    }
	
   @ApiOperation(value = "Get different paths of the specified concept to the root path", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/pathToRoot",produces = "application/json")
    public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		EvsConcept evsConcept = null;
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			paths = sparqlQueryManagerService.getPathToRoot(conceptCode);
		}
    	return paths;
    }

   @ApiOperation(value = "Get different paths of the specified concept to the specified parent path", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the EVS Paths"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/pathToParent/{parentConceptCode}",produces = "application/json")
    public @ResponseBody Paths getPathToRoot(@PathVariable(value = "conceptCode") String conceptCode ,@PathVariable(value = "parentConceptCode") String parentConceptCode,HttpServletResponse response) throws IOException{
		EvsConcept evsConcept = null;
		Paths paths = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			paths = sparqlQueryManagerService.getPathToParent(conceptCode,parentConceptCode);
		}
    	return paths;
    }
	
   @ApiOperation(value = "Get properties of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the properties of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/properties",produces = "application/json")
    public @ResponseBody List <EvsProperty> getPropertiesForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException {
		List <EvsProperty> evsProperties = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			evsProperties = sparqlQueryManagerService.getEvsProperties(conceptCode);
		}
    	return evsProperties;
    }
	
   @ApiOperation(value = "Get axioms of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the axioms of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/axioms",produces = "application/json")
    public @ResponseBody List <EvsAxiom> getAxiomsForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException {
		List <EvsAxiom> evsAxioms = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			evsAxioms = sparqlQueryManagerService.getEvsAxioms(conceptCode);
		}
    	return evsAxioms;
    }
	
   @ApiOperation(value = "Get subconcepts of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the subconcepts of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/subconcepts",produces = "application/json")
    public @ResponseBody List <EvsSubconcept> getSubconceptsForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException {
		List <EvsSubconcept> evsSubconcepts = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
    	    evsSubconcepts = sparqlQueryManagerService.getEvsSubconcepts(conceptCode);
		}
    	return evsSubconcepts;
    }

   @ApiOperation(value = "Get superconcepts of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the superconcepts of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/superconcepts",produces = "application/json")
    public @ResponseBody List <EvsSuperconcept> getSuperconceptsForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		List <EvsSuperconcept> evsSuperconcepts = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			evsSuperconcepts = sparqlQueryManagerService.getEvsSuperconcepts(conceptCode);
		}
    	return evsSuperconcepts;
    }

   @ApiOperation(value = "Get associations of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the associations of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/associations",produces = "application/json")
    public @ResponseBody List <EvsAssociation> getAssociationsForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		List <EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
    	     evsAssociations = sparqlQueryManagerService.getEvsAssociations(conceptCode);
		}
    	return evsAssociations;
    }

   @ApiOperation(value = "Get inverse associations of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the inverse associations of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/inverseassociations",produces = "application/json")
    public @ResponseBody List <EvsAssociation> getInverseAssociationsForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		List <EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
    	 evsAssociations = sparqlQueryManagerService.getEvsInverseAssociations(conceptCode);
		}
    	return evsAssociations;
    }

   @ApiOperation(value = "Get roles of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the roles of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/roles",produces = "application/json")
    public @ResponseBody List <EvsAssociation> getRolesForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		List <EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
			evsAssociations = sparqlQueryManagerService.getEvsRoles(conceptCode);
		}
    	return evsAssociations;
    }

   @ApiOperation(value = "Get inverse roles of the specified concept", response = EvsConcept.class)
   @ApiResponses(value = {
	        @ApiResponse(code = 200, message = "Successfully retrieved the inverse roles of the concept"),
	        @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
	        @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
	        @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
	}
	)
	@RequestMapping(method = RequestMethod.GET, value = "/ctrp/concept/{conceptCode}/inverseroles",produces = "application/json")
    public @ResponseBody List <EvsAssociation> getInverseRolesForCode(@PathVariable(value = "conceptCode") String conceptCode,HttpServletResponse response) throws IOException{
		List <EvsAssociation> evsAssociations = null;
		if (!sparqlQueryManagerService.checkConceptExists(conceptCode)){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The concept code " + conceptCode + " is invalid.");
		} else{
    	    evsAssociations = sparqlQueryManagerService.getEvsInverseRoles(conceptCode);
		}
    	return evsAssociations;
    }
}