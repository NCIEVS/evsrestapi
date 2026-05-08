# AGENTS.md

## Scope

This package contains cross-cutting metric recording support.

## Aspect Pattern

- `RecordMetric` is the marker annotation for methods whose runtime metrics should be captured.
- `MetricAdvice` is a Spring `@Aspect` and `@Component` that wraps annotated methods.
- Metric persistence and configuration depend on model, properties, and service classes; keep this
  package limited to the aspect boundary.

## Core Files

- `RecordMetric.java`
- `MetricAdvice.java`

## Related Configuration

Metric output and enablement are controlled through application properties. See the properties and
configuration package contexts before changing binding or setup.
