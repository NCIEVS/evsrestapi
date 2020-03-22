package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
  @Cacheable(value = "metadata", key="{#root.methodName, #include.orElse(''), #terminology}",
    condition = "#list.orElse('').isEmpty()")
  public List<Concept> getRoles(String terminology, 
      Optional<String> include, Optional<String> list) throws Exception {
    
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> roles = sparqlQueryManagerService.getAllRoles(dbType, ip);
    return ConceptUtils.applyIncludeAndList(roles, ip, list.orElse(null));
  }

  @Override
  public Optional<Concept> getRole(String terminology, String code, 
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
    final List<Concept> list = getRoles(terminology, Optional.ofNullable(include.orElse("summary")),
        Optional.ofNullable(code));
    if (list.size() > 0) {
      return Optional.of(list.get(0));
    }
    return Optional.empty();
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
  public Optional<Concept> getProperty(String terminology, String code, 
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

    final List<Concept> list =
        getProperties(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    logger.info(String.format("list from properties [%s] with size [%s]", String.valueOf(list), list==null?0:list.size()));
    if (list.size() > 0) {
      final Concept concept = ConceptUtils.convertConcept(
          sparqlQueryManagerService.getEvsProperty(list.get(0).getCode(), dbType, ip));
      return Optional.of(concept);
    }
    return Optional.empty();
  }

  @Override
  @Cacheable(value = "metadata", key="{#root.methodName, #terminology}", 
    condition = "#terminology.equals('ncit')")
  public Optional<List<String>> getConceptStatuses(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit")) return Optional.empty(); 
    
    List<String> result = sparqlQueryManagerService.getConceptStatusForDocumentation();
    return Optional.of(result);
  }

  @Override
  @Cacheable(value = "metadata", key="{#root.methodName, #terminology}", 
    condition = "#terminology.equals('ncit')")
  public Optional<List<String>> getContributingSources(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit")) return Optional.empty();
    
    List<String> result = sparqlQueryManagerService.getContributingSourcesForDocumentation();
    return Optional.of(result);
  }

  @Override
  @Cacheable(value = "metadata", key="{#root.methodName, #terminology, #code}")
  public Optional<List<String>> getAxiomQualifiersList(String terminology, String code) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam("minimal");

    // Like "get properties", if it's "name style", we need to get all and then
    // find
    // this one.
    Concept concept = null;
    if (ModelUtils.isCodeStyle(code)) {
      concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
    }

    final List<Concept> list =
        getProperties(terminology, Optional.ofNullable("minimal"), Optional.ofNullable(code));
    if (list.size() > 0) {
      concept = list.get(0);
    }

    if (concept == null || concept.getCode() == null) {
      return Optional.empty();
    }

    final List<String> propertyValues =
        sparqlQueryManagerService.getAxiomQualifiersList(concept.getCode(), dbType);
    return Optional.of(propertyValues);
  }

}
