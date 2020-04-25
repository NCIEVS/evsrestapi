package gov.nih.nci.evs.api.service;

import java.io.IOException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.LoadConfig;

/**
 * The service to load concepts to Elasticsearch
 * 
 * Retrieves concepts from stardog and creates necessary index on Elasticsearch
 * 
 * @author Arun
 *
 */
public interface ElasticLoadService {

  void load(LoadConfig config, Terminology terminology) throws IOException;

}
