# AGENTS.md

## Scope

This package wires Spring configuration for JSON, caching, OpenAPI, FHIR servlet registration,
OpenSearch clients, response highlighting, scheduling, and property binding.

## Spring Configuration Patterns

- Configuration classes are small Spring `@Configuration` classes that expose explicit `@Bean`
  methods where framework auto-configuration is not enough.
- `PropertiesConfiguration` is the central place that binds `nci.evs.*` YAML properties to typed
  objects in `gov.nih.nci.evs.api.properties`.
- `FHIRConfig` registers the HAPI R4 and R5 servlet beans and assigns their URL patterns.
- `OpensearchConfiguration` creates the `RestHighLevelClient` and wraps it in
  `EvsOpenSearchRestTemplate` for `OpenSearchOperations` injection.

## External System Wiring

- OpenSearch host, port, scheme, and timeout are read from `nci.evs.opensearch.server.*` and
  `nci.evs.opensearch.timeout`.
- FHIR servlet mappings are `/fhir/r4/*` and `/fhir/r5/*`.
- Cache behavior is configured here, but cache use and object semantics belong to the service and
  support package contexts.

## Core Files

- `PropertiesConfiguration.java`
- `OpensearchConfiguration.java`
- `EvsOpenSearchRestTemplate.java`
- `FHIRConfig.java`
- `CacheConfiguration.java`
- `JacksonConfiguration.java`
- `OpenAPIDefinition.java`
- `EvsResponseHighlighterInterceptor.java`
- `CapabilityStatementCustomizer.java`

## Verification Notes

For configuration changes, prefer focused startup or context-loading tests first. Run broader tests
when changing beans used by controllers, FHIR providers, or OpenSearch operations.
