
package gov.nih.nci.evs.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Implementation for {@link MetadataService}.
 *
 * @author Arun
 */
@Service
public class MetadataServiceImpl implements MetadataService {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

  /** The thesaurus properties. */
  @Autowired
  private ThesaurusProperties thesaurusProperties;

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;

  /** The self. */
  @Resource
  private MetadataService self;

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the associations
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #include.orElse(''), #terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getAssociations(String terminology, Optional<String> include,
    Optional<String> list) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> associations = sparqlQueryManagerService.getAllAssociations(term, ip);
    return ConceptUtils.applyIncludeAndList(associations, ip, list.orElse(null));
  }

  /**
   * Returns the association.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the association
   * @throws Exception the exception
   */
  @Override
  public Optional<Concept> getAssociation(String terminology, String code, Optional<String> include)
    throws Exception {

    // Verify that it is an association
    final List<Concept> list = self.getAssociations(terminology,
        Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() > 0) {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return Optional.of(sparqlQueryManagerService.getAssociation(list.get(0).getCode(), term, ip));
    }
    return Optional.empty();
  }

  /**
   * Returns the roles.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the roles
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #include.orElse(''), #terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getRoles(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> roles = sparqlQueryManagerService.getAllRoles(term, ip);
    return ConceptUtils.applyIncludeAndList(roles, ip, list.orElse(null));
  }

  /**
   * Returns the role.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the role
   * @throws Exception the exception
   */
  @Override
  public Optional<Concept> getRole(String terminology, String code, Optional<String> include)
    throws Exception {

    // Verify that it is a role
    final List<Concept> list = self.getRoles(terminology,
        Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() > 0) {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return Optional.of(sparqlQueryManagerService.getRole(list.get(0).getCode(), term, ip));
    }
    return Optional.empty();
  }

  /**
   * Returns the properties.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #include.orElse(''), #terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getProperties(String terminology, Optional<String> include,
    Optional<String> list) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    // TODO: "first-class attribute qualifiers", like 'NCH0'
    // Need to identify which attributes are associated with "other model
    // elements" - they should probably be excluded too - have to derive from
    // properties
    final Set<String> neverUsedCodes = sparqlQueryManagerService.getAllPropertiesNeverUsed(term, ip)
        .stream().map(q -> q.getCode()).collect(Collectors.toSet());
    final Set<String> qualifierCodes = sparqlQueryManagerService.getAllQualifiers(term, ip).stream()
        .map(q -> q.getCode()).collect(Collectors.toSet());

    // Remove qualifiers from properties list
    final List<Concept> properties = sparqlQueryManagerService.getAllProperties(term, ip).stream()
        .filter(p -> !qualifierCodes.contains(p.getCode()) && !neverUsedCodes.contains(p.getCode()))
        .collect(Collectors.toList());

    return ConceptUtils.applyIncludeAndList(properties, ip, list.orElse(null));
  }

  /**
   * Returns the qualifiers.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the qualifiers
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #include.orElse(''), #terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getQualifiers(String terminology, Optional<String> include,
    Optional<String> list) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> qualifiers = sparqlQueryManagerService.getAllQualifiers(term, ip);

    // IF "for documentation" mode, remove the "not considered" cases.
    return ConceptUtils.applyIncludeAndList(qualifiers, ip, list.orElse(null));
  }

  /**
   * Returns the qualifier.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the qualifier
   * @throws Exception the exception
   */
  @Override
  public Optional<Concept> getQualifier(String terminology, String code, Optional<String> include)
    throws Exception {
    // Verify that it is a qualifier
    final List<Concept> list =
        self.getQualifiers(terminology, Optional.of("minimal"), Optional.ofNullable(code));

    if (list.size() > 0) {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return Optional.of(sparqlQueryManagerService.getQualifier(list.get(0).getCode(), term, ip));
    }
    return Optional.empty();
  }

  /**
   * Returns the property.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the property
   * @throws Exception the exception
   */
  @Override
  public Optional<Concept> getProperty(String terminology, String code, Optional<String> include)
    throws Exception {

    // Verify that it is a property
    final List<Concept> list =
        self.getProperties(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    if (list.size() > 0) {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return Optional.of(sparqlQueryManagerService.getProperty(list.get(0).getCode(), term, ip));
    }
    return Optional.empty();
  }

  /**
   * Returns the concept statuses.
   *
   * @param terminology the terminology
   * @return the concept statuses
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology}",
      condition = "#terminology.equals('ncit')")
  public Optional<List<String>> getConceptStatuses(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit"))
      return Optional.empty();

    final Map<String, String> conceptStatuses = thesaurusProperties.getConceptStatuses();
    List<String> statuses = new ArrayList<String>();

    for (String name : conceptStatuses.keySet()) {
      // search for value
      String value = conceptStatuses.get(name);
      // String conSource = name + " : " + value;
      // sources.add(conSource);
      statuses.add(value);
    }

    return Optional.of(statuses);

  }

  /**
   * Returns the contributing sources.
   *
   * @param terminology the terminology
   * @return the contributing sources
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology}",
      condition = "#terminology.equals('ncit')")
  public List<ConceptMinimal> getContributingSources(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit"))
      return new ArrayList<>();

    return sparqlQueryManagerService.getContributingSources(term);

  }

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology}",
      condition = "#terminology.equals('ncit')")
  public List<ConceptMinimal> getSynonymSources(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit"))
      return new ArrayList<>();

    return sparqlQueryManagerService.getSynonymSources(term);

  }

  /**
   * Returns the axiom qualifiers list.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology, #code}")
  public Optional<List<String>> getAxiomQualifiersList(String terminology, String code)
    throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    // Verify that it is a qualifier
    final List<Concept> list =
        self.getQualifiers(terminology, Optional.of("minimal"), Optional.ofNullable(code));

    if (list.size() > 0) {
      return Optional
          .of(sparqlQueryManagerService.getAxiomQualifiersList(list.get(0).getCode(), term));
    }

    return Optional.empty();

  }

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology}")
  public List<ConceptMinimal> getTermTypes(String terminology) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit"))
      return new ArrayList<>();

    return sparqlQueryManagerService.getTermTypes(term);
  }

}