# Shared terminology library

This is the Java 25 `java-library` subproject consumed by the server and operations.
Keep existing `gov.nih.nci.evs.api` package names, Java style, JSON/index annotations,
and public contracts. This module must not depend on the server, operations,
loader implementations, HAPI, or servlet/application startup configuration.

`src/main/resources` contains explicitly shared SPARQL/cache resources. Application
and logging settings belong to the consuming applications. Use the configured
Spotless/google-java-format and SpotBugs checks; run pure tests with
`./gradlew :evsrestapi-core:test` from the repository root. Database integration
coverage remains in the consuming applications.
