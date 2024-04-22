package gov.nih.nci.evs.api.service;

import org.springframework.stereotype.Service;

/**
 * The implementation for {@link ElasticLoadService}.
 *
 * @author Arun
 */
@Service
public class StardogElasticLoadServiceImpl extends AbstractStardogLoadServiceImpl {

  // This loader is for reconciling data from stardog
  // All functionality was moved to the superclass
  // to support the StardogReport loader

}
