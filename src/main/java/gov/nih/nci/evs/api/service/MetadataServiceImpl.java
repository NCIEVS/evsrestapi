package gov.nih.nci.evs.api.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import gov.nih.nci.evs.api.controller.MetadataController;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.ConfigData;

/**
 * Implementation for {@link MetadataService}
 * 
 * @author Arun
 *
 */
public class MetadataServiceImpl implements MetadataService {

  /** The cache. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MetadataServiceImpl.class);
  
  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;
  
  @Override
  @Cacheable(value = "metadata", key="#root.methodName")
  public ConfigData getApplicationMetadata() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Cacheable(value = "metadata", key="#root.methodName")
  public List<Terminology> getTerminologies() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Cacheable(value = "metadata", key="#root.methodName")
  public List<Concept> getAssociations(String terminology, 
      Optional<String> include, Optional<String> list) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Concept getAssociation(String terminology, String code, 
      Optional<String> include) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Concept> getRoles(String terminology, 
      Optional<String> include, Optional<String> list) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Concept getRole(String terminology, String code, 
      Optional<String> include) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Concept> getProperties(String terminology, 
      Optional<String> include, Optional<String> list) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Concept getProperty(String terminology, String code, 
      Optional<String> include) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getConceptStatuses(String terminology) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getContributingSources(String terminology) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getAxiomQualifiersList(String terminology, String code) {
    // TODO Auto-generated method stub
    return null;
  }

}
