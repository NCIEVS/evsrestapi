# AGENTS.md

## Scope

This package contains the main application service layer: OpenSearch querying, SPARQL query
management, metadata lookup, index/load workflows, recaptcha validation, and term suggestion form
handling.

## Service Layer Patterns

- Service interfaces define public service contracts; `*Impl` classes hold Spring-managed
  implementations.
- Controllers and FHIR providers should delegate domain retrieval, search, and metadata behavior
  here instead of building backend queries directly.
- Existing code generally uses field injection; follow local style for targeted edits unless a
  larger refactor is explicitly requested.
- Keep external API details isolated behind services and utilities where possible.

## OpenSearch and SPARQL Boundaries

- `OpenSearchServiceImpl` builds user-facing concept search queries and maps search hits to API
  result objects.
- `OpensearchQueryServiceImpl` performs direct indexed object, concept, metadata, and cached object
  lookups.
- `OpensearchOperationsServiceImpl` centralizes index operations, save/delete behavior, and audit
  persistence helpers.
- `SparqlQueryManagerServiceImpl` executes graph queries through `RESTUtils`, parses SPARQL JSON
  bindings, and coordinates graph-derived concept data.
- `QueryBuilderServiceImpl` constructs prefixes and graph query templates.
- `SparqlQueryCacheService` caches graph query results used by higher-level services.

## Loader Patterns

Loader implementations and sampling entry points have moved to evsrestapi-operations
`web/`, preserving their Java package names. Shared data-access services named below
now live in `evsrestapi-core`; keep the server independent of loader classes.
`MetadataService` remains a shared interface; its server implementation,
`MetadataServiceImpl`, stays in this package.

## Entry Points/Core Logic Files

- `OpenSearchService.java`, `OpenSearchServiceImpl.java`
- `OpensearchQueryService.java`, `OpensearchQueryServiceImpl.java`
- `OpensearchOperationsService.java`, `OpensearchOperationsServiceImpl.java`
- `SparqlQueryManagerService.java`, `SparqlQueryManagerServiceImpl.java`
- `QueryBuilderService.java`, `QueryBuilderServiceImpl.java`
- `MetadataService.java`, `MetadataServiceImpl.java`
- `TermSuggestionFormService.java`, `TermSuggestionFormServiceImpl.java`
- `CaptchaService.java`

## Verification Notes

For targeted service changes, prefer the matching tests under
`src/test/java/gov/nih/nci/evs/api/service`. Broaden to controller or FHIR tests when the service
contract changes user-visible endpoint behavior.
