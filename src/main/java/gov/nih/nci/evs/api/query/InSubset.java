package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;

/**
 * Matches concepts that are members of the given subset, e.g. every concept in CDISC SDTM
 * Terminology (C66830). A subset is itself a concept, and membership is an association from the
 * member to the subset: in NCIt, Concept_In_Subset (A8). So this is a {@link HasAssociation} whose
 * type is the terminology's membership association, read from {@link
 * TerminologyMetadata#getSubsetMember}.
 *
 * <p>The subset is given by its code or name, like any relationship's target.
 *
 * <p><b>Exploratory.</b> Combinations of subsets (in any of several, in all of them, in one but not
 * another) combine {@link InSubset}s with {@link Or}, {@link And}, and {@link Not}: each membership
 * is its own association entry, so combining per concept is correct.
 *
 * <p>Unlike the current search's {@code subset} parameter, this doesn't narrow a CDISC grouper
 * subset to members named "CDISC ...". That special case is business logic in {@code
 * OpenSearchServiceImpl}, left out until it's decided whether the query language should keep it.
 */
public final class InSubset extends EvsQuery {

  /** The subset's code or name. */
  private String target;

  /** Instantiates an empty {@link InSubset}. */
  public InSubset() {
    // n/a
  }

  /**
   * Instantiates an {@link InSubset} that matches members of the subset.
   *
   * @param target the subset's code or name
   */
  public InSubset(final String target) {
    this.target = Objects.requireNonNull(target, "target");
  }

  /**
   * Returns the subset's code or name.
   *
   * @return the subset code or name
   */
  public String getTarget() {
    return target;
  }

  /**
   * Sets the subset's code or name.
   *
   * @param target the subset code or name
   */
  public void setTarget(final String target) {
    this.target = target;
  }

  /**
   * Returns the query matching members of the subset. The association types marking membership
   * (e.g. A8) are gathered across every searched terminology, and a concept is a member through any
   * of them.
   *
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies being searched
   * @return the query builder
   * @throws IllegalArgumentException if no terminology has a subset membership association
   */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final List<EvsQuery> memberships =
        getMembershipTypeNamesOrCodes(terminologies).stream()
            .map(membershipType -> (EvsQuery) new HasAssociation(membershipType, target))
            .toList();

    return new Or(memberships).fetchOpenSearchQuery(operations, terminologies);
  }

  /**
   * Returns every searched terminology's subset membership associations. Fails when there are none,
   * since nothing could match.
   *
   * @param terminologies the terminologies being searched
   * @return the membership association names or codes
   * @throws IllegalArgumentException if no terminology has a subset membership association
   */
  private static Set<String> getMembershipTypeNamesOrCodes(final List<Terminology> terminologies) {
    final Set<String> membershipTypeNamesOrCodes = new HashSet<>();
    for (final Terminology terminology : terminologies) {
      final boolean hasMetadata = terminology.getMetadata() != null;
      if (hasMetadata) {
        membershipTypeNamesOrCodes.addAll(terminology.getMetadata().getSubsetMember());
      }
    }

    final boolean hasMembershipType = !membershipTypeNamesOrCodes.isEmpty();
    if (!hasMembershipType) {
      throw new IllegalArgumentException(
          "No searched terminology has a subset membership association (metadata subsetMember)");
    }

    return Set.copyOf(membershipTypeNamesOrCodes);
  }
}
