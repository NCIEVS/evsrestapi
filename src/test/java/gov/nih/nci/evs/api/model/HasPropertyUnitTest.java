package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.AllQualifiers;
import gov.nih.nci.evs.api.query.AnyQualifiers;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasProperty;
import gov.nih.nci.evs.api.query.NoQualifiers;
import gov.nih.nci.evs.api.query.PropertyQualifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasProperty}. */
public class HasPropertyUnitTest {
  /**
   * The nucleotide-excision repair GO annotations with go-id GO:0006289 whose evidence is neither
   * TAS nor NR.
   */
  private static final String REPAIR_ANNOTATION_WITHOUT_TAS_OR_NR_QUERY =
      """
      {"type": "HasProperty", "property": "P211", "target": "nucleotide-excision repair",
       "qualifiers": {"type": "AllQualifiers", "conditions": [
         {"type": "PropertyQualifier", "qualifier": "P387", "target": "GO:0006289"},
         {"type": "NoQualifiers", "condition": {"type": "AnyQualifiers", "conditions": [
           {"type": "PropertyQualifier", "qualifier": "P389", "target": "TAS"},
           {"type": "PropertyQualifier", "qualifier": "P389", "target": "NR"}]}}]}}
      """;

  /**
   * Test HasProperty round trips unchanged through Jackson.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasPropertyRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(REPAIR_ANNOTATION_WITHOUT_TAS_OR_NR_QUERY);
  }

  /**
   * Test HasProperty serializes to the expected JSON when built in Java: the HasProperty with
   * qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasPropertySerializesToExpectedJson() throws Exception {
    final EvsQuery repairAnnotationWithoutTasOrNr =
        new HasProperty(
            "P211",
            "nucleotide-excision repair",
            new AllQualifiers(
                List.of(
                    new PropertyQualifier("P387", "GO:0006289"),
                    new NoQualifiers(
                        new AnyQualifiers(
                            List.of(
                                new PropertyQualifier("P389", "TAS"),
                                new PropertyQualifier("P389", "NR")))))));

    assertSerializesTo(repairAnnotationWithoutTasOrNr, REPAIR_ANNOTATION_WITHOUT_TAS_OR_NR_QUERY);
  }
}
