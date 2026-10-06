# AGENTS.md

## Scope

This package contains typed property holders for values bound from YAML, environment variables, and
Spring configuration.

## Property Binding Pattern

- Property objects are plain Java classes with fields and getters/setters.
- Binding is declared in `configuration/PropertiesConfiguration.java` using
  `@ConfigurationProperties`.
- Keep property names aligned with the existing `nci.evs.*` structure in application YAML files.
- Do not place connection logic or validation-heavy behavior here; put that in configuration or
  service classes.

## Core Files

- `ApplicationProperties.java` - application context, mail, metrics, UI license, test data, and
  recaptcha settings.
- `GraphProperties.java` - graph/SPARQL connection settings.
- `OpensearchQueryProperties.java` - search query tuning and field configuration.
- `OpensearchServerProperties.java` - OpenSearch server connection settings.

## Config Source References

Check `src/main/resources/application-local.yml`, `src/test/resources/application-test.yml`, and
`src/test/resources/application.yml` when changing property names or defaults.
