package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    String isSubset;
    List<Property> properties = concept.getProperties();

    if (concept.getTerminology().equals("ncit")) {
      for (Property property : properties) {
        if (property.getType().equals("Publish_Value_Set")) {
          isSubset = property.getValue();
          return isSubset.equals("yes");
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
    boolean isCdiscSubset = (isSubset(concept) && isCdisc(concept));
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
      if (synonym.getSource() == null
          || synonym.getSource().isEmpty()
          || synonym.getSource().isBlank()) {
        throw new Exception("Concept property source is null");
      }
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
    boolean isCdiscGrouper = (isCdiscSubset(concept) && !hasCdiscSynonym(concept));
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
    // Groupers don't have submission values
    if (isCdiscGrouper(concept)) {
      return "";
    }
    if (isCdiscGrouper(subset)) {
      return "";
    }
    List<Synonym> synonyms = concept.getSynonyms();

    // Codelist have a submission value matching CDISC or MRCT-Ctr
    for (Synonym synonym : synonyms) {
      if (isCdiscCodeList(concept) && synonym.getTermType().equals("PT")) {
        return synonym.getName();
      }
    }
    // Otherwise concept is just a regular value. ASSUMPTION: codelist only has one contributing
    // source
    String subsetContributingSource = getSubsetContributingSource(subset);
    if (subsetContributingSource == null) {
      throw new Exception(
          "Unable to find submission value because codelist lacks contributing source");
    }

    // Find the matching contributing source PT
    List<Synonym> cdiscSynonyms = getCdiscSynonyms(concept, subsetContributingSource);
    if (cdiscSynonyms.isEmpty()) {
      throw new Exception(
          "Unable to find submission value for " + subsetContributingSource + "/PT");
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
  private static String getSubsetContributingSource(Concept subset) throws Exception {
    List<Property> properties = subset.getProperties();
    Optional<Property> subsetContSource =
        properties.stream()
            .filter(item -> "Contributing_Source".equals(item.getType()))
            .findFirst();
    return subsetContSource.map(Property::getValue).orElse(null);
  }

  /**
   * Get the CDISC synonyms that match the subset contributing source helper method
   *
   * @param concept the concept
   * @param subsetContSource the subset contributing source
   * @return the CDISC synonyms that match the subset contributing source
   * @throws Exception exception
   */
  private static List<Synonym> getCdiscSynonyms(Concept concept, String subsetContSource)
      throws Exception {
    checkConceptIsNullOrEmpty(concept);
    return concept.getSynonyms().stream()
        .filter(syn -> syn.getSource().equals(subsetContSource) && syn.getTermType().equals("PT"))
        .collect(Collectors.toList());
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
    if (concept == null || concept.getProperties() == null || concept.getProperties().isEmpty()) {
      throw new Exception("Concept is null");
    }
  }
}
