# AGENTS.md

## Global Architecture Overview

EVS REST API is a Java 17, Gradle, Spring Boot application using a Maven-standard
`src/main` and `src/test` directory layout. The API exposes EVS terminology data through
Spring MVC REST controllers and HAPI FHIR R4/R5 endpoints.

The main runtime dependencies are a graph database endpoint for SPARQL queries and an
OpenSearch index for search, cached objects, metadata, and audit records. Production Java
code starts at `src/main/java/gov/nih/nci/evs/api/Application.java`.

Read these project docs before changing setup, infrastructure, graph, or OpenSearch behavior:

- `README.md`
- `JENA.md`
- `OPENSEARCH.md`
- `src/main/resources/application-local.yml`

## Build/Test Commands

- Build: `make clean build`
- Test: `make test`
- Format check: `./gradlew spotlessCheck`
- Format fix: `./gradlew spotlessApply`
- Vulnerability scan: `make scan` and review HIGH or CRITICAL entries in `report.html`

For local runs, use `-Dspring.profiles.active=local`. Local workflows usually require the
graph service and OpenSearch to be running.

## Global Coding Standards

- Prefer minimal, targeted changes.
- Do not modify unrelated files.
- Do not commit secrets, local credentials, or machine-specific data.
- Preserve existing project structure and naming conventions.
- Format Java code with the configured Spotless/google-java-format setup.
- Run the smallest useful verification for the files changed.
- Keep context clean: if a child directory has its own `AGENTS.md`, this root file only maps to
  that child context and does not duplicate its implementation rules.

## Context Hierarchy Map

- `src/main/java/gov/nih/nci/evs/api/AGENTS.md` - production Java package root and package map.
  - `configuration/AGENTS.md` - Spring bean and external system wiring.
  - `properties/AGENTS.md` - typed configuration property holders.
  - `controller/AGENTS.md` - Spring MVC REST endpoint layer.
  - `service/AGENTS.md` - search, SPARQL, metadata, form, and loader services.
  - `fhir/AGENTS.md` - HAPI FHIR R4/R5 servlet and provider layer.
  - `model/AGENTS.md` - API DTOs, OpenSearch documents, and SPARQL binding models.
  - `support/AGENTS.md` - support classes and OpenSearch wrapper objects.
  - `util/AGENTS.md` - shared utility classes.
  - `aop/AGENTS.md` - metrics aspect support.
- `src/test/java/gov/nih/nci/evs/api/AGENTS.md` - test package root and shared test helpers.
  - `controller/AGENTS.md` - MVC integration and terminology sample tests.
  - `fhir/AGENTS.md` - FHIR R4/R5 endpoint tests.
  - `service/AGENTS.md` - service, query, hierarchy, and form tests.
  - `model/AGENTS.md` - DTO, proxy, serialization, equals, and hash code tests.
  - `code/AGENTS.md` - source-code rule tests.
- `src/test/resources/AGENTS.md` - test configuration and fixture data.
