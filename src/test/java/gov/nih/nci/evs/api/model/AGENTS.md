# AGENTS.md

## Scope

This package tests API model behavior, including getters/setters, copy constructors,
serialization, equality, hash codes, and model-specific validation behavior.

## Model Test Pattern

- Use shared root test helpers instead of duplicating reflection, proxy, equality, or serialization
  setup.
- Keep model tests focused on object contracts and serialization/indexing behavior.
- When a model participates in endpoint behavior, cover endpoint semantics in controller or FHIR
  tests instead of expanding model unit tests into integration tests.

## Shared Tester Usage

- `GetterSetterTester`
- `CopyConstructorTester`
- `EqualsHashcodeTester`
- `SerializationTester`
- `ProxyTester`

## Core Test Files

- `ConceptUnitTest.java`
- `ConceptMinimalUnitTest.java`
- `ConceptResultListUnitTest.java`
- `TerminologyUnitTest.java`
- `TerminologyMetadataUnitTest.java`
- `PropertyUnitTest.java`
- `SynonymUnitTest.java`
- `DefinitionUnitTest.java`
- `AssociationUnitTest.java`
- `RoleUnitTest.java`
- `HistoryUnitTest.java`
- `ExtensionsUnitTest.java`
- `ResultListUnitTest.java`
- `EmailDetailsTest.java`
- `RecaptchaResponseTest.java`
