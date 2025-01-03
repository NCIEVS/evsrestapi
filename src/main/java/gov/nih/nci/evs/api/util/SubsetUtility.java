package gov.nih.nci.evs.api.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;

/**
 * Utility class for subset operations. Used for validating and determining if a CDISC subset
 * concepts are codelist or grouper subsets.
 */
public final class SubsetUtility {

  /**
   * Checks if the concept is ncit and a subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isSubset(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    List<Property> properties = concept.getProperties();

    // A concept is a subset if it has Publish_Value_Set=Yes (code=P372)
    if (concept.getTerminology().equals("ncit")) {
      for (Property property : properties) {
        if (property.getType().equals("Publish_Value_Set")) {
          return property.getValue().equals("Yes");
        }
      }
    }
    return false;
  }

  /**
   * Checks if the concept is a codelist subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a codelist subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdisc(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    String isCodeList;
    List<Property> properties = concept.getProperties();

    // A subset is CDISC if it has a contributing source starting with CDISC or MRCT-Ctr
    for (Property property : properties) {
      if (property.getType().equals("Contributing_Source")) {
        isCodeList = property.getValue();
        boolean isCdisc = isCodeList.startsWith("CDISC") || isCodeList.startsWith("MRCT-Ctr");
        return isCdisc;
      }
    }
    return false;
  }

  /**
   * Checks if the concept is a CDISC subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscSubset(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a cdisc subset if it is a subset and is cdisc
    boolean isCdiscSubset = isSubset(concept) && isCdisc(concept);
    return isCdiscSubset;
  }

  /**
   * Checks if the concept has a CDISC synonym.
   *
   * @param concept the ncit concept
   * @return true if the concept has a CDISC synonym, false otherwise
   * @throws Exception exception
   */
  public static boolean hasCdiscSynonym(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    List<Synonym> synonyms = concept.getSynonyms();

    for (Synonym synonym : synonyms) {
      // Skip entries where source is blank
      if (synonym.getSource() == null) {
        continue;
      }
      // A concept has a cdisc synonym if the source starts with CDISC or MRCT-Ctr and has a term
      // type of SY
      if ((synonym.getSource().startsWith("CDISC") || synonym.getSource().startsWith("MRCT-Ctr"))
          && synonym.getTermType().equals("SY")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the concept is a CDISC grouper subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC grouper subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscGrouper(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC grouper if it's a CDISC subset and does not have an SY synonym
    // (all codelist concepts have a CDISC/SY)
    boolean isCdiscGrouper = isCdiscSubset(concept) && !hasCdiscSynonym(concept);
    return isCdiscGrouper;
  }

  /**
   * Checks if the concept is a CDISC codelist subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC codelist subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscCodeList(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC codelist if it is a CDISC subset and has an CDISC/SY synonym
    boolean isCdiscCodeList = (isCdiscSubset(concept) && hasCdiscSynonym(concept));
    return isCdiscCodeList;
  }

  /**
   * Checks if the concept is a CDISC member.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC member, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscMember(Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC member if it has a CDISC contributing source but is not itself a subset.
    boolean isCdiscMember = (!isSubset(concept) && isCdisc(concept));
    return isCdiscMember;
  }

  /**
   * Get the CDISC submission value for a concept.
   *
   * @param subset The subset concept
   * @param concept The concept
   * @return The CDISC submission value
   * @throws Exception exception
   */
  public static String getCdiscSubmissionValue(Concept subset, Concept concept) throws Exception {
    // Make sure our concepts are not null or empty
    checkConceptIsNullOrEmpty(concept);
    checkConceptIsNullOrEmpty(subset);
    final String source = getCdiscContributingSource(subset);

    // Otherwise concept is just a regular value. ASSUMPTION: codelist only has one contributing
    // source
    if (source == null) {
      throw new Exception("Unable to find CDISC contributing source =" + subset.getCode());
    }

    // Groupers don't have submission values
    if (isCdiscGrouper(concept)) {
      return null;
    }

    // Code lists will have exactly one CDISC/PT
    if (isCdiscCodeList(concept)) {
      return getCdiscPt(concept, null).getName();
    }

    // Do this after the test of concept beign a codelist
    // This is an escape hatch for a "member" concept that gets connected to a grouper accidentally
    if (isCdiscGrouper(subset)) {
      return null;
    }

    // Find the matching contributing source PT
    List<Synonym> cdiscSynonyms = getCdiscPts(concept, source);
    if (cdiscSynonyms.isEmpty()) {
      throw new Exception("Unable to find submission value for " + source + "/PT");
    }
    // Check if there is exactly one unique synonym
    if (cdiscSynonyms.size() == 1) {
      return cdiscSynonyms.get(0).getName();
    }
    // Else, find the NCI/AB of the codelist concept and the CDISC/PT w/a code matching the NCI/AB
    // name
    String submissionValue = getNciAbName(subset, cdiscSynonyms);
    return submissionValue;
  }

  /**
   * Get the subset contributing source helper method
   *
   * @param subset the subset concept
   * @return the subset contributing source
   * @throws Exception exception
   */
  private static String getCdiscContributingSource(Concept subset) throws Exception {

    // A true subset concept here will have only a single contributing source
    for (final Property property : subset.getProperties()) {
      if (property.getType().equals("Contributing_Source")
          && (property.getValue().startsWith("CDISC")
              || property.getValue().startsWith("MRCT-Ctr"))) {
        return property.getValue();
      }
    }
    return null;
  }

  /**
   * Get the CDISC synonyms that match the subset contributing source helper method
   *
   * @param concept the concept
   * @param source the subset contributing source
   * @return the CDISC synonyms that match the subset contributing source
   * @throws Exception exception
   */
  private static Synonym getCdiscPt(Concept concept, String source) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // Find exactly one PT name for this concept whose source matches
    final List<Synonym> list =
        concept.getSynonyms().stream()
            .filter(
                syn ->
                    (source == null || syn.getSource().equals(source))
                        && syn.getTermType().equals("PT"))
            .collect(Collectors.toList());
    if (list.size() != 1) {
      throw new Exception(
          "Expecting exactly one synonym = "
              + list.size()
              + ", "
              + concept.getCode()
              + ", "
              + source
              + "/PT");
    }
    return list.get(0);
  }

  /**
   * Returns the cdisc pts.
   *
   * @param concept the concept
   * @param source the source
   * @return the cdisc pts
   * @throws Exception the exception
   */
  private static List<Synonym> getCdiscPts(Concept concept, String source) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // Find exactly one PT name for this concept whose source matches
    final List<Synonym> list =
        concept.getSynonyms().stream()
            .filter(
                syn ->
                    (source == null || syn.getSource().equals(source))
                        && syn.getTermType().equals("PT"))
            .collect(Collectors.toList());
    return list;
  }

  /**
   * Get the NCI/AB PT name helper method
   *
   * @param subset the subset concept
   * @return the NCI/AB PT name
   * @throws Exception exception
   */
  private static String getNciAbName(Concept subset, List<Synonym> cdiscSynonyms) throws Exception {
    Optional<Synonym> nciAbSynonym =
        subset.getSynonyms().stream()
            .filter(
                synonym -> "NCI".equals(synonym.getSource()) && "AB".equals(synonym.getTermType()))
            .findFirst();

    if (nciAbSynonym.isPresent()) {
      String nci_ab = nciAbSynonym.get().getName();
      Optional<Synonym> finalSynonym =
          cdiscSynonyms.stream().filter(syn -> nci_ab.equals(syn.getCode())).findFirst();
      if (finalSynonym.isPresent()) {
        return finalSynonym.get().getName();
      } else {
        throw new Exception(
            "Unable to find submission value (synonym that matches NCI source and AB term type)");
      }
    }
    return null;
  }

  /**
   * Checks if the concept is null or empty. Helper method
   *
   * @param concept the concept
   * @throws Exception exception
   */
  private static void checkConceptIsNullOrEmpty(Concept concept) throws Exception {
    if (concept == null) {
      throw new Exception("Concept is null");
    }
  }
}
