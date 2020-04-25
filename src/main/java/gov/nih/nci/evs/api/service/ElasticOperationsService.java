package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import gov.nih.nci.evs.api.model.Concept;

/**
 * The service for performing index related operations on Elasticsearch
 * 
 * @author Arun
 *
 */
public interface ElasticOperationsService {
  void createIndex(String indexName, boolean force) throws IOException;
  void loadConcepts(List<Concept> concepts, String index, String type, boolean async) throws IOException;
}
