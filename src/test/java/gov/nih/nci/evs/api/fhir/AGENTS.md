# AGENTS.md

## Scope

This package tests the HAPI FHIR R4 and R5 endpoints exposed under `/fhir/r4` and `/fhir/r5`.

## FHIR Test Pattern

- Tests commonly start a random-port Spring Boot context and call endpoints with
  `TestRestTemplate`.
- Version-specific parser setup uses `FhirContext.forR4()` or `FhirContext.forR5()`.
- Keep R4 expectations in R4 test files and R5 expectations in R5 test files.
- Preserve lowercase ID expectations where tests document internally normalized FHIR IDs.

## Versioned Test Families

- CodeSystem tests cover lookup, validate-code, subsumes, read/search, and general operations.
- ValueSet tests cover expand, validate-code, read/search, and general operations.
- ConceptMap tests cover translate, read/search, and general operations.
- SDK tests verify FHIR client behavior.
- `FhirVersioningStrategyTests.java` covers version selection behavior shared across FHIR versions.

## Core Test Files

- `FhirR4CodeSystemLookupTests.java`
- `FhirR4CodeSystemValidateTests.java`
- `FhirR4CodeSystemSubsumesTests.java`
- `FhirR4ValueSetExpandTests.java`
- `FhirR4ValueSetValidateTests.java`
- `FhirR4ConceptMapTranslateTests.java`
- `FhirR5CodeSystemLookupTests.java`
- `FhirR5CodeSystemValidateTests.java`
- `FhirR5CodeSystemSubsumesTests.java`
- `FhirR5ValueSetExpandTests.java`
- `FhirR5ValueSetValidateTests.java`
- `FhirR5ConceptMapTranslateTests.java`
- `FhirVersioningStrategyTests.java`

## Required Fixtures

FHIR tests depend on the same indexed terminology and metadata fixtures used by the broader test
suite. Check `src/test/resources/AGENTS.md` for fixture layout.
