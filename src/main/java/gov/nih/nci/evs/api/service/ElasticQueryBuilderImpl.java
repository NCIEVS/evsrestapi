package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.properties.ElasticQueryProperties;
import gov.nih.nci.evs.api.service.exception.InvalidParameterValueException;
import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;

@Service
public class ElasticQueryBuilderImpl implements ElasticQueryBuilder {

	private static final Logger log = LoggerFactory.getLogger(ElasticQueryBuilderImpl.class);

	private HashMap<String, String> returnFieldMap;
	private HashMap<String, String> propertyToQuery;
	private HashMap<String, String> propertyToQueryExact;
	private HashMap<String, String> propertyToQueryContains;

	@Autowired
	private ElasticQueryProperties elasticQueryProperties;

	@PostConstruct
	public void postInit() throws IOException {
		// User Input to Return field
		returnFieldMap = new HashMap<String, String>();
		returnFieldMap.put("code", "code");
		returnFieldMap.put("label", "label");
		returnFieldMap.put("displayname", "displayName");
		returnFieldMap.put("preferredname", "preferredName");
		returnFieldMap.put("definitions", "definitions");
		returnFieldMap.put("definition", "definitions");
		returnFieldMap.put("semantictypes", "semanticTypes");
		returnFieldMap.put("semantictype", "semanticTypes");
		returnFieldMap.put("synonyms", "synonyms");
		returnFieldMap.put("synonym", "synonyms");
		returnFieldMap.put("additionalproperties", "additionalProperties");
		returnFieldMap.put("additionalproperty", "additionalProperties");
		returnFieldMap.put("property", "additionalProperties");
		returnFieldMap.put("superconcepts", "superconcepts");
		returnFieldMap.put("superconcept", "superconcepts");
		returnFieldMap.put("subconcepts", "subconcepts");
		returnFieldMap.put("subconcept", "subconcepts");
		returnFieldMap.put("roles", "roles");
		returnFieldMap.put("role", "roles");
		returnFieldMap.put("inverseroles", "inverseRoles");
		returnFieldMap.put("inverserole", "inverseRoles");
		returnFieldMap.put("associations", "associations");
		returnFieldMap.put("association", "associations");
		returnFieldMap.put("inverseassociations", "inverseAssociations");
		returnFieldMap.put("inverseassociation", "inverseAssociations");

		// user input for property to property value for query for and, or
		propertyToQuery = new HashMap<String, String>();
		propertyToQuery.put("p108", elasticQueryProperties.getP108Default());
		propertyToQuery.put("p107", elasticQueryProperties.getP107Default());
		propertyToQuery.put("p90", elasticQueryProperties.getP90Default());
		propertyToQuery.put("nhc0", elasticQueryProperties.getNHC0Default());
		propertyToQuery.put("p97", elasticQueryProperties.getP97Default());
		propertyToQuery.put("preferredname", elasticQueryProperties.getP108Default());
		propertyToQuery.put("displayname", elasticQueryProperties.getP107Default());
		propertyToQuery.put("synonym", elasticQueryProperties.getP90Default());
		propertyToQuery.put("code", elasticQueryProperties.getNHC0Default());
		propertyToQuery.put("defintion", elasticQueryProperties.getP97Default());

		// user input for property to property value for query for exact and startswith
		propertyToQueryExact = new HashMap<String, String>();
		propertyToQueryExact.put("p108", elasticQueryProperties.getP108Exact());
		propertyToQueryExact.put("p107", elasticQueryProperties.getP107Exact());
		propertyToQueryExact.put("p90", elasticQueryProperties.getP90Exact());
		propertyToQueryExact.put("nhc0", elasticQueryProperties.getNHC0Exact());
		propertyToQueryExact.put("p97", elasticQueryProperties.getP97Exact());
		propertyToQueryExact.put("preferredname", elasticQueryProperties.getP108Exact());
		propertyToQueryExact.put("displayname", elasticQueryProperties.getP107Exact());
		propertyToQueryExact.put("synonym", elasticQueryProperties.getP90Exact());
		propertyToQueryExact.put("code", elasticQueryProperties.getNHC0Exact());
		propertyToQueryExact.put("defintion", elasticQueryProperties.getP97Exact());

		// user input for property to property value for query for contains
		propertyToQueryContains = new HashMap<String, String>();
		propertyToQueryContains.put("p108", elasticQueryProperties.getP108Contains());
		propertyToQueryContains.put("p107", elasticQueryProperties.getP107Contains());
		propertyToQueryContains.put("p90", elasticQueryProperties.getP90Contains());
		propertyToQueryContains.put("nhc0", elasticQueryProperties.getNHC0Contains());
		propertyToQueryContains.put("p97", elasticQueryProperties.getP97Contains());
		propertyToQueryContains.put("preferredname", elasticQueryProperties.getP108Contains());
		propertyToQueryContains.put("displayname", elasticQueryProperties.getP107Contains());
		propertyToQueryContains.put("synonym", elasticQueryProperties.getP90Contains());
		propertyToQueryContains.put("code", elasticQueryProperties.getNHC0Contains());
		propertyToQueryContains.put("defintion", elasticQueryProperties.getP97Contains());

	}

	public String constructQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) throws IOException {
		Map valuesMap = new HashMap();
		// setting defaults

		if (filterCriteriaElasticFields.getType() == null) {
			filterCriteriaElasticFields.setType("contains");
		}
		String display = "";
		if (filterCriteriaElasticFields.getReturnProperties() == null
				|| (filterCriteriaElasticFields.getReturnProperties().size() <= 0)) {
			display = "short";
		} else {
			if (filterCriteriaElasticFields.getReturnProperties().get(0).equalsIgnoreCase("all")) {
				display = "all";
			}
			if (filterCriteriaElasticFields.getReturnProperties().get(0).equalsIgnoreCase("short")) {
				display = "short";
			}
		}

		if (filterCriteriaElasticFields.getPageSize() == null) {
			filterCriteriaElasticFields.setPageSize(10);
		}

		if (filterCriteriaElasticFields.getFromRecord() == null) {
			filterCriteriaElasticFields.setFromRecord(0);
		}

		if (filterCriteriaElasticFields.getFormat() == null) {
			filterCriteriaElasticFields.setFormat("raw");
		}

		// *******source fields replace******************
		String returnFields = "";
		String sourcefields = "";

		if (!display.equalsIgnoreCase("")) {
			if (display.equalsIgnoreCase("short"))
				returnFields = elasticQueryProperties.getShortsourcefields();
			else if (display.equalsIgnoreCase("all")) {
				returnFields = "true";
			} else {
				returnFields = elasticQueryProperties.getShortsourcefields();
			}
		} else {
			returnFields = "[\n";
			returnFields = returnFields + elasticQueryProperties.getDefaultsourcefields();
			for (String field : filterCriteriaElasticFields.getReturnProperties()) {
				String fieldValue = this.returnFieldMap.get(field.toLowerCase());
				if (fieldValue == null) {
					throw new InvalidParameterValueException("Invalid Parameter value for returnProperties -" + field);

				} else {
					if (!(fieldValue.equalsIgnoreCase("label") || fieldValue.equalsIgnoreCase("code")))
						returnFields = returnFields + "\"" + fieldValue + "\",";
				}

				log.debug("returnFields - " + returnFields);

			}
			returnFields = returnFields.substring(0, returnFields.length() - 1);
			log.debug("returnFields - " + returnFields);
			returnFields = returnFields + "]";
		}
		sourcefields = returnFields;
		valuesMap.put("sourcefields", sourcefields);

		// *****fromRecord****************
		String fromRecord = filterCriteriaElasticFields.getFromRecord().toString();

		valuesMap.put("fromRecord", fromRecord);

		// ***********pageSize*****************
		String pageSize = filterCriteriaElasticFields.getPageSize().toString();

		valuesMap.put("pageSize", pageSize);

		// *******term and operator replace*************
		String term = filterCriteriaElasticFields.getTerm();
		if ((term == null) || term.equalsIgnoreCase("")) {
			throw new InvalidParameterValueException(
					"Term Parameter is a required field and value for term cannot be null");
		}
		String operator = "";
		if (filterCriteriaElasticFields.getType() != null) {
			if (filterCriteriaElasticFields.getType() == "OR") {
				operator = "";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("AND")) {
				operator = "\"default_operator\":\"AND\",\n";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("phrase")) {

				operator = "\"type\":\"phrase_prefix\",\n";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				operator = "";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
				operator = "";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("fuzzy")) {
				operator = "";
				term = term + "~";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")) {

				operator = "\"type\":\"phrase_prefix\",\n";
			}
		} else {
			operator = "";
		}
		valuesMap.put("operator", operator);
		valuesMap.put("term", term);

		// **********fields to search in replace***************
		String fields = "";
		if (filterCriteriaElasticFields.getProperty() != null && filterCriteriaElasticFields.getProperty().size() > 0) {
			List<String> properties = filterCriteriaElasticFields.getProperty();
			if (properties != null && properties.size() > 0) {
				fields = "[\n";
				for (String property : properties) {
					String value = "";
					if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
							|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
						value = this.propertyToQueryExact.get(property.toLowerCase());

					} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
						value = this.propertyToQueryContains.get(property.toLowerCase());
					} else {
						value = this.propertyToQuery.get(property.toLowerCase());
					}
					if (value == null) {
						throw new InvalidParameterValueException(
								"Invalid Parameter value for property field. Rejected value - " + property);

					} else
						fields = fields + value;
				}
				fields = fields.substring(0, fields.length() - 2);
				fields = fields + "]\n";

			}
		} else if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
				|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
			fields = elasticQueryProperties.getExactstartswithfields();
		} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
			fields = elasticQueryProperties.getContainsfields();

		} else {
			fields = elasticQueryProperties.getAndorfields();
		}
		valuesMap.put("fields", fields);

		// ***********replace highlight fields************************
		String highlightFields = "";
		if (!filterCriteriaElasticFields.getFormat().equalsIgnoreCase("clean")) {
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
					|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
				highlightFields = elasticQueryProperties.getHighlightexact();

			} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				highlightFields = elasticQueryProperties.getHighlightcontains();

			} else {
				highlightFields = elasticQueryProperties.getHighlightandor();
			}
			valuesMap.put("highlightFields", highlightFields);
		}

		// **********************filter replace********************
		String filter = this.constructFilterQuery(filterCriteriaElasticFields);
		valuesMap.put("filter", filter);

		// *********get main query
		String templateString = getMainQuery(filterCriteriaElasticFields);

		// replace values
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String resolvedString = sub.replace(templateString);

		log.debug("query string - " + resolvedString);
		return resolvedString;
	}

	private String constructFilterQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String filter = "";
		boolean diseaseFilter = false;
		boolean biomarkerFilter = false;

		if (filterCriteriaElasticFields.getDisease().equalsIgnoreCase("true")
				|| filterCriteriaElasticFields.getDisease().equalsIgnoreCase("false")) {
			diseaseFilter = true;
		}
		if (filterCriteriaElasticFields.getBiomarker().equalsIgnoreCase("true")
				|| filterCriteriaElasticFields.getBiomarker().equalsIgnoreCase("false")) {
			biomarkerFilter = true;
		}
		if (diseaseFilter || biomarkerFilter) {
			filter = filter + ",\n";
			filter = filter + "\"filter\":{\n";
			filter = filter + "\"bool\":{\n";
			filter = filter + "\"must\":[\n";

			if (filterCriteriaElasticFields.getDisease().equalsIgnoreCase("true")) {
				filter = filter + "{\"term\":{\"isDisease\":true}},\n";
			} else if (filterCriteriaElasticFields.getDisease().equalsIgnoreCase("false")) {
				filter = filter + "{\"term\":{\"isDisease\":false}},\n";
			}

			if (filterCriteriaElasticFields.getBiomarker().equalsIgnoreCase("true")) {
				filter = filter + "{\"term\":{\"isBiomarker\":true}},\n";
			} else if (filterCriteriaElasticFields.getBiomarker().equalsIgnoreCase("false")) {
				filter = filter + "{\"term\":{\"isBiomarker\":false}},\n";
			}

			filter = filter.substring(0, filter.length() - 2);
			filter = filter + "]\n";
			filter = filter + "}\n";
			filter = filter + "}\n";
		}
		return filter;
	}

	private String getMainQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String query = "";
		if (filterCriteriaElasticFields.getFormat().equalsIgnoreCase("clean")) {
			query = elasticQueryProperties.getMainQueryWithoutHighlights();
		} else
			query = elasticQueryProperties.getMainQuery();

		return query;

	}
}
