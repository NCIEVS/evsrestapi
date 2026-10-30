# AGENTS.md

## Scope

This package contains source-code rule tests that inspect Java files directly.

## Source Code Rule Tests

- `AbstractSourceCodeTest` provides shared file-inspection helpers.
- `FhirReferencesSourceCodeTest` prevents accidental R4/R5 import crossovers in versioned FHIR
  files.
- `FieldMatchUsageSourceCodeTest` parses test sources and fails on any use of the query package's
  `FieldMatch` outside a class, method, or field marked `@DemonstratesFieldMatch`.
- These tests intentionally read source files from `src/main/java` and `src/test/java`.

## Core Test Files

- `AbstractSourceCodeTest.java`
- `FhirReferencesSourceCodeTest.java`
- `FieldMatchUsageSourceCodeTest.java`
