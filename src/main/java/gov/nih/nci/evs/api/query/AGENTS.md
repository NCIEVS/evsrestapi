# AGENTS.md

## Scope

This package models a concept search as a tree of query nodes (`EvsQuery`). Each node builds its
OpenSearch query in `fetchOpenSearchQuery(operations)`. Read `README.md` in this package before
adding or changing a node: it covers field and value matches, `CaseInsensitive`, queries about other
concepts (`declareDependencies`), and how to add a new node.

## Combining Conditions: Raw OpenSearch For Now

- Queries will eventually be built almost entirely from logical operator nodes (`And`, `Or`, `Not`,
  `Boost`). Those live on a separate branch and are not tested yet. Do not add them here and do not
  depend on that branch.
- Until they land, combine conditions inside `fetchOpenSearchQuery` with raw OpenSearch
  (`QueryBuilders.boolQuery()`, `nestedQuery`, `constantScoreQuery`, ...). This code will be
  refactored onto the logical nodes later.
- Build leaf conditions with `FieldMatch` and a value match (`Exact`, `StartsWith`, ...) and call its
  `fetchOpenSearchQuery` instead of writing `termQuery`/`prefixQuery` by hand.
- `FieldMatch` is only for building nodes. Don't make it public. In tests, use it only inside a
  class, method, or field marked `@DemonstratesFieldMatch`; tutorials must use public nodes.
  `FieldMatchUsageSourceCodeTest` enforces this.
- Keep the raw combining code in small, clearly named helpers, so each maps onto one logical node.

## Tests

- `ValueMatchTest` runs against OpenSearch on port 9201.
- `DeclareDependenciesTest` uses a mocked `OpenSearchOperations` and needs no services.
