
package gov.nih.nci.evs.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
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

  /** The elastic query service *. */
  @Autowired
  private ElasticQueryService esQueryService;

  /** The self. */
  @Resource
  private MetadataService self;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

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
  @Cacheable(value = "metadata", key = "{#root.methodName,#include.orElse(''),#terminology}",
      condition = "#list.orElse('').isEmpty()")
  public List<Concept> getAssociations(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> associations = esQueryService.getAssociations(term, ip);
    return ConceptUtils.applyList(associations, ip, list.orElse(null));
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
  public Optional<Concept> getAssociation(String terminology, String code, Optional<String> include) throws Exception {
    // Verify that it is an association
    final List<Concept> list =
        self.getAssociations(terminology, Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getAssociation(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many associations " + code + " found");
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
  public List<Concept> getRoles(String terminology, Optional<String> include, Optional<String> list) throws Exception {

    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> roles = esQueryService.getRoles(term, ip);
    return ConceptUtils.applyList(roles, ip, list.orElse(null));
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
  public Optional<Concept> getRole(String terminology, String code, Optional<String> include) throws Exception {

    // Verify that it is a role
    final List<Concept> list =
        self.getRoles(terminology, Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getRole(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many roles " + code + " found");
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
  public List<Concept> getProperties(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    // Remove qualifiers from properties list
    final List<Concept> properties = esQueryService.getProperties(term, ip);

    return ConceptUtils.applyList(properties, ip, list.orElse(null));
  }

  /* see superclass */
  @Override
  public List<Concept> getQualifiers(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> qualifiers = esQueryService.getQualifiers(term, ip);

    // IF "for documentation" mode, remove the "not considered" cases.
    return ConceptUtils.applyList(qualifiers, ip, list.orElse(null));
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
  public Optional<Concept> getQualifier(String terminology, String code, Optional<String> include) throws Exception {
    // Verify that it is a qualifier
    final List<Concept> list = self.getQualifiers(terminology, Optional.of("minimal"), Optional.ofNullable(code));

    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getQualifier(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many qualifiers " + code + " found");
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
  public Optional<Concept> getProperty(String terminology, String code, Optional<String> include) throws Exception {

    // Verify that it is a property
    final List<Concept> list = self.getProperties(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getProperty(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many properties " + code + " found");
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
  public Optional<List<String>> getConceptStatuses(String terminology) throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    return Optional.of(term.getMetadata().getConceptStatuses());
  }

  /* see superclass */
  @Override
  public List<ConceptMinimal> getDefinitionSources(String terminology) throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);

    // Build the list from terminology metadata
    return buildList(term, term.getMetadata().getDefinitionSourceSet(), term.getMetadata().getSources());

  }

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  @Override
  public List<ConceptMinimal> getSynonymSources(String terminology) throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    if (!term.getTerminology().equals("ncit")) {
      // Build the list from terminology metadata
      return buildList(term, term.getMetadata().getSynonymSourceSet(), term.getMetadata().getSources());
    }

    return esQueryService.getSynonymSources(term);
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
  public Optional<List<String>> getQualifierValues(String terminology, String code) throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    final Map<String, Set<String>> map = esQueryService.getQualifierValues(term);
    if (map == null || !map.containsKey(code)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");
    }

    return Optional.ofNullable(map.get(code).stream().sorted().collect(Collectors.toList()));

  }

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  @Override
  public List<ConceptMinimal> getTermTypes(String terminology) throws Exception {

    final Terminology term = termUtils.getTerminology(terminology, true);
    // Build the list from terminology metadata
    return buildList(term, term.getMetadata().getTermTypes().keySet(), term.getMetadata().getTermTypes());

  }

  /**
   * Returns the welcome text.
   *
   * @param terminology the terminology
   * @return the welcome text
   * @throws Exception the exception
   */
  @Override
  // @Cacheable(value = "metadata", key = "{#root.methodName, #terminology}")
  public String getWelcomeText(String terminology) throws Exception {

    final Terminology term = termUtils.getTerminology(terminology, true);
    // Build the list from terminology metadata
    return term.getMetadata().getWelcomeText();

  }

  /* see superclass */
  @Override
  public List<Concept> getSynonymTypes(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {

    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> synonymTypes = esQueryService.getSynonymTypes(term, ip);
    return ConceptUtils.applyList(synonymTypes, ip, list.orElse(null));
  }

  /* see superclass */
  @Override
  public Optional<Concept> getSynonymType(String terminology, String code, Optional<String> include) throws Exception {

    // Verify that it is a synonym type
    final List<Concept> list =
        self.getSynonymTypes(terminology, Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getSynonymType(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many synonym types " + code + " found");
    }
    return Optional.empty();
  }

  /* see superclass */
  @Override
  public List<Concept> getDefinitionTypes(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {

    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<Concept> definitionTypes = esQueryService.getDefinitionTypes(term, ip);
    return ConceptUtils.applyList(definitionTypes, ip, list.orElse(null));
  }

  /* see superclass */
  @Override
  public Optional<Concept> getDefinitionType(String terminology, String code, Optional<String> include)
    throws Exception {

    // Verify that it is a definition type
    final List<Concept> list =
        self.getDefinitionTypes(terminology, Optional.ofNullable(include.orElse("minimal")), Optional.ofNullable(code));
    if (list.size() == 1) {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));
      return esQueryService.getDefinitionType(list.get(0).getCode(), term, ip);
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many definition types " + code + " found");
    }
    return Optional.empty();
  }

  /* see superclass */
  public AssociationEntryResultList getAssociationEntries(String terminology, String label, Integer fromRecord,
    Integer pageSize) throws Exception {
    return esQueryService.getAssociationEntries(terminology, label, fromRecord, pageSize);
  }

  /* see superclass */
  @Override
  public List<Concept> getSubsets(String terminology, Optional<String> include, Optional<String> list)
    throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    // subsets should always return children
    // (contributing source needed)
    ip.setChildren(true);
    // ip.setProperties(true);
    ip.setSubsetLink(true);
    List<Concept> subsets = esQueryService.getSubsets(term, ip);
    // No list of codes supplied
    if (!list.isPresent()) {
      final Set<String> codes =
          subsets.stream().flatMap(Concept::streamSelfAndChildren).map(c -> c.getCode()).collect(Collectors.toSet());
      final Map<String, Concept> conceptMap = esQueryService.getConceptsAsMap(codes, term, ip);

      subsets.stream().flatMap(Concept::streamSelfAndChildren)
          // Populate concepts
          .peek(c -> c.populateFrom(conceptMap.get(c.getCode())))
          // Keep only concepts that do have no "Publish_Value_Set" != "Yes" properties
          .filter(c -> c.getProperties().stream()
              .filter(p -> p.getType().equals("Publish_Value_Set") && !p.getValue().equals("Yes")).count() == 0)
          // Apply include
          .peek(c -> ConceptUtils.applyInclude(c, ip)).collect(Collectors.toList());

      return subsets;
    }

    // List of codes supplied - first apply the filter.
    subsets = ConceptUtils.applyListWithChildren(subsets, ip, list.orElse(null)).stream().collect(Collectors.toSet())
        .stream().collect(Collectors.toList());
    subsets.stream().peek(c -> c.populateFrom(esQueryService.getConcept(c.getCode(), term, ip).get(), true))
        .peek(c -> ConceptUtils.applyInclude(c, ip)).collect(Collectors.toList());

    return subsets;

  }

  /* see superclass */
  @Override
  public Optional<Concept> getSubset(String terminology, String code, Optional<String> include) throws Exception {

    // Verify that it is a property
    final List<Concept> list = self.getSubsets(terminology, include, Optional.ofNullable(code));
    if (list.size() == 1) {
      return Optional.of(list.get(0));
    } else if (list.size() > 1) {
      throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "Too many subsets " + code + " found");
    }
    return Optional.empty();
  }

  /**
   * Builds the list.
   *
   * @param terminology the terminology
   * @param values the values
   * @param nameMap the name map
   * @return the list
   */
  private List<ConceptMinimal> buildList(final Terminology terminology, final Set<String> values,
    final Map<String, String> nameMap) {
    final List<ConceptMinimal> list = new ArrayList<>();
    for (final String value : values) {
      final ConceptMinimal concept = new ConceptMinimal();
      concept.setCode(value);
      concept.setName(nameMap.get(value));
      concept.setTerminology(terminology.getTerminology());
      concept.setVersion(terminology.getVersion());
      list.add(concept);
    }
    Collections.sort(list, new Comparator<ConceptMinimal>() {

      @Override
      public int compare(final ConceptMinimal c1, final ConceptMinimal c2) {
        return c1.getCode().compareTo(c2.getCode());
      }

    });
    return list;
  }
}
