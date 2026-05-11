# AGENTS.md

## Scope

This package tests Spring MVC REST controllers, endpoint payload behavior, and terminology sample
coverage.

## MockMvc/SpringBootTest Pattern

- Most controller tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)` and
  `@AutoConfigureMockMvc`.
- Tests commonly exercise the actual Spring context and require the configured test OpenSearch and
  graph data described by the root project docs.
- Keep expected endpoint behavior near the controller being tested; shared terminology sample
  behavior belongs in `SampleTest` and `ConceptSampleTester`.

## Sample Test Pattern

- `SampleTest.java` is a disabled superclass that loads tab-delimited sample fixture files.
- Terminology-specific sample tests extend `SampleTest` and point at files under
  `src/test/resources/samples`.
- Do not duplicate sample parsing logic in individual terminology tests.

## Core Test Files

- `ConceptControllerTests.java`
- `ConceptControllerIncludeTests.java`
- `ConceptControllerExtensionTests.java`
- `SearchControllerTests.java`
- `MetadataControllerTests.java`
- `MetadataControllerIncludeTests.java`
- `SubsetControllerTests.java`
- `MapsetControllerTests.java`
- `HistoryControllerTests.java`
- `TermSuggestionFormControllerTests.java`
- `TermSuggestionFormControllerEmailTests.java`
- `SampleTest.java`

## Test Data Mapping

Controller tests use fixtures from `src/test/resources`, especially `samples`,
`mapsetControllerParamTestSamples`, `historyControllerParamTestSamples`, and `formSamples`.
