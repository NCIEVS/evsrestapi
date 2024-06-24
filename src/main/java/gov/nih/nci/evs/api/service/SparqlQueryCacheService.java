package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * This class will handle the Caching when querying for datasets. It has to be in a separate class
 * from where the cache is called from, otherwise the @Cacheable annotation is ignored and the
 * method is treated as private while using proxy-based AOP.
 */
@Service
public class SparqlQueryCacheService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(SparqlQueryManagerServiceImpl.class);

  /** The query builder service. */
  @Autowired QueryBuilderService queryBuilderService;

  /**
   * Returns the hierarchy.
   *
   * @param terminology the terminology
   * @param restUtils the rest utils
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the hierarchy
   * @throws Exception the exception
   */
  @Cacheable(
      value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public List<String> getHierarchy(
      final Terminology terminology,
      final RESTUtils restUtils,
      final SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    final List<String> parentchild = new ArrayList<>();
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("hierarchy", terminology);
    final String res =
        restUtils.runSPARQL(queryPrefix + query, sparqlQueryManagerService.getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final StringBuffer str = new StringBuffer();
      str.append(
          b.getParentCode() == null
              ? EVSUtils.getCodeFromUri(b.getParent().getValue())
              : b.getParentCode().getValue());
      str.append("\t");
      str.append(EVSUtils.getParentLabel(b));
      str.append("\t");
      str.append(
          b.getChildCode() == null
              ? EVSUtils.getCodeFromUri(b.getChild().getValue())
              : b.getChildCode().getValue());
      str.append("\t");
      str.append(EVSUtils.getChildLabel(b));
      str.append("\n");
      parentchild.add(str.toString());
    }

    // Add role entries
    parentchild.addAll(getHierarchyRoleHelper(terminology, restUtils, sparqlQueryManagerService));

    return parentchild;
  }

  /**
   * Returns the hierarchy role helper.
   *
   * @param terminology the terminology
   * @param restUtils the rest utils
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the hierarchy role helper
   * @throws Exception the exception
   */
  private List<String> getHierarchyRoleHelper(
      final Terminology terminology,
      final RESTUtils restUtils,
      final SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    final List<String> parentchild = new ArrayList<>();
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery(
            "roles.hierarchy",
            terminology,
            new ArrayList<>(terminology.getMetadata().getHierarchyRoles()));
    final String res =
        restUtils.runSPARQL(queryPrefix + query, sparqlQueryManagerService.getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final StringBuffer str = new StringBuffer();
      str.append(
          b.getParentCode() == null
              ? EVSUtils.getCodeFromUri(b.getParent().getValue())
              : b.getParentCode().getValue());
      str.append("\t");
      str.append(EVSUtils.getParentLabel(b));
      str.append("\t");
      str.append(
          b.getChildCode() == null
              ? EVSUtils.getCodeFromUri(b.getChild().getValue())
              : b.getChildCode().getValue());
      str.append("\t");
      str.append(EVSUtils.getChildLabel(b));
      str.append("\n");
      parentchild.add(str.toString());
    }

    logger.info("  role hierarchy entries = " + parentchild.size());
    return parentchild;
  }

  /**
   * Returns the all qualifiers.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @param restUtils the rest utils
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the all qualifiers
   * @throws Exception the exception
   */
  @Cacheable(
      value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(),#ip.toString()}")
  public List<Concept> getAllQualifiers(
      final Terminology terminology,
      final IncludeParam ip,
      final RESTUtils restUtils,
      final SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.qualifiers", terminology);
    final String res =
        restUtils.runSPARQL(queryPrefix + query, sparqlQueryManagerService.getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Qualifier> qualifiers = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Qualifier qualifier = new Qualifier();
      if (b.getPropertyCode() == null) {
        qualifier.setUri(b.getProperty().getValue());
      }
      qualifier.setCode(EVSUtils.getPropertyCode(b));
      qualifiers.add(qualifier);
    }

    final TerminologyMetadata md = terminology.getMetadata();
    for (final Qualifier qualifier : qualifiers) {

      // Send URI or code
      final Concept concept =
          sparqlQueryManagerService.getQualifier(
              qualifier.getUri() != null ? qualifier.getUri() : qualifier.getCode(),
              terminology,
              ip);

      // Skip unpublished qualifiers
      if (md.isUnpublished(qualifier.getCode()) || md.isUnpublished(qualifier.getUri())) {
        continue;
      }

      // Mark remodeled qualifiers
      if (md.isRemodeledQualifier(qualifier.getCode())
          || md.isRemodeledQualifier(qualifier.getUri())) {
        concept.getProperties().add(new Property("remodeled", "true"));
        if (qualifier.getCode() != null) {
          concept
              .getProperties()
              .add(
                  new Property(
                      "remodeledDescription",
                      "Remodeled as " + md.getRemodeledAsType(null, qualifier, md)));
        }
      }

      concepts.add(concept);
    }
    return concepts;
  }

  /**
   * Get hierarchy for a given terminology.
   *
   * @param terminology the terminology
   * @param restUtils the rest utils
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the hierarchy
   * @throws Exception the exception
   */
  @Cacheable(
      value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public HierarchyUtils getHierarchyUtils(
      final Terminology terminology,
      final RESTUtils restUtils,
      final SparqlQueryManagerService sparqlQueryManagerService)
      throws Exception {
    final List<String> parentchild =
        this.getHierarchy(terminology, restUtils, sparqlQueryManagerService);
    return new HierarchyUtils(terminology, parentchild);
  }
}
