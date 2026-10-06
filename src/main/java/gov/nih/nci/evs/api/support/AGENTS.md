# AGENTS.md

## Scope

This package contains supporting objects that do not fit the controller, service, model,
configuration, or utility packages. Its main subpackage is `support/es`.

## OpenSearch Support Objects

- `support/es/OpensearchObject.java` is a cached-object wrapper document for hierarchy, paths,
  concepts, association entries, statistics, and serialized map data.
- `support/es/OpensearchObjectMapping.java` and `OpensearchLoadConfig.java` describe load/index
  behavior.
- `support/es/IndexMetadata.java` represents index metadata used by load and operations services.
- `support/es/EVSPageable.java` customizes paging behavior for OpenSearch query offsets.
- `support/es/BaseResultMapper.java` and related mapper files are support code for result mapping.

## Mapper/Pageable Patterns

- Keep these classes focused on adapting OpenSearch data structures to EVS API objects.
- Avoid adding service orchestration here; service classes should own workflow decisions.
- Preserve Jackson and OpenSearch annotations that control how support objects are stored.

## Core Files

- `ApplicationVersion.java`
- `es/OpensearchObject.java`
- `es/OpensearchObjectMapping.java`
- `es/OpensearchLoadConfig.java`
- `es/IndexMetadata.java`
- `es/EVSPageable.java`
- `es/BaseResultMapper.java`
- `es/EVSConceptResultMapper.java`
- `es/EVSConceptMultiGetResultMapper.java`
