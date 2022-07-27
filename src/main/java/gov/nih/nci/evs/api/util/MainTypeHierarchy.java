
package gov.nih.nci.evs.api.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Extensions;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.service.StardogElasticLoadServiceImpl;

/**
 * Handler for the main type hierarchy CTRP extension computations.
 */
@Service
public class MainTypeHierarchy {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(StardogElasticLoadServiceImpl.class);

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService service;

  /** The elastic query service. */
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The main type set. */
  private Set<String> mainTypeSet = null;

  /** The broad category set. */
  private Set<String> broadCategorySet = null;

  /** The main type hierarchy. */
  private Map<String, Paths> mainTypeHierarchy = null;

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
   * @param hierarchy the hierarchy
   * @throws Exception the exception
   */
  public void initialize(final Terminology terminology, final HierarchyUtils hierarchy)
    throws Exception {

    if (terminology.getTerminology().equals("ncit") && broadCategorySet == null) {

      broadCategorySet = service.getSubsetMembers("C138189", terminology).stream()
          .map(c -> c.getCode()).collect(Collectors.toSet());
      logger.info("  Broad Category Set:");
      logCodes(broadCategorySet, terminology);

      mainTypeSet = service.getSubsetMembers("C138190", terminology).stream().map(c -> c.getCode())
          .collect(Collectors.toSet());
      logger.info("  Main Type Set:");
      logCodes(mainTypeSet, terminology);

      logger.info("  Compute main type hierarchy");
      mainTypeHierarchy =
          service.getMainTypeHierarchy(terminology, mainTypeSet, broadCategorySet, hierarchy);

    }
  }

  /**
   * Returns the extensions.
   *
   * @param concept the concept
   * @return the extensions
   */
  public Extensions getExtensions(final Concept concept) {

    // Identify cases where something is wrong with concept
    if (concept == null) {
      throw new RuntimeException("Unexpectedly null concept");
    }
    if (concept.getName() == null) {
      // If this is a retired concept, we're good, just return blank extensions
      if (concept.getProperties().stream()
          .filter(
              p -> p.getType().equals("Concept_Status") && p.getValue().equals("Retired_Concept"))
          .count() > 0) {
        return new Extensions();
      }
      throw new RuntimeException("Unexpectedly null concept name = " + concept.getCode());
    }

    // Skip for non-NCIT terminologies
    if (!concept.getTerminology().equals("ncit")) {
      return null;
    }
    try {
      final Extensions extensions = new Extensions();
      extensions.setIsDisease(isDisease(concept));
      extensions.setIsDiseaseStage(isDiseaseStage(concept));
      extensions.setIsDiseaseGrade(isDiseaseGrade(concept));
      extensions.setIsMainType(isMainType(concept));
      extensions.setIsBiomarker(isBiomarker(concept));
      extensions.setIsReferenceGene(isReferenceGene(concept));
      extensions.setMainMenuAncestors(getMainMenuAncestors(concept));
      // The concept has at least one main menu ancestor.
      extensions.setIsSubtype(isSubtype(concept) && !extensions.getMainMenuAncestors().isEmpty());
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
   * Indicates whether or not disease stage is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDiseaseStage(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // concept starts with Recurrent
    // OR has a "Disease_Is_Stage" role
    boolean recurrentFlag = concept.getName().toLowerCase().startsWith("recurrent");
    // if (recurrentFlag) {
    // logger.info("QA CASE isDiseaseStage: starts with Recurrent = " +
    // concept.getCode());
    // }
    boolean diseaseIsStageFlag =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    // if (diseaseIsStageFlag) {
    // logger
    // .info("QA CASE isDiseaseStage: member of Disease_Is_Stage subset = " +
    // concept.getCode());
    // }
    return recurrentFlag || diseaseIsStageFlag;
  }

  /**
   * Indicates whether or not disease grade is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDiseaseGrade(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // has a "Disease_Is_Grade" role
    boolean flag =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    // if (flag) {
    // logger
    // .info("QA CASE isDiseaseGrade: member of Disease_Is_Grade subset = " +
    // concept.getCode());
    // }
    return flag;
  }

  /**
   * Indicates whether or not main type is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isMainType(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());

    // if (mainTypeSet.contains(concept.getCode())) {
    // logger.info("QA CASE isMainType: member of main type subset = " +
    // concept.getCode());
    // }
    // if (broadCategorySet.contains(concept.getCode())) {
    // logger.info("QA CASE isMainType: member of broad category subset = " +
    // concept.getCode());
    // }
    return mainTypeSet.contains(concept.getCode()) || broadCategorySet.contains(concept.getCode());
  }

  /**
   * Indicates whether or not biomarker is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isBiomarker(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // Has "Concept_In_Subset" association to C142799 | CTRP Biomarker
    // Terminology
    boolean flag = concept.getAssociations().stream().filter(
        r -> r.getType().equals("Concept_In_Subset") && r.getRelatedCode().contentEquals("C142799"))
        .count() > 0;
    // if (flag) {
    // logger.info("QA CASE isBiomarker: member of CTRP Biomarker Terminology
    // subset = "
    // + concept.getCode());
    // }
    return flag;
  }

  /**
   * Indicates whether or not reference gene is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isReferenceGene(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // Has "Concept_In_Subset" association to C142801 | CTRP Reference Gene
    // Terminology |
    boolean flag = concept.getAssociations().stream().filter(
        r -> r.getType().equals("Concept_In_Subset") && r.getRelatedCode().contentEquals("C142801"))
        .count() > 0;
    // if (flag) {
    // logger.info("QA CASE isReferenceGene: member of CTRP Reference Gene
    // Terminology subset = "
    // + concept.getCode());
    // }
    return flag;
  }

  /**
   * Indicates whether or not disease is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDisease(final Concept concept) throws Exception {
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
  private boolean isNotDisease(final Concept concept) throws Exception {
    final String code = concept.getCode();

    // Disease, Disorder, or Finding
    if (code.equals("C7057")) {
      // logger.info("QA CASE isDisease: top level disease concept = " +
      // concept.getCode());
      return false;
    }
    // Diseases and Disorders
    if (code.equals("C2991")) {
      // logger.info("QA CASE isDisease: top level disease concept = " +
      // concept.getCode());
      return false;
    }

    // CTRP Disease Finding
    // NOTE: CTRP is like this, but it probably <i>should</i> be a disease
    if (code.equals("C173902")) {
      // logger.info("QA CASE !isDisease: broad category exclusion C173902 = " +
      // concept.getCode());
      return true;
    }

    // Main type concept descendants of C2991
    if (mainTypeSet.contains(concept.getCode()) && concept
        .getPaths().getPaths().stream().filter(p -> p.getConcepts().stream()
            .filter(c -> c.getCode().equals("C2991")).findFirst().orElse(null) != null)
        .findFirst().orElse(null) != null) {
      // logger.info("QA CASE !isDisease: is disease main type = " +
      // concept.getCode());
      return false;
    }

    if (broadCategorySet.contains(concept.getCode())) {
      // logger.info("QA CASE !isDisease: is broad category = " +
      // concept.getCode());
      return false;
    }

    // Escape hatch for possibly bad data - needs research
    if (concept.getPaths() == null) {
      return false;
    }
    List<Paths> mma =
        concept.getPaths().rewritePaths(mainTypeHierarchy, mainTypeSet, broadCategorySet);
    if (mma.isEmpty()) {
      // logger.info("QA CASE !isDisease: does not have main menu ancestors = "
      // + concept.getCode());
      return true;
    }
    // logger.info("QA CASE isDisease: has main menu ancestors = " +
    // concept.getCode());
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
      // logger.info("QA CASE !isSubtype: top level concept = " +
      // concept.getCode());
      return false;
    }
    if (isNotDisease(concept)) {
      // logger.info("QA CASE !isSubtype: is not disease = " +
      // concept.getCode());
      return false;
    }

    // Does not qualify if it starts with "recurrent"
    if (concept.getName().toLowerCase().startsWith("recurrent")) {
      // logger.info("QA CASE !isSubtype: starts with Recurrent = " +
      // concept.getCode());
      return false;
    }

    // Member of "Disease_Is_Stage" subset
    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (isDiseaseStage) {
      // if it doesn't have the word "stage"
      if (!concept.getName().toLowerCase().contains("stage")) {
        // logger.info(
        // "QA CASE isSubtype: Disease_Is_Stage not containing 'stage' = " +
        // concept.getCode());
        return true;
      }
      // logger.info("QA CASE !isSubtype: Disease_Is_Stage containing 'stage' =
      // " + concept.getCode());
      return false;
    }

    // Member of "Disease_Is_Grade" subset
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    if (isDiseaseGrade) {
      if (!concept.getName().toLowerCase().contains("grade")) {
        // logger.info("QA CASE isSubtype: Disease_Is_Grade without 'grade' = "
        // + concept.getCode());
        return true;
      }
      // logger.info("QA CASE !isSubtype: Disease_Is_Grade with the 'grade' = "
      // + concept.getCode());
      return false;
    }

    // logger.info("QA CASE isSubtype: default case = " + concept.getCode());
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
        concept.getName().toLowerCase().startsWith("recurrent") || concept.getRoles().stream()
            .filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    final boolean subtypeFlag = isSubtype(concept);
    if (!subtypeFlag && !isDiseaseStage && !isDiseaseGrade) {
      // logger
      // .info("QA CASE !mainMenuAncestors: !subtype && !stage && !grade = " +
      // concept.getCode());
      return null;
    }

    if (concept.getPaths() == null) {
      return null;
    }
    // Get concept paths and check if any end at "main type" concepts
    // If so -> re-render paths as such
    final List<Paths> paths =
        concept.getPaths().rewritePaths(mainTypeHierarchy, mainTypeSet, broadCategorySet);

    // if (subtypeFlag) {
    // logger.info("QA CASE mainMenuAncestors: subtype = " +
    // concept.getCode());
    // }
    // if (isDiseaseStage) {
    // logger.info("QA CASE mainMenuAncestors: Disease_Is_Stage = " +
    // concept.getCode());
    // }
    // if (isDiseaseGrade) {
    // logger.info("QA CASE mainMenuAncestors: Disease_Is_Grade = " +
    // concept.getCode());
    // }

    if (paths.size() == 0) {
      // logger.info("QA CASE !mainMenuAncestors: default no paths = " +
      // concept.getCode());
      return null;
    } else if (paths.size() == 1) {
      if (paths.get(0).getPaths().size() == 0) {
        throw new Exception("This condition should not happen");
        // } else if (paths.get(0).getPaths().size() == 1) {
        // logger.info("QA CASE mainMenuAncestors: default single paths = " +
        // concept.getCode());
        // } else if (paths.get(0).getPaths().size() > 1) {
        // logger.info("QA CASE mainMenuAncestors: default multiple paths = " +
        // concept.getCode());
      }
      // } else if (paths.size() > 1) {
      // logger
      // .info("QA CASE !mainMenuAncestors: default multiple paths lists = " +
      // concept.getCode());
    }
    return paths;
  }

  /**
   * Log set.
   *
   * @param codes the codes
   * @param terminology the terminology
   * @throws Exception the exception
   */
  private void logCodes(final Set<String> codes, final Terminology terminology) throws Exception {
    final Map<String, String> map = new TreeMap<>();
    for (final String conceptCode : codes) {
      final Concept concept =
          service.getConcept(conceptCode, terminology, new IncludeParam("minimal"));
      map.put(concept.getName(), concept.getCode());
    }
    for (final String name : map.keySet()) {
      final String code = map.get(name);
      logger.info("    " + code + " " + name);
    }
  }

}
