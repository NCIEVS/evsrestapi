# AGENTS.md

## Scope

This directory contains test configuration and fixture data used by controller, service, FHIR,
model, and utility tests.

## Application Test Config

- `application-test.yml` is the active Spring profile configuration for Gradle tests.
- `application.yml` provides additional test resource configuration.
- `logback-test.xml` controls test logging.

## Sample Data Directories

- `samples/` contains tab-delimited terminology sample fixtures consumed by `SampleTest`.
- `formSamples/` contains JSON, XLS, and XLSX fixtures for term suggestion form tests.
- `mapsetControllerParamTestSamples/` contains mapset parameter fixtures.
- `historyControllerParamTestSamples/` contains history replacement parameter fixtures.

## Other Fixture Files

- `extensions-test-*.txt` files support extension and hierarchy-related tests.
- `value_set_report_config.txt` supports value set report/load-related test behavior.
- `ncit.json` is a JSON terminology fixture.

## Fixture Guidelines

- Keep fixture names descriptive and aligned with the test class or behavior using them.
- Do not add real credentials or local machine paths.
- Prefer updating existing fixture families over introducing new one-off directories.
