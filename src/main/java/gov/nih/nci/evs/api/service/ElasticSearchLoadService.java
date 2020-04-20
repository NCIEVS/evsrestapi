package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.support.LoadConfig;

/**
 * 
 * 
 * @author Arun
 */
public interface ElasticSearchLoadService {

  void load(LoadConfig config);

}
