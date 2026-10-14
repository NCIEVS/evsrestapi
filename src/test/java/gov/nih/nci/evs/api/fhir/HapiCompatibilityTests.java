package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.junit.jupiter.api.Test;

/** Data-independent smoke tests for the HAPI runtime and its JSON/XML dependencies. */
class HapiCompatibilityTests {

  /** Verify R4 JSON serialization and parsing with the resolved runtime dependencies. */
  @Test
  void r4JsonRoundTrip() {
    assertR4RoundTrip(FhirContext.forR4Cached().newJsonParser());
  }

  /** Verify R4 XML serialization and parsing with the resolved runtime dependencies. */
  @Test
  void r4XmlRoundTrip() {
    assertR4RoundTrip(FhirContext.forR4Cached().newXmlParser());
  }

  /** Verify R5 JSON serialization and parsing with the resolved runtime dependencies. */
  @Test
  void r5JsonRoundTrip() {
    assertR5RoundTrip(FhirContext.forR5Cached().newJsonParser());
  }

  /** Verify R5 XML serialization and parsing with the resolved runtime dependencies. */
  @Test
  void r5XmlRoundTrip() {
    assertR5RoundTrip(FhirContext.forR5Cached().newXmlParser());
  }

  /**
   * Check an R4 terminology resource round trip.
   *
   * @param parser the parser
   */
  private void assertR4RoundTrip(final IParser parser) {
    final org.hl7.fhir.r4.model.CodeSystem resource = new org.hl7.fhir.r4.model.CodeSystem();
    resource.setUrl("http://example.org/test-codes");
    resource.addConcept().setCode("test").setDisplay("Test & example");
    final org.hl7.fhir.r4.model.CodeSystem parsed =
        parser.parseResource(
            org.hl7.fhir.r4.model.CodeSystem.class, parser.encodeResourceToString(resource));
    assertEquals(resource.getUrl(), parsed.getUrl());
    assertEquals("test", parsed.getConceptFirstRep().getCode());
    assertEquals("Test & example", parsed.getConceptFirstRep().getDisplay());
  }

  /**
   * Check an R5 terminology resource round trip.
   *
   * @param parser the parser
   */
  private void assertR5RoundTrip(final IParser parser) {
    final org.hl7.fhir.r5.model.CodeSystem resource = new org.hl7.fhir.r5.model.CodeSystem();
    resource.setUrl("http://example.org/test-codes");
    resource.addConcept().setCode("test").setDisplay("Test & example");
    final org.hl7.fhir.r5.model.CodeSystem parsed =
        parser.parseResource(
            org.hl7.fhir.r5.model.CodeSystem.class, parser.encodeResourceToString(resource));
    assertEquals(resource.getUrl(), parsed.getUrl());
    assertEquals("test", parsed.getConceptFirstRep().getCode());
    assertEquals("Test & example", parsed.getConceptFirstRep().getDisplay());
  }
}
