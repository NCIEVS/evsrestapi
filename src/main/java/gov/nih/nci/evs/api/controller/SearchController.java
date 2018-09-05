package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptFull;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSubconcept;
import gov.nih.nci.evs.api.model.evs.EvsSuperconcept;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.QueryBuilderServiceImpl;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;
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
public class SearchController {

	private static final Logger log = LoggerFactory.getLogger(SearchController.class);

	@Autowired
	StardogProperties stardogProperties;

	@Autowired
	ElasticSearchService elasticSearchService;

	
	@RequestMapping(method = RequestMethod.GET, value = "/concept/elasticsearch", produces = "application/json")
	public @ResponseBody String elasticsearch(
			@ModelAttribute FilterCriteriaElasticFields filterCriteriaElasticFields,BindingResult bindingResult,HttpServletResponse response ) throws IOException {
		if (bindingResult.hasErrors()) {
			log.debug("Error " + bindingResult.getObjectName());
		    List<FieldError> errors= bindingResult.getFieldErrors();
		    String errorMessage = "";
		    for (FieldError error:errors) {
		    	log.debug("field name :" + error.getField());
		    	log.debug("Error Code :" + error.getCode());
		    	String newlinetest = System.getProperty("line.separator");
		    	if (error.getCode().equalsIgnoreCase("typeMismatch")) {
		    		errorMessage = errorMessage + "Could not convert the value of the field " + error.getField() + " to the expected type. Details: " + error.getDefaultMessage() + ". " ;
		    	}
		    	
		    }
		    int statusCode = HttpServletResponse.SC_BAD_REQUEST;	       
	        log.error("returning status code " + statusCode + " with error message " + errorMessage);
	        response.sendError(statusCode, errorMessage);
	        return "";
		}
		String result = "";
		String queryTerm = filterCriteriaElasticFields.getTerm();
		if (queryTerm == null) {
			return null;
		}
		queryTerm = escapeLuceneSpecialCharacters(queryTerm);
		filterCriteriaElasticFields.setTerm(queryTerm);
		log.debug("Term/Partial Term - " + filterCriteriaElasticFields.getTerm());		
		log.debug("Biomarker - " + filterCriteriaElasticFields.getBiomarker());
		log.debug("Disease -" + filterCriteriaElasticFields.getDisease());
		log.debug("Type - " + filterCriteriaElasticFields.getType());
		log.debug("From Record - " + filterCriteriaElasticFields.getFromRecord());
		log.debug("Page size - " + filterCriteriaElasticFields.getPageSize());
		
		log.debug("Format - " + filterCriteriaElasticFields.getFormat());
		if (filterCriteriaElasticFields.getReturnProperties() != null) {
			for (String returnField : filterCriteriaElasticFields.getReturnProperties()) {
				log.debug("return field - " + returnField);
			}
		}
		if (filterCriteriaElasticFields.getProperty() != null) {
			for (String returnField : filterCriteriaElasticFields.getProperty()) {
				log.debug("property - " + returnField);
			}
		}
		//Set default values NA for disease and biomarker
		if (filterCriteriaElasticFields.getBiomarker() == null)
			filterCriteriaElasticFields.setBiomarker("NotSet");
		if (filterCriteriaElasticFields.getDisease() == null)
			filterCriteriaElasticFields.setDisease("NotSet");
		try {
		  result = elasticSearchService.elasticsearch(filterCriteriaElasticFields);
		}catch(IOException exception) {
			  int statusCode = HttpServletResponse.SC_BAD_REQUEST;
	          String errorMessage = exception.getMessage();
	          log.error("returning status code " + statusCode + " with error message " + errorMessage);
	          response.sendError(statusCode, errorMessage);
		}catch(HttpClientErrorException httpClientErrorException) {
			 int statusCode = httpClientErrorException.getStatusCode().value();
	          String errorMessage = httpClientErrorException.getMessage();
	          log.error("returning status code " + statusCode + " with error message " + errorMessage);
	          response.sendError(statusCode, errorMessage);
		}catch(Exception e) {
			log.error(e.getMessage(), e);
			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	        String errorMessage = e.getMessage();
	        log.error("returning status code " + statusCode + " with error message " + errorMessage);
	        response.sendError(statusCode, errorMessage);
		}
		return result;
	}

	private String escapeLuceneSpecialCharacters(String before) {
		String patternString = "([+:!~*?/\\-/{}\\[\\]\\(\\)\\^\\\"])";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(before);
		StringBuffer buf = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(buf, before.substring(matcher.start(), matcher.start(1)) + "\\\\" + "\\\\"
					+ matcher.group(1) + before.substring(matcher.end(1), matcher.end()));
		}
		String after = matcher.appendTail(buf).toString();
		return after;
	}

}