package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.JacksonConfiguration;
import gov.nih.nci.evs.api.query.EvsQuery;

/**
 * Automates JUnit testing of the JSON wire format of {@link EvsQuery} nodes, using the
 * application's object mapper from {@link JacksonConfiguration}. JSON is compared as {@link
 * JsonNode}s, so key order and whitespace don't matter.
 */
public final class EvsQuerySerializationTester {

  /** The application's object mapper, the one the query endpoint reads queries with. */
  private static final ObjectMapper MAPPER = new JacksonConfiguration().objectMapper();

  /** Instantiates an empty {@link EvsQuerySerializationTester}. */
  private EvsQuerySerializationTester() {
    // n/a
  }

  /**
   * Reads query JSON as an {@link EvsQuery}, writes it back, and asserts the result equals the
   * original.
   *
   * @param queryJson the query tree as JSON
   * @throws Exception the exception
   */
  public static void assertRoundTripsUnchanged(final String queryJson) throws Exception {
    final JsonNode original = MAPPER.readTree(queryJson);

    final EvsQuery query = MAPPER.readValue(queryJson, EvsQuery.class);
    final JsonNode roundTripped = MAPPER.readTree(MAPPER.writeValueAsString(query));

    assertThat(roundTripped).isEqualTo(original);
  }

  /**
   * Asserts the query is written as the expected JSON. A regression check on the wire format: a
   * renamed class, property, or enum constant changes the JSON clients send and receive.
   *
   * @param query the query, built in Java
   * @param expectedJson the JSON it must be written as
   * @throws Exception the exception
   */
  public static void assertSerializesTo(final EvsQuery query, final String expectedJson)
      throws Exception {
    final JsonNode serialized = MAPPER.readTree(MAPPER.writeValueAsString(query));

    assertThat(serialized).isEqualTo(MAPPER.readTree(expectedJson));
  }
}
