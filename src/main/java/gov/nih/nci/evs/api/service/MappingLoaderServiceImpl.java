
package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The implementation for {@link LoaderService}.
 *
 * @author Arun
 */
@Service
public class MappingLoaderServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(MappingLoaderServiceImpl.class);

  /** the environment *. */
  @Autowired
  Environment env;

  @Autowired
  ApplicationProperties applicationProperties;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    final String uri = applicationProperties.getConfigBaseUri();
    boolean created = operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
    if (created) {
      operationsService.getElasticsearchOperations().putMapping(
          ElasticOperationsService.MAPPING_INDEX, ElasticOperationsService.CONCEPT_TYPE,
          Concept.class);
    }

  }

  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config,
    String filepath, String termName, boolean forceDelete) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
