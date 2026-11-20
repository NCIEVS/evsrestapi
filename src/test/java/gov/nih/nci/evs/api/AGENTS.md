# AGENTS.md

## Test Architecture Overview

This is the test Java package root. Tests use JUnit 5, Spring Boot test support, MockMvc,
`TestRestTemplate`, and shared helpers for model/proxy/serialization checks.

## Test Selection

- `make test-unit` runs PR-safe tests without prepared graph/OpenSearch data.
- Tag tests requiring loaded backends or real external services with `@Tag("integration")`.
  The tag is inherited by subclasses; terminology sample tests inherit it from `SampleTest`.
- Mock side effects such as audit writes in otherwise isolated controller tests.
- `make test` retains the full non-performance suite and requires configured backends and data.
- `make test-integration` runs only tagged integration tests; `perfTest` remains separate.

## Shared Test Helpers

- `ProxyTester.java` creates populated objects through reflection and proxies.
- `GetterSetterTester.java`, `CopyConstructorTester.java`, `EqualsHashcodeTester.java`, and
  `SerializationTester.java` provide reusable model contract checks.
- `ConceptSampleTester.java` and `SampleRecord.java` support terminology sample-based endpoint
  tests.
- `ApplicationTests.java` and `DynamicMappingTest.java` cover broader application/index behavior.

## Context Map

- `controller/AGENTS.md` - Spring MVC controller and sample endpoint tests.
- `fhir/AGENTS.md` - FHIR R4/R5 endpoint and SDK tests.
- `service/AGENTS.md` - service, query, hierarchy, mapping, and form tests.
- `model/AGENTS.md` - API model unit and contract tests.
- `code/AGENTS.md` - source-code rule tests.

## Small Mirror Package Notes

The `configuration`, `properties`, and `util` test packages are currently small. Keep their
package-specific notes here unless they grow enough to justify their own context files.
