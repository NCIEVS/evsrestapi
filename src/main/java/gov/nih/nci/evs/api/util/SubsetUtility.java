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
public class SubsetUtility {

  /**
   * Checks if the concept is ncit and a subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a subset, false otherwise
   * @throws Exception exception
   */
  public boolean isSubset(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
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
  public boolean isCdisc(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    String isCodeList;
    List<Property> properties = concept.getProperties();

    for (Property property : properties) {
      if (property.getType().equals("Contributing_Source")) {
        isCodeList = property.getValue();
        return isCodeList.equals("CDISC") || isCodeList.equals("MRCT-Ctr");
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
  public boolean isCdiscSubset(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    return isSubset(concept) && isCdisc(concept);
  }

  /**
   * Checks if the concept has a CDISC synonym.
   *
   * @param concept the ncit concept
   * @return true if the concept has a CDISC synonym, false otherwise
   * @throws Exception exception
   */
  public boolean hasCdiscSynonym(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    List<Synonym> synonyms = concept.getSynonyms();

    for (Synonym synonym : synonyms) {
      if (!synonym.getSource().isEmpty() || !synonym.getSource().isBlank()) {
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
  public boolean isCdiscGrouper(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    return isCdiscSubset(concept) && !hasCdiscSynonym(concept);
  }

  /**
   * Checks if the concept is a CDISC codelist subset.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC codelist subset, false otherwise
   * @throws Exception exception
   */
  public boolean isCdiscCodeList(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    return isCdiscSubset(concept) && hasCdiscSynonym(concept);
  }

  /**
   * Checks if the concept is a CDISC member.
   *
   * @param concept the ncit concept
   * @return true if the concept is a CDISC member, false otherwise
   * @throws Exception exception
   */
  public boolean isCdiscMember(Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    return !isSubset(concept) && isCdisc(concept);
  }

  public String getCdiscSubmissionValue(Concept subset, Concept concept) throws Exception {
    if (concept == null || concept.getProperties() == null) {
      throw new Exception("Concept is null");
    }
    // Groupers don't have submission values
    if (isCdiscGrouper(concept) || isCdiscCodeList(subset)) {
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
        throw new Exception("Unable to find submission value because codelist lacks contributing source");
    }

    // Find the matching contributing source PT
    List<Synonym> cdiscSynonyms = getCdiscSynonyms(concept, subsetContributingSource);
    if (cdiscSynonyms.isEmpty()) {
        throw new Exception("Unable to find submission value for " + subsetContributingSource + "/PT");
    }
    // Check if there is exactly one unique synonym
    String nci_ab;
    if (cdiscSynonyms.size() == 1) {
      return cdiscSynonyms.get(0).getName();
    }
    // Else, find the NCI/AB of the codelist concept and the CDISC/PT w/a code matching the NCI/AB name
    return getNciAbName(subset, cdiscSynonyms);
  }

    /**
     * Get the subset contributing source helper method
     * @param subset the subset concept
     * @return the subset contributing source
     * @throws Exception exception
     */
  private String getSubsetContributingSource(Concept subset) throws Exception {
    if (subset == null || subset.getProperties() == null) {
      throw new Exception("Subset is null");
    }
    List<Property> properties = subset.getProperties();
    Optional<Property> subsetContSource =
        properties.stream()
            .filter(item -> "Contributing_Source".equals(item.getType()))
            .findFirst();
    return subsetContSource.map(Property::getValue).orElse(null);
  }

  /**
   * Get the CDISC synonyms that match the subset contributing source helper method
   * @param concept the concept
   * @param subsetContSource the subset contributing source
   * @return the CDISC synonyms that match the subset contributing source
   * @throws Exception exception
   */
  private List<Synonym> getCdiscSynonyms(Concept concept, String subsetContSource) throws Exception {
    if (concept == null || concept.getSynonyms() == null) {
      throw new Exception("Concept is null");
    }
    return concept.getSynonyms().stream()
            .filter(syn -> subsetContSource.equals(syn.getSource()) && "PT".equals(syn.getTermType()))
            .collect(Collectors.toList());
  }

  /**
   * Get the NCI/AB PT name helper method
   * @param subset the subset concept
   * @return the NCI/AB PT name
   * @throws Exception exception
   */
  private String getNciAbName(Concept subset, List<Synonym> cdiscSynonyms) throws Exception {
    if (subset == null || subset.getProperties() == null) {
      throw new Exception("Selected subset is null");
    }
    Optional<Synonym> nciAbSynonym = subset.getSynonyms().stream()
            .filter(synonym -> "NCI".equals(synonym.getSource()) && "AB".equals(synonym.getTermType()))
            .findFirst();

    if (nciAbSynonym.isPresent()) {
      String nci_ab = nciAbSynonym.get().getName();
      Optional<Synonym> finalSynonym = cdiscSynonyms.stream()
              .filter(syn -> nci_ab.equals(syn.getCode()))
              .findFirst();
      if (finalSynonym.isPresent()) {
        return finalSynonym.get().getName();
      } else {
        throw new Exception("Unable to find submission value (synonym that matches NCI source and AB term type)");
      }
    }
    return null;
  }

}
