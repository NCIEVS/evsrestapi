package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.support.ConfigData;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.ModelUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Implementation for {@link MetadataService}
 * 
 * @author Arun
 *
 */
@Service
public class MetadataServiceImpl implements MetadataService {

  /** The cache. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MetadataServiceImpl.class);
  
  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;
  
  @Override
  public ConfigData getApplicationMetadata() throws IOException {
    return getApplicationMetadata("monthly");
  }

  @Override
  @Cacheable(value = "metadata", key="{#root.methodName, #dbType}")
  public ConfigData getApplicationMetadata(String dbType) throws IOException {
    return sparqlQueryManagerService.getConfigurationData(dbType);
  }

  @Override
  @Cacheable(value = "metadata", key="#root.methodName")
  public List<Terminology> getTerminologies() throws IOException {
    return TerminologyUtils.getTerminologies(sparqlQueryManagerService);
  }

  @Override
  @Cacheable(value = "metadata", key="{#root.methodName, #include.orElse(''), #terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getAssociations(String terminology, 
      Optional<String> include, Optional<String> list) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> associations = sparqlQueryManagerService.getAllAssociations(dbType, ip);
    return ConceptUtils.applyIncludeAndList(associations, ip, list.orElse(null));
  }

  @Override
  public Optional<Concept> getAssociation(String terminology, String code, 
      Optional<String> include) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
      if (concept == null || concept.getCode() == null) {
        return Optional.empty();
      }
      return Optional.of(concept);
    }
    final List<Concept> list = getAssociations(terminology,
        Optional.ofNullable(include.orElse("summary")), Optional.ofNullable(code));
    if (list.size() > 0) {
      return Optional.of(list.get(0));
    }
    return Optional.empty();
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
  @Cacheable(value = "metadata", key="{#root.methodName, #include.orElse(''), #terminology}", 
    condition = "#list.orElse('').isEmpty()")
  public List<Concept> getProperties(String terminology, Optional<String> include, 
      Optional<String> list) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> properties = sparqlQueryManagerService.getAllProperties(dbType, ip);
    return ConceptUtils.applyIncludeAndList(properties, ip, list.orElse(null));
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
