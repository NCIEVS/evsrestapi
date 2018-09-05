package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.service.exception.InvalidParameterValueException;
import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;

public interface ElasticSearchService {

	
	public String elasticsearch(FilterCriteriaElasticFields filterCriteriaElasticFields)throws IOException, HttpClientErrorException;
}
