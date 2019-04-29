package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
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
	private HashMap<String, String> conceptStatus;
	private HashMap<String, String> contributingSource;
	private HashMap<String, String> roleToQuery;

	@Autowired
	private ElasticQueryProperties elasticQueryProperties;
	
	@Autowired
	private ThesaurusProperties thesaurusProperties;

	@PostConstruct
	public void postInit() throws IOException {
		
    	conceptStatus = (HashMap<String, String>) thesaurusProperties.getConceptStatuses();
    	contributingSource = (HashMap<String, String>) thesaurusProperties.getContributingSources();
		roleToQuery = (HashMap<String, String>) thesaurusProperties.getRoles();
		associationToQuery = (HashMap<String, String>) thesaurusProperties.getAssociations();
		returnFieldMap = (HashMap<String, String>) thesaurusProperties.getReturnFields();		
		propertyToQuery = (HashMap<String, String>) elasticQueryProperties.getPropertyToQuery();
		propertyToQueryExact = (HashMap<String, String>) elasticQueryProperties.getPropertyToQueryExact();
		propertyToQueryContains = (HashMap<String, String>) elasticQueryProperties.getPropertyToQueryContains();
	}

	public String constructQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) throws IOException {
		Map valuesMap = new HashMap();
		// setting defaults
		boolean synonymSource = false;
		boolean definitionSource = false;
		boolean associationSearch = false;
		boolean roleSearch = false;
		String relation = null;

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
			} else if (filterCriteriaElasticFields.getRoleSearch() != null) {
				display = "role";
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

		//association
		if (filterCriteriaElasticFields.getAssociationSearch() != null) {
			if (!(filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("source")
					|| filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("target"))) {
				throw new InvalidParameterValueException("Invalid Parameter value for associationSearch -"
						+ filterCriteriaElasticFields.getAssociationSearch() + ". The valid values are source,target.");

			}
			if (filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("source")){
				relation = "Association";
			}else {
				relation = "InverseAssociation";
			}
			associationSearch = true;
		}
		//role
		if (filterCriteriaElasticFields.getRoleSearch() != null) {
			if (!(filterCriteriaElasticFields.getRoleSearch().equalsIgnoreCase("source")
					|| filterCriteriaElasticFields.getRoleSearch().equalsIgnoreCase("target"))) {
				throw new InvalidParameterValueException("Invalid Parameter value for roleSearch -"
						+ filterCriteriaElasticFields.getRoleSearch() + ". The valid values are source,target.");

			}
			if (filterCriteriaElasticFields.getRoleSearch().equalsIgnoreCase("source")){
				relation = "Role";
			}else {
				relation = "InverseRole";
			}
			roleSearch = true;
		}

		//synonym
		if ((!(filterCriteriaElasticFields.getSynonymSource() == null)
				&& !(filterCriteriaElasticFields.getSynonymSource().equalsIgnoreCase("")))
				|| (!(filterCriteriaElasticFields.getSynonymGroup() == null)
						&& !(filterCriteriaElasticFields.getSynonymGroup().equalsIgnoreCase("")))) {
			synonymSource = true;
		}

		//definition
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
			} else if (display.equalsIgnoreCase("association") || display.equalsIgnoreCase("role")) {
				returnFields = elasticQueryProperties.getAssociationsourcefields();			
				returnFields = returnFields.replace("${relation}", relation);
				
			} else {
				returnFields = elasticQueryProperties.getShortsourcefields();
			}
		} else {
			returnFields = "[\n";
			returnFields = returnFields + elasticQueryProperties.getDefaultsourcefields();
			returnFields = returnFields.substring(0, returnFields.length() - 1);
			for (String field : filterCriteriaElasticFields.getReturnProperties()) {
				String fieldValue = this.returnFieldMap.get(field.toLowerCase());
				if (fieldValue == null) {
					throw new InvalidParameterValueException("Invalid Parameter value for returnProperties -" + field);

				} else {
					if (!(fieldValue.equalsIgnoreCase("Label") || fieldValue.equalsIgnoreCase("Code")))
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
		String rescore = "";
		if (filterCriteriaElasticFields.getType() != null) {
			if (filterCriteriaElasticFields.getType() == "OR") {
				operator = "";
			}
			if (filterCriteriaElasticFields.getType().equalsIgnoreCase("AND")) {
				if (synonymSource || associationSearch || roleSearch|| definitionSource)
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
				rescore = elasticQueryProperties.getRescoreQuery();
			}
		} else {
			operator = "";
		}
		valuesMap.put("rescore", rescore);
		valuesMap.put("operator", operator);
		valuesMap.put("searchterm", term);

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

		} else if (associationSearch || roleSearch) {
			// any getProperty field will be ignored
			if ((filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith"))
					|| (filterCriteriaElasticFields.getType().equalsIgnoreCase("match"))) {
				fields = elasticQueryProperties.getExactstartswithassociationfields();
			} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
				fields = elasticQueryProperties.getContainsassociationfields();

			} else {
				fields = elasticQueryProperties.getAndorassociationfields();
			}
			
				fields = fields.replace("${relation}", relation);
			
			
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
					fields = fields.substring(0, fields.length() - 1);
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
			} else if (associationSearch || roleSearch) {
				if (filterCriteriaElasticFields.getType().equalsIgnoreCase("startswith")
						|| filterCriteriaElasticFields.getType().equalsIgnoreCase("match")) {
					highlightFields = elasticQueryProperties.getHighlightassociationexact();

				} else if (filterCriteriaElasticFields.getType().equalsIgnoreCase("contains")) {
					highlightFields = elasticQueryProperties.getHighlightassociationcontains();

				} else {
					highlightFields = elasticQueryProperties.getHighlightassociationandor();
				}
				
				
					highlightFields = highlightFields.replace("${relation}", relation);
				

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
		if (associationSearch || roleSearch) {
			String associationRelationship = constructAssociationRelationship(filterCriteriaElasticFields,relation);
			valuesMap.put("searchFilter", associationRelationship);
			
			valuesMap.put("nestedPath", relation);
		}

		// ***************synonym source***********
		if (synonymSource) {
			String synonymSourceStr = constructSynonymSource(filterCriteriaElasticFields);
			valuesMap.put("searchFilter", synonymSourceStr);
			
			valuesMap.put("nestedPath", "FULL_SYN");
		}

		// ***************definition source***********
		if (definitionSource) {
			String definitionSourceStr = constructDefintionSource(filterCriteriaElasticFields);
			valuesMap.put("searchFilter1", definitionSourceStr);
			definitionSourceStr = constructAltDefintionSource(filterCriteriaElasticFields);
			valuesMap.put("searchFilter2", definitionSourceStr);
			valuesMap.put("nestedPath1", "DEFINITION");
			valuesMap.put("nestedPath2", "ALT_DEFINITION");
		}

		// **********************filter replace********************
		if (!(synonymSource || associationSearch || roleSearch)) {
			String filter = this.constructFilterQuery(filterCriteriaElasticFields);
			valuesMap.put("filter", filter);
		}

		// *********get main query
		String templateString = "";
		if (definitionSource) {
			templateString = getMainMultipleNestedQuery(filterCriteriaElasticFields);
		}else if (synonymSource || associationSearch || roleSearch) {
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
			synonymSourceStr = synonymSourceStr + ",{ \"match\" : {\"FULL_SYN.term-source\" : \"" + synonymSource
					+ "\"} }";
		}
		if (synonymGroup != null && !synonymGroup.equalsIgnoreCase("")) {
			synonymSourceStr = synonymSourceStr + ",{ \"match\" : {\"FULL_SYN.term-group\" : \"" + synonymGroup
					+ "\"} }";
		}

		return synonymSourceStr;

	}
	
	private String constructDefintionSource(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String definitionSourceStr = "";
		String definitionSource = filterCriteriaElasticFields.getDefinitionSource();
		

		if (definitionSource != null && !definitionSource.equalsIgnoreCase("")) {
			definitionSourceStr = definitionSourceStr + ",{ \"match\" : {\"DEFINITION.def-source\" : \"" + definitionSource
					+ "\"} }";
		}
		

		return definitionSourceStr;

	}
	
	private String constructAltDefintionSource(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String definitionSourceStr = "";
		String definitionSource = filterCriteriaElasticFields.getDefinitionSource();
		

		if (definitionSource != null && !definitionSource.equalsIgnoreCase("")) {
			definitionSourceStr = definitionSourceStr + ",{ \"match\" : {\"ALT_DEFINITION.def-source\" : \"" + definitionSource
					+ "\"} }";
		}
		

		return definitionSourceStr;

	}


	private String constructAssociationRelationship(FilterCriteriaElasticFields filterCriteriaElasticFields,String relation)
			throws InvalidParameterValueException {
		String associationRelationship = "";
		if (filterCriteriaElasticFields.getRelationship() == null
				|| filterCriteriaElasticFields.getRelationship().size() <= 0) {
			associationRelationship = "";
			if (((filterCriteriaElasticFields.getAssociationSearch() != null) && (filterCriteriaElasticFields.getAssociationSearch().equalsIgnoreCase("target"))) ||
					((filterCriteriaElasticFields.getRoleSearch() != null) && (filterCriteriaElasticFields.getRoleSearch().equalsIgnoreCase("target")))){
				throw new InvalidParameterValueException(
						"If association/role search is specified as target then a relationship should be specified."
								);
			}
		} else {
			
			HashMap<String, String> mapToSearch = null;
            if (relation.equalsIgnoreCase("Association") || relation.equalsIgnoreCase("InverseAssociation")) {
            	mapToSearch = this.associationToQuery;
            }else {
            	mapToSearch = this.roleToQuery;
            }
			
			if (filterCriteriaElasticFields.getRelationship().get(0).toLowerCase().equalsIgnoreCase("all")) {
				associationRelationship = "";
			} else if (filterCriteriaElasticFields.getRelationship().size() == 1) {
				String value = null;
				value = searhValue(filterCriteriaElasticFields.getRelationship().get(0),mapToSearch);
				
				
				
				
				if (value != null) {
					associationRelationship = associationRelationship + ",\n";
					associationRelationship = associationRelationship
							+ "{\"match\" : {\"" + relation + ".relationship\" : \"" + value + "\"} }";
				} else {
					
						throw new InvalidParameterValueException(
							"Invalid Parameter value for relationship field for " + relation + " search. Rejected value - "
									+ filterCriteriaElasticFields.getRelationship().get(0));
					
				}
			} else {
				associationRelationship = associationRelationship + ",\n";
				associationRelationship = associationRelationship + " {\"bool\":{\n";
				associationRelationship = associationRelationship + "   \"should\":[\n";
				for (String relationship : filterCriteriaElasticFields.getRelationship()) {
					String value = null;
					value = searhValue(relationship,mapToSearch);
						
					
					if (value != null) {

						associationRelationship = associationRelationship
								+ "{\"match\" : {\"" + relation + ".relationship\" : \"" + value + "\"} },";
					} else {
						
							throw new InvalidParameterValueException(
								"Invalid Parameter value for relationship field for " + relation + " search. Rejected value - "
										+ filterCriteriaElasticFields.getRelationship().get(0));
						
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
	
	private String searhValue(String term,Map searchMap) {
		String value = null;
		
		value = (String)searchMap
				.get(term.toLowerCase());
		
		
		return value;
	}

	private String constructFilterQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) throws InvalidParameterValueException {
		String filter = "";
		boolean contributingSourceFilter = false;		
		boolean conceptStatusFilter = false;


		ArrayList<String> conceptStatuses = filterCriteriaElasticFields.getConceptStatus();
		
		if (conceptStatuses != null && conceptStatuses.size() > 0) {
			
			for (String conceptStatus: conceptStatuses ) {
			conceptStatus = conceptStatus.toLowerCase();
			if (conceptStatus == null) {
				throw new InvalidParameterValueException(
						"Invalid Parameter value for conceptStatus field. Rejected value - "
								+ conceptStatus);
			    }
			}
			conceptStatusFilter = true;
		}
		
		ArrayList<String> contributingSources = filterCriteriaElasticFields.getContributingSource();
		if (contributingSources != null && contributingSources.size() > 0) {
			
			for (String contributingSource: contributingSources ) {
			contributingSource = contributingSource.toLowerCase();
			if (contributingSource == null) {
				throw new InvalidParameterValueException(
						"Invalid Parameter value for contributingSource field. Rejected value - "
								+ filterCriteriaElasticFields.getContributingSource());
			 }
			}
			contributingSourceFilter = true;
		}
		
		if (contributingSourceFilter  || conceptStatusFilter) {
			filter = filter + ",\n";
			filter = filter + "\"filter\":\n";
			filter = filter + "[\n";
		//	filter = filter + "\"bool\":{\n";
		//	filter = filter + "\"should\":[\n";

			
			
			if (conceptStatusFilter) {
				if (conceptStatuses.size() == 1) {
					 filter = filter + "{\"term\":{\"Concept_Status\":\"" + conceptStatuses.get(0) + "\"}},\n";
				} else {
					filter = filter + "{\"terms\":{\"Concept_Status\": [" ;
				for (String conceptStatus: conceptStatuses ) {
				   filter = filter + "\"" + conceptStatus + "\",";
				 }
				filter = filter.substring(0, filter.length() - 1);
				filter = filter + "]}},\n";
				}
			}
			
			//if (contributingSourceFilter) {
			//	for (String contributingSource: contributingSources ) {
			//	  filter = filter + "{\"term\":{\"Contributing_Source\":\"" + contributingSource + "\"}},\n";
			//	}
			//}
			
			if (contributingSourceFilter) {
				if (contributingSources.size() == 1) {
					 filter = filter + "{\"term\":{\"Contributing_Source\":\"" + contributingSources.get(0) + "\"}},\n";
				} else {
					filter = filter + "{\"terms\":{\"Contributing_Source\": [" ;
				for (String contributingSource: contributingSources ) {
				   filter = filter + "\"" + contributingSource + "\",";
				 }
				filter = filter.substring(0, filter.length() - 1);
				filter = filter + "]}},\n";
				}
			}

			filter = filter.substring(0, filter.length() - 2);
			filter = filter + "]\n";
			//filter = filter + "}\n";
			//filter = filter + "}\n";
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
	
	private String getMainMultipleNestedQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) {
		String query = "";
		if (filterCriteriaElasticFields.getFormat().equalsIgnoreCase("clean")) {
			query = elasticQueryProperties.getMainMultipleNestedQueryWithoutHighlights();
		} else
			query = elasticQueryProperties.getMainMultipleNestedQuery();

		return query;

	}

}
