
package gov.nih.nci.evs.api.util.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The Class MainTypeHierarchy.
 */
@Service
public class MainTypeHierarchy {

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService service;

  /** The main type set. */
  private List<String> mainTypeSet = new ArrayList<>();

  /** The broad category set. */
  private List<String> broadCategorySet = new ArrayList<>();

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
  private void initialize(Terminology terminology) throws Exception {
    if (hierarchy == null) {
      hierarchy = service.getHierarchyUtils(terminology);

      // TODO: use sparql manager service to get these 2 subsets - just the
      // codes

      // static String CTS_API_Disease_Main_Type_Terminology_Code = "C138190";
      // mainTypeSet = ...;

      // static String CTS_API_Disease_Broad_Category_Terminology_Code =
      // "C138189";
      // broadCategorySet = ...;

      //
    }
  }

  /**
   * Find main menu ancestors.
   *
   * @param concept the concept
   * @return the list
   * @throws Exception the exception
   */
  private List<String> findMainMenuAncestors(final Concept concept) throws Exception {
    final String rootCode = concept.getCode();

    int maxLevel = -1;
    final List<String> mainMenuAncestors = new ArrayList<>();
    final List<String> tree = findCode2MainTypesTree(concept);
    for (final String line : tree) {
      final String[] fields = line.split("\\|");
      final String parent_label = fields[0];
      final String parentCode = fields[1];
      final String child_label = fields[2];
      final String child_code = fields[3];

      if (parentCode.compareTo(rootCode) != 0) {
        Integer n1 = (Integer) levelMap.get(parentCode);
        if (n1 != null) {
          int i1 = n1.intValue();
          if (i1 > maxLevel) {
            maxLevel = i1;
            mainMenuAncestors.add(parent_label + "|" + parentCode + "|" + maxLevel);
          } else if (i1 == maxLevel) {
            String t = parent_label + "|" + parentCode + "|" + maxLevel;
            if (!w.contains(t)) {
              w.add(t);
            }
          }
        }
      }

      if (child_code.compareTo(rootCode) != 0) {
        Integer n2 = (Integer) levelMap.get(child_code);
        if (n2 != null) {
          int i2 = n2.intValue();
          if (i2 > maxLevel) {
            maxLevel = i2;
            w = new Vector();
            w.add(child_label + "|" + child_code + "|" + maxLevel);
          } else if (i2 == maxLevel) {
            String t = child_label + "|" + child_code + "|" + maxLevel;
            if (!w.contains(t)) {
              w.add(t);
            }
          }
        }
      }
    }
    return w;
  }

  /**
   * Find code 2 main types tree.
   *
   * @param concept the concept
   * @return the list
   * @throws Exception the exception
   */
  public List<String> findCode2MainTypesTree(Concept concept) throws Exception {
    final Terminology terminology = service.getTerminologies().stream().
        filter(t -> "ncit".getName(t.getTerminology())).findFirst().orElse(null);
    if (terminology == null) {
      return new ArrayList<>();
    }
    final HierarchyUtils hierarchy = service.getHierarchyUtils(terminology);
    final Set<String> visitedNodes = new HashSet<>();
    List<String> codeToMainType = new ArrayList<>();
    LinkedList<String> queue = new LinkedList<>();
    queue.add("@|" + rootCode);
    while (!queue.isEmpty()) {
      final String line = queue.remove();
      final String[] fields = line.split("\\|");
      String parentCode = fields[0];
      String code = fields[1];

      // mark visited nodes
      if (!visitedNodes.contains(parentCode)) {
        visitedNodes.add(parentCode);
      }
      if (!visitedNodes.contains(code)) {
        visitedNodes.add(code);
      }

      if (nodeSet.contains(parentCode) && nodeSet.contains(code)) {
        final String parentLabel = hierarchy.getName(parentCode);
        final String childLabel = hierarchy.getName(code);
        final String record = parentLabel + "|" + parentCode + "|" + childLabel + "|" + code;
        // TODO: inefficient, check against a set
        if (!codeToMainType.contains(record)) {
          codeToMainType.add(record);
        }

        if (visitedNodes.size() == nodeSet.size()) {
          break;
        }
        final List<String> parents = hierarchy.getSuperclassCodes(code);
        if (parents!= null) {
          for (final String parent : parents);
            queue.add(code + "|" + child);
          }
        
      } else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
        final List<String> children = hierarchy.getSubclassCodes(code);
        if (children != null) {
          for (final String child : children) {
            queue.add(parentCode + "|" + childCode);
          }
        }
      } else if (!nodeSet.contains(parentCode)) {
        final List<String> children = hierarchy.getSubclassCodes(code);
        if (children != null) {
          for (final String child : children) {
            queue.add(code + "|" + childCode);
          }
        }
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

    final List<String> mma = findMainMenuAncestors(concept);
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
   */
  public boolean isSubtype(final Concept concept) throws Exception {
    terminologyCheck(concept.getTerminology());
    String code = concept.getCode();
    if (code.equals("C7057"))
      return false;
    if (isNotDisease(concept)) {
      return false;
    }

    // Does not qualify if it starts with "recurrent"
    if (concept.getName().startsWith("recurrent"))
      return false;

    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    if (isDiseaseStage) {
      // if it doesn't have the word "stage"
      if (!name.contains("stage"))
        return true;
      return false;
    }

    final boolean isMainType = concept.getAssociations().stream()
        .filter(
            a -> a.getType().equals("Concept_In_Subset") && a.getRelatedCode().equals("C138190"))
        .count() > 0;

    if (isMainType) {
      final List<String> parents = hierarchy.getSuperclassCodes(code);
      // If there are parents that are not broad category codes,
      // then it qualifies
      if (parents.stream().filter(c -> !broadCategorySet.contains(code)).count() > 0) {
        return true;
      } else {
        return false;
      }
    }

    // Member of "Disease_Is_Grade" subset
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    if (isDiseaseGrade) {
      if (name.indexOf("grade") == -1) {
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
   */
  public Paths getMainMenuAncestors(final Concept concept) {
    terminologyCheck(concept.getTerminology());
    String code = concept.getCode();
    final boolean isDiseaseGrade =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count() > 0;
    final boolean isDiseaseStage =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;
    final boolean isSubType =
        concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Stage")).count() > 0;

    if (!isSubtype(concept) && !isDiseaseStage && !isDiseaseGrade) {
      return null;
    }

    List list = new ArrayList();
    Vector v = findMainMenuAncestors(concept);
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String ancestor_label = (String) u.elementAt(0);
      String ancestor_code = (String) u.elementAt(1);
      Paths paths = find_path_to_main_type_roots(ancestor_code);
      list.add(paths);
    }
    // KLO, 09182017
    list = selectPaths(list);
    return list;
  }

}
