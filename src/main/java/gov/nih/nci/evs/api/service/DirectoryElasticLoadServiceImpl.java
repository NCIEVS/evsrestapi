
package gov.nih.nci.evs.api.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link DirectoryElasticLoadService}.
 *
 * @author Arun
 */
@Service
public class DirectoryElasticLoadServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(DirectoryElasticLoadServiceImpl.class);
  
  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;

  /** the lock file name *. */
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;

  /** download batch size *. */
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;

  /** index batch size *. */
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;

  /** the environment *. */
  @Autowired
  Environment env;
  
  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /* see superclass */
  @Override
  public void loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException {

  }
 

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException {
    
  }
  
  @Override
  public void setUpConceptLoading(ApplicationContext app, CommandLine cmd) throws Exception {
	try {
	  ElasticLoadConfig config = buildConfig(cmd, CONCEPTS_OUT_DIR);

      if (StringUtils.isBlank(config.getTerminology())) {
        logger.error(
            "Terminology (-t or --terminology) is required! Try -h or --help to learn more about command line options available.");
        return;
      }
      TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
      Terminology term = termUtils.getTerminology(config.getTerminology(), false);
      HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtils(term);
      File filepath = new File(cmd.getOptionValue('d'));
      if(filepath.exists()){
    	  logger.info("file path exists!");
      }
      else {
    	  logger.error("Given file path does not exist");
    	  return;
      }
      loadConcepts(config, term, hierarchy);
      loadObjects(config, term, hierarchy);
      cleanStaleIndexes();
      updateLatestFlag();
	} catch (Exception e) {
	  logger.error(e.getMessage(), e);
	  throw new RuntimeException(e);
	}
	  
  }
}
