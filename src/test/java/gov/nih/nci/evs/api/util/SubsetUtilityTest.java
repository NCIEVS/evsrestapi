package gov.nih.nci.evs.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SubsetUtilityTest {

  /**
   * Test isSubset method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
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

  /**
   * Test isCdisc method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
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

  /**
   * Test isCdiscSubset method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getTestConceptsForIsCdiscSubset")
  void testIsCdiscSubset(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception =
          assertThrows(Exception.class, () -> SubsetUtility.isCdiscSubset(concept));
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
   * Test hasCdiscSynonym method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getConceptsForHasCdiscSynonym")
  void testHasCdiscSynonym(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception =
          assertThrows(Exception.class, () -> SubsetUtility.hasCdiscSynonym(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.hasCdiscSynonym(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }

  /**
   * Test isCdiscGrouper method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getConceptsForIsCdiscGrouper")
  void testIsCdiscGrouper(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception =
          assertThrows(Exception.class, () -> SubsetUtility.isCdiscGrouper(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.isCdiscGrouper(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }

  /**
   * Test isCdiscCodeList method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getConceptsForIsCdiscCodeList")
  void testIsCdiscCodeList(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception =
          assertThrows(Exception.class, () -> SubsetUtility.isCdiscCodeList(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.isCdiscCodeList(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }

  /**
   * Test isCdiscMember method for positive and negative cases.
   *
   * @param concept the concept
   * @param expectedResult the expected result
   * @param expectedExceptionMsg the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getConceptsForIsCdiscCodeList")
  void testIsCdiscMember(Concept concept, boolean expectedResult, String expectedExceptionMsg)
      throws Exception {
    if (expectedExceptionMsg != null) {
      Exception exception =
          assertThrows(Exception.class, () -> SubsetUtility.isCdiscMember(concept));
      assertEquals(expectedExceptionMsg, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.isCdiscCodeList(concept));
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
      }
    }
  }

  /**
   * Test getCdiscSubmissionValue method for positive and negative cases. TODO: Add more tests cases
   * for subsetContributingSource, NCI/AB synonyms, and CDISC/PT synonyms
   *
   * @param concept the concept
   * @param subset the subset concept
   * @param expectedResult the expected result
   * @param expectedExceptionMessage the expected exception message
   * @throws Exception exception
   */
  @ParameterizedTest
  @MethodSource("getConceptsForGetCdiscSubmissionValue")
  void testGetCdiscSubmissionValue(
      Concept subset, Concept concept, String expectedResult, String expectedExceptionMessage)
      throws Exception {
    if (expectedExceptionMessage != null) {
      Exception exception =
          assertThrows(
              Exception.class, () -> SubsetUtility.getCdiscSubmissionValue(concept, subset));
      assertEquals(expectedExceptionMessage, exception.getMessage());
    } else {
      try {
        assertEquals(expectedResult, SubsetUtility.getCdiscSubmissionValue(subset, concept));
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
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(createConcept("other"), false, "Concept has no properties"),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "No")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "Yes")), true, null));
  }

  /**
   * Test data for isCdisc method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getTestConceptsForIsCdisc() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(createConcept(null), false, "Concept has no properties"),
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
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getTestConceptsForIsCdiscSubset() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(createConcept(null), false, "Concept has no properties"),
        Arguments.of(createConcept("other", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "No")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "Yes")), false, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "CDISC")), false, null),
        Arguments.of(
            createConcept(
                "ncit",
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConcept(
                "ncit",
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "MRCT-Ctr")),
            true,
            null));
  }

  /**
   * Test data for hasCdiscSynonym method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getConceptsForHasCdiscSynonym() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("MRCT-Ctr", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("Other", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("", "", "")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            "Concept property source is null"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("", "SY", "")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            "Concept property source is null"));
  }

  /**
   * Test data for isCdiscGrouper method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getConceptsForIsCdiscGrouper() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(createConceptWithSynonyms(null, null), false, "Concept has no properties"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym(null, null, null)),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            "Concept property source is null"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("", "", "")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            "Concept property source is null"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("MRCT-Ctr", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "MRCT-Ctr")),
            false,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("Other", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("Other", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "MRCT-Ctr")),
            true,
            null));
  }

  /**
   * Test data for isCdiscCodeList method.
   *
   * @return Stream of arguments for the test method.
   */
  private static Stream<Arguments> getConceptsForIsCdiscCodeList() {
    return Stream.of(
        Arguments.of(null, false, "Concept is null"),
        Arguments.of(new Concept(), false, "Concept has no properties"),
        Arguments.of(createConcept(null), false, "Concept has no properties"),
        Arguments.of(createConcept("other", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Other_Type", "value")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "No")), false, null),
        Arguments.of(createConcept("ncit", new Property("Publish_Value_Set", "Yes")), false, null),
        Arguments.of(
            createConcept("ncit", new Property("Contributing_Source", "CDISC")), false, null),
        Arguments.of(
            createConcept(
                "ncit",
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            false,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("MRCT-Ctr", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            true,
            null));
  }

  private static Stream<Arguments> getConceptsForGetCdiscSubmissionValue() {
    return Stream.of(
        Arguments.of(null, null, null, "Concept is null"),
        Arguments.of(new Concept(), new Concept(), null, "Concept has no properties"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "Yes")),
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes")),
            null,
            "Unable to find submission value because codelist lacks contributing source"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "No")),
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes")),
            null,
            "Unable to find submission value because codelist lacks contributing source"),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            "",
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            "",
            null),
        // Should return the PT synonym value from the concept
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(
                    createSynonym("NCI", "AB", "NCI AB value"),
                    createSynonym("CDISC", "PT", "CDISC PT value"),
                    createSynonym("CDISC", "SY", "CDISC SY value")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            createConceptWithSynonyms(
                "ncit",
                List.of(
                    createSynonym("NCI", "PT", "NCI PT value"),
                    createSynonym("NCI", "AB", "AB Value"),
                    createSynonym("CDISC", "SY", "Other SY value")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            "NCI PT value",
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(
                    createSynonym("NCI", "PT", "test term name"),
                    createSynonym("NCI", "AB", "return value"),
                    createSynonym("CDISC", "SY", "Other synonym")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            createConceptWithSynonyms(
                "ncit",
                List.of(
                    createSynonym("CDISC", "PT", "SDTM"),
                    createSynonym("NCI", "PT", "CDISC SDTM Terminology"),
                    createSynonym("CDISC", "SY", "Other synonym")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "CDISC")),
            "SDTM",
            null),
        Arguments.of(
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "SY", "test term name")),
                new Property("Publish_Value_Set", "Yes")),
            createConceptWithSynonyms(
                "ncit",
                List.of(createSynonym("CDISC", "PT", "test term name")),
                new Property("Publish_Value_Set", "Yes"),
                new Property("Contributing_Source", "Other")),
            null,
            "Unable to find submission value for Other/PT"));
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

  private static Synonym createSynonym(String source, String termType, String termName) {
    Synonym synonym = new Synonym();
    synonym.setSource(source);
    synonym.setTermType(termType);
    synonym.setName(termName);
    return synonym;
  }

  /**
   * Create a concept with the given synonyms. Helper method for testing.
   *
   * @param terminology the terminology
   * @param synonyms the list of synonyms
   * @param properties the properties of the concept
   * @return the concept
   */
  private static Concept createConceptWithSynonyms(
      String terminology, List<Synonym> synonyms, Property... properties) {
    Concept concept = createConcept(terminology, properties);
    concept.setSynonyms(synonyms);

    return concept;
  }
}
