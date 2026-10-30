package gov.nih.nci.evs.api.query;

import org.opensearch.index.query.QueryBuilder;

/**
 * How a value is matched, e.g. exactly, by prefix, or within a few edits, without saying which
 * field holds the value. {@link FieldMatch} applies a match to a concept field. A node that owns
 * its own fields applies it to those, e.g. a relationship's type to the entry's type name and code.
 *
 * <p>Matches compare against the whole indexed value, so they are intended for keyword fields.
 * Against an analyzed text field they operate on individual lowercased tokens instead.
 */
public interface ValueMatch {

  /**
   * Returns the OpenSearch query matching this against the field.
   *
   * @param field the field holding the value
   * @return the query builder
   */
  QueryBuilder getQueryBuilder(String field);
}
