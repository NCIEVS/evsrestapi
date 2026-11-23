# AGENTS.md

## Scope

This package models a concept search as a tree of query nodes (`EvsQuery`). Each node builds its
OpenSearch query in `fetchOpenSearchQuery(operations, terminologies)`; nodes don't hold the
terminologies. Read `README.md` in this package before
adding or changing a node: it covers term searches, field nodes, relationships, hierarchy
queries, and how to add a new node.

## Flat Nodes

- Only the logical nodes (`And`, `Or`, `Not`, `Boost`) hold other `EvsQuery` nodes. Every other node
  holds only plain `String` names, codes, and values, compared exactly against fields stored on the
  concept, e.g. a relationship's `target`. Don't add nodes that nest queries.
- Partial, fuzzy, or case-insensitive matching is only done by the term searches (`Exact`,
  `StartsWith`, `Contains`, `Fuzzy`, subclasses of `TermSearch`), which mirror the legacy
  `GET /concept/search` by calling `OpenSearchServiceImpl.getMatchQuery`/`getContainsQuery`. Don't
  add per-field match modes to the field nodes.
- The hierarchy nodes call the target "target", not "anchor". They look up target codes first
  (`HasHierarchyRelative.fetchTargetCodes`) only until ancestors and descendants are reindexed as nested
  entries. Don't add other searches that run before the main query.
- `StoredPaths` and `StoredHierarchyFallback` belong to the reindexing work; leave them as they are.

## Combining Conditions: Raw OpenSearch For Now

- Queries will eventually be built almost entirely from logical operator nodes (`And`, `Or`, `Not`,
  `Boost`). On the exploratory branch `eas/explore-filters` they're merged in and may be used to
  combine nodes, e.g. in tests. They're still only lightly tested: on other branches, don't add them
  and don't depend on `eas/logical-operators`.
- Until they land, combine conditions inside `fetchOpenSearchQuery` with raw OpenSearch
  (`QueryBuilders.boolQuery()`, `nestedQuery`, `constantScoreQuery`, ...). This code will be
  refactored onto the logical nodes later.
- Build leaf conditions with `MatchAnyOf` (or `NameOrCodeMatch` for a name or code) and call its
  `fetchOpenSearchQuery` instead of writing `termQuery`/`termsQuery` by hand.
- `MatchAnyOf` and `NameOrCodeMatch` are only for building nodes. Don't make them public;
  tutorials like `HasRoleTest` must use public nodes.
- Keep the raw combining code in small, clearly named helpers, so each maps onto one logical node.

## Tests

- `TermSearchTest`, `HasRoleTest`, and `HasAssociationTest` run against OpenSearch on port 9201.
- `HasHierarchyRelativeTargetCodesTest` uses a mocked `OpenSearchOperations` and needs no services.
