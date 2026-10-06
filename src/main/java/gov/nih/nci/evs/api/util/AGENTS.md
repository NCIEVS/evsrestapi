# AGENTS.md

## Scope

This package contains shared utility classes for terminology resolution, concept manipulation,
hierarchy/path calculations, REST/SPARQL calls, JSON handling, RRF sample/load helpers, FHIR helper
behavior, and model normalization.

## Utility Categories

- Terminology and concept helpers: `TerminologyUtils`, `ConceptUtils`, `ModelUtils`, `EVSUtils`.
- Hierarchy/path helpers: `HierarchyUtils`, `MainTypeHierarchy`, `PathUtils`, `HistoryUtils`.
- JSON/object mapper helpers: `JsonUtils`, `ThreadLocalMapper`.
- REST/FHIR helpers: `RESTUtils`, `FhirUtility`, `FHIRServerResponseException`.
- RRF/file helpers: `RrfReaders`, `RrfSampleGenerator`, `RrfFileCopier`, `FileSystemMap`,
  `PushBackReader`.

## Stateful vs Stateless Helpers

- Some helpers are Spring beans and can depend on services or properties.
- Many helpers are static or final utility classes and should stay free of Spring lifecycle
  assumptions.
- Avoid hiding endpoint-specific behavior in utilities; keep request semantics in controllers,
  providers, or services.

## Core Files

- `TerminologyUtils.java`
- `ConceptUtils.java`
- `HierarchyUtils.java`
- `MainTypeHierarchy.java`
- `RESTUtils.java`
- `FhirUtility.java`
- `ThreadLocalMapper.java`
- `FileSystemMap.java`
- `HistoryUtils.java`
- `PathUtils.java`

## Test Mapping

Utility-focused tests live under `src/test/java/gov/nih/nci/evs/api/util`, with additional service
tests covering utilities used through Spring services.
