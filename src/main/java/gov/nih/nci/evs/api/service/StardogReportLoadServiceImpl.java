package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** The implementation for {@link ElasticLoadService} that just generates a report. */
@Service
public class StardogReportLoadServiceImpl extends AbstractStardogLoadServiceImpl {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(StardogReportLoadServiceImpl.class);

  /** The mapper. */
  private ObjectMapper mapper = new ObjectMapper();

  /** The lines. */
  private List<String> lines = new ArrayList<>();

  /** the environment *. */
  @Autowired Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /** The sparql query manager service. */
  @Autowired private SparqlQueryManagerService sparqlQueryManagerService;

  /** The stardog properties. */
  @Autowired StardogProperties stardogProperties;

  /** The main type hierarchy. */
  @Autowired MainTypeHierarchy mainTypeHierarchy;

  /* see superclass */
  @Override
  public int loadConcepts(
      final ElasticLoadConfig config, final Terminology terminology, final HierarchyUtils hierarchy)
      throws Exception {

    final String resource = "metadata/" + terminology.getTerminology() + ".txt";

    // Load samples from file
    final Set<String> samples = new HashSet<>();
    try (final InputStream is =
        terminology.getClass().getClassLoader().getResourceAsStream(resource)) {
      for (final String line : IOUtils.toString(is, "UTF-8").split("[\r\n]")) {
        if (line.isEmpty() || line.startsWith("# ")) {
          continue;
        }
        samples.add(line);
      }
    }
    logReport("  ", "samples = " + samples.size());

    // Get all concepts
    List<Concept> concepts = sparqlQueryManagerService.getAllConceptsWithoutCode(terminology);

    try {
      logReport("  ", "concepts without codes = " + concepts.size());
      int ct = 0;
      for (final Concept concept : concepts) {
        if (++ct < 3 || samples.contains(concept.getCode())) {
          final Concept concept2 =
              sparqlQueryManagerService.getConcept(
                  concept.getUri(), terminology, new IncludeParam("full"));
          concept2.setUri(concept.getUri());
          logReport("    ", "concept", concept2);
        }
      }
    } catch (final Exception e) {
      throw new IOException(e);
    }

    // Get all concepts
    concepts = sparqlQueryManagerService.getAllConceptsWithCode(terminology);
    try {
      logReport("  ", "concepts with codes = " + concepts.size());
      int ct = 0;
      for (final Concept concept : concepts) {
        if (++ct < (6 - samples.size()) || samples.contains(concept.getCode())) {
          logReport(
              "    ",
              "concept",
              sparqlQueryManagerService.getConcept(
                  concept.getCode(), terminology, new IncludeParam("full")));
          // logReport(" ", " paths", hierarchy.getPaths(terminology,
          // concept.getCode()));
        }
      }

    } catch (final Exception e) {
      throw new IOException(e);
    }

    return -1;
  }

  /* see superclass */
  @Override
  public void loadObjects(
      final ElasticLoadConfig config, final Terminology terminology, final HierarchyUtils hierarchy)
      throws Exception {

    // TODO: show hierarchy (passed in)

    // Show qualifiers
    final List<Concept> qualifiers =
        sparqlQueryManagerService.getAllQualifiersCache(terminology, new IncludeParam("full"));
    logReport("  ", "qualifiers", qualifiers);

    // Show remodeled qualifiers
    final List<Concept> remodeledQualifiers =
        sparqlQueryManagerService.getRemodeledQualifiers(terminology, new IncludeParam("full"));
    logReport("  ", "remodeled qualifiers", remodeledQualifiers);

    // Show qualifier values by code and by qualifier name
    final Map<String, Set<String>> map = new HashMap<>();
    for (final Concept qualifier : qualifiers) {
      for (final String value :
          sparqlQueryManagerService.getQualifierValues(qualifier.getCode(), terminology)) {
        if (!map.containsKey(qualifier.getCode())) {
          map.put(qualifier.getCode(), new HashSet<>());
        }
        if (!map.containsKey(qualifier.getName())) {
          map.put(qualifier.getName(), new HashSet<>());
        }
        if (map.get(qualifier.getCode()).size() > 5) {
          map.get(qualifier.getCode()).add("...");
          map.get(qualifier.getName()).add("...");
          break;
        }
        map.get(qualifier.getCode()).add(value);
        map.get(qualifier.getName()).add(value);
      }
    }
    logReport("  ", "qualifier values", map);

    // Show properties
    final List<Concept> properties =
        sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
    logReport("  ", "properties", properties);

    // Show remodeled properties
    final List<Concept> remodeledProperties =
        sparqlQueryManagerService.getRemodeledProperties(terminology, new IncludeParam("full"));
    logReport("  ", "remodeled properties", remodeledProperties);

    // Show never used properties
    final List<Concept> neverUsedProperties =
        sparqlQueryManagerService.getNeverUsedProperties(terminology, new IncludeParam("full"));
    logReport("  ", "never used properties", neverUsedProperties);

    // Show associations
    final List<Concept> associations =
        sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
    logReport("  ", "associations", associations);

    // Show roles
    final List<Concept> roles =
        sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
    logReport("  ", "roles", roles);

    // Show synonym sources
    final List<ConceptMinimal> synonymSources =
        sparqlQueryManagerService.getSynonymSources(terminology);
    logReport("  ", "synonym sources", synonymSources);

    // Show definition sources
    if (terminology.getMetadata().getDefinitionSource() != null) {
      final List<ConceptMinimal> definitionSources =
          sparqlQueryManagerService.getDefinitionSources(terminology);
      logReport("  ", "definition sources", definitionSources);
    }

    // Show concept statuses
    if (terminology.getMetadata().getConceptStatus() != null) {
      final List<String> conceptStatuses =
          sparqlQueryManagerService.getDistinctPropertyValues(
              terminology, terminology.getMetadata().getConceptStatus());
      // Hack borrowed from superclass to fix "true" as a value
      if (conceptStatuses.size() == 1 && "true".equals(conceptStatuses.get(0))) {
        conceptStatuses.clear();
        conceptStatuses.add("Retired_Concept");
      }
      logReport("  ", "concept statuses = " + conceptStatuses);
    }

    // Show synonym types
    final List<Concept> synonymTypes =
        sparqlQueryManagerService.getAllSynonymTypes(terminology, new IncludeParam("full"));
    logReport("  ", "synonym types", synonymTypes);

    // Show synonym types
    if (terminology.getMetadata().getDefinition() != null) {
      final List<Concept> definitionTypes =
          sparqlQueryManagerService.getAllDefinitionTypes(terminology, new IncludeParam("full"));
      logReport("  ", "definition types", definitionTypes);
    }

    // LATER (ncit only): Show subsets
    // List<Concept> subsets =
    // sparqlQueryManagerServiceImpl.getAllSubsets(terminology);
    // ElasticObject subsetsObject = new ElasticObject("subsets");
    // for (Concept subset : subsets)
    // addSubsetLinks(subset, terminology.getMetadata().getSubsetLinks(),
    // terminology.getMetadata().getSubsetPrefix());
    // subsetsObject.setConcepts(subsets);

    // Show association entries
    // for (Concept association : associations) {
    // logger.info(association.getName());
    // if (association.getName().equals("Concept_In_Subset"))
    // continue;
    // List<AssociationEntry> entries =
    // sparqlQueryManagerService.getAssociationEntries(terminology,
    // association);
    // }

  }

  /* see superclass */
  @Override
  public Terminology getTerminology(
      final ApplicationContext app,
      final ElasticLoadConfig config,
      final String filepath,
      final String terminology,
      final boolean forceDelete)
      throws Exception {

    // Write report header
    lines.add("--------------------------------------------------------");
    lines.add("Started ..." + new Date());
    lines.add("--------------------------------------------------------");

    final Terminology term = super.getTerminology(app, config, filepath, terminology, forceDelete);
    final TerminologyMetadata metadata = term.getMetadata();
    term.setMetadata(null);
    logReport("  ", "terminology = " + term.getTerminology());
    logReport("    ", null, term);
    logReport("  ", "metadata", metadata);
    term.setMetadata(metadata);

    return term;
  }

  /* see superclass */
  @Override
  public void checkLoadStatus(final int total, final Terminology term) throws IOException {
    // n/a - report only
  }

  /* see superclass */
  @Override
  public void loadIndexMetadata(final int total, final Terminology term) throws IOException {
    // n/a - report only
  }

  /* see superclass */
  @Override
  public void cleanStaleIndexes(final Terminology terminology) throws Exception {
    // n/a - report only
  }

  /* see superclass */
  @Override
  public void updateLatestFlag(final Terminology terminology) throws Exception {
    // n/a - report only
    lines.add("--------------------------------------------------------");
    lines.add("Finished ..." + new Date());
    lines.add("--------------------------------------------------------");
    FileUtils.writeLines(new File("report.txt"), "UTF-8", lines, "\n");
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(final Terminology term) throws Exception {
    final HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtilsCache(term);
    logReport("  ", "hierarchy = " + hierarchy.getPathsMap(term).size());
    logReport("  ", "roots = " + hierarchy.getHierarchyRoots());
    final String minPathsCode = hierarchy.getCodeWithMinPaths(term);
    logReport(
        "  ",
        "  min paths = "
            + minPathsCode
            + ", "
            + hierarchy.getPathsMap(term).get(minPathsCode).size());
    final String maxPathsCode = hierarchy.getCodeWithMaxPaths(term);
    logReport(
        "  ",
        "  max paths = "
            + maxPathsCode
            + ", "
            + hierarchy.getPathsMap(term).get(maxPathsCode).size());
    final String maxChildrenCode = hierarchy.getCodeWithMaxChildren(term);
    logReport(
        "  ",
        "  max children = "
            + maxChildrenCode
            + ", "
            + (maxChildrenCode == null ? "0" : hierarchy.getChildNodes(maxChildrenCode, 0).size()));

    return hierarchy;
  }

  /**
   * Log report.
   *
   * @param indent the indent
   * @param line the line
   */
  private void logReport(final String indent, final String line) {
    logger.info(indent + line);
    lines.add(indent + line);
  }

  /**
   * Log report.
   *
   * @param indent the indent
   * @param label the label
   * @param object the object
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void logReport(final String indent, final String label, final Object object)
      throws IOException {
    final String str = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    if (label != null) {
      logger.info(indent + label + " = " + object);
      lines.add(indent + label + " = ");
      lines.addAll(
          Arrays.asList(str.split("\n")).stream()
              .map(s -> indent + "  " + s)
              .collect(Collectors.toList()));
    } else {
      logger.info(indent + object);
      lines.addAll(
          Arrays.asList(str.split("\n")).stream()
              .map(s -> indent + s)
              .collect(Collectors.toList()));
    }
  }
}
