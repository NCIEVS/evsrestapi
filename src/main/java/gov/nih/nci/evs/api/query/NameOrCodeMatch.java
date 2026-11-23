package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches when either the name field or the code field equals any of the names or codes, e.g. a
 * relationship entry whose type name or type code is "R101". Combined with a raw bool query until
 * the logical operator nodes land, when it becomes an {@code Or} of two {@link MatchAnyOf}s.
 */
final class NameOrCodeMatch extends EvsQuery {

  /** Either the name or the code matching is enough. */
  private static final int MINIMUM_MATCHING_FIELDS = 1;

  /** The field holding the name. */
  private String nameField;

  /** The field holding the code. */
  private String codeField;

  /** The names or codes, any of which the name or code field may equal. */
  private Set<String> namesOrCodes;

  /** Instantiates an empty {@link NameOrCodeMatch}. */
  NameOrCodeMatch() {
    // n/a
  }

  /**
   * Instantiates a {@link NameOrCodeMatch}.
   *
   * @param nameField the field holding the name
   * @param codeField the field holding the code
   * @param namesOrCodes the names or codes, any of which the name or code field may equal
   */
  NameOrCodeMatch(
      final String nameField, final String codeField, final Collection<String> namesOrCodes) {
    this.nameField = Objects.requireNonNull(nameField, "nameField");
    this.codeField = Objects.requireNonNull(codeField, "codeField");
    this.namesOrCodes = new TreeSet<>(Objects.requireNonNull(namesOrCodes, "namesOrCodes"));
  }

  /**
   * Returns the field holding the name.
   *
   * @return the name field
   */
  public String getNameField() {
    return nameField;
  }

  /**
   * Sets the field holding the name.
   *
   * @param nameField the name field
   */
  public void setNameField(final String nameField) {
    this.nameField = nameField;
  }

  /**
   * Returns the field holding the code.
   *
   * @return the code field
   */
  public String getCodeField() {
    return codeField;
  }

  /**
   * Sets the field holding the code.
   *
   * @param codeField the code field
   */
  public void setCodeField(final String codeField) {
    this.codeField = codeField;
  }

  /**
   * Returns the names or codes, any of which the name or code field may equal.
   *
   * @return the names or codes
   */
  public Set<String> getNamesOrCodes() {
    return namesOrCodes;
  }

  /**
   * Sets the names or codes, any of which the name or code field may equal.
   *
   * @param namesOrCodes the names or codes
   */
  public void setNamesOrCodes(final Set<String> namesOrCodes) {
    this.namesOrCodes = namesOrCodes;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final MatchAnyOf nameMatch = new MatchAnyOf(nameField, namesOrCodes);
    final MatchAnyOf codeMatch = new MatchAnyOf(codeField, namesOrCodes);

    return QueryBuilders.boolQuery()
        .should(nameMatch.fetchOpenSearchQuery(operations, terminologies))
        .should(codeMatch.fetchOpenSearchQuery(operations, terminologies))
        .minimumShouldMatch(MINIMUM_MATCHING_FIELDS);
  }
}
