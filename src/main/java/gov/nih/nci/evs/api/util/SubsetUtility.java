package gov.nih.nci.evs.api.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public static boolean isSubset(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    final List<Property> properties = concept.getProperties();

    // A concept is a subset if it has Publish_Value_Set=Yes (code=P372)
    if (concept.getTerminology().equals("ncit")) {
      for (final Property property : properties) {
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
  public static boolean isCdisc(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    String isCodeList;
    final List<Property> properties = concept.getProperties();

    // A subset is CDISC if it has a contributing source starting with CDISC or MRCT-Ctr
    for (final Property property : properties) {
      if (property.getType().equals("Contributing_Source")) {
        isCodeList = property.getValue();
        final boolean isCdisc = isCodeList.startsWith("CDISC") || isCodeList.startsWith("MRCT-Ctr");
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
  public static boolean isCdiscSubset(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a cdisc subset if it is a subset and is cdisc
    final boolean isCdiscSubset = isSubset(concept) && isCdisc(concept);
    return isCdiscSubset;
  }

  /**
   * Checks if the concept is a CDISC grouper subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC grouper subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscGrouper(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC grouper if it's a CDISC subset and does not have an SY synonym
    // (all codelist concepts have a CDISC/SY)
    final boolean isCdiscGrouper = isCdiscSubset(concept) && !hasCdiscSynonym(concept);
    return isCdiscGrouper;
  }

  /**
   * Checks if the concept is a CDISC codelist subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC codelist subset, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscCodeList(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC codelist if it is a CDISC subset and has an CDISC/SY synonym
    final boolean isCdiscCodeList = (isCdiscSubset(concept) && hasCdiscSynonym(concept));
    return isCdiscCodeList;
  }

  /**
   * Checks if the concept is a CDISC member.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC member, false otherwise
   * @throws Exception exception
   */
  public static boolean isCdiscMember(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // A concept is a CDISC member if it has a CDISC contributing source but is not itself a subset.
    final boolean isCdiscMember = (!isSubset(concept) && isCdisc(concept));
    return isCdiscMember;
  }

  /**
   * Returns the nci ab name for a subset to use in looking up a member map. This is for
   * extensions.cdiscSubmissionValueCode.
   *
   * @param subset the subset
   * @return the nci ab name
   * @throws Exception the exception
   */
  public static String getCodeListNciAbName(final Concept subset) throws Exception {
    final Optional<Synonym> nciAbSynonym =
        subset.getSynonyms().stream()
            .filter(
                synonym -> "NCI".equals(synonym.getSource()) && "AB".equals(synonym.getTermType()))
            .findFirst();

    if (nciAbSynonym.isPresent()) {
      return nciAbSynonym.get().getName();
    }
    return null;
  }

  /**
   * Get the CDISC submission value map for a concept. This all PT values with "code" as the key or
   * "default" if there is no code.
   *
   * @param subset The subset concept
   * @param concept The concept
   * @return The CDISC submission value
   * @throws Exception exception
   */
  public static Map<String, String> getCdiscSubmissionValues(final Concept concept)
      throws Exception {
    // Make sure our concepts are not null or empty
    checkConceptIsNullOrEmpty(concept);

    // Groupers don't have submission values
    if (isCdiscGrouper(concept)) {
      return null;
    }

    // Code lists will have exactly one CDISC/PT
    if (isCdiscCodeList(concept)) {
      return Map.of("default", getCdiscPt(concept).getName());
    }

    // Otherwise, get all of them and create entries by code (or if no code then use "default").
    final List<Synonym> cdiscSynonyms = getCdiscPts(concept);
    if (cdiscSynonyms.isEmpty()) {
      throw new Exception("Unable to find CDISC/PT = " + concept.getCode());
    }
    // Check if there is exactly one unique synonym
    if (cdiscSynonyms.size() == 1) {
      return Map.of("default", cdiscSynonyms.get(0).getName());
    }

    final Map<String, String> map = new HashMap<>();
    for (final Synonym synonym : cdiscSynonyms) {
      if (synonym.getCode() == null || synonym.getCode().isEmpty()) {
        throw new Exception(
            "Unexpected empty code CDISC/PT on a concept with multiple CDISC/PTs = "
                + concept.getCode());
      }
      map.put(synonym.getCode(), synonym.getName());
    }
    return map;
  }

  /**
   * Checks if the concept has a CDISC synonym.
   *
   * @param concept the ncit concept
   * @return true if the concept has a CDISC synonym, false otherwise
   * @throws Exception exception
   */
  public static boolean hasCdiscSynonym(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    final List<Synonym> synonyms = concept.getSynonyms();

    for (final Synonym synonym : synonyms) {
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
   * Returns the cdisc pts.
   *
   * @param concept the concept
   * @param source the source
   * @return the cdisc pts
   * @throws Exception the exception
   */
  private static List<Synonym> getCdiscPts(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    return concept.getSynonyms().stream()
        .filter(
            syn ->
                (syn.getSource().startsWith("CDISC")
                    || syn.getSource().equals("MRCT-Ctr") && syn.getTermType().equals("PT")))
        .collect(Collectors.toList());
  }

  /**
   * Get the CDISC synonyms that match the subset contributing source helper method
   *
   * @param concept the concept
   * @param source the subset contributing source
   * @return the CDISC synonyms that match the subset contributing source
   * @throws Exception exception
   */
  private static Synonym getCdiscPt(final Concept concept) throws Exception {
    checkConceptIsNullOrEmpty(concept);
    // Find exactly one PT name for this concept whose source matches
    final List<Synonym> list = getCdiscPts(concept);

    if (list.size() != 1) {
      throw new Exception(
          "Expecting exactly one synonym = "
              + list.size()
              + ", "
              + concept.getCode()
              + ", "
              + "CDISC/PT");
    }
    return list.get(0);
  }

  /**
   * Checks if the concept is null or empty. Helper method
   *
   * @param concept the concept
   * @throws Exception exception
   */
  private static void checkConceptIsNullOrEmpty(final Concept concept) throws Exception {
    if (concept == null) {
      throw new Exception("Concept is null");
    }
  }

  //  public static String getCdiscContributingSource(Concept subset) throws Exception {
  //
  //    if (!SubsetUtility.isCdiscSubset(subset)) {
  //      throw new Exception("A single contributing source is only valid for a CDISC subset");
  //    }
  //
  //    // A true subset concept here will have only a single contributing source
  //    for (final Property property : subset.getProperties()) {
  //      if (property.getType().equals("Contributing_Source")
  //          && (property.getValue().startsWith("CDISC")
  //              || property.getValue().startsWith("MRCT-Ctr"))) {
  //        return property.getValue();
  //      }
  //    }
  //    return null;
  //  }

  //  public static String getNciAbName(Concept subset, List<Synonym> cdiscSynonyms) throws
  // Exception {
  //    Optional<Synonym> nciAbSynonym =
  //        subset.getSynonyms().stream()
  //            .filter(
  //                synonym -> "NCI".equals(synonym.getSource()) &&
  // "AB".equals(synonym.getTermType()))
  //            .findFirst();
  //
  //    if (nciAbSynonym.isPresent()) {
  //      String nci_ab = nciAbSynonym.get().getName();
  //      Optional<Synonym> finalSynonym =
  //          cdiscSynonyms.stream().filter(syn -> nci_ab.equals(syn.getCode())).findFirst();
  //      if (finalSynonym.isPresent()) {
  //        return finalSynonym.get().getName();
  //      } else {
  //        throw new Exception(
  //            "Unable to find submission value (synonym that matches NCI source and AB term
  // type)");
  //      }
  //    }
  //    return null;
  //  }

}
