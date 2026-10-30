package gov.nih.nci.evs.api.query;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The concepts a dependency query found (see {@link EvsQuery#declareDependencies}), each identified
 * by its code within its terminology version. Codes are only unique within one terminology version:
 * two versions share codes.
 */
public final class ConceptKeys {

  /** The codes found in each terminology version. */
  private final Map<TerminologyVersion, Set<String>> codesByTerminologyVersion;

  /**
   * Instantiates a {@link ConceptKeys}.
   *
   * @param codesByTerminologyVersion the codes found in each terminology version
   */
  private ConceptKeys(final Map<TerminologyVersion, Set<String>> codesByTerminologyVersion) {
    this.codesByTerminologyVersion = Map.copyOf(codesByTerminologyVersion);
  }

  /**
   * Returns the keys of the concepts found, given their codes in each terminology version.
   *
   * @param codesByTerminologyVersion the codes found in each terminology version
   * @return the keys
   */
  static ConceptKeys of(final Map<TerminologyVersion, Set<String>> codesByTerminologyVersion) {
    return new ConceptKeys(
        codesByTerminologyVersion.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    codesOfTerminologyVersion ->
                        Set.copyOf(codesOfTerminologyVersion.getValue()))));
  }

  /**
   * Returns the codes found in each terminology version.
   *
   * @return the codes by terminology version
   */
  Map<TerminologyVersion, Set<String>> getCodesByTerminologyVersion() {
    return codesByTerminologyVersion;
  }

  /**
   * A terminology and one of its versions, e.g. ("ncit", "25.12e"). Codes are only unique within
   * one of these.
   *
   * @param terminology the terminology
   * @param version the version
   */
  record TerminologyVersion(String terminology, String version) {

    /**
     * Instantiates a {@link TerminologyVersion}, failing if either part is missing.
     *
     * @param terminology the terminology
     * @param version the version
     */
    TerminologyVersion {
      Objects.requireNonNull(terminology, "terminology");
      Objects.requireNonNull(version, "version");
    }
  }
}
