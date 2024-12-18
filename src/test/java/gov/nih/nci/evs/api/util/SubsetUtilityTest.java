package gov.nih.nci.evs.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import gov.nih.nci.evs.api.model.Synonym;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SubsetUtilityTest {
  static final String nullExceptionMsg = "Concept is null";

  @ParameterizedTest
  @MethodSource("getTestConceptsForIsSubset")
  void testIsSubset(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception = assertThrows(Exception.class, () -> SubsetUtility.isSubset(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.isSubset(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("getTestConceptsForIsCdisc")
  void testIsCdisc(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception = assertThrows(Exception.class, () -> SubsetUtility.isCdisc(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.isCdisc(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }


    @ParameterizedTest
    @MethodSource("getTestConceptsForIsCdiscSubset")
    void testIsCdiscSubset(Concept concept, boolean expectedResult, String expectedExceptionMsg)
        throws Exception {
      if (expectedExceptionMsg != null) {
        Exception exception = assertThrows(Exception.class, () -> SubsetUtility.isCdiscSubset(concept));
        assertEquals(expectedExceptionMsg, exception.getMessage());
      } else {
        try {
          assertEquals(expectedResult, SubsetUtility.isCdiscSubset(concept));
        } catch (Exception e) {
          fail("Unexpected exception: " + e.getMessage());
        }
      }
    }

  /**
   * Test data for isSubset method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getTestConceptsForIsSubset() {
    return Stream.of(
        Arguments.of(null, false, nullExceptionMsg),
        Arguments.of(new Concept(), false, nullExceptionMsg),
        Arguments.of(createConcept("other", (Property) null), false, nullExceptionMsg),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "no")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "yes")), true, null));
  }

  /**
   * Test data for isCdisc method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getTestConceptsForIsCdisc() {
    return Stream.of(
        Arguments.of(null, false, nullExceptionMsg),
        Arguments.of(new Concept(), false, nullExceptionMsg),
        Arguments.of(createConcept(null, (Property) null), false, nullExceptionMsg),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "CDISC")), true, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "MRCT-Ctr")), true, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "Other")), false, null));
  }

    /**
     * Test isCdiscSubset method.
     * @return
     */
  private static Stream<Arguments> getTestConceptsForIsCdiscSubset() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept is null"),
        Arguments.of(createConcept(null, (Property) null), false, "Concept is null"),
        Arguments.of(createConcept("other", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "no")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "yes")), false, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "CDISC")), false, null),
        Arguments.of(
            createConcept(
                "ncit",
                new Property("Publish_Value_Set", "yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConcept(
                "ncit",
                new Property("Publish_Value_Set", "yes"),
                new Property("Contributing_Source", "MRCT-Ctr")),
            true,
            null));
  }

    private static Stream<Arguments> provideConceptsForHasCdiscSynonym() {
        return Stream.of(
                Arguments.of(null, false, "Concept is null"),
                Arguments.of(new Concept(), false, "Concept is null"),
                Arguments.of(createConceptWithSynonyms(Collections.emptyList()), false, "Concept is null"),
                Arguments.of(createConceptWithSynonyms(List.of(new Synonym("CDISC", "SY", "name"))), true, null),
                Arguments.of(createConceptWithSynonyms(List.of(new Synonym("MRCT-Ctr", "SY", "name"))), true, null),
                Arguments.of(createConceptWithSynonyms(List.of(new Synonym("Other", "SY", "name"))), false, null),
                Arguments.of(createConceptWithSynonyms(List.of(new Synonym("CDISC", "PT", "name"))), false, null),
                Arguments.of(createConceptWithSynonyms(List.of(new Synonym("CDISC", "SY", ""))), false, "Concept property source is null")
        );
    }

  /**
   * Create a concept with the given terminology and properties. Helper method for testing.
   *
   * @param terminology the terminology
   * @param properties the properties of the concept
   * @return the concept
   */
  private static Concept createConcept(String terminology, Property... properties) {
    Concept concept = new Concept();
    concept.setTerminology(terminology);
    concept.setProperties(properties == null ? null : Arrays.asList(properties));
    return concept;
  }

    /**
     * Create a concept with the given synonyms. Helper method for testing.
     *
     * @param synonyms the synonyms of the concept
     * @return the concept
     */
    private static Concept createConceptWithSynonyms(List<Synonym> synonyms) {
        Concept concept = new Concept();
        concept.setSynonyms(synonyms);
        return concept;
    }
}
