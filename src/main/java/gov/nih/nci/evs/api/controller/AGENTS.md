# AGENTS.md

## Scope

This package contains Spring MVC REST controllers for the non-FHIR EVS REST API.

## Controller Design Patterns

- Controllers are `@RestController` classes, usually mapped under
  `${nci.evs.application.contextPath}`.
- Most domain controllers extend `BaseController` to share unexpected exception handling and audit
  logging.
- Controllers should validate request parameters, resolve terminology metadata with
  `TerminologyUtils` or metadata services, and delegate retrieval/search logic to services.
- Keep OpenAPI annotations close to endpoint methods because they describe public API behavior.
- Avoid putting reusable query construction or OpenSearch access logic in controllers; use the
  service layer.

## Error/Audit Handling

- Use `BaseController.handleException(...)` for unexpected endpoint errors where the existing
  pattern applies.
- `ResponseStatusException` is allowed to pass through without being wrapped.
- Audit writes flow through `OpensearchOperationsService`.

## Entry Points/Core Logic Files

- `ConceptController.java` - concept reads, relationships, hierarchy, maps, history, and code lists.
- `SearchController.java` - concept search and SPARQL endpoint support.
- `MetadataController.java` - terminology metadata and metadata facets.
- `SubsetController.java` - subset listing, lookup, and members.
- `MapsetController.java` - mapset and map access.
- `HistoryController.java` - replacement history endpoints.
- `TermSuggestionFormController.java` - term suggestion form download and submission.
- `VersionController.java` - API version endpoint.
- `AdminController.java` - admin cache operations.
- `ErrorHandlerController.java` - HTML and JSON error response handling.
- `BaseController.java` - shared exception/audit behavior.

## Controller Test Mapping

Controller tests live under `src/test/java/gov/nih/nci/evs/api/controller`; use that directory's
context file for test-specific patterns.
