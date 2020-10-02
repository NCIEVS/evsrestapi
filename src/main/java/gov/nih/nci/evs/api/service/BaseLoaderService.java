package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The service to load concepts to Elasticsearch
 * 
 * Retrieves concepts from stardog and creates necessary index on Elasticsearch
 * 
 * @author Arun
 *
 */
public abstract class BaseLoaderService {
	
  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(BaseLoaderService.class);
  
  /**
   * Load cached objects to elasticsearch.
   * 
   * @param config the load config from command line input
   * @param terminology the terminology
   * @throws IOException the io exception
   */
  abstract void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy) throws IOException;
  
  /**
   * Load concepts to elasticsearch.
   * 
   * @param config the load config from command line input
   * @param terminology the terminology
   * @throws IOException the io exception
   */
  abstract void loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy) throws IOException;

  /**
   * Clean stale indexes.
   *
   * @throws Exception the exception
   */
  abstract void cleanStaleIndexes() throws Exception;

  /**
   * Update latest flag.
   *
   * @throws Exception the exception
   */
  abstract void updateLatestFlag() throws Exception;
  
  /**
   * Set up concept loading
 * @param cmd 
 * @param app 
   *
   * @throws Exception the exception
   */
  abstract void setUpConceptLoading(ApplicationContext app, CommandLine cmd) throws Exception;
  
  /**
   * build config object from command line options.
   *
   * @param cmd the command line object
   * @param defaultLocation the default download location to use
   * @return the config object
   */
  public static ElasticLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    ElasticLoadConfig config = new ElasticLoadConfig();

    config.setTerminology(cmd.getOptionValue('t'));
    config.setRealTime(cmd.hasOption('r'));
    config.setForceDeleteIndex(cmd.hasOption('f'));
    if (cmd.hasOption('l')) {
      String location = cmd.getOptionValue('l');
      if (StringUtils.isBlank(location)) {
        logger.error("Location is empty!");

      }
      if (!location.endsWith("/")) {
        location += "/";
      }
      logger.info("location - {}", location);
      config.setLocation(location);
    } else {
      config.setLocation(defaultLocation);
    }
    if (cmd.hasOption('d')) {
        String location = cmd.getOptionValue('d');
        if (StringUtils.isBlank(location)) {
          logger.error("Location is empty!");

        }
        if (!location.endsWith("/")) {
          location += "/";
        }
        logger.info("location - {}", location);
        config.setLocation(location);
      } else {
        config.setLocation(defaultLocation);
      }

    return config;
  }

}
