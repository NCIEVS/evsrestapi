# AGENTS.md

## Scope

This package contains API response/request models, OpenSearch document models, result lists, search
criteria objects, metadata objects, relationship objects, and SPARQL JSON binding models.

## Model/Object Pattern

- Models are plain Java objects with explicit constructors, getters, setters, copy/populate
  methods, and `equals`/`hashCode` where locally established.
- JSON behavior is controlled with Jackson annotations on model fields or classes.
- Public API documentation is often attached with Swagger/OpenAPI `@Schema` annotations.
- OpenSearch indexing behavior is controlled with Spring Data Elasticsearch/OpenSearch annotations
  such as `@Document`, `@Field`, `@Transient`, and `@WriteOnlyProperty`.
- Preserve no-argument constructors for serialization and test helpers unless there is a clear
  reason to change them.

## Serialization and Indexing Notes

- `Concept.java` is both a public API model and the primary OpenSearch concept document.
- `IncludeParam.java` controls field inclusion/exclusion used by query and controller layers.
- Relationship-like models include `Relationship`, `Association`, `Role`, `AssociationEntry`,
  `DisjointWith`, and related result lists.
- Hierarchy/path models include `HierarchyNode`, `Path`, and `Paths`.

## SPARQL Binding Models

The `model/sparql` subpackage contains JSON binding objects used when parsing graph query results:
`Sparql`, `Head`, `Results`, `Bindings`, and SPARQL-specific `Property`.

## Core Files

- `BaseModel.java`
- `Concept.java`
- `ConceptMinimal.java`
- `SearchCriteria.java`
- `SearchCriteriaWithoutTerminology.java`
- `Terminology.java`
- `TerminologyMetadata.java`
- `IncludeParam.java`
- `ResultList.java`
- `ConceptResultList.java`
- `Mapping.java`
- `MappingResultList.java`
- `EmailDetails.java`
- `Metric.java`
- `Audit.java`

## Model Test Mapping

Model tests and shared proxy/serialization test helpers live under
`src/test/java/gov/nih/nci/evs/api/model` and the test package root.
