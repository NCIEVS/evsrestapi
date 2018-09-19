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
	private HashMap<String, String> associationToQuery;

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
		returnFieldMap.put("conceptStatus", "conceptStatus");
		returnFieldMap.put("conceptstatus", "conceptStatus");

		/// associations
		associationToQuery = new HashMap<String, String>();
		associationToQuery.put("role_has_domain", "Role_Has_Domain");
		associationToQuery.put("has_cdrh_parent", "Has_CDRH_Parent");
		associationToQuery.put("has_nichd_parent", "Has_NICHD_Parent");
		associationToQuery.put("has_data_element", "Has_Data_Element");
		associationToQuery.put("related_to_genetic_biomarker", "Related_To_Genetic_Biomarker");
		associationToQuery.put("neoplasm_has_special_category", "Neoplasm_Has_Special_Category");
		associationToQuery.put("has_ctcae_5_parent", "Has_CTCAE_5_Parent");
		associationToQuery.put("role_has_range", "Role_Has_Range");
		associationToQuery.put("role_has_parent", "Role_Has_Parent");
		associationToQuery.put("qualifier_applies_To", "Qualifier_Applies_To");
		associationToQuery.put("has_salt_form", "Has_Salt_Form");
		associationToQuery.put("has_free_acid_or_base_form", "Has_Free_Acid_Or_Base_Form");
		associationToQuery.put("has_target", "Has_Target");
		associationToQuery.put("concept_in_subset", "Concept_In_Subset");
		associationToQuery.put("is_related_to_endogenous_product", "Is_Related_To_Endogenous_Product");

		associationToQuery.put("a1", "Role_Has_Domain");
		associationToQuery.put("a10", "Has_CDRH_Parent");
		associationToQuery.put("a11", "Has_NICHD_Parent");
		associationToQuery.put("a12", "Has_Data_Element");
		associationToQuery.put("a13", "Related_To_Genetic_Biomarker");
		associationToQuery.put("a14", "Neoplasm_Has_Special_Category");
		associationToQuery.put("a15", "Has_CTCAE_5_Parent");
		associationToQuery.put("a2", "Role_Has_Range");
		associationToQuery.put("a3", "Role_Has_Parent");
		associationToQuery.put("a4", "Qualifier_Applies_To");
		associationToQuery.put("a5", "Has_Salt_Form");
		associationToQuery.put("a6", "Has_Free_Acid_Or_Base_Form");
		associationToQuery.put("a7", "Has_Target");
		associationToQuery.put("a8", "Concept_In_Subset");
		associationToQuery.put("a9", "Is_Related_To_Endogenous_Product");

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
		propertyToQuery.put("definition", elasticQueryProperties.getP97Default());
		propertyToQuery.put("conceptstatus", elasticQueryProperties.getStatusDefault());

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
		propertyToQueryExact.put("conceptstatus", elasticQueryProperties.getStatusDefault());

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
		propertyToQueryContains.put("conceptstatus", elasticQueryProperties.getStatusDefault());

	}

	public String constructQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) throws IOException {
		Map valuesMap = new HashMap();
		// setting defaults
		boolean synonymSource = false;
		boolean definitionSource = false;
		boolean associationSearch = false;

		if (filterCriteriaElasticFields.getType() == null) {
			filterCriteriaElasticFields.setType("contains");
		}
		String display = "";
		if (filterCriteriaElasticFields.getReturnProperties() == null
				|| (filterCriteriaElasticFields.getReturnProperties().size() <= 0)) {
			display = "all";
			if (filterCriteriaElasticFields.getAssociationSearch() != null) {
				display = "association";
			} else if (filterCriteriaElasticFields.getDefinitionSource() != null) {
				display = "definition";
			}
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

		if (filterCriteriaElasticFields.getAssociationSearch() != null) {
			if (!(filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("source")
					|| filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("target"))) {
				throw new InvalidParameterValueException("Invalid Parameter value for associationSearch -"
						+ filterCriteriaElasticFields.getAssociationSearch() + ". The valid values are source,target.");

			}
			associationSearch = true;
		}

		if ((!(filterCriteriaElasticFields.getSynonymSource() == null)
				&& !(filterCriteriaElasticFields.getSynonymSource().equalsIgnoreCase("")))
				|| (!(filterCriteriaElasticFields.getSynonymGroup() == null)
						&& !(filterCriteriaElasticFields.getSynonymGroup().equalsIgnoreCase("")))) {
			synonymSource = true;
		}

		if ((!(filterCriteriaElasticFields.getDefinitionSource() == null)
				&& !(filterCriteriaElasticFields.getDefinitionSource().equalsIgnoreCase("")))) {
			definitionSource = true;
		}

		// *******source fields replace******************
		String returnFields = "";
		String sourcefields = "";

		if (!display.equalsIgnoreCase("")) {
			if (display.equalsIgnoreCase("short"))
				returnFields = elasticQueryProperties.getShortsourcefields();
			else if (display.equalsIgnoreCase("all")) {
				returnFields = "true";
			} else if (display.equalsIgnoreCase("definition")) {
				returnFields = elasticQueryProperties.getDefinitionsourcefields();
			} else if (display.equalsIgnoreCase("association")) {
				returnFields = elasticQueryProperties.getAssociationsourcefields();
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
				if (synonymSource || associationSearch || definitionSource)
					operator = "\"operator\":\"and\",\n";
				else
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
		if (synonymSource) {
			// any getProperty field will be ignored
			if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
					|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
				fields = elasticQueryProperties.getExactstartswithsynonymfields();
			} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				fields = elasticQueryProperties.getContainssynonymfields();

			} else {
				fields = elasticQueryProperties.getAndorsynonymfields();
			}

		} else if (associationSearch) {
			// any getProperty field will be ignored
			if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
					|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
				fields = elasticQueryProperties.getExactstartswithassociationfields();
			} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				fields = elasticQueryProperties.getContainsassociationfields();

			} else {
				fields = elasticQueryProperties.getAndorassociationfields();
			}
		} else if (definitionSource) {
			// any getProperty field will be ignored
			if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
					|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
				fields = elasticQueryProperties.getExactstartswithdefinitionfields();
			} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				fields = elasticQueryProperties.getContainsdefinitionfields();

			} else {
				fields = elasticQueryProperties.getAndordefinitionfields();
			}
		} else {
			if (filterCriteriaElasticFields.getProperty() != null
					&& filterCriteriaElasticFields.getProperty().size() > 0) {
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
		}
		valuesMap.put("fields", fields);

		// ***********replace highlight fields************************
		String highlightFields = "";
		if (!filterCriteriaElasticFields.getFormat().equalsIgnoreCase("clean")) {
			if (synonymSource) {
				if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
						|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
					highlightFields = elasticQueryProperties.getHighlightsynonymexact();

				} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
					highlightFields = elasticQueryProperties.getHighlightsynonymcontains();

				} else {
					highlightFields = elasticQueryProperties.getHighlightsynonymandor();
				}
			} else if (associationSearch) {
				if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
						|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
					highlightFields = elasticQueryProperties.getHighlightassociationexact();

				} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
					highlightFields = elasticQueryProperties.getHighlightassociationcontains();

				} else {
					highlightFields = elasticQueryProperties.getHighlightassociationandor();
				}

			} else if (definitionSource) {
				if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
						|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
					highlightFields = elasticQueryProperties.getHighlightdefinitionexact();

				} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
					highlightFields = elasticQueryProperties.getHighlightdefinitioncontains();

				} else {
					highlightFields = elasticQueryProperties.getHighlightdefinitionandor();
				}

			} else {
				if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
						|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
					highlightFields = elasticQueryProperties.getHighlightexact();

				} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
					highlightFields = elasticQueryProperties.getHighlightcontains();

				} else {
					highlightFields = elasticQueryProperties.getHighlightandor();
				}
			}
			valuesMap.put("highlightFields", highlightFields);
		}

		// ***********replace highlight Tags**********************************
		if (filterCriteriaElasticFields.getFormat().equalsIgnoreCase("raw")) {
			String highlightTags = elasticQueryProperties.getHighlightTags();
			valuesMap.put("highlightTags", highlightTags);
		} else {
			valuesMap.put("highlightTags", "");
		}
		// get association relationship
		if (associationSearch) {
			String associationRelationship = constructAssociationRelationship(filterCriteriaElasticFields);
			valuesMap.put("searchFilter", associationRelationship);
			
			valuesMap.put("nestedPath", "associations");
		}

		// ***************synonym source***********
		if (synonymSource) {
			String synonymSourceStr = constructSynonymSource(filterCriteriaElasticFields);
			valuesMap.put("searchFilter", synonymSourceStr);
			
			valuesMap.put("nestedPath", "synonyms");
		}

		// ***************definition source***********
		if (definitionSource) {
			String definitionSourceStr = constructDefintionSource(filterCriteriaElasticFields);
			valuesMap.put("searchFilter", definitionSourceStr);
			
			valuesMap.put("nestedPath", "definitions");
		}

		// **********************filter replace********************
		if (!(synonymSource || associationSearch)) {
			String filter = this.constructFilterQuery(filterCriteriaElasticFields);
			valuesMap.put("filter", filter);
		}

		// *********get main query
		String templateString = "";
		if (synonymSource || associationSearch || definitionSource) {
			templateString = getMainNestedQuery(filterCriteriaElasticFields);
		} else {
			templateString = getMainQuery(filterCriteriaElasticFields);
		}

		// replace values
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String resolvedString = sub.replace(templateString);

		log.debug("query string - " + resolvedString);
		return resolvedString;
	}

	private String constructSynonymSource(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String synonymSourceStr = "";
		String synonymSource = filterCriteriaElasticFields.getSynonymSource();
		String synonymGroup = filterCriteriaElasticFields.getSynonymGroup();

		if (synonymSource != null && !synonymSource.equalsIgnoreCase("")) {
			synonymSourceStr = synonymSourceStr + ",{ \"match\" : {\"synonyms.termSource\" : \"" + synonymSource
					+ "\"} }";
		}
		if (synonymGroup != null && !synonymGroup.equalsIgnoreCase("")) {
			synonymSourceStr = synonymSourceStr + ",{ \"match\" : {\"synonyms.termGroup\" : \"" + synonymGroup
					+ "\"} }";
		}

		return synonymSourceStr;

	}
	
	private String constructDefintionSource(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String definitionSourceStr = "";
		String definitionSource = filterCriteriaElasticFields.getDefinitionSource();
		

		if (definitionSource != null && !definitionSource.equalsIgnoreCase("")) {
			definitionSourceStr = definitionSourceStr + ",{ \"match\" : {\"definitions.defSource\" : \"" + definitionSource
					+ "\"} }";
		}
		

		return definitionSourceStr;

	}

	private String constructAssociationRelationship(FilterCriteriaElasticFields filterCriteriaElasticFields)
			throws InvalidParameterValueException {
		String associationRelationship = "";
		if (filterCriteriaElasticFields.getRelationship() == null
				|| filterCriteriaElasticFields.getRelationship().size() <= 0) {
			associationRelationship = "";
		} else {

			if (filterCriteriaElasticFields.getRelationship().get(0).toLowerCase().equalsIgnoreCase("all")) {
				associationRelationship = "";
			} else if (filterCriteriaElasticFields.getRelationship().size() == 1) {
				String value = this.associationToQuery
						.get(filterCriteriaElasticFields.getRelationship().get(0).toLowerCase());
				if (value != null) {
					associationRelationship = associationRelationship + ",\n";
					associationRelationship = associationRelationship
							+ "{\"match\" : {\"associations.relationship\" : \"" + value + "\"} }";
				} else {
					throw new InvalidParameterValueException(
							"Invalid Parameter value for relationship field for association search. Rejected value - "
									+ filterCriteriaElasticFields.getRelationship().get(0));

				}
			} else {
				associationRelationship = associationRelationship + ",\n";
				associationRelationship = associationRelationship + " {\"bool\":{\n";
				associationRelationship = associationRelationship + "   \"should\":[\n";
				for (String relationship : filterCriteriaElasticFields.getRelationship()) {

					String value = this.associationToQuery.get(relationship.toLowerCase());
					if (value != null) {

						associationRelationship = associationRelationship
								+ "{\"match\" : {\"associations.relationship\" : \"" + value + "\"} },";
					} else {
						throw new InvalidParameterValueException(
								"Invalid Parameter value for relationship field for association search. Rejected value - "
										+ relationship);
					}

				}

				associationRelationship = associationRelationship.substring(0, associationRelationship.length() - 1);
				associationRelationship = associationRelationship + "]\n";
				associationRelationship = associationRelationship + "}\n";
				associationRelationship = associationRelationship + "}\n";
			}

		}
		return associationRelationship;
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

	private String getMainNestedQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String query = "";
		if (filterCriteriaElasticFields.getFormat().equalsIgnoreCase("clean")) {
			query = elasticQueryProperties.getMainNestedQueryWithoutHighlights();
		} else
			query = elasticQueryProperties.getMainNestedQuery();

		return query;

	}

}
