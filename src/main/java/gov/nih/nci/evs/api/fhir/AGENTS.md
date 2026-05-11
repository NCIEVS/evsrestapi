# AGENTS.md

## Scope

This package contains the HAPI FHIR terminology server implementation for R4 and R5. Versioned
implementation classes live in `R4/` and `R5/`.

## HAPI FHIR Provider Pattern

- `HapiR4RestfulServlet` and `HapiR5RestfulServlet` register version-specific resource providers
  from the Spring application context.
- Provider classes implement HAPI `IResourceProvider` and expose operations through HAPI
  annotations such as `@Search`, `@Read`, and `@Operation`.
- Providers delegate EVS concept, terminology, and map lookup to controllers and services rather
  than duplicating backend access from scratch.
- Utility classes named `FhirUtilityR4` and `FhirUtilityR5` handle version-specific model creation,
  parameter validation, and exception conversion.

## Version Package Map

- `R4/` uses `org.hl7.fhir.r4.*` model classes and R4 HAPI annotations.
- `R5/` uses `org.hl7.fhir.r5.*` model classes and R5 HAPI annotations.
- Do not mix R4 imports into R5 files or R5 imports into R4 files. Static tests enforce this for
  most files.

## R4 Core Files

- `R4/HapiR4RestfulServlet.java`
- `R4/CodeSystemProviderR4.java`
- `R4/ValueSetProviderR4.java`
- `R4/ConceptMapProviderR4.java`
- `R4/FhirMetadataProviderR4.java`
- `R4/FHIRTerminologyCapabilitiesR4.java`
- `R4/FhirUtilityR4.java`
- `R4/OpenApiInterceptorR4.java`

## R5 Core Files

- `R5/HapiR5RestfulServlet.java`
- `R5/CodeSystemProviderR5.java`
- `R5/ValueSetProviderR5.java`
- `R5/ConceptMapProviderR5.java`
- `R5/FhirMetadataProviderR5.java`
- `R5/FhirTerminologyCapabilitiesR5.java`
- `R5/FhirUtilityR5.java`
- `R5/OpenApiInterceptorR5.java`

## FHIR Test Mapping

FHIR endpoint tests live under `src/test/java/gov/nih/nci/evs/api/fhir`; use that directory's
context file for test-specific patterns.
