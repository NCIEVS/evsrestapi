# AGENTS.md

## Package Scope

This is the production Java package root for the EVS REST API. It contains the Spring Boot
application entry point and package-level context links for the main implementation areas.

## Application Entry Point

- `Application.java` starts the Spring Boot application, enables caching and scheduling, excludes
  JDBC and Elasticsearch auto-configuration, and configures SpringDoc map handling.

## Context Map

- `configuration/AGENTS.md` - Spring configuration, bean creation, and external system clients.
- `properties/AGENTS.md` - configuration property objects bound by `PropertiesConfiguration`.
- `controller/AGENTS.md` - Spring MVC REST controllers under the configured EVS context path.
- `service/AGENTS.md` - search, SPARQL, metadata, form, and load service implementations.
- `fhir/AGENTS.md` - HAPI FHIR R4/R5 servlets, providers, metadata, and OpenAPI interceptors.
- `model/AGENTS.md` - API models, result lists, criteria objects, and SPARQL JSON bindings.
- `support/AGENTS.md` - support classes, especially OpenSearch cached object wrappers.
- `util/AGENTS.md` - utility helpers for terminology, concepts, hierarchy, JSON, REST, and FHIR.
- `aop/AGENTS.md` - metric recording aspect and annotation.

## Do Not Duplicate Package Rules

When working inside a child package listed above, read that child package's `AGENTS.md` and keep
implementation-specific guidance there. This file should remain a package map and entry-point
summary.
