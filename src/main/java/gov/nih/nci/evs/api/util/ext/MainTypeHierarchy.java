
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
  @SuppressWarnings("unused")
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
      extensions.setIsDiseaseStage(isDiseaseStage(concept));
      extensions.setIsDiseaseGrade(isDiseaseGrade(concept));
      extensions.setIsMainType(isMainType(concept));
      extensions.setIsSubtype(isSubtype(concept));
      extensions.setIsBiomarker(isBiomarker(concept));
      extensions.setIsReferenceGene(isReferenceGene(concept));
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
   * Indicates whether or not disease stage is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDiseaseStage(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // concept starts with Recurrent
    // OR has a "Disease_Is_Stage" role
    boolean recurrentFlag = concept.getName().toLowerCase().startsWith("recurrent");
    if (recurrentFlag) {
      logger.info("QA CASE isDiseaseStage: starts with Recurrent = " + concept.getCode());
    }
    boolean diseaseIsStageFlag =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (diseaseIsStageFlag) {
      logger
          .info("QA CASE isDiseaseStage: member of Disease_Is_Stage subset = " + concept.getCode());
    }
    return recurrentFlag || diseaseIsStageFlag;
  }

  /**
   * Indicates whether or not disease grade is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDiseaseGrade(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // has a "Disease_Is_Grade" role
    boolean flag =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    if (flag) {
      logger
          .info("QA CASE isDiseaseGrade: member of Disease_Is_Grade subset = " + concept.getCode());
    }
    return flag;
  }

  /**
   * Indicates whether or not main type is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isMainType(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // Has "Concept_In_Subset" association to C138190 | CTS-API Disease Main
    // Type Terminology |
    boolean flag = concept.getAssociations().stream().filter(
        r -> r.getType().equals("Concept_In_Subset") && r.getRelatedCode().contentEquals("C138190"))
        .count() > 0;
    if (flag) {
      logger.info("QA CASE isMainType: member of Main_Type subset = " + concept.getCode());
    }
    return flag;
  }

  /**
   * Indicates whether or not biomarker is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isBiomarker(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // Has "Concept_In_Subset" association to C142799 | CTRP Biomarker
    // Termionlogy
    boolean flag = concept.getAssociations().stream().filter(
        r -> r.getType().equals("Concept_In_Subset") && r.getRelatedCode().contentEquals("C142799"))
        .count() > 0;
    if (flag) {
      logger.info("QA CASE isMainType: member of Biomarker subset = " + concept.getCode());
    }
    return flag;
  }

  /**
   * Indicates whether or not reference gene is the case.
   *
   * @param concept the concept
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isReferenceGene(Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    // Has "Concept_In_Subset" association to C142801 | CTRP Reference Gene
    // Terminology |
    boolean flag = concept.getAssociations().stream().filter(
        r -> r.getType().equals("Concept_In_Subset") && r.getRelatedCode().contentEquals("C142801"))
        .count() > 0;
    if (flag) {
      logger.info("QA CASE isMainType: member of Reference Gene subset = " + concept.getCode());
    }
    return flag;
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
    if (code.compareTo("C7057") == 0) {
      logger.info("QA CASE isDisease: top level disease concept = " + concept.getCode());
      return false;
    }
    // Diseases and Disorders
    if (code.compareTo("C2991") == 0) {
      logger.info("QA CASE isDisease: top level disease concept = " + concept.getCode());
      return false;
    }

    // Check "concept in subset" association to
    // CTS_API_Disease_Broad_Category_Terminology_Code = "C138189"
    if (concept.getAssociations().stream()
        .filter(
            a -> a.getType().equals("Concept_In_Subset") && a.getRelatedCode().equals("C138189"))
        .count() > 0) {
      logger.info("QA CASE isDisease: broad category concept = " + concept.getCode());
      return false;
    }

    List<Paths> mma = concept.getPaths().rewritePathsForAncestors(mainTypeSet);
    if (mma == null || mma.isEmpty()) {
      logger.info("QA CASE !isDisease: does not have main menu ancestors = " + concept.getCode());
      return true;
    }
    logger.info("QA CASE isDisease: has main menu ancestors = " + concept.getCode());
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
      logger.info("QA CASE !isSubtype: top level concept = " + concept.getCode());
      return false;
    }
    if (isNotDisease(concept)) {
      logger.info("QA CASE !isSubtype: is not disease = " + concept.getCode());
      return false;
    }

    // Does not qualify if it starts with "recurrent"
    if (concept.getName().toLowerCase().startsWith("recurrent")) {
      logger.info("QA CASE !isSubtype: starts with Recurrent = " + concept.getCode());
      return false;
    }

    // Member of "Disease_Is_Stage" subset
    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (isDiseaseStage) {
      // if it doesn't have the word "stage"
      if (!concept.getName().toLowerCase().contains("stage")) {
        logger.info(
            "QA CASE isSubtype: Disease_Is_Stage not containing 'stage' = " + concept.getCode());
        return true;
      }
      logger.info("QA CASE !isSubtype: Disease_Is_Stage containing 'stage' = " + concept.getCode());
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
        logger
            .info("QA CASE isSubtype: Main Type with non-main-type parent = " + concept.getCode());
        return true;
      } else {
        logger.info("QA CASE !isSubtype: Main Type with main-type parent = " + concept.getCode());
        return false;
      }
    }

    // Member of "Disease_Is_Grade" subset
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    if (isDiseaseGrade) {
      if (concept.getName().toLowerCase().indexOf("grade") == -1) {
        logger.info("QA CASE isSubtype: Disease_Is_Grade without 'grade' = " + concept.getCode());
        return true;
      }
      logger.info("QA CASE !isSubtype: Disease_Is_Grade with the 'grade' = " + concept.getCode());
      return false;
    }

    logger.info("QA CASE isSubtype: default case = " + concept.getCode());
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
    final boolean subtypeFlag = isSubtype(concept);
    if (!subtypeFlag && !isDiseaseStage && !isDiseaseGrade) {
      logger
          .info("QA CASE !mainMenuAncestors: !subtype && !stage && !grade = " + concept.getCode());
      return new ArrayList<>();
    }

    // Get concept paths and check if any end at "main type" concepts
    // If so -> re-render paths as such
    List<Paths> paths = concept.getPaths().rewritePathsForAncestors(mainTypeSet);

    if (subtypeFlag) {
      logger.info("QA CASE mainMenuAncestors: subtype = " + concept.getCode());
    }
    if (isDiseaseStage) {
      logger.info("QA CASE mainMenuAncestors: Disease_Is_Stage = " + concept.getCode());
    }
    if (isDiseaseGrade) {
      logger.info("QA CASE mainMenuAncestors: Disease_Is_Grade = " + concept.getCode());
    }

    if (paths.size() == 0) {
      logger.info("QA CASE !mainMenuAncestors: default no paths = " + concept.getCode());
    } else if (paths.size() == 1) {
      if (paths.get(0).getPaths().size() == 0) {
        throw new Exception("This condition should not happen");
      } else if (paths.get(0).getPaths().size() == 1) {
        logger.info("QA CASE !mainMenuAncestors: default single paths = " + concept.getCode());
      } else if (paths.get(0).getPaths().size() > 1) {
        logger.info("QA CASE !mainMenuAncestors: default multipe paths = " + concept.getCode());
      }
    } else if (paths.size() > 1) {
      logger
          .info("QA CASE !mainMenuAncestors: default multiple paths lists = " + concept.getCode());
    }
    return paths;
  }

}
