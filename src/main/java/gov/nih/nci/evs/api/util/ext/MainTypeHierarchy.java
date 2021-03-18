
package gov.nih.nci.evs.api.util.ext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Extensions;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.service.StardogElasticLoadServiceImpl;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * Handler for the main type hierarchy CTRP extension computations.
 */
@Service
public class MainTypeHierarchy {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(StardogElasticLoadServiceImpl.class);

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService service;

  /** The elastic query service. */
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The main type set. */
  private Set<String> mainTypeSet = new HashSet<>();

  /** The hierarchy. */
  private HierarchyUtils hierarchy = null;

  /**
   * Instantiates an empty {@link MainTypeHierarchy}.
   */
  public MainTypeHierarchy() {
    // n/a
  }

  /**
   * Initialize.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  public void initialize(Terminology terminology) throws Exception {

    if (terminology.getTerminology().equals("ncit") && hierarchy == null) {
      hierarchy = service.getHierarchyUtils(terminology);

      mainTypeSet = service.getSubsetMembers("C138190", terminology).stream().map(c -> c.getCode())
          .collect(Collectors.toSet());
    }
  }

  /**
   * Returns the extensions.
   *
   * @param concept the concept
   * @return the extensions
   */
  public Extensions getExtensions(final Concept concept) {
    // Skip for non-NCIT terminologies
    if (!concept.getTerminology().equals("ncit")) {
      return null;
    }
    try {
      final Extensions extensions = new Extensions();
      extensions.setIsDisease(isDisease(concept));
      extensions.setIsSubtype(isSubtype(concept));
      extensions.setMainMenuAncestors(getMainMenuAncestors(concept));
      return extensions;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Terminology check.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  private void terminologyCheck(final String terminology) throws Exception {
    if (!"ncit".equals(terminology)) {
      throw new Exception("Requires 'ncit' terminology = " + terminology);
    }
  }

  /**
   * Indicates whether or not disease is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDisease(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    return !isNotDisease(concept);
  }

  /**
   * Indicates whether or not not disease is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  private boolean isNotDisease(Concept concept) throws Exception {
    final String code = concept.getCode();

    // Disease, Disorder, or Finding
    if (code.compareTo("C7057") == 0)
      return false;
    // Diseases and Disorders
    if (code.compareTo("C2991") == 0)
      return false;

    // Check "concept in subset" association to
    // CTS_API_Disease_Broad_Category_Terminology_Code = "C138189"
    if (concept.getAssociations().stream()
        .filter(
            a -> a.getType().equals("Concept_In_Subset") && a.getRelatedCode().equals("C138189"))
        .count() > 0) {
      return false;
    }

    List<Paths> mma = concept.getPaths().rewritePathsForAncestors(mainTypeSet);
    if (mma == null || mma.isEmpty()) {
      return true;
    }
    return false;
  }

  /**
   * Indicates whether or not subtype is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isSubtype(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    String code = concept.getCode();

    // Disease, Disorder, or Finding
    if (code.equals("C7057")) {
      return false;
    }
    if (isNotDisease(concept)) {
      return false;
    }

    // Does not qualify if it starts with "recurrent"
    if (concept.getName().toLowerCase().startsWith("recurrent")) {
      return false;
    }

    // Member of "Disease_Is_Stage" subset
    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (isDiseaseStage) {
      // if it doesn't have the word "stage"
      if (!concept.getName().toLowerCase().contains("stage"))
        return true;
      return false;
    }

    // Check participation in the main types set
    final boolean isMainType = concept.getAssociations().stream()
        .filter(
            a -> a.getType().equals("Concept_In_Subset") && a.getRelatedCode().equals("C138190"))
        .count() > 0;

    if (isMainType) {
      // TODO: may have to look higher than superclass (e.g. paths)
      final List<String> parents = hierarchy.getSuperclassCodes(code);
      // If there are parents that are not main type codes,
      // then it qualifies
      if (parents.stream().filter(c -> !mainTypeSet.contains(code)).count() > 0) {
        return true;
      } else {
        return false;
      }
    }

    // Member of "Disease_Is_Grade" subset
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    if (isDiseaseGrade) {
      if (concept.getName().indexOf("grade") == -1) {
        return true;
      }
      return false;
    }

    return true;
  }

  /**
   * Returns the main menu ancestors.
   *
   * @param concept the concept
   * @return the main menu ancestors
   * @throws Exception the exception
   */
  public List<Paths> getMainMenuAncestors(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());

    // No main menu ancestors if is disease grade or stage or subtype.
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (!isSubtype(concept) && !isDiseaseStage && !isDiseaseGrade) {
      return new ArrayList<>();
    }

    // Get concept paths and check if any end at "main type" concepts
    // If so -> re-render paths as such
    List<Paths> paths = concept.getPaths().rewritePathsForAncestors(mainTypeSet);
    return paths;
  }

}
