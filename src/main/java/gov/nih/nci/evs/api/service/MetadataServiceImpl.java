
package gov.nih.nci.evs.api.service;

import java.io.IOException;
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
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
import gov.nih.nci.evs.api.support.ConfigData;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.ModelUtils;
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
  private static final Logger logger = LoggerFactory.getLogger(MetadataServiceImpl.class);

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
   * Returns the application metadata.
   *
   * @return the application metadata
   * @throws Exception the exception
   */
  @Override
  public ConfigData getApplicationMetadata() throws Exception {
    return self
        .getApplicationMetadata(TerminologyUtils.getLatestTerminology(sparqlQueryManagerService));
  }

  /**
   * Returns the application metadata.
   *
   * @param terminology the terminology
   * @return the application metadata
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "metadata", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public ConfigData getApplicationMetadata(Terminology terminology) throws IOException {
    return sparqlQueryManagerService.getConfigurationData(terminology);
  }

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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = sparqlQueryManagerService.getProperty(code, term, ip);
      if (concept == null || concept.getCode() == null) {
        return Optional.empty();
      }
      return Optional.of(concept);
    }
    final List<Concept> list = self.getAssociations(terminology,
        Optional.ofNullable(include.orElse("summary")), Optional.ofNullable(code));
    if (list.size() > 0) {
      return Optional.of(list.get(0));
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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = sparqlQueryManagerService.getProperty(code, term, ip);
      if (concept == null || concept.getCode() == null) {
        return Optional.empty();
      }
      return Optional.of(concept);
    }
    final List<Concept> list = self.getRoles(terminology,
        Optional.ofNullable(include.orElse("summary")), Optional.ofNullable(code));
    if (list.size() > 0) {
      return Optional.of(list.get(0));
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
    // elements"
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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = sparqlQueryManagerService.getQualifier(code, term, ip);
      if (concept == null || concept.getCode() == null) {
        return Optional.empty();
      }
      return Optional.of(concept);
    }

    final List<Concept> list =
        self.getQualifiers(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    if (logger.isDebugEnabled())
      logger.debug(String.format("list from qualifiers [%s] with size [%s]", String.valueOf(list),
          list == null ? 0 : list.size()));
    if (list.size() > 0) {
      final Concept concept =
          sparqlQueryManagerService.getQualifier(list.get(0).getCode(), term, ip);
      return Optional.of(concept);
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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = sparqlQueryManagerService.getProperty(code, term, ip);
      if (concept == null || concept.getCode() == null) {
        return Optional.empty();
      }
      return Optional.of(concept);
    }

    final List<Concept> list =
        self.getProperties(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    if (logger.isDebugEnabled())
      logger.debug(String.format("list from properties [%s] with size [%s]", String.valueOf(list),
          list == null ? 0 : list.size()));
    if (list.size() > 0) {
      final Concept concept =
          sparqlQueryManagerService.getProperty(list.get(0).getCode(), term, ip);
      return Optional.of(concept);
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
  public List<Concept> getContributingSources(String terminology) throws Exception {
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (!term.getTerminology().equals("ncit"))
      return new ArrayList<>();

    final Map<String, String> contributingSources = thesaurusProperties.getContributingSources();
    final List<Concept> result = new ArrayList<>();
    for (final String key : contributingSources.keySet().stream().sorted()
        .collect(Collectors.toList())) {
      result.add(new Concept(terminology, key, contributingSources.get(key)));
    }

    return result;
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
    final IncludeParam ip = new IncludeParam("minimal");

    // Like "get properties", if it's "name style", we need to get all and then
    // find
    // this one.
    Concept concept = null;
    if (ModelUtils.isCodeStyle(code)) {
      concept = sparqlQueryManagerService.getProperty(code, term, ip);
    }

    final List<Concept> list =
        self.getProperties(terminology, Optional.ofNullable("minimal"), Optional.ofNullable(code));
    if (list.size() > 0) {
      concept = list.get(0);
    }

    if (concept == null || concept.getCode() == null) {
      return Optional.empty();
    }

    final List<String> propertyValues =
        sparqlQueryManagerService.getAxiomQualifiersList(concept.getCode(), term);
    return Optional.of(propertyValues);
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
  public List<Concept> getTermTypes(String terminology) throws Exception {
    // Verify terminology setting
    TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Concept> list = new ArrayList<>();
    list.add(new Concept(terminology, "AB", "Abbreviation"));
    list.add(new Concept(terminology, "AD", "Adjectival form (and other parts of grammar)"));
    list.add(new Concept(terminology, "AQ*", "Antiquated preferred term"));
    list.add(new Concept(terminology, "AQS",
        "Antiquated term, use when tehre are antiquated synonyms within a concept"));
    list.add(new Concept(terminology, "BR", "US brand name, which may be trademarked"));
    list.add(new Concept(terminology, "CA2", "ISO 3166 alpha-2 country code"));
    list.add(new Concept(terminology, "CA3", "ISO 3166 alpha-3 country code"));
    list.add(new Concept(terminology, "CNU", "ISO 3166 numeric country code"));
    list.add(new Concept(terminology, "CI", "ISO country code"));
    list.add(new Concept(terminology, "CN", "Drug study code"));
    list.add(new Concept(terminology, "CS", "US State Department country code"));
    list.add(new Concept(terminology, "DN", "Display name"));
    list.add(new Concept(terminology, "FB", "Foreign brand name, which may be trademarked"));
    list.add(new Concept(terminology, "LLT", "Lower level term"));
    list.add(
        new Concept(terminology, "HD*", "Header (groups concepts, but not used for coding data)"));
    list.add(new Concept(terminology, "PT*", "Preferred term"));
    list.add(new Concept(terminology, "SN", "Chemical structure name"));
    list.add(new Concept(terminology, "SY", "Synonym"));

    return list;
  }

}