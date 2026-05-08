# AGENTS.md

## Scope

This package tests service-layer behavior for query construction, SPARQL manager behavior,
hierarchy utilities, concept utility behavior, mapping, captcha, and term suggestion forms.

## Service Test Pattern

- Spring-backed service tests use `@SpringBootTest`, often with `RANDOM_PORT` and
  `@AutoConfigureMockMvc` when endpoint context or web beans are needed.
- Narrow pure-utility behavior should stay in focused tests when possible.
- Tests that depend on graph or OpenSearch data should align with the root setup docs and test
  resource configuration.

## Core Test Files

- `QueryBuilderServiceImplTest.java`
- `SparqlQueryManagerServiceImplTests.java`
- `HierarchyUtilsTest.java`
- `ConceptUtilsTest.java`
- `MainTypeHierarchyTest.java`
- `ConceptMappingTest.java`
- `TermSuggestionFormServiceTest.java`
- `CaptchaServiceTest.java`

## External Dependency Notes

Some service tests require the prepared UnitTestData, graph endpoint, or OpenSearch index. Prefer a
targeted test class first, then broaden only when service contracts affect controllers or FHIR
providers.
