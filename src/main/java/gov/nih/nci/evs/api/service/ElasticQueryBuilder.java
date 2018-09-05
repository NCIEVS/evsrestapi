package gov.nih.nci.evs.api.service;

import java.io.IOException;

import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;

public interface ElasticQueryBuilder {
	
	String constructQuery(FilterCriteriaElasticFields filterCriteriaElasticFields) throws IOException;

}
